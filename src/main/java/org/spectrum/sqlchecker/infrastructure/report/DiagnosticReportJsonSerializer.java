package org.spectrum.sqlchecker.infrastructure.report;

import org.spectrum.sqlchecker.application.report.dto.DiagnosticReport;

import java.util.List;

/**
 * Dependency-free JSON serializer for the stable diagnostic report contract.
 */
public final class DiagnosticReportJsonSerializer {

    private DiagnosticReportJsonSerializer() {
    }

    public static String toJson(DiagnosticReport report) {
        DiagnosticReport safeReport = report != null ? report : DiagnosticReport.builder().build();
        StringBuilder sb = new StringBuilder(8192);
        boolean first = beginObject(sb);
        first = fieldObject(sb, first, "metadata", safeReport.getMetadata(), DiagnosticReportJsonSerializer::appendMetadata);
        first = fieldObject(sb, first, "summary", safeReport.getSummary(), DiagnosticReportJsonSerializer::appendSummary);
        first = fieldObject(sb, first, "hotspots", safeReport.getHotspots(), DiagnosticReportJsonSerializer::appendHotspots);
        first = fieldObject(sb, first, "insights", safeReport.getInsights(), DiagnosticReportJsonSerializer::appendInsights);
        first = fieldObject(sb, first, "schemaAnalysis", safeReport.getSchemaAnalysis(), DiagnosticReportJsonSerializer::appendSchemaAnalysis);
        first = fieldList(sb, first, "findings", safeReport.getFindings(), DiagnosticReportJsonSerializer::appendFinding);
        first = fieldObject(sb, first, "diagnostics", safeReport.getDiagnostics(), DiagnosticReportJsonSerializer::appendDiagnostics);
        first = fieldObject(sb, first, "executiveSummary", safeReport.getExecutiveSummary(), DiagnosticReportJsonSerializer::appendExecutiveSummary);
        first = fieldList(sb, first, "campaigns", safeReport.getCampaigns(), DiagnosticReportJsonSerializer::appendCampaign);
        first = fieldObject(sb, first, "confidence", safeReport.getConfidence(), DiagnosticReportJsonSerializer::appendConfidence);
        first = fieldObject(sb, first, "methodology", safeReport.getMethodology(), DiagnosticReportJsonSerializer::appendMethodology);
        fieldObject(sb, first, "remediation", safeReport.getRemediation(), DiagnosticReportJsonSerializer::appendRemediation);
        endObject(sb);
        return sb.toString();
    }

    private static void appendMetadata(StringBuilder sb, DiagnosticReport.Metadata metadata) {
        boolean first = beginObject(sb);
        first = fieldString(sb, first, "reportVersion", metadata.getReportVersion());
        first = fieldString(sb, first, "generatedAt", metadata.getGeneratedAt());
        first = fieldString(sb, first, "projectName", metadata.getProjectName());
        first = fieldString(sb, first, "scanPath", metadata.getScanPath());
        fieldNumber(sb, first, "durationMs", metadata.getDurationMs());
        endObject(sb);
    }

    private static void appendSummary(StringBuilder sb, DiagnosticReport.Summary summary) {
        boolean first = beginObject(sb);
        first = fieldNumber(sb, first, "score", summary.getScore());
        first = fieldString(sb, first, "riskLevel", summary.getRiskLevel());
        first = fieldObject(sb, first, "counts", summary.getCounts(), DiagnosticReportJsonSerializer::appendCounts);
        fieldObject(sb, first, "coverage", summary.getCoverage(), DiagnosticReportJsonSerializer::appendCoverage);
        endObject(sb);
    }

