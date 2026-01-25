package org.spectrum.sqlchecker.application.scan.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 扫描统计信息
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
public class ScanStatistics {

    private int totalFiles;
    private int javaFiles;
    private int xmlFiles;
    private int sqlFiles;
    private int filesScanned;
    private int sqlFound;
    private int sqlParsed;
    private long durationMs;
    private int criticalIssues;
    private int warningIssues;
    private int infoIssues;
}
