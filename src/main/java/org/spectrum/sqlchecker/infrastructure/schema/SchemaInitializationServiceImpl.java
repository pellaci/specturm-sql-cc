package org.spectrum.sqlchecker.infrastructure.schema;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.application.schema.SchemaInitializationService;
import org.spectrum.sqlchecker.application.schema.dto.SchemaInitializationResult;
import org.spectrum.sqlchecker.application.schema.dto.SchemaInitializationResult.TableCreationDetail;
import org.spectrum.sqlchecker.application.schema.dto.TableDefinition;
import org.spectrum.sqlchecker.infrastructure.database.ConnectionManager;
import org.spectrum.sqlchecker.infrastructure.extractor.MyBatisSqlExtractor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Stream;

/**
 * Schema 初始化服务实现
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaInitializationServiceImpl implements SchemaInitializationService {

    private final ConnectionManager connectionManager;
    private final DdlExtractor ddlExtractor;
    private final DmlSchemaInferrer dmlSchemaInferrer;
    private final MyBatisSqlExtractor myBatisSqlExtractor;

    @Override
    public SchemaInitializationResult initialize(Path scanPath, String connectionId) {
        long startTime = System.currentTimeMillis();
        List<TableCreationDetail> details = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> processedTables = new HashSet<>();

        try {
            // 1. 扫描 DDL 文件
            List<Path> ddlFiles = findDdlFiles(scanPath);
            log.info("Found {} DDL files in {}", ddlFiles.size(), scanPath);

            // 2. 提取并执行 CREATE TABLE（从 DDL 文件）
            for (Path ddlFile : ddlFiles) {
                try {
                    String content = Files.readString(ddlFile);
                    List<TableDefinition> tables = ddlExtractor.extractCreateTables(
                            content, ddlFile.toString());

                    for (TableDefinition table : tables) {
                        String tableNameLower = table.getTableName().toLowerCase();
                        if (!processedTables.contains(tableNameLower)) {
                            TableCreationDetail detail = executeCreateTable(table, connectionId, "DDL");
                            details.add(detail);
                            if (detail.isCreated() || detail.isSkipped()) {
                                processedTables.add(tableNameLower);
                            }
                        }
                    }
                } catch (IOException e) {
                    String errorMsg = "Failed to read file: " + ddlFile + " - " + e.getMessage();
                    errors.add(errorMsg);
                    log.warn(errorMsg);
                }
            }

            // 3. 如果没有 DDL 文件，从 MyBatis mapper 推断表结构
            if (ddlFiles.isEmpty()) {
                log.info("No DDL files found, inferring schema from MyBatis mappers...");
                List<String> dmlStatements = extractDmlFromMappers(scanPath);
                log.info("Found {} DML statements from MyBatis mappers", dmlStatements.size());

                if (!dmlStatements.isEmpty()) {
                    List<TableDefinition> inferredTables = dmlSchemaInferrer.inferFromDml(dmlStatements);
                    log.info("Inferred {} tables from DML statements", inferredTables.size());

                    for (TableDefinition table : inferredTables) {
                        String tableNameLower = table.getTableName().toLowerCase();
                        if (!processedTables.contains(tableNameLower)) {
                            TableCreationDetail detail = executeCreateTable(table, connectionId, "INFERRED");
                            details.add(detail);
                            if (detail.isCreated() || detail.isSkipped()) {
                                processedTables.add(tableNameLower);
                            }
                        }
                    }
                }
            }

            log.info("Schema initialization completed: {} tables processed", processedTables.size());

        } catch (Exception e) {
            log.error("Schema initialization failed", e);
            errors.add("Initialization failed: " + e.getMessage());
        }

        // 4. 统计结果
        int created = (int) details.stream().filter(TableCreationDetail::isCreated).count();
        int skipped = (int) details.stream().filter(TableCreationDetail::isSkipped).count();
        int failed = (int) details.stream()
                .filter(d -> !d.isCreated() && !d.isSkipped()).count();

        return SchemaInitializationResult.builder()
                .success(errors.isEmpty() || failed == 0)
                .tablesCreated(created)
                .tablesSkipped(skipped)
                .tablesFailed(failed)
                .details(details)
                .errors(errors)
                .durationMs(System.currentTimeMillis() - startTime)
                .build();
    }

    @Override
    public SchemaInitializationResult initializeFromDml(List<String> dmlStatements, String connectionId) {
        long startTime = System.currentTimeMillis();
        List<TableCreationDetail> details = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            // 1. 从 DML 推断表结构
            List<TableDefinition> tables = dmlSchemaInferrer.inferFromDml(dmlStatements);
            log.info("Inferred {} tables from {} DML statements", tables.size(), dmlStatements.size());

            // 2. 执行 CREATE TABLE
            for (TableDefinition table : tables) {
                TableCreationDetail detail = executeCreateTable(table, connectionId, "INFERRED");
                details.add(detail);
            }

        } catch (Exception e) {
            log.error("Schema initialization from DML failed", e);
            errors.add("Initialization from DML failed: " + e.getMessage());
        }

        // 3. 统计结果
        int created = (int) details.stream().filter(TableCreationDetail::isCreated).count();
        int skipped = (int) details.stream().filter(TableCreationDetail::isSkipped).count();
        int failed = (int) details.stream()
                .filter(d -> !d.isCreated() && !d.isSkipped()).count();

        return SchemaInitializationResult.builder()
                .success(errors.isEmpty() || failed == 0)
                .tablesCreated(created)
                .tablesSkipped(skipped)
                .tablesFailed(failed)
                .details(details)
                .errors(errors)
                .durationMs(System.currentTimeMillis() - startTime)
                .build();
    }

    /**
     * 查找包含 CREATE TABLE 的 SQL 文件
     */
    private List<Path> findDdlFiles(Path scanPath) throws IOException {
        List<Path> ddlFiles = new ArrayList<>();

        if (!Files.exists(scanPath)) {
            log.warn("Scan path does not exist: {}", scanPath);
            return ddlFiles;
        }

        try (Stream<Path> paths = Files.walk(scanPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".sql"))
                    .filter(p -> !p.toString().contains("/target/"))
                    .filter(p -> !p.toString().contains("/build/"))
                    .filter(p -> !p.toString().contains("/.git/"))
                    .filter(this::containsCreateTable)
                    .forEach(ddlFiles::add);
        }

        return ddlFiles;
    }

    /**
     * 检查文件是否包含 CREATE TABLE
     */
    private boolean containsCreateTable(Path file) {
        try {
            String content = Files.readString(file);
            return ddlExtractor.containsCreateTable(content);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 执行 CREATE TABLE
     */
    private TableCreationDetail executeCreateTable(TableDefinition table, String connectionId, String source) {
        try {
            Connection conn = connectionManager.getConnection(connectionId);

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(table.getOriginalDdl());
                log.info("Created table: {} (source: {}, file: {})",
                        table.getTableName(), source, table.getSourceFile());

                return TableCreationDetail.builder()
                        .tableName(table.getTableName())
                        .source(source)
                        .sourceFile(table.getSourceFile())
                        .created(true)
                        .skipped(false)
                        .build();
            }
        } catch (SQLException e) {
            // 检查是否是"表已存在"错误
            if (isTableExistsError(e)) {
                log.debug("Table already exists: {}", table.getTableName());
                return TableCreationDetail.builder()
                        .tableName(table.getTableName())
                        .source(source)
                        .sourceFile(table.getSourceFile())
                        .created(false)
                        .skipped(true)
                        .build();
            }

            log.warn("Failed to create table {}: {}", table.getTableName(), e.getMessage());
            return TableCreationDetail.builder()
                    .tableName(table.getTableName())
                    .source(source)
                    .sourceFile(table.getSourceFile())
                    .created(false)
                    .skipped(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * 从 MyBatis mapper 文件中提取 DML 语句
     */
    private List<String> extractDmlFromMappers(Path scanPath) {
        List<String> dmlStatements = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(scanPath)) {
            List<Path> xmlFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".xml"))
                    .filter(p -> !p.toString().contains("/target/"))
                    .filter(p -> !p.toString().contains("/build/"))
                    .filter(p -> !p.toString().contains("/.git/"))
                    .toList();

            for (Path xmlFile : xmlFiles) {
                try {
                    String content = Files.readString(xmlFile);
                    // 检查是否是 MyBatis mapper 文件
                    if (content.contains("<mapper") &&
                            (content.contains("mybatis.org") || content.contains("ibatis.apache.org"))) {
                        extractSqlFromMapper(content, dmlStatements);
                    }
                } catch (IOException e) {
                    log.debug("Failed to read file: {}", xmlFile);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to scan for MyBatis mappers: {}", e.getMessage());
        }

        return dmlStatements;
    }

    /**
     * 从 MyBatis mapper 内容中提取 SQL
     */
    private void extractSqlFromMapper(String content, List<String> dmlStatements) {
        try {
            List<String> sqls = myBatisSqlExtractor.extract(content);
            for (String sql : sqls) {
                if (!sql.isBlank() && looksLikeDml(sql)) {
                    dmlStatements.add(sql);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract SQL from mapper for schema inference: {}", e.getMessage());
        }
    }

    /**
     * 检查是否是 DML 语句
     */
    private boolean looksLikeDml(String sql) {
        String upper = sql.toUpperCase().trim();
        return upper.startsWith("SELECT ") || upper.startsWith("INSERT ") ||
                upper.startsWith("UPDATE ") || upper.startsWith("DELETE ");
    }

    /**
     * 检查是否是表已存在错误
     */
    private boolean isTableExistsError(SQLException e) {
        // MySQL: Error code 1050 "Table already exists"
        // 也检查消息内容以支持其他数据库
        return e.getErrorCode() == 1050 ||
                e.getMessage().toLowerCase().contains("already exists");
    }
}
