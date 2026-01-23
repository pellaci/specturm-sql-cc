package org.spectrum.sqlchecker.domain.rule;

import java.util.Map;

/**
 * 规则配置
 * <p>
 * 封装单个规则的配置信息，包括启用状态、严重级别覆盖、例外条件等
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
public interface RuleConfig {

    /**
     * 检查规则是否启用
     *
     * @param ruleId 规则 ID
     * @return 是否启用
     */
    boolean isRuleEnabled(String ruleId);

    /**
     * 获取规则配置的严重级别
     * <p>
     * 如果配置中没有覆盖严重级别，则返回 null
     *
     * @param ruleId 规则 ID
     * @return 配置的严重级别，可能为 null
     */
    default Integer getConfiguredSeverity(String ruleId) {
        return null;
    }

    /**
     * 检查是否在例外列表中
     *
     * @param ruleId 规则 ID
     * @param sql    SQL 语句
     * @return 是否在例外列表中
     */
    default boolean isInExceptions(String ruleId, String sql) {
        return false;
    }

    /**
     * 获取规则的自定义参数
     *
     * @param ruleId    规则 ID
     * @param paramKey  参数键
     * @param defaultValue 默认值
     * @return 参数值
     */
    default String getParameter(String ruleId, String paramKey, String defaultValue) {
        return defaultValue;
    }

    /**
     * 获取规则的所有自定义参数
     *
     * @param ruleId 规则 ID
     * @return 参数映射，可能为空
     */
    default Map<String, Object> getParameters(String ruleId) {
        return Map.of();
    }
}
