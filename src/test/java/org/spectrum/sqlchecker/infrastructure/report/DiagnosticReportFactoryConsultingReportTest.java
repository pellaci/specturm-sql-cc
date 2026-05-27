package org.spectrum.sqlchecker.infrastructure.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spectrum.sqlchecker.application.report.dto.DiagnosticReport;
import org.spectrum.sqlchecker.application.scan.dto.ScanResult;
import org.spectrum.sqlchecker.application.scan.dto.SqlStatementDto;
import org.spectrum.sqlchecker.domain.shared.enumeration.ExplainEligibility;
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
}
