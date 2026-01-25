package org.spectrum.sqlchecker.infrastructure.database.adapter;

import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.application.analysis.dto.PlanNode;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MySQL 数据库适配器
 * <p>
 * 支持 MySQL 5.7+ 和 MariaDB 的 EXPLAIN 命令
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Slf4j
@Component
public class MySqlAdapter implements DatabaseAdapter {

    private static final String DATABASE_NAME = "MySQL";

    @Override
    public String buildExplainSql(String sql) {
        String trimmed = sql.trim();
        // 如果已经以 EXPLAIN 开头，直接返回
        if (trimmed.toUpperCase().startsWith("EXPLAIN")) {
            return trimmed;
        }
        return "EXPLAIN " + trimmed;
    }

    @Override
    public ResultSetParseResult parseExplainResult(ResultSet rs) throws SQLException {
        List<PlanNode> nodes = new ArrayList<>();
        Map<String, Object> metadata = new HashMap<>();

        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // 收集所有列名
        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
            columns.add(metaData.getColumnLabel(i).toLowerCase());
        }

        while (rs.next()) {
            PlanNode.PlanNodeBuilder builder = PlanNode.builder();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = columns.get(i - 1);
                String value = rs.getString(i);

                if (value == null) {
                    continue;
                }

                // 标准 EXPLAIN 字段
                switch (columnName) {
                    case "id":
                        try {
                            builder.id(Integer.parseInt(value));
                        } catch (NumberFormatException ignored) {
                            builder.id(null);
                        }
                        break;
                    case "select_type":
                        builder.selectType(value);
                        break;
                    case "type":
                        builder.type(value);
                        break;
                    case "table":
                        builder.table(value);
                        break;
                    case "partitions":
                        builder.partitions(value);
                        break;
                    case "possible_keys":
                        builder.possibleKeys(value);
                        break;
                    case "key":
                        builder.key(value);
                        break;
                    case "key_len":
                        builder.keyLen(value);
                        break;
                    case "ref":
                        builder.ref(value);
                        break;
                    case "rows":
                        try {
                            builder.rows(Long.parseLong(value));
                        } catch (NumberFormatException ignored) {
                            builder.rows(0L);
                        }
                        break;
                    case "extra":
                        builder.extra(value);
                        break;
                    default:
                        // 其他字段存入 metadata
                        break;
                }
            }

            PlanNode node = builder.build();
            node.setExplanation(generateExplanation(node));
            nodes.add(node);
        }

        metadata.put("database", DATABASE_NAME);
        metadata.put("format", "traditional");

        return new ResultSetParseResult(nodes, metadata);
    }

    @Override
    public boolean testConnection(Connection conn) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            return rs.next();
        } catch (SQLException e) {
            log.warn("MySQL connection test failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getDatabaseName() {
        return DATABASE_NAME;
    }

    /**
     * 生成执行计划节点的友好说明
     */
    private String generateExplanation(PlanNode node) {
        StringBuilder explanation = new StringBuilder();

        // 访问类型说明
        explanation.append("访问类型: ").append(node.getType());
        if ("ALL".equals(node.getType())) {
            explanation.append(" (全表扫描)");
        } else if ("index".equals(node.getType())) {
            explanation.append(" (索引扫描)");
        } else if ("range".equals(node.getType())) {
            explanation.append(" (范围扫描)");
        } else if ("ref".equals(node.getType()) || "eq_ref".equals(node.getType())) {
            explanation.append(" (索引查找)");
        }

        // 索引使用说明
        if (node.getKey() != null && !node.getKey().isEmpty()) {
            explanation.append(", 使用索引: ").append(node.getKey());
        } else if (node.getPossibleKeys() != null && !node.getPossibleKeys().isEmpty()) {
            explanation.append(", 可用索引但未使用: ").append(node.getPossibleKeys());
        } else {
            explanation.append(", 无索引可用");
        }

        // 扫描行数说明
        if (node.getRows() != null && node.getRows() > 0) {
            explanation.append(", 预计扫描行数: ").append(node.getRows());
        }

        // 额外信息说明
        if (node.getExtra() != null && !node.getExtra().isEmpty()) {
            explanation.append(", ").append(node.getExtra());
        }

        return explanation.toString();
    }
}
