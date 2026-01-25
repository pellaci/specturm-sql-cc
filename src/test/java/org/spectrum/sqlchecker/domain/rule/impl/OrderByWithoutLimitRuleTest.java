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
 * OrderByWithoutLimitRule 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@DisplayName("OrderByWithoutLimitRule 单元测试")
class OrderByWithoutLimitRuleTest {

    private OrderByWithoutLimitRule rule;

    @BeforeEach
    void setUp() {
        rule = new OrderByWithoutLimitRule();
    }

    @Nested
    @DisplayName("getMeta 方法测试")
    class GetMetaTests {

        @Test
        @DisplayName("应该返回正确的规则元数据")
        void should_return_correct_meta() {
            RuleMeta meta = rule.getMeta();

            assertThat(meta).isNotNull();
            assertThat(meta.id()).isEqualTo("orderby-without-limit");
            assertThat(meta.name()).isEqualTo("ORDER BY without LIMIT");
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
        @DisplayName("应该检测到 ORDER BY 无 LIMIT")
        void should_detect_orderby_without_limit() throws Exception {
            String sql = "SELECT id, name FROM users ORDER BY created_at DESC";
            RuleContext context = createContext("test-1", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getPlainSelect();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isTrue();
            assertThat(context.getIssueCount()).isEqualTo(1);

            RuleIssue issue = context.getIssues().get(0);
            assertThat(issue.getRuleId()).isEqualTo("orderby-without-limit");
            assertThat(issue.getSeverity()).isEqualTo(SeverityLevel.WARNING);
        }

        @Test
        @DisplayName("不应该对有 LIMIT 的 ORDER BY 报告问题")
        void should_not_report_orderby_with_limit() throws Exception {
            String sql = "SELECT id, name FROM users ORDER BY created_at DESC LIMIT 10";
            RuleContext context = createContext("test-2", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getPlainSelect();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对没有 ORDER BY 的查询报告问题")
        void should_not_report_without_orderby() throws Exception {
            String sql = "SELECT id, name FROM users";
            RuleContext context = createContext("test-3", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getPlainSelect();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对 ORDER BY 数字位置报告问题")
        void should_not_report_orderby_position() throws Exception {
            String sql = "SELECT id, name FROM users ORDER BY 1";
            RuleContext context = createContext("test-4", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getPlainSelect();

            rule.visit(select, context);

            // ORDER BY 数字（位置）通常用于特定场景，不报告
            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("应该检测到多列 ORDER BY 无 LIMIT")
        void should_detect_multi_column_orderby_without_limit() throws Exception {
            String sql = "SELECT id, name FROM users ORDER BY status ASC, created_at DESC";
            RuleContext context = createContext("test-5", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getPlainSelect();

            rule.visit(select, context);

            assertThat(context.hasIssues()).isTrue();
        }

        @Test
        @DisplayName("应该处理复杂的 JOIN 查询")
        void should_handle_complex_join() throws Exception {
            String sql = "SELECT u.id, o.amount FROM users u JOIN orders o ON u.id = o.user_id ORDER BY o.amount DESC";
            RuleContext context = createContext("test-6", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getPlainSelect();

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
            assertThat(rule.getPriority()).isEqualTo(25);
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
