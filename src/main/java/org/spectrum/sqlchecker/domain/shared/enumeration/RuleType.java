package org.spectrum.sqlchecker.domain.shared.enumeration;

/**
 * 规则类型
 * <p>
 * 定义规则的检测类型，用于区分不同性质的规则
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
public enum RuleType {

    /**
     * 问题类规则
     * <p>
     * 检测明确的代码问题，通常必须修复
     */
    PROBLEM,

    /**
     * 建议类规则
     * <p>
     * 检测可以优化的代码，属于可选改进
     */
    SUGGESTION
}
