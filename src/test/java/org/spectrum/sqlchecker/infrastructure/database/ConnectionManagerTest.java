package org.spectrum.sqlchecker.infrastructure.database;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.spectrum.sqlchecker.domain.shared.exception.ConnectionException;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * ConnectionManager 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@DisplayName("ConnectionManager 单元测试")
class ConnectionManagerTest {

    private ConnectionManager connectionManager;

    @BeforeEach
    void setUp() {
        connectionManager = new ConnectionManager();
    }

    @Nested
    @DisplayName("registerConnection 方法测试")
    class RegisterConnectionTests {

        @Test
        @DisplayName("应该成功注册连接配置")
        void should_register_connection_config() {
            ConnectionManager.ConnectionConfig config = createMySqlConfig("test-db");

            connectionManager.registerConnection("test", config);

            assertThat(connectionManager.getConfig("test")).isEqualTo(config);
        }

        @Test
        @DisplayName("应该覆盖已存在的连接配置")
        void should_override_existing_config() {
            ConnectionManager.ConnectionConfig config1 = createMySqlConfig("db1");
            ConnectionManager.ConnectionConfig config2 = createMySqlConfig("db2");

            connectionManager.registerConnection("test", config1);
            connectionManager.registerConnection("test", config2);

            assertThat(connectionManager.getConfig("test")).isEqualTo(config2);
        }
    }

    @Nested
    @DisplayName("getConfig 方法测试")
    class GetConfigTests {

        @Test
        @DisplayName("应该返回存在的配置")
        void should_return_existing_config() {
            ConnectionManager.ConnectionConfig config = createMySqlConfig("test-db");
            connectionManager.registerConnection("test", config);

            assertThat(connectionManager.getConfig("test")).isEqualTo(config);
        }

        @Test
        @DisplayName("应该返回 null 当配置不存在")
        void should_return_null_when_config_not_exists() {
            assertThat(connectionManager.getConfig("non-existent")).isNull();
        }
    }

    @Nested
    @DisplayName("getRegisteredConnections 方法测试")
    class GetRegisteredConnectionsTests {

        @Test
        @DisplayName("应该返回所有已注册连接名称")
        void should_return_all_registered_connections() {
            connectionManager.registerConnection("conn1", createMySqlConfig("db1"));
            connectionManager.registerConnection("conn2", createMySqlConfig("db2"));

            assertThat(connectionManager.getRegisteredConnections())
                    .containsExactlyInAnyOrder("conn1", "conn2");
        }

