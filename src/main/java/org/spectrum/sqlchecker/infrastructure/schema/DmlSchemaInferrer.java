package org.spectrum.sqlchecker.infrastructure.schema;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import org.spectrum.sqlchecker.application.schema.dto.TableDefinition;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * DML Schema 推断器
 * <p>
 * 从 DML 语句（SELECT/INSERT/UPDATE/DELETE）中推断表结构
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Slf4j
@Component
public class DmlSchemaInferrer {

    /**
     * 从 DML 语句列表推断表结构
     *
     * @param dmlStatements DML 语句列表
     * @return 表定义列表
     */
    public List<TableDefinition> inferFromDml(List<String> dmlStatements) {
        Map<String, Set<String>> tableColumns = new LinkedHashMap<>();

        for (String sql : dmlStatements) {
            try {
                // 预处理 SQL，替换 MyBatis 占位符
                String processedSql = preprocessSql(sql);
                Statement stmt = CCJSqlParserUtil.parse(processedSql);
                extractTableAndColumns(stmt, tableColumns);
            } catch (JSQLParserException e) {
                log.debug("Failed to parse SQL for schema inference: {}",
                        sql.substring(0, Math.min(100, sql.length())));
                fallbackExtractFromSql(sql, tableColumns);
            }
        }

        return buildTableDefinitions(tableColumns);
    }

    /**
     * 预处理 SQL，替换 MyBatis 占位符为有效值
     */
    private String preprocessSql(String sql) {
        // 替换 #{xxx} 为 'placeholder'
        String processed = sql.replaceAll("#\\{[^}]+\\}", "'placeholder'");
        // 替换 ${xxx} 为 placeholder
        processed = processed.replaceAll("\\$\\{[^}]+\\}", "placeholder");
        // 替换 INCLUDE 注释为 *
        processed = processed.replaceAll("/\\*\\s*INCLUDE:[^*]+\\*/", "*");
        // 移除 BIND 注释
        processed = processed.replaceAll("/\\*\\s*BIND:[^*]+\\*/", "");
        // 修正常见动态 SQL 片段导致的语法问题
        processed = processed.replaceAll("(?i)\\bFROM\\s+([`\\w.]+)\\s+AND\\b", "FROM $1 WHERE ");
        processed = processed.replaceAll("(?i)\\bSET\\s*,\\s*", "SET ");
        processed = processed.replaceAll(",\\s*,", ",");
        processed = processed.replaceAll("(?i),\\s*(WHERE|AND|OR|GROUP\\s+BY|ORDER\\s+BY|LIMIT)", " $1");
        processed = processed.replaceAll("(?i)\\bCASE\\s+([`\\w\\.]+)\\s+([^\\s]+)\\s+END", "CASE WHEN $1 THEN $2 END");
        processed = processed.replaceAll("(?i)\\bCASE\\s+([`\\w\\.]+)\\s+END", "CASE WHEN $1 THEN 1 END");
        return processed;
    }

    /**
     * 从 Statement 中提取表名和列名
     */
    private void extractTableAndColumns(Statement stmt, Map<String, Set<String>> tableColumns) {
        if (stmt instanceof Select select) {
            extractFromSelect(select, tableColumns);
        } else if (stmt instanceof Insert insert) {
            extractFromInsert(insert, tableColumns);
        } else if (stmt instanceof Update update) {
            extractFromUpdate(update, tableColumns);
        } else if (stmt instanceof Delete delete) {
            extractFromDelete(delete, tableColumns);
        }
    }