    private static void appendCounts(StringBuilder sb, DiagnosticReport.Counts counts) {
        boolean first = beginObject(sb);
        first = fieldNumber(sb, first, "totalFiles", counts.getTotalFiles());
        first = fieldNumber(sb, first, "totalSql", counts.getTotalSql());
        first = fieldNumber(sb, first, "uniqueSql", counts.getUniqueSql());
        first = fieldNumber(sb, first, "issueSql", counts.getIssueSql());
        first = fieldNumber(sb, first, "cleanSql", counts.getCleanSql());
        first = fieldNumber(sb, first, "totalIssues", counts.getTotalIssues());
        first = fieldNumber(sb, first, "criticalIssues", counts.getCriticalIssues());
        first = fieldNumber(sb, first, "warningIssues", counts.getWarningIssues());
        fieldNumber(sb, first, "infoIssues", counts.getInfoIssues());
        endObject(sb);
    }

    private static void appendCoverage(StringBuilder sb, DiagnosticReport.Coverage coverage) {
        boolean first = beginObject(sb);
        first = fieldNumber(sb, first, "parseRate", coverage.getParseRate());
        fieldNumber(sb, first, "explainCoverage", coverage.getExplainCoverage());
        endObject(sb);
    }

    private static void appendHotspots(StringBuilder sb, DiagnosticReport.Hotspots hotspots) {
        boolean first = beginObject(sb);
        first = fieldList(sb, first, "byFile", hotspots.getByFile(), DiagnosticReportJsonSerializer::appendStatItem);
        first = fieldList(sb, first, "byRule", hotspots.getByRule(), DiagnosticReportJsonSerializer::appendStatItem);
        fieldList(sb, first, "bySeverity", hotspots.getBySeverity(), DiagnosticReportJsonSerializer::appendStatItem);
        endObject(sb);
    }

    private static void appendStatItem(StringBuilder sb, DiagnosticReport.StatItem item) {
        boolean first = beginObject(sb);
        first = fieldString(sb, first, "label", item.getLabel());
        first = fieldNumber(sb, first, "count", item.getCount());
        first = fieldNumber(sb, first, "ratio", item.getRatio());
        fieldString(sb, first, "severity", item.getSeverity());
        endObject(sb);
    }

    private static void appendInsights(StringBuilder sb, DiagnosticReport.Insights insights) {
        boolean first = beginObject(sb);
        first = fieldList(sb, first, "duplicateSql", insights.getDuplicateSql(), DiagnosticReportJsonSerializer::appendInsightItem);
        first = fieldList(sb, first, "parseFailures", insights.getParseFailures(), DiagnosticReportJsonSerializer::appendInsightItem);
        first = fieldList(sb, first, "skippedExplain", insights.getSkippedExplain(), DiagnosticReportJsonSerializer::appendInsightItem);
        first = fieldList(sb, first, "dangerousDml", insights.getDangerousDml(), DiagnosticReportJsonSerializer::appendInsightItem);
        first = fieldList(sb, first, "potentialInjection", insights.getPotentialInjection(), DiagnosticReportJsonSerializer::appendInsightItem);
        fieldList(sb, first, "fullScanOrNoIndex", insights.getFullScanOrNoIndex(), DiagnosticReportJsonSerializer::appendInsightItem);
        endObject(sb);
    }

    private static void appendInsightItem(StringBuilder sb, DiagnosticReport.InsightItem item) {
        boolean first = beginObject(sb);
        first = fieldString(sb, first, "id", item.getId());
        first = fieldString(sb, first, "title", item.getTitle());
        first = fieldNumber(sb, first, "count", item.getCount());
        first = fieldString(sb, first, "severity", item.getSeverity());
        first = fieldString(sb, first, "evidence", item.getEvidence());
        fieldStringList(sb, first, "locations", item.getLocations());
        endObject(sb);
    }

