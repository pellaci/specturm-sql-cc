package org.spectrum.sqlchecker.infrastructure.rule;

import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.domain.rule.RuleConfig;
import org.spectrum.sqlchecker.domain.rule.SqlRule;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 规则注册表
 * <p>
 * 管理所有已注册的规则，支持按节点类型查询规则。
 * 规则通过 Spring 的依赖注入自动注册。
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Slf4j
@Component
public class RuleRegistry {

    /**
     * 节点类型 -> 规则列表 映射
     */
    private final Map<Class<?>, List<SqlRule>> rulesByNodeType = new ConcurrentHashMap<>();

    /**
     * 规则 ID -> 规则 映射
     */
    private final Map<String, SqlRule> rulesById = new ConcurrentHashMap<>();

    /**
     * 规则配置
     */
    private RuleConfig ruleConfig;

    /**
     * 注册规则
     *
     * @param rule 规则实例
     */
    public void register(SqlRule rule) {
        if (rule == null) {
            log.warn("Attempted to register null rule, skipping");
            return;
        }

        String ruleId = rule.getMeta().id();
        if (ruleId == null || ruleId.isBlank()) {
            log.warn("Rule has null or blank id, skipping: {}", rule.getClass().getName());
            return;
        }

        // 检查是否已存在
        if (rulesById.containsKey(ruleId)) {
            log.warn("Rule with id '{}' already registered, replacing with: {}", ruleId, rule.getClass().getName());
            removeRuleMappings(ruleId);
        }

        rulesById.put(ruleId, rule);

        Set<Class<?>> nodeTypes = rule.supportedNodeTypes();
        if (nodeTypes == null || nodeTypes.isEmpty()) {
            log.warn("Rule '{}' supports no node types", ruleId);
            return;
        }

        for (Class<?> nodeType : nodeTypes) {
            rulesByNodeType.computeIfAbsent(nodeType, k -> new ArrayList<>())
                    .add(rule);
        }

        log.debug("Registered rule: {} ({})", ruleId, rule.getClass().getSimpleName());
    }

    /**
     * 批量注册规则
     *
     * @param rules 规则列表
     */
    public void registerAll(Collection<SqlRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return;
        }
        rules.forEach(this::register);
        log.info("Registered {} rules in total", rules.size());
    }

    /**
     * 获取指定节点的所有规则
     * <p>
     * 支持精确匹配和接口匹配（查找实现指定接口的所有类）
     *
     * @param nodeType 节点类型
     * @return 规则列表
     */
    public List<SqlRule> getRulesForNode(Class<?> nodeType) {
        if (nodeType == null) {
            return List.of();
        }

        List<SqlRule> rules = new ArrayList<>();

        // 精确匹配
        rules.addAll(rulesByNodeType.getOrDefault(nodeType, Collections.emptyList()));

        // 接口/父类匹配
        for (Map.Entry<Class<?>, List<SqlRule>> entry : rulesByNodeType.entrySet()) {
            if (nodeType.isAssignableFrom(entry.getKey())) {
                rules.addAll(entry.getValue());
            }
        }

        // 按优先级排序并去重
        return rules.stream()
                .collect(Collectors.toMap(
                        rule -> rule.getMeta().id(),
                        rule -> rule,
                        (first, replacement) -> replacement,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .sorted(Comparator.comparingInt(SqlRule::getPriority))
                .collect(Collectors.toList());
    }

    private void removeRuleMappings(String ruleId) {
        rulesByNodeType.values().forEach(rules ->
                rules.removeIf(rule -> ruleId.equals(rule.getMeta().id()))
        );
        rulesByNodeType.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /**
     * 获取所有已注册的规则
     *
     * @return 所有规则
     */
    public Collection<SqlRule> getAllRules() {
        return Collections.unmodifiableCollection(rulesById.values());
    }

    /**
     * 获取所有启用的规则
     *
     * @return 启用的规则列表
     */
    public List<SqlRule> getEnabledRules() {
        if (ruleConfig == null) {
            return new ArrayList<>(rulesById.values());
        }

        return rulesById.values().stream()
                .filter(rule -> !rule.getMeta().deprecated())
                .filter(rule -> ruleConfig.isRuleEnabled(rule.getMeta().id()))
                .sorted(Comparator.comparingInt(SqlRule::getPriority))
                .collect(Collectors.toList());
    }

    /**
     * 根据 ID 获取规则
     *
     * @param ruleId 规则 ID
     * @return 规则实例（Optional）
     */
    public Optional<SqlRule> getRule(String ruleId) {
        return Optional.ofNullable(rulesById.get(ruleId));
    }

    /**
     * 检查规则是否存在
     *
     * @param ruleId 规则 ID
     * @return 是否存在
     */
    public boolean hasRule(String ruleId) {
        return rulesById.containsKey(ruleId);
    }

    /**
     * 获取已注册规则的数量
     *
     * @return 规则数量
     */
    public int getRuleCount() {
        return rulesById.size();
    }

    /**
     * 设置规则配置
     *
     * @param config 规则配置
     */
    public void setRuleConfig(RuleConfig config) {
        this.ruleConfig = config;
        log.debug("Rule config set: {}", config);
    }

    /**
     * 获取规则配置
     *
     * @return 规则配置
     */
    public RuleConfig getRuleConfig() {
        return ruleConfig;
    }

    /**
     * 清空所有规则
     * <p>
     * 主要用于测试
     */
    public void clear() {
        rulesByNodeType.clear();
        rulesById.clear();
        log.debug("All rules cleared");
    }

    /**
     * 获取规则统计信息
     *
     * @return 统计信息字符串
     */
    public String getStatistics() {
        return String.format("RuleRegistry: %d rules registered, %d node types mapped",
                rulesById.size(), rulesByNodeType.size());
    }
}
