package org.spectrum.sqlchecker.application.scan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 扫描错误
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanError {

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 错误类型
     */
    private String errorType;

    /**
     * 堆栈信息（可选）
     */
    private String stackTrace;
}
