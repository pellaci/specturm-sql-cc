package org.spectrum.sqlchecker.infrastructure.database;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.infrastructure.config.YamlConfigLoader;
import org.spectrum.sqlchecker.infrastructure.database.adapter.DatabaseAdapterFactory;
import org.spectrum.sqlchecker.infrastructure.database.socket.UnixDomainSocketFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 数据库连接初始化器
 * <p>
 * 在应用启动时从 sqlchecker.yml 加载数据库连接配置并注册到 ConnectionManager
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sqlchecker.database", name = "auto-init", havingValue = "true", matchIfMissing = false)
public class DatabaseConnectionInitializer {

    private final ConnectionManager connectionManager;
    private final YamlConfigLoader yamlConfigLoader;
    private final DatabaseAdapterFactory adapterFactory;

    @PostConstruct
    public void initializeConnections() {
        log.info("Initializing database connections from sqlchecker.yml...");

        try {
            Map<String, Object> config = loadSqlCheckerConfig();

            if (config == null || config.isEmpty()) {
                log.info("No sqlchecker.yml found, skipping database connection initialization");
                return;
            }

            // 提取 database 配置
            Map<String, Object> dbConfig = (Map<String, Object>) config.get("database");
            if (dbConfig == null) {
                log.info("No 'database' section in sqlchecker.yml, skipping database connection initialization");
                return;
            }

            // 提取 connections 配置
            Map<String, Map<String, Object>> connections =
                    (Map<String, Map<String, Object>>) dbConfig.get("connections");

            if (connections == null || connections.isEmpty()) {
                log.info("No 'connections' found in sqlchecker.yml");
                return;
            }

            // 注册所有连接
            List<String> registered = new ArrayList<>();
            for (Map.Entry<String, Map<String, Object>> entry : connections.entrySet()) {
                String name = entry.getKey();
                try {
                    ConnectionManager.ConnectionConfig connConfig = buildConnectionConfig(entry.getValue());
                    connectionManager.registerConnection(name, connConfig);

                    // 验证适配器支持
                    if (!adapterFactory.isSupported(connConfig.getType())) {
                        log.warn("Database type '{}' is not supported for connection '{}'",
                                connConfig.getType(), name);
                    }

                    registered.add(name);
                    log.info("Registered database connection: {} ({})", name, connConfig.getType());
                } catch (Exception e) {
                    log.error("Failed to register database connection '{}': {}", name, e.getMessage());
                }
            }

            if (!registered.isEmpty()) {
                log.info("Database connections initialized: {}", registered);
            }

        } catch (Exception e) {
            log.error("Failed to initialize database connections", e);
        }
    }

    private Map<String, Object> loadSqlCheckerConfig() {
        Path local = Path.of("sqlchecker.yml");
        if (Files.exists(local)) {
            return yamlConfigLoader.loadExternalConfig(local.toString());
        }

        Path home = Path.of(System.getProperty("user.home"), ".sqlchecker.yml");
        if (Files.exists(home)) {
            return yamlConfigLoader.loadExternalConfig(home.toString());
        }

        return yamlConfigLoader.loadConfig("sqlchecker.yml");
    }

    /**
     * 从配置 Map 构建 ConnectionConfig
     */
    private ConnectionManager.ConnectionConfig buildConnectionConfig(Map<String, Object> config) {
        String type = getString(config, "type", "mysql");
        String host = getString(config, "host", "localhost");
        int port = getInt(config, "port", getDefaultPort(type));
        String database = getString(config, "database", "");
        String username = getString(config, "username", "");
        String password = getString(config, "password", "");
        String charset = getString(config, "charset", "utf8mb4");
        Map<String, Object> rawParameters = getMap(config.get("parameters"));
        String serverTimezone = getString(rawParameters, "serverTimezone",
                getString(config, "serverTimezone", "UTC"));
        boolean useSSL = getBoolean(rawParameters, "useSSL",
                getBoolean(config, "useSSL", false));
        String jdbcUrl = getString(config, "jdbcUrl",
                getString(config, "jdbc-url", ""));

        ConnectionManager.ConnectionConfig connConfig = new ConnectionManager.ConnectionConfig();
        connConfig.setType(type);
        connConfig.setHost(host);
        connConfig.setPort(port);
        connConfig.setDatabase(database);
        connConfig.setUsername(username);
        connConfig.setPassword(password);
        connConfig.setCharset(charset);
        connConfig.setJdbcUrlOverride(jdbcUrl);

        // 添加连接参数
        java.util.Map<String, String> parameters = new java.util.HashMap<>();
        for (Map.Entry<String, Object> entry : rawParameters.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                parameters.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        if (!parameters.containsKey("serverTimezone")) {
            parameters.put("serverTimezone", serverTimezone);
        }
        if (!parameters.containsKey("useSSL")) {
            parameters.put("useSSL", String.valueOf(useSSL));
        }
        if (charset != null && !charset.isBlank() && !parameters.containsKey("characterEncoding")) {
            String encoding = charset;
            if ("utf8".equalsIgnoreCase(charset) || "utf8mb4".equalsIgnoreCase(charset)) {
                encoding = "UTF-8";
            }
            parameters.put("characterEncoding", encoding);
        }
        if (parameters.containsKey("unix_socket") && !parameters.containsKey("socketFactory")) {
            if (UnixDomainSocketFactory.isSupported()) {
                parameters.put("socketFactory",
                        "org.spectrum.sqlchecker.infrastructure.database.socket.UnixDomainSocketFactory");
            } else {
                log.warn("Unix domain sockets are not supported by this JVM; ignoring unix_socket and using TCP.");
                parameters.remove("unix_socket");
            }
        }
        connConfig.setParameters(parameters);

        return connConfig;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return java.util.Collections.emptyMap();
    }

    private String getString(Map<String, Object> config, String key, String defaultValue) {
        Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }

    private int getInt(Map<String, Object> config, String key, int defaultValue) {
        Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean getBoolean(Map<String, Object> config, String key, boolean defaultValue) {
        Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private int getDefaultPort(String type) {
        return switch (type.toLowerCase()) {
            case "mysql", "mariadb" -> 3306;
            case "postgresql", "postgres" -> 5432;
            case "h2" -> 9092;
            case "oracle" -> 1521;
            case "sqlserver" -> 1433;
            default -> 3306;
        };
    }
}
