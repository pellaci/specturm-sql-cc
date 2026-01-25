package org.spectrum.sqlchecker.application.scan.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 扫描发现的问题
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
public class ScanIssue {

    private String fileName;
    private String sql;
    private String type;
    private String severity;
    private String message;
}