    private static void appendSchemaAnalysis(StringBuilder sb, DiagnosticReport.SchemaAnalysis schemaAnalysis) {
        boolean first = beginObject(sb);
        first = fieldString(sb, first, "schemaPath", schemaAnalysis.getSchemaPath());
        first = fieldBoolean(sb, first, "ddlDetected", schemaAnalysis.isDdlDetected());
        first = fieldNumber(sb, first, "ddlFileCount", schemaAnalysis.getDdlFileCount());
        first = fieldNumber(sb, first, "tableCount", schemaAnalysis.getTableCount());
        first = fieldNumber(sb, first, "referencedTableCount", schemaAnalysis.getReferencedTableCount());
        first = fieldNumber(sb, first, "coveredTableCount", schemaAnalysis.getCoveredTableCount());
        first = fieldNumber(sb, first, "missingDdlTableCount", schemaAnalysis.getMissingDdlTableCount());
        first = fieldNumber(sb, first, "unindexedPredicateCount", schemaAnalysis.getUnindexedPredicateCount());
        first = fieldList(sb, first, "tables", schemaAnalysis.getTables(), DiagnosticReportJsonSerializer::appendSchemaTable);
        first = fieldList(sb, first, "risks", schemaAnalysis.getRisks(), DiagnosticReportJsonSerializer::appendSchemaRisk);
        fieldStringList(sb, first, "warnings", schemaAnalysis.getWarnings());
        endObject(sb);
    }

    private static void appendSchemaTable(StringBuilder sb, DiagnosticReport.SchemaTable table) {
        boolean first = beginObject(sb);
        first = fieldString(sb, first, "tableName", table.getTableName());
        first = fieldString(sb, first, "sourceFile", table.getSourceFile());
        first = fieldStringList(sb, first, "columns", table.getColumns());
        first = fieldStringList(sb, first, "primaryKeyColumns", table.getPrimaryKeyColumns());
        first = fieldStringList(sb, first, "indexedColumns", table.getIndexedColumns());
        first = fieldNumber(sb, first, "referencedSqlCount", table.getReferencedSqlCount());
        fieldString(sb, first, "coverage", table.getCoverage());
        endObject(sb);
    }

    private static void appendSchemaRisk(StringBuilder sb, DiagnosticReport.SchemaRisk risk) {
        boolean first = beginObject(sb);
        first = fieldString(sb, first, "sqlId", risk.getSqlId());
        first = fieldString(sb, first, "riskType", risk.getRiskType());
        first = fieldString(sb, first, "severity", risk.getSeverity());
        first = fieldString(sb, first, "tableName", risk.getTableName());
        first = fieldStringList(sb, first, "predicateColumns", risk.getPredicateColumns());
        first = fieldStringList(sb, first, "indexedPredicateColumns", risk.getIndexedPredicateColumns());
        first = fieldStringList(sb, first, "missingIndexColumns", risk.getMissingIndexColumns());
        first = fieldStringList(sb, first, "locations", risk.getLocations());
        first = fieldString(sb, first, "evidence", risk.getEvidence());
        fieldString(sb, first, "recommendation", risk.getRecommendation());
        endObject(sb);
    }

    private static void appendFinding(StringBuilder sb, DiagnosticReport.Finding finding) {
        boolean first = beginObject(sb);
        first = fieldString(sb, first, "id", finding.getId());
        first = fieldString(sb, first, "severity", finding.getSeverity());
        first = fieldString(sb, first, "category", finding.getCategory());
        first = fieldString(sb, first, "sqlType", finding.getSqlType());
        first = fieldNumber(sb, first, "score", finding.getScore());
        first = fieldList(sb, first, "locations", finding.getLocations(), DiagnosticReportJsonSerializer::appendLocation);
        first = fieldObject(sb, first, "sql", finding.getSql(), DiagnosticReportJsonSerializer::appendSqlText);
        first = fieldList(sb, first, "issues", finding.getIssues(), DiagnosticReportJsonSerializer::appendIssue);
        first = fieldStringList(sb, first, "recommendations", finding.getRecommendations());
        fieldObject(sb, first, "explain", finding.getExplain(), DiagnosticReportJsonSerializer::appendExplain);
        endObject(sb);
    }

