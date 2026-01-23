package org.spectrum.sqlchecker.application.scan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlSourceType;

/**
 * SQL 位置 DTO
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlLocationDto {

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 起始行号
     */
    private int startLine;

    /**
     * 结束行号
     */
    private int endLine;

    /**
     * 起始列号
     */
    private int startColumn;

    /**
     * 结束列号
     */
    private int endColumn;

    /**
     * 类名
     */
    private String className;

    /**
     * 方法名
     */
    private String methodName;

    /**
     * 来源类型
     */
    private SqlSourceType sourceType;

    /**
     * 获取位置描述字符串
     */
    public String getLocationString() {
        return String.format("%s:%d:%d", filePath, startLine, startColumn);
    }

    /**
     * 获取短位置描述
     */
    public String getShortLocationString() {
        return String.format("%s:%d", fileName, startLine);
    }
}
