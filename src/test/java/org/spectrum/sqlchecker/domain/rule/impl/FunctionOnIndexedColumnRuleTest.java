package org.spectrum.sqlchecker.domain.rule.impl;

import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.spectrum.sqlchecker.domain.rule.RuleContext;
import org.spectrum.sqlchecker.domain.rule.RuleIssue;
import org.spectrum.sqlchecker.domain.rule.annotation.RuleMeta;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;

import static org.assertj.core.api.Assertions.*;

/**
 * FunctionOnIndexedColumnRule 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@DisplayName("FunctionOnIndexedColumnRule 单元测试")
class FunctionOnIndexedColumnRuleTest {

    private FunctionOnIndexedColumnRule rule;

    @BeforeEach
    void setUp() {
        rule = new FunctionOnIndexedColumnRule();
    }

    @Nested
    @DisplayName("getMeta 方法测试")
    class GetMetaTests {

        @Test
        @DisplayName("应该返回正确的规则元数据")
        void should_return_correct_meta() {
            RuleMeta meta = rule.getMeta();

            assertThat(meta).isNotNull();
            assertThat(meta.id()).isEqualTo("function-on-indexed-column");
            assertThat(meta.name()).isEqualTo("Function on indexed column in WHERE");
            assertThat(meta.severity()).isEqualTo(SeverityLevel.CRITICAL);
        }
    }

    @Nested
    @DisplayName("supportedNodeTypes 方法测试")
    class SupportedNodeTypesTests {

        @Test
        @DisplayName("应该支持 Function 节点类型")
        void should_support_function() {
            assertThat(rule.supportedNodeTypes()).contains(Function.class);
        }

        @Test
        @DisplayName("应该支持 PlainSelect 节点类型")
        void should_support_plain_select() {
            assertThat(rule.supportedNodeTypes()).contains(PlainSelect.class);
        }
    }

    @Nested
    @DisplayName("visit 方法测试")
    class VisitTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "SELECT * FROM users WHERE UPPER(name) = 'JOHN'",
            "SELECT * FROM users WHERE LOWER(email) = 'test@example.com'",
            "SELECT * FROM users WHERE TRIM(name) = 'John'",
            "SELECT * FROM users WHERE YEAR(created_at) = 2024",
            "SELECT * FROM orders WHERE DATE(order_date) = '2024-01-01'"
        })
        @DisplayName("应该检测到 WHERE 子句中索引列使用函数")
        void should_detect_function_on_indexed_column(String sql) throws Exception {
            RuleContext context = createContext("test-func", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            // 从解析结果中提取 Function 节点
            PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();

            // 模拟提取函数并测试
            Function function = new Function();
            function.setName(extractFunctionName(sql));

            rule.visit(function, context);

            assertThat(context.hasIssues()).isTrue();
            RuleIssue issue = context.getIssues().get(0);
            assertThat(issue.getRuleId()).isEqualTo("function-on-indexed-column");
        }

        @Test
        @DisplayName("不应该对 SELECT 子句中的函数报告问题")
        void should_not_report_for_function_in_select() throws Exception {
            String sql = "SELECT UPPER(name) FROM users";
            RuleContext context = createContext("test-no-where", sql);

            Function function = new Function();
            function.setName("UPPER");

            rule.visit(function, context);

            // 因为没有 WHERE 子句，不应该报告问题
            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对非索引失效函数报告问题")
        void should_not_report_for_safe_functions() throws Exception {
            String sql = "SELECT * FROM users WHERE ABS(score) > 10";
            RuleContext context = createContext("test-safe", sql);

            Function function = new Function();
            function.setName("ABS");

            rule.visit(function, context);

            // ABS 不在非 SARGable 函数列表中
            assertThat(context.hasIssues()).isFalse();
        }
    }

    @Nested
    @DisplayName("getPriority 方法测试")
    class GetPriorityTests {

        @Test
        @DisplayName("应该返回正确的优先级值")
        void should_return_correct_priority() {
            assertThat(rule.getPriority()).isEqualTo(12);
        }
    }

    private RuleContext createContext(String sqlId, String sql) {
        return RuleContext.builder()
                .sqlId(sqlId)
                .sql(sql)
                .build();
    }

    private String extractFunctionName(String sql) {
        String upperSql = sql.toUpperCase();
        String[] functions = {"UPPER", "LOWER", "TRIM", "YEAR", "MONTH", "DAY", "DATE", "SUBSTRING"};
        for (String func : functions) {
            if (upperSql.contains(func + "(")) {
                return func;
            }
        }
        return "UNKNOWN";
    }
}
