package org.spectrum.sqlchecker.infrastructure.rule;

import net.sf.jsqlparser.statement.select.PlainSelect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.spectrum.sqlchecker.domain.rule.RuleConfig;
import org.spectrum.sqlchecker.domain.rule.RuleContext;
import org.spectrum.sqlchecker.domain.rule.RuleIssue;
import org.spectrum.sqlchecker.domain.rule.RuleLocation;
import org.spectrum.sqlchecker.domain.rule.SqlRule;
import org.spectrum.sqlchecker.domain.rule.annotation.RuleMeta;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SqlRuleEngine 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@DisplayName("SqlRuleEngine 单元测试")
class SqlRuleEngineTest {

    private SqlRuleEngine engine;
    private RuleRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new RuleRegistry();
        engine = new SqlRuleEngine(registry);
    }

    @Nested
    @DisplayName("analyze 方法测试")
    class AnalyzeTests {

        @Test
        @DisplayName("应该分析简单 SQL 并返回结果")
        void should_analyze_simple_sql() {
            // 注册一个会检测 SELECT * 的规则
            SqlRule selectStarRule = createSelectStarRule();
            registry.register(selectStarRule);

            String sql = "SELECT * FROM users";
            List<RuleIssue> issues = engine.analyze("test-1", sql);

            // 规则应该检测到 SELECT *
            assertThat(issues).isNotEmpty();
        }

        @Test
        @DisplayName("应该处理空 SQL")
        void should_handle_empty_sql() {
            List<RuleIssue> issues = engine.analyze("test-empty", "");

            assertThat(issues).isEmpty();
        }

        @Test
        @DisplayName("应该处理 null SQL")
        void should_handle_null_sql() {
            List<RuleIssue> issues = engine.analyze("test-null", null);

            assertThat(issues).isEmpty();
        }

        @Test
        @DisplayName("应该处理空白 SQL")
        void should_handle_blank_sql() {
            List<RuleIssue> issues = engine.analyze("test-blank", "   ");

            assertThat(issues).isEmpty();
        }

        @Test
        @DisplayName("应该处理无效 SQL")
        void should_handle_invalid_sql() {
            String invalidSql = "THIS IS NOT VALID SQL";
            List<RuleIssue> issues = engine.analyze("test-invalid", invalidSql);

            // 无效 SQL 应该不会导致异常，但可能会有解析错误
            assertThat(issues).isEmpty();
        }

        @Test
        @DisplayName("应该自动生成 SQL ID")
        void should_auto_generate_sql_id() {
            SqlRule selectStarRule = createSelectStarRule();
            registry.register(selectStarRule);

            String sql = "SELECT * FROM users";
            List<RuleIssue> issues = engine.analyze(sql);

            assertThat(issues).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("analyzeBatch 方法测试")
    class AnalyzeBatchTests {

        @Test
        @DisplayName("应该批量分析 SQL Map")
        void should_analyze_batch_sql_map() {
            SqlRule selectStarRule = createSelectStarRule();
            registry.register(selectStarRule);

            Map<String, String> sqls = new HashMap<>();
            sqls.put("sql-1", "SELECT * FROM users");
            sqls.put("sql-2", "SELECT id FROM orders");

            Map<String, List<RuleIssue>> results = engine.analyzeBatch(sqls);

            assertThat(results).hasSize(2);
            assertThat(results).containsKeys("sql-1", "sql-2");
        }

        @Test
        @DisplayName("应该处理空 Map")
        void should_handle_empty_map() {
            Map<String, List<RuleIssue>> results = engine.analyzeBatch(Collections.<String, String>emptyMap());

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("应该处理 null Map")
        void should_handle_null_map() {
            Map<String, List<RuleIssue>> results = engine.analyzeBatch((Map<String, String>) null);

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("应该批量分析 SQL List")
        void should_analyze_batch_sql_list() {
            SqlRule selectStarRule = createSelectStarRule();
            registry.register(selectStarRule);

            List<String> sqls = List.of(
                "SELECT * FROM users",
                "SELECT id FROM orders"
            );

            Map<String, List<RuleIssue>> results = engine.analyzeBatch(sqls);

            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("应该处理空 List")
        void should_handle_empty_list() {
            Map<String, List<RuleIssue>> results = engine.analyzeBatch(Collections.<String>emptyList());

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("应该处理 null List")
        void should_handle_null_list() {
            Map<String, List<RuleIssue>> results = engine.analyzeBatch((List<String>) null);

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("setRuleConfig 方法测试")
    class SetRuleConfigTests {

        @Test
        @DisplayName("应该设置规则配置")
        void should_set_rule_config() {
            RuleConfig config = mock(RuleConfig.class);

            engine.setRuleConfig(config);

            assertThat(registry.getRuleConfig()).isEqualTo(config);
        }
    }

    @Nested
    @DisplayName("getRuleRegistry 方法测试")
    class GetRuleRegistryTests {

        @Test
        @DisplayName("应该返回规则注册表")
        void should_return_rule_registry() {
            assertThat(engine.getRuleRegistry()).isEqualTo(registry);
        }
    }

    @Nested
    @DisplayName("getRegisteredRuleCount 方法测试")
    class GetRegisteredRuleCountTests {

        @Test
        @DisplayName("应该返回注册规则数量")
        void should_return_registered_rule_count() {
            assertThat(engine.getRegisteredRuleCount()).isEqualTo(0);

            SqlRule rule = createSelectStarRule();
            registry.register(rule);

            assertThat(engine.getRegisteredRuleCount()).isEqualTo(1);
        }
    }

    /**
     * 创建一个检测 SELECT * 的规则
     */
    private SqlRule createSelectStarRule() {
        return new SqlRule() {
            @Override
            public RuleMeta getMeta() {
                return TestRuleMeta.create("select-star", "Avoid SELECT *", SeverityLevel.WARNING);
            }

            @Override
            public Set<Class<?>> supportedNodeTypes() {
                return Set.of(PlainSelect.class);
            }

            @Override
            public void visit(Object node, RuleContext context) {
                if (node instanceof PlainSelect select) {
                    String sql = context.getSql().toUpperCase();
                    if (sql.contains("SELECT *") || sql.contains("SELECT  *")) {
                        RuleIssue issue = RuleIssue.builder()
                                .ruleId(getMeta().id())
                                .ruleName(getMeta().name())
                                .severity(getMeta().severity())
                                .location(RuleLocation.of(context.getSqlId(), 1))
                                .message("Avoid using SELECT *")
                                .build();
                        context.reportIssue(issue);
                    }
                }
            }

            @Override
            public int getPriority() {
                return 10;
            }
        };
    }

    /**
     * 测试用的 RuleMeta 实现
     */
    private static class TestRuleMeta implements RuleMeta {
        private final String id;
        private final String name;
        private final SeverityLevel severity;

        private TestRuleMeta(String id, String name, SeverityLevel severity) {
            this.id = id;
            this.name = name;
            this.severity = severity;
        }

        static RuleMeta create(String id, String name, SeverityLevel severity) {
            return new TestRuleMeta(id, name, severity);
        }

        @Override
        public String id() { return id; }

        @Override
        public String name() { return name; }

        @Override
        public String description() { return ""; }

        @Override
        public org.spectrum.sqlchecker.domain.shared.enumeration.RuleType type() {
            return org.spectrum.sqlchecker.domain.shared.enumeration.RuleType.PROBLEM;
        }

        @Override
        public SeverityLevel severity() { return severity; }

        @Override
        public String[] tags() { return new String[0]; }

        @Override
        public org.spectrum.sqlchecker.domain.shared.enumeration.RuleCategory category() {
            return org.spectrum.sqlchecker.domain.shared.enumeration.RuleCategory.PERFORMANCE;
        }

        @Override
        public boolean deprecated() { return false; }

        @Override
        public Class<? extends java.lang.annotation.Annotation> annotationType() {
            return RuleMeta.class;
        }
    }
}
