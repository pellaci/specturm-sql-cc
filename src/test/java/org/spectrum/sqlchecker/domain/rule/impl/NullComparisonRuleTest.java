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
 * NullComparisonRule 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@DisplayName("NullComparisonRule 单元测试")
class NullComparisonRuleTest {

    private NullComparisonRule rule;

    @BeforeEach
    void setUp() {
        rule = new NullComparisonRule();
    }

    @Nested
    @DisplayName("getMeta 方法测试")
    class GetMetaTests {

        @Test
        @DisplayName("应该返回正确的规则元数据")
        void should_return_correct_meta() {
            RuleMeta meta = rule.getMeta();

            assertThat(meta).isNotNull();
            assertThat(meta.id()).isEqualTo("null-comparison");
            assertThat(meta.name()).isEqualTo("Incorrect NULL comparison");
            assertThat(meta.severity()).isEqualTo(SeverityLevel.CRITICAL);
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
    @DisplayName("visit 方法测试 - = NULL")
    class EqualsNullTests {

        @Test
        @DisplayName("应该检测到 = NULL")
        void should_detect_equals_null() throws Exception {
            String sql = "SELECT * FROM users WHERE name = NULL";
            RuleContext context = createContext("test-1", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isTrue();
            assertThat(context.getIssueCount()).isEqualTo(1);

            RuleIssue issue = context.getIssues().get(0);
            assertThat(issue.getRuleId()).isEqualTo("null-comparison");
            assertThat(issue.getMessage()).contains("= NULL");
        }

        @Test
        @DisplayName("应该检测到小写的 = null")
        void should_detect_lowercase_equals_null() throws Exception {
            String sql = "SELECT * FROM users WHERE name = null";
            RuleContext context = createContext("test-2", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isTrue();
        }
    }

    @Nested
    @DisplayName("visit 方法测试 - != NULL / <> NULL")
    class NotEqualsNullTests {

        @Test
        @DisplayName("应该检测到 != NULL")
        void should_detect_not_equals_null() throws Exception {
            String sql = "SELECT * FROM users WHERE name != NULL";
            RuleContext context = createContext("test-3", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isTrue();
            assertThat(context.getIssues().get(0).getMessage()).contains("!= NULL");
        }

        @Test
        @DisplayName("应该检测到 <> NULL")
        void should_detect_diamond_null() throws Exception {
            String sql = "SELECT * FROM users WHERE name <> NULL";
            RuleContext context = createContext("test-4", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isTrue();
        }
    }

    @Nested
    @DisplayName("visit 方法测试 - 正确用法")
    class CorrectUsageTests {

        @Test
        @DisplayName("不应该检测到 IS NULL")
        void should_not_detect_is_null() throws Exception {
            String sql = "SELECT * FROM users WHERE name IS NULL";
            RuleContext context = createContext("test-5", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该检测到 IS NOT NULL")
        void should_not_detect_is_not_null() throws Exception {
            String sql = "SELECT * FROM users WHERE name IS NOT NULL";
            RuleContext context = createContext("test-6", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该检测到正常的字符串值")
        void should_not_detect_normal_string_value() throws Exception {
            String sql = "SELECT * FROM users WHERE name = 'John'";
            RuleContext context = createContext("test-7", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该检测到 COALESCE 函数")
        void should_not_detect_coalesce() throws Exception {
            String sql = "SELECT COALESCE(name, 'Unknown') FROM users";
            RuleContext context = createContext("test-8", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isFalse();
        }
    }

    @Nested
    @DisplayName("visit 方法测试 - 复杂场景")
    class ComplexScenarioTests {

        @Test
        @DisplayName("应该检测到 UPDATE 中的 = NULL")
        void should_detect_in_update() throws Exception {
            String sql = "UPDATE users SET status = 1 WHERE deleted_at = NULL";
            RuleContext context = createContext("test-9", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isTrue();
        }

        @Test
        @DisplayName("应该检测到多个 NULL 比较")
        void should_detect_multiple_null_comparisons() throws Exception {
            String sql = "SELECT * FROM users WHERE name = NULL OR email != NULL";
            RuleContext context = createContext("test-10", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            // 应该检测到两个问题
            assertThat(context.hasIssues()).isTrue();
            assertThat(context.getIssueCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("getPriority 方法测试")
    class GetPriorityTests {

        @Test
        @DisplayName("应该返回高优先级值")
        void should_return_high_priority() {
            // NULL 比较是逻辑错误，优先级应该很高
            assertThat(rule.getPriority()).isEqualTo(3);
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