    private static void appendLocation(StringBuilder sb, DiagnosticReport.Location location) {
        boolean first = beginObject(sb);
        first = fieldString(sb, first, "filePath", location.getFilePath());
        first = fieldString(sb, first, "fileName", location.getFileName());
        first = fieldNumber(sb, first, "startLine", location.getStartLine());
        first = fieldNumber(sb, first, "endLine", location.getEndLine());
        fieldString(sb, first, "sourceType", location.getSourceType());
        endObject(sb);
    }

    private static void appendSqlText(StringBuilder sb, DiagnosticReport.SqlText sql) {
        boolean first = beginObject(sb);
        first = fieldString(sb, first, "original", sql.getOriginal());
        first = fieldString(sb, first, "normalized", sql.getNormalized());
        first = fieldString(sb, first, "abstracted", sql.getAbstracted());
        fieldString(sb, first, "explain", sql.getExplain());
        endObject(sb);
    }

    private static void appendIssue(StringBuilder sb, DiagnosticReport.Issue issue) {
        boolean first = beginObject(sb);
        first = fieldString(sb, first, "source", issue.getSource());
        first = fieldString(sb, first, "type", issue.getType());
        first = fieldString(sb, first, "severity", issue.getSeverity());
        first = fieldString(sb, first, "message", issue.getMessage());
        first = fieldString(sb, first, "suggestion", issue.getSuggestion());
        fieldString(sb, first, "evidence", issue.getEvidence());
        endObject(sb);
    }

    private static void appendExplain(StringBuilder sb, DiagnosticReport.Explain explain) {
        boolean first = beginObject(sb);
        first = fieldString(sb, first, "eligibility", explain.getEligibility());
        first = fieldBoolean(sb, first, "executed", explain.isExecuted());
        first = fieldNumber(sb, first, "durationMs", explain.getDurationMs());
        first = fieldNumber(sb, first, "issueCount", explain.getIssueCount());
        fieldStringList(sb, first, "planSummary", explain.getPlanSummary());
        endObject(sb);
    }

    private static void appendDiagnostics(StringBuilder sb, DiagnosticReport.Diagnostics diagnostics) {
        boolean first = beginObject(sb);
        first = fieldStringList(sb, first, "parseFailures", diagnostics.getParseFailures());
        first = fieldStringList(sb, first, "skippedExplain", diagnostics.getSkippedExplain());
        first = fieldStringList(sb, first, "manualReview", diagnostics.getManualReview());
        fieldStringList(sb, first, "configWarnings", diagnostics.getConfigWarnings());
        endObject(sb);
    }

    private static void appendExecutiveSummary(StringBuilder sb, DiagnosticReport.ExecutiveSummary summary) {
        boolean first = beginObject(sb);
        first = fieldString(sb, first, "riskConclusion", summary.getRiskConclusion());
        first = fieldStringList(sb, first, "topDrivers", summary.getTopDrivers());
        first = fieldStringList(sb, first, "recommendedActions", summary.getRecommendedActions());
        fieldString(sb, first, "confidenceSummary", summary.getConfidenceSummary());
        endObject(sb);
    }

    private static void appendCampaign(StringBuilder sb, DiagnosticReport.RemediationCampaign campaign) {
        boolean first = beginObject(sb);
        first = fieldString(sb, first, "id", campaign.getId());
        first = fieldString(sb, first, "priority", campaign.getPriority());
        first = fieldString(sb, first, "theme", campaign.getTheme());
        first = fieldString(sb, first, "title", campaign.getTitle());
        first = fieldString(sb, first, "summary", campaign.getSummary());
        first = fieldObject(sb, first, "scope", campaign.getScope(), DiagnosticReportJsonSerializer::appendCampaignScope);
        first = fieldString(sb, first, "evidenceLevel", campaign.getEvidenceLevel());
        first = fieldStringList(sb, first, "recommendations", campaign.getRecommendations());
        first = fieldStringList(sb, first, "acceptanceChecklist", campaign.getAcceptanceChecklist());
        fieldStringList(sb, first, "findingIds", campaign.getFindingIds());
        endObject(sb);
    }

