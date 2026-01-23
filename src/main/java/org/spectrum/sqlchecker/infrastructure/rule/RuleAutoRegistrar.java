package org.spectrum.sqlchecker.infrastructure.rule;

import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.domain.rule.SqlRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * 规则自动注册器
 * <p>
 * 自动发现并注册所有 SqlRule Bean。Spring 会自动将所有 SqlRule 实现类注入为 Bean。
 * 同时初始化 YAML 配置并应用到规则引擎。
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Slf4j
@Component
public class RuleAutoRegistrar {

    private final RuleRegistry ruleRegistry;
    private final SqlRuleEngine ruleEngine;
    private final List<SqlRule> allRules;
    private final YamlRuleConfig yamlRuleConfig;

    /**
     * 构造函数
     * <p>
     * Spring 自动注入所有 SqlRule Bean 和配置组件
     *
     * @param ruleRegistry  规则注册表
     * @param ruleEngine    规则引擎
     * @param allRules      所有规则 Bean（自动注入）
     * @param yamlRuleConfig YAML 配置
     */
    @Autowired
    public RuleAutoRegistrar(
            RuleRegistry ruleRegistry,
            SqlRuleEngine ruleEngine,
            List<SqlRule> allRules,
            YamlRuleConfig yamlRuleConfig) {
        this.ruleRegistry = ruleRegistry;
        this.ruleEngine = ruleEngine;
        this.allRules = allRules;
        this.yamlRuleConfig = yamlRuleConfig;
    }

    /**
     * 初始化并注册所有规则
     */
    @PostConstruct
    public void registerRules() {
        if (allRules == null || allRules.isEmpty()) {
            log.warn("No SQL rules found for registration");
            return;
        }

        // 注册所有规则
        ruleRegistry.registerAll(allRules);

        // 应用 YAML 配置
        ruleRegistry.setRuleConfig(yamlRuleConfig);
        ruleEngine.setRuleConfig(yamlRuleConfig);

        // 输出已注册规则列表
        allRules.forEach(rule -> {
            var meta = rule.getMeta();
            boolean enabled = yamlRuleConfig.isRuleEnabled(meta.id());
            log.info("Registered rule: {} - {} [{}] {}",
                    meta.id(),
                    meta.name(),
                    meta.category(),
                    enabled ? "✓" : "(disabled)");
        });

        // 输出配置信息
        if (!yamlRuleConfig.getDisabledRules().isEmpty()) {
            log.info("Disabled rules: {}", yamlRuleConfig.getDisabledRules());
        }
        if (!yamlRuleConfig.getRuleExceptions().isEmpty()) {
            log.info("Rule exceptions configured for: {}",
                    yamlRuleConfig.getRuleExceptions().keySet());
        }

        log.info("Total rules registered: {}", allRules.size());
    }
}
