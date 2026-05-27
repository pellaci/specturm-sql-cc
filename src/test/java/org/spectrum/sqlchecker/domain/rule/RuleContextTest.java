package org.spectrum.sqlchecker.domain.rule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("RuleContext 单元测试")
class RuleContextTest {

    @Test
    @DisplayName("相同 rule、message、location 的问题应该去重")
    void should_deduplicate_same_rule_message_and_location() {
        RuleContext context = createContext();
        RuleIssue issue = issue("select-star", "使用了 SELECT *", RuleLocation.of("sql-1", 1, 1));

        context.reportIssue(issue);
        context.reportIssue(issue("select-star", "使用了 SELECT *", RuleLocation.of("sql-1", 1, 1)));

        assertThat(context.getIssues()).containsExactly(issue);
    }

    @Test
    @DisplayName("相同 rule 和 location 但不同 message 的问题应该保留")
    void should_keep_distinct_messages_at_same_location() {
        RuleContext context = createContext();

        context.reportIssue(issue("hardcoded-secrets", "发现硬编码敏感信息", RuleLocation.of("sql-1", 1, 1)));
        context.reportIssue(issue("hardcoded-secrets", "密码字段包含明文值", RuleLocation.of("sql-1", 1, 1)));

        assertThat(context.getIssues())
                .extracting(RuleIssue::getMessage)
                .containsExactly("发现硬编码敏感信息", "密码字段包含明文值");
    }

    @Test
    @DisplayName("相同 rule 和 message 但不同 location 的问题应该保留")
    void should_keep_same_message_at_different_locations() {
        RuleContext context = createContext();

        context.reportIssue(issue("like-leading-wildcard", "LIKE 以通配符开头", RuleLocation.of("sql-1", 1, 10)));
        context.reportIssue(issue("like-leading-wildcard", "LIKE 以通配符开头", RuleLocation.of("sql-1", 2, 10)));

        assertThat(context.getIssues()).hasSize(2);
        assertThat(context.getIssues())
                .extracting(issue -> issue.getLocation().getStartLine())
                .containsExactly(1, 2);
    }

    @Test
    @DisplayName("禁用规则的问题不应该进入去重集合或问题列表")
    void should_skip_disabled_rule_before_deduplication() {
        RuleConfig config = mock(RuleConfig.class);
        when(config.isRuleEnabled("select-star")).thenReturn(false);

        RuleContext context = RuleContext.builder()
                .sqlId("sql-1")
                .sql("SELECT * FROM users")
                .ruleConfig(config)
                .build();

        context.reportIssue(issue("select-star", "使用了 SELECT *", RuleLocation.of("sql-1", 1, 1)));
        context.reportIssue(issue("select-star", "使用了 SELECT *", RuleLocation.of("sql-1", 1, 1)));

        assertThat(context.getIssues()).isEmpty();
        assertThat(context.getIssueKeys()).isEmpty();
    }

    private RuleContext createContext() {
        return RuleContext.builder()
                .sqlId("sql-1")
                .sql("SELECT * FROM users")
                .build();
    }

    private RuleIssue issue(String ruleId, String message, RuleLocation location) {
        return RuleIssue.builder()
                .ruleId(ruleId)
                .severity(SeverityLevel.WARNING)
                .message(message)
                .location(location)
                .build();
    }
}