    private static void appendCampaignScope(StringBuilder sb, DiagnosticReport.CampaignScope scope) {
        boolean first = beginObject(sb);
        first = fieldNumber(sb, first, "sqlCount", scope.getSqlCount());
        first = fieldNumber(sb, first, "fileCount", scope.getFileCount());
        first = fieldStringList(sb, first, "files", scope.getFiles());
        fieldStringList(sb, first, "examples", scope.getExamples());
        endObject(sb);
    }

    private static void appendConfidence(StringBuilder sb, DiagnosticReport.Confidence confidence) {
        boolean first = beginObject(sb);
        first = fieldString(sb, first, "level", confidence.getLevel());
        first = fieldStringList(sb, first, "evidenceSources", confidence.getEvidenceSources());
        fieldStringList(sb, first, "limitations", confidence.getLimitations());
        endObject(sb);
    }

    private static void appendMethodology(StringBuilder sb, DiagnosticReport.Methodology methodology) {
        boolean first = beginObject(sb);
        first = fieldStringList(sb, first, "scoring", methodology.getScoring());
        first = fieldStringList(sb, first, "severityDefinitions", methodology.getSeverityDefinitions());
        first = fieldStringList(sb, first, "coverageDefinitions", methodology.getCoverageDefinitions());
        fieldStringList(sb, first, "knownLimits", methodology.getKnownLimits());
        endObject(sb);
    }

    private static void appendRemediation(StringBuilder sb, DiagnosticReport.Remediation remediation) {
        boolean first = beginObject(sb);
        first = fieldObject(sb, first, "summary", remediation.getSummary(), DiagnosticReportJsonSerializer::appendRemediationSummary);
        first = fieldList(sb, first, "campaigns", remediation.getCampaigns(), DiagnosticReportJsonSerializer::appendCampaign);
        first = fieldList(sb, first, "tasks", remediation.getTasks(), DiagnosticReportJsonSerializer::appendRemediationTask);
        fieldList(sb, first, "recipes", remediation.getRecipes(), DiagnosticReportJsonSerializer::appendRepairRecipe);
        endObject(sb);
    }

    private static void appendRemediationSummary(StringBuilder sb, DiagnosticReport.RemediationSummary summary) {
        boolean first = beginObject(sb);
        first = fieldNumber(sb, first, "campaignCount", summary.getCampaignCount());
        first = fieldNumber(sb, first, "taskCount", summary.getTaskCount());
        first = fieldNumber(sb, first, "confirmedTaskCount", summary.getConfirmedTaskCount());
        first = fieldNumber(sb, first, "likelyTaskCount", summary.getLikelyTaskCount());
        first = fieldNumber(sb, first, "reviewTaskCount", summary.getReviewTaskCount());
        fieldString(sb, first, "estimatedFirstPassFocus", summary.getEstimatedFirstPassFocus());
        endObject(sb);
    }

    private static void appendRemediationTask(StringBuilder sb, DiagnosticReport.RemediationTask task) {
        boolean first = beginObject(sb);
        first = fieldString(sb, first, "id", task.getId());
        first = fieldString(sb, first, "title", task.getTitle());
        first = fieldString(sb, first, "priority", task.getPriority());
        first = fieldString(sb, first, "severity", task.getSeverity());
        first = fieldString(sb, first, "theme", task.getTheme());
        first = fieldString(sb, first, "confidence", task.getConfidence());
        first = fieldObject(sb, first, "location", task.getLocation(), DiagnosticReportJsonSerializer::appendLocation);
        first = fieldString(sb, first, "campaignId", task.getCampaignId());
        first = fieldStringList(sb, first, "ruleTypes", task.getRuleTypes());
        first = fieldString(sb, first, "impact", task.getImpact());
        first = fieldString(sb, first, "repairRecipeId", task.getRepairRecipeId());
        first = fieldString(sb, first, "recommendation", task.getRecommendation());
        first = fieldString(sb, first, "acceptanceCheck", task.getAcceptanceCheck());
        first = fieldString(sb, first, "evidence", task.getEvidence());
        fieldObject(sb, first, "sql", task.getSql(), DiagnosticReportJsonSerializer::appendSqlText);
        endObject(sb);
    }

