package org.spectrum.sqlchecker.application.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Stable report view model shared by HTML and JSON report outputs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosticReport {

    private Metadata metadata;
    private Summary summary;
    private Hotspots hotspots;
    private Insights insights;
    private List<Finding> findings;
    private Diagnostics diagnostics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {
        private String reportVersion;
        private String generatedAt;
        private String projectName;
        private String scanPath;
        private long durationMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private int score;
        private String riskLevel;
        private Counts counts;
        private Coverage coverage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Counts {
        private int totalFiles;
        private int totalSql;
        private int uniqueSql;
        private int totalIssues;
        private int criticalIssues;
        private int warningIssues;
        private int infoIssues;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Coverage {
        private double parseRate;
        private double explainCoverage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Hotspots {
        private List<StatItem> byFile;
        private List<StatItem> byRule;
        private List<StatItem> bySeverity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Insights {
        private List<InsightItem> duplicateSql;
        private List<InsightItem> parseFailures;
        private List<InsightItem> skippedExplain;
        private List<InsightItem> dangerousDml;
        private List<InsightItem> potentialInjection;
        private List<InsightItem> fullScanOrNoIndex;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsightItem {
        private String id;
        private String title;
        private int count;
        private String severity;
        private String evidence;
        private List<String> locations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatItem {
        private String label;
        private int count;
        private double ratio;
        private String severity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Finding {
        private String id;
        private String severity;
        private String category;
        private String sqlType;
        private int score;
        private List<Location> locations;
        private SqlText sql;
        private List<Issue> issues;
        private List<String> recommendations;
        private Explain explain;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location {
        private String filePath;
        private String fileName;
        private int startLine;
        private int endLine;
        private String sourceType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SqlText {
        private String original;
        private String normalized;
        private String abstracted;
        private String explain;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Issue {
        private String source;
        private String type;
        private String severity;
        private String message;
        private String suggestion;
        private String evidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Explain {
        private String eligibility;
        private boolean executed;
        private long durationMs;
        private int issueCount;
        private List<String> planSummary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Diagnostics {
        private List<String> parseFailures;
        private List<String> skippedExplain;
        private List<String> manualReview;
        private List<String> configWarnings;
    }
}
