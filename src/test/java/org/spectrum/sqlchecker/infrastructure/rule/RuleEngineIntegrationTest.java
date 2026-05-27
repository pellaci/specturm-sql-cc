package org.spectrum.sqlchecker.infrastructure.rule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spectrum.sqlchecker.cli.SqlCheckerApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 规则引擎集成测试
 * 验证所有规则是否正确注册
 */
@SpringBootTest(classes = SqlCheckerApplication.class, properties = "sqlchecker.cli.enabled=false")
@DisplayName("规则引擎集成测试")
class RuleEngineIntegrationTest {

    @Autowired
    private RuleRegistry ruleRegistry;

    @Autowired
    private SqlRuleEngine ruleEngine;

    @Test
    @DisplayName("应该注册至少 15 条规则")
    void should_register_at_least_15_rules() {
        assertThat(ruleRegistry.getRuleCount()).isGreaterThanOrEqualTo(15);
    }

    @Test
    @DisplayName("应该包含关键规则")
    void should_contain_key_rules() {
        assertThat(ruleRegistry.hasRule("select-star")).isTrue();
        assertThat(ruleRegistry.hasRule("missing-where")).isTrue();
        assertThat(ruleRegistry.hasRule("like-leading-wildcard")).isTrue();
        assertThat(ruleRegistry.hasRule("orderby-without-limit")).isTrue();
        assertThat(ruleRegistry.hasRule("implicit-join")).isTrue();
        assertThat(ruleRegistry.hasRule("complex-subquery")).isTrue();
        assertThat(ruleRegistry.hasRule("function-on-indexed-column")).isTrue();
        assertThat(ruleRegistry.hasRule("multi-column-or")).isTrue();
        assertThat(ruleRegistry.hasRule("in-subquery")).isTrue();
        assertThat(ruleRegistry.hasRule("not-in")).isTrue();
        assertThat(ruleRegistry.hasRule("dangerous-drop-truncate")).isTrue();
    }

    @Test
    @DisplayName("应该正确分析 SQL")
    void should_analyze_sql_correctly() {
        var issues = ruleEngine.analyze("test-1", "SELECT * FROM users");

        assertThat(issues)
                .extracting("ruleId")
                .containsExactlyInAnyOrder("select-star", "missing-where");
    }

    @Test
    @DisplayName("应该检测 LIKE 前缀通配符")
    void should_detect_like_leading_wildcard() {
        var issues = ruleEngine.analyze("test-like", "SELECT * FROM users WHERE name LIKE '%test'");

        assertThat(issues)
                .extracting("ruleId")
                .containsExactlyInAnyOrder("select-star", "like-leading-wildcard");
    }

    @Test
    @DisplayName("应该检测 ORDER BY 无 LIMIT")
    void should_detect_orderby_without_limit() {
        var issues = ruleEngine.analyze("test-orderby", "SELECT id FROM users ORDER BY created_at");

        assertThat(issues)
                .extracting("ruleId")
                .containsExactlyInAnyOrder("missing-where", "orderby-without-limit");
    }
}
