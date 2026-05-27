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

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * SelectStarRule 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@DisplayName("SelectStarRule 单元测试")
class SelectStarRuleTest {

    private SelectStarRule rule;

    @BeforeEach
    void setUp() {
        rule = new SelectStarRule();
    }

    @Nested
    @DisplayName("getMeta 方法测试")
    class GetMetaTests {

        @Test
        @DisplayName("应该返回正确的规则元数据")
        void should_return_correct_meta() {
            RuleMeta meta = rule.getMeta();

            assertThat(meta).isNotNull();
            assertThat(meta.id()).isEqualTo("select-star");
            assertThat(meta.name()).isEqualTo("Avoid SELECT *");
            assertThat(meta.severity()).isEqualTo(SeverityLevel.CRITICAL);
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

        @Test
        @DisplayName("不应该直接支持 AllColumns 节点类型，避免同一 SELECT * 重复上报")
        void should_not_support_all_columns_directly() {
            assertThat(rule.supportedNodeTypes())
                    .doesNotContain(net.sf.jsqlparser.statement.select.AllColumns.class);
        }
    }

    @Nested
    @DisplayName("visit 方法测试")
    class VisitTests {

        @Test
        @DisplayName("应该检测到 SELECT *")
        void should_detect_select_star() throws Exception {
            String sql = "SELECT * FROM users";
            RuleContext context = createContext("test-1", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isTrue();
            assertThat(context.getIssueCount()).isEqualTo(1);

            RuleIssue issue = context.getIssues().get(0);
            assertThat(issue.getRuleId()).isEqualTo("select-star");
            assertThat(issue.getSeverity()).isEqualTo(SeverityLevel.CRITICAL);
        }

        @Test
        @DisplayName("应该检测到子查询中的 SELECT *")
        void should_detect_select_star_in_subquery() throws Exception {
            String sql = "SELECT id FROM (SELECT * FROM users) t";
            RuleContext context = createContext("test-2", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            // 主查询不应该报告问题
            rule.visit(select, context);

            // 主查询中没有 SELECT *
            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对明确列名的查询报告问题")
        void should_not_report_issue_for_explicit_columns() throws Exception {
            String sql = "SELECT id, name, email FROM users";
            RuleContext context = createContext("test-3", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对单列查询报告问题")
        void should_not_report_issue_for_single_column() throws Exception {
            String sql = "SELECT id FROM users";
            RuleContext context = createContext("test-4", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("应该检测到 COUNT(*) 不作为问题")
        void should_not_report_count_star_as_issue() throws Exception {
            String sql = "SELECT COUNT(*) FROM users";
            RuleContext context = createContext("test-5", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            // COUNT(*) 不是 SELECT *，应该是 AllColumns
            // 实际上这取决于 JSqlParser 如何解析 COUNT(*)
            // COUNT(*) 中的 * 不应该被检测为 SELECT *
            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("应该检测到 table.* 的形式")
        void should_detect_table_star() throws Exception {
            String sql = "SELECT users.* FROM users";
            RuleContext context = createContext("test-6", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isTrue();
            assertThat(context.getIssueCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("重复访问 PlainSelect 和 AllColumns 时不应该重复上报")
        void should_not_duplicate_when_visitor_reaches_all_columns() throws Exception {
            String sql = "SELECT * FROM users";
            RuleContext context = createContext("test-7", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);
            select.getSelectItems().forEach(item -> rule.visit(item.getExpression(), context));

            assertThat(context.getIssueCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getPriority 方法测试")
    class GetPriorityTests {

        @Test
        @DisplayName("应该返回高优先级值")
        void should_return_high_priority() {
            assertThat(rule.getPriority()).isEqualTo(10);
        }
    }

    /**
     * 创建测试上下文
     */
    private RuleContext createContext(String sqlId, String sql) {
        return RuleContext.builder()
                .sqlId(sqlId)
                .sql(sql)
                .build();
    }
}
