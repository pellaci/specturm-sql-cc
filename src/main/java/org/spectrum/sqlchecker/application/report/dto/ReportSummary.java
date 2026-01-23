package org.spectrum.sqlchecker.application.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

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
     * 平均得分
     */
    private double averageScore;

    /**
     * 生成时间
     */
    private Instant generatedAt;
}
