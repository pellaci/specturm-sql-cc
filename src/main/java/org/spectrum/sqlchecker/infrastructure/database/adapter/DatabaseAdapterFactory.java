package org.spectrum.sqlchecker.infrastructure.database.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据库适配器工厂
 * <p>
 * 根据数据库类型返回对应的适配器实例
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Slf4j
@Component
public class DatabaseAdapterFactory {

    private final Map<String, DatabaseAdapter> adapters = new HashMap<>();

    public DatabaseAdapterFactory() {
        // 注册适配器实例
        MySqlAdapter mysqlAdapter = new MySqlAdapter();
        PostgreSqlAdapter postgreSqlAdapter = new PostgreSqlAdapter();
        H2Adapter h2Adapter = new H2Adapter();

        adapters.put("mysql", mysqlAdapter);
        adapters.put("mariadb", mysqlAdapter);
        adapters.put("postgresql", postgreSqlAdapter);
        adapters.put("postgres", postgreSqlAdapter);
        adapters.put("h2", h2Adapter);
    }

    /**
     * 获取数据库适配器
     *
     * @param dbType 数据库类型 (mysql, postgresql, h2, etc.)
     * @return 数据库适配器
     * @throws IllegalArgumentException 不支持的数据库类型
     */
    public DatabaseAdapter getAdapter(String dbType) {
        if (dbType == null || dbType.isEmpty()) {
            throw new IllegalArgumentException("Database type cannot be null or empty");
        }

        DatabaseAdapter adapter = adapters.get(dbType.toLowerCase());
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported database type: " + dbType +
                    ". Supported types: " + adapters.keySet());
        }

        return adapter;
    }

    /**
     * 检查是否支持指定的数据库类型
     *
     * @param dbType 数据库类型
     * @return 是否支持
     */
    public boolean isSupported(String dbType) {
        if (dbType == null) {
            return false;
        }
        return adapters.containsKey(dbType.toLowerCase());
    }

    /**
     * 获取所有支持的数据库类型
     *
     * @return 数据库类型列表
     */
    public java.util.Set<String> getSupportedTypes() {
        return adapters.keySet();
    }
}
