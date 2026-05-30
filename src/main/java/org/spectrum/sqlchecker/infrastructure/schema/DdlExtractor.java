package org.spectrum.sqlchecker.infrastructure.schema;

import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.application.schema.dto.TableDefinition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DDL 提取器
 * <p>
 * 从 SQL 文件内容中提取 CREATE TABLE 语句
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Slf4j
@Component
public class DdlExtractor {

    /**
     * 匹配 CREATE TABLE 语句
     * 支持：CREATE TABLE、CREATE TABLE IF NOT EXISTS
     * 支持：带反引号或不带的表名
     * 支持：带 schema 前缀的表名
     */
    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile(
            "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?" +
                    "(?:`?([\\w]+)`?\\.)?`?([\\w]+)`?\\s*\\(" +
                    "([^;]+?)\\)\\s*(?:ENGINE\\s*=\\s*\\w+)?\\s*(?:DEFAULT\\s+CHARSET\\s*=\\s*\\w+)?\\s*(?:COLLATE\\s*=\\s*\\w+)?\\s*(?:COMMENT\\s*=\\s*'[^']*')?\\s*;?",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    /**
     * 简化的 CREATE TABLE 匹配（更宽松，用于提取整个语句）
     */
    private static final Pattern CREATE_TABLE_SIMPLE_PATTERN = Pattern.compile(
            "(CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?[^;]+;?)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    /**
     * 从内容中提取 CREATE TABLE 语句
     *
     * @param content    SQL 文件内容
     * @param sourceFile 来源文件路径
     * @return 表定义列表
     */
    public List<TableDefinition> extractCreateTables(String content, String sourceFile) {
        List<TableDefinition> tables = new ArrayList<>();

        if (content == null || content.isBlank()) {
            return tables;
        }

        // 移除 SQL 注释
        String cleanContent = removeComments(content);

        Matcher matcher = CREATE_TABLE_SIMPLE_PATTERN.matcher(cleanContent);

        while (matcher.find()) {
            String ddl = matcher.group(1).trim();
            String tableName = extractTableName(ddl);

            if (tableName != null && !tableName.isEmpty()) {
                // 确保使用 IF NOT EXISTS
                String safeDdl = ensureIfNotExists(ddl);
                List<String> columns = extractColumns(ddl);
                List<String> primaryKeyColumns = extractPrimaryKeyColumns(ddl);
                List<String> indexedColumns = extractIndexedColumns(ddl, primaryKeyColumns);

                tables.add(TableDefinition.builder()
                        .tableName(tableName)
                        .originalDdl(safeDdl)
                        .columns(columns)
                        .primaryKeyColumns(primaryKeyColumns)
                        .indexedColumns(indexedColumns)
                        .inferred(false)
                        .sourceFile(sourceFile)
                        .build());

                log.debug("Extracted CREATE TABLE: {} from {}", tableName, sourceFile);
            }
        }

        return tables;
    }

    private List<String> extractColumns(String ddl) {
        Set<String> columns = new LinkedHashSet<>();
        for (String part : splitTopLevelDefinitions(extractDefinitionBody(ddl))) {
            String definition = part.trim();
            if (definition.isBlank() || isTableConstraint(definition)) {
                continue;
            }
            Matcher matcher = Pattern.compile("^`?([\\w]+)`?\\s+.+", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
                    .matcher(definition);
            if (matcher.find()) {
                columns.add(matcher.group(1));
            }
        }
        return new ArrayList<>(columns);
    }

    private List<String> extractPrimaryKeyColumns(String ddl) {
        Matcher matcher = Pattern.compile("PRIMARY\\s+KEY\\s*\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE)
                .matcher(ddl);
        if (!matcher.find()) {
            return List.of();
        }
        return parseColumnList(matcher.group(1));
    }

    private List<String> extractIndexedColumns(String ddl, List<String> primaryKeyColumns) {
        Set<String> indexedColumns = new LinkedHashSet<>(primaryKeyColumns != null ? primaryKeyColumns : List.of());
        Matcher inlineIndexMatcher = Pattern.compile(
                        "(?:UNIQUE\\s+|FULLTEXT\\s+|SPATIAL\\s+)?(?:KEY|INDEX)\\s+`?[\\w]+`?\\s*\\(([^)]+)\\)",
                        Pattern.CASE_INSENSITIVE)
                .matcher(ddl);
        while (inlineIndexMatcher.find()) {
            indexedColumns.addAll(parseColumnList(inlineIndexMatcher.group(1)));
        }
        return new ArrayList<>(indexedColumns);
    }

    private String extractDefinitionBody(String ddl) {
        if (ddl == null) {
            return "";
        }
        int start = ddl.indexOf('(');
        int end = ddl.lastIndexOf(')');
        if (start < 0 || end <= start) {
            return "";
        }
        return ddl.substring(start + 1, end);
    }

    private List<String> splitTopLevelDefinitions(String body) {
        List<String> definitions = new ArrayList<>();
        if (body == null || body.isBlank()) {
            return definitions;
        }
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < body.length(); i++) {
            char ch = body.charAt(i);
            if (ch == '(') {
                depth++;
            } else if (ch == ')' && depth > 0) {
                depth--;
            }
            if (ch == ',' && depth == 0) {
                definitions.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        if (current.length() > 0) {
            definitions.add(current.toString());
        }
        return definitions;
    }

    private boolean isTableConstraint(String definition) {
        String upper = definition.trim().toUpperCase(Locale.ROOT);
        return upper.startsWith("PRIMARY KEY")
                || upper.startsWith("UNIQUE KEY")
                || upper.startsWith("UNIQUE INDEX")
                || upper.startsWith("KEY ")
                || upper.startsWith("INDEX ")
                || upper.startsWith("FULLTEXT KEY")
                || upper.startsWith("FULLTEXT INDEX")
                || upper.startsWith("SPATIAL KEY")
                || upper.startsWith("SPATIAL INDEX")
                || upper.startsWith("CONSTRAINT ")
                || upper.startsWith("FOREIGN KEY")
                || upper.startsWith("CHECK ");
    }

    private List<String> parseColumnList(String rawColumns) {
        if (rawColumns == null || rawColumns.isBlank()) {
            return List.of();
        }
        Set<String> columns = new LinkedHashSet<>();
        for (String part : rawColumns.split(",")) {
            String column = part.trim()
                    .replaceAll("`", "")
                    .replaceAll("\"", "")
                    .replaceAll("'","")
                    .replaceAll("\\s+(ASC|DESC)\\b.*", "")
                    .replaceAll("\\(.+$", "")
                    .trim();
            if (!column.isBlank()) {
                columns.add(column);
            }
        }
        return new ArrayList<>(columns);
    }

    /**
     * 从 DDL 语句中提取表名
     */
    private String extractTableName(String ddl) {
        // 匹配表名：CREATE TABLE [IF NOT EXISTS] [`schema`.]`table_name`
        Pattern tableNamePattern = Pattern.compile(
                "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(?:`?\\w+`?\\.)?`?([\\w]+)`?",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = tableNamePattern.matcher(ddl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 确保 DDL 使用 IF NOT EXISTS
     */
    private String ensureIfNotExists(String ddl) {
        if (ddl.toUpperCase().contains("IF NOT EXISTS")) {
            return ddl;
        }
        return ddl.replaceFirst(
                "(?i)CREATE\\s+TABLE\\s+",
                "CREATE TABLE IF NOT EXISTS "
        );
    }

    /**
     * 移除 SQL 注释
     */
    private String removeComments(String content) {
        // 移除单行注释 (-- 和 #)
        String result = content.replaceAll("--[^\r\n]*", "");
        result = result.replaceAll("#[^\r\n]*", "");

        // 移除多行注释 (/* */)
        result = result.replaceAll("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/", "");

        return result;
    }

    /**
     * 检查内容是否包含 CREATE TABLE 语句
     */
    public boolean containsCreateTable(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        return content.toUpperCase().contains("CREATE TABLE");
    }
}
