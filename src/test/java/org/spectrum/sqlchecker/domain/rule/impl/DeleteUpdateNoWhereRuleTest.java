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
 * DeleteUpdateNoWhereRule 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@DisplayName("DeleteUpdateNoWhereRule 单元测试")
class DeleteUpdateNoWhereRuleTest {

    private DeleteUpdateNoWhereRule rule;

    @BeforeEach
    void setUp() {
        rule = new DeleteUpdateNoWhereRule();
    }

    @Nested
    @DisplayName("getMeta 方法测试")
    class GetMetaTests {

        @Test
        @DisplayName("应该返回正确的规则元数据")
        void should_return_correct_meta() {
            RuleMeta meta = rule.getMeta();

            assertThat(meta).isNotNull();
            assertThat(meta.id()).isEqualTo("delete-update-no-where");
            assertThat(meta.name()).isEqualTo("DELETE/UPDATE without WHERE");
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
    @DisplayName("visit 方法测试 - DELETE 语句")
    class DeleteTests {

        @Test
        @DisplayName("应该检测到 DELETE 无 WHERE")
        void should_detect_delete_without_where() throws Exception {
            String sql = "DELETE FROM users";
            RuleContext context = createContext("test-1", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isTrue();
            assertThat(context.getIssueCount()).isEqualTo(1);

            RuleIssue issue = context.getIssues().get(0);
            assertThat(issue.getRuleId()).isEqualTo("delete-update-no-where");
            assertThat(issue.getMessage()).contains("DELETE");
        }

        @Test
        @DisplayName("不应该对有 WHERE 的 DELETE 报告问题")
        void should_not_report_delete_with_where() throws Exception {
            String sql = "DELETE FROM users WHERE id = 1";
            RuleContext context = createContext("test-2", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("应该处理多余空格的 DELETE")
        void should_handle_delete_with_spaces() throws Exception {
            String sql = "DELETE   FROM   users";
            RuleContext context = createContext("test-3", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isTrue();
        }
    }

    @Nested
    @DisplayName("visit 方法测试 - UPDATE 语句")
    class UpdateTests {

        @Test
        @DisplayName("应该检测到 UPDATE 无 WHERE")
        void should_detect_update_without_where() throws Exception {
            String sql = "UPDATE users SET status = 0";
            RuleContext context = createContext("test-4", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isTrue();
            assertThat(context.getIssueCount()).isEqualTo(1);

            RuleIssue issue = context.getIssues().get(0);
            assertThat(issue.getRuleId()).isEqualTo("delete-update-no-where");
            assertThat(issue.getMessage()).contains("UPDATE");
        }

        @Test
        @DisplayName("不应该对有 WHERE 的 UPDATE 报告问题")
        void should_not_report_update_with_where() throws Exception {
            String sql = "UPDATE users SET status = 0 WHERE id = 1";
            RuleContext context = createContext("test-5", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("应该处理复杂的 UPDATE 无 WHERE")
        void should_detect_complex_update_without_where() throws Exception {
            String sql = "UPDATE users SET status = 0, updated_at = NOW()";
            RuleContext context = createContext("test-6", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isTrue();
        }

        @Test
        @DisplayName("不应该对有复杂 WHERE 的 UPDATE 报告问题")
        void should_not_report_complex_update_with_where() throws Exception {
            String sql = "UPDATE users SET status = 0 WHERE id IN (SELECT user_id FROM expired_accounts)";
            RuleContext context = createContext("test-7", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isFalse();
        }
    }

    @Nested
    @DisplayName("visit 方法测试 - 其他语句")
    class OtherStatementTests {

        @Test
        @DisplayName("不应该对 SELECT 报告问题")
        void should_not_report_select() throws Exception {
            String sql = "SELECT * FROM users";
            RuleContext context = createContext("test-8", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对 INSERT 报告问题")
        void should_not_report_insert() throws Exception {
            String sql = "INSERT INTO users (name) VALUES ('test')";
            RuleContext context = createContext("test-9", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isFalse();
        }
    }

    @Nested
    @DisplayName("getPriority 方法测试")
    class GetPriorityTests {

        @Test
        @DisplayName("应该返回极高优先级值")
        void should_return_very_high_priority() {
            // DELETE/UPDATE 无 WHERE 是灾难性操作
            assertThat(rule.getPriority()).isEqualTo(2);
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
