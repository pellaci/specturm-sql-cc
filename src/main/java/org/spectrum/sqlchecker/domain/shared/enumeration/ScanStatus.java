package org.spectrum.sqlchecker.domain.shared.enumeration;

/**
 * 扫描状态
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public enum ScanStatus {

    /**
     * 待执行
     */
    PENDING,

    /**
     * 扫描中
     */
    SCANNING,

    /**
     * 分析中
     */
    ANALYZING,

    /**
     * 已完成
     */
    COMPLETED,

    /**
     * 已取消
     */
    CANCELLED,

    /**
     * 失败
     */
    FAILED;

    /**
     * 是否正在进行中
     *
     * @return 是否正在进行中
     */
    public boolean isInProgress() {
        return this == SCANNING || this == ANALYZING;
    }

    /**
     * 是否已完成（包括成功和失败）
     *
     * @return 是否已完成
     */
    public boolean isCompleted() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}
