package org.spectrum.sqlchecker.domain.shared.enumeration;

/**
 * 扫描阶段
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public enum ScanStage {

    /**
     * 初始化
     */
    INITIALIZING,

    /**
     * 文件扫描
     */
    FILE_SCANNING,

    /**
     * SQL 提取
     */
    SQL_EXTRACTION,

    /**
     * SQL 去重
     */
    SQL_DEDUPLICATION,

    /**
     * 静态分析
     */
    STATIC_ANALYSIS,

    /**
     * EXPLAIN 分析
     */
    EXPLAIN_ANALYSIS,

    /**
     * 专家规则分析
     */
    EXPERT_ANALYSIS,

    /**
     * 报告生成
     */
    REPORT_GENERATING
}
