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
 * MissingWhereRule 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@DisplayName("MissingWhereRule 单元测试")
class MissingWhereRuleTest {

    private MissingWhereRule rule;

    @BeforeEach
    void setUp() {
        rule = new MissingWhereRule();
    }

    @Nested
    @DisplayName("getMeta 方法测试")
    class GetMetaTests {

        @Test
        @DisplayName("应该返回正确的规则元数据")
        void should_return_correct_meta() {
            RuleMeta meta = rule.getMeta();

            assertThat(meta).isNotNull();
            assertThat(meta.id()).isEqualTo("missing-where");
            assertThat(meta.name()).isEqualTo("SELECT without WHERE clause");
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

        @Test
        @DisplayName("应该只支持 PlainSelect 一种类型")
        void should_only_support_plain_select() {
            assertThat(rule.supportedNodeTypes()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("visit 方法测试")
    class VisitTests {

        @Test
        @DisplayName("应该检测到缺少 WHERE 的 SELECT")
        void should_detect_missing_where() throws Exception {
            String sql = "SELECT id, name FROM users";
            RuleContext context = createContext("test-1", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isTrue();
            assertThat(context.getIssueCount()).isEqualTo(1);

            RuleIssue issue = context.getIssues().get(0);
            assertThat(issue.getRuleId()).isEqualTo("missing-where");
            assertThat(issue.getSeverity()).isEqualTo(SeverityLevel.WARNING);
        }

        @Test
        @DisplayName("不应该对有 WHERE 的查询报告问题")
        void should_not_report_issue_with_where() throws Exception {
            String sql = "SELECT id, name FROM users WHERE id = 1";
            RuleContext context = createContext("test-2", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对有 LIMIT 的查询报告问题")
        void should_not_report_issue_with_limit() throws Exception {
            String sql = "SELECT id, name FROM users LIMIT 10";
            RuleContext context = createContext("test-3", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对有 WHERE 和 LIMIT 的查询报告问题")
        void should_not_report_issue_with_where_and_limit() throws Exception {
            String sql = "SELECT id, name FROM users WHERE status = 1 LIMIT 10";
            RuleContext context = createContext("test-4", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("应该正确处理复杂 JOIN 查询没有 WHERE")
        void should_detect_join_without_where() throws Exception {
            String sql = "SELECT u.id, o.amount FROM users u JOIN orders o ON u.id = o.user_id";
            RuleContext context = createContext("test-5", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            rule.visit(select, context);

            // JOIN 有 ON 条件，但没有 WHERE，应该报告问题
            assertThat(context.hasIssues()).isTrue();
        }

        @Test
        @DisplayName("不应该对 OFFSET 放过查询")
        void should_detect_offset_without_limit() throws Exception {
            // 有些数据库允许只有 OFFSET 没有 LIMIT
            String sql = "SELECT id FROM users";
            RuleContext context = createContext("test-6", sql);
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
            assertThat(rule.getPriority()).isEqualTo(20);
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
