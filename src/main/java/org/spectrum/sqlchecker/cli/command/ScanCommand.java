package org.spectrum.sqlchecker.cli.command;

import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.application.report.ReportService;
import org.spectrum.sqlchecker.application.report.dto.DiagnosticReport;
import org.spectrum.sqlchecker.application.scan.dto.ScanExecutionRequest;
import org.spectrum.sqlchecker.application.scan.dto.ScanExecutionResult;
import org.spectrum.sqlchecker.application.scan.dto.ScanProgressSnapshot;
import org.spectrum.sqlchecker.application.scan.dto.ScanStartInfo;
import org.spectrum.sqlchecker.application.scan.dto.ScanStatistics;
import org.spectrum.sqlchecker.application.scan.orchestrator.ScanOrchestrator;
import org.spectrum.sqlchecker.application.scan.orchestrator.ScanProgressListener;
import org.spectrum.sqlchecker.domain.shared.exception.ScanException;
import org.spectrum.sqlchecker.infrastructure.report.DiagnosticReportFactory;
import org.spectrum.sqlchecker.infrastructure.report.DiagnosticReportJsonSerializer;
import org.spectrum.sqlchecker.infrastructure.report.FallbackReportRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * 扫描命令
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Slf4j
@Component
@Command(
        name = "scan",
        description = "Scan a codebase for SQL statements",
        mixinStandardHelpOptions = true
)
public class ScanCommand implements Callable<Integer> {

    @Option(names = {"-p", "--path"}, description = "Path to the codebase to scan", defaultValue = ".")
    private String path;

    @Option(names = {"-o", "--output"}, description = "Output file path", defaultValue = "reports/sql-checker-report.html")
    private String outputPath;

    @Option(names = {"-v", "--verbose"}, description = "Verbose output")
    private boolean verbose;

    @Option(names = {"--no-progress"}, description = "Disable progress bar")
    private boolean noProgress;

    @Option(names = {"--enable-explain"}, description = "Enable EXPLAIN analysis (requires database connection)")
    private boolean enableExplain;

    @Option(names = {"--db-connection"}, description = "Database connection name to use for EXPLAIN", defaultValue = "default")
    private String dbConnection = "default";

    @Option(names = {"--init-schema"}, description = "Auto-initialize database schema before EXPLAIN analysis")
    private boolean initSchema;

    @Option(names = {"--schema-path"}, description = "Path to DDL files (default: same as scan path)")
    private String schemaPath;

    @Autowired(required = false)
    private ProgressDisplay progressDisplay;

    @Autowired(required = false)
    private ReportService reportService;

    @Autowired
    private ScanOrchestrator scanOrchestrator;

    @Autowired(required = false)
    private FallbackReportRenderer fallbackReportRenderer;

    @Override
    public Integer call() throws Exception {
        ScanExecutionRequest request = ScanExecutionRequest.builder()
                .path(path)
                .verbose(verbose)
                .enableExplain(enableExplain)
                .dbConnection(dbConnection)
                .initSchema(initSchema)
                .schemaPath(schemaPath)
                .build();

        ScanProgressListener listener = buildProgressListener();
        ScanExecutionResult result = scanOrchestrator.execute(request, listener);

        String output = writeReport(result);
        showResult(result, output, deriveJsonOutputPath(output));

        return 0;
    }

    private ScanProgressListener buildProgressListener() {
        if (progressDisplay == null || noProgress) {
            return null;
        }
        return new ScanProgressListener() {
            @Override
            public void onStart(ScanStartInfo info) {
                progressDisplay.showScanStart(info.getPath(), info.getTotalFiles(), info.getJavaFiles(), info.getXmlFiles(), info.getSqlFiles());
            }

            @Override
            public void onProgress(ScanProgressSnapshot snapshot) {
                progressDisplay.showProgress(snapshot.getProgress(), snapshot.getStage(), snapshot.getFilesScanned(),
                        snapshot.getTotalFiles(), snapshot.getSqlFound(), snapshot.getCurrentFile());
            }

            @Override
            public void onComplete() {
                progressDisplay.println();
            }
        };
    }

    private String writeReport(ScanExecutionResult result) throws ScanException {
        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null) {
            parentDir.mkdirs();
        }
        String absolutePath = outputFile.getAbsolutePath();

