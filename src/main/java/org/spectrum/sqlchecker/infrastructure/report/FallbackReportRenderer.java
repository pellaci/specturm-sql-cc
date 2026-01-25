package org.spectrum.sqlchecker.infrastructure.report;

import org.spectrum.sqlchecker.application.scan.dto.ScanExecutionResult;
import org.spectrum.sqlchecker.application.scan.dto.ScanIssue;
import org.spectrum.sqlchecker.application.scan.dto.ScanSqlEntry;
import org.spectrum.sqlchecker.application.scan.dto.ScanStatistics;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 兜底 HTML 报告渲染器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Component
public class FallbackReportRenderer {

    public String render(ScanExecutionResult result) {
        StringBuilder sb = new StringBuilder();
        ScanStatistics stats = result.getStatistics();

        sb.append("<!DOCTYPE html>\n");
        sb.append("<html lang=\"zh-CN\">\n");
        sb.append("<head>\n");
        sb.append("    <meta charset=\"UTF-8\">\n");
        sb.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("    <title>SQL Checker Report</title>\n");
        sb.append("    <style>\n");
        sb.append(getCssStyles());
        sb.append("    </style>\n");
        sb.append("</head>\n");
        sb.append("<body>\n");

        sb.append("    <div class=\"container\">\n");
        sb.append("        <header class=\"header\">\n");
        sb.append("            <h1>🔍 SQL Quality Checker Report</h1>\n");
        sb.append("            <p class=\"subtitle\">Scan Path: ").append(escapeHtml(new File(result.getScanPath()).getAbsolutePath())).append("</p>\n");
        sb.append("        </header>\n");

        sb.append("        <div class=\"stats-grid\">\n");
        sb.append("            <div class=\"stat-card\">\n");
        sb.append("                <div class=\"stat-value\">").append(stats.getTotalFiles()).append("</div>\n");
        sb.append("                <div class=\"stat-label\">Files Scanned</div>\n");
        sb.append("                <div class=\"stat-detail\">").append(stats.getJavaFiles()).append(" Java + ").append(stats.getXmlFiles()).append(" XML</div>\n");
        sb.append("            </div>\n");
        sb.append("            <div class=\"stat-card\">\n");
        sb.append("                <div class=\"stat-value\">").append(stats.getSqlFound()).append("</div>\n");
        sb.append("                <div class=\"stat-label\">SQL Found</div>\n");
        sb.append("                <div class=\"stat-detail\">").append(stats.getSqlParsed()).append(" Parsed</div>\n");
        sb.append("            </div>\n");
        sb.append("            <div class=\"stat-card stat-critical\">\n");
        sb.append("                <div class=\"stat-value\">").append(stats.getCriticalIssues()).append("</div>\n");
        sb.append("                <div class=\"stat-label\">Critical</div>\n");
        sb.append("            </div>\n");
        sb.append("            <div class=\"stat-card stat-warning\">\n");
        sb.append("                <div class=\"stat-value\">").append(stats.getWarningIssues()).append("</div>\n");
        sb.append("                <div class=\"stat-label\">Warning</div>\n");
        sb.append("            </div>\n");
        sb.append("            <div class=\"stat-card stat-info\">\n");
        sb.append("                <div class=\"stat-value\">").append(stats.getInfoIssues()).append("</div>\n");
        sb.append("                <div class=\"stat-label\">Info</div>\n");
        sb.append("            </div>\n");
        sb.append("        </div>\n");

        List<ScanIssue> allIssues = result.getIssues() != null ? result.getIssues() : new ArrayList<>();
        if (!allIssues.isEmpty()) {
            sb.append("        <div class=\"issues-section\">\n");
            sb.append("            <h2>Issues Found</h2>\n");

            Map<String, List<ScanIssue>> bySeverity = new LinkedHashMap<>();
            bySeverity.put("CRITICAL", new ArrayList<>());
            bySeverity.put("WARNING", new ArrayList<>());
            bySeverity.put("INFO", new ArrayList<>());

            for (ScanIssue issue : allIssues) {
                bySeverity.getOrDefault(issue.getSeverity(), bySeverity.get("INFO")).add(issue);
            }

            for (Map.Entry<String, List<ScanIssue>> entry : bySeverity.entrySet()) {
                List<ScanIssue> issues = entry.getValue();
                if (!issues.isEmpty()) {
                    String severityClass = entry.getKey().toLowerCase();
                    sb.append("            <div class=\"issue-group issue-").append(severityClass).append("\">\n");
                    sb.append("                <h3>").append(entry.getKey()).append(" (").append(issues.size()).append(")</h3>\n");
                    for (ScanIssue issue : issues) {
                        sb.append("                <div class=\"issue-item\">\n");
                        sb.append("                    <div class=\"issue-header\">\n");
                        sb.append("                    <span class=\"issue-type\">").append(escapeHtml(issue.getType())).append("</span>\n");
                        sb.append("                    <span class=\"issue-file\">").append(escapeHtml(issue.getFileName())).append("</span>\n");
                        sb.append("                    </div>\n");
                        sb.append("                    <pre class=\"issue-sql\">").append(escapeHtml(issue.getSql())).append("</pre>\n");
                        sb.append("                    <div class=\"issue-message\">💡 ").append(escapeHtml(issue.getMessage())).append("</div>\n");
                        sb.append("                </div>\n");
                    }
                    sb.append("            </div>\n");
                }
            }

            sb.append("        </div>\n");
        } else {
            sb.append("        <div class=\"success-message\">\n");
            sb.append("            <div class=\"success-icon\">✅</div>\n");
            sb.append("            <h2>No Issues Found!</h2>\n");
            sb.append("            <p>All SQL statements passed the quality checks.</p>\n");
            sb.append("        </div>\n");
        }

        if (result.getIssueSummary() != null && !result.getIssueSummary().isEmpty()) {
            sb.append("        <div class=\"summary-section\">\n");
            sb.append("            <h2>Issue Types</h2>\n");
            sb.append("            <div class=\"type-list\">\n");
            result.getIssueSummary().entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .forEach(e -> {
                        sb.append("                <div class=\"type-item\">\n");
                        sb.append("                    <span class=\"type-name\">").append(escapeHtml(e.getKey())).append("</span>\n");
                        sb.append("                    <span class=\"type-count\">").append(e.getValue()).append("</span>\n");
                        sb.append("                </div>\n");
                    });
            sb.append("            </div>\n");
            sb.append("        </div>\n");
        }

        if (result.getSqlEntries() != null && !result.getSqlEntries().isEmpty()) {
            sb.append("        <div class=\"sql-section\">\n");
            sb.append("            <h2>SQL Statements</h2>\n");

            for (ScanSqlEntry entry : result.getSqlEntries()) {
                String category = entry.getPreprocessResult() != null && entry.getPreprocessResult().getCategory() != null
                        ? entry.getPreprocessResult().getCategory().name() : "N/A";
                String validity = entry.getPreprocessResult() != null && entry.getPreprocessResult().getValidity() != null
                        ? entry.getPreprocessResult().getValidity().name() : "N/A";
                String eligibility = entry.getPreprocessResult() != null && entry.getPreprocessResult().getExplainEligibility() != null
                        ? entry.getPreprocessResult().getExplainEligibility().name() : "N/A";
                String normalized = entry.getPreprocessResult() != null && entry.getPreprocessResult().getNormalizedSql() != null
                        ? entry.getPreprocessResult().getNormalizedSql() : entry.getAbstractSql();
                String explainSql = entry.getPreprocessResult() != null ? entry.getPreprocessResult().getExplainSql() : null;
                String reason = entry.getPreprocessResult() != null ? entry.getPreprocessResult().getErrorReason() : null;

                sb.append("            <div class=\"sql-card\">\n");
                sb.append("                <div class=\"sql-meta\">分类: ").append(escapeHtml(category))
                        .append(" | 合法性: ").append(escapeHtml(validity))
                        .append(" | Explain: ").append(escapeHtml(eligibility)).append("</div>\n");
                if (reason != null && !reason.isBlank()) {
                    sb.append("                <div class=\"sql-reason\">").append(escapeHtml(reason)).append("</div>\n");
                }
                sb.append("                <div class=\"sql-code\">").append(escapeHtml(entry.getAbstractSql())).append("</div>\n");
                if (entry.getOriginalSql() != null && !entry.getOriginalSql().equals(entry.getAbstractSql())) {
                    sb.append("                <details class=\"sql-details\"><summary>查看原始 SQL</summary>\n");
                    sb.append("                    <pre class=\"sql-code\">").append(escapeHtml(entry.getOriginalSql())).append("</pre>\n");
                    sb.append("                </details>\n");
                }
                if (normalized != null && !normalized.equals(entry.getAbstractSql())) {
                    sb.append("                <details class=\"sql-details\"><summary>查看规范化 SQL</summary>\n");
                    sb.append("                    <pre class=\"sql-code\">").append(escapeHtml(normalized)).append("</pre>\n");
                    sb.append("                </details>\n");
                }
                if (explainSql != null && !explainSql.isBlank()) {
                    sb.append("                <details class=\"sql-details\"><summary>查看 Explain SQL</summary>\n");
                    sb.append("                    <pre class=\"sql-code\">").append(escapeHtml(explainSql)).append("</pre>\n");
                    sb.append("                </details>\n");
                }
                if (entry.getExplainAnalysis() != null
                        && entry.getExplainAnalysis().getPlan() != null
                        && entry.getExplainAnalysis().getPlan().getNodes() != null
                        && !entry.getExplainAnalysis().getPlan().getNodes().isEmpty()) {
                    sb.append("                <div class=\"explain-plan\">\n");
                    sb.append("                    <div class=\"sql-meta\">执行计划节点:</div>\n");
                    entry.getExplainAnalysis().getPlan().getNodes().forEach(node -> {
                        sb.append("                    <div class=\"explain-node\">\n");
                        sb.append("                        <span>").append(escapeHtml(String.valueOf(node.getSelectType()))).append("</span>");
                        sb.append(" | <span>").append(escapeHtml(String.valueOf(node.getType()))).append("</span>");
                        sb.append(" | <span>").append(escapeHtml(String.valueOf(node.getTable()))).append("</span>\n");
                        if (node.getKey() != null && !node.getKey().isBlank()) {
                            sb.append("                        <div>索引: ").append(escapeHtml(node.getKey())).append("</div>\n");
                        }
                        if (node.getRows() != null) {
                            sb.append("                        <div>扫描行数: ").append(node.getRows()).append("</div>\n");
                        }
                        if (node.getExtra() != null && !node.getExtra().isBlank()) {
                            sb.append("                        <div>").append(escapeHtml(node.getExtra())).append("</div>\n");
                        }
                        sb.append("                    </div>\n");
                    });
                    sb.append("                </div>\n");
                }
                sb.append("            </div>\n");
            }

            sb.append("        </div>\n");
        }

        sb.append("        <footer class=\"footer\">\n");
        sb.append("            <p>Generated by SQL Checker v1.0.0</p>\n");
        sb.append("            <p>Scan Duration: ").append(stats.getDurationMs()).append("ms</p>\n");
        sb.append("        </footer>\n");
        sb.append("    </div>\n");
        sb.append("</body>\n");
        sb.append("</html>\n");

        return sb.toString();
    }

