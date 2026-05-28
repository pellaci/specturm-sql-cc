package org.spectrum.sqlchecker.infrastructure.report;

import org.spectrum.sqlchecker.application.analysis.dto.ExplainIssue;
import org.spectrum.sqlchecker.application.analysis.dto.PlanNode;
import org.spectrum.sqlchecker.application.analysis.dto.StaticIssue;
import org.spectrum.sqlchecker.application.report.dto.DiagnosticReport;
import org.spectrum.sqlchecker.application.scan.dto.SqlLocationDto;
import org.spectrum.sqlchecker.application.scan.dto.ScanResult;
import org.spectrum.sqlchecker.application.scan.dto.SqlStatementDto;
import org.spectrum.sqlchecker.domain.shared.enumeration.ExplainEligibility;
import org.spectrum.sqlchecker.domain.shared.enumeration.IssueType;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlType;
import org.spectrum.sqlchecker.domain.shared.enumeration.ValidityStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the stable diagnostic view model used by both HTML and JSON reports.
 */
public final class DiagnosticReportFactory {

    private static final String REPORT_VERSION = "1.0";

    private DiagnosticReportFactory() {
    }

    public static DiagnosticReport from(ScanResult scanResult) {
        ScanResult result = scanResult != null ? scanResult : ScanResult.builder().build();
        List<SqlStatementDto> sqlStatements = result.getSqlStatements() != null
                ? result.getSqlStatements()
                : List.of();

        List<DiagnosticReport.Finding> findings = sqlStatements.stream()
                .map(DiagnosticReportFactory::toFinding)
                .sorted(FINDING_PRIORITY)
                .toList();

        int totalIssues = countIssues(sqlStatements);
        int critical = countIssuesBySeverity(sqlStatements, SeverityLevel.CRITICAL);
        int warning = countIssuesBySeverity(sqlStatements, SeverityLevel.WARNING);
        int info = countIssuesBySeverity(sqlStatements, SeverityLevel.INFO);
        int totalSql = result.getSqlFound();
        int uniqueSql = result.getUniqueSqlFound() > 0 ? result.getUniqueSqlFound() : sqlStatements.size();
        int parsedSql = countParsedSql(sqlStatements);
        int parseScope = sqlStatements.size();
        int totalFiles = result.getTotalFiles() > 0 ? result.getTotalFiles() : result.getFilesScanned();
        int issueSql = countIssueSql(sqlStatements);
        int cleanSql = Math.max(0, uniqueSql - issueSql);
        double parseRate = parseScope == 0 ? 0.0 : round(Math.min(parsedSql, parseScope) * 100.0 / parseScope);
        int explainEligible = 0;
        int explainExecuted = 0;
        for (SqlStatementDto sql : sqlStatements) {
            if (sql.getExplainEligibility() == ExplainEligibility.SUPPORTED) {
                explainEligible++;
                if (hasExplainEvidence(sql)) {
                    explainExecuted++;
                }
            }
        }
        double explainCoverage = explainEligible == 0 ? 0.0 : round(explainExecuted * 100.0 / explainEligible);

        DiagnosticReport.Counts counts = DiagnosticReport.Counts.builder()
                .totalFiles(totalFiles)
                .totalSql(totalSql)
                .uniqueSql(uniqueSql)
                .issueSql(issueSql)
                .cleanSql(cleanSql)
                .totalIssues(totalIssues)
                .criticalIssues(critical)
                .warningIssues(warning)
                .infoIssues(info)
                .build();

        DiagnosticReport.Summary summary = DiagnosticReport.Summary.builder()
                .score(riskAdjustedScore(sqlStatements, critical, warning))
                .riskLevel(resolveRiskLevel(critical, warning, totalIssues))
                .counts(counts)
                .coverage(DiagnosticReport.Coverage.builder()
                        .parseRate(parseRate)
                        .explainCoverage(explainCoverage)
                        .build())
                .build();

        DiagnosticReport.Diagnostics diagnostics = DiagnosticReport.Diagnostics.builder()
                .parseFailures(parseFailures(sqlStatements))
                .skippedExplain(skippedExplain(sqlStatements))
                .manualReview(manualReview(sqlStatements))
                .configWarnings(explainFailures(sqlStatements))
                .build();
        DiagnosticReport.Confidence confidence = buildConfidence(summary, diagnostics);
        List<DiagnosticReport.RemediationCampaign> campaigns = buildCampaigns(findings);
        DiagnosticReport.Remediation remediation = buildRemediation(findings, campaigns);

        return DiagnosticReport.builder()
                .metadata(DiagnosticReport.Metadata.builder()
                        .reportVersion(REPORT_VERSION)
                        .generatedAt(Instant.now().toString())
                        .projectName(resolveProjectName(result.getScanPath()))
                        .scanPath(result.getScanPath())
                        .durationMs(result.getDurationMs())
                        .build())
                .summary(summary)
                .hotspots(DiagnosticReport.Hotspots.builder()
                        .byFile(topStatsByMax(countIssuesByFile(sqlStatements)))
                        .byRule(topStats(countByRule(sqlStatements), Math.max(1, totalIssues)))
                        .bySeverity(topSeverityStats(critical, warning, info, Math.max(1, totalIssues)))
                        .build())
                .insights(buildInsights(sqlStatements))
                .findings(findings)
                .diagnostics(diagnostics)
                .executiveSummary(buildExecutiveSummary(result, summary, confidence, diagnostics, sqlStatements))
                .campaigns(campaigns)
                .confidence(confidence)
                .methodology(defaultMethodology())
                .remediation(remediation)
                .build();
    }

    private static final Comparator<DiagnosticReport.Finding> FINDING_PRIORITY = Comparator
            .comparingInt((DiagnosticReport.Finding finding) -> severityRank(finding.getSeverity())).reversed()
            .thenComparing(Comparator.comparingInt(DiagnosticReportFactory::findingIssueCount).reversed())
            .thenComparingInt(DiagnosticReport.Finding::getScore)
            .thenComparing(finding -> safe(finding.getId(), ""));

    private static DiagnosticReport.Insights buildInsights(List<SqlStatementDto> sqlStatements) {
        return DiagnosticReport.Insights.builder()
                .duplicateSql(limit(duplicateSql(sqlStatements)))
                .parseFailures(limit(parseFailureInsights(sqlStatements)))
                .skippedExplain(limit(skippedExplainInsights(sqlStatements)))
                .dangerousDml(limit(dangerousDml(sqlStatements)))
                .potentialInjection(limit(potentialInjection(sqlStatements)))
                .fullScanOrNoIndex(limit(fullScanOrNoIndex(sqlStatements)))
                .build();
    }

