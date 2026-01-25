package org.spectrum.sqlchecker.cli.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spectrum.sqlchecker.application.scan.dto.ScanExecutionRequest;
import org.spectrum.sqlchecker.application.scan.dto.ScanExecutionResult;
import org.spectrum.sqlchecker.application.scan.dto.ScanStatistics;
import org.spectrum.sqlchecker.application.scan.orchestrator.ScanOrchestrator;
import org.spectrum.sqlchecker.infrastructure.report.FallbackReportRenderer;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should delegate to orchestrator and write fallback report")
    void should_delegate_and_write_report() throws Exception {
        ScanCommand command = new ScanCommand();
        String outputPath = tempDir.resolve("report.html").toString();

        ScanExecutionResult result = ScanExecutionResult.builder()
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
                .scanResult(org.spectrum.sqlchecker.application.scan.dto.ScanResult.builder()
                        .scanId("scan-1")
                        .status(org.spectrum.sqlchecker.domain.shared.enumeration.ScanStatus.COMPLETED)
                        .filesScanned(1)
                        .sqlFound(1)
                        .uniqueSqlFound(1)
                        .durationMs(10)
                        .sqlStatements(new java.util.ArrayList<>())
                        .errors(new java.util.ArrayList<>())
                        .build())
                .build();

        when(orchestrator.execute(any(ScanExecutionRequest.class), any())).thenReturn(result);

        setField(command, "path", tempDir.toString());
        setField(command, "outputPath", outputPath);
        setField(command, "scanOrchestrator", orchestrator);
        setField(command, "fallbackReportRenderer", new FallbackReportRenderer());

        Integer exitCode = command.call();
        File reportFile = new File(outputPath);

        assertThat(exitCode).isEqualTo(0);
        assertThat(reportFile).exists();
        assertThat(reportFile.length()).isGreaterThan(0);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
