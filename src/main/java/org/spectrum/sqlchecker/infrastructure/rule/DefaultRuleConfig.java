package org.spectrum.sqlchecker.infrastructure.rule;

import org.spectrum.sqlchecker.domain.rule.RuleConfig;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 默认规则配置实现
 * <p>
 * 提供基本的规则配置管理，所有规则默认启用
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Component
public class DefaultRuleConfig implements RuleConfig {

    /**
     * 禁用的规则 ID 集合
     */
    private final Set<String> disabledRules = Set.of();

    /**
     * 规则特定的例外配置
     * 规则 ID -> 例外关键字列表
     */
    private final Map<String, List<String>> ruleExceptions = Map.of();

    /**
     * 规则参数配置
     * 规则 ID -> 参数映射
     */
    private final Map<String, Map<String, Object>> ruleParameters = Map.of();

    @Override
    public boolean isRuleEnabled(String ruleId) {
        if (ruleId == null) {
            return false;
        }
        return !disabledRules.contains(ruleId);
    }

    @Override
    public boolean isInExceptions(String ruleId, String sql) {
        if (ruleId == null || sql == null) {
            return false;
        }

        List<String> exceptions = ruleExceptions.get(ruleId);
        if (exceptions == null || exceptions.isEmpty()) {
            return false;
        }

        return exceptions.stream().anyMatch(sql::contains);
    }

    @Override
    public String getParameter(String ruleId, String paramKey, String defaultValue) {
        Map<String, Object> params = ruleParameters.get(ruleId);
        if (params == null) {
            return defaultValue;
        }

        Object value = params.get(paramKey);
        return value != null ? value.toString() : defaultValue;
    }

    @Override
    public Map<String, Object> getParameters(String ruleId) {
        Map<String, Object> params = ruleParameters.get(ruleId);
        return params != null ? params : Collections.emptyMap();
    }
}