    private static List<DiagnosticReport.RemediationCampaign> buildCampaigns(List<DiagnosticReport.Finding> findings) {
        List<DiagnosticReport.RemediationCampaign> campaigns = new ArrayList<>();
        addCampaign(campaigns, campaignForRule(
                "p0-dynamic-sql-safety",
                "P0",
                "SAFETY",
                "动态 SQL 安全止血",
                "集中处理 ${}、字符串拼接和潜在注入风险，先消除可导致越权查询或 SQL 注入的入口。",
                "STRONG",
                findings,
                List.of("SQL_INJECTION_RISK", "DYNAMIC_SQL"),
                List.of("把 ${} 改为参数绑定；动态列名和排序字段使用白名单映射。"),
                List.of("重新扫描并确认该战役 finding 数为 0。", "为动态排序/筛选分支补充单元或集成测试。")));
        addCampaign(campaigns, campaignForRule(
                "p1-unbounded-query-containment",
                "P1",
                "PERFORMANCE",
                "无边界查询和排序风险收敛",
                "处理 SELECT 无 WHERE、ORDER BY 无 LIMIT、潜在全表扫描等会扩大数据库负载的查询。",
                "MEDIUM",
                findings,
                List.of("SELECT_WITHOUT_WHERE", "ORDER_BY_WITHOUT_LIMIT", "MISSING_INDEX", "NO_INDEX_USED", "FULL_TABLE_SCAN"),
                List.of("补充可命中索引的过滤条件、分页边界，并用 EXPLAIN 验证访问路径。"),
                List.of("重新扫描确认 P1 高风险项下降。", "对关键查询执行 EXPLAIN 并记录访问类型、行数和索引命中。")));
        addCampaign(campaigns, campaignForRule(
                "p2-template-review",
                "P2",
                "MAINTAINABILITY",
                "模板解析与未知规则复核",
                "集中复核 UNKNOWN、SQL_SYNTAX_ERROR 等无法直接派修的模板问题，先把噪音转成明确规则或修复项。",
                "NEEDS_REVIEW",
                findings,
                List.of("UNKNOWN", "SQL_SYNTAX_ERROR"),
                List.of("定位 MyBatis 动态分支、数据库方言或占位符归一化问题，补齐最小样例后重新扫描。"),
                List.of("重新扫描并确认 UNKNOWN/SQL_SYNTAX_ERROR 已下降，或被归类到明确规则。")));
        return campaigns;
    }

    private static DiagnosticReport.Remediation buildRemediation(
            List<DiagnosticReport.Finding> findings,
            List<DiagnosticReport.RemediationCampaign> campaigns) {
        List<DiagnosticReport.RemediationTask> tasks = buildRemediationTasks(findings);
        int confirmed = countTasksByConfidence(tasks, "CONFIRMED");
        int likely = countTasksByConfidence(tasks, "LIKELY");
        int review = countTasksByConfidence(tasks, "NEEDS_REVIEW");
        return DiagnosticReport.Remediation.builder()
                .summary(DiagnosticReport.RemediationSummary.builder()
                        .campaignCount(campaigns.size())
                        .taskCount(tasks.size())
                        .confirmedTaskCount(confirmed)
                        .likelyTaskCount(likely)
                        .reviewTaskCount(review)
                        .estimatedFirstPassFocus(estimatedFirstPassFocus(tasks))
                        .build())
                .campaigns(campaigns)
                .tasks(tasks)
                .recipes(defaultRepairRecipes())
                .build();
    }

    private static List<DiagnosticReport.RemediationTask> buildRemediationTasks(List<DiagnosticReport.Finding> findings) {
        List<DiagnosticReport.RemediationTask> tasks = new ArrayList<>();
        for (DiagnosticReport.Finding finding : findings) {
            if (finding.getIssues() == null || finding.getIssues().isEmpty()) {
                continue;
            }
            DiagnosticReport.Issue primaryIssue = primaryIssue(finding);
            String primaryRule = safe(primaryIssue.getType(), "UNKNOWN");
            List<String> ruleTypes = ruleTypes(finding);
            String priority = remediationPriority(ruleTypes, finding.getSeverity());
            String theme = remediationTheme(ruleTypes);
            String confidence = remediationConfidence(primaryRule, finding);
            DiagnosticReport.Location location = firstLocation(finding);
            tasks.add(DiagnosticReport.RemediationTask.builder()
                    .id(remediationTaskId(finding, primaryRule))
                    .title(remediationTitle(finding, location, primaryRule))
                    .priority(priority)
                    .severity(finding.getSeverity())
                    .theme(theme)
                    .confidence(confidence)
                    .location(location)
                    .campaignId(campaignIdForTask(ruleTypes, theme))
                    .ruleTypes(ruleTypes)
                    .impact(remediationImpact(priority, theme))
                    .repairRecipeId(repairRecipeId(primaryRule, finding))
                    .recommendation(remediationRecommendation(primaryRule))
                    .acceptanceCheck(remediationAcceptanceCheck(priority, primaryRule))
                    .evidence(primaryIssue.getMessage())
                    .sql(finding.getSql())
                    .build());
        }
        return tasks;
    }

    private static DiagnosticReport.Issue primaryIssue(DiagnosticReport.Finding finding) {
        DiagnosticReport.Issue primary = finding.getIssues().get(0);
        int primaryRank = ruleRank(primary.getType());
        for (int i = 1; i < finding.getIssues().size(); i++) {
            DiagnosticReport.Issue candidate = finding.getIssues().get(i);
            int candidateRank = ruleRank(candidate.getType());
            if (candidateRank < primaryRank) {
                primary = candidate;
                primaryRank = candidateRank;
            }
        }
        return primary;
    }

    private static int ruleRank(String ruleType) {
        return switch (normalizeRule(ruleType)) {
            case "SQL_INJECTION_RISK" -> 0;
            case "DYNAMIC_SQL" -> 1;
            case "DANGEROUS_DROP_TRUNCATE" -> 2;
            case "DELETE_UPDATE_NO_WHERE" -> 3;
            case "SELECT_WITHOUT_WHERE" -> 4;
            case "ORDER_BY_WITHOUT_LIMIT" -> 5;
            case "FULL_TABLE_SCAN" -> 6;
            case "NO_INDEX_USED" -> 7;
            case "MISSING_INDEX" -> 8;
            case "SELECT_STAR" -> 9;
            case "UNKNOWN" -> 10;
            case "SQL_SYNTAX_ERROR" -> 11;
            default -> 100;
        };
    }

    private static int countTasksByConfidence(List<DiagnosticReport.RemediationTask> tasks, String confidence) {
        int count = 0;
        for (DiagnosticReport.RemediationTask task : tasks) {
            if (confidence.equals(task.getConfidence())) {
                count++;
            }
        }
        return count;
    }

    private static String estimatedFirstPassFocus(List<DiagnosticReport.RemediationTask> tasks) {
        if (tasks.isEmpty()) {
            return "暂无修复任务。";
        }
        for (DiagnosticReport.RemediationTask task : tasks) {
            if ("P0".equals(task.getPriority())) {
                return "优先处理 P0 安全止血任务，完成后重新扫描。";
            }
        }
        for (DiagnosticReport.RemediationTask task : tasks) {
            if ("P1".equals(task.getPriority())) {
                return "优先收敛 P1 无边界查询与索引风险。";
            }
        }
        return "优先复核模板和低风险正确性任务。";
    }

    private static List<String> ruleTypes(DiagnosticReport.Finding finding) {
        List<String> ruleTypes = new ArrayList<>();
        for (DiagnosticReport.Issue issue : finding.getIssues()) {
            String type = safe(issue.getType(), "UNKNOWN");
            if (!ruleTypes.contains(type)) {
                ruleTypes.add(type);
            }
        }
        return ruleTypes;
    }

    private static String remediationPriority(List<String> ruleTypes, String severity) {
        if (hasAnyRule(ruleTypes,
                "SQL_INJECTION_RISK",
                "DYNAMIC_SQL",
                "DANGEROUS_DROP_TRUNCATE",
                "DELETE_UPDATE_NO_WHERE")
                || "CRITICAL".equals(severity)) {
            return "P0";
        }
        if (hasAnyRule(ruleTypes,
                "SELECT_WITHOUT_WHERE",
                "ORDER_BY_WITHOUT_LIMIT",
                "MISSING_INDEX",
                "NO_INDEX_USED",
                "FULL_TABLE_SCAN")) {
            return "P1";
        }
        return "P2";
    }

