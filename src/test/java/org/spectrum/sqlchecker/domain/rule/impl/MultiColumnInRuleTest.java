package org.spectrum.sqlchecker.domain.rule.impl;

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

/**
 * MultiColumnInRule 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@DisplayName("MultiColumnInRule 单元测试")
class MultiColumnInRuleTest {

    private MultiColumnInRule rule;

    @BeforeEach
    void setUp() {
        rule = new MultiColumnInRule();
    }

    @Nested
    @DisplayName("getMeta 方法测试")
    class GetMetaTests {

        @Test
        @DisplayName("应该返回正确的规则元数据")
        void should_return_correct_meta() {
            RuleMeta meta = rule.getMeta();

            assertThat(meta).isNotNull();
            assertThat(meta.id()).isEqualTo("multi-column-in");
            assertThat(meta.name()).isEqualTo("Multi-column IN detected");
            assertThat(meta.severity()).isEqualTo(SeverityLevel.INFO);
        }
    }

    @Nested
    @DisplayName("supportedNodeTypes 方法测试")
    class SupportedNodeTypesTests {

        @Test
        @DisplayName("应该支持 Statement 节点类型")
        void should_support_statement() {
            assertThat(rule.supportedNodeTypes()).contains(Statement.class);
        }
    }

    @Nested
    @DisplayName("visit 方法测试")
    class VisitTests {

        @Test
        @DisplayName("应该检测到多列 IN 子查询")
        void should_detect_multi_column_in_subquery() throws Exception {
            String sql = "SELECT * FROM orders WHERE (user_id, product_id) IN (SELECT user_id, product_id FROM wishlist)";
            RuleContext context = createContext("test-1", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isTrue();
            RuleIssue issue = context.getIssues().get(0);
            assertThat(issue.getRuleId()).isEqualTo("multi-column-in");
        }

        @Test
        @DisplayName("不应该对单列 IN 报告问题")
        void should_not_report_for_single_column_in() throws Exception {
            String sql = "SELECT * FROM users WHERE id IN (SELECT user_id FROM orders)";
            RuleContext context = createContext("test-2", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对 IN 列表报告问题")
        void should_not_report_for_in_list() throws Exception {
            String sql = "SELECT * FROM users WHERE id IN (1, 2, 3)";
            RuleContext context = createContext("test-3", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对没有 IN 的查询报告问题")
        void should_not_report_for_no_in() throws Exception {
            String sql = "SELECT * FROM users WHERE id = 1";
            RuleContext context = createContext("test-4", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isFalse();
        }
    }

    @Nested
    @DisplayName("getPriority 方法测试")
    class GetPriorityTests {

        @Test
        @DisplayName("应该返回正确的优先级值")
        void should_return_correct_priority() {
            assertThat(rule.getPriority()).isEqualTo(65);
        }
    }

    private RuleContext createContext(String sqlId, String sql) {
        return RuleContext.builder()
                .sqlId(sqlId)
                .sql(sql)
                .build();
    }
}
