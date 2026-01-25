package org.spectrum.sqlchecker.application.scan.impl;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.spectrum.sqlchecker.application.analysis.ExplainAnalysisService;
import org.spectrum.sqlchecker.application.analysis.dto.ExplainAnalysisDto;
import org.spectrum.sqlchecker.application.analysis.dto.ExplainIssue;
import org.spectrum.sqlchecker.application.analysis.dto.StaticAnalysisDto;
import org.spectrum.sqlchecker.application.analysis.dto.StaticIssue;
import org.spectrum.sqlchecker.application.preprocess.SqlPreprocessService;
import org.spectrum.sqlchecker.application.preprocess.dto.PreprocessRequest;
import org.spectrum.sqlchecker.application.preprocess.dto.PreprocessResult;
import org.spectrum.sqlchecker.application.scan.dto.*;
import org.spectrum.sqlchecker.application.scan.orchestrator.ScanOrchestrator;
import org.spectrum.sqlchecker.application.scan.orchestrator.ScanProgressListener;
import org.spectrum.sqlchecker.application.schema.SchemaInitializationService;
import org.spectrum.sqlchecker.application.schema.dto.SchemaInitializationResult;
import org.spectrum.sqlchecker.domain.rule.RuleIssue;
import org.spectrum.sqlchecker.domain.shared.enumeration.ExplainEligibility;
import org.spectrum.sqlchecker.domain.shared.enumeration.IssueType;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlCategory;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlSourceType;
import org.spectrum.sqlchecker.domain.shared.enumeration.ValidityStatus;
import org.spectrum.sqlchecker.infrastructure.database.ConnectionManager;
import org.spectrum.sqlchecker.infrastructure.extractor.MyBatisSqlExtractor;
import org.spectrum.sqlchecker.infrastructure.rule.SqlRuleEngine;
import org.spectrum.sqlchecker.infrastructure.scan.SqlScanSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * 扫描编排器实现
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Slf4j
@Service
public class ScanOrchestratorImpl implements ScanOrchestrator {

    @Autowired(required = false)
    private ExplainAnalysisService explainAnalysisService;

    @Autowired(required = false)
    private ConnectionManager connectionManager;

    @Autowired(required = false)
    private SchemaInitializationService schemaInitializationService;

    @Autowired(required = false)
    private MyBatisSqlExtractor myBatisSqlExtractor;

    @Autowired(required = false)
    private SqlPreprocessService preprocessService;

    @Autowired(required = false)
    private SqlRuleEngine ruleEngine;

    @Override
    public ScanExecutionResult execute(ScanExecutionRequest request, ScanProgressListener listener) {
        ScanContext context = new ScanContext(request);
        long startTime = System.currentTimeMillis();

        if (request.isEnableExplain() && request.isInitSchema()) {
            initializeSchema(request.getSchemaPath(), request.getPath(), request.getDbConnection());
        }

        List<File> files = findFiles(new File(request.getPath()), context);
        context.totalFiles = files.size();

        if (listener != null) {
            listener.onStart(ScanStartInfo.builder()
                    .path(new File(request.getPath()).getAbsolutePath())
                    .totalFiles(context.totalFiles)
                    .javaFiles(context.javaFiles)
                    .xmlFiles(context.xmlFiles)
                    .sqlFiles(context.sqlFiles)
                    .build());
        }

        for (File file : files) {
            scanFile(file, context);
            context.filesScanned++;
            updateProgress(context, listener, file.getName());
        }

        if (listener != null) {
            listener.onProgress(ScanProgressSnapshot.builder()
                    .progress(100)
                    .stage("Complete")
                    .filesScanned(context.filesScanned)
                    .totalFiles(context.totalFiles)
                    .sqlFound(context.sqlFound)
                    .currentFile("")
                    .build());
            listener.onComplete();
        }

        long duration = System.currentTimeMillis() - startTime;
        context.durationMs = duration;
        computeIssueSummary(context);

        ScanResult scanResult = buildScanResult(context);
        ScanStatistics statistics = ScanStatistics.builder()
                .totalFiles(context.totalFiles)
                .javaFiles(context.javaFiles)
                .xmlFiles(context.xmlFiles)
                .sqlFiles(context.sqlFiles)
                .filesScanned(context.filesScanned)
                .sqlFound(context.sqlFound)
                .sqlParsed(context.sqlParsed)
                .durationMs(duration)
                .criticalIssues(context.critical)
                .warningIssues(context.warning)
                .infoIssues(context.info)
                .build();

        return ScanExecutionResult.builder()
                .scanResult(scanResult)
                .statistics(statistics)
                .issues(new ArrayList<>(context.allIssues))
                .issueSummary(new HashMap<>(context.issueSummary))
                .sqlEntries(new ArrayList<>(context.sqlEntries.values()))
                .scanPath(request.getPath())
                .build();
    }

