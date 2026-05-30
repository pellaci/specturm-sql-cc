package org.spectrum.sqlchecker.infrastructure.schema;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.application.scan.dto.SchemaAnalysisDto;
import org.spectrum.sqlchecker.application.scan.dto.SqlLocationDto;
import org.spectrum.sqlchecker.application.scan.dto.SqlStatementDto;
import org.spectrum.sqlchecker.application.schema.dto.TableDefinition;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Correlates scanned SQL with project DDL without requiring a live database.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaRiskAnalyzer {

    private static final Pattern CREATE_INDEX_PATTERN = Pattern.compile(
            "CREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+`?[\\w]+`?\\s+ON\\s+(?:`?[\\w]+`?\\.)?`?([\\w]+)`?\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final DdlExtractor ddlExtractor;

    public SchemaAnalysisDto analyze(Path scanPath, List<SqlStatementDto> sqlStatements) {
        List<SqlStatementDto> statements = sqlStatements != null ? sqlStatements : List.of();
        Map<String, TableInfo> ddlTables = loadDdlTables(scanPath);
        Map<String, TableUsage> usages = collectTableUsage(statements);
        List<SchemaAnalysisDto.SqlSchemaRisk> risks = buildRisks(ddlTables, usages);
        List<SchemaAnalysisDto.TableSummary> tables = buildTableSummaries(ddlTables, usages);
        int covered = 0;
        for (String table : usages.keySet()) {
            if (ddlTables.containsKey(table)) {
                covered++;
            }
        }
        int unindexedPredicateCount = 0;
        for (SchemaAnalysisDto.SqlSchemaRisk risk : risks) {
            if ("UNINDEXED_PREDICATE".equals(risk.getRiskType())) {
                unindexedPredicateCount++;
            }
        }
        List<String> warnings = new ArrayList<>();
        if (scanPath == null || !Files.exists(scanPath)) {
            warnings.add("DDL 证据路径不存在，无法进行表结构关联分析: " + schemaPathLabel(scanPath));
        } else if (ddlTables.isEmpty()) {
            warnings.add("未在扫描范围内发现 CREATE TABLE DDL，无法进行表结构关联分析。证据路径: " + schemaPathLabel(scanPath));
        } else if (covered < usages.size()) {
            warnings.add("部分 SQL 引用的表未在项目 DDL 中出现，需确认 DDL 是否完整或由迁移系统托管。");
        }

        return SchemaAnalysisDto.builder()
                .schemaPath(schemaPathLabel(scanPath))
                .ddlDetected(!ddlTables.isEmpty())
                .ddlFileCount(countDdlFiles(ddlTables))
                .tableCount(ddlTables.size())
                .referencedTableCount(usages.size())
                .coveredTableCount(covered)
                .missingDdlTableCount(Math.max(0, usages.size() - covered))
                .unindexedPredicateCount(unindexedPredicateCount)
                .tables(tables)
                .risks(risks)
                .warnings(warnings)
                .build();
    }

    private Map<String, TableInfo> loadDdlTables(Path scanPath) {
        Map<String, TableInfo> tables = new LinkedHashMap<>();
        if (scanPath == null || !Files.exists(scanPath)) {
            return tables;
        }
        try (Stream<Path> paths = Files.walk(scanPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isSupportedDdlCarrier)
                    .filter(path -> !isIgnoredScanPath(path))
                    .forEach(path -> readDdlFile(scanPath, path, tables));
        } catch (IOException e) {
            log.debug("Failed to inspect DDL under {}: {}", scanPath, e.getMessage());
        }
        return tables;
    }

    private String schemaPathLabel(Path scanPath) {
        if (scanPath == null) {
            return "";
        }
        return scanPath.toAbsolutePath().normalize().toString();
    }

    private void readDdlFile(Path root, Path path, Map<String, TableInfo> tables) {
        try {
            String content = Files.readString(path);
            if (!ddlExtractor.containsCreateTable(content) && !containsCreateIndex(content)) {
                return;
            }
            String source = relativize(root, path);
            for (TableDefinition table : ddlExtractor.extractCreateTables(content, source)) {
                String key = normalizeIdentifier(table.getTableName());
                if (key.isBlank()) {
                    continue;
                }
                tables.putIfAbsent(key, new TableInfo(
                        table.getTableName(),
                        source,
                        orderedSet(table.getColumns()),
                        orderedSet(table.getPrimaryKeyColumns()),
                        orderedSet(table.getIndexedColumns())));
            }
            mergeCreateIndexes(content, tables);
        } catch (IOException e) {
            log.debug("Failed to read DDL carrier {}: {}", path, e.getMessage());
        }
    }

    private void mergeCreateIndexes(String content, Map<String, TableInfo> tables) {
        Matcher matcher = CREATE_INDEX_PATTERN.matcher(content);
        while (matcher.find()) {
            String key = normalizeIdentifier(matcher.group(1));
            TableInfo table = tables.get(key);
            if (table != null) {
                table.indexedColumns.addAll(parseColumnList(matcher.group(2)));
            }
        }
    }

    private Map<String, TableUsage> collectTableUsage(List<SqlStatementDto> sqlStatements) {
        Map<String, TableUsage> usages = new LinkedHashMap<>();
        for (SqlStatementDto sql : sqlStatements) {
            if (sql == null || sql.getSqlType() == SqlType.CREATE || sql.getSqlType() == SqlType.ALTER || sql.getSqlType() == SqlType.DROP) {
                continue;
            }
            String sqlText = chooseSqlText(sql);
            if (sqlText.isBlank()) {
                continue;
            }
            List<String> tableNames = extractReferencedTables(sqlText);
            if (tableNames.isEmpty()) {
                continue;
            }
            Set<String> predicates = extractPredicateColumns(sqlText);
            for (String tableName : tableNames) {
                String key = normalizeIdentifier(tableName);
                if (key.isBlank()) {
                    continue;
                }
                TableUsage usage = usages.computeIfAbsent(key, ignored -> new TableUsage(tableName));
                usage.sqlCount++;
                usage.examples.add(sql);
                usage.predicateColumns.addAll(predicates);
            }
        }
        return usages;
    }

    private List<SchemaAnalysisDto.SqlSchemaRisk> buildRisks(Map<String, TableInfo> ddlTables, Map<String, TableUsage> usages) {
        List<SchemaAnalysisDto.SqlSchemaRisk> risks = new ArrayList<>();
        if (usages.isEmpty() || ddlTables.isEmpty()) {
            return risks;
        }
        for (Map.Entry<String, TableUsage> entry : usages.entrySet()) {
            TableInfo table = ddlTables.get(entry.getKey());
            TableUsage usage = entry.getValue();
            if (table == null) {
                for (SqlStatementDto sql : usage.examples) {
                    risks.add(risk(sql, "SCHEMA_GAP", "INFO", usage.displayName, List.copyOf(usage.predicateColumns),
                            List.of(), List.of(), "SQL 引用的表未在扫描范围 DDL 中出现",
                            "确认 DDL 是否缺失、是否由迁移系统托管，或在扫描时补充 schema-path。"));
                }
                continue;
            }
            for (SqlStatementDto sql : usage.examples) {
                Set<String> predicateColumns = extractPredicateColumns(chooseSqlText(sql));
                if (predicateColumns.isEmpty()) {
                    continue;
                }
                Set<String> indexed = new LinkedHashSet<>();
                Set<String> missing = new LinkedHashSet<>();
                for (String column : predicateColumns) {
                    String normalized = normalizeIdentifier(column);
                    if (normalized.isBlank()) {
                        continue;
                    }
                    if (table.indexedColumnsLower().contains(normalized)) {
                        indexed.add(column);
                    } else {
                        missing.add(column);
                    }
                }
                if (!missing.isEmpty()) {
                    risks.add(risk(sql, "UNINDEXED_PREDICATE", "WARNING", table.tableName,
                            List.copyOf(predicateColumns), List.copyOf(indexed), List.copyOf(missing),
                            "过滤/排序/关联字段未在 DDL 索引列中命中: " + String.join(", ", missing),
                            "结合访问频次和表规模评估单列或联合索引，并用 EXPLAIN 验证访问路径。"));
                }
            }
        }
        return risks.stream()
                .sorted(Comparator.comparing(SchemaAnalysisDto.SqlSchemaRisk::getSeverity).reversed()
                        .thenComparing(SchemaAnalysisDto.SqlSchemaRisk::getTableName)
                        .thenComparing(SchemaAnalysisDto.SqlSchemaRisk::getSqlId))
                .toList();
    }

    private SchemaAnalysisDto.SqlSchemaRisk risk(
            SqlStatementDto sql,
            String riskType,
            String severity,
            String tableName,
            List<String> predicateColumns,
            List<String> indexedPredicateColumns,
            List<String> missingIndexColumns,
            String evidence,
            String recommendation) {
        return SchemaAnalysisDto.SqlSchemaRisk.builder()
                .sqlId(sql.getId())
                .riskType(riskType)
                .severity(severity)
                .tableName(tableName)
                .predicateColumns(predicateColumns)
                .indexedPredicateColumns(indexedPredicateColumns)
                .missingIndexColumns(missingIndexColumns)
                .locations(locationLabels(sql.getLocations()))
                .evidence(evidence)
                .recommendation(recommendation)
                .build();
    }

    private List<SchemaAnalysisDto.TableSummary> buildTableSummaries(
            Map<String, TableInfo> ddlTables,
            Map<String, TableUsage> usages) {
        List<SchemaAnalysisDto.TableSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, TableInfo> entry : ddlTables.entrySet()) {
            TableInfo table = entry.getValue();
            TableUsage usage = usages.get(entry.getKey());
            summaries.add(SchemaAnalysisDto.TableSummary.builder()
                    .tableName(table.tableName)
                    .sourceFile(table.sourceFile)
                    .columns(List.copyOf(table.columns))
                    .primaryKeyColumns(List.copyOf(table.primaryKeyColumns))
                    .indexedColumns(List.copyOf(table.indexedColumns))
                    .referencedSqlCount(usage != null ? usage.sqlCount : 0)
                    .coverage(usage != null ? "REFERENCED" : "DDL_ONLY")
                    .build());
        }
        for (Map.Entry<String, TableUsage> entry : usages.entrySet()) {
            if (ddlTables.containsKey(entry.getKey())) {
                continue;
            }
            TableUsage usage = entry.getValue();
            summaries.add(SchemaAnalysisDto.TableSummary.builder()
                    .tableName(usage.displayName)
                    .sourceFile("SQL_REFERENCE_ONLY")
                    .columns(List.of())
                    .primaryKeyColumns(List.of())
                    .indexedColumns(List.of())
                    .referencedSqlCount(usage.sqlCount)
                    .coverage("MISSING_DDL")
                    .build());
        }
        return summaries;
    }

    private int countDdlFiles(Map<String, TableInfo> tables) {
        return (int) tables.values().stream()
                .map(table -> table.sourceFile)
                .distinct()
                .count();
    }

    private String chooseSqlText(SqlStatementDto sql) {
        if (sql.getNormalizedSql() != null && !sql.getNormalizedSql().isBlank()) {
            return sql.getNormalizedSql();
        }
        if (sql.getOriginalSql() != null && !sql.getOriginalSql().isBlank()) {
            return sql.getOriginalSql();
        }
        return sql.getAbstractSql() != null ? sql.getAbstractSql() : "";
    }

    private List<String> extractReferencedTables(String sql) {
        Set<String> tables = new LinkedHashSet<>();
        collectMatches(tables, sql, "\\bFROM\\s+([`\\w.]+)");
        collectMatches(tables, sql, "\\bJOIN\\s+([`\\w.]+)");
        collectMatches(tables, sql, "^\\s*UPDATE\\s+([`\\w.]+)");
        collectMatches(tables, sql, "\\bINTO\\s+([`\\w.]+)");
        collectMatches(tables, sql, "\\bDELETE\\s+FROM\\s+([`\\w.]+)");
        return new ArrayList<>(tables);
    }

    private void collectMatches(Set<String> result, String sql, String pattern) {
        Matcher matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(sql);
        while (matcher.find()) {
            String table = stripQualifiedIdentifier(matcher.group(1));
            if (!table.isBlank()) {
                result.add(table);
            }
        }
    }

    private Set<String> extractPredicateColumns(String sql) {
        Set<String> columns = new LinkedHashSet<>();
        String scope = sql.replaceAll("[\\r\\n\\t]+", " ");
        Matcher matcher = Pattern.compile(
                        "(?:\\b[`\\w]+`?\\.)?`?([A-Za-z_][\\w]*)`?\\s*(=|<>|!=|<=|>=|<|>|\\bIN\\b|\\bLIKE\\b|\\bBETWEEN\\b|\\bIS\\b)",
                        Pattern.CASE_INSENSITIVE)
                .matcher(scope);
        while (matcher.find()) {
            String column = matcher.group(1);
            if (isColumnLike(column)) {
                columns.add(stripQualifiedIdentifier(column));
            }
        }
        Matcher orderMatcher = Pattern.compile("\\bORDER\\s+BY\\s+(.+?)(\\bLIMIT\\b|$)", Pattern.CASE_INSENSITIVE)
                .matcher(scope);
        if (orderMatcher.find()) {
            for (String part : orderMatcher.group(1).split(",")) {
                String column = stripQualifiedIdentifier(part.replaceAll("(?i)\\s+(ASC|DESC)\\b.*", ""));
                if (isColumnLike(column)) {
                    columns.add(column);
                }
            }
        }
        return columns;
    }

    private List<String> parseColumnList(String rawColumns) {
        if (rawColumns == null || rawColumns.isBlank()) {
            return List.of();
        }
        Set<String> columns = new LinkedHashSet<>();
        for (String part : rawColumns.split(",")) {
            String column = stripQualifiedIdentifier(part.replaceAll("\\(.+$", ""));
            if (isColumnLike(column)) {
                columns.add(column);
            }
        }
        return new ArrayList<>(columns);
    }

    private boolean isColumnLike(String column) {
        if (column == null || column.isBlank()) {
            return false;
        }
        String normalized = normalizeIdentifier(column);
        return !Set.of("select", "from", "where", "and", "or", "order", "group", "by", "limit",
                "join", "on", "as", "in", "like", "between", "is", "null", "not", "exists")
                .contains(normalized);
    }

    private boolean containsCreateIndex(String content) {
        return content != null && Pattern.compile("\\bCREATE\\s+(?:UNIQUE\\s+)?INDEX\\b", Pattern.CASE_INSENSITIVE)
                .matcher(content)
                .find();
    }

    private boolean isSupportedDdlCarrier(Path path) {
        String name = path.toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".sql") || name.endsWith(".xml") || name.endsWith(".java");
    }

    private boolean isIgnoredScanPath(Path path) {
        String normalized = path.toString().replace('\\', '/');
        return normalized.contains("/target/")
                || normalized.contains("/build/")
                || normalized.contains("/out/")
                || normalized.contains("/dist/")
                || normalized.contains("/coverage/")
                || normalized.contains("/.git/")
                || normalized.contains("/.idea/")
                || normalized.contains("/.mvn/")
                || normalized.contains("/.gradle/")
                || normalized.contains("/node_modules/");
    }

    private String relativize(Path root, Path path) {
        try {
            return root.toAbsolutePath().normalize().relativize(path.toAbsolutePath().normalize()).toString();
        } catch (IllegalArgumentException e) {
            return path.toString();
        }
    }

    private String stripQualifiedIdentifier(String token) {
        if (token == null) {
            return "";
        }
        String cleaned = token.replace("`", "")
                .replace("\"", "")
                .replace("'", "")
                .trim();
        if (cleaned.contains(".")) {
            cleaned = cleaned.substring(cleaned.lastIndexOf('.') + 1);
        }
        return cleaned.replaceAll("[^A-Za-z0-9_].*$", "").trim();
    }

    private String normalizeIdentifier(String identifier) {
        return stripQualifiedIdentifier(identifier).toLowerCase(Locale.ROOT);
    }

    private LinkedHashSet<String> orderedSet(List<String> values) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    set.add(value);
                }
            }
        }
        return set;
    }

    private List<String> locationLabels(List<SqlLocationDto> locations) {
        if (locations == null || locations.isEmpty()) {
            return List.of();
        }
        return locations.stream()
                .map(location -> (location.getFilePath() != null ? location.getFilePath() : location.getFileName())
                        + ":" + location.getStartLine())
                .toList();
    }

    private record TableInfo(
            String tableName,
            String sourceFile,
            LinkedHashSet<String> columns,
            LinkedHashSet<String> primaryKeyColumns,
            LinkedHashSet<String> indexedColumns) {

        private Set<String> indexedColumnsLower() {
            Set<String> values = new LinkedHashSet<>();
            for (String column : indexedColumns) {
                values.add(column.toLowerCase(Locale.ROOT));
            }
            return values;
        }
    }

    private static final class TableUsage {
        private final String displayName;
        private final Set<String> predicateColumns = new LinkedHashSet<>();
        private final List<SqlStatementDto> examples = new ArrayList<>();
        private int sqlCount;

        private TableUsage(String displayName) {
            this.displayName = displayName;
        }
    }
}
