package org.spectrum.sqlchecker.infrastructure.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spectrum.sqlchecker.application.report.dto.DiagnosticReport;
import org.spectrum.sqlchecker.application.report.ReportService;
import org.spectrum.sqlchecker.application.analysis.dto.ExplainAnalysisDto;
import org.spectrum.sqlchecker.application.analysis.dto.ExplainIssue;
import org.spectrum.sqlchecker.application.analysis.dto.ExplainPlan;
import org.spectrum.sqlchecker.application.report.dto.ReportSummary;
import org.spectrum.sqlchecker.application.scan.dto.SchemaAnalysisDto;
import org.spectrum.sqlchecker.application.scan.dto.SqlLocationDto;
import org.spectrum.sqlchecker.application.scan.dto.ScanResult;
import org.spectrum.sqlchecker.application.scan.dto.SqlStatementDto;
import org.spectrum.sqlchecker.application.analysis.dto.StaticAnalysisDto;
import org.spectrum.sqlchecker.application.analysis.dto.StaticIssue;
import org.spectrum.sqlchecker.domain.shared.enumeration.ExplainEligibility;
import org.spectrum.sqlchecker.domain.shared.enumeration.ScanStatus;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlSourceType;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlType;
import org.spectrum.sqlchecker.domain.shared.enumeration.IssueType;
import org.spectrum.sqlchecker.domain.shared.enumeration.ValidityStatus;
import org.spectrum.sqlchecker.infrastructure.template.TemplateEngine;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * ReportServiceImpl 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReportServiceImpl 单元测试")
class ReportServiceImplTest {

    /**
     * 验证 ReportServiceImpl 有 @Service 注解
     */
    @Test
    @DisplayName("应该有 @Service 注解")
    void should_have_service_annotation() {
        Class<?> clazz = org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl.class;

        assertThat(clazz.isAnnotationPresent(Service.class))
                .isTrue();
    }

    /**
     * 验证 ReportServiceImpl 实现了 ReportService 接口
     */
    @Test
    @DisplayName("应该实现 ReportService 接口")
    void should_implement_report_service() {
        org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl impl =
                new org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl(null);

        assertThat(impl).isInstanceOf(ReportService.class);
    }

    @Nested
    @DisplayName("generateSummary 测试")
    class GenerateSummaryTests {

        @Test
        @DisplayName("空 ScanResult 应该生成零统计")
        void should_return_zero_summary_for_empty_result() {
            org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl service =
                    new org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl(null);

            ScanResult emptyResult = ScanResult.builder()
                    .scanId("test-1")
                    .status(ScanStatus.COMPLETED)
                    .filesScanned(0)
                    .sqlFound(0)
                    .uniqueSqlFound(0)
                    .durationMs(0)
                    .sqlStatements(new ArrayList<>())
                    .errors(new ArrayList<>())
                    .build();

            ReportSummary summary = service.generateSummary(emptyResult);

            assertThat(summary.getTotalSql()).isEqualTo(0);
            assertThat(summary.getTotalIssues()).isEqualTo(0);
            assertThat(summary.getCriticalIssues()).isEqualTo(0);
            assertThat(summary.getWarningIssues()).isEqualTo(0);
            assertThat(summary.getInfoIssues()).isEqualTo(0);
            assertThat(summary.getAverageScore()).isEqualTo(100.0); // 空列表返回 100
        }

        @Test
        @DisplayName("应该正确统计问题数量")
        void should_count_issues_correctly() {
            org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl service =
                    new org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl(null);

            // 创建带问题的 SQL 语句列表
            ArrayList<org.spectrum.sqlchecker.application.scan.dto.SqlStatementDto> sqls = new ArrayList<>();
            sqls.add(createSqlWithSeverity(org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel.CRITICAL));
            sqls.add(createSqlWithSeverity(org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel.CRITICAL));
            sqls.add(createSqlWithSeverity(org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel.WARNING));
            sqls.add(createSqlWithSeverity(org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel.INFO));

            ScanResult result = ScanResult.builder()
                    .scanId("test-2")
                    .status(ScanStatus.COMPLETED)
                    .filesScanned(1)
                    .sqlFound(4)
                    .uniqueSqlFound(4)
                    .durationMs(100)
                    .sqlStatements(sqls)
                    .errors(new ArrayList<>())
                    .build();

            ReportSummary summary = service.generateSummary(result);

            assertThat(summary.getTotalSql()).isEqualTo(4);
            assertThat(summary.getCriticalIssues()).isEqualTo(2);
            assertThat(summary.getWarningIssues()).isEqualTo(1);
            assertThat(summary.getInfoIssues()).isEqualTo(1);
            assertThat(summary.getTotalIssues()).isEqualTo(4);
        }
    }

    @Test
    @DisplayName("should generate structured diagnostic json")
    void should_generate_structured_diagnostic_json(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl service =
                new org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl(null);
        Path output = tempDir.resolve("report.json");

        service.generateJsonReport(createDiagnosticScanResult(), output.toString());

        String json = Files.readString(output);
        assertThat(json).contains("\"metadata\"");
        assertThat(json).contains("\"summary\"");
        assertThat(json).contains("\"hotspots\"");
        assertThat(json).contains("\"findings\"");
        assertThat(json).contains("\"schemaAnalysis\"");
        assertThat(json).contains("\"diagnostics\"");
        assertThat(json).contains("\"executiveSummary\"");
        assertThat(json).contains("\"campaigns\"");
        assertThat(json).contains("\"confidence\"");
        assertThat(json).contains("\"methodology\"");
        assertThat(json).contains("\"remediation\"");
        assertThat(json).contains("\"remediation\":{\"summary\":");
        assertThat(json).contains("\"taskCount\":1");
        assertThat(json).contains(
                "\"summary\":{\"campaignCount\":0,\"taskCount\":1,\"confirmedTaskCount\":0,"
                        + "\"likelyTaskCount\":1,\"reviewTaskCount\":0,"
                        + "\"estimatedFirstPassFocus\":\"优先复核模板和低风险正确性任务。\"}");
        assertThat(json).contains("\"tasks\"");
        assertThat(json).contains(
                "\"tasks\":[{\"id\":\"sql-1-remediation-select_star\","
                        + "\"title\":\"UserMapper.xml:12 · SELECT_STAR\","
                        + "\"priority\":\"P2\",\"severity\":\"WARNING\",\"theme\":\"CORRECTNESS\","
                        + "\"confidence\":\"LIKELY\"");
        assertThat(json).contains("\"recipes\"");
        assertThat(json).contains("\"repairRecipeId\"");
        assertThat(json).contains("\"select-star-field-list\"");
        assertThat(json).contains(
                "\"id\":\"select-star-field-list\",\"title\":\"SELECT * 改为字段清单\","
                        + "\"appliesToRules\":[\"SELECT_STAR\"],\"unsafePattern\":\"SELECT *\","
                        + "\"safePattern\":\"SELECT id, status, updated_at\","
                        + "\"steps\":[\"确认调用方实际读取字段。\",\"替换为稳定字段清单。\","
                        + "\"回归序列化和映射结果。\"],"
                        + "\"verification\":\"重新扫描确认 SELECT_STAR 消失。\","
                        + "\"knownLimits\":[\"字段清单需随业务 DTO 演进同步维护。\"]");
        assertThat(json).contains("\"issueSql\":1");
        assertThat(json).contains("\"cleanSql\":0");
        assertThat(json).contains("\"totalIssues\":1");
        assertThat(json).contains("\"warningIssues\":1");
        assertThat(json).contains("\"infoIssues\":0");
        assertThat(json).contains("SELECT_STAR");
    }

