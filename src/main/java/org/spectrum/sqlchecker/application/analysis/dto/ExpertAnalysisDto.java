package org.spectrum.sqlchecker.application.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;

import java.util.List;

/**
 * 专家分析结果
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpertAnalysisDto {

    /**
     * SQL ID
     */
    private String sqlId;

    /**
     * 优化建议列表
     */
    private List<Recommendation> recommendations;

    /**
     * 总体评分（0-100）
     */
    @Builder.Default
    private int score = 100;

    /**
     * 严重等级
     */
    private SeverityLevel severity;

    /**
     * 分析耗时（毫秒）
     */
    private long durationMs;

    /**
     * 是否有建议
     */
    public boolean hasRecommendations() {
        return recommendations != null && !recommendations.isEmpty();
    }

    /**
     * 获取高优先级建议数量
     */
    public long getHighPriorityCount() {
        return recommendations == null ? 0 : recommendations.stream()
            .filter(r -> r.getPriority() >= 4)
            .count();
    }
}
