package org.spectrum.sqlchecker.infrastructure.rule;

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.spectrum.sqlchecker.domain.rule.RuleConfig;
import org.spectrum.sqlchecker.domain.rule.SqlRule;
import org.spectrum.sqlchecker.domain.rule.annotation.RuleMeta;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RuleRegistry 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@DisplayName("RuleRegistry 单元测试")
class RuleRegistryTest {

    private RuleRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new RuleRegistry();
    }

    @Nested
    @DisplayName("register 方法测试")
    class RegisterTests {

        @Test
        @DisplayName("应该成功注册规则")
        void should_register_rule_successfully() {
            SqlRule rule = createMockRule("test-rule", Set.of(PlainSelect.class));

            registry.register(rule);

            assertThat(registry.getRuleCount()).isEqualTo(1);
            assertThat(registry.hasRule("test-rule")).isTrue();
        }

        @Test
        @DisplayName("应该忽略 null 规则")
        void should_ignore_null_rule() {
            registry.register(null);

            assertThat(registry.getRuleCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("应该替换已存在的规则")
        void should_replace_existing_rule() {
            SqlRule rule1 = createMockRule("test-rule", Set.of(PlainSelect.class));
            SqlRule rule2 = createMockRule("test-rule", Set.of(Statement.class));

            registry.register(rule1);
            registry.register(rule2);

            assertThat(registry.getRuleCount()).isEqualTo(1);
            assertThat(registry.getRule("test-rule").orElse(null)).isEqualTo(rule2);
            assertThat(registry.getRulesForNode(PlainSelect.class)).isEmpty();
            assertThat(registry.getRulesForNode(Statement.class)).containsExactly(rule2);
        }

        @Test
        @DisplayName("重复注册同 ID 规则时不应该重复返回")
        void should_not_return_duplicate_rules_for_same_id() {
            SqlRule rule1 = createMockRule("test-rule", Set.of(PlainSelect.class));
            SqlRule rule2 = createMockRule("test-rule", Set.of(PlainSelect.class));

            registry.register(rule1);
            registry.register(rule2);

            assertThat(registry.getRulesForNode(PlainSelect.class)).containsExactly(rule2);
        }

        @Test
        @DisplayName("应该处理无节点类型的规则")
        void should_handle_rule_with_no_node_types() {
            SqlRule rule = createMockRule("test-rule", Set.of());

            registry.register(rule);

            // 规则应该被注册到 rulesById，但不会映射到任何节点类型
            assertThat(registry.hasRule("test-rule")).isTrue();
            assertThat(registry.getRulesForNode(PlainSelect.class)).isEmpty();
        }
    }

    @Nested
    @DisplayName("registerAll 方法测试")
    class RegisterAllTests {

        @Test
        @DisplayName("应该批量注册规则")
        void should_register_all_rules() {
            SqlRule rule1 = createMockRule("rule-1", Set.of(PlainSelect.class));
            SqlRule rule2 = createMockRule("rule-2", Set.of(Statement.class));

            registry.registerAll(List.of(rule1, rule2));

            assertThat(registry.getRuleCount()).isEqualTo(2);
            assertThat(registry.hasRule("rule-1")).isTrue();
            assertThat(registry.hasRule("rule-2")).isTrue();
        }

        @Test
        @DisplayName("重复批量注册同一组规则不应该产生重复节点映射")
        void should_not_duplicate_node_mappings_when_register_all_repeatedly() {
            SqlRule rule1 = createMockRule("rule-1", Set.of(PlainSelect.class), 10);
            SqlRule rule2 = createMockRule("rule-2", Set.of(PlainSelect.class), 20);

            registry.registerAll(List.of(rule1, rule2));
            registry.registerAll(List.of(rule1, rule2));

            assertThat(registry.getRuleCount()).isEqualTo(2);
            assertThat(registry.getRulesForNode(PlainSelect.class)).containsExactly(rule1, rule2);
        }

        @Test
        @DisplayName("同 ID 规则被新节点类型替换后旧节点类型不应残留")
        void should_remove_old_node_mapping_when_replaced_by_different_node_type() {
            SqlRule plainSelectRule = createMockRule("shared-rule", Set.of(PlainSelect.class), 10);
            SqlRule statementRule = createMockRule("shared-rule", Set.of(Statement.class), 5);

            registry.registerAll(List.of(plainSelectRule));
            registry.registerAll(List.of(statementRule));

            assertThat(registry.getRuleCount()).isEqualTo(1);
            assertThat(registry.getRule("shared-rule")).contains(statementRule);
            assertThat(registry.getRulesForNode(PlainSelect.class)).isEmpty();
            assertThat(registry.getRulesForNode(Statement.class)).containsExactly(statementRule);
        }

        @Test
        @DisplayName("应该处理空列表")
        void should_handle_empty_list() {
            registry.registerAll(Collections.emptyList());

            assertThat(registry.getRuleCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("应该处理 null 列表")
        void should_handle_null_list() {
            registry.registerAll(null);

            assertThat(registry.getRuleCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getRulesForNode 方法测试")
    class GetRulesForNodeTests {

        @Test
        @DisplayName("应该返回指定节点类型的规则")
        void should_return_rules_for_node_type() {
            SqlRule rule1 = createMockRule("rule-1", Set.of(PlainSelect.class), 10);
            SqlRule rule2 = createMockRule("rule-2", Set.of(PlainSelect.class), 20);
            SqlRule rule3 = createMockRule("rule-3", Set.of(Statement.class), 5);

            registry.registerAll(List.of(rule1, rule2, rule3));

            List<SqlRule> rules = registry.getRulesForNode(PlainSelect.class);

            assertThat(rules).hasSize(2);
            // 应该按优先级排序
            assertThat(rules.get(0)).isEqualTo(rule1);
            assertThat(rules.get(1)).isEqualTo(rule2);
        }

        @Test
        @DisplayName("父类型查询会返回已注册的子类型规则并按 ID 去重")
        void should_return_child_rules_for_parent_node_query_without_duplicates() {
            SqlRule plainSelectRule = createMockRule("rule-1", Set.of(PlainSelect.class), 20);
            SqlRule statementRule = createMockRule("rule-2", Set.of(Statement.class), 10);

            registry.registerAll(List.of(plainSelectRule, statementRule));

            List<SqlRule> rules = registry.getRulesForNode(Statement.class);

            assertThat(rules).containsExactly(statementRule, plainSelectRule);
        }

        @Test
        @DisplayName("子类型查询不应该匹配仅注册在父类型上的规则")
        void should_not_match_parent_only_rule_when_querying_child_node() {
            SqlRule statementRule = createMockRule("statement-rule", Set.of(Statement.class), 10);

            registry.register(statementRule);

            assertThat(registry.getRulesForNode(PlainSelect.class)).isEmpty();
        }

        @Test
        @DisplayName("应该处理 null 节点类型")
        void should_handle_null_node_type() {
            SqlRule rule = createMockRule("test-rule", Set.of(PlainSelect.class));
            registry.register(rule);

            List<SqlRule> rules = registry.getRulesForNode(null);

            assertThat(rules).isEmpty();
        }

        @Test
        @DisplayName("应该返回空列表当没有匹配的规则")
        void should_return_empty_when_no_matching_rules() {
            // 使用一个不会被继承的具体类型
            SqlRule rule = createMockRule("test-rule", Set.of(String.class));
            registry.register(rule);

            // 使用完全不相关的类型来测试
            List<SqlRule> rules = registry.getRulesForNode(Integer.class);

            assertThat(rules).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAllRules 方法测试")
    class GetAllRulesTests {

        @Test
        @DisplayName("应该返回所有规则")
        void should_return_all_rules() {
            SqlRule rule1 = createMockRule("rule-1", Set.of(PlainSelect.class));
            SqlRule rule2 = createMockRule("rule-2", Set.of(Statement.class));

            registry.registerAll(List.of(rule1, rule2));

            Collection<SqlRule> allRules = registry.getAllRules();

            assertThat(allRules).hasSize(2);
            assertThat(allRules).contains(rule1, rule2);
        }

        @Test
        @DisplayName("返回的集合应该是不可修改的")
        void should_return_unmodifiable_collection() {
            SqlRule rule = createMockRule("test-rule", Set.of(PlainSelect.class));
            registry.register(rule);

            Collection<SqlRule> allRules = registry.getAllRules();

            assertThatThrownBy(() -> allRules.add(rule))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("getRule 方法测试")
    class GetRuleTests {

        @Test
        @DisplayName("应该返回存在的规则")
        void should_return_existing_rule() {
            SqlRule rule = createMockRule("test-rule", Set.of(PlainSelect.class));
            registry.register(rule);

            Optional<SqlRule> result = registry.getRule("test-rule");

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(rule);
        }

        @Test
        @DisplayName("应该返回空 Optional 当规则不存在")
        void should_return_empty_when_rule_not_exists() {
            Optional<SqlRule> result = registry.getRule("non-existent");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("hasRule 方法测试")
    class HasRuleTests {

        @Test
        @DisplayName("应该返回 true 当规则存在")
        void should_return_true_when_rule_exists() {
            SqlRule rule = createMockRule("test-rule", Set.of(PlainSelect.class));
            registry.register(rule);

            assertThat(registry.hasRule("test-rule")).isTrue();
        }

        @Test
        @DisplayName("应该返回 false 当规则不存在")
        void should_return_false_when_rule_not_exists() {
            assertThat(registry.hasRule("non-existent")).isFalse();
        }
    }

    @Nested
    @DisplayName("clear 方法测试")
    class ClearTests {

        @Test
        @DisplayName("应该清空所有规则")
        void should_clear_all_rules() {
            SqlRule rule1 = createMockRule("rule-1", Set.of(PlainSelect.class));
            SqlRule rule2 = createMockRule("rule-2", Set.of(Statement.class));
            registry.registerAll(List.of(rule1, rule2));

            registry.clear();

            assertThat(registry.getRuleCount()).isEqualTo(0);
            assertThat(registry.hasRule("rule-1")).isFalse();
            assertThat(registry.hasRule("rule-2")).isFalse();
        }
    }

    @Nested
    @DisplayName("ruleConfig 相关测试")
    class RuleConfigTests {

        @Test
        @DisplayName("应该设置和获取规则配置")
        void should_set_and_get_rule_config() {
            RuleConfig config = mock(RuleConfig.class);

            registry.setRuleConfig(config);

            assertThat(registry.getRuleConfig()).isEqualTo(config);
        }

        @Test
        @DisplayName("getEnabledRules 应该过滤禁用的规则")
        void should_filter_disabled_rules() {
            SqlRule rule1 = createMockRule("rule-1", Set.of(PlainSelect.class));
            SqlRule rule2 = createMockRule("rule-2", Set.of(Statement.class));
            registry.registerAll(List.of(rule1, rule2));

            RuleConfig config = mock(RuleConfig.class);
            when(config.isRuleEnabled("rule-1")).thenReturn(true);
            when(config.isRuleEnabled("rule-2")).thenReturn(false);
            registry.setRuleConfig(config);

            List<SqlRule> enabledRules = registry.getEnabledRules();

            assertThat(enabledRules).hasSize(1);
            assertThat(enabledRules.get(0)).isEqualTo(rule1);
        }
    }

    @Nested
    @DisplayName("getStatistics 方法测试")
    class GetStatisticsTests {

        @Test
        @DisplayName("应该返回正确的统计信息")
        void should_return_correct_statistics() {
            SqlRule rule1 = createMockRule("rule-1", Set.of(PlainSelect.class));
            SqlRule rule2 = createMockRule("rule-2", Set.of(Statement.class));
            registry.registerAll(List.of(rule1, rule2));

            String stats = registry.getStatistics();

            assertThat(stats).contains("2 rules registered");
            assertThat(stats).contains("2 node types mapped");
        }
    }

    /**
     * 创建 mock 规则
     */
    private SqlRule createMockRule(String ruleId, Set<Class<?>> nodeTypes) {
        return createMockRule(ruleId, nodeTypes, 10);
    }

    private SqlRule createMockRule(String ruleId, Set<Class<?>> nodeTypes, int priority) {
        SqlRule rule = mock(SqlRule.class);
        RuleMeta meta = mock(RuleMeta.class);

        when(rule.getMeta()).thenReturn(meta);
        when(meta.id()).thenReturn(ruleId);
        when(meta.deprecated()).thenReturn(false);
        when(rule.supportedNodeTypes()).thenReturn(nodeTypes);
        when(rule.getPriority()).thenReturn(priority);

        return rule;
    }
}
