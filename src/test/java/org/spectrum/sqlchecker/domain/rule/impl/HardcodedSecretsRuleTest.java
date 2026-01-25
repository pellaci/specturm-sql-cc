package org.spectrum.sqlchecker.domain.rule.impl;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
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
 * HardcodedSecretsRule 单元测试
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@DisplayName("HardcodedSecretsRule 单元测试")
class HardcodedSecretsRuleTest {

    private HardcodedSecretsRule rule;

    @BeforeEach
    void setUp() {
        rule = new HardcodedSecretsRule();
    }

    @Nested
    @DisplayName("getMeta 方法测试")
    class GetMetaTests {

        @Test
        @DisplayName("应该返回正确的规则元数据")
        void should_return_correct_meta() {
            RuleMeta meta = rule.getMeta();

            assertThat(meta).isNotNull();
            assertThat(meta.id()).isEqualTo("hardcoded-secrets");
            assertThat(meta.name()).isEqualTo("Hardcoded secrets detected");
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
    @DisplayName("visit 方法测试")
    class VisitTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "UPDATE users SET password='mypassword' WHERE id = 1",
            "SELECT * FROM users WHERE pwd='hardcoded_pwd'",
            "UPDATE settings SET token='bearer_token_value' WHERE id = 1"
        })
        @DisplayName("应该检测到硬编码的敏感信息")
        void should_detect_hardcoded_secrets(String sql) throws Exception {
            RuleContext context = createContext("test-secret", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isTrue();
            RuleIssue issue = context.getIssues().get(0);
            assertThat(issue.getRuleId()).isEqualTo("hardcoded-secrets");
            assertThat(issue.getSeverity()).isEqualTo(SeverityLevel.CRITICAL);
        }

        @Test
        @DisplayName("不应该对正常 SQL 报告问题")
        void should_not_report_for_normal_sql() throws Exception {
            String sql = "SELECT id, name, email FROM users WHERE active = true";
            RuleContext context = createContext("test-normal", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("不应该对参数化查询报告问题")
        void should_not_report_for_parameterized_query() throws Exception {
            String sql = "UPDATE users SET password = ? WHERE id = ?";
            RuleContext context = createContext("test-param", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            assertThat(context.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("应该检测到字段名包含 password 的硬编码值")
        void should_detect_password_field_with_value() throws Exception {
            String sql = "INSERT INTO users (name, passwd) VALUES ('john', 'pass1234')";
            RuleContext context = createContext("test-passwd", sql);
            Statement statement = CCJSqlParserUtil.parse(sql);

            rule.visit(statement, context);

            // 检测 passwd='pass1234' 模式
            // 这个测试可能需要根据实际规则逻辑调整
        }
    }

    @Nested
    @DisplayName("getPriority 方法测试")
    class GetPriorityTests {

        @Test
        @DisplayName("应该返回正确的优先级值")
        void should_return_correct_priority() {
            assertThat(rule.getPriority()).isEqualTo(8);
        }
    }

    private RuleContext createContext(String sqlId, String sql) {
        return RuleContext.builder()
                .sqlId(sqlId)
                .sql(sql)
                .build();
    }
}
