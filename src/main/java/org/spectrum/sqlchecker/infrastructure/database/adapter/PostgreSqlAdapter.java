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
 * PostgreSQL 数据库适配器
 * <p>
 * 支持 PostgreSQL 9.1+ 的 EXPLAIN (ANALYZE, BUFFERS, VERBOSE) 命令
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Slf4j
@Component
public class PostgreSqlAdapter implements DatabaseAdapter {

    private static final String DATABASE_NAME = "PostgreSQL";

    @Override
    public String buildExplainSql(String sql) {
        String trimmed = sql.trim();
        // 如果已经以 EXPLAIN 开头，直接返回
        if (trimmed.toUpperCase().startsWith("EXPLAIN")) {
            return trimmed;
        }
        // 使用 EXPLAIN ANALYZE 获取实际执行时间
        return "EXPLAIN (ANALYZE, BUFFERS) " + trimmed;
    }

    @Override
    public ResultSetParseResult parseExplainResult(ResultSet rs) throws SQLException {
        List<PlanNode> nodes = new ArrayList<>();
        Map<String, Object> metadata = new HashMap<>();

        // PostgreSQL EXPLAIN 返回的是文本格式，需要解析
        // 这里使用简化实现，假设结果有 QUERY PLAN 列
        while (rs.next()) {
            String planText = rs.getString("QUERY PLAN");
            if (planText != null) {
                PlanNode node = parsePostgreSQLPlanText(planText);
                if (node != null) {
                    nodes.add(node);
                }
            }
        }

        metadata.put("database", DATABASE_NAME);
        metadata.put("format", "text");

        return new ResultSetParseResult(nodes, metadata);
    }

    /**
     * 解析 PostgreSQL 的 EXPLAIN 文本输出
     * <p>
     * 示例: "Seq Scan on users  (cost=0.00..35.50 rows=2550 width=4)"
     */
    private PlanNode parsePostgreSQLPlanText(String planText) {
        if (planText == null || planText.isEmpty()) {
            return null;
        }

        PlanNode.PlanNodeBuilder builder = PlanNode.builder();

        // 解析节点类型 (Seq Scan, Index Scan, Nested Loop, etc.)
        String lowerText = planText.toLowerCase();

        if (lowerText.contains("seq scan on")) {
            String table = extractAfter(planText, "on", "(");
            builder.type("ALL").table(table);
        } else if (lowerText.contains("index scan using")) {
            String index = extractAfter(planText, "using", "(");
            String table = extractAfter(planText, "on", "(");
            builder.type("index").table(table).key(index);
        } else if (lowerText.contains("index only scan")) {
            String index = extractAfter(planText, "using", "(");
            builder.type("index").table(index).key(index);
        } else if (lowerText.contains("nested loop")) {
            builder.type("Nested Loop");
        } else if (lowerText.contains("hash join")) {
            builder.type("Hash Join");
        } else if (lowerText.contains("merge join")) {
            builder.type("Merge Join");
        } else {
            builder.type("unknown");
        }

        // 解析 rows
        if (planText.contains("rows=")) {
            String rowsPart = extractBetween(planText, "rows=", "width=");
            try {
                builder.rows(Long.parseLong(rowsPart.trim()));
            } catch (NumberFormatException ignored) {
            }
        }

        PlanNode node = builder.build();
        node.setExplanation(planText);
        return node;
    }

    private String extractAfter(String text, String after, String until) {
        int afterIndex = text.indexOf(after);
        if (afterIndex == -1) {
            return "";
        }
        int start = afterIndex + after.length();
        int untilIndex = text.indexOf(until, start);
        if (untilIndex == -1) {
            untilIndex = text.length();
        }
        return text.substring(start, untilIndex).trim();
    }

    private String extractBetween(String text, String start, String end) {
        int startIndex = text.indexOf(start);
        if (startIndex == -1) {
            return "";
        }
        int valueStart = startIndex + start.length();
        int endIndex = text.indexOf(end, valueStart);
        if (endIndex == -1) {
            return "";
        }
        return text.substring(valueStart, endIndex).trim();
    }

    @Override
    public boolean testConnection(Connection conn) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            return rs.next();
        } catch (SQLException e) {
            log.warn("PostgreSQL connection test failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getDatabaseName() {
        return DATABASE_NAME;
    }
}
