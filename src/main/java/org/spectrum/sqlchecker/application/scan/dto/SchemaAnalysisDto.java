package org.spectrum.sqlchecker.application.scan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Static DDL-to-SQL association summary for report-level schema risk analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaAnalysisDto {

    private String schemaPath;
    private boolean ddlDetected;
    private int ddlFileCount;
    private int tableCount;
    private int referencedTableCount;
    private int coveredTableCount;
    private int missingDdlTableCount;
    private int unindexedPredicateCount;
    private List<TableSummary> tables;
    private List<SqlSchemaRisk> risks;
    private List<String> warnings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableSummary {
        private String tableName;
        private String sourceFile;
        private List<String> columns;
        private List<String> primaryKeyColumns;
        private List<String> indexedColumns;
        private int referencedSqlCount;
        private String coverage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SqlSchemaRisk {
        private String sqlId;
        private String riskType;
        private String severity;
        private String tableName;
        private List<String> predicateColumns;
        private List<String> indexedPredicateColumns;
        private List<String> missingIndexColumns;
        private List<String> locations;
        private String evidence;
        private String recommendation;
    }
}