    private String getCssStyles() {
        return """
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                min-height: 100vh;
                padding: 20px;
            }
            .container {
                max-width: 1200px;
                margin: 0 auto;
            }
            .header {
                background: white;
                border-radius: 16px;
                padding: 30px;
                margin-bottom: 24px;
                box-shadow: 0 4px 20px rgba(0,0,0,0.1);
                text-align: center;
            }
            .header h1 {
                font-size: 28px;
                color: #333;
                margin-bottom: 8px;
            }
            .subtitle {
                color: #666;
                font-size: 14px;
            }
            .stats-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                gap: 16px;
                margin-bottom: 24px;
            }
            .stat-card {
                background: white;
                border-radius: 12px;
                padding: 20px;
                text-align: center;
                box-shadow: 0 4px 20px rgba(0,0,0,0.1);
            }
            .stat-card.stat-critical {
                border-top: 4px solid #ef4444;
            }
            .stat-card.stat-warning {
                border-top: 4px solid #f59e0b;
            }
            .stat-card.stat-info {
                border-top: 4px solid #3b82f6;
            }
            .stat-value {
                font-size: 28px;
                font-weight: 700;
                color: #333;
            }
            .stat-label {
                color: #666;
                font-size: 14px;
                margin-top: 4px;
            }
            .stat-detail {
                color: #999;
                font-size: 12px;
                margin-top: 6px;
            }
            .issues-section {
                background: white;
                border-radius: 16px;
                padding: 24px;
                margin-bottom: 24px;
                box-shadow: 0 4px 20px rgba(0,0,0,0.1);
            }
            .issue-group {
                margin-top: 16px;
            }
            .issue-item {
                background: #f8f9fa;
                padding: 16px;
                border-radius: 12px;
                margin-top: 12px;
            }
            .issue-header {
                display: flex;
                justify-content: space-between;
                margin-bottom: 8px;
            }
            .issue-type {
                font-weight: 600;
                color: #333;
            }
            .issue-file {
                color: #666;
                font-size: 13px;
                font-family: 'Monaco', 'Menlo', monospace;
            }
            .issue-sql {
                background: #1e1e1e;
                color: #d4d4d4;
                padding: 12px;
                border-radius: 6px;
                font-size: 13px;
                overflow-x: auto;
                margin-bottom: 8px;
            }
            .issue-message {
                color: #666;
                font-size: 14px;
            }
            .sql-section {
                background: white;
                border-radius: 16px;
                padding: 24px;
                margin-bottom: 24px;
                box-shadow: 0 4px 20px rgba(0,0,0,0.1);
            }
            .sql-card {
                border: 1px solid #e0e0e0;
                border-radius: 12px;
                padding: 16px;
                margin-bottom: 16px;
                background: #fafafa;
            }
            .sql-meta {
                font-size: 12px;
                color: #666;
                margin-bottom: 8px;
            }
            .sql-reason {
                font-size: 12px;
                color: #d97706;
                margin-bottom: 8px;
            }
            .sql-code {
                background: #1f2937;
                color: #f3f4f6;
                padding: 12px;
                border-radius: 8px;
                font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
                font-size: 12px;
                white-space: pre-wrap;
                word-break: break-all;
            }
            .sql-details summary {
                cursor: pointer;
                color: #4f46e5;
                font-size: 12px;
                margin-top: 8px;
            }
            .explain-plan {
                margin-top: 8px;
            }
            .explain-node {
                background: #ffffff;
                border: 1px dashed #e0e0e0;
                border-radius: 8px;
                padding: 8px 10px;
                margin-top: 6px;
                font-size: 12px;
                color: #333;
            }
            .type-list {
                display: flex;
                flex-direction: column;
                gap: 8px;
            }
            .type-item {
                display: flex;
                justify-content: space-between;
                padding: 12px 16px;
                background: #f8f9fa;
                border-radius: 8px;
            }
            .type-name {
                color: #333;
                font-weight: 500;
            }
            .type-count {
                background: #667eea;
                color: white;
                padding: 2px 10px;
                border-radius: 12px;
                font-size: 14px;
            }
            .success-message {
                background: white;
                border-radius: 16px;
                padding: 60px 20px;
                text-align: center;
                box-shadow: 0 4px 20px rgba(0,0,0,0.1);
            }
            .success-icon {
                font-size: 64px;
                margin-bottom: 16px;
            }
            .success-message h2 {
                color: #10b981;
                margin-bottom: 8px;
            }
            .success-message p {
                color: #666;
            }
            .footer {
                text-align: center;
                color: white;
                opacity: 0.9;
                font-size: 14px;
            }
            """;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
