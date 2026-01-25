package org.spectrum.sqlchecker.infrastructure.database.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.spectrum.sqlchecker.application.analysis.dto.PlanNode;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * MySqlAdapter 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@DisplayName("MySqlAdapter 单元测试")
class MySqlAdapterTest {

    private MySqlAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MySqlAdapter();
    }

    @Nested
    @DisplayName("buildExplainSql 方法测试")
    class BuildExplainSqlTests {

        @Test
        @DisplayName("应该为普通 SQL 添加 EXPLAIN 前缀")
        void should_add_explain_prefix_to_normal_sql() {
            String sql = "SELECT * FROM users";

            String result = adapter.buildExplainSql(sql);

            assertThat(result).isEqualTo("EXPLAIN SELECT * FROM users");
        }

        @Test
        @DisplayName("不应该重复添加 EXPLAIN 前缀")
        void should_not_duplicate_explain_prefix() {
            String sql = "EXPLAIN SELECT * FROM users";

            String result = adapter.buildExplainSql(sql);

            assertThat(result).isEqualTo("EXPLAIN SELECT * FROM users");
        }

        @Test
        @DisplayName("应该处理小写 EXPLAIN")
        void should_handle_lowercase_explain() {
            String sql = "explain SELECT * FROM users";

            String result = adapter.buildExplainSql(sql);

            assertThat(result).isEqualTo("explain SELECT * FROM users");
        }

        @Test
        @DisplayName("应该处理带空格的 SQL")
        void should_handle_sql_with_whitespace() {
            String sql = "  SELECT * FROM users  ";

            String result = adapter.buildExplainSql(sql);

            assertThat(result).isEqualTo("EXPLAIN SELECT * FROM users");
        }
    }

    @Nested
    @DisplayName("parseExplainResult 方法测试")
    class ParseExplainResultTests {

        @Test
        @DisplayName("应该解析 EXPLAIN 结果集")
        void should_parse_explain_result_set() throws SQLException {
            ResultSet rs = mockExplainResultSet();

            DatabaseAdapter.ResultSetParseResult result = adapter.parseExplainResult(rs);

            assertThat(result.nodes()).isNotEmpty();
            assertThat(result.metadata()).containsKey("database");
            assertThat(result.metadata().get("database")).isEqualTo("MySQL");
        }

        @Test
        @DisplayName("应该正确解析 PlanNode 字段")
        void should_parse_plan_node_fields() throws SQLException {
            ResultSet rs = mockExplainResultSetWithData();

            DatabaseAdapter.ResultSetParseResult result = adapter.parseExplainResult(rs);

            assertThat(result.nodes()).hasSize(1);
            PlanNode node = result.nodes().get(0);
            assertThat(node.getId()).isEqualTo(1);
            assertThat(node.getSelectType()).isEqualTo("SIMPLE");
            assertThat(node.getTable()).isEqualTo("users");
            assertThat(node.getType()).isEqualTo("ALL");
            assertThat(node.getRows()).isEqualTo(1000L);
        }

        @Test
        @DisplayName("应该处理空结果集")
        void should_handle_empty_result_set() throws SQLException {
            ResultSet rs = mock(ResultSet.class);
            ResultSetMetaData metaData = mock(ResultSetMetaData.class);
            when(rs.getMetaData()).thenReturn(metaData);
            when(metaData.getColumnCount()).thenReturn(0);
            when(rs.next()).thenReturn(false);

            DatabaseAdapter.ResultSetParseResult result = adapter.parseExplainResult(rs);

            assertThat(result.nodes()).isEmpty();
        }
    }

    @Nested
    @DisplayName("testConnection 方法测试")
    class TestConnectionTests {

        @Test
        @DisplayName("应该返回 true 当连接有效")
        void should_return_true_when_connection_valid() throws SQLException {
            Connection conn = mock(Connection.class);
            Statement stmt = mock(Statement.class);
            ResultSet rs = mock(ResultSet.class);

            when(conn.createStatement()).thenReturn(stmt);
            when(stmt.executeQuery("SELECT 1")).thenReturn(rs);
            when(rs.next()).thenReturn(true);

            boolean result = adapter.testConnection(conn);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("应该返回 false 当查询失败")
        void should_return_false_when_query_fails() throws SQLException {
            Connection conn = mock(Connection.class);
            Statement stmt = mock(Statement.class);

            when(conn.createStatement()).thenReturn(stmt);
            when(stmt.executeQuery("SELECT 1")).thenThrow(new SQLException("Connection error"));

            boolean result = adapter.testConnection(conn);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("应该返回 false 当结果集为空")
        void should_return_false_when_result_set_empty() throws SQLException {
            Connection conn = mock(Connection.class);
            Statement stmt = mock(Statement.class);
            ResultSet rs = mock(ResultSet.class);

            when(conn.createStatement()).thenReturn(stmt);
            when(stmt.executeQuery("SELECT 1")).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            boolean result = adapter.testConnection(conn);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getDatabaseName 方法测试")
    class GetDatabaseNameTests {

        @Test
        @DisplayName("应该返回 MySQL")
        void should_return_mysql() {
            assertThat(adapter.getDatabaseName()).isEqualTo("MySQL");
        }
    }

    /**
     * 创建模拟的 EXPLAIN 结果集
     */
    private ResultSet mockExplainResultSet() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);

        when(rs.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("id");
        when(rs.next()).thenReturn(true, false);
        when(rs.getString(1)).thenReturn("1");

        return rs;
    }

    /**
     * 创建带完整数据的模拟 EXPLAIN 结果集
     */
    private ResultSet mockExplainResultSetWithData() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);

        when(rs.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(11);
        when(metaData.getColumnLabel(1)).thenReturn("id");
        when(metaData.getColumnLabel(2)).thenReturn("select_type");
        when(metaData.getColumnLabel(3)).thenReturn("table");
        when(metaData.getColumnLabel(4)).thenReturn("partitions");
        when(metaData.getColumnLabel(5)).thenReturn("type");
        when(metaData.getColumnLabel(6)).thenReturn("possible_keys");
        when(metaData.getColumnLabel(7)).thenReturn("key");
        when(metaData.getColumnLabel(8)).thenReturn("key_len");
        when(metaData.getColumnLabel(9)).thenReturn("ref");
        when(metaData.getColumnLabel(10)).thenReturn("rows");
        when(metaData.getColumnLabel(11)).thenReturn("extra");

        when(rs.next()).thenReturn(true, false);
        when(rs.getString(1)).thenReturn("1");
        when(rs.getString(2)).thenReturn("SIMPLE");
        when(rs.getString(3)).thenReturn("users");
        when(rs.getString(4)).thenReturn(null);
        when(rs.getString(5)).thenReturn("ALL");
        when(rs.getString(6)).thenReturn(null);
        when(rs.getString(7)).thenReturn(null);
        when(rs.getString(8)).thenReturn(null);
        when(rs.getString(9)).thenReturn(null);
        when(rs.getString(10)).thenReturn("1000");
        when(rs.getString(11)).thenReturn("Using where");

        return rs;
    }
}
