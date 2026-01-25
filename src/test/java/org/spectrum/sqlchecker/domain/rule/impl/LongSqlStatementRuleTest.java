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
 * LongSqlStatementRule 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@DisplayName("LongSqlStatementRule 单元测试")
class LongSqlStatementRuleTest {

    private LongSqlStatementRule rule;

    @BeforeEach
    void setUp() {
        rule = new LongSqlStatementRule();
    }

    @Nested
    @DisplayName("getMeta 方法测试")
    class GetMetaTests {

        @Test
        @DisplayName("应该返回正确的规则元数据")
        void should_return_correct_meta() {
            RuleMeta meta = rule.getMeta();

            assertThat(meta).isNotNull();
            assertThat(meta.id()).isEqualTo("long-sql-statement");
            assertThat(meta.name()).isEqualTo("Long SQL statement");
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
        @DisplayName("应该检测到超长 SQL 语句")
        void should_detect_long_sql_by_characters() throws Exception {
            // 创建一个超过 500 字符的 SQL
            StringBuilder sb = new StringBuilder("SELECT ");
            for (int i = 0; i < 100; i++) {
                if (i > 0) sb.append(", ");
                sb.append("column_name_").append(i).append(" AS alias_").append(i);
            }
            sb.append(" FROM some_table");
            String sql = sb.toString();

            assertThat(sql.length()).isGreaterThan(500);

            RuleContext context = createContext("test-1", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isTrue();
            RuleIssue issue = context.getIssues().get(0);
            assertThat(issue.getRuleId()).isEqualTo("long-sql-statement");
            assertThat(issue.getMessage()).contains("字符数超过 500");
        }

        @Test
        @DisplayName("应该检测到超过行数阈值的 SQL")
        void should_detect_long_sql_by_lines() throws Exception {
            // 创建一个超过 20 行的 SQL
            StringBuilder sb = new StringBuilder("SELECT\n");
            for (int i = 0; i < 25; i++) {
                if (i > 0) sb.append(",\n");
                sb.append("col").append(i);
            }
            sb.append("\nFROM some_table");
            String sql = sb.toString();

            RuleContext context = createContext("test-2", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isTrue();
            RuleIssue issue = context.getIssues().get(0);
            assertThat(issue.getRuleId()).isEqualTo("long-sql-statement");
        }

        @Test
        @DisplayName("不应该对短 SQL 报告问题")
        void should_not_report_for_short_sql() throws Exception {
            String sql = "SELECT id, name FROM users WHERE id = 1";
            RuleContext context = createContext("test-3", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对刚好在阈值内的 SQL 报告问题")
        void should_not_report_for_sql_at_threshold() throws Exception {
            // 创建一个刚好 500 字符以内的 SQL
            StringBuilder sb = new StringBuilder("SELECT ");
            while (sb.length() < 480) {
                sb.append("col, ");
            }
            sb.append("id FROM t");
            String sql = sb.toString();

            // 确保 SQL 长度不超过 500
            if (sql.length() > 500) {
                sql = sql.substring(0, 495) + " FROM t";
            }

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
            assertThat(rule.getPriority()).isEqualTo(70);
        }
    }

    private RuleContext createContext(String sqlId, String sql) {
        return RuleContext.builder()
                .sqlId(sqlId)
                .sql(sql)
                .build();
    }
}
