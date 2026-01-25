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
 * ImplicitJoinRule 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@DisplayName("ImplicitJoinRule 单元测试")
class ImplicitJoinRuleTest {

    private ImplicitJoinRule rule;

    @BeforeEach
    void setUp() {
        rule = new ImplicitJoinRule();
    }

    @Nested
    @DisplayName("getMeta 方法测试")
    class GetMetaTests {

        @Test
        @DisplayName("应该返回正确的规则元数据")
        void should_return_correct_meta() {
            RuleMeta meta = rule.getMeta();

            assertThat(meta).isNotNull();
            assertThat(meta.id()).isEqualTo("implicit-join");
            assertThat(meta.name()).isEqualTo("Implicit JOIN detected");
            assertThat(meta.severity()).isEqualTo(SeverityLevel.WARNING);
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
        @DisplayName("不应该对显式 JOIN 报告问题")
        void should_not_report_for_explicit_join() throws Exception {
            String sql = "SELECT * FROM users u INNER JOIN orders o ON u.id = o.user_id";
            RuleContext context = createContext("test-1", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对 LEFT JOIN 报告问题")
        void should_not_report_for_left_join() throws Exception {
            String sql = "SELECT * FROM users u LEFT JOIN orders o ON u.id = o.user_id";
            RuleContext context = createContext("test-2", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对单表查询报告问题")
        void should_not_report_for_single_table() throws Exception {
            String sql = "SELECT * FROM users WHERE id = 1";
            RuleContext context = createContext("test-3", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对子查询报告问题")
        void should_not_report_for_subquery() throws Exception {
            String sql = "SELECT * FROM (SELECT id FROM users) t";
            RuleContext context = createContext("test-4", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isFalse();
        }
    }

    @Nested
    @DisplayName("getPriority 方法测试")
    class GetPriorityTests {

        @Test
        @DisplayName("应该返回正确的优先级值")
        void should_return_correct_priority() {
            assertThat(rule.getPriority()).isEqualTo(30);
        }
    }

    private RuleContext createContext(String sqlId, String sql) {
        return RuleContext.builder()
                .sqlId(sqlId)
                .sql(sql)
                .build();
    }
}
