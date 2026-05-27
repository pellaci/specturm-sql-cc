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

class DiagnosticReportFactoryRemediationTest {

    @Test
    @DisplayName("should expose remediation summary tasks and recipes")
    void should_expose_remediation_summary_tasks_and_recipes() {
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

        assertThat(report.getRemediation()).isNotNull();
        assertThat(report.getRemediation().getSummary()).isNotNull();
        assertThat(report.getRemediation().getTasks()).isNotNull();
        assertThat(report.getRemediation().getRecipes()).isNotNull();
    }
}