    @Test
    @DisplayName("should expose DDL association in diagnostic json and html")
    void should_expose_ddl_association_in_diagnostic_json_and_html(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl service =
                new org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl(new TemplateEngine());
        Path jsonOutput = tempDir.resolve("schema-report.json");
        Path htmlOutput = tempDir.resolve("schema-report.html");
        ScanResult scanResult = createSchemaLinkedScanResult();

        service.generateJsonReport(scanResult, jsonOutput.toString());
        service.generateHtmlReport(scanResult, htmlOutput.toString());

        String json = Files.readString(jsonOutput);
        String html = Files.readString(htmlOutput);
        assertThat(json).contains("\"schemaPath\":\"/tmp/project/db/schema\"");
        assertThat(json).contains("\"ddlDetected\":true");
        assertThat(json).contains("\"riskType\":\"UNINDEXED_PREDICATE\"");
        assertThat(json).contains("\"missingIndexColumns\":[\"status\"]");
        assertThat(html).contains("DDL 关联风险分析");
        assertThat(html).contains("DDL 证据源");
        assertThat(html).contains("/tmp/project/db/schema");
        assertThat(html).contains("t_order · UNINDEXED_PREDICATE");
        assertThat(html).contains("疑似未索引谓词");
    }

    @Test
    @DisplayName("should expose report insights in diagnostic json")
    void should_expose_report_insights_in_diagnostic_json(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl service =
                new org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl(null);
        Path output = tempDir.resolve("insights.json");

        service.generateJsonReport(createInsightScanResult(), output.toString());

        String json = Files.readString(output);
        assertThat(json).contains("\"insights\"");
        assertThat(json).contains("\"duplicateSql\"");
        assertThat(json).contains("\"parseFailures\"");
        assertThat(json).contains("\"dangerousDml\"");
        assertThat(json).contains("\"potentialInjection\"");
        assertThat(json).contains("\"fullScanOrNoIndex\"");
        assertThat(json).contains("duplicate-query");
        assertThat(json).contains("invalid-query");
        assertThat(json).contains("dangerous-delete");
        assertThat(json).contains("injection-risk");
        assertThat(json).contains("full-scan");
    }

    @Test
    @DisplayName("diagnostic report should count manual review evidence gaps")
    void should_count_manual_review_evidence_gaps() {
        DiagnosticReport report = DiagnosticReportFactory.from(createManualReviewScanResult());

        assertThat(report.getDiagnostics().getParseFailures()).hasSize(1);
        assertThat(report.getDiagnostics().getSkippedExplain()).hasSize(3);
        assertThat(report.getDiagnostics().getManualReview()).hasSize(2);
        assertThat(report.getDiagnostics().getManualReview())
                .anyMatch(item -> item.contains("invalid-sql"))
                .anyMatch(item -> item.contains("unknown-sql"));
        assertThat(report.getDiagnostics().getManualReview())
                .noneMatch(item -> item.contains("skipped-sql"));
    }

    @Test
    @DisplayName("diagnostic report should not count EXPLAIN execution failures as SQL risk issues")
    void should_treat_explain_execution_failures_as_diagnostics() {
        DiagnosticReport report = DiagnosticReportFactory.from(createExplainFailureScanResult());

        assertThat(report.getSummary().getCounts().getTotalIssues()).isZero();
        assertThat(report.getSummary().getCoverage().getExplainCoverage()).isZero();
        assertThat(report.getDiagnostics().getConfigWarnings()).hasSize(1);
        assertThat(report.getDiagnostics().getManualReview())
                .anyMatch(item -> item.contains("EXPLAIN 失败"));
        assertThat(report.getFindings().get(0).getIssues()).isEmpty();
        assertThat(report.getFindings().get(0).getExplain().isExecuted()).isFalse();
    }

    @Test
    @DisplayName("html quality gate should use manual review count instead of parse failures only")
    void should_render_manual_review_count_in_quality_gate(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl service =
                new org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl(new TemplateEngine());
        Path output = tempDir.resolve("manual-review.html");

        service.generateHtmlReport(createManualReviewScanResult(), output.toString());

        String html = Files.readString(output);
        assertThat(html).contains("人工复核项</span><strong>2</strong>");
    }

    @Test
    @DisplayName("should render insights section in html report")
    void should_render_insights_section_in_html_report(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl service =
                new org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl(new TemplateEngine());
        Path output = tempDir.resolve("report.html");

        service.generateHtmlReport(createInsightScanResult(), output.toString());

        String html = Files.readString(output);
        assertThat(html).contains("洞察");
        assertThat(html).contains("重复 SQL");
        assertThat(html).contains("解析 / 模板待确认");
        assertThat(html).contains("执行计划证据");
        assertThat(html).contains("危险 DML");
        assertThat(html).contains("潜在注入");
        assertThat(html).contains("全表扫描 / 无索引");
    }

    @Test
    @DisplayName("html details should default to issue findings when issues exist")
    void should_default_html_details_to_issue_findings(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl service =
                new org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl(new TemplateEngine());
        Path output = tempDir.resolve("report.html");

        service.generateHtmlReport(createInsightScanResult(), output.toString());

        String html = Files.readString(output);
        assertThat(html).doesNotContain("id=\"issueState\"");
        assertThat(html).doesNotContain("<option value=\"problem\" selected>仅问题 SQL</option>");
        assertThat(html).contains("data-has-issue=\"true\"");
        assertThat(html).doesNotContain("data-has-issue=\"false\"");
        assertThat(html).contains("条问题 SQL");
        assertThat(html).doesNotContain("body:not(.interactive-ready) .finding[data-has-issue=\"false\"]");
        assertThat(html).contains("document.body.classList.add('interactive-ready')");
    }