    private static String remediationTheme(List<String> ruleTypes) {
        if (hasAnyRule(ruleTypes,
                "SQL_INJECTION_RISK",
                "DYNAMIC_SQL",
                "DANGEROUS_DROP_TRUNCATE",
                "DELETE_UPDATE_NO_WHERE")) {
            return "SAFETY";
        }
        if (hasAnyRule(ruleTypes,
                "SELECT_WITHOUT_WHERE",
                "ORDER_BY_WITHOUT_LIMIT",
                "MISSING_INDEX",
                "NO_INDEX_USED",
                "FULL_TABLE_SCAN")) {
            return "PERFORMANCE";
        }
        if (hasAnyRule(ruleTypes, "UNKNOWN", "SQL_SYNTAX_ERROR")) {
            return "MAINTAINABILITY";
        }
        return "CORRECTNESS";
    }

    private static String remediationConfidence(String primaryRule, DiagnosticReport.Finding finding) {
        boolean explainExecuted = finding.getExplain() != null && finding.getExplain().isExecuted();
        if (!explainExecuted && matchesRule(primaryRule,
                "UNKNOWN",
                "SQL_SYNTAX_ERROR",
                "SQL_INJECTION_RISK",
                "DYNAMIC_SQL")) {
            return "NEEDS_REVIEW";
        }
        if (explainExecuted) {
            return "CONFIRMED";
        }
        return "LIKELY";
    }

    private static DiagnosticReport.Location firstLocation(DiagnosticReport.Finding finding) {
        if (finding.getLocations() == null || finding.getLocations().isEmpty()) {
            return null;
        }
        return finding.getLocations().get(0);
    }

    private static String remediationTaskId(DiagnosticReport.Finding finding, String primaryRule) {
        return safe(finding.getId(), "sql") + "-remediation-" + normalizeRule(primaryRule).toLowerCase();
    }

    private static String remediationTitle(
            DiagnosticReport.Finding finding,
            DiagnosticReport.Location location,
            String primaryRule) {
        if (location != null) {
            String fileName = safe(location.getFileName(), safe(location.getFilePath(), "Unknown"));
            return fileName + ":" + location.getStartLine() + " · " + primaryRule;
        }
        return safe(finding.getSqlType(), "UNKNOWN") + " SQL · " + primaryRule;
    }

    private static String campaignIdForTask(List<String> ruleTypes, String theme) {
        if ("SAFETY".equals(theme) && hasAnyRule(ruleTypes, "SQL_INJECTION_RISK", "DYNAMIC_SQL")) {
            return "p0-dynamic-sql-safety";
        }
        if ("PERFORMANCE".equals(theme)) {
            return "p1-unbounded-query-containment";
        }
        if ("MAINTAINABILITY".equals(theme) && hasAnyRule(ruleTypes, "UNKNOWN", "SQL_SYNTAX_ERROR")) {
            return "p2-template-review";
        }
        return null;
    }

    private static String repairRecipeId(String primaryRule, DiagnosticReport.Finding finding) {
        if (matchesRule(primaryRule, "SQL_INJECTION_RISK")) {
            return "dynamic-value-binding";
        }
        if (matchesRule(primaryRule, "DYNAMIC_SQL")) {
            return containsDynamicOrderBy(finding) ? "dynamic-order-by-whitelist" : "dynamic-value-binding";
        }
        if (matchesRule(primaryRule,
                "SELECT_WITHOUT_WHERE",
                "ORDER_BY_WITHOUT_LIMIT",
                "MISSING_INDEX",
                "NO_INDEX_USED",
                "FULL_TABLE_SCAN")) {
            return "unbounded-query-containment";
        }
        if (matchesRule(primaryRule, "SELECT_STAR")) {
            return "select-star-field-list";
        }
        if (matchesRule(primaryRule, "DANGEROUS_DROP_TRUNCATE", "DELETE_UPDATE_NO_WHERE")) {
            return "dangerous-dml-guardrail";
        }
        if (matchesRule(primaryRule, "UNKNOWN", "SQL_SYNTAX_ERROR")) {
            return "template-review-normalization";
        }
        return "general-rule-remediation";
    }

    private static boolean containsDynamicOrderBy(DiagnosticReport.Finding finding) {
        DiagnosticReport.SqlText sql = finding.getSql();
        String text = sql != null ? safe(sql.getOriginal(), safe(sql.getNormalized(), safe(sql.getAbstracted(), ""))) : "";
        String normalized = text.toUpperCase();
        return normalized.contains("ORDER BY") && text.contains("${");
    }

    private static String remediationRecommendation(String primaryRule) {
        if (matchesRule(primaryRule, "SQL_INJECTION_RISK", "DYNAMIC_SQL")) {
            return "把动态值改为参数绑定；动态列名、排序字段使用白名单映射。";
        }
        if (matchesRule(primaryRule,
                "SELECT_WITHOUT_WHERE",
                "ORDER_BY_WITHOUT_LIMIT",
                "MISSING_INDEX",
                "NO_INDEX_USED",
                "FULL_TABLE_SCAN")) {
            return "补充业务过滤、分页边界或索引，并用 EXPLAIN 验证扫描范围。";
        }
        if (matchesRule(primaryRule, "SELECT_STAR")) {
            return "改为显式字段清单，只返回业务需要字段。";
        }
        if (matchesRule(primaryRule, "DANGEROUS_DROP_TRUNCATE", "DELETE_UPDATE_NO_WHERE")) {
            return "为危险 DML 增加 WHERE、影响行数校验和审批保护。";
        }
        if (matchesRule(primaryRule, "UNKNOWN", "SQL_SYNTAX_ERROR")) {
            return "先归一化模板或修正语法，再重新扫描归类。";
        }
        return "按规则建议完成最小化修复，并重新扫描确认。";
    }

    private static String remediationImpact(String priority, String theme) {
        if ("P0".equals(priority) && "SAFETY".equals(theme)) {
            return "可能导致 SQL 注入、越权读取或破坏性变更，需优先止血。";
        }
        if ("PERFORMANCE".equals(theme)) {
            return "可能扩大扫描范围或排序成本，影响数据库稳定性。";
        }
        if ("MAINTAINABILITY".equals(theme)) {
            return "模板或规则证据不足，需先复核以降低误报和漏报。";
        }
        return "可能导致查询结果或业务约束偏离预期。";
    }

    private static String remediationAcceptanceCheck(String priority, String primaryRule) {
        if ("P0".equals(priority)) {
            return "完成 P0 修复后重新扫描，确认 " + primaryRule + " 不再出现。";
        }
        if ("P1".equals(priority)) {
            return "重新扫描并补充 EXPLAIN 证据，确认 " + primaryRule + " 风险下降。";
        }
        return "重新扫描并确认 " + primaryRule + " 已修复或完成人工复核记录。";
    }

