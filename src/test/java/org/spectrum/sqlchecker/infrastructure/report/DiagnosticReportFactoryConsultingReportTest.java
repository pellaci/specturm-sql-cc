package org.spectrum.sqlchecker.infrastructure.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spectrum.sqlchecker.application.analysis.dto.StaticAnalysisDto;
import org.spectrum.sqlchecker.application.analysis.dto.StaticIssue;
import org.spectrum.sqlchecker.application.report.dto.DiagnosticReport;
import org.spectrum.sqlchecker.application.scan.dto.SchemaAnalysisDto;
import org.spectrum.sqlchecker.application.scan.dto.ScanResult;
import org.spectrum.sqlchecker.application.scan.dto.SqlStatementDto;
import org.spectrum.sqlchecker.domain.shared.enumeration.ExplainEligibility;
import org.spectrum.sqlchecker.domain.shared.enumeration.IssueType;
import org.spectrum.sqlchecker.domain.shared.enumeration.ScanStatus;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlType;
import org.spectrum.sqlchecker.domain.shared.enumeration.ValidityStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosticReportFactoryConsultingReportTest {

    @Test
    @DisplayName("should expose consulting report sections")
    void should_expose_consulting_report_sections() {
        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanId("scan-consulting")
                .status(ScanStatus.COMPLETED)
                .scanPath("/repo/installment-commodity")
                .totalFiles(1)
                .filesScanned(1)
                .sqlFound(1)
                .uniqueSqlFound(1)
                .sqlStatements(List.of(SqlStatementDto.builder()
                        .id("sql-risk")
                        .sqlType(SqlType.SELECT)
                        .originalSql("SELECT id FROM users ORDER BY ${orderBy}")
                        .abstractSql("SELECT id FROM users ORDER BY ${orderBy}")
                        .validity(ValidityStatus.VALID)
                        .explainEligibility(ExplainEligibility.NOT_SUPPORTED)
                        .severity(SeverityLevel.CRITICAL)
                        .score(60)
                        .build()))
                .build());

        assertThat(report.getExecutiveSummary()).isNotNull();
        assertThat(report.getCampaigns()).isNotNull();
        assertThat(report.getConfidence()).isNotNull();
        assertThat(report.getMethodology()).isNotNull();
    }

    @Test
    @DisplayName("should expose DDL schema association in report model")
    void should_expose_ddl_schema_association_in_report_model() {
        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanPath("/repo/installment-commodity")
                .totalFiles(2)
                .filesScanned(2)
                .sqlFound(1)
                .uniqueSqlFound(1)
                .schemaAnalysis(SchemaAnalysisDto.builder()
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
                                .sqlId("sql-order")
                                .riskType("UNINDEXED_PREDICATE")
                                .severity("WARNING")
                                .tableName("t_order")
                                .predicateColumns(List.of("status"))
                                .indexedPredicateColumns(List.of())
                                .missingIndexColumns(List.of("status"))
                                .locations(List.of("mapper/OrderMapper.xml:10"))
                                .evidence("过滤字段未在 DDL 索引列中命中: status")
                                .recommendation("评估索引")
                                .build()))
                        .warnings(List.of())
                        .build())
                .sqlStatements(List.of(skippedExplainSql()))
                .build());

        assertThat(report.getSchemaAnalysis().isDdlDetected()).isTrue();
        assertThat(report.getSchemaAnalysis().getRisks())
                .extracting(DiagnosticReport.SchemaRisk::getRiskType)
                .contains("UNINDEXED_PREDICATE");
        assertThat(report.getExecutiveSummary().getRecommendedActions())
                .anySatisfy(action -> assertThat(action).contains("DDL 关联分析"));
        assertThat(report.getConfidence().getEvidenceSources())
                .contains("DDL schema association");
    }

    @Test
    @DisplayName("should generate executive summary from risk counts and diagnostics")
    void should_generate_executive_summary_from_risk_counts_and_diagnostics() {
        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanPath("/repo/installment-commodity")
                .totalFiles(2)
                .filesScanned(2)
                .sqlFound(2)
                .uniqueSqlFound(2)
                .sqlStatements(List.of(dynamicInjectionSql(), skippedExplainSql()))
                .build());

        assertThat(report.getExecutiveSummary().getRiskConclusion())
                .contains("CRITICAL")
                .contains("installment-commodity");
        assertThat(report.getExecutiveSummary().getTopDrivers())
                .anySatisfy(driver -> assertThat(driver).contains("SQL_INJECTION_RISK"));
        assertThat(report.getExecutiveSummary().getRecommendedActions())
                .anySatisfy(action -> assertThat(action).contains("P0"));
        assertThat(report.getExecutiveSummary().getConfidenceSummary())
                .contains("人工复核")
                .contains("EXPLAIN 未执行");
    }

    @Test
    @DisplayName("should classify report confidence from evidence coverage")
    void should_classify_report_confidence_from_evidence_coverage() {
        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanPath("/repo/installment-commodity")
                .totalFiles(1)
                .filesScanned(1)
                .sqlFound(1)
                .uniqueSqlFound(1)
                .sqlStatements(List.of(skippedExplainSql()))
                .build());

        assertThat(report.getConfidence().getLevel()).isEqualTo("NEEDS_REVIEW");
        assertThat(report.getConfidence().getLimitations())
                .anySatisfy(limit -> assertThat(limit).contains("EXPLAIN"));
        assertThat(report.getMethodology().getKnownLimits())
                .anySatisfy(limit -> assertThat(limit).contains("Static analysis"));
    }

    @Test
    @DisplayName("should group injection findings into P0 remediation campaign")
    void should_group_injection_findings_into_p0_remediation_campaign() {
        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanPath("/repo/installment-commodity")
                .totalFiles(1)
                .filesScanned(1)
                .sqlFound(1)
                .uniqueSqlFound(1)
                .sqlStatements(List.of(dynamicInjectionSql()))
                .build());

        assertThat(report.getCampaigns())
                .anySatisfy(campaign -> {
                    assertThat(campaign.getId()).isEqualTo("p0-dynamic-sql-safety");
                    assertThat(campaign.getPriority()).isEqualTo("P0");
                    assertThat(campaign.getTheme()).isEqualTo("SAFETY");
                    assertThat(campaign.getFindingIds()).contains("dynamic-order");
                    assertThat(campaign.getAcceptanceChecklist())
                            .anySatisfy(item -> assertThat(item).contains("重新扫描"));
                });
    }

    @Test
    @DisplayName("should not turn clean skipped EXPLAIN items into remediation campaigns")
    void should_not_turn_clean_skipped_explain_items_into_remediation_campaigns() {
        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanPath("/repo/installment-commodity")
                .totalFiles(1)
                .filesScanned(1)
                .sqlFound(1)
                .uniqueSqlFound(1)
                .sqlStatements(List.of(skippedExplainSql()))
                .build());

        assertThat(report.getCampaigns())
                .noneMatch(campaign -> "p2-template-review".equals(campaign.getId()));
    }

    @Test
    @DisplayName("should group unknown findings into template review campaign")
    void should_group_unknown_findings_into_template_review_campaign() {
        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanPath("/repo/installment-commodity")
                .totalFiles(1)
                .filesScanned(1)
                .sqlFound(1)
                .uniqueSqlFound(1)
                .sqlStatements(List.of(riskySkippedExplainSql()))
                .build());

        assertThat(report.getCampaigns())
                .anySatisfy(campaign -> {
                    assertThat(campaign.getId()).isEqualTo("p2-template-review");
                    assertThat(campaign.getPriority()).isEqualTo("P2");
                    assertThat(campaign.getEvidenceLevel()).isEqualTo("NEEDS_REVIEW");
                    assertThat(campaign.getScope().getSqlCount()).isEqualTo(1);
                });
    }

    private static SqlStatementDto dynamicInjectionSql() {
        return SqlStatementDto.builder()
                .id("dynamic-order")
                .sqlType(SqlType.SELECT)
                .originalSql("SELECT id FROM users ORDER BY ${orderBy}")
                .abstractSql("SELECT id FROM users ORDER BY ${orderBy}")
                .validity(ValidityStatus.VALID)
                .explainEligibility(ExplainEligibility.NOT_SUPPORTED)
                .severity(SeverityLevel.CRITICAL)
                .score(55)
                .staticAnalysis(StaticAnalysisDto.builder()
                        .issues(List.of(StaticIssue.builder()
                                .type(IssueType.SQL_INJECTION_RISK)
                                .severity(SeverityLevel.CRITICAL)
                                .message("存在 ${} 动态拼接")
                                .suggestion("改为参数绑定或白名单映射")
                                .build()))
                        .build())
                .build();
    }

    private static SqlStatementDto skippedExplainSql() {
        return SqlStatementDto.builder()
                .id("skipped-query")
                .sqlType(SqlType.SELECT)
                .originalSql("SELECT * FROM orders")
                .abstractSql("SELECT * FROM orders")
                .validity(ValidityStatus.VALID)
                .explainEligibility(ExplainEligibility.SKIPPED)
                .severity(SeverityLevel.WARNING)
                .score(80)
                .build();
    }

    private static SqlStatementDto riskySkippedExplainSql() {
        return SqlStatementDto.builder()
                .id("unknown-template-query")
                .sqlType(SqlType.SELECT)
                .originalSql("SELECT id FROM orders WHERE <dynamic>")
                .abstractSql("SELECT id FROM orders WHERE ?")
                .validity(ValidityStatus.VALID)
                .explainEligibility(ExplainEligibility.SKIPPED)
                .severity(SeverityLevel.WARNING)
                .score(80)
                .staticAnalysis(StaticAnalysisDto.builder()
                        .issues(List.of(StaticIssue.builder()
                                .type(IssueType.UNKNOWN)
                                .severity(SeverityLevel.WARNING)
                                .message("动态模板需要人工复核")
                                .build()))
                        .build())
                .build();
    }
}
