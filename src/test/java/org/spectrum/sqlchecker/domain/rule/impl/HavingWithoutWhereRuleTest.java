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
 * HavingWithoutWhereRule 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@DisplayName("HavingWithoutWhereRule 单元测试")
class HavingWithoutWhereRuleTest {

    private HavingWithoutWhereRule rule;

    @BeforeEach
    void setUp() {
        rule = new HavingWithoutWhereRule();
    }

    @Nested
    @DisplayName("getMeta 方法测试")
    class GetMetaTests {

        @Test
        @DisplayName("应该返回正确的规则元数据")
        void should_return_correct_meta() {
            RuleMeta meta = rule.getMeta();

            assertThat(meta).isNotNull();
            assertThat(meta.id()).isEqualTo("having-without-where");
            assertThat(meta.name()).isEqualTo("HAVING without WHERE");
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
        @DisplayName("应该检测到 HAVING 无 WHERE 的情况")
        void should_detect_having_without_where() throws Exception {
            String sql = "SELECT department, COUNT(*) FROM employees GROUP BY department HAVING COUNT(*) > 5";
            RuleContext context = createContext("test-1", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isTrue();
            RuleIssue issue = context.getIssues().get(0);
            assertThat(issue.getRuleId()).isEqualTo("having-without-where");
        }

        @Test
        @DisplayName("不应该对同时有 WHERE 和 HAVING 的查询报告问题")
        void should_not_report_when_both_where_and_having() throws Exception {
            String sql = "SELECT department, COUNT(*) FROM employees WHERE active = true GROUP BY department HAVING COUNT(*) > 5";
            RuleContext context = createContext("test-2", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对没有 GROUP BY 的查询报告问题")
        void should_not_report_without_group_by() throws Exception {
            String sql = "SELECT * FROM employees WHERE active = true";
            RuleContext context = createContext("test-3", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对只有 GROUP BY 无 HAVING 的查询报告问题")
        void should_not_report_group_by_without_having() throws Exception {
            String sql = "SELECT department, COUNT(*) FROM employees GROUP BY department";
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
            assertThat(rule.getPriority()).isEqualTo(45);
        }
    }

    private RuleContext createContext(String sqlId, String sql) {
        return RuleContext.builder()
                .sqlId(sqlId)
                .sql(sql)
                .build();
    }
}
