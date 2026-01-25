package org.spectrum.sqlchecker.infrastructure.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spectrum.sqlchecker.application.report.ReportService;
import org.spectrum.sqlchecker.application.report.dto.ReportSummary;
import org.spectrum.sqlchecker.application.scan.dto.ScanResult;
import org.spectrum.sqlchecker.domain.shared.enumeration.ScanStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

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

    private org.spectrum.sqlchecker.application.scan.dto.SqlStatementDto createSqlWithSeverity(
            org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel severity) {
        return org.spectrum.sqlchecker.application.scan.dto.SqlStatementDto.builder()
                .id("sql-" + System.nanoTime())
                .originalSql("SELECT * FROM test")
                .severity(severity)
                .score(80)
                .build();
    }
}
