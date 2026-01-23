package org.spectrum.sqlchecker.application.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;

import java.util.List;

/**
 * 静态分析结果
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaticAnalysisDto {

    /**
     * SQL ID
     */
    private String sqlId;

    /**
     * 严重等级
     */
    private SeverityLevel severity;

    /**
     * 问题列表
     */
    private List<StaticIssue> issues;

    /**
     * 得分（0-100）
     */
    @Builder.Default
    private int score = 100;

    /**
     * 分析耗时（毫秒）
     */
    private long durationMs;

    /**
     * 是否有问题
     */
    public boolean hasIssues() {
        return issues != null && !issues.isEmpty();
    }

    /**
     * 获取严重问题数量
     */
    public long getCriticalCount() {
        return issues == null ? 0 : issues.stream()
            .filter(i -> i.getSeverity() == SeverityLevel.CRITICAL)
            .count();
    }

    /**
     * 获取警告问题数量
     */
    public long getWarningCount() {
        return issues == null ? 0 : issues.stream()
            .filter(i -> i.getSeverity() == SeverityLevel.WARNING)
            .count();
    }

    /**
     * 获取提示问题数量
     */
    public long getInfoCount() {
        return issues == null ? 0 : issues.stream()
            .filter(i -> i.getSeverity() == SeverityLevel.INFO)
            .count();
    }
}
