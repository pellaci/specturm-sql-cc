package org.spectrum.sqlchecker.domain.shared.enumeration;

/**
 * 规则类别
 * <p>
 * 按问题领域对规则进行分类
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
public enum RuleCategory {

    /**
     * 性能问题
     * <p>
     * 影响查询性能的问题，如索引缺失、全表扫描等
     */
    PERFORMANCE,

    /**
     * 安全问题
     * <p>
     * 安全相关的风险，如 SQL 注入、敏感数据暴露等
     */
    SECURITY,

    /**
     * 可维护性
     * <p>
     * 影响代码可维护性的问题，如复杂度过高、命名不规范等
     */
    MAINTAINABILITY,

    /**
     * 最佳实践
     * <p>
     * 业界推荐的 SQL 编写规范
     */
    BEST_PRACTICE,

    /**
     * 代码风格
     * <p>
     * 代码格式和风格相关的问题
     */
    STYLE
}
