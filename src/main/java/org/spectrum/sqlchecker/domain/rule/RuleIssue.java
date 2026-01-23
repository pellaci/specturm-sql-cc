package org.spectrum.sqlchecker.domain.rule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;

/**
 * 规则问题
 * <p>
 * 表示规则检测到的问题，包含问题描述、位置、建议修复方案等信息
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleIssue {

    /**
     * 规则 ID
     */
    private String ruleId;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 严重级别
     */
    private SeverityLevel severity;

    /**
     * 问题描述
     */
    private String message;

    /**
     * 建议修复方案
     */
    private String suggestion;

    /**
     * 问题位置
     */
    private RuleLocation location;

    /**
     * 创建规则问题的便捷方法
     *
     * @param ruleId     规则 ID
     * @param severity   严重级别
     * @param message    问题描述
     * @param suggestion 建议修复方案
     * @return 规则问题
     */
    public static RuleIssue of(String ruleId, SeverityLevel severity, String message, String suggestion) {
        return RuleIssue.builder()
                .ruleId(ruleId)
                .severity(severity)
                .message(message)
                .suggestion(suggestion)
                .build();
    }
}
