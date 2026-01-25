package org.spectrum.sqlchecker.domain.rule.impl;

import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
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
 * LikeLeadingWildcardRule 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@DisplayName("LikeLeadingWildcardRule 单元测试")
class LikeLeadingWildcardRuleTest {

    private LikeLeadingWildcardRule rule;

    @BeforeEach
    void setUp() {
        rule = new LikeLeadingWildcardRule();
    }

    @Nested
    @DisplayName("getMeta 方法测试")
    class GetMetaTests {

        @Test
        @DisplayName("应该返回正确的规则元数据")
        void should_return_correct_meta() {
            RuleMeta meta = rule.getMeta();

            assertThat(meta).isNotNull();
            assertThat(meta.id()).isEqualTo("like-leading-wildcard");
            assertThat(meta.name()).isEqualTo("Avoid LIKE with leading wildcard");
            assertThat(meta.severity()).isEqualTo(SeverityLevel.CRITICAL);
        }
    }

    @Nested
    @DisplayName("supportedNodeTypes 方法测试")
    class SupportedNodeTypesTests {

        @Test
        @DisplayName("应该支持 LikeExpression 节点类型")
        void should_support_like_expression() {
            assertThat(rule.supportedNodeTypes()).contains(LikeExpression.class);
        }
    }

    @Nested
    @DisplayName("visit 方法测试")
    class VisitTests {

        @Test
        @DisplayName("应该检测到 LIKE '%xxx'")
        void should_detect_like_leading_percent() {
            LikeExpression likeExpr = new LikeExpression();
            likeExpr.setLeftExpression(new Column("name"));
            likeExpr.setRightExpression(new StringValue("%test"));

            RuleContext context = createContext("test-1", "SELECT * FROM users WHERE name LIKE '%test'");

            rule.visit(likeExpr, context);

            assertThat(context.hasIssues()).isTrue();
            assertThat(context.getIssueCount()).isEqualTo(1);

            RuleIssue issue = context.getIssues().get(0);
            assertThat(issue.getRuleId()).isEqualTo("like-leading-wildcard");
            assertThat(issue.getSeverity()).isEqualTo(SeverityLevel.CRITICAL);
        }

        @Test
        @DisplayName("应该检测到 LIKE '%xxx%'")
        void should_detect_like_both_wildcards() {
            LikeExpression likeExpr = new LikeExpression();
            likeExpr.setLeftExpression(new Column("name"));
            likeExpr.setRightExpression(new StringValue("%test%"));

            RuleContext context = createContext("test-2", "SELECT * FROM users WHERE name LIKE '%test%'");

            rule.visit(likeExpr, context);

            assertThat(context.hasIssues()).isTrue();
        }

        @Test
        @DisplayName("不应该检测到 LIKE 'xxx%'")
        void should_not_detect_like_trailing_wildcard() {
            LikeExpression likeExpr = new LikeExpression();
            likeExpr.setLeftExpression(new Column("name"));
            likeExpr.setRightExpression(new StringValue("test%"));

            RuleContext context = createContext("test-3", "SELECT * FROM users WHERE name LIKE 'test%'");

            rule.visit(likeExpr, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该检测到没有通配符的 LIKE")
        void should_not_detect_like_without_wildcard() {
            LikeExpression likeExpr = new LikeExpression();
            likeExpr.setLeftExpression(new Column("name"));
            likeExpr.setRightExpression(new StringValue("test"));

            RuleContext context = createContext("test-4", "SELECT * FROM users WHERE name LIKE 'test'");

            rule.visit(likeExpr, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该检测到单独的 '%'")
        void should_not_detect_single_percent() {
            LikeExpression likeExpr = new LikeExpression();
            likeExpr.setLeftExpression(new Column("name"));
            likeExpr.setRightExpression(new StringValue("%"));

            RuleContext context = createContext("test-5", "SELECT * FROM users WHERE name LIKE '%'");

            rule.visit(likeExpr, context);

            // 单独的 % 不触发规则（根据规则实现）
            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("应该处理复杂的 SQL 语句")
        void should_handle_complex_sql() throws Exception {
            String sql = "SELECT * FROM users WHERE name LIKE '%test' AND status = 1";
            RuleContext context = createContext("test-6", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            PlainSelect select = (PlainSelect) ((Select) statement).getPlainSelect();

            // 从 WHERE 中提取 LikeExpression
            if (select.getWhere() instanceof net.sf.jsqlparser.expression.operators.conditional.AndExpression and) {
                if (and.getLeftExpression() instanceof LikeExpression likeExpr) {
                    rule.visit(likeExpr, context);
                }
            }

            assertThat(context.hasIssues()).isTrue();
        }

        @Test
        @DisplayName("应该处理 NOT LIKE")
        void should_handle_not_like() {
            LikeExpression likeExpr = new LikeExpression();
            likeExpr.setLeftExpression(new Column("name"));
            likeExpr.setRightExpression(new StringValue("%test"));
            likeExpr.setNot(true);

            RuleContext context = createContext("test-7", "SELECT * FROM users WHERE name NOT LIKE '%test'");

            rule.visit(likeExpr, context);

            // NOT LIKE '%xxx' 同样有性能问题
            assertThat(context.hasIssues()).isTrue();
        }
    }

    @Nested
    @DisplayName("getPriority 方法测试")
    class GetPriorityTests {

        @Test
        @DisplayName("应该返回正确的优先级值")
        void should_return_correct_priority() {
            assertThat(rule.getPriority()).isEqualTo(15);
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