    private static boolean hasAnyRule(List<String> ruleTypes, String... candidates) {
        for (String ruleType : ruleTypes) {
            if (matchesRule(ruleType, candidates)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesRule(String ruleType, String... candidates) {
        String normalized = normalizeRule(ruleType);
        for (String candidate : candidates) {
            if (normalized.equals(normalizeRule(candidate))) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeRule(String ruleType) {
        return ruleType == null || ruleType.isBlank() ? "UNKNOWN" : ruleType.trim().toUpperCase();
    }

    private static List<DiagnosticReport.RepairRecipe> defaultRepairRecipes() {
        return List.of(
                repairRecipe(
                        "dynamic-value-binding",
                        "动态值改为参数绑定",
                        List.of("SQL_INJECTION_RISK", "DYNAMIC_SQL"),
                        "${value} 或字符串拼接直接进入 SQL",
                        "#{value} / ? 参数绑定；动态标识符走白名单",
                        List.of("定位动态片段来源。", "普通值改为参数绑定。", "列名、排序方向等标识符改为白名单映射。"),
                        "重新扫描确认 SQL_INJECTION_RISK/DYNAMIC_SQL 消失，并补充动态分支测试。",
                        List.of("表名、列名等标识符不能直接参数绑定，必须白名单。")),
                repairRecipe(
                        "dynamic-order-by-whitelist",
                        "动态排序白名单",
                        List.of("DYNAMIC_SQL", "SQL_INJECTION_RISK"),
                        "ORDER BY ${orderBy}",
                        "ORDER BY <mapped_column> <mapped_direction>",
                        List.of("枚举允许排序字段。", "枚举 ASC/DESC。", "默认值落到安全排序。"),
                        "重新扫描并覆盖非法排序字段回退分支。",
                        List.of("排序字段来自业务枚举时仍需处理默认分支。")),
                repairRecipe(
                        "unbounded-query-containment",
                        "无边界查询收敛",
                        List.of("SELECT_WITHOUT_WHERE", "ORDER_BY_WITHOUT_LIMIT", "MISSING_INDEX", "NO_INDEX_USED", "FULL_TABLE_SCAN"),
                        "缺少 WHERE/LIMIT 或无法命中索引",
                        "业务过滤 + 分页边界 + 可验证索引路径",
                        List.of("补充业务过滤条件。", "增加分页或 LIMIT 上限。", "用 EXPLAIN 验证访问路径和扫描行数。"),
                        "重新扫描并记录 EXPLAIN 访问类型、索引和 rows 下降。",
                        List.of("离线批处理全量读取需要显式说明业务边界。")),
                repairRecipe(
                        "select-star-field-list",
                        "SELECT * 改为字段清单",
                        List.of("SELECT_STAR"),
                        "SELECT *",
                        "SELECT id, status, updated_at",
                        List.of("确认调用方实际读取字段。", "替换为稳定字段清单。", "回归序列化和映射结果。"),
                        "重新扫描确认 SELECT_STAR 消失。",
                        List.of("字段清单需随业务 DTO 演进同步维护。")),
                repairRecipe(
                        "dangerous-dml-guardrail",
                        "危险 DML 防护",
                        List.of("DANGEROUS_DROP_TRUNCATE", "DELETE_UPDATE_NO_WHERE"),
                        "DROP/TRUNCATE 或无 WHERE UPDATE/DELETE",
                        "审批保护 + WHERE 边界 + 影响行数校验",
                        List.of("确认语句是否允许在线执行。", "补充 WHERE 或迁移审批保护。", "增加影响行数阈值和回滚预案。"),
                        "重新扫描确认危险 DML 规则消失或完成审批记录。",
                        List.of("结构变更类 SQL 可能需要迁移系统单独治理。")),
                repairRecipe(
                        "template-review-normalization",
                        "模板归一化复核",
                        List.of("UNKNOWN", "SQL_SYNTAX_ERROR"),
                        "动态模板或方言语法无法稳定解析",
                        "最小样例 + 模板归一化 + 明确规则归类",
                        List.of("抽取最小 SQL 样例。", "修正模板占位符或数据库方言。", "重新扫描并归类到明确规则。"),
                        "重新扫描确认 UNKNOWN/SQL_SYNTAX_ERROR 下降或已有人工复核结论。",
                        List.of("复杂模板可能需要结合运行时参数人工确认。")),
                repairRecipe(
                        "general-rule-remediation",
                        "通用规则修复",
                        List.of("KNOWN_RULE"),
                        "已识别规则暂无专用修复模板",
                        "按规则建议做最小化修复并保留验证证据",
                        List.of("确认规则消息和 SQL 位置。", "按规则建议完成最小化修改。", "重新扫描确认该规则不再触发。"),
                        "重新扫描确认规则消失，必要时补充 SQL 单元或集成测试。",
                        List.of("复杂 SQL 可能需要结合业务语义决定最终改写方式。"))
        );
    }

    private static DiagnosticReport.RepairRecipe repairRecipe(
            String id,
            String title,
            List<String> appliesToRules,
            String unsafePattern,
            String safePattern,
            List<String> steps,
            String verification,
            List<String> knownLimits) {
        return DiagnosticReport.RepairRecipe.builder()
                .id(id)
                .title(title)
                .appliesToRules(appliesToRules)
                .unsafePattern(unsafePattern)
                .safePattern(safePattern)
                .steps(steps)
                .verification(verification)
                .knownLimits(knownLimits)
                .build();
    }

    private static DiagnosticReport.RemediationCampaign campaignForRule(
            String id,
            String priority,
            String theme,
            String title,
            String summary,
            String evidenceLevel,
            List<DiagnosticReport.Finding> findings,
            List<String> rules,
            List<String> recommendations,
            List<String> checklist) {
        List<DiagnosticReport.Finding> matched = findings.stream()
                .filter(finding -> finding.getIssues() != null && finding.getIssues().stream()
                        .anyMatch(issue -> rules.contains(issue.getType())))
                .toList();
        return matched.isEmpty() ? null : campaign(id, priority, theme, title, summary, evidenceLevel, matched, recommendations, checklist);
    }

    private static DiagnosticReport.RemediationCampaign campaign(
            String id,
            String priority,
            String theme,
            String title,
            String summary,
            String evidenceLevel,
            List<DiagnosticReport.Finding> findings,
            List<String> recommendations,
            List<String> checklist) {
        List<String> files = findings.stream()
                .flatMap(finding -> finding.getLocations() != null ? finding.getLocations().stream() : List.<DiagnosticReport.Location>of().stream())
                .map(DiagnosticReport.Location::getFilePath)
                .filter(path -> path != null && !path.isBlank())
                .distinct()
                .limit(6)
                .toList();
        List<String> examples = findings.stream()
                .map(DiagnosticReport.Finding::getId)
                .limit(5)
                .toList();
        return DiagnosticReport.RemediationCampaign.builder()
                .id(id)
                .priority(priority)
                .theme(theme)
                .title(title)
                .summary(summary)
                .scope(DiagnosticReport.CampaignScope.builder()
                        .sqlCount(findings.size())
                        .fileCount(files.size())
                        .files(files)
                        .examples(examples)
                        .build())
                .evidenceLevel(evidenceLevel)
                .recommendations(recommendations)
                .acceptanceChecklist(checklist)
                .findingIds(findings.stream().map(DiagnosticReport.Finding::getId).toList())
                .build();
    }

    private static void addCampaign(List<DiagnosticReport.RemediationCampaign> campaigns,
                                    DiagnosticReport.RemediationCampaign campaign) {
        if (campaign != null) {
            campaigns.add(campaign);
        }
    }

    private static DiagnosticReport.ExecutiveSummary buildExecutiveSummary(
            ScanResult result,
            DiagnosticReport.Summary summary,
            DiagnosticReport.Confidence confidence,
            DiagnosticReport.Diagnostics diagnostics,
            List<SqlStatementDto> sqlStatements) {
        List<String> topRules = countByRule(sqlStatements).entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .map(entry -> entry.getKey() + " × " + entry.getValue())
                .toList();

        List<String> actions = new ArrayList<>();
        if (hasRule(sqlStatements, IssueType.SQL_INJECTION_RISK.name())) {
            actions.add("P0: 先处理动态 SQL 安全止血，替换 ${} 或字符串拼接。");
        }
        if (hasRule(sqlStatements, IssueType.SELECT_WITHOUT_WHERE.name())
                || hasRule(sqlStatements, IssueType.ORDER_BY_WITHOUT_LIMIT.name())) {
            actions.add("P1: 收敛无边界读取和排序分页风险。");
        }
        if (actions.isEmpty()) {
            actions.add("P2: 保留当前报告作为治理基线，持续关注人工复核项。");
        }

        String projectName = resolveProjectName(result.getScanPath());
        String conclusion = projectName + " 当前 SQL 风险等级为 " + summary.getRiskLevel()
                + "，共发现 " + summary.getCounts().getTotalIssues() + " 个问题，建议按修复战役推进。";
        return DiagnosticReport.ExecutiveSummary.builder()
                .riskConclusion(conclusion)
                .topDrivers(topRules)
                .recommendedActions(actions)
                .confidenceSummary("证据可信度: " + confidence.getLevel()
                        + " · 人工复核 " + diagnostics.getManualReview().size()
                        + " · EXPLAIN 未执行 " + diagnostics.getSkippedExplain().size())
                .build();
    }

    private static DiagnosticReport.Confidence buildConfidence(
            DiagnosticReport.Summary summary,
            DiagnosticReport.Diagnostics diagnostics) {
        List<String> sources = new ArrayList<>();
        sources.add("Static AST rules");
        if (summary.getCoverage().getExplainCoverage() > 0) {
            sources.add("EXPLAIN evidence");
        }

        List<String> limits = new ArrayList<>();
        if (!diagnostics.getSkippedExplain().isEmpty()) {
            limits.add("未配置或未执行 EXPLAIN，性能判断以静态规则为主。");
        }
        if (!diagnostics.getManualReview().isEmpty()) {
            limits.add("动态模板、解析异常或 EXPLAIN 异常需要人工复核。");
        }
        if (!diagnostics.getParseFailures().isEmpty()) {
            limits.add("部分 SQL 需要修正模板或占位符后再进入精确诊断。");
        }

        String level = diagnostics.getParseFailures().isEmpty()
                && diagnostics.getManualReview().isEmpty()
                && diagnostics.getSkippedExplain().isEmpty()
                && diagnostics.getConfigWarnings().isEmpty()
                ? "STRONG"
                : "NEEDS_REVIEW";
        return DiagnosticReport.Confidence.builder()
                .level(level)
                .evidenceSources(sources)
                .limitations(limits)
                .build();
    }

    private static boolean hasRule(List<SqlStatementDto> sqlStatements, String rule) {
        for (SqlStatementDto sql : sqlStatements) {
            if (containsIssue(sql, rule)) {
                return true;
            }
        }
        return false;
    }

    private static DiagnosticReport.Methodology defaultMethodology() {
        return DiagnosticReport.Methodology.builder()
                .scoring(List.of("Score starts from SQL-level diagnostic scores and is capped by critical or warning issue concentration."))
                .severityDefinitions(List.of(
                        "CRITICAL: safety, destructive DML, injection, or production-impacting risk.",
                        "WARNING: performance, maintainability, or correctness risk that should enter remediation planning.",
                        "INFO: evidence or optimization signal that does not by itself block release."))
                .coverageDefinitions(List.of(
                        "Parse coverage measures SQL statements accepted by preprocessing and classification.",
                        "EXPLAIN coverage measures eligible SQL with successful execution-plan evidence.",
                        "Manual review marks dynamic templates, skipped EXPLAIN, and evidence gaps."))
                .knownLimits(List.of(
                        "Static analysis cannot prove runtime parameter values.",
                        "Database-free reports do not include real optimizer evidence.",
                        "Dynamic SQL templates may require manual review even when parse coverage is 100%."))
                .build();
    }

    private static DiagnosticReport.Finding toFinding(SqlStatementDto sql) {
        List<DiagnosticReport.Issue> issues = new ArrayList<>();
        if (sql.getStaticAnalysis() != null && sql.getStaticAnalysis().getIssues() != null) {
            for (StaticIssue issue : sql.getStaticAnalysis().getIssues()) {
                String issueType = issue.getType() != null ? issue.getType().name() : "UNKNOWN";
                issues.add(DiagnosticReport.Issue.builder()
                        .source("STATIC")
                        .type(issueType)
                        .severity(formatSeverity(issue.getSeverity()))
                        .message(issue.getMessage())
                        .suggestion(safeRecommendation(issue.getSuggestion(), issue.getType(), issue.getMessage()))
                        .evidence(issue.getLocation())
                        .build());
            }
        }
        if (sql.getExplainAnalysis() != null && sql.getExplainAnalysis().getIssues() != null) {
            for (ExplainIssue issue : sql.getExplainAnalysis().getIssues()) {
                issues.add(DiagnosticReport.Issue.builder()
                        .source("EXPLAIN")
                        .type(issue.getType())
                        .severity(formatSeverity(issue.getSeverity()))
                        .message(issue.getMessage())
                        .suggestion(safeRecommendation(issue.getSuggestion(), null, issue.getMessage()))
                        .evidence(issue.getTableName())
                        .build());
            }
        }

        List<String> recommendations = issues.stream()
                .map(DiagnosticReport.Issue::getSuggestion)
                .filter(suggestion -> suggestion != null && !suggestion.isBlank())
                .distinct()
                .toList();

        return DiagnosticReport.Finding.builder()
                .id(sql.getId())
                .severity(formatSeverity(sql.getSeverity()))
                .category(sql.getCategory() != null ? sql.getCategory().name() : "UNKNOWN")
                .sqlType(sql.getSqlType() != null ? sql.getSqlType().name() : "UNKNOWN")
                .score(sql.getScore() != null ? sql.getScore() : 100)
                .locations(toLocations(sql.getLocations()))
                .sql(DiagnosticReport.SqlText.builder()
                        .original(sql.getOriginalSql())
                        .normalized(sql.getNormalizedSql())
                        .abstracted(sql.getAbstractSql())
                        .explain(sql.getExplainSql())
                        .build())
                .issues(issues)
                .recommendations(recommendations)
                .explain(toExplain(sql))
                .build();
    }

    private static DiagnosticReport.Explain toExplain(SqlStatementDto sql) {
        List<String> planSummary = new ArrayList<>();
        if (sql.getExplainAnalysis() != null
                && sql.getExplainAnalysis().getPlan() != null
                && sql.getExplainAnalysis().getPlan().getNodes() != null) {
            for (PlanNode node : sql.getExplainAnalysis().getPlan().getNodes()) {
                String table = node.getTable() != null ? node.getTable() : "<unknown>";
                String type = node.getType() != null ? node.getType() : "unknown";
                String rows = node.getRows() != null ? String.valueOf(node.getRows()) : "?";
                planSummary.add(table + " / " + type + " / rows=" + rows);
            }
        }
        int issueCount = sql.getExplainAnalysis() != null && sql.getExplainAnalysis().getIssues() != null
                ? sql.getExplainAnalysis().getIssues().size()
                : 0;
        return DiagnosticReport.Explain.builder()
                .eligibility(sql.getExplainEligibility() != null ? sql.getExplainEligibility().name() : "UNKNOWN")
                .executed(hasExplainEvidence(sql))
                .durationMs(sql.getExplainAnalysis() != null ? sql.getExplainAnalysis().getDurationMs() : 0)
                .issueCount(issueCount)
                .planSummary(planSummary)
                .build();
    }

    private static List<DiagnosticReport.Location> toLocations(List<SqlLocationDto> locations) {
        if (locations == null || locations.isEmpty()) {
            return List.of();
        }
        return locations.stream()
                .map(location -> DiagnosticReport.Location.builder()
                        .filePath(location.getFilePath())
                        .fileName(location.getFileName())
                        .startLine(location.getStartLine())
                        .endLine(location.getEndLine())
                        .sourceType(location.getSourceType() != null ? location.getSourceType().name() : "UNKNOWN")
                        .build())
                .toList();
    }

    private static String safeRecommendation(String suggestion, IssueType issueType, String message) {
        if (suggestion != null && !suggestion.isBlank()) {
            return suggestion;
        }
        if (issueType == null) {
            return defaultRecommendationByMessage(message);
        }
        return switch (issueType) {
            case SQL_INJECTION_RISK, DYNAMIC_SQL -> "把 ${} 或字符串拼接改为参数绑定；动态列名、排序字段必须走白名单映射。";
            case SELECT_WITHOUT_WHERE -> "补充可命中索引的 WHERE 条件；确需全量读取时增加 LIMIT/分页并在代码中说明业务边界。";
            case ORDER_BY_WITHOUT_LIMIT -> "补充 LIMIT/分页边界；如果用于离线全量排序，需要说明业务上限并确认排序字段索引。";
            case SELECT_STAR -> "改为显式列清单，只返回业务需要字段，避免字段变更和大字段传输拖慢查询。";
            case MISSING_INDEX -> "确认 WHERE/JOIN/ORDER BY 字段组合，为高频过滤条件补充单列或联合索引，并用 EXPLAIN 验证。";
            case SQL_SYNTAX_ERROR -> "先修正 SQL 结构错误，再用数据库方言解析或最小样例执行验证。";
            case LIKE_LEADING_WILDCARD -> "避免前置通配符；可改为后缀匹配、全文索引、反向索引或搜索服务。";
            case CROSS_JOIN -> "改为显式 JOIN ... ON，并确认关联条件能限制结果集规模。";
            case SUBQUERY_IN_SELECT, UNCORRELATED_SUBQUERY -> "评估改写为 JOIN、EXISTS 或 CTE，减少重复执行和中间结果膨胀。";
            case POTENTIAL_N_PLUS_ONE -> "批量预取关联数据，或把循环内查询改为一次 IN/JOIN 查询。";
            case IMPLICIT_TYPE_CONVERSION -> "确保比较两侧类型一致，必要时调整字段类型或入参类型，避免索引失效。";
            case SUSPICIOUS_JOIN_ORDER -> "检查 JOIN 顺序和驱动表选择，把过滤性强的小结果集放在前面。";
            case TOO_MANY_JOINS -> "拆分查询或沉淀中间表/视图，降低单条 SQL 的 JOIN 复杂度。";
            case UNKNOWN -> "先确认规则来源和触发条件，再按原始 SQL 与位置复核是否需要修复。";
        };
    }

    private static String defaultRecommendationByMessage(String message) {
        String text = message != null ? message : "";
        if (text.contains("全表扫描")) {
            return "补充过滤条件或索引，并用 EXPLAIN 确认访问类型不再是全表扫描。";
        }
        if (text.contains("索引")) {
            return "按过滤、关联和排序字段补充合适索引，并验证扫描行数下降。";
        }
        return "结合位置、SQL 和证据复核风险，补充最小化修复并重新扫描确认。";
    }

    private static List<DiagnosticReport.InsightItem> duplicateSql(List<SqlStatementDto> sqlStatements) {
        List<DiagnosticReport.InsightItem> items = new ArrayList<>();
        for (SqlStatementDto sql : sqlStatements) {
            int locationCount = sql.getLocations() != null ? sql.getLocations().size() : 0;
            if (locationCount > 1) {
                items.add(insight(sql, "重复 SQL 出现在 " + locationCount + " 处", locationCount,
                        formatSeverity(sql.getSeverity()), safe(sql.getAbstractSql(), sql.getOriginalSql())));
            }
        }
        return items;
    }

    private static List<DiagnosticReport.InsightItem> parseFailureInsights(List<SqlStatementDto> sqlStatements) {
        List<DiagnosticReport.InsightItem> items = new ArrayList<>();
        for (SqlStatementDto sql : sqlStatements) {
            if (sql.getValidity() != null && "INVALID".equals(sql.getValidity().name())) {
                items.add(insight(sql, parseFailureTitle(sql), 1, formatSeverity(sql.getSeverity()),
                        safe(sql.getPreprocessErrorReason(), "SQL 无法解析")));
            }
        }
        return items.stream()
                .sorted(Comparator.comparingInt(item -> parseFailureRank(item.getTitle())))
                .toList();
    }

    private static List<DiagnosticReport.InsightItem> skippedExplainInsights(List<SqlStatementDto> sqlStatements) {
        List<DiagnosticReport.InsightItem> items = new ArrayList<>();
        for (SqlStatementDto sql : sqlStatements) {
            if (issueCount(sql) > 0
                    && sql.getExplainEligibility() != null
                    && sql.getExplainEligibility() != ExplainEligibility.SUPPORTED) {
                items.add(insight(sql, "EXPLAIN 未执行", 1, formatSeverity(sql.getSeverity()),
                        explainSkipReason(sql)));
            }
        }
        return items;
    }

    private static List<DiagnosticReport.InsightItem> dangerousDml(List<SqlStatementDto> sqlStatements) {
        List<DiagnosticReport.InsightItem> items = new ArrayList<>();
        for (SqlStatementDto sql : sqlStatements) {
            if (isDangerousDml(sql)) {
                items.add(insight(sql, "危险 DML", 1, formatSeverity(sql.getSeverity()),
                        "优先确认影响范围和 WHERE 条件"));
            }
        }
        return items;
    }

    private static List<DiagnosticReport.InsightItem> potentialInjection(List<SqlStatementDto> sqlStatements) {
        List<DiagnosticReport.InsightItem> items = new ArrayList<>();
        for (SqlStatementDto sql : sqlStatements) {
            if (containsIssue(sql, "SQL_INJECTION_RISK") || containsIssue(sql, "DYNAMIC_SQL")
                    || (sql.getOriginalSql() != null && sql.getOriginalSql().contains("${"))) {
                items.add(insight(sql, "潜在注入", 1, formatSeverity(sql.getSeverity()),
                        "优先替换动态拼接，改用参数绑定或白名单映射"));
            }
        }
        return items;
    }

    private static List<DiagnosticReport.InsightItem> fullScanOrNoIndex(List<SqlStatementDto> sqlStatements) {
        List<DiagnosticReport.InsightItem> items = new ArrayList<>();
        for (SqlStatementDto sql : sqlStatements) {
            if (containsIssue(sql, "FULL_TABLE_SCAN") || containsIssue(sql, "NO_INDEX") || containsIssue(sql, "MISSING_INDEX")) {
                items.add(insight(sql, "全表扫描 / 无索引", 1, formatSeverity(sql.getSeverity()),
                        "结合表规模和过滤列补充索引或收窄扫描范围"));
            }
        }
        return items;
    }

    private static DiagnosticReport.InsightItem insight(SqlStatementDto sql, String title, int count, String severity, String evidence) {
        return DiagnosticReport.InsightItem.builder()
                .id(sql.getId())
                .title(title)
                .count(count)
                .severity(severity)
                .evidence(evidence)
                .locations(locationLabels(sql.getLocations()))
                .build();
    }

    private static List<String> locationLabels(List<SqlLocationDto> locations) {
        if (locations == null || locations.isEmpty()) {
            return List.of();
        }
        return locations.stream()
                .map(location -> (location.getFilePath() != null ? location.getFilePath() : location.getFileName())
                        + ":" + location.getStartLine())
                .toList();
    }

    private static List<DiagnosticReport.InsightItem> limit(List<DiagnosticReport.InsightItem> items) {
        if (items == null || items.size() <= 6) {
            return items == null ? List.of() : items;
        }
        return items.subList(0, 6);
    }

    private static boolean isDangerousDml(SqlStatementDto sql) {
        SqlType type = sql.getSqlType();
        if (type != SqlType.UPDATE && type != SqlType.DELETE) {
            return false;
        }
        String normalized = sql.getNormalizedSql() != null ? sql.getNormalizedSql() : sql.getOriginalSql();
        if (normalized == null || !normalized.toUpperCase().contains(" WHERE ")) {
            return true;
        }
        return containsIssue(sql, IssueType.SELECT_WITHOUT_WHERE.name());
    }

    private static boolean containsIssue(SqlStatementDto sql, String issueType) {
        if (issueType == null) {
            return false;
        }
        if (sql.getStaticAnalysis() != null && sql.getStaticAnalysis().getIssues() != null) {
            for (StaticIssue issue : sql.getStaticAnalysis().getIssues()) {
                if (issue.getType() != null && issueType.equalsIgnoreCase(issue.getType().name())) {
                    return true;
                }
            }
        }
        if (sql.getExplainAnalysis() != null && sql.getExplainAnalysis().getIssues() != null) {
            for (ExplainIssue issue : sql.getExplainAnalysis().getIssues()) {
                if (issue.getType() != null && issueType.equalsIgnoreCase(issue.getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int countIssuesBySeverity(List<SqlStatementDto> sqlStatements, SeverityLevel severity) {
        int count = 0;
        for (SqlStatementDto sql : sqlStatements) {
            if (sql.getStaticAnalysis() != null && sql.getStaticAnalysis().getIssues() != null) {
                for (StaticIssue issue : sql.getStaticAnalysis().getIssues()) {
                    if (issue.getSeverity() == severity) {
                        count++;
                    }
                }
            }
            if (sql.getExplainAnalysis() != null && sql.getExplainAnalysis().getIssues() != null) {
                for (ExplainIssue issue : sql.getExplainAnalysis().getIssues()) {
                    if (issue.getSeverity() == severity) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static int countIssues(List<SqlStatementDto> sqlStatements) {
        int count = 0;
        for (SqlStatementDto sql : sqlStatements) {
            count += issueCount(sql);
        }
        return count;
    }

    private static int countParsedSql(List<SqlStatementDto> sqlStatements) {
        int count = 0;
        for (SqlStatementDto sql : sqlStatements) {
            if (sql.getValidity() != null) {
                count++;
            }
        }
        return count;
    }

    private static int issueCount(SqlStatementDto sql) {
        int count = 0;
        if (sql.getStaticAnalysis() != null && sql.getStaticAnalysis().getIssues() != null) {
            count += sql.getStaticAnalysis().getIssues().size();
        }
        if (sql.getExplainAnalysis() != null && sql.getExplainAnalysis().getIssues() != null) {
            count += sql.getExplainAnalysis().getIssues().size();
        }
        return count;
    }

    private static int countIssueSql(List<SqlStatementDto> sqlStatements) {
        int count = 0;
        for (SqlStatementDto sql : sqlStatements) {
            if (issueCount(sql) > 0) {
                count++;
            }
        }
        return count;
    }

    private static int averageScore(List<SqlStatementDto> sqlStatements) {
        if (sqlStatements == null || sqlStatements.isEmpty()) {
            return 100;
        }
        int total = 0;
        for (SqlStatementDto sql : sqlStatements) {
            total += sql.getScore() != null ? sql.getScore() : 100;
        }
        return Math.round((float) total / sqlStatements.size());
    }

    private static int riskAdjustedScore(List<SqlStatementDto> sqlStatements, int critical, int warning) {
        int score = averageScore(sqlStatements);
        if (critical >= 20) {
            return Math.min(score, 59);
        }
        if (critical >= 5) {
            return Math.min(score, 69);
        }
        if (critical > 0) {
            return Math.min(score, 79);
        }
        if (warning >= 10) {
            return Math.min(score, 84);
        }
        if (warning > 0) {
            return Math.min(score, 89);
        }
        return score;
    }

    private static String resolveRiskLevel(int critical, int warning, int totalIssues) {
        if (critical > 0) {
            return "CRITICAL";
        }
        if (warning > 0) {
            return "WARNING";
        }
        return totalIssues > 0 ? "INFO" : "HEALTHY";
    }

    private static Map<String, Integer> countIssuesByFile(List<SqlStatementDto> sqlStatements) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (SqlStatementDto sql : sqlStatements) {
            int issues = issueCount(sql);
            if (issues == 0) {
                continue;
            }
            if (sql.getLocations() == null || sql.getLocations().isEmpty()) {
                increment(counts, "Unknown", issues);
                continue;
            }
            for (SqlLocationDto location : sql.getLocations()) {
                increment(counts, location.getFilePath() != null ? location.getFilePath() : location.getFileName(), issues);
            }
        }
        return counts;
    }

    private static Map<String, Integer> countByRule(List<SqlStatementDto> sqlStatements) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (SqlStatementDto sql : sqlStatements) {
            if (sql.getStaticAnalysis() != null && sql.getStaticAnalysis().getIssues() != null) {
                for (StaticIssue issue : sql.getStaticAnalysis().getIssues()) {
                    increment(counts, issue.getType() != null ? issue.getType().name() : "UNKNOWN");
                }
            }
            if (sql.getExplainAnalysis() != null && sql.getExplainAnalysis().getIssues() != null) {
                for (ExplainIssue issue : sql.getExplainAnalysis().getIssues()) {
                    increment(counts, issue.getType() != null ? issue.getType() : "UNKNOWN");
                }
            }
        }
        return counts;
    }

    private static List<DiagnosticReport.StatItem> topStats(Map<String, Integer> counts, int total) {
        if (counts == null || counts.isEmpty()) {
            return List.of();
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(8)
                .map(entry -> DiagnosticReport.StatItem.builder()
                        .label(entry.getKey())
                        .count(entry.getValue())
                        .ratio(total == 0 ? 0.0 : round(entry.getValue() * 100.0 / total))
                        .severity(null)
                        .build())
                .toList();
    }

    private static List<DiagnosticReport.StatItem> topStatsByMax(Map<String, Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            return List.of();
        }
        int max = counts.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
        return topStats(counts, Math.max(1, max));
    }

    private static List<DiagnosticReport.StatItem> topSeverityStats(int critical, int warning, int info, int total) {
        List<DiagnosticReport.StatItem> stats = new ArrayList<>();
        stats.add(stat("CRITICAL", critical, total, "CRITICAL"));
        stats.add(stat("WARNING", warning, total, "WARNING"));
        stats.add(stat("INFO", info, total, "INFO"));
        return stats;
    }

    private static DiagnosticReport.StatItem stat(String label, int count, int total, String severity) {
        return DiagnosticReport.StatItem.builder()
                .label(label)
                .count(count)
                .ratio(total == 0 ? 0.0 : round(count * 100.0 / total))
                .severity(severity)
                .build();
    }

    private static List<String> parseFailures(List<SqlStatementDto> sqlStatements) {
        List<String> failures = new ArrayList<>();
        for (SqlStatementDto sql : sqlStatements) {
            if (sql.getValidity() != null && "INVALID".equals(sql.getValidity().name())) {
                failures.add(describeSql(sql) + ": " + parseFailureTitle(sql) + " - "
                        + safe(sql.getPreprocessErrorReason(), "SQL 无法解析"));
            }
        }
        return failures.stream()
                .sorted(Comparator.comparingInt(DiagnosticReportFactory::parseFailureDiagnosticRank))
                .toList();
    }

    private static int parseFailureDiagnosticRank(String diagnostic) {
        if (diagnostic == null) {
            return 99;
        }
        if (diagnostic.contains("动态拼接待确认")) {
            return 0;
        }
        if (diagnostic.contains("模板归一化失败")) {
            return 1;
        }
        if (diagnostic.contains("MyBatis 模板待确认")) {
            return 2;
        }
        return 3;
    }

    private static int parseFailureRank(String title) {
        if ("动态拼接待确认".equals(title)) {
            return 0;
        }
        if ("模板归一化失败".equals(title)) {
            return 1;
        }
        if ("MyBatis 模板待确认".equals(title)) {
            return 2;
        }
        return 3;
    }

    private static String parseFailureTitle(SqlStatementDto sql) {
        String original = safe(sql.getOriginalSql(), "");
        String normalized = safe(sql.getNormalizedSql(), "");
        String reason = safe(sql.getPreprocessErrorReason(), "");
        if (original.contains("${") || normalized.contains("${") || reason.contains("${}")) {
            return "动态拼接待确认";
        }
        if (containsTemplateFragmentPlaceholder(original) || containsTemplateFragmentPlaceholder(normalized)) {
            return "模板归一化失败";
        }
        if (sql.getCategory() != null && sql.getCategory().name().startsWith("MYBATIS")) {
            return "MyBatis 模板待确认";
        }
        return "SQL 语法错误";
    }

    private static boolean containsTemplateFragmentPlaceholder(String sql) {
        if (sql == null || sql.isBlank()) {
            return false;
        }
        String normalized = sql.replaceAll("\\s+", " ").toUpperCase();
        return normalized.contains("WHERE ?")
                || normalized.contains("VALUES ?")
                || normalized.contains("CASE ?")
                || normalized.contains(" SET ?")
                || normalized.contains(" AND ?")
                || normalized.contains(" OR ?");
    }

    private static List<String> skippedExplain(List<SqlStatementDto> sqlStatements) {
        List<String> skipped = new ArrayList<>();
        for (SqlStatementDto sql : sqlStatements) {
            if (sql.getExplainEligibility() != null && sql.getExplainEligibility() != ExplainEligibility.SUPPORTED) {
                skipped.add(describeSql(sql) + ": " + explainSkipReason(sql));
            }
        }
        return skipped;
    }

    private static List<String> manualReview(List<SqlStatementDto> sqlStatements) {
        List<String> reviewItems = new ArrayList<>();
        for (SqlStatementDto sql : sqlStatements) {
            String reason = manualReviewReason(sql);
            if (reason != null && !reason.isBlank()) {
                reviewItems.add(describeSql(sql) + ": " + reason);
            }
        }
        return reviewItems;
    }

    private static String manualReviewReason(SqlStatementDto sql) {
        if (sql.getValidity() == ValidityStatus.INVALID) {
            return parseFailureTitle(sql) + " - " + safe(sql.getPreprocessErrorReason(), "SQL 无法解析");
        }
        if (sql.getValidity() == ValidityStatus.UNKNOWN) {
            return "解析状态未知 - " + safe(sql.getPreprocessErrorReason(), "需要人工确认 SQL 模板和占位符");
        }
        if (hasExplainFailure(sql)) {
            return "EXPLAIN 失败 - " + sql.getExplainAnalysis().getErrorMessage();
        }
        if (containsIssue(sql, IssueType.SQL_INJECTION_RISK.name())
                || containsIssue(sql, IssueType.DYNAMIC_SQL.name())
                || safe(sql.getOriginalSql(), "").contains("${")
                || safe(sql.getNormalizedSql(), "").contains("${")) {
            return "动态模板需确认白名单或参数绑定边界";
        }
        return null;
    }

    private static List<String> explainFailures(List<SqlStatementDto> sqlStatements) {
        List<String> failures = new ArrayList<>();
        for (SqlStatementDto sql : sqlStatements) {
            if (hasExplainFailure(sql)) {
                failures.add(describeSql(sql) + ": " + sql.getExplainAnalysis().getErrorMessage());
            }
        }
        return failures;
    }

    private static boolean hasExplainFailure(SqlStatementDto sql) {
        return sql.getExplainAnalysis() != null
                && sql.getExplainAnalysis().getErrorMessage() != null
                && !sql.getExplainAnalysis().getErrorMessage().isBlank();
    }

    private static boolean hasExplainEvidence(SqlStatementDto sql) {
        return sql.getExplainAnalysis() != null
                && !hasExplainFailure(sql)
                && sql.getExplainAnalysis().getPlan() != null
                && sql.getExplainAnalysis().getPlan().getNodes() != null
                && !sql.getExplainAnalysis().getPlan().getNodes().isEmpty();
    }

    private static String explainSkipReason(SqlStatementDto sql) {
        if (sql.getPreprocessErrorReason() != null && !sql.getPreprocessErrorReason().isBlank()) {
            return sql.getPreprocessErrorReason();
        }
        ExplainEligibility eligibility = sql.getExplainEligibility();
        if (eligibility == ExplainEligibility.SKIPPED) {
            return "未启用数据库连接或 EXPLAIN，当前仅有静态诊断证据";
        }
        if (eligibility == ExplainEligibility.NOT_SUPPORTED) {
            return "非只读语句、动态模板或安全策略不支持执行计划";
        }
        return eligibility != null ? eligibility.name() : "EXPLAIN 状态未知";
    }

    private static String describeSql(SqlStatementDto sql) {
        String id = sql.getId() != null ? sql.getId() : "sql";
        String location = "unknown";
        if (sql.getLocations() != null && !sql.getLocations().isEmpty()) {
            SqlLocationDto first = sql.getLocations().get(0);
            location = (first.getFilePath() != null ? first.getFilePath() : first.getFileName())
                    + ":" + first.getStartLine();
        }
        return id + "@" + location;
    }

    private static String formatSeverity(SeverityLevel severity) {
        return severity != null ? severity.name() : "INFO";
    }

    private static int findingIssueCount(DiagnosticReport.Finding finding) {
        return finding.getIssues() != null ? finding.getIssues().size() : 0;
    }

    private static int severityRank(String severity) {
        if ("CRITICAL".equals(severity)) {
            return 3;
        }
        if ("WARNING".equals(severity)) {
            return 2;
        }
        if ("INFO".equals(severity)) {
            return 1;
        }
        return 0;
    }

    private static void increment(Map<String, Integer> counts, String key) {
        increment(counts, key, 1);
    }

    private static void increment(Map<String, Integer> counts, String key, int amount) {
        String normalized = key == null || key.isBlank() ? "Unknown" : key;
        counts.put(normalized, counts.getOrDefault(normalized, 0) + amount);
    }

    private static String resolveProjectName(String scanPath) {
        if (scanPath == null || scanPath.isBlank()) {
            return "未命名项目";
        }
        String normalized = scanPath.replace("\\", "/");
        int idx = normalized.lastIndexOf('/');
        if (idx >= 0 && idx < normalized.length() - 1) {
            return normalized.substring(idx + 1);
        }
        return normalized;
    }

    private static String safe(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