    /**
     * 从 SELECT 语句中提取表名和列名
     */
    private void extractFromSelect(Select select, Map<String, Set<String>> tableColumns) {
        if (select.getPlainSelect() != null) {
            PlainSelect plainSelect = select.getPlainSelect();
            Map<String, String> aliasMap = new HashMap<>();
            String baseTableName = null;

            // 提取 FROM 子句中的表名
            if (plainSelect.getFromItem() != null) {
                baseTableName = extractTableNameFromItem(plainSelect.getFromItem(), aliasMap);
                if (baseTableName != null) {
                    tableColumns.computeIfAbsent(baseTableName, k -> new LinkedHashSet<>());

                    // 提取 SELECT 列表中的列名
                    if (plainSelect.getSelectItems() != null) {
                        for (SelectItem<?> item : plainSelect.getSelectItems()) {
                            extractColumnsFromSelectItem(item, baseTableName, tableColumns);
                            if (item.getExpression() != null) {
                                extractColumnsFromExpression(item.getExpression(), baseTableName, tableColumns, aliasMap);
                            }
                        }
                    }

                    // 提取 WHERE 子句中的列名
                    if (plainSelect.getWhere() != null) {
                        extractColumnsFromExpression(plainSelect.getWhere(), baseTableName, tableColumns, aliasMap);
                    }
                }
            }

            // 提取 JOIN 中的表名
            if (plainSelect.getJoins() != null) {
                for (Join join : plainSelect.getJoins()) {
                    String joinTable = extractTableNameFromItem(join.getRightItem(), aliasMap);
                    if (joinTable != null) {
                        tableColumns.computeIfAbsent(joinTable, k -> new LinkedHashSet<>());
                    }
                    if (join.getOnExpressions() != null && !join.getOnExpressions().isEmpty()) {
                        for (Expression onExpression : join.getOnExpressions()) {
                            extractColumnsFromExpression(onExpression, baseTableName, tableColumns, aliasMap);
                        }
                    }
                    if (join.getUsingColumns() != null && !join.getUsingColumns().isEmpty()) {
                        for (Column column : join.getUsingColumns()) {
                            if (baseTableName != null) {
                                tableColumns.computeIfAbsent(baseTableName, k -> new LinkedHashSet<>())
                                        .add(column.getColumnName());
                            }
                            if (joinTable != null) {
                                tableColumns.computeIfAbsent(joinTable, k -> new LinkedHashSet<>())
                                        .add(column.getColumnName());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 从 INSERT 语句中提取表名和列名
     */
    private void extractFromInsert(Insert insert, Map<String, Set<String>> tableColumns) {
        String tableName = insert.getTable().getName();
        tableColumns.computeIfAbsent(tableName, k -> new LinkedHashSet<>());

        if (insert.getColumns() != null) {
            for (Column col : insert.getColumns()) {
                tableColumns.get(tableName).add(col.getColumnName());
            }
        }
    }

    /**
     * 从 UPDATE 语句中提取表名和列名
     */
    private void extractFromUpdate(Update update, Map<String, Set<String>> tableColumns) {
        String tableName = update.getTable().getName();
        tableColumns.computeIfAbsent(tableName, k -> new LinkedHashSet<>());

        // 提取 SET 子句中的列名
        if (update.getUpdateSets() != null) {
            for (UpdateSet updateSet : update.getUpdateSets()) {
                if (updateSet.getColumns() != null) {
                    for (Column col : updateSet.getColumns()) {
                        tableColumns.get(tableName).add(col.getColumnName());
                    }
                }
                if (updateSet.getValues() != null && updateSet.getValues().getExpressions() != null) {
                    for (Expression expr : updateSet.getValues().getExpressions()) {
                        extractColumnsFromExpression(expr, tableName, tableColumns, Map.of());
                    }
                }
            }
        }

        // 提取 WHERE 子句中的列名
        if (update.getWhere() != null) {
            extractColumnsFromExpression(update.getWhere(), tableName, tableColumns, Map.of());
        }
    }

    /**
     * 从 DELETE 语句中提取表名
     */
    private void extractFromDelete(Delete delete, Map<String, Set<String>> tableColumns) {
        String tableName = delete.getTable().getName();
        tableColumns.computeIfAbsent(tableName, k -> new LinkedHashSet<>());

        // 提取 WHERE 子句中的列名
        if (delete.getWhere() != null) {
            extractColumnsFromExpression(delete.getWhere(), tableName, tableColumns, Map.of());
        }
    }

    /**
     * 从 FromItem 中提取表名
     */
    private String extractTableNameFromItem(FromItem fromItem, Map<String, String> aliasMap) {
        if (fromItem instanceof Table table) {
            String name = table.getName();
            if (table.getAlias() != null && table.getAlias().getName() != null) {
                aliasMap.put(table.getAlias().getName(), name);
            }
            return name;
        }
        return null;
    }

    /**
     * 从 SelectItem 中提取列名
     */
    private void extractColumnsFromSelectItem(SelectItem<?> item, String tableName,
                                              Map<String, Set<String>> tableColumns) {
        if (item.getExpression() instanceof Column col) {
            tableColumns.get(tableName).add(col.getColumnName());
        }
    }

    /**
     * 从表达式中提取列名（简化实现）
     */
    private void extractColumnsFromExpression(net.sf.jsqlparser.expression.Expression expr,
                                              String tableName, Map<String, Set<String>> tableColumns) {
        extractColumnsFromExpression(expr, tableName, tableColumns, Map.of());
    }

    private void extractColumnsFromExpression(Expression expr,
                                              String defaultTableName,
                                              Map<String, Set<String>> tableColumns,
                                              Map<String, String> aliasMap) {
        if (expr == null) {
            return;
        }

        expr.accept(new ExpressionVisitorAdapter() {
            @Override
            public void visit(Column column) {
                String tableName = defaultTableName;
                if (column.getTable() != null && column.getTable().getName() != null
                        && !column.getTable().getName().isBlank()) {
                    String candidate = column.getTable().getName();
                    tableName = aliasMap.getOrDefault(candidate, candidate);
                }
                if (tableName == null || tableName.isBlank()) {
                    return;
                }
                String columnName = column.getColumnName();
                if (columnName == null || columnName.isBlank() || "*".equals(columnName)) {
                    return;
                }
                tableColumns.computeIfAbsent(tableName, k -> new LinkedHashSet<>()).add(columnName);
            }
        });
    }

    /**
     * 从收集的表和列信息构建表定义
     */
    private List<TableDefinition> buildTableDefinitions(Map<String, Set<String>> tableColumns) {
        List<TableDefinition> tables = new ArrayList<>();

        for (Map.Entry<String, Set<String>> entry : tableColumns.entrySet()) {
            String tableName = entry.getKey();
            Set<String> columns = entry.getValue();

            // 生成 CREATE TABLE IF NOT EXISTS 语句
            String ddl = generateCreateTableDdl(tableName, columns);

            tables.add(TableDefinition.builder()
                    .tableName(tableName)
                    .originalDdl(ddl)
                    .inferred(true)
                    .sourceFile("INFERRED_FROM_DML")
                    .build());

            log.debug("Inferred table: {} with {} columns", tableName, columns.size());
        }

        return tables;
    }

    /**
     * 生成 CREATE TABLE DDL
     */
    private String generateCreateTableDdl(String tableName, Set<String> columns) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS `").append(tableName).append("` (\n");
        sb.append("  `id` BIGINT AUTO_INCREMENT PRIMARY KEY");

        for (String col : columns) {
            // 跳过 id 列（已经添加）
            if (!"id".equalsIgnoreCase(col)) {
                sb.append(",\n  `").append(col).append("` VARCHAR(255)");
            }
        }

        sb.append("\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        return sb.toString();
    }

    /**
     * 解析失败时使用简单正则提取表/列
     */
    private void fallbackExtractFromSql(String sql, Map<String, Set<String>> tableColumns) {
        if (sql == null || sql.isBlank()) {
            return;
        }
        String normalized = sql.replaceAll("\\s+", " ").trim();
        String upper = normalized.toUpperCase(Locale.ROOT);
        String tableName = extractTableNameByRegex(upper, normalized);
        if (tableName == null || tableName.isBlank()) {
            return;
        }
        Set<String> columns = tableColumns.computeIfAbsent(tableName, k -> new LinkedHashSet<>());

        if (upper.startsWith("INSERT")) {
            extractInsertColumns(normalized, columns);
        } else if (upper.startsWith("UPDATE")) {
            extractUpdateColumns(normalized, columns);
        } else if (upper.startsWith("DELETE")) {
            extractPredicateColumns(normalized, columns);
        } else if (upper.startsWith("SELECT")) {
            extractSelectColumns(normalized, columns);
            extractPredicateColumns(normalized, columns);
        }
    }

    private String extractTableNameByRegex(String upper, String normalized) {
        java.util.regex.Matcher matcher;
        if (upper.startsWith("SELECT") || upper.startsWith("DELETE")) {
            matcher = java.util.regex.Pattern.compile("(?i)\\bFROM\\s+([`\\w.]+)").matcher(normalized);
            if (matcher.find()) {
                return stripIdentifier(matcher.group(1));
            }
        }
        if (upper.startsWith("UPDATE")) {
            matcher = java.util.regex.Pattern.compile("(?i)^\\s*UPDATE\\s+([`\\w.]+)").matcher(normalized);
            if (matcher.find()) {
                return stripIdentifier(matcher.group(1));
            }
        }
        if (upper.startsWith("INSERT")) {
            matcher = java.util.regex.Pattern.compile("(?i)\\bINTO\\s+([`\\w.]+)").matcher(normalized);
            if (matcher.find()) {
                return stripIdentifier(matcher.group(1));
            }
        }
        return null;
    }

    private void extractInsertColumns(String sql, Set<String> columns) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?i)\\bINTO\\s+[`\\w.]+\\s*\\(([^)]+)\\)")
                .matcher(sql);
        if (matcher.find()) {
            String list = matcher.group(1);
            for (String part : list.split(",")) {
                String col = stripIdentifier(part);
                if (isValidColumnName(col)) {
                    columns.add(col);
                }
            }
        }
    }

    private void extractUpdateColumns(String sql, Set<String> columns) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?i)\\bSET\\s+(.+?)(\\bWHERE\\b|$)")
                .matcher(sql);
        if (matcher.find()) {
            String assignments = matcher.group(1);
            java.util.regex.Matcher assignMatcher = java.util.regex.Pattern
                    .compile("(?i)\\b([`\\w]+)\\b\\s*=")
                    .matcher(assignments);
            while (assignMatcher.find()) {
                String col = stripIdentifier(assignMatcher.group(1));
                if (isValidColumnName(col)) {
                    columns.add(col);
                }
            }
        }
        extractPredicateColumns(sql, columns);
    }

    private void extractSelectColumns(String sql, Set<String> columns) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?i)\\bSELECT\\s+(.+?)\\bFROM\\b")
                .matcher(sql);
        if (matcher.find()) {
            String selectList = matcher.group(1);
            for (String part : selectList.split(",")) {
                String col = stripSelectItem(part);
                if (isValidColumnName(col)) {
                    columns.add(col);
                }
            }
        }
    }

    private void extractPredicateColumns(String sql, Set<String> columns) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?i)\\b([`\\w]+)\\b\\s*(=|<|>|<=|>=|!=|<>|\\bIN\\b|\\bLIKE\\b|\\bIS\\b|\\bBETWEEN\\b)")
                .matcher(sql);
        while (matcher.find()) {
            String col = stripIdentifier(matcher.group(1));
            if (isValidColumnName(col)) {
                columns.add(col);
            }
        }
    }

    private String stripSelectItem(String part) {
        String cleaned = part.trim();
        cleaned = cleaned.replaceAll("(?i)^DISTINCT\\s+", "");
        if (cleaned.contains(".")) {
            cleaned = cleaned.substring(cleaned.lastIndexOf('.') + 1);
        }
        cleaned = cleaned.replaceAll("(?i)\\s+AS\\s+.*$", "");
        cleaned = cleaned.replaceAll("\\s+.*$", "");
        return stripIdentifier(cleaned);
    }

    private String stripIdentifier(String token) {
        if (token == null) {
            return "";
        }
        return token.replaceAll("[`\"']", "").trim();
    }

    private boolean isValidColumnName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String upper = name.toUpperCase(Locale.ROOT);
        return !Set.of("SELECT", "FROM", "WHERE", "AND", "OR", "IN", "LIKE", "IS", "BETWEEN",
                "CASE", "WHEN", "THEN", "END", "AS", "SET", "VALUES", "UPDATE", "DELETE",
                "INSERT", "INTO", "ORDER", "GROUP", "BY", "LIMIT").contains(upper)
                && !"*".equals(name);
    }
}
