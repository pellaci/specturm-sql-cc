package org.spectrum.sqlchecker.application.scan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.spectrum.sqlchecker.domain.shared.enumeration.ScanStatus;

import java.time.Instant;
import java.util.List;

/**
 * 扫描结果
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanResult {

    /**
     * 扫描 ID
     */
    private String scanId;

    /**
     * 扫描状态
     */
    private ScanStatus status;

    /**
     * 扫描的文件数量
     */
    private int filesScanned;

    /**
     * 扫描路径
     */
    private String scanPath;

    /**
     * 扫描的文件总数
     */
    private int totalFiles;

    /**
     * Java 文件数量
     */
    private int javaFiles;

    /**
     * XML 文件数量
     */
    private int xmlFiles;

    /**
     * SQL 文件数量
     */
    private int sqlFiles;

    /**
     * 发现的 SQL 数量
     */
    private int sqlFound;

    /**
     * 去重后的 SQL 数量
     */
    private int uniqueSqlFound;

    /**
     * 扫描耗时（毫秒）
     */
    private long durationMs;

    /**
     * 提取的 SQL 语句列表
     */
    private List<SqlStatementDto> sqlStatements;

    /**
     * 错误信息列表
     */
    private List<ScanError> errors;

    /**
     * 开始时间
     */
    private Instant startTime;

    /**
     * 结束时间
     */
    private Instant endTime;

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return status == ScanStatus.COMPLETED;
    }

    /**
     * 是否失败
     */
    public boolean isFailed() {
        return status == ScanStatus.FAILED;
    }
}
