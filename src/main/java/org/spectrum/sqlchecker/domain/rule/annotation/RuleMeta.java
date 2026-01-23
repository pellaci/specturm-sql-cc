package org.spectrum.sqlchecker.domain.rule.annotation;

import org.spectrum.sqlchecker.domain.shared.enumeration.RuleCategory;
import org.spectrum.sqlchecker.domain.shared.enumeration.RuleType;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 规则元数据注解
 * <p>
 * 用于标记 SQL 规则类的元信息，包括规则 ID、名称、描述、严重级别等
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RuleMeta {

    /**
     * 规则唯一标识
     * <p>
     * 使用 kebab-case 格式，如 "select-star"、"missing-where"
     *
     * @return 规则 ID
     */
    String id();

    /**
     * 规则名称
     * <p>
     * 简洁的规则名称，用于展示
     *
     * @return 规则名称
     */
    String name();

    /**
     * 规则描述
     * <p>
     * 详细说明该规则检测的问题以及原因
     *
     * @return 规则描述
     */
    String description();

    /**
     * 规则类型
     * <p>
     * 默认为问题类规则
     *
     * @return 规则类型
     */
    RuleType type() default RuleType.PROBLEM;

    /**
     * 严重级别
     * <p>
     * 默认为重要级别
     *
     * @return 严重级别
     */
    SeverityLevel severity() default SeverityLevel.WARNING;

    /**
     * 规则标签
     * <p>
     * 用于规则分类和筛选，如 ["performance", "index"]
     *
     * @return 规则标签数组
     */
    String[] tags() default {};

    /**
     * 规则类别
     * <p>
     * 按问题领域分类
     *
     * @return 规则类别
     */
    RuleCategory category() default RuleCategory.BEST_PRACTICE;

    /**
     * 是否已废弃
     * <p>
     * 标记规则是否已被废弃，废弃的规则默认不会执行
     *
     * @return 是否已废弃
     */
    boolean deprecated() default false;
}
