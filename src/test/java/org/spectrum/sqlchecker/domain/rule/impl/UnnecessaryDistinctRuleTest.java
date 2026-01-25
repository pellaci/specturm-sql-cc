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
 * UnnecessaryDistinctRule 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@DisplayName("UnnecessaryDistinctRule 单元测试")
class UnnecessaryDistinctRuleTest {

    private UnnecessaryDistinctRule rule;

    @BeforeEach
    void setUp() {
        rule = new UnnecessaryDistinctRule();
    }

    @Nested
    @DisplayName("getMeta 方法测试")
    class GetMetaTests {

        @Test
        @DisplayName("应该返回正确的规则元数据")
        void should_return_correct_meta() {
            RuleMeta meta = rule.getMeta();

            assertThat(meta).isNotNull();
            assertThat(meta.id()).isEqualTo("unnecessary-distinct");
            assertThat(meta.name()).isEqualTo("Unnecessary DISTINCT");
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
        @DisplayName("应该检测到 DISTINCT 与 JOIN 一起使用")
        void should_detect_distinct_with_join() throws Exception {
            String sql = "SELECT DISTINCT u.name FROM users u INNER JOIN orders o ON u.id = o.user_id";
            RuleContext context = createContext("test-1", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isTrue();
            RuleIssue issue = context.getIssues().get(0);
            assertThat(issue.getRuleId()).isEqualTo("unnecessary-distinct");
            assertThat(issue.getMessage()).contains("JOIN");
        }

        @Test
        @DisplayName("应该检测到 DISTINCT 与 GROUP BY 一起使用")
        void should_detect_distinct_with_group_by() throws Exception {
            String sql = "SELECT DISTINCT department FROM employees GROUP BY department";
            RuleContext context = createContext("test-2", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isTrue();
            RuleIssue issue = context.getIssues().get(0);
            assertThat(issue.getMessage()).contains("GROUP BY");
        }

        @Test
        @DisplayName("不应该对没有 DISTINCT 的查询报告问题")
        void should_not_report_for_no_distinct() throws Exception {
            String sql = "SELECT u.name FROM users u INNER JOIN orders o ON u.id = o.user_id";
            RuleContext context = createContext("test-3", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对只有 DISTINCT 没有 JOIN 或 GROUP BY 的查询报告问题")
        void should_not_report_for_distinct_without_join_or_group_by() throws Exception {
            String sql = "SELECT DISTINCT name FROM users";
            RuleContext context = createContext("test-4", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对非 PlainSelect 节点处理")
        void should_not_process_non_plain_select() throws Exception {
            String sql = "SELECT * FROM users";
            RuleContext context = createContext("test-5", sql);

            rule.visit(new Object(), context);

            assertThat(context.hasIssues()).isFalse();
        }
    }

    @Nested
    @DisplayName("getPriority 方法测试")
    class GetPriorityTests {

        @Test
        @DisplayName("应该返回正确的优先级值")
        void should_return_correct_priority() {
            assertThat(rule.getPriority()).isEqualTo(60);
        }
    }

    private RuleContext createContext(String sqlId, String sql) {
        return RuleContext.builder()
                .sqlId(sqlId)
                .sql(sql)
                .build();
    }
}
