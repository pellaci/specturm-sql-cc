package org.spectrum.sqlchecker.infrastructure.database.adapter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 数据库适配器接口
 * <p>
 * 为不同数据库提供统一的 EXPLAIN 执行和结果解析接口
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
public interface DatabaseAdapter {

    /**
     * 构建 EXPLAIN SQL 语句
     *
     * @param sql 原始 SQL
     * @return EXPLAIN SQL
     */
    String buildExplainSql(String sql);

    /**
     * 解析 EXPLAIN 结果集
     *
     * @param rs 结果集
     * @return 执行计划节点列表
     * @throws SQLException 解析异常
     */
    ResultSetParseResult parseExplainResult(ResultSet rs) throws SQLException;

    /**
     * 测试数据库连接
     *
     * @param conn 数据库连接
     * @return 连接是否可用
     */
    boolean testConnection(Connection conn);

    /**
     * 获取数据库名称
     */
    String getDatabaseName();

    /**
     * EXPLAIN 结果解析结果
     */
    record ResultSetParseResult(
            java.util.List<org.spectrum.sqlchecker.application.analysis.dto.PlanNode> nodes,
            java.util.Map<String, Object> metadata
    ) {
    }
}
