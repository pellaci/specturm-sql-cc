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
 * DangerousDropTruncateRule 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@DisplayName("DangerousDropTruncateRule 单元测试")
class DangerousDropTruncateRuleTest {

    private DangerousDropTruncateRule rule;

    @BeforeEach
    void setUp() {
        rule = new DangerousDropTruncateRule();
    }

    @Nested
    @DisplayName("getMeta 方法测试")
    class GetMetaTests {

        @Test
        @DisplayName("应该返回正确的规则元数据")
        void should_return_correct_meta() {
            RuleMeta meta = rule.getMeta();

            assertThat(meta).isNotNull();
            assertThat(meta.id()).isEqualTo("dangerous-drop-truncate");
            assertThat(meta.name()).isEqualTo("DROP/TRUNCATE TABLE detected");
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
    @DisplayName("visit 方法测试 - DROP TABLE")
    class DropTableTests {

        @Test
        @DisplayName("应该检测到 DROP TABLE")
        void should_detect_drop_table() throws Exception {
            String sql = "DROP TABLE users";
            RuleContext context = createContext("test-1", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isTrue();
            assertThat(context.getIssueCount()).isEqualTo(1);

            RuleIssue issue = context.getIssues().get(0);
            assertThat(issue.getRuleId()).isEqualTo("dangerous-drop-truncate");
            assertThat(issue.getMessage()).contains("DROP TABLE");
        }

        @Test
        @DisplayName("应该检测到 DROP TABLE IF EXISTS")
        void should_detect_drop_table_if_exists() throws Exception {
            String sql = "DROP TABLE IF EXISTS users";
            RuleContext context = createContext("test-2", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isTrue();
        }

        @Test
        @DisplayName("应该检测到小写的 drop table")
        void should_detect_lowercase_drop_table() throws Exception {
            String sql = "drop table users";
            RuleContext context = createContext("test-3", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isTrue();
        }
    }

    @Nested
    @DisplayName("visit 方法测试 - TRUNCATE TABLE")
    class TruncateTableTests {

        @Test
        @DisplayName("应该检测到 TRUNCATE TABLE")
        void should_detect_truncate_table() throws Exception {
            String sql = "TRUNCATE TABLE users";
            RuleContext context = createContext("test-4", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isTrue();
            assertThat(context.getIssues().get(0).getMessage()).contains("TRUNCATE TABLE");
        }

        @Test
        @DisplayName("应该检测到小写的 truncate table")
        void should_detect_lowercase_truncate_table() throws Exception {
            String sql = "truncate table users";
            RuleContext context = createContext("test-5", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isTrue();
        }
    }

    @Nested
    @DisplayName("visit 方法测试 - 其他语句")
    class OtherStatementTests {

        @Test
        @DisplayName("不应该对 SELECT 报告问题")
        void should_not_report_select() throws Exception {
            String sql = "SELECT * FROM users";
            RuleContext context = createContext("test-6", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对 DELETE 报告问题")
        void should_not_report_delete() throws Exception {
            String sql = "DELETE FROM users WHERE id = 1";
            RuleContext context = createContext("test-7", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对 CREATE TABLE 报告问题")
        void should_not_report_create_table() throws Exception {
            String sql = "CREATE TABLE users (id INT)";
            RuleContext context = createContext("test-8", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对 ALTER TABLE 报告问题")
        void should_not_report_alter_table() throws Exception {
            String sql = "ALTER TABLE users ADD COLUMN email VARCHAR(100)";
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
        @DisplayName("应该返回最高优先级值")
        void should_return_highest_priority() {
            // DROP/TRUNCATE 是危险操作，应该优先检测
            assertThat(rule.getPriority()).isEqualTo(5);
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