        @Test
        @DisplayName("应该返回空集合当没有注册连接")
        void should_return_empty_when_no_connections() {
            assertThat(connectionManager.getRegisteredConnections()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getConnection 方法测试")
    class GetConnectionTests {

        @Test
        @DisplayName("应该抛出异常当连接不存在")
        void should_throw_exception_when_connection_not_exists() {
            assertThatThrownBy(() -> connectionManager.getConnection("non-existent"))
                    .isInstanceOf(ConnectionException.class)
                    .hasMessageContaining("Connection not found");
        }
    }

    @Nested
    @DisplayName("closeConnection 方法测试")
    class CloseConnectionTests {

        @Test
        @DisplayName("应该安全地关闭不存在的连接")
        void should_safely_close_non_existent_connection() {
            // 不应该抛出异常
            assertThatCode(() -> connectionManager.closeConnection("non-existent"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("closeAll 方法测试")
    class CloseAllTests {

        @Test
        @DisplayName("应该安全地关闭所有连接")
        void should_safely_close_all_connections() {
            connectionManager.registerConnection("conn1", createMySqlConfig("db1"));
            connectionManager.registerConnection("conn2", createMySqlConfig("db2"));

            assertThatCode(() -> connectionManager.closeAll())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("ConnectionConfig 内部类测试")
    class ConnectionConfigTests {

        @Test
        @DisplayName("应该返回正确的 MySQL 驱动类名")
        void should_return_mysql_driver_class_name() {
            ConnectionManager.ConnectionConfig config = new ConnectionManager.ConnectionConfig();
            config.setType("mysql");

            assertThat(config.getDriverClassName()).isEqualTo("com.mysql.cj.jdbc.Driver");
        }

        @Test
        @DisplayName("应该返回正确的 PostgreSQL 驱动类名")
        void should_return_postgresql_driver_class_name() {
            ConnectionManager.ConnectionConfig config = new ConnectionManager.ConnectionConfig();
            config.setType("postgresql");

            assertThat(config.getDriverClassName()).isEqualTo("org.postgresql.Driver");
        }

        @Test
        @DisplayName("应该返回正确的 H2 驱动类名")
        void should_return_h2_driver_class_name() {
            ConnectionManager.ConnectionConfig config = new ConnectionManager.ConnectionConfig();
            config.setType("h2");

            assertThat(config.getDriverClassName()).isEqualTo("org.h2.Driver");
        }

        @Test
        @DisplayName("应该抛出异常当数据库类型不支持")
        void should_throw_exception_for_unsupported_type() {
            ConnectionManager.ConnectionConfig config = new ConnectionManager.ConnectionConfig();
            config.setType("unsupported");

            assertThatThrownBy(config::getDriverClassName)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported database type");
        }

        @Test
        @DisplayName("应该构建正确的 JDBC URL")
        void should_build_correct_jdbc_url() {
            ConnectionManager.ConnectionConfig config = new ConnectionManager.ConnectionConfig();
            config.setType("mysql");
            config.setHost("localhost");
            config.setPort(3306);
            config.setDatabase("testdb");

            String url = config.getJdbcUrl();

            assertThat(url).isEqualTo("jdbc:mysql://localhost:3306/testdb");
        }

        @Test
        @DisplayName("应该构建带参数的 JDBC URL")
        void should_build_jdbc_url_with_parameters() {
            ConnectionManager.ConnectionConfig config = new ConnectionManager.ConnectionConfig();
            config.setType("mysql");
            config.setHost("localhost");
            config.setPort(3306);
            config.setDatabase("testdb");

            Map<String, String> params = new HashMap<>();
            params.put("useSSL", "false");
            config.setParameters(params);

            String url = config.getJdbcUrl();

            assertThat(url).contains("jdbc:mysql://localhost:3306/testdb");
            assertThat(url).contains("useSSL=false");
        }

        @Test
        @DisplayName("应该处理没有数据库名的 URL")
        void should_handle_url_without_database() {
            ConnectionManager.ConnectionConfig config = new ConnectionManager.ConnectionConfig();
            config.setType("mysql");
            config.setHost("localhost");
            config.setPort(3306);

            String url = config.getJdbcUrl();

            assertThat(url).isEqualTo("jdbc:mysql://localhost:3306");
        }

        @Test
        @DisplayName("getter 和 setter 应该正常工作")
        void should_work_with_getters_and_setters() {
            ConnectionManager.ConnectionConfig config = new ConnectionManager.ConnectionConfig();

            config.setName("test-name");
            config.setType("postgresql");
            config.setHost("db.example.com");
            config.setPort(5432);
            config.setDatabase("mydb");
            config.setUsername("admin");
            config.setPassword("secret");
            config.setCharset("utf8");

            assertThat(config.getName()).isEqualTo("test-name");
            assertThat(config.getType()).isEqualTo("postgresql");
            assertThat(config.getHost()).isEqualTo("db.example.com");
            assertThat(config.getPort()).isEqualTo(5432);
            assertThat(config.getDatabase()).isEqualTo("mydb");
            assertThat(config.getUsername()).isEqualTo("admin");
            assertThat(config.getPassword()).isEqualTo("secret");
            assertThat(config.getCharset()).isEqualTo("utf8");
        }
    }

    /**
     * 创建 MySQL 连接配置
     */
    private ConnectionManager.ConnectionConfig createMySqlConfig(String database) {
        ConnectionManager.ConnectionConfig config = new ConnectionManager.ConnectionConfig();
        config.setType("mysql");
        config.setHost("localhost");
        config.setPort(3306);
        config.setDatabase(database);
        config.setUsername("root");
        config.setPassword("password");
        return config;
    }
}
