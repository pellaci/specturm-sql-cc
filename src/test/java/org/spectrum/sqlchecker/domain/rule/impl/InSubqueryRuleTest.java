package org.spectrum.sqlchecker.domain.rule.impl;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.spectrum.sqlchecker.domain.rule.RuleContext;
import org.spectrum.sqlchecker.domain.rule.RuleIssue;
import org.spectrum.sqlchecker.domain.rule.annotation.RuleMeta;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * InSubqueryRule 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@DisplayName("InSubqueryRule 单元测试")
class InSubqueryRuleTest {

    private InSubqueryRule rule;

    @BeforeEach
    void setUp() {
        rule = new InSubqueryRule();
    }

    @Nested
    @DisplayName("getMeta 方法测试")
    class GetMetaTests {

        @Test
        @DisplayName("应该返回正确的规则元数据")
        void should_return_correct_meta() {
            RuleMeta meta = rule.getMeta();

            assertThat(meta).isNotNull();
            assertThat(meta.id()).isEqualTo("in-subquery");
            assertThat(meta.name()).isEqualTo("IN with subquery");
            assertThat(meta.severity()).isEqualTo(SeverityLevel.INFO);
        }
    }

    @Nested
    @DisplayName("supportedNodeTypes 方法测试")
    class SupportedNodeTypesTests {

        @Test
        @DisplayName("应该支持 InExpression 节点类型")
        void should_support_in_expression() {
            assertThat(rule.supportedNodeTypes()).contains(InExpression.class);
        }
    }

    @Nested
    @DisplayName("visit 方法测试")
    class VisitTests {

        @Test
        @DisplayName("应该检测到 IN 子查询")
        void should_detect_in_subquery() throws Exception {
            String sql = "SELECT * FROM users WHERE id IN (SELECT user_id FROM orders)";
            RuleContext context = createContext("test-1", sql);

            // 创建模拟的 InExpression
            InExpression inExpr = mock(InExpression.class);
            Expression rightExpr = mock(Expression.class);
            when(inExpr.getRightExpression()).thenReturn(rightExpr);
            when(rightExpr.toString()).thenReturn("SELECT user_id FROM orders");

            rule.visit(inExpr, context);

            assertThat(context.hasIssues()).isTrue();
            RuleIssue issue = context.getIssues().get(0);
            assertThat(issue.getRuleId()).isEqualTo("in-subquery");
        }

        @Test
        @DisplayName("不应该对 IN 列表报告问题")
        void should_not_report_for_in_list() throws Exception {
            String sql = "SELECT * FROM users WHERE id IN (1, 2, 3)";
            RuleContext context = createContext("test-2", sql);

            // 创建模拟的 InExpression
            InExpression inExpr = mock(InExpression.class);
            Expression rightExpr = mock(Expression.class);
            when(inExpr.getRightExpression()).thenReturn(rightExpr);
            when(rightExpr.toString()).thenReturn("(1, 2, 3)");

            rule.visit(inExpr, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对非 InExpression 节点处理")
        void should_not_process_non_in_expression() throws Exception {
            String sql = "SELECT * FROM users WHERE id = 1";
            RuleContext context = createContext("test-3", sql);

            // 传递一个非 InExpression 对象
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
            assertThat(rule.getPriority()).isEqualTo(55);
        }
    }

    private RuleContext createContext(String sqlId, String sql) {
        return RuleContext.builder()
                .sqlId(sqlId)
                .sql(sql)
                .build();
    }
}
