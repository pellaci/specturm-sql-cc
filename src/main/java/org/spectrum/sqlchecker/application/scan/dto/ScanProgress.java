package org.spectrum.sqlchecker.application.scan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.spectrum.sqlchecker.domain.shared.enumeration.ScanStatus;
import org.spectrum.sqlchecker.domain.shared.enumeration.ScanStage;

import java.time.Instant;

/**
 * 扫描进度
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanProgress {

    /**
     * 扫描 ID
     */
    private String scanId;

    /**
     * 扫描状态
     */
    private ScanStatus status;

    /**
     * 当前进度阶段
     */
    private ScanStage stage;

    /**
     * 进度百分比 (0-100)
     */
    @Builder.Default
    private int progress = 0;

    /**
     * 已扫描文件数
     */
    @Builder.Default
    private int filesScanned = 0;

    /**
     * 总文件数
     */
    @Builder.Default
    private int totalFiles = 0;

    /**
     * 已发现 SQL 数量
     */
    @Builder.Default
    private int sqlFound = 0;

    /**
     * 已分析 SQL 数量
     */
    @Builder.Default
    private int sqlAnalyzed = 0;

    /**
     * 预计剩余时间（秒）
     */
    @Builder.Default
    private long estimatedSecondsRemaining = 0;

    /**
     * 当前处理文件
     */
    private String currentFile;

    /**
     * 开始时间
     */
    private Instant startTime;

    /**
     * 更新进度
     */
    public void incrementProgress() {
        if (progress < 100) {
            this.progress = Math.min(100, progress + 1);
        }
    }

    /**
     * 增加已扫描文件数
     */
    public void incrementFilesScanned() {
        this.filesScanned++;
    }

    /**
     * 增加已发现 SQL 数量
     */
    public void incrementSqlFound() {
        this.sqlFound++;
    }

    /**
     * 增加已分析 SQL 数量
     */
    public void incrementSqlAnalyzed() {
        this.sqlAnalyzed++;
    }

    /**
     * 计算进度百分比
     */
    public void calculateProgress() {
        if (totalFiles > 0) {
            this.progress = (int) ((double) filesScanned / totalFiles * 100);
        }
    }

    /**
     * 计算预计剩余时间
     */
    public void calculateEstimatedRemaining() {
        if (startTime != null && filesScanned > 0) {
            long elapsed = Instant.now().toEpochMilli() - startTime.toEpochMilli();
            long avgPerFile = elapsed / filesScanned;
            long remaining = (totalFiles - filesScanned) * avgPerFile;
            this.estimatedSecondsRemaining = remaining / 1000;
        }
    }
}