        if (progressDisplay != null) {
            progressDisplay.showReportGeneration(absolutePath);
        }

        try {
            if (reportService != null) {
                reportService.generateHtmlReport(result.getScanResult(), absolutePath);
                reportService.generateJsonReport(result.getScanResult(), deriveJsonOutputPath(absolutePath));
                return absolutePath;
            }
            return writeFallbackReport(result, absolutePath);
        } catch (Exception e) {
            log.warn("Failed to generate report with ReportService, fallback to simple report", e);
            return writeFallbackReport(result, absolutePath);
        }
    }

    private String writeFallbackReport(ScanExecutionResult result, String outputPath) throws ScanException {
        if (fallbackReportRenderer == null) {
            throw new ScanException("FallbackReportRenderer not available");
        }
        String html = fallbackReportRenderer.render(result);
        try {
            Files.writeString(Path.of(outputPath), html);
            Files.writeString(
                    Path.of(deriveJsonOutputPath(outputPath)),
                    DiagnosticReportJsonSerializer.toJson(DiagnosticReportFactory.from(result.getScanResult()))
            );
            return outputPath;
        } catch (IOException e) {
            throw new ScanException("Failed to write fallback report", e);
        }
    }

    private String deriveJsonOutputPath(String htmlOutputPath) {
        if (htmlOutputPath == null || htmlOutputPath.isBlank()) {
            return "report.json";
        }
        String lower = htmlOutputPath.toLowerCase();
        if (lower.endsWith(".html")) {
            return htmlOutputPath.substring(0, htmlOutputPath.length() - 5) + ".json";
        }
        if (lower.endsWith(".htm")) {
            return htmlOutputPath.substring(0, htmlOutputPath.length() - 4) + ".json";
        }
        return htmlOutputPath + ".json";
    }

    private void showResult(ScanExecutionResult result, String reportPath, String jsonReportPath) {
        ScanStatistics stats = result.getStatistics();
        DiagnosticReport report = DiagnosticReportFactory.from(result.getScanResult());
        DiagnosticReport.Counts counts = report.getSummary().getCounts();
        DiagnosticReport.Coverage coverage = report.getSummary().getCoverage();
        int parseFailures = report.getDiagnostics().getParseFailures() == null
                ? 0
                : report.getDiagnostics().getParseFailures().size();
        int manualReview = report.getDiagnostics().getManualReview() == null
                ? 0
                : report.getDiagnostics().getManualReview().size();
        int skippedExplain = report.getDiagnostics().getSkippedExplain() == null
                ? 0
                : report.getDiagnostics().getSkippedExplain().size();
        if (progressDisplay != null) {
            progressDisplay.showSimpleResult(
                    stats.getTotalFiles(),
                    stats.getJavaFiles(),
                    stats.getXmlFiles(),
                    stats.getSqlFiles(),
                    counts.getTotalSql(),
                    counts.getUniqueSql(),
                    coverage.getParseRate(),
                    parseFailures,
                    manualReview,
                    skippedExplain,
                    counts.getCriticalIssues(),
                    counts.getWarningIssues(),
                    counts.getInfoIssues(),
                    stats.getDurationMs(),
                    reportPath,
                    jsonReportPath
            );
            return;
        }

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Scan Results");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  Files scanned: " + stats.getTotalFiles() + " (" + stats.getJavaFiles() + " Java, " + stats.getXmlFiles() + " XML, " + stats.getSqlFiles() + " SQL)");
        System.out.println("  SQL occurrences: " + counts.getTotalSql());
        System.out.println("  Unique SQL:       " + counts.getUniqueSql());
        System.out.printf("  Parse coverage:   %.1f%%%n", coverage.getParseRate());
        System.out.println("  Parse failures:   " + parseFailures);
        System.out.println("  Manual review:    " + manualReview);
        System.out.println("  EXPLAIN skipped:  " + skippedExplain);
        System.out.println("  Duration:         " + stats.getDurationMs() + "ms");
        System.out.println();
        System.out.println("Issue Summary:");
        System.out.println("  Critical: " + counts.getCriticalIssues());
        System.out.println("  Warning:  " + counts.getWarningIssues());
        System.out.println("  Info:     " + counts.getInfoIssues());
        System.out.println();
        System.out.println("HTML report: " + reportPath);
        System.out.println("JSON report: " + jsonReportPath);
        System.out.println();
    }
}
