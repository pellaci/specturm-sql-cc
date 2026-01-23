package org.spectrum.sqlchecker.infrastructure.rule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 规则引擎集成测试
 * 验证所有规则是否正确注册
 */
@SpringBootTest
@DisplayName("规则引擎集成测试")
class RuleEngineIntegrationTest {

    @Autowired
    private RuleRegistry ruleRegistry;

    @Autowired
    private SqlRuleEngine ruleEngine;

    @Test
    @DisplayName("应该注册至少 15 条规则")
    void should_register_at_least_15_rules() {
        int ruleCount = ruleRegistry.getRuleCount();
        System.out.println("Registered rules: " + ruleCount);
        assertThat(ruleCount).isGreaterThanOrEqualTo(15);
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

        System.out.println("Issues found: " + issues.size());
        issues.forEach(issue -> {
            System.out.println("  - " + issue.getRuleId() + ": " + issue.getMessage());
        });

        // 应该检测到 SELECT * 问题
        assertThat(issues).anyMatch(issue ->
            "select-star".equals(issue.getRuleId()) ||
            "SELECT_STAR".equals(issue.getRuleId())
        );
    }

    @Test
    @DisplayName("应该检测 LIKE 前缀通配符")
    void should_detect_like_leading_wildcard() {
        var issues = ruleEngine.analyze("test-like", "SELECT * FROM users WHERE name LIKE '%test'");

        System.out.println("LIKE issues found: " + issues.size());
        issues.forEach(issue -> {
            System.out.println("  - " + issue.getRuleId() + ": " + issue.getMessage());
        });

        // 应该检测到 LIKE 前缀通配符问题
        assertThat(issues).anyMatch(issue ->
            issue.getRuleId().contains("like") || issue.getRuleId().contains("LIKE")
        );
    }

    @Test
    @DisplayName("应该检测 ORDER BY 无 LIMIT")
    void should_detect_orderby_without_limit() {
        var issues = ruleEngine.analyze("test-orderby", "SELECT id FROM users ORDER BY created_at");

        System.out.println("ORDER BY issues found: " + issues.size());
        issues.forEach(issue -> {
            System.out.println("  - " + issue.getRuleId() + ": " + issue.getMessage());
        });

        // 应该检测到 ORDER BY 无 LIMIT 问题
        assertThat(issues).anyMatch(issue ->
            issue.getRuleId().contains("orderby") || issue.getRuleId().contains("ORDER") ||
            issue.getRuleId().contains("limit") || issue.getRuleId().contains("LIMIT")
        );
    }
}