    @Test
    @DisplayName("html report should render executive audit sections")
    void should_render_executive_audit_sections_in_html_report(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl service =
                new org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl(new TemplateEngine());
        Path output = tempDir.resolve("report.html");

        service.generateHtmlReport(createInsightScanResult(), output.toString());

        String html = Files.readString(output);
        assertThat(html).contains("审计结论");
        assertThat(html).contains("质量门禁");
        assertThat(html).contains("建议处置顺序");
        assertThat(html).contains("优先修复队列");
        assertThat(html).contains("默认展示前 12 条");
        assertThat(html).contains("id=\"priorityToggle\"");
        assertThat(html).contains("先处理 Critical");
    }

    @Test
    @DisplayName("html report should render consulting summary campaigns confidence and methodology")
    void should_render_consulting_summary_campaigns_confidence_and_methodology(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl service =
                new org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl(new TemplateEngine());
        Path output = tempDir.resolve("report.html");

        service.generateHtmlReport(createInsightScanResult(), output.toString());

        String html = Files.readString(output);
        assertThat(html).contains("Executive Conclusion");
        assertThat(html).contains("Remediation Campaigns");
        assertThat(html).contains("Evidence Confidence");
        assertThat(html).contains("Methodology");
        assertThat(html).contains("治理简报");
        assertThat(html).contains("修复任务");
        assertThat(html).contains("Repair Recipes");
        assertThat(html).contains("修复配方");
        assertThat(html).contains("Task Detail");
        assertThat(html).contains("复制任务摘要");
        assertThat(html).contains("复制验收条件");
        assertThat(html).contains("acceptance-checklist");
        assertThat(html).contains("href=\"#finding-");
    }

    @Test
    @DisplayName("html report should render navigation and operational filters")
    void should_render_navigation_and_operational_filters(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl service =
                new org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl(new TemplateEngine());
        Path output = tempDir.resolve("report.html");

        service.generateHtmlReport(createInsightScanResult(), output.toString());

        String html = Files.readString(output);
        assertThat(html).contains("class=\"toc\"");
        assertThat(html).contains("href=\"#overview\"");
        assertThat(html).contains("href=\"#queue\"");
        assertThat(html).contains("href=\"#details\"");
        assertThat(html).contains("id=\"visibleCount\"");
        assertThat(html).contains("id=\"clearFilters\"");
        assertThat(html).contains("data-rule-filter");
        assertThat(html).contains("data-rules=");
        assertThat(html).contains("id=\"interactionStatus\"");
        assertThat(html).contains("交互初始化中");
        assertThat(html).contains("交互已启用");
        assertThat(html).contains("规则快捷筛选");
    }

    @Test
    @DisplayName("html report should keep long SQL details collapsible and copyable")
    void should_keep_long_sql_details_collapsible_and_copyable(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl service =
                new org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl(new TemplateEngine());
        Path output = tempDir.resolve("report.html");

        service.generateHtmlReport(createInsightScanResult(), output.toString());

        String html = Files.readString(output);
        assertThat(html).contains("<details class=\"sql-detail\"");
        assertThat(html).contains("查看 SQL");
        assertThat(html).contains("data-copy-sql");
        assertThat(html).contains("window.navigator && navigator.clipboard");
        assertThat(html).contains("navigator.clipboard.writeText");
        assertThat(html).contains("function selectTextFallback(text)");
        assertThat(html).contains("document.createElement('textarea')");
        assertThat(html).contains("textarea.select()");
    }

    @Test
    @DisplayName("html report should omit full SQL bodies for clean findings")
    void should_omit_full_sql_bodies_for_clean_findings(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl service =
                new org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl(new TemplateEngine());
        Path output = tempDir.resolve("report.html");

        service.generateHtmlReport(createCleanAndRiskyScanResult(), output.toString());

        String html = Files.readString(output);
        assertThat(html).doesNotContain("clean-finding");
        assertThat(html).contains("无问题 SQL 保留在 JSON");
        assertThat(html).doesNotContain("SELECT clean_secret_payload FROM clean_table");
        assertThat(html).contains("SELECT * FROM risky_table");
    }

    @Test
    @DisplayName("html report should show original SQL when it carries risk evidence")
    void should_show_original_sql_risk_evidence_in_html_report(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl service =
                new org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl(new TemplateEngine());
        Path output = tempDir.resolve("report.html");

        service.generateHtmlReport(createInsightScanResult(), output.toString());

        String html = Files.readString(output);
        assertThat(html).contains("原始 SQL");
        assertThat(html).contains("${orderBy}");
    }

    @Test
    @DisplayName("html report should render issue evidence and recommendations as actionable cards")
    void should_render_issue_evidence_and_recommendations_as_actionable_cards(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl service =
                new org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl(new TemplateEngine());
        Path output = tempDir.resolve("report.html");

        service.generateHtmlReport(createInsightScanResult(), output.toString());

        String html = Files.readString(output);
        assertThat(html).contains("dangerous-delete");
        assertThat(html).contains("DELETE 缺少 WHERE 条件");
        assertThat(html).contains("为 DML 添加明确 WHERE 条件");
        assertThat(html).contains("证据");
        assertThat(html).contains("CleanupJob.java");
    }

    @Test
    @DisplayName("html report should escape untrusted scan content")
    void should_escape_untrusted_scan_content_in_html_report(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl service =
                new org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl(new TemplateEngine());
        Path output = tempDir.resolve("report.html");

        service.generateHtmlReport(createMaliciousScanResult(), output.toString());

        String html = Files.readString(output);
        assertThat(html).doesNotContain("<script>alert('sql')</script>");
        assertThat(html).doesNotContain("onmouseover=\"alert(1)\"");
        assertThat(html).doesNotContain("<img src=x onerror=alert(2)>");
        assertThat(html).contains("&lt;script&gt;alert(&#39;sql&#39;)&lt;/script&gt;");
        assertThat(html).contains("&lt;img src=x onerror=alert(2)&gt;");
    }

    @Test
    @DisplayName("html report should not duplicate free-form scan text in data attributes")
    void should_not_duplicate_free_form_scan_text_in_data_attributes(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl service =
                new org.spectrum.sqlchecker.infrastructure.report.impl.ReportServiceImpl(new TemplateEngine());
        Path output = tempDir.resolve("report.html");

        service.generateHtmlReport(createInsightScanResult(), output.toString());

        String html = Files.readString(output);
        assertThat(html).doesNotContain("data-search=");
        assertThat(html).doesNotContain("dataset.searchText");
        assertThat(html).contains("row._searchText = (row.textContent || '').toLowerCase()");
        assertThat(html).contains("function applyFilters()");
        assertThat(html).doesNotContain("=>");
        assertThat(html).doesNotContain("?.");
    }