    private void updateProgress(ScanContext context, ScanProgressListener listener, String currentFile) {
        if (listener == null || context.totalFiles == 0) {
            return;
        }
        int progress = (int) (context.filesScanned * 100.0 / context.totalFiles);
        listener.onProgress(ScanProgressSnapshot.builder()
                .progress(progress)
                .stage("Scanning")
                .filesScanned(context.filesScanned)
                .totalFiles(context.totalFiles)
                .sqlFound(context.sqlFound)
                .currentFile(currentFile)
                .build());
    }

    private void initializeSchema(String schemaPath, String rootPath, String connectionId) {
        if (schemaInitializationService == null) {
            return;
        }
        try {
            String actualPath = schemaPath != null && !schemaPath.isBlank() ? schemaPath : rootPath;
            SchemaInitializationResult result = schemaInitializationService.initialize(Path.of(actualPath), connectionId);
            if (!result.isSuccess()) {
                log.warn("Schema initialization had failures: {}", result.getErrors());
            }
        } catch (Exception e) {
            log.error("Schema initialization failed", e);
        }
    }

    private List<File> findFiles(File dir, ScanContext context) {
        List<File> files = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(dir.toPath())) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java") || p.toString().endsWith(".xml") || p.toString().endsWith(".sql"))
                    .filter(p -> !p.toString().contains("/target/"))
                    .filter(p -> !p.toString().contains("/build/"))
                    .filter(p -> !p.toString().contains("/.git/"))
                    .filter(p -> !p.toString().contains("/node_modules/"))
                    .forEach(p -> {
                        files.add(p.toFile());
                        if (p.toString().endsWith(".java")) {
                            context.javaFiles++;
                        } else if (p.toString().endsWith(".xml")) {
                            context.xmlFiles++;
                        } else if (p.toString().endsWith(".sql")) {
                            context.sqlFiles++;
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to scan files from {}", dir.getAbsolutePath(), e);
        }
        return files;
    }

    private void scanFile(File file, ScanContext context) {
        String name = file.getName();
        if (name.endsWith(".java")) {
            scanJavaFile(file, context);
        } else if (name.endsWith(".xml")) {
            scanXmlFile(file, context);
        } else if (name.endsWith(".sql")) {
            scanSqlFile(file, context);
        }
    }

    private void scanJavaFile(File file, ScanContext context) {
        try {
            String content = Files.readString(file.toPath());
            String relativePath = getRelativePath(file, context.path);

            List<SqlScanSupport.SqlCandidate> candidates = SqlScanSupport.extractSqlFromJavaWithLocations(content);
            for (SqlScanSupport.SqlCandidate candidate : candidates) {
                handleSqlFound(file, relativePath, candidate.sql(), candidate.line(), SqlSourceType.STRING_LITERAL, context);
            }

            List<SqlScanSupport.SqlCandidate> mybatisAnnotations = SqlScanSupport.extractSqlFromMyBatisAnnotations(content);
            for (SqlScanSupport.SqlCandidate candidate : mybatisAnnotations) {
                handleSqlFound(file, relativePath, candidate.sql(), candidate.line(), SqlSourceType.MYBATIS_ANNOTATION, context);
            }

            List<SqlScanSupport.SqlCandidate> jpaAnnotations = SqlScanSupport.extractSqlFromJpaAnnotations(content);
            for (SqlScanSupport.SqlCandidate candidate : jpaAnnotations) {
                handleSqlFound(file, relativePath, candidate.sql(), candidate.line(), SqlSourceType.JPA_ANNOTATION, context);
            }

        } catch (IOException e) {
            if (context.verbose) {
                System.out.println("  [ERROR] Failed to read: " + file.getName());
            }
        }
    }

    private void scanXmlFile(File file, ScanContext context) {
        try {
            String content = Files.readString(file.toPath());
            String relativePath = getRelativePath(file, context.path);

            List<String> sqls;
            if (myBatisSqlExtractor != null) {
                sqls = myBatisSqlExtractor.extract(content);
            } else {
                sqls = Collections.emptyList();
            }

            for (String sql : sqls) {
                handleSqlFound(file, relativePath, sql, 1, SqlSourceType.MYBATIS, context);
            }

        } catch (IOException e) {
            if (context.verbose) {
                System.out.println("  [ERROR] Failed to read: " + file.getName());
            }
        } catch (Exception e) {
            if (context.verbose) {
                System.out.println("  [ERROR] Failed to parse XML: " + file.getName() + " - " + e.getMessage());
            }
        }
    }

    private void scanSqlFile(File file, ScanContext context) {
        try {
            String content = Files.readString(file.toPath());
            String relativePath = getRelativePath(file, context.path);

            List<SqlScanSupport.SqlCandidate> statements = SqlScanSupport.extractSqlFromSqlFile(content);
            for (SqlScanSupport.SqlCandidate statement : statements) {
                handleSqlFound(file, relativePath, statement.sql(), statement.line(), SqlSourceType.SQL_FILE, context);
            }
        } catch (IOException e) {
            if (context.verbose) {
                System.out.println("  [ERROR] Failed to read: " + file.getName());
            }
        }
    }

    private void handleSqlFound(File file, String relativePath, String sql, int line, SqlSourceType sourceType, ScanContext context) {
        if (sql == null || sql.isBlank() || !SqlScanSupport.looksLikeSql(sql)) {
            return;
        }

        context.sqlFound++;
        String abstractSql = SqlScanSupport.abstractSql(sql);
        String sqlKey = abstractSql.toUpperCase();
        ScanSqlEntry entry = context.sqlEntries.get(sqlKey);
        if (entry == null) {
            String sqlId = "sql-" + org.spectrum.sqlchecker.domain.shared.valueobject.SqlHash.fromAbstract(abstractSql).getValue();
            entry = ScanSqlEntry.builder()
                    .id(sqlId)
                    .abstractSql(abstractSql)
                    .originalSql(sql)
                    .build();
            context.sqlEntries.put(sqlKey, entry);
            context.sqlParsed++;
        }

        entry.getLocations().add(SqlLocationDto.builder()
                .filePath(relativePath)
                .fileName(file.getName())
                .startLine(line)
                .endLine(line)
                .startColumn(1)
                .endColumn(1)
                .sourceType(sourceType)
                .build());

        if (entry.getPreprocessResult() == null && preprocessService != null) {
            PreprocessRequest request = PreprocessRequest.builder()
                    .sqlId(entry.getId())
                    .originalSql(sql)
                    .sourceType(sourceType)
                    .sourceContext(relativePath + ":" + line)
                    .explainEnabled(context.enableExplain)
                    .build();
            try {
                entry.setPreprocessResult(preprocessService.preprocess(request));
            } catch (Exception e) {
                log.warn("SQL preprocess failed for {}: {}", relativePath, e.getMessage());
            }
        }

        if (!entry.isAnalyzed()) {
            List<ScanIssue> issues = analyzeSql(relativePath, sql, entry, entry.getPreprocessResult(), context);
            entry.getIssues().addAll(issues);
            entry.setAnalyzed(true);
        }
    }

    private List<ScanIssue> analyzeSql(String fileName, String originalSql, ScanSqlEntry entry, PreprocessResult preprocessResult, ScanContext context) {
        List<ScanIssue> issues = new ArrayList<>();

        issues.addAll(checkSqlInjectionRisk(fileName, originalSql));

        String analysisSql = preprocessResult != null && preprocessResult.getNormalizedSql() != null
                ? preprocessResult.getNormalizedSql()
                : originalSql;

        try {
            Statement stmt = CCJSqlParserUtil.parse(analysisSql);

            if (ruleEngine != null) {
                issues.addAll(checkWithRuleEngine(fileName, analysisSql, ruleEngine));
            } else {
                issues.addAll(checkSelectStar(fileName, analysisSql, stmt));
                issues.addAll(checkMissingWhere(fileName, analysisSql, stmt));
                issues.addAll(checkLikeLeadingWildcard(fileName, analysisSql, stmt));
                issues.addAll(checkOrderByWithoutLimit(fileName, analysisSql, stmt));
                issues.addAll(checkJoinType(fileName, analysisSql, stmt));
                issues.addAll(checkSubquery(fileName, analysisSql, stmt));
            }

        } catch (JSQLParserException e) {
            if (context.verbose) {
                System.out.println("  [PARSE_FAIL] " + fileName + ": " + truncate(analysisSql, 60));
            }
            issues.addAll(checkSimplePatterns(fileName, originalSql));
        }

        if (context.enableExplain && explainAnalysisService != null && connectionManager != null) {
            try {
                if (connectionManager.getConfig(context.dbConnection) != null) {
                    String sqlId = entry != null && entry.getId() != null
                            ? entry.getId()
                            : fileName.replace("/", "_").replace(".", "_") + "_" + context.sqlFound;
                    String explainSql = originalSql;
                    boolean explainSupported = true;
                    if (preprocessResult != null) {
                        if (preprocessResult.getExplainEligibility() != null
                                && preprocessResult.getExplainEligibility() != ExplainEligibility.SUPPORTED) {
                            explainSupported = false;
                            if (context.verbose) {
                                System.out.println("  [EXPLAIN_SKIP] " + fileName + ": " + preprocessResult.getExplainEligibility());
                            }
                        }
                        if (preprocessResult.getExplainSql() != null) {
                            explainSql = preprocessResult.getExplainSql();
                        }
                    }

                    ExplainAnalysisDto explainResult = null;
                    if (explainSupported) {
                        explainResult = explainAnalysisService.analyze(sqlId, explainSql, context.dbConnection);
                        if (entry != null) {
                            entry.setExplainAnalysis(explainResult);
                        }
                    }

                    if (explainResult != null && explainResult.getIssues() != null && !explainResult.getIssues().isEmpty()) {
                        for (ExplainIssue explainIssue : explainResult.getIssues()) {
                            ScanIssue explainSqlIssue = ScanIssue.builder()
                                    .fileName(fileName + " [EXPLAIN]")
                                    .sql(explainSql)
                                    .type(explainIssue.getType())
                                    .severity(explainIssue.getSeverity().name())
                                    .message(explainIssue.getMessage() + (explainIssue.getSuggestion() != null
                                            ? " 💡 " + explainIssue.getSuggestion() : ""))
                                    .build();
                            issues.add(explainSqlIssue);
                        }
                    }
                } else if (context.verbose) {
                    System.out.println("  [EXPLAIN_SKIP] Connection '" + context.dbConnection + "' not configured");
                }
            } catch (Exception e) {
                if (context.verbose) {
                    System.out.println("  [EXPLAIN_ERROR] " + fileName + ": " + e.getMessage());
                }
                log.warn("EXPLAIN analysis failed for {}: {}", fileName, e.getMessage());
            }
        }

        if (!issues.isEmpty()) {
            context.allIssues.addAll(issues);
        }

        return issues;
    }

    private List<ScanIssue> checkWithRuleEngine(String fileName, String sql, SqlRuleEngine engine) {
        List<ScanIssue> issues = new ArrayList<>();
        try {
            List<RuleIssue> ruleIssues = engine.analyze(fileName + "_sql", sql);
            for (RuleIssue ruleIssue : ruleIssues) {
                issues.add(convertToSqlIssue(fileName, sql, ruleIssue));
            }
        } catch (Exception e) {
            // ignore
        }
        return issues;
    }

    private ScanIssue convertToSqlIssue(String fileName, String sql, RuleIssue ruleIssue) {
        return ScanIssue.builder()
                .fileName(fileName)
                .sql(sql)
                .type(ruleIssue.getRuleId().toUpperCase().replace("-", "_"))
                .severity(severityToString(ruleIssue.getSeverity()))
                .message(ruleIssue.getMessage() + (ruleIssue.getSuggestion() != null ? " 💡 " + ruleIssue.getSuggestion() : ""))
                .build();
    }

    private String severityToString(SeverityLevel level) {
        if (level == null) {
            return "INFO";
        }
        return switch (level) {
            case CRITICAL -> "CRITICAL";
            case WARNING -> "WARNING";
            default -> "INFO";
        };
    }

    private List<ScanIssue> checkSelectStar(String fileName, String sql, Statement stmt) {
        List<ScanIssue> issues = new ArrayList<>();
        if (sql.toUpperCase().contains("SELECT *")) {
            issues.add(ScanIssue.builder()
                    .fileName(fileName)
                    .sql(sql)
                    .type("SELECT_STAR")
                    .severity("WARNING")
                    .message("使用了 SELECT *")
                    .build());
        }
        return issues;
    }

    private List<ScanIssue> checkMissingWhere(String fileName, String sql, Statement stmt) {
        List<ScanIssue> issues = new ArrayList<>();
        String upper = sql.toUpperCase();
        if (upper.startsWith("UPDATE") || upper.startsWith("DELETE")) {
            if (!upper.contains(" WHERE ")) {
                issues.add(ScanIssue.builder()
                        .fileName(fileName)
                        .sql(sql)
                        .type("MISSING_WHERE")
                        .severity("CRITICAL")
                        .message("UPDATE/DELETE 缺少 WHERE 条件")
                        .build());
            }
        }
        return issues;
    }

    private List<ScanIssue> checkLikeLeadingWildcard(String fileName, String sql, Statement stmt) {
        List<ScanIssue> issues = new ArrayList<>();
        if (sql.toUpperCase().matches(".*LIKE\\s+['\"]%.*")) {
            issues.add(ScanIssue.builder()
                    .fileName(fileName)
                    .sql(sql)
                    .type("LIKE_LEADING_WILDCARD")
                    .severity("WARNING")
                    .message("LIKE 以通配符开头")
                    .build());
        }
        return issues;
    }

    private List<ScanIssue> checkOrderByWithoutLimit(String fileName, String sql, Statement stmt) {
        List<ScanIssue> issues = new ArrayList<>();
        String upper = sql.toUpperCase();
        if (upper.contains("ORDER BY") && !upper.contains("LIMIT")) {
            issues.add(ScanIssue.builder()
                    .fileName(fileName)
                    .sql(sql)
                    .type("ORDER_BY_WITHOUT_LIMIT")
                    .severity("WARNING")
                    .message("ORDER BY 未限制数量")
                    .build());
        }
        return issues;
    }

    private List<ScanIssue> checkJoinType(String fileName, String sql, Statement stmt) {
        List<ScanIssue> issues = new ArrayList<>();
        if (sql.toUpperCase().contains(",")) {
            issues.add(ScanIssue.builder()
                    .fileName(fileName)
                    .sql(sql)
                    .type("IMPLICIT_JOIN")
                    .severity("WARNING")
                    .message("检测到隐式 JOIN，建议使用显式 JOIN")
                    .build());
        }
        return issues;
    }

    private List<ScanIssue> checkSubquery(String fileName, String sql, Statement stmt) {
        List<ScanIssue> issues = new ArrayList<>();
        long parenthesesCount = sql.chars().filter(ch -> ch == '(').count();
        if (parenthesesCount > 2) {
            issues.add(ScanIssue.builder()
                    .fileName(fileName)
                    .sql(sql)
                    .type("COMPLEX_SUBQUERY")
                    .severity("INFO")
                    .message("检测到复杂的嵌套结构，建议拆分")
                    .build());
        }
        return issues;
    }

    private List<ScanIssue> checkSimplePatterns(String fileName, String sql) {
        List<ScanIssue> issues = new ArrayList<>();
        String upper = sql.toUpperCase();

        if (upper.contains("SELECT *")) {
            issues.add(ScanIssue.builder()
                    .fileName(fileName)
                    .sql(sql)
                    .type("SELECT_STAR")
                    .severity("CRITICAL")
                    .message("使用了 SELECT *")
                    .build());
        }
        if (upper.matches(".*LIKE\\s+['\"]%.*")) {
            issues.add(ScanIssue.builder()
                    .fileName(fileName)
                    .sql(sql)
                    .type("LIKE_LEADING_WILDCARD")
                    .severity("CRITICAL")
                    .message("LIKE 以通配符开头")
                    .build());
        }

        return issues;
    }

    private List<ScanIssue> checkSqlInjectionRisk(String fileName, String sql) {
        List<ScanIssue> issues = new ArrayList<>();
        if (sql.contains("${")) {
            issues.add(ScanIssue.builder()
                    .fileName(fileName)
                    .sql(sql)
                    .type("SQL_INJECTION")
                    .severity("CRITICAL")
                    .message("检测到 ${} 动态拼接，可能存在 SQL 注入风险")
                    .build());
        }
        return issues;
    }

    private void computeIssueSummary(ScanContext context) {
        for (ScanIssue issue : context.allIssues) {
            context.issueSummary.put(issue.getType(), context.issueSummary.getOrDefault(issue.getType(), 0) + 1);
            switch (issue.getSeverity()) {
                case "CRITICAL" -> context.critical++;
                case "WARNING" -> context.warning++;
                default -> context.info++;
            }
        }
    }

    private ScanResult buildScanResult(ScanContext context) {
        List<SqlStatementDto> sqlStatements = new ArrayList<>();

        for (ScanSqlEntry entry : context.sqlEntries.values()) {
            List<ScanIssue> sqlIssues = entry.getIssues();
            String sqlId = entry.getId() != null ? entry.getId() : UUID.randomUUID().toString();

            SeverityLevel highestSeverity = SeverityLevel.INFO;
            int lowestScore = 100;

            for (ScanIssue issue : sqlIssues) {
                if ("CRITICAL".equals(issue.getSeverity())) {
                    highestSeverity = SeverityLevel.CRITICAL;
                    lowestScore = Math.min(lowestScore, 40);
                } else if ("WARNING".equals(issue.getSeverity()) && highestSeverity != SeverityLevel.CRITICAL) {
                    highestSeverity = SeverityLevel.WARNING;
                    lowestScore = Math.min(lowestScore, 60);
                } else if (!sqlIssues.isEmpty()) {
                    lowestScore = Math.min(lowestScore, 80);
                }
            }

            List<StaticIssue> staticIssues = new ArrayList<>();
            for (ScanIssue issue : sqlIssues) {
                String message = issue.getMessage().split("💡")[0].trim();
                String suggestion = issue.getMessage().contains("💡") ? issue.getMessage().split("💡")[1].trim() : null;

                SeverityLevel issueSeverity = SeverityLevel.INFO;
                if ("CRITICAL".equals(issue.getSeverity())) {
                    issueSeverity = SeverityLevel.CRITICAL;
                } else if ("WARNING".equals(issue.getSeverity())) {
                    issueSeverity = SeverityLevel.WARNING;
                }

                StaticIssue staticIssue = StaticIssue.builder()
                        .type(mapToIssueType(issue.getType()))
                        .severity(issueSeverity)
                        .message(message)
                        .suggestion(suggestion)
                        .location(entry.getLocations().isEmpty() ? issue.getFileName() : entry.getLocations().get(0).getFileName())
                        .build();

                staticIssues.add(staticIssue);
            }

            StaticAnalysisDto staticAnalysis = StaticAnalysisDto.builder()
                    .sqlId(sqlId)
                    .severity(highestSeverity)
                    .issues(staticIssues)
                    .score(lowestScore)
                    .build();

            PreprocessResult preprocess = entry.getPreprocessResult();

            SqlStatementDto dto = SqlStatementDto.builder()
                    .id(sqlId)
                    .sqlType(org.spectrum.sqlchecker.domain.shared.enumeration.SqlType.fromSql(entry.getOriginalSql()))
                    .originalSql(entry.getOriginalSql())
                    .abstractSql(entry.getAbstractSql())
                    .category(preprocess != null ? preprocess.getCategory() : SqlCategory.UNKNOWN)
                    .normalizedSql(preprocess != null ? preprocess.getNormalizedSql() : entry.getAbstractSql())
                    .explainSql(preprocess != null ? preprocess.getExplainSql() : null)
                    .validity(preprocess != null ? preprocess.getValidity() : ValidityStatus.UNKNOWN)
                    .explainEligibility(preprocess != null ? preprocess.getExplainEligibility() : ExplainEligibility.SKIPPED)
                    .preprocessErrorReason(preprocess != null ? preprocess.getErrorReason() : null)
                    .sqlHash(org.spectrum.sqlchecker.domain.shared.valueobject.SqlHash.fromAbstract(entry.getAbstractSql()).getValue())
                    .locations(entry.getLocations())
                    .staticAnalysis(staticAnalysis)
                    .explainAnalysis(entry.getExplainAnalysis())
                    .severity(highestSeverity)
                    .score(lowestScore)
                    .build();

            sqlStatements.add(dto);
        }

        return ScanResult.builder()
                .scanId(UUID.randomUUID().toString())
                .status(org.spectrum.sqlchecker.domain.shared.enumeration.ScanStatus.COMPLETED)
                .filesScanned(context.totalFiles)
                .sqlFound(context.sqlFound)
                .uniqueSqlFound(context.sqlParsed)
                .durationMs(context.durationMs)
                .sqlStatements(sqlStatements)
                .errors(new ArrayList<>())
                .build();
    }

    private IssueType mapToIssueType(String type) {
        if (type == null) {
            return IssueType.SELECT_WITHOUT_WHERE;
        }
        return switch (type.toUpperCase()) {
            case "SELECT_STAR" -> IssueType.SELECT_STAR;
            case "MISSING_WHERE" -> IssueType.SELECT_WITHOUT_WHERE;
            case "LIKE_LEADING_WILDCARD" -> IssueType.LIKE_LEADING_WILDCARD;
            case "IMPLICIT_JOIN" -> IssueType.CROSS_JOIN;
            case "MULTI_COLUMN_OR" -> IssueType.MISSING_INDEX;
            case "MULTI_COLUMN_IN" -> IssueType.MISSING_INDEX;
            case "NOT_IN" -> IssueType.MISSING_INDEX;
            case "COMPLEX_SUBQUERY" -> IssueType.SUBQUERY_IN_SELECT;
            case "N_PLUS_ONE" -> IssueType.POTENTIAL_N_PLUS_ONE;
            case "SQL_INJECTION" -> IssueType.SQL_INJECTION_RISK;
            default -> IssueType.SELECT_WITHOUT_WHERE;
        };
    }

    private String getRelativePath(File file, String basePath) {
        String base = new File(basePath).getAbsolutePath();
        String fullPath = file.getAbsolutePath();
        if (fullPath.startsWith(base)) {
            return fullPath.substring(base.length() + 1);
        }
        return file.getName();
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static class ScanContext {
        private final String path;
        private final boolean verbose;
        private final boolean enableExplain;
        private final String dbConnection;

        private int totalFiles = 0;
        private int javaFiles = 0;
        private int xmlFiles = 0;
        private int sqlFiles = 0;
        private int sqlFound = 0;
        private int sqlParsed = 0;
        private int filesScanned = 0;
        private long durationMs = 0;
        private int critical = 0;
        private int warning = 0;
        private int info = 0;

        private final List<ScanIssue> allIssues = new ArrayList<>();
        private final Map<String, Integer> issueSummary = new HashMap<>();
        private final Map<String, ScanSqlEntry> sqlEntries = new LinkedHashMap<>();

        private ScanContext(ScanExecutionRequest request) {
            this.path = request.getPath();
            this.verbose = request.isVerbose();
            this.enableExplain = request.isEnableExplain();
            this.dbConnection = request.getDbConnection();
        }
    }
}
