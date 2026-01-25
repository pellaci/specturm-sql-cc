package org.spectrum.sqlchecker.domain.rule.impl;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.spectrum.sqlchecker.domain.rule.RuleContext;
import org.spectrum.sqlchecker.domain.rule.RuleIssue;
import org.spectrum.sqlchecker.domain.rule.annotation.RuleMeta;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;

import static org.assertj.core.api.Assertions.*;

/**
 * ComplexSubqueryRule 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@DisplayName("ComplexSubqueryRule 单元测试")
class ComplexSubqueryRuleTest {

    private ComplexSubqueryRule rule;

    @BeforeEach
    void setUp() {
        rule = new ComplexSubqueryRule();
    }

    @Nested
    @DisplayName("getMeta 方法测试")
    class GetMetaTests {

        @Test
        @DisplayName("应该返回正确的规则元数据")
        void should_return_correct_meta() {
            RuleMeta meta = rule.getMeta();

            assertThat(meta).isNotNull();
            assertThat(meta.id()).isEqualTo("complex-subquery");
            assertThat(meta.name()).isEqualTo("Complex subquery detected");
            assertThat(meta.severity()).isEqualTo(SeverityLevel.INFO);
        }
    }

    @Nested
    @DisplayName("supportedNodeTypes 方法测试")
    class SupportedNodeTypesTests {

        @Test
        @DisplayName("应该支持 PlainSelect 节点类型")
        void should_support_plain_select() {
            assertThat(rule.supportedNodeTypes()).contains(PlainSelect.class);
        }
    }

    @Nested
    @DisplayName("visit 方法测试")
    class VisitTests {

        @Test
        @DisplayName("应该检测到深度嵌套的子查询")
        void should_detect_deeply_nested_subquery() throws Exception {
            // 嵌套深度 > 3 (4层括号)
            String sql = "SELECT * FROM (SELECT * FROM (SELECT * FROM (SELECT * FROM (SELECT id FROM users))))";
            RuleContext context = createContext("test-1", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isTrue();
            assertThat(context.getIssueCount()).isEqualTo(1);

            RuleIssue issue = context.getIssues().get(0);
            assertThat(issue.getRuleId()).isEqualTo("complex-subquery");
            assertThat(issue.getMessage()).contains("嵌套深度");
        }

        @Test
        @DisplayName("不应该对简单查询报告问题")
        void should_not_report_for_simple_query() throws Exception {
            String sql = "SELECT id, name FROM users WHERE id = 1";
            RuleContext context = createContext("test-2", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对浅层嵌套查询报告问题")
        void should_not_report_for_shallow_nesting() throws Exception {
            // 嵌套深度 = 2，低于阈值 3
            String sql = "SELECT * FROM (SELECT id FROM users) t";
            RuleContext context = createContext("test-3", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("应该检测到刚好超过阈值的嵌套")
        void should_detect_threshold_exceeded() throws Exception {
            // 嵌套深度 = 4，超过阈值 3
            String sql = "SELECT * FROM ((((SELECT id FROM users))))";
            RuleContext context = createContext("test-4", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isTrue();
        }
    }

    @Nested
    @DisplayName("getPriority 方法测试")
    class GetPriorityTests {

        @Test
        @DisplayName("应该返回正确的优先级值")
        void should_return_correct_priority() {
            assertThat(rule.getPriority()).isEqualTo(50);
        }
    }

    private RuleContext createContext(String sqlId, String sql) {
        return RuleContext.builder()
                .sqlId(sqlId)
                .sql(sql)
                .build();
    }
}
