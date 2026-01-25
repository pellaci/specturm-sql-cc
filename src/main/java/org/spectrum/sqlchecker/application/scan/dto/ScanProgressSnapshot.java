package org.spectrum.sqlchecker.application.scan.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 扫描进度快照
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
public class ScanProgressSnapshot {

    private int progress;
    private String stage;
    private int filesScanned;
    private int totalFiles;
    private int sqlFound;
    private String currentFile;
}
