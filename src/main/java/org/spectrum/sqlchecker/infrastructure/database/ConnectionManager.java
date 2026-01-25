package org.spectrum.sqlchecker.infrastructure.database;

import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.domain.shared.exception.ConnectionException;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库连接管理器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Slf4j
@Component
public class ConnectionManager {

    private final Map<String, ConnectionConfig> connections = new ConcurrentHashMap<>();
    private final Map<String, Connection> activeConnections = new ConcurrentHashMap<>();

    /**
     * 注册连接配置
     *
     * @param name 连接名称
     * @param config 连接配置
     */
    public void registerConnection(String name, ConnectionConfig config) {
        connections.put(name, config);
    }

    /**
     * 获取连接配置
     *
     * @param name 连接名称
     * @return 连接配置，不存在时返回 null
     */
    public ConnectionConfig getConfig(String name) {
        return connections.get(name);
    }

    /**
     * 获取所有已注册的连接名称
     *
     * @return 连接名称集合
     */
    public java.util.Set<String> getRegisteredConnections() {
        return connections.keySet();
    }

    /**
     * 获取连接
     *
     * @param name 连接名称
     * @return 数据库连接
     * @throws ConnectionException 连接失败
     */
    public Connection getConnection(String name) throws ConnectionException {
        ConnectionConfig config = connections.get(name);
        if (config == null) {
            throw new ConnectionException("Connection not found: " + name);
        }

        try {
            Connection conn = activeConnections.get(name);
            if (conn != null && !conn.isClosed()) {
                return conn;
            }

            conn = createConnection(config);
            activeConnections.put(name, conn);
            return conn;
        } catch (SQLException e) {
            throw new ConnectionException("Failed to get connection: " + name, e);
        }
    }

    /**
     * 创建连接
     */
    private Connection createConnection(ConnectionConfig config) throws SQLException {
        try {
            Class.forName(config.getDriverClassName());
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver not found: " + config.getDriverClassName(), e);
        }

        return DriverManager.getConnection(
                config.getJdbcUrl(),
                config.getUsername(),
                config.getPassword()
        );
    }

    /**
     * 关闭连接
     *
     * @param name 连接名称
     */
    public void closeConnection(String name) {
        Connection conn = activeConnections.remove(name);
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.error("Failed to close connection: {}", name, e);
            }
        }
    }

    /**
     * 关闭所有连接
     */
    public void closeAll() {
        activeConnections.keySet().forEach(this::closeConnection);
    }

    /**
     * 测试连接
     *
     * @param name 连接名称
     * @return 是否可用
     */
    public boolean testConnection(String name) {
        try (Connection conn = getConnection(name)) {
            return conn.isValid(5);
        } catch (Exception e) {
            log.error("Connection test failed: {}", name, e);
            return false;
        }
    }

    /**
     * 连接配置
     */
    public static class ConnectionConfig {
        private String name;
        private String type = "mysql";
        private String host = "localhost";
        private int port = 3306;
        private String database;
        private String username;
        private String password;
        private String charset = "utf8mb4";
        private String jdbcUrl;
        private Map<String, String> parameters = new ConcurrentHashMap<>();

        public String getDriverClassName() {
            return switch (type.toLowerCase()) {
                case "mysql" -> "com.mysql.cj.jdbc.Driver";
                case "postgresql" -> "org.postgresql.Driver";
                case "h2" -> "org.h2.Driver";
                default -> throw new IllegalArgumentException("Unsupported database type: " + type);
            };
        }

        public String getJdbcUrl() {
            if (jdbcUrl != null && !jdbcUrl.isBlank()) {
                return jdbcUrl;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("jdbc:").append(type).append("://");
            sb.append(host).append(":").append(port);
            if (database != null && !database.isEmpty()) {
                sb.append("/").append(database);
            }
            if (!parameters.isEmpty()) {
                sb.append("?");
                boolean first = true;
                for (Map.Entry<String, String> entry : parameters.entrySet()) {
                    if (!first) {
                        sb.append("&");
                    }
                    sb.append(entry.getKey()).append("=").append(entry.getValue());
                    first = false;
                }
            }
            return sb.toString();
        }

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getDatabase() { return database; }
        public void setDatabase(String database) { this.database = database; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getCharset() { return charset; }
        public void setCharset(String charset) { this.charset = charset; }
        public String getJdbcUrlOverride() { return jdbcUrl; }
        public void setJdbcUrlOverride(String jdbcUrl) { this.jdbcUrl = jdbcUrl; }
        public Map<String, String> getParameters() { return parameters; }
        public void setParameters(Map<String, String> parameters) { this.parameters = parameters; }
    }
}
