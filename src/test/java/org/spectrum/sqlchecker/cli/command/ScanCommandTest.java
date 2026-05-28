package org.spectrum.sqlchecker.cli.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spectrum.sqlchecker.application.report.ReportService;
import org.spectrum.sqlchecker.application.scan.dto.ScanExecutionRequest;
import org.spectrum.sqlchecker.application.scan.dto.ScanExecutionResult;
import org.spectrum.sqlchecker.application.scan.dto.ScanResult;
import org.spectrum.sqlchecker.application.scan.dto.ScanStatistics;
import org.spectrum.sqlchecker.application.scan.dto.SqlStatementDto;
import org.spectrum.sqlchecker.application.scan.orchestrator.ScanOrchestrator;
import org.spectrum.sqlchecker.application.scan.orchestrator.ScanProgressListener;
import org.spectrum.sqlchecker.domain.shared.enumeration.ExplainEligibility;
import org.spectrum.sqlchecker.domain.shared.enumeration.ScanStatus;
import org.spectrum.sqlchecker.domain.shared.enumeration.ValidityStatus;
import org.spectrum.sqlchecker.domain.shared.exception.ScanException;
import org.spectrum.sqlchecker.infrastructure.report.FallbackReportRenderer;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ScanCommand 单元测试（CLI 仅编排）
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScanCommand 单元测试")
class ScanCommandTest {

    @Mock
    private ScanOrchestrator orchestrator;

    @Mock
    private ReportService reportService;

    @Mock
    private ProgressDisplay progressDisplay;

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should delegate to orchestrator and write fallback report")
    void should_delegate_and_write_report() throws Exception {
        ScanCommand command = new ScanCommand();
        String outputPath = tempDir.resolve("report.html").toString();

        when(orchestrator.execute(any(ScanExecutionRequest.class), isNull())).thenReturn(createResult());

        setField(command, "path", tempDir.toString());
        setField(command, "outputPath", outputPath);
        setField(command, "noProgress", true);
        setField(command, "scanOrchestrator", orchestrator);
        setField(command, "fallbackReportRenderer", new FallbackReportRenderer());

        Integer exitCode = command.call();
        File reportFile = new File(outputPath);
        File jsonFile = tempDir.resolve("report.json").toFile();

        assertThat(exitCode).isEqualTo(0);
        assertThat(reportFile).exists();
        assertThat(jsonFile).exists();
        assertThat(reportFile.length()).isGreaterThan(0);
    }

    @Test
    @DisplayName("should pass CLI options to orchestrator request")
    void should_pass_cli_options_to_orchestrator_request() throws Exception {
        ScanCommand command = new ScanCommand();
        ArgumentCaptor<ScanExecutionRequest> requestCaptor = ArgumentCaptor.forClass(ScanExecutionRequest.class);

        when(orchestrator.execute(requestCaptor.capture(), isNull())).thenReturn(createResult());

        setField(command, "path", tempDir.resolve("repo").toString());
        setField(command, "outputPath", tempDir.resolve("report.html").toString());
        setField(command, "verbose", true);
        setField(command, "noProgress", true);
        setField(command, "enableExplain", true);
        setField(command, "dbConnection", "readonly");
        setField(command, "initSchema", true);
        setField(command, "schemaPath", tempDir.resolve("schema").toString());
        setField(command, "scanOrchestrator", orchestrator);
        setField(command, "fallbackReportRenderer", new FallbackReportRenderer());

        Integer exitCode = command.call();

        assertThat(exitCode).isEqualTo(0);
        assertThat(requestCaptor.getValue())
                .extracting(
                        ScanExecutionRequest::getPath,
                        ScanExecutionRequest::isVerbose,
                        ScanExecutionRequest::isEnableExplain,
                        ScanExecutionRequest::getDbConnection,
                        ScanExecutionRequest::isInitSchema,
                        ScanExecutionRequest::getSchemaPath
                )
                .containsExactly(
                        tempDir.resolve("repo").toString(),
                        true,
                        true,
                        "readonly",
                        true,
                        tempDir.resolve("schema").toString()
                );
    }

    @Test
    @DisplayName("should build progress listener when progress display is available")
    void should_build_progress_listener_when_progress_display_available() throws Exception {
        ScanCommand command = new ScanCommand();
        ArgumentCaptor<ScanProgressListener> listenerCaptor = ArgumentCaptor.forClass(ScanProgressListener.class);

        when(orchestrator.execute(any(ScanExecutionRequest.class), listenerCaptor.capture())).thenReturn(createResult());

        setField(command, "path", tempDir.toString());
        setField(command, "outputPath", tempDir.resolve("report.html").toString());
        setField(command, "progressDisplay", progressDisplay);
        setField(command, "scanOrchestrator", orchestrator);
        setField(command, "fallbackReportRenderer", new FallbackReportRenderer());

        Integer exitCode = command.call();

        assertThat(exitCode).isEqualTo(0);
        assertThat(listenerCaptor.getValue()).isNotNull();
        verify(progressDisplay).showReportGeneration(anyString());
        verify(progressDisplay).showSimpleResult(eq(1), eq(1), eq(0), eq(0), eq(1), eq(1), eq(100.0), eq(0),
                eq(0), eq(1), eq(0), eq(0), eq(0), eq(0), eq(0), eq(0), eq(0), eq(10L), anyString(), anyString());
    }

