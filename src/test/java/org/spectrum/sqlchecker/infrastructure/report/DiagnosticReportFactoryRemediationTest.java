package org.spectrum.sqlchecker.infrastructure.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spectrum.sqlchecker.application.analysis.dto.ExplainAnalysisDto;
import org.spectrum.sqlchecker.application.analysis.dto.ExplainIssue;
import org.spectrum.sqlchecker.application.analysis.dto.StaticAnalysisDto;
import org.spectrum.sqlchecker.application.analysis.dto.StaticIssue;
import org.spectrum.sqlchecker.application.report.dto.DiagnosticReport;
import org.spectrum.sqlchecker.application.scan.dto.ScanResult;
import org.spectrum.sqlchecker.application.scan.dto.SqlLocationDto;
import org.spectrum.sqlchecker.application.scan.dto.SqlStatementDto;
import org.spectrum.sqlchecker.domain.shared.enumeration.ExplainEligibility;
import org.spectrum.sqlchecker.domain.shared.enumeration.IssueType;
import org.spectrum.sqlchecker.domain.shared.enumeration.ScanStatus;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlSourceType;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlType;
import org.spectrum.sqlchecker.domain.shared.enumeration.ValidityStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosticReportFactoryRemediationTest {

    @Test
    @DisplayName("should expose remediation recipes without tasks for clean findings")
    void should_expose_remediation_recipes_without_tasks_for_clean_findings() {
        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanId("scan-remediation")
                .status(ScanStatus.COMPLETED)
                .scanPath("/repo/installment-trade")
                .totalFiles(1)
                .filesScanned(1)
                .sqlFound(1)
                .uniqueSqlFound(1)
                .sqlStatements(List.of(SqlStatementDto.builder()
                        .id("sql-dynamic-order")
                        .sqlType(SqlType.SELECT)
                        .originalSql("SELECT id FROM orders ORDER BY ${orderBy}")
                        .normalizedSql("SELECT id FROM orders ORDER BY ${orderBy}")
                        .abstractSql("SELECT id FROM orders ORDER BY ${orderBy}")
                        .validity(ValidityStatus.VALID)
                        .explainEligibility(ExplainEligibility.SKIPPED)
                        .severity(SeverityLevel.CRITICAL)
                        .score(60)
                        .build()))
                .build());

        DiagnosticReport.Remediation remediation = report.getRemediation();
        assertThat(remediation).isNotNull();
        assertThat(remediation.getSummary()).isNotNull();
        assertThat(remediation.getSummary().getCampaignCount()).isZero();
        assertThat(remediation.getSummary().getTaskCount()).isZero();
        assertThat(remediation.getSummary().getConfirmedTaskCount()).isZero();
        assertThat(remediation.getSummary().getLikelyTaskCount()).isZero();
        assertThat(remediation.getSummary().getReviewTaskCount()).isZero();
        assertThat(remediation.getSummary().getEstimatedFirstPassFocus()).isEqualTo("暂无修复任务。");
        assertThat(remediation.getCampaigns()).isEmpty();
        assertThat(remediation.getTasks()).isEmpty();
        assertThat(remediation.getRecipes())
                .extracting(DiagnosticReport.RepairRecipe::getId)
                .contains(
                        "dynamic-value-binding",
                        "dynamic-order-by-whitelist",
                        "unbounded-query-containment",
                        "select-star-field-list",
                        "dangerous-dml-guardrail",
                        "template-review-normalization",
                        "general-rule-remediation");
    }

    @Test
    @DisplayName("should create P0 remediation task for dynamic SQL injection risk")
    void should_create_p0_remediation_task_for_dynamic_sql_injection_risk() {
        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanPath("/repo/installment-trade")
                .totalFiles(1)
                .filesScanned(1)
                .sqlFound(1)
                .uniqueSqlFound(1)
                .sqlStatements(List.of(dynamicSql()))
                .build());

        assertThat(report.getRemediation().getSummary().getTaskCount()).isEqualTo(1);
        assertThat(report.getRemediation().getSummary().getReviewTaskCount()).isEqualTo(1);
        assertThat(report.getRemediation().getTasks())
                .singleElement()
                .satisfies(task -> {
                    assertThat(task.getTitle()).isEqualTo("OrderMapper.xml:42 · SQL_INJECTION_RISK");
                    assertThat(task.getPriority()).isEqualTo("P0");
                    assertThat(task.getTheme()).isEqualTo("SAFETY");
                    assertThat(task.getConfidence()).isEqualTo("NEEDS_REVIEW");
                    assertThat(task.getRepairRecipeId()).isEqualTo("dynamic-value-binding");
                    assertThat(task.getRecommendation()).contains("参数绑定").contains("白名单");
                    assertThat(task.getAcceptanceCheck()).contains("P0").contains("重新扫描");
                });
    }

    @Test
    @DisplayName("should create P1 remediation task for unbounded query")
    void should_create_p1_remediation_task_for_unbounded_query() {
        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanPath("/repo/installment-trade")
                .totalFiles(1)
                .filesScanned(1)
                .sqlFound(1)
                .uniqueSqlFound(1)
                .sqlStatements(List.of(unboundedSql()))
                .build());

        assertThat(report.getRemediation().getSummary().getTaskCount()).isEqualTo(1);
        assertThat(report.getRemediation().getSummary().getLikelyTaskCount()).isEqualTo(1);
        assertThat(report.getRemediation().getTasks())
                .singleElement()
                .satisfies(task -> {
                    assertThat(task.getTitle()).isEqualTo("OrderMapper.xml:88 · SELECT_WITHOUT_WHERE");
                    assertThat(task.getPriority()).isEqualTo("P1");
                    assertThat(task.getTheme()).isEqualTo("PERFORMANCE");
                    assertThat(task.getConfidence()).isEqualTo("LIKELY");
                    assertThat(task.getRepairRecipeId()).isEqualTo("unbounded-query-containment");
                });
    }

    @Test
    @DisplayName("should keep future dangerous DML remediation task likely without EXPLAIN evidence")
    void should_keep_future_dangerous_dml_remediation_task_likely_without_explain_evidence() {
        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanPath("/repo/installment-trade")
                .totalFiles(1)
                .filesScanned(1)
                .sqlFound(1)
                .uniqueSqlFound(1)
                .sqlStatements(List.of(deleteWithoutWhereSql()))
                .build());

        assertThat(report.getRemediation().getSummary().getTaskCount()).isEqualTo(1);
        assertThat(report.getRemediation().getSummary().getLikelyTaskCount()).isEqualTo(1);
        assertThat(report.getRemediation().getSummary().getReviewTaskCount()).isZero();
        assertThat(report.getRemediation().getTasks())
                .singleElement()
                .satisfies(task -> {
                    assertThat(task.getTitle()).isEqualTo("OrderMapper.xml:99 · DELETE_UPDATE_NO_WHERE");
                    assertThat(task.getPriority()).isEqualTo("P0");
                    assertThat(task.getTheme()).isEqualTo("SAFETY");
                    assertThat(task.getConfidence()).isEqualTo("LIKELY");
                    assertThat(task.getRepairRecipeId()).isEqualTo("dangerous-dml-guardrail");
                });
    }

    @Test
    @DisplayName("should choose canonical primary issue for multi-issue remediation tasks")
    void should_choose_canonical_primary_issue_for_multi_issue_remediation_tasks() {
        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanPath("/repo/installment-trade")
                .totalFiles(1)
                .filesScanned(1)
                .sqlFound(1)
                .uniqueSqlFound(1)
                .sqlStatements(List.of(injectionSecondSql()))
                .build());

        assertThat(report.getRemediation().getSummary().getTaskCount()).isEqualTo(1);
        assertThat(report.getRemediation().getSummary().getReviewTaskCount()).isEqualTo(1);
        assertThat(report.getRemediation().getTasks())
                .singleElement()
                .satisfies(task -> {
                    assertThat(task.getTitle()).isEqualTo("OrderMapper.xml:120 · SQL_INJECTION_RISK");
                    assertThat(task.getPriority()).isEqualTo("P0");
                    assertThat(task.getRepairRecipeId()).isEqualTo("dynamic-value-binding");
                    assertThat(task.getRecommendation()).contains("参数绑定").contains("白名单");
                    assertThat(task.getConfidence()).isEqualTo("NEEDS_REVIEW");
                    assertThat(task.getEvidence()).isEqualTo("动态 SQL 存在注入风险");
                });
    }

    @Test
    @DisplayName("should use general recipe for known unmapped rules")
    void should_use_general_recipe_for_known_unmapped_rules() {
        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanPath("/repo/installment-trade")
                .totalFiles(1)
                .filesScanned(1)
                .sqlFound(1)
                .uniqueSqlFound(1)
                .sqlStatements(List.of(leadingWildcardSql()))
                .build());

        assertThat(report.getRemediation().getTasks())
                .singleElement()
                .satisfies(task -> {
                    assertThat(task.getTitle()).isEqualTo("OrderMapper.xml:140 · LIKE_LEADING_WILDCARD");
                    assertThat(task.getRepairRecipeId()).isEqualTo("general-rule-remediation");
                    assertThat(task.getRepairRecipeId()).isNotEqualTo("template-review-normalization");
                });
    }

    @Test
    @DisplayName("should prefer known unmapped rule over unknown for remediation recipe")
    void should_prefer_known_unmapped_rule_over_unknown_for_remediation_recipe() {
        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanPath("/repo/installment-trade")
                .totalFiles(1)
                .filesScanned(1)
                .sqlFound(1)
                .uniqueSqlFound(1)
                .sqlStatements(List.of(leadingWildcardWithUnknownSql()))
                .build());

        assertThat(report.getRemediation().getTasks())
                .singleElement()
                .satisfies(task -> {
                    assertThat(task.getTitle()).isEqualTo("OrderMapper.xml:150 · LIKE_LEADING_WILDCARD");
                    assertThat(task.getRepairRecipeId()).isEqualTo("general-rule-remediation");
                    assertThat(task.getRepairRecipeId()).isNotEqualTo("template-review-normalization");
                });
    }

    @Test
    @DisplayName("should expose initial repair recipes")
    void should_expose_initial_repair_recipes() {
        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanPath("/repo/installment-trade")
                .totalFiles(1)
                .filesScanned(1)
                .sqlFound(1)
                .uniqueSqlFound(1)
                .sqlStatements(List.of(dynamicSql()))
                .build());

        assertThat(report.getRemediation().getRecipes())
                .extracting(DiagnosticReport.RepairRecipe::getId)
                .contains(
                        "dynamic-value-binding",
                        "dynamic-order-by-whitelist",
                        "unbounded-query-containment",
                        "select-star-field-list",
                        "dangerous-dml-guardrail",
                        "template-review-normalization",
                        "general-rule-remediation");
    }

    private static SqlStatementDto dynamicSql() {
        return SqlStatementDto.builder()
                .id("sql-dynamic")
                .sqlType(SqlType.SELECT)
                .originalSql("SELECT id FROM orders ORDER BY ${orderBy}")
                .normalizedSql("SELECT id FROM orders ORDER BY ${orderBy}")
                .abstractSql("SELECT id FROM orders ORDER BY ${orderBy}")
                .validity(ValidityStatus.VALID)
                .explainEligibility(ExplainEligibility.SKIPPED)
                .severity(SeverityLevel.CRITICAL)
                .score(55)
                .locations(List.of(location("OrderMapper.xml", 42)))
                .staticAnalysis(StaticAnalysisDto.builder()
                        .issues(List.of(StaticIssue.builder()
                                .type(IssueType.SQL_INJECTION_RISK)
                                .severity(SeverityLevel.CRITICAL)
                                .message("动态 SQL 存在注入风险")
                                .suggestion("使用参数绑定或白名单")
                                .build()))
                        .build())
                .build();
    }

    private static SqlStatementDto unboundedSql() {
        return SqlStatementDto.builder()
                .id("sql-unbounded")
                .sqlType(SqlType.SELECT)
                .originalSql("SELECT id, status FROM orders")
                .normalizedSql("SELECT id, status FROM orders")
                .abstractSql("SELECT id, status FROM orders")
                .validity(ValidityStatus.VALID)
                .explainEligibility(ExplainEligibility.SKIPPED)
                .severity(SeverityLevel.WARNING)
                .score(80)
                .locations(List.of(location("OrderMapper.xml", 88)))
                .staticAnalysis(StaticAnalysisDto.builder()
                        .issues(List.of(StaticIssue.builder()
                                .type(IssueType.SELECT_WITHOUT_WHERE)
                                .severity(SeverityLevel.WARNING)
                                .message("SELECT 缺少 WHERE 条件")
                                .suggestion("补充业务过滤条件或分页限制")
                                .build()))
                        .build())
                .build();
    }

    private static SqlStatementDto deleteWithoutWhereSql() {
        return SqlStatementDto.builder()
                .id("sql-delete-no-where")
                .sqlType(SqlType.DELETE)
                .originalSql("DELETE FROM orders")
                .normalizedSql("DELETE FROM orders")
                .abstractSql("DELETE FROM orders")
                .validity(ValidityStatus.VALID)
                .explainEligibility(ExplainEligibility.SKIPPED)
                .severity(SeverityLevel.CRITICAL)
                .score(40)
                .locations(List.of(location("OrderMapper.xml", 99)))
                .explainAnalysis(ExplainAnalysisDto.builder()
                        .sqlId("sql-delete-no-where")
                        .severity(SeverityLevel.CRITICAL)
                        .issues(List.of(ExplainIssue.builder()
                                .type("DELETE_UPDATE_NO_WHERE")
                                .severity(SeverityLevel.CRITICAL)
                                .message("DELETE 缺少 WHERE 条件")
                                .suggestion("补充 WHERE 条件和影响行数保护")
                                .tableName("orders")
                                .build()))
                        .build())
                .build();
    }

    private static SqlStatementDto injectionSecondSql() {
        return SqlStatementDto.builder()
                .id("sql-multi-issue")
                .sqlType(SqlType.SELECT)
                .originalSql("SELECT id FROM orders WHERE status = ${status}")
                .normalizedSql("SELECT id FROM orders WHERE status = ${status}")
                .abstractSql("SELECT id FROM orders WHERE status = ${status}")
                .validity(ValidityStatus.VALID)
                .explainEligibility(ExplainEligibility.SKIPPED)
                .severity(SeverityLevel.CRITICAL)
                .score(45)
                .locations(List.of(location("OrderMapper.xml", 120)))
                .staticAnalysis(StaticAnalysisDto.builder()
                        .issues(List.of(
                                StaticIssue.builder()
                                        .type(IssueType.SELECT_WITHOUT_WHERE)
                                        .severity(SeverityLevel.WARNING)
                                        .message("SELECT 缺少 WHERE 条件")
                                        .suggestion("补充业务过滤条件或分页限制")
                                        .build(),
                                StaticIssue.builder()
                                        .type(IssueType.SQL_INJECTION_RISK)
                                        .severity(SeverityLevel.CRITICAL)
                                        .message("动态 SQL 存在注入风险")
                                        .suggestion("使用参数绑定或白名单")
                                        .build()))
                        .build())
                .build();
    }

    private static SqlStatementDto leadingWildcardSql() {
        return SqlStatementDto.builder()
                .id("sql-leading-wildcard")
                .sqlType(SqlType.SELECT)
                .originalSql("SELECT id FROM orders WHERE customer_name LIKE '%alice'")
                .normalizedSql("SELECT id FROM orders WHERE customer_name LIKE ?")
                .abstractSql("SELECT id FROM orders WHERE customer_name LIKE ?")
                .validity(ValidityStatus.VALID)
                .explainEligibility(ExplainEligibility.SKIPPED)
                .severity(SeverityLevel.WARNING)
                .score(75)
                .locations(List.of(location("OrderMapper.xml", 140)))
                .staticAnalysis(StaticAnalysisDto.builder()
                        .issues(List.of(StaticIssue.builder()
                                .type(IssueType.LIKE_LEADING_WILDCARD)
                                .severity(SeverityLevel.WARNING)
                                .message("LIKE 使用前置通配符")
                                .suggestion("改用后缀匹配或搜索索引")
                                .build()))
                        .build())
                .build();
    }

    private static SqlStatementDto leadingWildcardWithUnknownSql() {
        return SqlStatementDto.builder()
                .id("sql-leading-wildcard-unknown")
                .sqlType(SqlType.SELECT)
                .originalSql("SELECT id FROM orders WHERE customer_name LIKE '%alice'")
                .normalizedSql("SELECT id FROM orders WHERE customer_name LIKE ?")
                .abstractSql("SELECT id FROM orders WHERE customer_name LIKE ?")
                .validity(ValidityStatus.VALID)
                .explainEligibility(ExplainEligibility.SKIPPED)
                .severity(SeverityLevel.WARNING)
                .score(75)
                .locations(List.of(location("OrderMapper.xml", 150)))
                .staticAnalysis(StaticAnalysisDto.builder()
                        .issues(List.of(
                                StaticIssue.builder()
                                        .type(IssueType.LIKE_LEADING_WILDCARD)
                                        .severity(SeverityLevel.WARNING)
                                        .message("LIKE 使用前置通配符")
                                        .suggestion("改用后缀匹配或搜索索引")
                                        .build(),
                                StaticIssue.builder()
                                        .type(IssueType.UNKNOWN)
                                        .severity(SeverityLevel.WARNING)
                                        .message("未知规则待复核")
                                        .suggestion("确认规则来源")
                                        .build()))
                        .build())
                .build();
    }

    private static SqlLocationDto location(String fileName, int line) {
        return SqlLocationDto.builder()
                .filePath("/repo/src/main/resources/mapper/" + fileName)
                .fileName(fileName)
                .startLine(line)
                .endLine(line)
                .sourceType(SqlSourceType.MYBATIS)
                .build();
    }
}
