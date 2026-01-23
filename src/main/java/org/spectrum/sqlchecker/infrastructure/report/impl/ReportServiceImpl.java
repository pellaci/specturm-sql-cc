package org.spectrum.sqlchecker.infrastructure.report.impl;

import io.pebbletemplates.pebble.PebbleEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.application.report.ReportService;
import org.spectrum.sqlchecker.application.report.dto.ReportSummary;
import org.spectrum.sqlchecker.application.scan.dto.ScanResult;
import org.spectrum.sqlchecker.domain.shared.exception.ScanException;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 报告服务实现
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final PebbleEngine pebbleEngine;

    @Override
    public void generateHtmlReport(ScanResult scanResult, String outputPath) throws ScanException {
        try {
            Path path = Paths.get(outputPath);
            Files.createDirectories(path.getParent());

            try (OutputStream out = new FileOutputStream(outputPath);
                 OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                generateHtmlReport(scanResult, writer);
            }

            log.info("Report generated: {}", outputPath);

        } catch (IOException e) {
            throw new ScanException("Failed to generate report: " + outputPath, e);
        }
    }

    @Override
    public void generateHtmlReport(ScanResult scanResult, OutputStream outputStream) throws ScanException {
        try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            generateHtmlReport(scanResult, writer);
        } catch (IOException e) {
            throw new ScanException("Failed to generate report", e);
        }
    }

    private void generateHtmlReport(ScanResult scanResult, OutputStreamWriter writer) throws IOException {
        Map<String, Object> context = new HashMap<>();
        context.put("summary", generateSummary(scanResult));
        context.put("sqlStatements", scanResult.getSqlStatements());

        pebbleEngine.getTemplate("report").evaluate(writer, context);
    }

    @Override
    public ReportSummary generateSummary(ScanResult scanResult) {
        int criticalIssues = 0;
        int warningIssues = 0;
        int infoIssues = 0;
        int totalScore = 0;

        for (var sql : scanResult.getSqlStatements()) {
            if (sql.getSeverity() != null) {
                switch (sql.getSeverity()) {
                    case CRITICAL -> criticalIssues++;
                    case WARNING -> warningIssues++;
                    case INFO -> infoIssues++;
                }
            }
            if (sql.getScore() != null) {
                totalScore += sql.getScore();
            }
        }

        double avgScore = scanResult.getSqlStatements().isEmpty() ? 100.0 :
                (double) totalScore / scanResult.getSqlStatements().size();

        return ReportSummary.builder()
                .totalSql(scanResult.getSqlFound())
                .totalIssues(criticalIssues + warningIssues + infoIssues)
                .criticalIssues(criticalIssues)
                .warningIssues(warningIssues)
                .infoIssues(infoIssues)
                .durationMs(scanResult.getDurationMs())
                .averageScore(avgScore)
                .generatedAt(Instant.now())
                .build();
    }

    @Override
    public void generateHtmlReportByScanId(String scanId, String outputPath) throws ScanException {
        // 简化实现，实际应从存储中获取 ScanResult
        ScanResult mockResult = ScanResult.builder()
                .scanId(scanId)
                .status(org.spectrum.sqlchecker.domain.shared.enumeration.ScanStatus.COMPLETED)
                .filesScanned(0)
                .sqlFound(0)
                .uniqueSqlFound(0)
                .durationMs(0)
                .sqlStatements(new java.util.ArrayList<>())
                .errors(new java.util.ArrayList<>())
                .build();

        generateHtmlReport(mockResult, outputPath);
    }
}