    @Test
    @DisplayName("diagnostic summary parse rate should use diagnosed unique SQL as denominator")
    void should_use_diagnosed_unique_sql_for_parse_rate() {
        DiagnosticReport report = DiagnosticReportFactory.from(createInsightScanResult());

        assertThat(report.getSummary().getCoverage().getParseRate()).isEqualTo(100.0);
        assertThat(report.getDiagnostics().getParseFailures()).hasSize(1);
    }

    @Test
    @DisplayName("diagnostic report should classify dollar substitutions separately from generic parse failures")
    void should_classify_dollar_substitution_parse_failures() {
        SqlStatementDto dynamicSql = SqlStatementDto.builder()
                .id("dynamic-order")
                .sqlType(SqlType.SELECT)
                .originalSql("SELECT id FROM users ORDER BY ${orderBy}")
                .abstractSql("SELECT id FROM users ORDER BY ${orderBy}")
                .validity(ValidityStatus.INVALID)
                .preprocessErrorReason("包含 ${} 文本占位符，无法安全静态归一化")
                .explainEligibility(ExplainEligibility.NOT_SUPPORTED)
                .severity(SeverityLevel.CRITICAL)
                .score(65)
                .locations(List.of(location("mapper/DynamicMapper.xml")))
                .build();

        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanId("scan-dynamic")
                .status(ScanStatus.COMPLETED)
                .scanPath("/tmp/project")
                .totalFiles(1)
                .filesScanned(1)
                .sqlFound(1)
                .uniqueSqlFound(1)
                .durationMs(10)
                .sqlStatements(List.of(dynamicSql))
                .errors(new ArrayList<>())
                .build());

