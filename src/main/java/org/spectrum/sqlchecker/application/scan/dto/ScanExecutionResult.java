package org.spectrum.sqlchecker.application.scan.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 扫描执行结果
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
public class ScanExecutionResult {

    private ScanResult scanResult;
    private ScanStatistics statistics;
    private List<ScanIssue> issues;
    private Map<String, Integer> issueSummary;
    private List<ScanSqlEntry> sqlEntries;
    private String scanPath;
}
