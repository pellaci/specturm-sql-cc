package org.spectrum.sqlchecker.application.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 报告摘要
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummary {

    /**
     * SQL 总数
     */
    private int totalSql;

    /**
     * 解析成功的 SQL 数量
     */
    private int parsedSql;

    /**
     * 问题总数
     */
    private int totalIssues;

    /**
     * 严重问题数
     */
    private int criticalIssues;

    /**
     * 警告问题数
     */
    private int warningIssues;

    /**
     * 提示问题数
     */
    private int infoIssues;

    /**
     * 扫描耗时（毫秒）
     */
    private long durationMs;

    /**
     * 解析覆盖率（0-100）
     */
    private double parseRate;

    /**
     * 可执行 EXPLAIN 的 SQL 数量
     */
    private int explainEligible;

    /**
     * 实际执行 EXPLAIN 的 SQL 数量
     */
    private int explainExecuted;

    /**
     * EXPLAIN 覆盖率（0-100）
     */
    private double explainCoverage;

    /**
     * 平均得分
     */
    private double averageScore;

    /**
     * SQL 分类统计
     */
    private List<ReportStatItem> categoryStats;

    /**
     * 预处理合法性统计
     */
    private List<ReportStatItem> validityStats;

    /**
     * EXPLAIN 可执行性统计
     */
    private List<ReportStatItem> explainEligibilityStats;

    /**
     * SQL 来源统计
     */
    private List<ReportStatItem> sourceTypeStats;

    /**
     * 静态分析问题类型统计
     */
    private List<ReportStatItem> staticIssueStats;

    /**
     * EXPLAIN 问题类型统计
     */
    private List<ReportStatItem> explainIssueStats;

    /**
     * 生成时间
     */
    private Instant generatedAt;
}
