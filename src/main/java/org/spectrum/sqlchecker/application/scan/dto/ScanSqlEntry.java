package org.spectrum.sqlchecker.application.scan.dto;

import lombok.Builder;
import lombok.Data;
import org.spectrum.sqlchecker.application.analysis.dto.ExplainAnalysisDto;
import org.spectrum.sqlchecker.application.preprocess.dto.PreprocessResult;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL 语句扫描条目
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
public class ScanSqlEntry {

    private String id;
    private String abstractSql;
    private String originalSql;

    @Builder.Default
    private List<SqlLocationDto> locations = new ArrayList<>();

    @Builder.Default
    private List<ScanIssue> issues = new ArrayList<>();

    @Builder.Default
    private boolean analyzed = false;

    private PreprocessResult preprocessResult;
    private ExplainAnalysisDto explainAnalysis;
}
