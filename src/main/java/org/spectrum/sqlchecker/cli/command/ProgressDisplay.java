package org.spectrum.sqlchecker.cli.command;

import lombok.RequiredArgsConstructor;
import org.spectrum.sqlchecker.application.scan.ScanService;
import org.spectrum.sqlchecker.application.scan.dto.ScanProgress;
import org.spectrum.sqlchecker.application.scan.dto.ScanResult;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;
import org.springframework.stereotype.Component;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 进度显示器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class ProgressDisplay {

    private final ScanService scanService;
    private final PrintStream out = System.out;
    private final PrintStream err = System.err;

    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final String BLUE = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";

    /**
     * 显示扫描开始信息
     */
    public void showScanStart(Object request) {
        out.println();
        out.println(BLUE + "╔════════════════════════════════════════════════════════════════╗" + RESET);
        out.println(BLUE + "║" + RESET + BOLD + "              SQL Checker - SQL Quality Scanner              " + RESET + BLUE + "║" + RESET);
        out.println(BLUE + "╚════════════════════════════════════════════════════════════════╝" + RESET);
        out.println();
        out.println(CYAN + "Starting scan..." + RESET);
        out.println("  Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        out.println();
    }

    /**
     * 显示进度
     */
    public void showProgress(String scanId) {
        ScanProgress progress = scanService.getProgress(scanId);
        if (progress == null) {
            return;
        }

        int barWidth = 40;
        int filled = (int) (barWidth * progress.getProgress() / 100.0);

        out.print("\r[");
        for (int i = 0; i < barWidth; i++) {
            if (i < filled) {
                out.print(GREEN + "█" + RESET);
            } else {
                out.print("░");
            }
        }
        out.print(String.format("] %d%% (%s) - %d files, %d SQL",
                progress.getProgress(),
                progress.getStage(),
                progress.getFilesScanned(),
                progress.getSqlFound()));

        if (progress.getCurrentFile() != null && !progress.getCurrentFile().isEmpty()) {
            out.print(" - " + truncate(progress.getCurrentFile(), 30));
        }

        if (progress.getStatus().isCompleted()) {
            out.println();
        }
    }

    /**
     * 显示报告生成信息
     */
    public void showReportGeneration(String outputPath) {
        out.println();
        out.println(CYAN + "Generating report..." + RESET);
        out.println("  Output: " + outputPath);
        out.println();
    }

    /**
     * 显示扫描结果
     */
    public void showScanResult(ScanResult result) {
        out.println();
        out.println(BLUE + "╔════════════════════════════════════════════════════════════════╗" + RESET);
        out.println(BLUE + "║" + RESET + BOLD + "                         Scan Complete                            " + RESET + BLUE + "║" + RESET);
        out.println(BLUE + "╚════════════════════════════════════════════════════════════════╝" + RESET);
        out.println();

        // 统计信息
        out.println(BOLD + "Statistics:" + RESET);
        out.println("  Files scanned:  " + GREEN + result.getFilesScanned() + RESET);
        out.println("  SQL found:      " + GREEN + result.getSqlFound() + RESET);
        out.println("  Unique SQL:     " + GREEN + result.getUniqueSqlFound() + RESET);
        out.println("  Duration:       " + (result.getDurationMs() / 1000.0) + "s");
        out.println();

        // 问题统计
        long critical = result.getSqlStatements().stream()
                .filter(s -> s.getSeverity() == SeverityLevel.CRITICAL)
                .count();
        long warning = result.getSqlStatements().stream()
                .filter(s -> s.getSeverity() == SeverityLevel.WARNING)
                .count();
        long info = result.getSqlStatements().stream()
                .filter(s -> s.getSeverity() == SeverityLevel.INFO)
                .count();

        out.println(BOLD + "Issues:" + RESET);
        if (critical > 0) {
            out.println("  " + RED + "●" + RESET + " Critical:  " + critical);
        }
        if (warning > 0) {
            out.println("  " + YELLOW + "●" + RESET + " Warning:   " + warning);
        }
        if (info > 0) {
            out.println("  " + CYAN + "●" + RESET + " Info:      " + info);
        }
        if (critical == 0 && warning == 0 && info == 0) {
            out.println("  " + GREEN + "✓" + RESET + " No issues found!");
        }
        out.println();

        // 错误信息
        if (!result.getErrors().isEmpty()) {
            out.println(BOLD + RED + "Errors:" + RESET);
            result.getErrors().stream()
                    .limit(5)
                    .forEach(e -> out.println("  " + RED + "✗" + RESET + " " + e.getFilePath() + ": " + e.getMessage()));
            if (result.getErrors().size() > 5) {
                out.println("  ... and " + (result.getErrors().size() - 5) + " more errors");
            }
            out.println();
        }

        // 报告路径
        out.println(GREEN + "✓" + RESET + " Scan completed successfully!");
        out.println();
    }

    /**
     * 显示错误
     */
    public void showError(String message) {
        err.println();
        err.println(RED + "✗ Error: " + message + RESET);
        err.println();
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int max) {
        if (str == null) {
            return "";
        }
        if (str.length() <= max) {
            return str;
        }
        return "..." + str.substring(str.length() - max + 3);
    }
}