    @Test
    @DisplayName("should not build progress listener when no-progress is enabled")
    void should_not_build_progress_listener_when_no_progress_enabled() throws Exception {
        ScanCommand command = new ScanCommand();

        when(orchestrator.execute(any(ScanExecutionRequest.class), isNull())).thenReturn(createResult());

        setField(command, "path", tempDir.toString());
        setField(command, "outputPath", tempDir.resolve("report.html").toString());
        setField(command, "noProgress", true);
        setField(command, "progressDisplay", progressDisplay);
        setField(command, "scanOrchestrator", orchestrator);
        setField(command, "fallbackReportRenderer", new FallbackReportRenderer());

        Integer exitCode = command.call();

        assertThat(exitCode).isEqualTo(0);
        verify(progressDisplay).showReportGeneration(anyString());
        verify(progressDisplay).showSimpleResult(eq(1), eq(1), eq(0), eq(0), eq(1), eq(1), eq(100.0), eq(0),
                eq(0), eq(1), eq(0), eq(0), eq(0), eq(0), eq(0), eq(0), eq(0), eq(10L), anyString(), anyString());
    }

    @Test
    @DisplayName("should use report service when available")
    void should_use_report_service_when_available() throws Exception {
        ScanCommand command = new ScanCommand();
        ScanExecutionResult result = createResult();
        String outputPath = tempDir.resolve("service-report.html").toString();

        when(orchestrator.execute(any(ScanExecutionRequest.class), isNull())).thenReturn(result);

        setField(command, "path", tempDir.toString());
        setField(command, "outputPath", outputPath);
        setField(command, "noProgress", true);
        setField(command, "reportService", reportService);
        setField(command, "scanOrchestrator", orchestrator);
        setField(command, "fallbackReportRenderer", new FallbackReportRenderer());

        Integer exitCode = command.call();

        assertThat(exitCode).isEqualTo(0);
        verify(reportService).generateHtmlReport(result.getScanResult(), new File(outputPath).getAbsolutePath());
        verify(reportService).generateJsonReport(result.getScanResult(),
                tempDir.resolve("service-report.json").toFile().getAbsolutePath());
        assertThat(new File(outputPath)).doesNotExist();
    }

    @Test
    @DisplayName("should fallback when report service fails")
    void should_fallback_when_report_service_fails() throws Exception {
        ScanCommand command = new ScanCommand();
        ScanExecutionResult result = createResult();
        String outputPath = tempDir.resolve("fallback-report.html").toString();

        when(orchestrator.execute(any(ScanExecutionRequest.class), isNull())).thenReturn(result);
        doThrow(new ScanException("boom"))
                .when(reportService).generateHtmlReport(result.getScanResult(), new File(outputPath).getAbsolutePath());

        setField(command, "path", tempDir.toString());
        setField(command, "outputPath", outputPath);
        setField(command, "noProgress", true);
        setField(command, "reportService", reportService);
        setField(command, "scanOrchestrator", orchestrator);
        setField(command, "fallbackReportRenderer", new FallbackReportRenderer());

        Integer exitCode = command.call();

        assertThat(exitCode).isEqualTo(0);
        assertThat(new File(outputPath)).exists();
        assertThat(tempDir.resolve("fallback-report.json")).exists();
    }

    @Test
    @DisplayName("should throw when neither report service nor fallback renderer can write")
    void should_throw_when_fallback_renderer_missing() throws Exception {
        ScanCommand command = new ScanCommand();

        when(orchestrator.execute(any(ScanExecutionRequest.class), isNull())).thenReturn(createResult());

        setField(command, "path", tempDir.toString());
        setField(command, "outputPath", tempDir.resolve("missing-renderer.html").toString());
        setField(command, "noProgress", true);
        setField(command, "scanOrchestrator", orchestrator);

        assertThatThrownBy(command::call)
                .isInstanceOf(ScanException.class)
                .hasMessageContaining("FallbackReportRenderer not available");
    }

    private ScanExecutionResult createResult() {
        return ScanExecutionResult.builder()
                .scanPath(tempDir.toString())
                .statistics(ScanStatistics.builder()
                        .totalFiles(1)
                        .javaFiles(1)
                        .xmlFiles(0)
                        .sqlFiles(0)
                        .filesScanned(1)
                        .sqlFound(1)
                        .sqlParsed(1)
                        .durationMs(10)
                        .criticalIssues(0)
                        .warningIssues(0)
                        .infoIssues(0)
                        .build())
                .scanResult(ScanResult.builder()
                        .scanId("scan-1")
                        .status(ScanStatus.COMPLETED)
                        .filesScanned(1)
                        .sqlFound(1)
                        .uniqueSqlFound(1)
                        .durationMs(10)
                        .sqlStatements(List.of(SqlStatementDto.builder()
                                .id("sql-1")
                                .originalSql("select 1")
                                .normalizedSql("select 1")
                                .validity(ValidityStatus.VALID)
                                .explainEligibility(ExplainEligibility.SKIPPED)
                                .preprocessErrorReason("数据库未配置，跳过 EXPLAIN")
                                .build()))
                        .errors(new ArrayList<>())
                        .build())
                .build();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
