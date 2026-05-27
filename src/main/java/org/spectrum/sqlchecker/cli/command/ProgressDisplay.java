package org.spectrum.sqlchecker.cli.command;

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
 * @since 2.0.0
 */
@Component
public class ProgressDisplay {

    private final PrintStream out = System.out;
    private final PrintStream err = System.err;

    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final String BLUE = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    private static final String GRAY = "\u001B[90m";

    /**
     * 显示扫描开始信息
     */
    public void showScanStart(String path, int totalFiles, int javaFiles, int xmlFiles, int sqlFiles) {
        out.println();
        out.println(BLUE + "╔════════════════════════════════════════════════════════════════╗" + RESET);
        out.println(BLUE + "║" + RESET + BOLD + "              SQL Checker - SQL Quality Scanner              " + RESET + BLUE + "║" + RESET);
        out.println(BLUE + "╚════════════════════════════════════════════════════════════════╝" + RESET);
        out.println();
        out.println(CYAN + "Starting scan..." + RESET);
        out.println("  Path:  " + path);
        out.println("  Files: " + totalFiles + " (" + javaFiles + " Java, " + xmlFiles + " XML, " + sqlFiles + " SQL)");
        out.println("  Time:  " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        out.println();
    }

    /**
     * 显示扫描进度
     *
     * @param progress 当前进度百分比 (0-100)
     * @param stage 当前阶段
     * @param filesScanned 已扫描文件数
     * @param totalFiles 总文件数
     * @param sqlFound 已发现 SQL 数量
     * @param currentFile 当前处理文件
     */
    public void showProgress(int progress, String stage, int filesScanned, int totalFiles, int sqlFound, String currentFile) {
        int barWidth = 40;
        int filled = (int) (barWidth * progress / 100.0);

        out.print("\r[");
        for (int i = 0; i < barWidth; i++) {
            if (i < filled) {
                out.print(GREEN + "█" + RESET);
            } else {
                out.print(GRAY + "░" + RESET);
            }
        }
        out.print(String.format("] %3d%% %s | %d/%d files | %d SQL",
                progress, stage, filesScanned, totalFiles, sqlFound));

        if (currentFile != null && !currentFile.isEmpty()) {
            out.print(" | " + truncate(currentFile, 25));
        }

        out.flush();
    }

    /**
     * 换行（用于进度条结束）
     */
    public void println() {
        out.println();
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
        out.println("  Duration:       " + String.format("%.2f", result.getDurationMs() / 1000.0) + "s");
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
        if (result.getErrors() != null && !result.getErrors().isEmpty()) {
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
     * 显示简单扫描结果（用于 ScanCommand 的内联显示）
     */
    public void showSimpleResult(int totalFiles, int javaFiles, int xmlFiles, int sqlFiles, int sqlFound, int sqlParsed,
                                  int critical, int warning, int info, long duration, String reportPath) {
        showSimpleResult(totalFiles, javaFiles, xmlFiles, sqlFiles, sqlFound, sqlParsed,
                critical, warning, info, duration, reportPath, null);
    }

    public void showSimpleResult(int totalFiles, int javaFiles, int xmlFiles, int sqlFiles, int sqlFound, int sqlParsed,
                                  int critical, int warning, int info, long duration, String reportPath, String jsonReportPath) {
        double parseRate = sqlFound == 0 ? 0.0 : Math.min(sqlParsed, sqlFound) * 100.0 / sqlFound;
        int parseFailures = Math.max(sqlFound - sqlParsed, 0);
        showSimpleResult(totalFiles, javaFiles, xmlFiles, sqlFiles, sqlFound, sqlParsed, parseRate, parseFailures,
                critical, warning, info, duration, reportPath, jsonReportPath);
    }

    public void showSimpleResult(int totalFiles, int javaFiles, int xmlFiles, int sqlFiles, int sqlFound, int uniqueSql,
                                  double parseRate, int parseFailures, int critical, int warning, int info,
                                  long duration, String reportPath, String jsonReportPath) {
        showSimpleResult(totalFiles, javaFiles, xmlFiles, sqlFiles, sqlFound, uniqueSql, parseRate, parseFailures,
                0, 0, critical, warning, info, duration, reportPath, jsonReportPath);
    }

    public void showSimpleResult(int totalFiles, int javaFiles, int xmlFiles, int sqlFiles, int sqlFound, int uniqueSql,
                                  double parseRate, int parseFailures, int manualReview, int skippedExplain,
                                  int critical, int warning, int info, long duration, String reportPath, String jsonReportPath) {
        out.println();
        out.println("═══════════════════════════════════════════════════════════════");
        out.println(BOLD + "Scan Results" + RESET);
        out.println("═══════════════════════════════════════════════════════════════");
        out.println("  Files scanned: " + totalFiles + " (" + javaFiles + " Java, " + xmlFiles + " XML, " + sqlFiles + " SQL)");
        out.println("  SQL occurrences: " + sqlFound);
        out.println("  Unique SQL:       " + uniqueSql);
        out.println("  Parse coverage:   " + String.format("%.1f%%", parseRate));
        out.println("  Parse failures:   " + parseFailures);
        out.println("  Manual review:    " + manualReview);
        out.println("  EXPLAIN skipped:  " + skippedExplain);
        out.println("  Duration:         " + duration + "ms");
        out.println();

        out.println(BOLD + "Issue Summary:" + RESET);
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
            out.println("  " + GREEN + "✅" + RESET + " No issues found!");
        }
        out.println();

        out.println(GREEN + "📄" + RESET + " HTML report: " + reportPath);
        if (jsonReportPath != null && !jsonReportPath.isBlank()) {
            out.println(GREEN + "📦" + RESET + " JSON report: " + jsonReportPath);
        }
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
     * 显示警告
     */
    public void showWarning(String message) {
        out.println(YELLOW + "⚠ " + message + RESET);
    }

    /**
     * 显示信息
     */
    public void showInfo(String message) {
        out.println(CYAN + "ℹ " + message + RESET);
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
