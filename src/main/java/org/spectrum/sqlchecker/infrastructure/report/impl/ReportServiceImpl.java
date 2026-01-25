package org.spectrum.sqlchecker.infrastructure.report.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.application.report.ReportService;
import org.spectrum.sqlchecker.application.report.dto.ReportStatItem;
import org.spectrum.sqlchecker.application.report.dto.ReportSummary;
import org.spectrum.sqlchecker.application.scan.dto.ScanResult;
import org.spectrum.sqlchecker.application.scan.dto.SqlLocationDto;
import org.spectrum.sqlchecker.application.scan.dto.SqlStatementDto;
import org.spectrum.sqlchecker.domain.shared.exception.ScanException;
import org.spectrum.sqlchecker.infrastructure.template.TemplateEngine;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 报告服务实现
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final TemplateEngine templateEngine;
    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("MYBATIS_XML_STATIC", "MyBatis XML 静态"),
            Map.entry("MYBATIS_XML_DYNAMIC", "MyBatis XML 动态"),
            Map.entry("MYBATIS_ANNOTATION", "MyBatis 注解"),
            Map.entry("JPA_NATIVE_QUERY", "JPA 原生查询"),
            Map.entry("STRING_CONCAT", "字符串拼接"),
            Map.entry("PLACEHOLDER_TEMPLATE", "占位符模板"),
            Map.entry("UNKNOWN", "未知"),
            Map.entry("VALID", "合法"),
            Map.entry("INVALID", "非法"),
            Map.entry("SUPPORTED", "可执行"),
            Map.entry("NOT_SUPPORTED", "不支持"),
            Map.entry("SKIPPED", "已跳过"),
            Map.entry("JAVA", "Java"),
            Map.entry("JAVASCRIPT", "JavaScript/TypeScript"),
            Map.entry("MYBATIS", "MyBatis XML"),
            Map.entry("JPA_ANNOTATION", "JPA 注解"),
            Map.entry("STRING_LITERAL", "字符串拼接"),
            Map.entry("SQL_FILE", "SQL 文件"),
            Map.entry("SELECT_STAR", "SELECT *"),
            Map.entry("SELECT_WITHOUT_WHERE", "SELECT 无 WHERE"),
            Map.entry("MISSING_INDEX", "缺失索引"),
            Map.entry("IMPLICIT_TYPE_CONVERSION", "隐式类型转换"),
            Map.entry("SUSPICIOUS_JOIN_ORDER", "可疑 JOIN 顺序"),
            Map.entry("CROSS_JOIN", "隐式/CROSS JOIN"),
            Map.entry("SUBQUERY_IN_SELECT", "SELECT 中子查询"),
            Map.entry("UNCORRELATED_SUBQUERY", "非相关子查询"),
            Map.entry("POTENTIAL_N_PLUS_ONE", "潜在 N+1"),
            Map.entry("SQL_INJECTION_RISK", "SQL 注入风险"),
            Map.entry("DYNAMIC_SQL", "动态 SQL"),
            Map.entry("TOO_MANY_JOINS", "过多 JOIN"),
            Map.entry("LIKE_LEADING_WILDCARD", "前置通配符 LIKE"),
            Map.entry("FULL_TABLE_SCAN", "全表扫描"),
            Map.entry("NO_INDEX_USED", "未使用索引"),
            Map.entry("HIGH_ROWS", "扫描行数过多"),
            Map.entry("USING_TEMPORARY", "使用临时表"),
            Map.entry("USING_FILESORT", "使用文件排序")
    );

    @Override
    public void generateHtmlReport(ScanResult scanResult, String outputPath) throws ScanException {
        try {
            Path path = Paths.get(outputPath);
            Files.createDirectories(path.getParent());

            try (OutputStream out = new FileOutputStream(outputPath);
                 OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                generateHtmlReport(scanResult, writer);
            }

            log.info("Report generated: {}", outputPath);

        } catch (IOException e) {
            throw new ScanException("Failed to generate report: " + outputPath, e);
        }
    }

    @Override
    public void generateHtmlReport(ScanResult scanResult, OutputStream outputStream) throws ScanException {
        try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            generateHtmlReport(scanResult, writer);
        } catch (IOException e) {
            throw new ScanException("Failed to generate report", e);
        }
    }

    private void generateHtmlReport(ScanResult scanResult, OutputStreamWriter writer) throws IOException {
        Map<String, Object> context = new HashMap<>();
        context.put("summary", generateSummary(scanResult));
        context.put("sqlStatements", scanResult.getSqlStatements());

        templateEngine.render("report", context, writer);
    }

    @Override
    public ReportSummary generateSummary(ScanResult scanResult) {
        int criticalIssues = 0;
        int warningIssues = 0;
        int infoIssues = 0;
        int totalScore = 0;
        int parsedSql = 0;
        int explainEligible = 0;
        int explainExecuted = 0;

        Map<String, Integer> categoryCounts = new LinkedHashMap<>();
        Map<String, Integer> validityCounts = new LinkedHashMap<>();
        Map<String, Integer> explainEligibilityCounts = new LinkedHashMap<>();
        Map<String, Integer> sourceTypeCounts = new LinkedHashMap<>();
        Map<String, Integer> staticIssueCounts = new LinkedHashMap<>();
        Map<String, Integer> explainIssueCounts = new LinkedHashMap<>();

        List<SqlStatementDto> sqlStatements = scanResult.getSqlStatements();
        if (sqlStatements != null) {
            parsedSql = sqlStatements.size();
        }

        if (sqlStatements != null) {
            for (SqlStatementDto sql : sqlStatements) {
            if (sql.getSeverity() != null) {
                switch (sql.getSeverity()) {
                    case CRITICAL -> criticalIssues++;
                    case WARNING -> warningIssues++;
                    case INFO -> infoIssues++;
                }
            }
            if (sql.getScore() != null) {
                totalScore += sql.getScore();
            }

                if (sql.getCategory() != null) {
                    increment(categoryCounts, formatLabel(sql.getCategory().name()));
                } else {
                    increment(categoryCounts, "Unknown");
                }

                if (sql.getValidity() != null) {
                    increment(validityCounts, formatLabel(sql.getValidity().name()));
                } else {
                    increment(validityCounts, "Unknown");
                }

                if (sql.getExplainEligibility() != null) {
                    increment(explainEligibilityCounts, formatLabel(sql.getExplainEligibility().name()));
                    if (sql.getExplainEligibility().name().equals("SUPPORTED")) {
                        explainEligible++;
                        if (sql.getExplainAnalysis() != null && sql.getExplainAnalysis().getPlan() != null) {
                            explainExecuted++;
                        }
                    }
                } else {
                    increment(explainEligibilityCounts, "Unknown");
                }

                if (sql.getLocations() != null && !sql.getLocations().isEmpty()) {
                    SqlLocationDto location = sql.getLocations().get(0);
                    if (location.getSourceType() != null) {
                        increment(sourceTypeCounts, formatLabel(location.getSourceType().name()));
                    } else {
                        increment(sourceTypeCounts, "Unknown");
                    }
                } else {
                    increment(sourceTypeCounts, "Unknown");
                }

                if (sql.getStaticAnalysis() != null && sql.getStaticAnalysis().getIssues() != null) {
                    sql.getStaticAnalysis().getIssues()
                            .forEach(issue -> {
                                if (issue.getType() != null) {
                                    increment(staticIssueCounts, formatLabel(issue.getType().name()));
                                } else {
                                    increment(staticIssueCounts, "Unknown");
                                }
                            });
                }

                if (sql.getExplainAnalysis() != null && sql.getExplainAnalysis().getIssues() != null) {
                    sql.getExplainAnalysis().getIssues()
                            .forEach(issue -> {
                                if (issue.getType() != null) {
                                    increment(explainIssueCounts, formatLabel(issue.getType()));
                                } else {
                                    increment(explainIssueCounts, "Unknown");
                                }
                            });
                }
            }
        }

        double avgScore = (sqlStatements == null || sqlStatements.isEmpty()) ? 100.0
                : (double) totalScore / sqlStatements.size();
        int totalSql = scanResult.getSqlFound();
        double parseRate = totalSql == 0 ? 0.0 : parsedSql * 100.0 / totalSql;
        double explainCoverage = explainEligible == 0 ? 0.0 : explainExecuted * 100.0 / explainEligible;

        int staticIssueTotal = sumCounts(staticIssueCounts);
        int explainIssueTotal = sumCounts(explainIssueCounts);

        return ReportSummary.builder()
                .totalSql(totalSql)
                .parsedSql(parsedSql)
                .totalIssues(criticalIssues + warningIssues + infoIssues)
                .criticalIssues(criticalIssues)
                .warningIssues(warningIssues)
                .infoIssues(infoIssues)
                .durationMs(scanResult.getDurationMs())
                .parseRate(parseRate)
                .explainEligible(explainEligible)
                .explainExecuted(explainExecuted)
                .explainCoverage(explainCoverage)
                .averageScore(avgScore)
                .categoryStats(toStatList(categoryCounts, parsedSql))
                .validityStats(toStatList(validityCounts, parsedSql))
                .explainEligibilityStats(toStatList(explainEligibilityCounts, parsedSql))
                .sourceTypeStats(toStatList(sourceTypeCounts, parsedSql))
                .staticIssueStats(toStatList(staticIssueCounts, staticIssueTotal))
                .explainIssueStats(toStatList(explainIssueCounts, explainIssueTotal))
                .generatedAt(Instant.now())
                .build();
    }

    private void increment(Map<String, Integer> counts, String key) {
        counts.put(key, counts.getOrDefault(key, 0) + 1);
    }

    private String formatLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            return "未知";
        }
        String normalized = raw.trim();
        String mapped = LABELS.get(normalized);
        if (mapped != null) {
            return mapped;
        }
        String[] parts = raw.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    private List<ReportStatItem> toStatList(Map<String, Integer> counts, int total) {
        List<ReportStatItem> items = new ArrayList<>();
        if (counts == null || counts.isEmpty()) {
            return items;
        }
        counts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(8)
                .forEach(entry -> {
                    double ratio = total == 0 ? 0.0 : entry.getValue() * 100.0 / total;
                    items.add(ReportStatItem.builder()
                            .label(entry.getKey())
                            .count(entry.getValue())
                            .ratio(ratio)
                            .build());
                });
        return items;
    }

    private int sumCounts(Map<String, Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (int value : counts.values()) {
            total += value;
        }
        return total;
    }

    @Override
    public void generateHtmlReportByScanId(String scanId, String outputPath) throws ScanException {
        // 简化实现，实际应从存储中获取 ScanResult
        ScanResult mockResult = ScanResult.builder()
                .scanId(scanId)
                .status(org.spectrum.sqlchecker.domain.shared.enumeration.ScanStatus.COMPLETED)
                .filesScanned(0)
                .sqlFound(0)
                .uniqueSqlFound(0)
                .durationMs(0)
                .sqlStatements(new java.util.ArrayList<>())
                .errors(new java.util.ArrayList<>())
                .build();

        generateHtmlReport(mockResult, outputPath);
    }
}
