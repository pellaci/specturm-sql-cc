package org.spectrum.sqlchecker.domain.rule.impl;

import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
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
 * NotInRule 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@DisplayName("NotInRule 单元测试")
class NotInRuleTest {

    private NotInRule rule;

    @BeforeEach
    void setUp() {
        rule = new NotInRule();
    }

    @Nested
    @DisplayName("getMeta 方法测试")
    class GetMetaTests {

        @Test
        @DisplayName("应该返回正确的规则元数据")
        void should_return_correct_meta() {
            RuleMeta meta = rule.getMeta();

            assertThat(meta).isNotNull();
            assertThat(meta.id()).isEqualTo("not-in");
            assertThat(meta.name()).isEqualTo("NOT IN with potential NULL issue");
            assertThat(meta.severity()).isEqualTo(SeverityLevel.WARNING);
        }
    }

    @Nested
    @DisplayName("supportedNodeTypes 方法测试")
    class SupportedNodeTypesTests {

        @Test
        @DisplayName("应该支持 NotExpression 节点类型")
        void should_support_not_expression() {
            assertThat(rule.supportedNodeTypes()).contains(NotExpression.class);
        }
    }

    @Nested
    @DisplayName("visit 方法测试")
    class VisitTests {

        @Test
        @DisplayName("应该检测到 NOT IN")
        void should_detect_not_in() throws Exception {
            String sql = "SELECT * FROM users WHERE id NOT IN (SELECT user_id FROM blocked_users)";
            RuleContext context = createContext("test-1", sql);

            // 创建模拟的 NotExpression
            NotExpression notExpr = mock(NotExpression.class);
            when(notExpr.toString()).thenReturn("id NOT IN (SELECT user_id FROM blocked_users)");

            rule.visit(notExpr, context);

            assertThat(context.hasIssues()).isTrue();
            RuleIssue issue = context.getIssues().get(0);
            assertThat(issue.getRuleId()).isEqualTo("not-in");
        }

        @Test
        @DisplayName("应该检测到 NOT IN 列表")
        void should_detect_not_in_list() throws Exception {
            String sql = "SELECT * FROM users WHERE id NOT IN (1, 2, 3)";
            RuleContext context = createContext("test-2", sql);

            NotExpression notExpr = mock(NotExpression.class);
            when(notExpr.toString()).thenReturn("id NOT IN (1, 2, 3)");

            rule.visit(notExpr, context);

            assertThat(context.hasIssues()).isTrue();
        }

        @Test
        @DisplayName("不应该对非 NOT IN 的 NOT 表达式报告问题")
        void should_not_report_for_other_not_expressions() throws Exception {
            String sql = "SELECT * FROM users WHERE NOT active";
            RuleContext context = createContext("test-3", sql);

            NotExpression notExpr = mock(NotExpression.class);
            when(notExpr.toString()).thenReturn("NOT active");

            rule.visit(notExpr, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对非 NotExpression 节点处理")
        void should_not_process_non_not_expression() throws Exception {
            String sql = "SELECT * FROM users WHERE id IN (1, 2, 3)";
            RuleContext context = createContext("test-4", sql);

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
            assertThat(rule.getPriority()).isEqualTo(40);
        }
    }

    private RuleContext createContext(String sqlId, String sql) {
        return RuleContext.builder()
                .sqlId(sqlId)
                .sql(sql)
                .build();
    }
}
