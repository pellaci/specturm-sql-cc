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
 * MagicNumbersRule 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@DisplayName("MagicNumbersRule 单元测试")
class MagicNumbersRuleTest {

    private MagicNumbersRule rule;

    @BeforeEach
    void setUp() {
        rule = new MagicNumbersRule();
    }

    @Nested
    @DisplayName("getMeta 方法测试")
    class GetMetaTests {

        @Test
        @DisplayName("应该返回正确的规则元数据")
        void should_return_correct_meta() {
            RuleMeta meta = rule.getMeta();

            assertThat(meta).isNotNull();
            assertThat(meta.id()).isEqualTo("magic-numbers");
            assertThat(meta.name()).isEqualTo("Magic numbers detected");
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
        @DisplayName("应该检测到多个魔法数字")
        void should_detect_multiple_magic_numbers() throws Exception {
            String sql = "SELECT * FROM users WHERE age > 18 AND salary > 50000 AND score = 100";
            RuleContext context = createContext("test-1", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isTrue();
            RuleIssue issue = context.getIssues().get(0);
            assertThat(issue.getRuleId()).isEqualTo("magic-numbers");
            assertThat(issue.getMessage()).contains("魔法数字");
        }

        @Test
        @DisplayName("不应该对单个数字报告问题")
        void should_not_report_for_single_number() throws Exception {
            String sql = "SELECT * FROM users WHERE id = 123";
            RuleContext context = createContext("test-2", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            // 只有一个魔法数字，不报告
            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对 LIMIT 后的数字报告问题")
        void should_not_report_for_limit_numbers() throws Exception {
            String sql = "SELECT * FROM users LIMIT 100";
            RuleContext context = createContext("test-3", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对 OFFSET 后的数字报告问题")
        void should_not_report_for_offset_numbers() throws Exception {
            String sql = "SELECT * FROM users LIMIT 10 OFFSET 20";
            RuleContext context = createContext("test-4", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对参数化查询报告问题")
        void should_not_report_for_parameterized_query() throws Exception {
            String sql = "SELECT * FROM users WHERE id = ? AND age > ?";
            RuleContext context = createContext("test-5", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对单位数数字报告问题")
        void should_not_report_for_single_digit_numbers() throws Exception {
            String sql = "SELECT * FROM users WHERE status = 1 AND flag = 0";
            RuleContext context = createContext("test-6", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            // 单位数不被视为魔法数字
            assertThat(context.hasIssues()).isFalse();
        }
    }

    @Nested
    @DisplayName("getPriority 方法测试")
    class GetPriorityTests {

        @Test
        @DisplayName("应该返回正确的优先级值")
        void should_return_correct_priority() {
            assertThat(rule.getPriority()).isEqualTo(75);
        }
    }

    private RuleContext createContext(String sqlId, String sql) {
        return RuleContext.builder()
                .sqlId(sqlId)
                .sql(sql)
                .build();
    }
}
