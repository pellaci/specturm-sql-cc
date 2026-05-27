package org.spectrum.sqlchecker.application.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;

import java.util.List;

/**
 * EXPLAIN 分析结果
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplainAnalysisDto {

    /**
     * SQL ID
     */
    private String sqlId;

    /**
     * 执行计划
     */
    private ExplainPlan plan;

    /**
     * 检测到的问题列表
     */
    private List<ExplainIssue> issues;

    /**
     * 严重等级
     */
    private SeverityLevel severity;

    /**
     * 分析耗时（毫秒）
     */
    private long durationMs;

    /**
     * EXPLAIN 未能完成时的诊断信息。该字段用于报告诊断，不计入 SQL 风险问题。
     */
    private String errorMessage;

    /**
     * 是否有问题
     */
    public boolean hasIssues() {
        return issues != null && !issues.isEmpty();
    }
}