    private static void appendRepairRecipe(StringBuilder sb, DiagnosticReport.RepairRecipe recipe) {
        boolean first = beginObject(sb);
        first = fieldString(sb, first, "id", recipe.getId());
        first = fieldString(sb, first, "title", recipe.getTitle());
        first = fieldStringList(sb, first, "appliesToRules", recipe.getAppliesToRules());
        first = fieldString(sb, first, "unsafePattern", recipe.getUnsafePattern());
        first = fieldString(sb, first, "safePattern", recipe.getSafePattern());
        first = fieldStringList(sb, first, "steps", recipe.getSteps());
        first = fieldString(sb, first, "verification", recipe.getVerification());
        fieldStringList(sb, first, "knownLimits", recipe.getKnownLimits());
        endObject(sb);
    }

    private static <T> boolean fieldObject(StringBuilder sb, boolean first, String name, T value, ObjectAppender<T> appender) {
        first = fieldPrefix(sb, first, name);
        if (value == null) {
            sb.append("null");
        } else {
            appender.append(sb, value);
        }
        return first;
    }

    private static <T> boolean fieldList(StringBuilder sb, boolean first, String name, List<T> values, ObjectAppender<T> appender) {
        first = fieldPrefix(sb, first, name);
        appendList(sb, values, appender);
        return first;
    }

    private static boolean fieldStringList(StringBuilder sb, boolean first, String name, List<String> values) {
        first = fieldPrefix(sb, first, name);
        appendList(sb, values, DiagnosticReportJsonSerializer::appendStringValue);
        return first;
    }

    private static boolean fieldString(StringBuilder sb, boolean first, String name, String value) {
        first = fieldPrefix(sb, first, name);
        appendStringValue(sb, value);
        return first;
    }

    private static boolean fieldBoolean(StringBuilder sb, boolean first, String name, boolean value) {
        first = fieldPrefix(sb, first, name);
        sb.append(value);
        return first;
    }

    private static boolean fieldNumber(StringBuilder sb, boolean first, String name, long value) {
        first = fieldPrefix(sb, first, name);
        sb.append(value);
        return first;
    }

    private static boolean fieldNumber(StringBuilder sb, boolean first, String name, double value) {
        first = fieldPrefix(sb, first, name);
        if (Double.isFinite(value)) {
            sb.append(value);
        } else {
            sb.append('0');
        }
        return first;
    }

    private static boolean fieldPrefix(StringBuilder sb, boolean first, String name) {
        if (!first) {
            sb.append(',');
        }
        appendStringValue(sb, name);
        sb.append(':');
        return false;
    }

    private static boolean beginObject(StringBuilder sb) {
        sb.append('{');
        return true;
    }

    private static void endObject(StringBuilder sb) {
        sb.append('}');
    }

    private static <T> void appendList(StringBuilder sb, List<T> values, ObjectAppender<T> appender) {
        sb.append('[');
        if (values != null) {
            boolean first = true;
            for (T value : values) {
                if (!first) {
                    sb.append(',');
                }
                if (value == null) {
                    sb.append("null");
                } else {
                    appender.append(sb, value);
                }
                first = false;
            }
        }
        sb.append(']');
    }

    private static void appendStringValue(StringBuilder sb, String value) {
        if (value == null) {
            sb.append("null");
            return;
        }
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        sb.append('"');
    }

    @FunctionalInterface
    private interface ObjectAppender<T> {
        void append(StringBuilder sb, T value);
    }
}
