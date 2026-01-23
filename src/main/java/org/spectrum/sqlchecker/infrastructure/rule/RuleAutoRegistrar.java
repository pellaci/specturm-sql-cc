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
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Slf4j
@Component
public class RuleAutoRegistrar {

    private final RuleRegistry ruleRegistry;
    private final List<SqlRule> allRules;

    /**
     * 构造函数
     * <p>
     * Spring 自动注入所有 SqlRule Bean
     *
     * @param ruleRegistry 规则注册表
     * @param allRules     所有规则 Bean（自动注入）
     */
    @Autowired
    public RuleAutoRegistrar(RuleRegistry ruleRegistry, List<SqlRule> allRules) {
        this.ruleRegistry = ruleRegistry;
        this.allRules = allRules;
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

        ruleRegistry.registerAll(allRules);

        // 输出已注册规则列表
        allRules.forEach(rule -> {
            var meta = rule.getMeta();
            log.info("Registered rule: {} - {} [{}]",
                    meta.id(),
                    meta.name(),
                    meta.category());
        });

        log.info("Total rules registered: {}", allRules.size());
    }
}
