package org.spectrum.sqlchecker.application.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 优化建议
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {

    /**
     * 建议类型
     */
    private String type;

    /**
     * 建议标题
     */
    private String title;

    /**
     * 详细描述
     */
    private String description;

    /**
     * 优化建议
     */
    private String suggestion;

    /**
     * 预期收益
     */
    private String expectedBenefit;

    /**
     * 参考链接
     */
    private String referenceUrl;

    /**
     * 优先级（1-5，5最高）
     */
    @Builder.Default
    private int priority = 3;
}
