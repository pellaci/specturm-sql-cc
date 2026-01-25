package org.spectrum.sqlchecker.application.scan.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 扫描开始信息
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
public class ScanStartInfo {

    private String path;
    private int totalFiles;
    private int javaFiles;
    private int xmlFiles;
    private int sqlFiles;
}
