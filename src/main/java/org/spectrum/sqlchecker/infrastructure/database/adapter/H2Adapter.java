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
 * H2 数据库适配器
 * <p>
 * 支持 H2 2.x 的 EXPLAIN 命令
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Slf4j
@Component
public class H2Adapter implements DatabaseAdapter {

    private static final String DATABASE_NAME = "H2";

    @Override
    public String buildExplainSql(String sql) {
        String trimmed = sql.trim();
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

                switch (columnName) {
                    case "id":
                        try {
                            builder.id(Integer.parseInt(value));
                        } catch (NumberFormatException ignored) {
                            builder.id(null);
                        }
                        break;
                    case "type":
                        builder.selectType(value);
                        builder.type(value);
                        break;
                    case "tableName":
                        builder.table(value);
                        break;
                    default:
                        break;
                }
            }

            PlanNode node = builder.build();
            node.setExplanation(generateExplanation(node));
            nodes.add(node);
        }

        metadata.put("database", DATABASE_NAME);
        return new ResultSetParseResult(nodes, metadata);
    }

    @Override
    public boolean testConnection(Connection conn) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            return rs.next();
        } catch (SQLException e) {
            log.warn("H2 connection test failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getDatabaseName() {
        return DATABASE_NAME;
    }

    private String generateExplanation(PlanNode node) {
        StringBuilder explanation = new StringBuilder();
        explanation.append("操作类型: ").append(node.getType());
        if (node.getTable() != null) {
            explanation.append(", 表: ").append(node.getTable());
        }
        return explanation.toString();
    }
}
