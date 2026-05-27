package org.spectrum.sqlchecker.infrastructure.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spectrum.sqlchecker.application.analysis.dto.StaticAnalysisDto;
import org.spectrum.sqlchecker.application.analysis.dto.StaticIssue;
import org.spectrum.sqlchecker.application.report.dto.DiagnosticReport;
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
                .contains("Manual review");
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
    @DisplayName("should group skipped explain findings into review campaign")
    void should_group_skipped_explain_findings_into_review_campaign() {
        DiagnosticReport report = DiagnosticReportFactory.from(ScanResult.builder()
                .scanPath("/repo/installment-commodity")
                .totalFiles(1)
                .filesScanned(1)
                .sqlFound(1)
                .uniqueSqlFound(1)
                .sqlStatements(List.of(skippedExplainSql()))
                .build());

        assertThat(report.getCampaigns())
                .anySatisfy(campaign -> {
                    assertThat(campaign.getId()).isEqualTo("p2-evidence-completion");
                    assertThat(campaign.getPriority()).isEqualTo("P2");
                    assertThat(campaign.getEvidenceLevel()).isEqualTo("NEEDS_REVIEW");
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
}