        assertThat(report.getInsights().getParseFailures())
                .extracting(DiagnosticReport.InsightItem::getTitle)
                .containsExactly("动态拼接待确认");
        assertThat(report.getDiagnostics().getParseFailures().get(0))
                .contains("动态拼接待确认");
    }

    @Test
    @DisplayName("diagnostic report should prioritize dynamic parse failures before generic parser errors")
    void should_prioritize_dynamic_parse_failures() {
        SqlStatementDto genericBroken = SqlStatementDto.builder()
                .id("generic-broken")
                .sqlType(SqlType.SELECT)
                .originalSql("SELECT * FROM")
                .abstractSql("SELECT * FROM")
                .validity(ValidityStatus.INVALID)
                .preprocessErrorReason("SQL 无法解析")
                .severity(SeverityLevel.INFO)
                .score(100)
                .locations(List.of(location("mapper/BrokenMapper.xml")))
                .build();
        SqlStatementDto dynamicSql = SqlStatementDto.builder()
                .id("dynamic-order")
                .sqlType(SqlType.SELECT)
                .originalSql("SELECT id FROM users ORDER BY ${orderBy}")
                .abstractSql("SELECT id FROM users ORDER BY ${orderBy}")
                .validity(ValidityStatus.INVALID)
                .preprocessErrorReason("包含 ${} 文本占位符，无法安全静态归一化")
                .severity(SeverityLevel.CRITICAL)
                .score(65)
                .locations(List.of(location("mapper/DynamicMapper.xml")))
                .build();

        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanId("scan-parse-priority")
                .status(ScanStatus.COMPLETED)
                .scanPath("/tmp/project")
                .totalFiles(1)
                .filesScanned(1)
                .sqlFound(2)
                .uniqueSqlFound(2)
                .durationMs(10)
                .sqlStatements(List.of(genericBroken, dynamicSql))
                .errors(new ArrayList<>())
                .build());

        assertThat(report.getInsights().getParseFailures())
                .extracting(DiagnosticReport.InsightItem::getTitle)
                .containsExactly("动态拼接待确认", "SQL 语法错误");
    }

    @Test
    @DisplayName("diagnostic report should order findings by remediation priority")
    void should_order_findings_by_remediation_priority() {
        SqlStatementDto cleanFirst = SqlStatementDto.builder()
                .id("clean-first")
                .originalSql("SELECT id FROM users WHERE id = 1")
                .abstractSql("SELECT id FROM users WHERE id = ?")
                .severity(SeverityLevel.INFO)
                .score(100)
                .build();
        SqlStatementDto warningSecond = SqlStatementDto.builder()
                .id("warning-second")
                .originalSql("SELECT id FROM users")
                .abstractSql("SELECT id FROM users")
                .severity(SeverityLevel.WARNING)
                .score(80)
                .staticAnalysis(StaticAnalysisDto.builder()
                        .sqlId("warning-second")
                        .severity(SeverityLevel.WARNING)
                        .issues(List.of(StaticIssue.builder()
                                .type(IssueType.SELECT_WITHOUT_WHERE)
                                .severity(SeverityLevel.WARNING)
                                .message("缺少 WHERE 条件")
                                .build()))
                        .score(80)
                        .build())
                .build();
        SqlStatementDto criticalThird = SqlStatementDto.builder()
                .id("critical-third")
                .originalSql("SELECT * FROM users")
                .abstractSql("SELECT * FROM users")
                .severity(SeverityLevel.CRITICAL)
                .score(70)
                .staticAnalysis(StaticAnalysisDto.builder()
                        .sqlId("critical-third")
                        .severity(SeverityLevel.CRITICAL)
                        .issues(List.of(StaticIssue.builder()
                                .type(IssueType.SELECT_STAR)
                                .severity(SeverityLevel.CRITICAL)
                                .message("使用了 SELECT *")
                                .build()))
                        .score(70)
                        .build())
                .build();

        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanId("scan-priority")
                .status(ScanStatus.COMPLETED)
                .scanPath("/tmp/project")
                .totalFiles(1)
                .filesScanned(1)
                .sqlFound(3)
                .uniqueSqlFound(3)
                .durationMs(10)
                .sqlStatements(List.of(cleanFirst, warningSecond, criticalThird))
                .errors(new ArrayList<>())
                .build());

        assertThat(report.getFindings())
                .extracting(DiagnosticReport.Finding::getId)
                .containsExactly("critical-third", "warning-second", "clean-first");
    }

    @Test
    @DisplayName("diagnostic report should add actionable fallback recommendations")
    void should_add_actionable_fallback_recommendations() {
        SqlStatementDto injectionRisk = SqlStatementDto.builder()
                .id("injection-risk-without-suggestion")
                .originalSql("SELECT * FROM users ORDER BY ${orderBy}")
                .abstractSql("SELECT * FROM users ORDER BY ${orderBy}")
                .severity(SeverityLevel.CRITICAL)
                .score(60)
                .staticAnalysis(StaticAnalysisDto.builder()
                        .sqlId("injection-risk-without-suggestion")
                        .severity(SeverityLevel.CRITICAL)
                        .issues(List.of(StaticIssue.builder()
                                .type(IssueType.SQL_INJECTION_RISK)
                                .severity(SeverityLevel.CRITICAL)
                                .message("检测到 ${} 动态拼接，可能存在 SQL 注入风险")
                                .build()))
                        .score(60)
                        .build())
                .build();

        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanId("scan-recommendations")
                .status(ScanStatus.COMPLETED)
                .scanPath("/tmp/project")
                .totalFiles(1)
                .filesScanned(1)
                .sqlFound(1)
                .uniqueSqlFound(1)
                .durationMs(10)
                .sqlStatements(List.of(injectionRisk))
                .errors(new ArrayList<>())
                .build());

        DiagnosticReport.Finding finding = report.getFindings().get(0);
        assertThat(finding.getIssues().get(0).getSuggestion())
                .contains("参数绑定")
                .contains("白名单");
        assertThat(finding.getRecommendations())
                .containsExactly(finding.getIssues().get(0).getSuggestion());
    }

    @Test
    @DisplayName("file hotspot ratios should be scaled by hotspot max count")
    void should_scale_file_hotspot_ratios_by_max_count() {
        SqlStatementDto repeatedRisk = SqlStatementDto.builder()
                .id("repeated-risk")
                .originalSql("SELECT * FROM users")
                .abstractSql("SELECT * FROM users")
                .severity(SeverityLevel.CRITICAL)
                .score(70)
                .locations(List.of(
                        location("mapper/A.xml"),
                        location("mapper/A.xml"),
                        location("mapper/A.xml")))
                .staticAnalysis(StaticAnalysisDto.builder()
                        .sqlId("repeated-risk")
                        .severity(SeverityLevel.CRITICAL)
                        .issues(List.of(StaticIssue.builder()
                                .type(IssueType.SELECT_STAR)
                                .severity(SeverityLevel.CRITICAL)
                                .message("使用了 SELECT *")
                                .build()))
                        .score(70)
                        .build())
                .build();
        SqlStatementDto singleRisk = SqlStatementDto.builder()
                .id("single-risk")
                .originalSql("SELECT id FROM orders")
                .abstractSql("SELECT id FROM orders")
                .severity(SeverityLevel.WARNING)
                .score(80)
                .locations(List.of(location("mapper/B.xml")))
                .staticAnalysis(StaticAnalysisDto.builder()
                        .sqlId("single-risk")
                        .severity(SeverityLevel.WARNING)
                        .issues(List.of(StaticIssue.builder()
                                .type(IssueType.SELECT_WITHOUT_WHERE)
                                .severity(SeverityLevel.WARNING)
                                .message("缺少 WHERE 条件")
                                .build()))
                        .score(80)
                        .build())
                .build();

        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanId("scan-hotspot-ratio")
                .status(ScanStatus.COMPLETED)
                .scanPath("/tmp/project")
                .totalFiles(3)
                .filesScanned(3)
                .sqlFound(2)
                .uniqueSqlFound(2)
                .durationMs(10)
                .sqlStatements(List.of(repeatedRisk, singleRisk))
                .errors(new ArrayList<>())
                .build());

        assertThat(report.getHotspots().getByFile())
                .allSatisfy(item -> assertThat(item.getRatio()).isBetween(0.0, 100.0));
        assertThat(report.getHotspots().getByFile().get(0).getLabel()).isEqualTo("mapper/A.xml");
        assertThat(report.getHotspots().getByFile().get(0).getCount()).isEqualTo(3);
        assertThat(report.getHotspots().getByFile().get(0).getRatio()).isEqualTo(100.0);
        assertThat(report.getHotspots().getByFile().get(1).getRatio()).isEqualTo(33.3);
    }

    @Test
    @DisplayName("diagnostic summary score should be capped when critical issues exist")
    void should_cap_diagnostic_summary_score_when_critical_issues_exist() {
        List<SqlStatementDto> sqls = new ArrayList<>();
        for (int i = 0; i < 99; i++) {
            sqls.add(SqlStatementDto.builder()
                    .id("clean-" + i)
                    .originalSql("SELECT id FROM users WHERE id = " + i)
                    .abstractSql("SELECT id FROM users WHERE id = ?")
                    .severity(SeverityLevel.INFO)
                    .score(100)
                    .build());
        }
        sqls.add(SqlStatementDto.builder()
                .id("critical-1")
                .originalSql("SELECT * FROM users")
                .abstractSql("SELECT * FROM users")
                .severity(SeverityLevel.CRITICAL)
                .score(70)
                .staticAnalysis(StaticAnalysisDto.builder()
                        .sqlId("critical-1")
                        .severity(SeverityLevel.CRITICAL)
                        .issues(List.of(StaticIssue.builder()
                                .type(IssueType.SELECT_STAR)
                                .severity(SeverityLevel.CRITICAL)
                                .message("使用了 SELECT *")
                                .suggestion("明确列出需要字段")
                                .build()))
                        .score(70)
                        .build())
                .build());

        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanId("scan-risk")
                .status(ScanStatus.COMPLETED)
                .scanPath("/tmp/project")
                .totalFiles(10)
                .filesScanned(10)
                .sqlFound(100)
                .uniqueSqlFound(100)
                .durationMs(10)
                .sqlStatements(sqls)
                .errors(new ArrayList<>())
                .build());

        assertThat(report.getSummary().getRiskLevel()).isEqualTo("CRITICAL");
        assertThat(report.getSummary().getScore()).isLessThanOrEqualTo(79);
    }

    @Test
    @DisplayName("file hotspots should count issue locations instead of SQL volume")
    void should_count_file_hotspots_by_issue_locations() {
        SqlStatementDto cleanHighVolume = SqlStatementDto.builder()
                .id("clean-high-volume")
                .originalSql("SELECT id FROM users WHERE id = 1")
                .abstractSql("SELECT id FROM users WHERE id = ?")
                .severity(SeverityLevel.INFO)
                .score(100)
                .locations(List.of(
                        location("mapper/CleanMapper.xml"),
                        location("mapper/CleanMapper.xml"),
                        location("mapper/CleanMapper.xml")))
                .build();
        SqlStatementDto risky = SqlStatementDto.builder()
                .id("risky")
                .originalSql("SELECT * FROM orders")
                .abstractSql("SELECT * FROM orders")
                .severity(SeverityLevel.CRITICAL)
                .score(70)
                .locations(List.of(location("mapper/RiskyMapper.xml")))
                .staticAnalysis(StaticAnalysisDto.builder()
                        .sqlId("risky")
                        .severity(SeverityLevel.CRITICAL)
                        .issues(List.of(StaticIssue.builder()
                                .type(IssueType.SELECT_STAR)
                                .severity(SeverityLevel.CRITICAL)
                                .message("使用了 SELECT *")
                                .build()))
                        .score(70)
                        .build())
                .build();

        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanId("scan-hotspots")
                .status(ScanStatus.COMPLETED)
                .scanPath("/tmp/project")
                .totalFiles(2)
                .filesScanned(2)
                .sqlFound(4)
                .uniqueSqlFound(2)
                .durationMs(10)
                .sqlStatements(List.of(cleanHighVolume, risky))
                .errors(new ArrayList<>())
                .build());

        assertThat(report.getHotspots().getByFile())
                .extracting(DiagnosticReport.StatItem::getLabel)
                .containsExactly("mapper/RiskyMapper.xml");
        assertThat(report.getHotspots().getByFile().get(0).getCount()).isEqualTo(1);
    }

    private org.spectrum.sqlchecker.application.scan.dto.SqlStatementDto createSqlWithSeverity(
            org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel severity) {
        return org.spectrum.sqlchecker.application.scan.dto.SqlStatementDto.builder()
                .id("sql-" + System.nanoTime())
                .originalSql("SELECT * FROM test")
                .severity(severity)
                .score(80)
                .build();
    }

    private SqlLocationDto location(String filePath) {
        return SqlLocationDto.builder()
                .filePath(filePath)
                .fileName(filePath.substring(filePath.lastIndexOf('/') + 1))
                .startLine(1)
                .endLine(1)
                .sourceType(SqlSourceType.MYBATIS)
                .build();
    }

    private ScanResult createManualReviewScanResult() {
        SqlStatementDto invalid = SqlStatementDto.builder()
                .id("invalid-sql")
                .originalSql("SELECT * FROM WHERE")
                .abstractSql("SELECT * FROM WHERE")
                .validity(ValidityStatus.INVALID)
                .preprocessErrorReason("SQL 语法错误")
                .explainEligibility(ExplainEligibility.NOT_SUPPORTED)
                .severity(SeverityLevel.INFO)
                .score(100)
                .locations(List.of(location("mapper/BrokenMapper.xml")))
                .build();
        SqlStatementDto unknown = SqlStatementDto.builder()
                .id("unknown-sql")
                .originalSql("SELECT * FROM users ORDER BY ${sort}")
                .abstractSql("SELECT * FROM users ORDER BY ${sort}")
                .validity(ValidityStatus.UNKNOWN)
                .preprocessErrorReason("包含 ${} 动态拼接，需人工确认白名单")
                .explainEligibility(ExplainEligibility.NOT_SUPPORTED)
                .severity(SeverityLevel.CRITICAL)
                .score(70)
                .locations(List.of(location("mapper/UserMapper.xml")))
                .build();
        SqlStatementDto skipped = SqlStatementDto.builder()
                .id("skipped-sql")
                .originalSql("SELECT id FROM users WHERE status = ?")
                .abstractSql("SELECT id FROM users WHERE status = ?")
                .validity(ValidityStatus.VALID)
                .explainEligibility(ExplainEligibility.SKIPPED)
                .preprocessErrorReason("数据库未配置，跳过 EXPLAIN")
                .severity(SeverityLevel.INFO)
                .score(100)
                .locations(List.of(location("mapper/UserMapper.xml")))
                .build();
        SqlStatementDto supported = SqlStatementDto.builder()
                .id("supported-sql")
                .originalSql("SELECT id FROM users WHERE id = ?")
                .abstractSql("SELECT id FROM users WHERE id = ?")
                .validity(ValidityStatus.VALID)
                .explainEligibility(ExplainEligibility.SUPPORTED)
                .severity(SeverityLevel.INFO)
                .score(100)
                .locations(List.of(location("mapper/UserMapper.xml")))
                .build();

        return ScanResult.builder()
                .scanId("scan-manual-review")
                .status(ScanStatus.COMPLETED)
                .scanPath("/tmp/project")
                .totalFiles(1)
                .filesScanned(1)
                .sqlFound(4)
                .uniqueSqlFound(4)
                .durationMs(10)
                .sqlStatements(List.of(invalid, unknown, skipped, supported))
                .errors(new ArrayList<>())
                .build();
    }

    private ScanResult createCleanAndRiskyScanResult() {
        SqlStatementDto clean = SqlStatementDto.builder()
                .id("clean-finding")
                .originalSql("SELECT clean_secret_payload FROM clean_table")
                .abstractSql("SELECT clean_secret_payload FROM clean_table")
                .validity(ValidityStatus.VALID)
                .explainEligibility(ExplainEligibility.SUPPORTED)
                .severity(SeverityLevel.INFO)
                .score(100)
                .locations(List.of(location("mapper/CleanMapper.xml")))
                .build();
        SqlStatementDto risky = SqlStatementDto.builder()
                .id("risky-finding")
                .originalSql("SELECT * FROM risky_table")
                .abstractSql("SELECT * FROM risky_table")
                .validity(ValidityStatus.VALID)
                .explainEligibility(ExplainEligibility.SKIPPED)
                .severity(SeverityLevel.CRITICAL)
                .score(70)
                .locations(List.of(location("mapper/RiskyMapper.xml")))
                .staticAnalysis(StaticAnalysisDto.builder()
                        .sqlId("risky-finding")
                        .severity(SeverityLevel.CRITICAL)
                        .issues(List.of(StaticIssue.builder()
                                .type(IssueType.SELECT_STAR)
                                .severity(SeverityLevel.CRITICAL)
                                .message("使用了 SELECT *")
                                .build()))
                        .score(70)
                        .build())
                .build();

        return ScanResult.builder()
                .scanId("scan-clean-and-risky")
                .status(ScanStatus.COMPLETED)
                .scanPath("/tmp/project")
                .totalFiles(2)
                .filesScanned(2)
                .sqlFound(2)
                .uniqueSqlFound(2)
                .durationMs(10)
                .sqlStatements(List.of(clean, risky))
                .errors(new ArrayList<>())
                .build();
    }

    private ScanResult createExplainFailureScanResult() {
        SqlStatementDto sql = SqlStatementDto.builder()
                .id("explain-failed")
                .originalSql("SELECT id FROM users")
                .abstractSql("SELECT id FROM users")
                .validity(ValidityStatus.VALID)
                .explainEligibility(ExplainEligibility.SUPPORTED)
                .explainAnalysis(ExplainAnalysisDto.builder()
                        .sqlId("explain-failed")
                        .plan(ExplainPlan.builder().nodes(List.of()).build())
                        .issues(List.of())
                        .severity(SeverityLevel.INFO)
                        .durationMs(0)
                        .errorMessage("EXPLAIN 执行失败: bad syntax")
                        .build())
                .severity(SeverityLevel.INFO)
                .score(100)
                .locations(List.of(location("mapper/UserMapper.xml")))
                .build();

        return ScanResult.builder()
                .scanId("scan-explain-failure")
                .status(ScanStatus.COMPLETED)
                .scanPath("/tmp/project")
                .totalFiles(1)
                .filesScanned(1)
                .sqlFound(1)
                .uniqueSqlFound(1)
                .durationMs(10)
                .sqlStatements(List.of(sql))
                .errors(new ArrayList<>())
                .build();
    }

    private ScanResult createDiagnosticScanResult() {
        StaticIssue issue = StaticIssue.builder()
                .type(IssueType.SELECT_STAR)
                .severity(SeverityLevel.WARNING)
                .message("使用了 SELECT *")
                .suggestion("明确列出需要字段")
                .location("UserMapper.xml")
                .build();

        SqlStatementDto sql = SqlStatementDto.builder()
                .id("sql-1")
                .sqlType(SqlType.SELECT)
                .originalSql("SELECT * FROM users WHERE id = 1")
                .abstractSql("SELECT * FROM users WHERE id = ?")
                .severity(SeverityLevel.WARNING)
                .score(80)
                .locations(List.of(SqlLocationDto.builder()
                        .filePath("mapper/UserMapper.xml")
                        .fileName("UserMapper.xml")
                        .startLine(12)
                        .endLine(12)
                        .sourceType(SqlSourceType.MYBATIS)
                        .build()))
                .staticAnalysis(StaticAnalysisDto.builder()
                        .sqlId("sql-1")
                        .severity(SeverityLevel.WARNING)
                        .issues(List.of(issue))
                        .score(80)
                        .build())
                .build();

        return ScanResult.builder()
                .scanId("scan-1")
                .status(ScanStatus.COMPLETED)
                .scanPath("/tmp/project")
                .totalFiles(1)
                .filesScanned(1)
                .sqlFound(1)
                .uniqueSqlFound(1)
                .durationMs(42)
                .sqlStatements(List.of(sql))
                .errors(new ArrayList<>())
                .build();
    }

    private ScanResult createSchemaLinkedScanResult() {
        SqlStatementDto sql = SqlStatementDto.builder()
                .id("sql-schema-risk")
                .sqlType(SqlType.SELECT)
                .originalSql("SELECT id FROM t_order WHERE status = ?")
                .abstractSql("SELECT id FROM t_order WHERE status = ?")
                .normalizedSql("SELECT id FROM t_order WHERE status = ?")
                .validity(ValidityStatus.VALID)
                .explainEligibility(ExplainEligibility.SKIPPED)
                .severity(SeverityLevel.WARNING)
                .score(85)
                .locations(List.of(location("mapper/OrderMapper.xml")))
                .build();

        SchemaAnalysisDto schemaAnalysis = SchemaAnalysisDto.builder()
                .schemaPath("/tmp/project/db/schema")
                .ddlDetected(true)
                .ddlFileCount(1)
                .tableCount(1)
                .referencedTableCount(1)
                .coveredTableCount(1)
                .missingDdlTableCount(0)
                .unindexedPredicateCount(1)
                .tables(List.of(SchemaAnalysisDto.TableSummary.builder()
                        .tableName("t_order")
                        .sourceFile("schema.sql")
                        .columns(List.of("id", "status"))
                        .primaryKeyColumns(List.of("id"))
                        .indexedColumns(List.of("id"))
                        .referencedSqlCount(1)
                        .coverage("REFERENCED")
                        .build()))
                .risks(List.of(SchemaAnalysisDto.SqlSchemaRisk.builder()
                        .sqlId("sql-schema-risk")
                        .riskType("UNINDEXED_PREDICATE")
                        .severity("WARNING")
                        .tableName("t_order")
                        .predicateColumns(List.of("status"))
                        .indexedPredicateColumns(List.of())
                        .missingIndexColumns(List.of("status"))
                        .locations(List.of("mapper/OrderMapper.xml:12"))
                        .evidence("过滤字段未在 DDL 索引列中命中: status")
                        .recommendation("结合访问频次和表规模评估索引。")
                        .build()))
                .warnings(List.of())
                .build();

        return ScanResult.builder()
                .scanId("scan-schema")
                .status(ScanStatus.COMPLETED)
                .scanPath("/tmp/project")
                .totalFiles(2)
                .filesScanned(2)
                .sqlFound(1)
                .uniqueSqlFound(1)
                .durationMs(12)
                .schemaAnalysis(schemaAnalysis)
                .sqlStatements(List.of(sql))
                .errors(new ArrayList<>())
                .build();
    }

    private ScanResult createMaliciousScanResult() {
        SqlStatementDto sql = SqlStatementDto.builder()
                .id("malicious")
                .sqlType(SqlType.SELECT)
                .originalSql("SELECT '<script>alert('sql')</script>' FROM users")
                .abstractSql("SELECT '<script>alert('sql')</script>' FROM users")
                .severity(SeverityLevel.CRITICAL)
                .score(70)
                .locations(List.of(SqlLocationDto.builder()
                        .filePath("mapper/Bad\" onmouseover=\"alert(1).xml")
                        .fileName("Bad\" onmouseover=\"alert(1).xml")
                        .startLine(7)
                        .endLine(7)
                        .sourceType(SqlSourceType.MYBATIS)
                        .build()))
                .staticAnalysis(StaticAnalysisDto.builder()
                        .sqlId("malicious")
                        .severity(SeverityLevel.CRITICAL)
                        .issues(List.of(StaticIssue.builder()
                                .type(IssueType.SELECT_STAR)
                                .severity(SeverityLevel.CRITICAL)
                                .message("<img src=x onerror=alert(2)>")
                                .suggestion("明确列出需要字段")
                                .location("BadMapper.xml")
                                .build()))
                        .score(70)
                        .build())
                .build();

        return ScanResult.builder()
                .scanId("scan-malicious")
                .status(ScanStatus.COMPLETED)
                .scanPath("/tmp/project\" onclick=\"alert(3)")
                .totalFiles(1)
                .filesScanned(1)
                .sqlFound(1)
                .uniqueSqlFound(1)
                .durationMs(10)
                .sqlStatements(List.of(sql))
                .errors(new ArrayList<>())
                .build();
    }

    private ScanResult createInsightScanResult() {
        SqlStatementDto duplicate = SqlStatementDto.builder()
                .id("duplicate-query")
                .sqlType(SqlType.SELECT)
                .originalSql("SELECT id FROM users WHERE status = 'ACTIVE'")
                .abstractSql("SELECT id FROM users WHERE status = ?")
                .validity(ValidityStatus.VALID)
                .explainEligibility(ExplainEligibility.SKIPPED)
                .preprocessErrorReason("数据库未配置，跳过 EXPLAIN")
                .severity(SeverityLevel.INFO)
                .score(100)
                .locations(List.of(
                        SqlLocationDto.builder()
                                .filePath("mapper/UserMapper.xml")
                                .fileName("UserMapper.xml")
                                .startLine(8)
                                .endLine(8)
                                .sourceType(SqlSourceType.MYBATIS)
                                .build(),
                        SqlLocationDto.builder()
                                .filePath("dao/UserDao.java")
                                .fileName("UserDao.java")
                                .startLine(21)
                                .endLine(21)
                                .sourceType(SqlSourceType.STRING_LITERAL)
                                .build()
                ))
                .build();

        SqlStatementDto invalid = SqlStatementDto.builder()
                .id("invalid-query")
                .sqlType(SqlType.SELECT)
                .originalSql("SELECT * FROM users WHERE id = #{id}")
                .abstractSql("SELECT * FROM users WHERE id = #{id}")
                .validity(ValidityStatus.INVALID)
                .preprocessErrorReason("占位符无法解析")
                .explainEligibility(ExplainEligibility.NOT_SUPPORTED)
                .severity(SeverityLevel.INFO)
                .score(100)
                .locations(List.of(SqlLocationDto.builder()
                        .filePath("mapper/BrokenMapper.xml")
                        .fileName("BrokenMapper.xml")
                        .startLine(14)
                        .endLine(14)
                        .sourceType(SqlSourceType.MYBATIS)
                        .build()))
                .build();

        SqlStatementDto dangerousDml = SqlStatementDto.builder()
                .id("dangerous-delete")
                .sqlType(SqlType.DELETE)
                .originalSql("DELETE FROM orders")
                .abstractSql("DELETE FROM orders")
                .validity(ValidityStatus.VALID)
                .explainEligibility(ExplainEligibility.NOT_SUPPORTED)
                .preprocessErrorReason("EXPLAIN 仅允许只读查询语句，已跳过变更语句")
                .severity(SeverityLevel.CRITICAL)
                .score(70)
                .locations(List.of(SqlLocationDto.builder()
                        .filePath("service/CleanupJob.java")
                        .fileName("CleanupJob.java")
                        .startLine(33)
                        .endLine(33)
                        .sourceType(SqlSourceType.STRING_LITERAL)
                        .build()))
                .staticAnalysis(StaticAnalysisDto.builder()
                        .sqlId("dangerous-delete")
                        .severity(SeverityLevel.CRITICAL)
                        .issues(List.of(StaticIssue.builder()
                                .type(IssueType.SELECT_WITHOUT_WHERE)
                                .severity(SeverityLevel.CRITICAL)
                                .message("DELETE 缺少 WHERE 条件")
                                .suggestion("为 DML 添加明确 WHERE 条件")
                                .location("CleanupJob.java")
                                .build()))
                        .score(70)
                        .build())
                .build();

        SqlStatementDto injection = SqlStatementDto.builder()
                .id("injection-risk")
                .sqlType(SqlType.SELECT)
                .originalSql("SELECT * FROM users ORDER BY ${orderBy}")
                .abstractSql("SELECT * FROM users ORDER BY ${orderBy}")
                .validity(ValidityStatus.VALID)
                .explainEligibility(ExplainEligibility.NOT_SUPPORTED)
                .severity(SeverityLevel.CRITICAL)
                .score(70)
                .locations(List.of(SqlLocationDto.builder()
                        .filePath("web/UserController.java")
                        .fileName("UserController.java")
                        .startLine(44)
                        .endLine(44)
                        .sourceType(SqlSourceType.STRING_LITERAL)
                        .build()))
                .staticAnalysis(StaticAnalysisDto.builder()
                        .sqlId("injection-risk")
                        .severity(SeverityLevel.CRITICAL)
                        .issues(List.of(StaticIssue.builder()
                                .type(IssueType.SQL_INJECTION_RISK)
                                .severity(SeverityLevel.CRITICAL)
                                .message("检测到 ${} 动态拼接")
                                .suggestion("改用参数绑定或白名单字段映射")
                                .location("UserController.java")
                                .build()))
                        .score(70)
                        .build())
                .build();

        SqlStatementDto fullScan = SqlStatementDto.builder()
                .id("full-scan")
                .sqlType(SqlType.SELECT)
                .originalSql("SELECT id FROM audit_log WHERE created_at > '2026-01-01'")
                .abstractSql("SELECT id FROM audit_log WHERE created_at > ?")
                .validity(ValidityStatus.VALID)
                .explainEligibility(ExplainEligibility.SUPPORTED)
                .severity(SeverityLevel.WARNING)
                .score(80)
                .locations(List.of(SqlLocationDto.builder()
                        .filePath("report/AuditReport.java")
                        .fileName("AuditReport.java")
                        .startLine(51)
                        .endLine(51)
                        .sourceType(SqlSourceType.STRING_LITERAL)
                        .build()))
                .explainAnalysis(ExplainAnalysisDto.builder()
                        .sqlId("full-scan")
                        .severity(SeverityLevel.WARNING)
                        .issues(List.of(ExplainIssue.builder()
                                .type("FULL_TABLE_SCAN")
                                .severity(SeverityLevel.WARNING)
                                .message("执行计划显示全表扫描")
                                .suggestion("为过滤列补充索引")
                                .tableName("audit_log")
                                .build()))
                        .durationMs(5)
                        .build())
                .build();

        return ScanResult.builder()
                .scanId("scan-insights")
                .status(ScanStatus.COMPLETED)
                .scanPath("/tmp/project")
                .totalFiles(5)
                .filesScanned(5)
                .sqlFound(5)
                .uniqueSqlFound(5)
                .durationMs(90)
                .sqlStatements(List.of(duplicate, invalid, dangerousDml, injection, fullScan))
                .errors(new ArrayList<>())
                .build();
    }
}
