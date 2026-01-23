package org.spectrum.sqlchecker.domain.rule.impl;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.statement.Statement;
import org.spectrum.sqlchecker.domain.rule.RuleContext;
import org.spectrum.sqlchecker.domain.rule.RuleIssue;
import org.spectrum.sqlchecker.domain.rule.RuleLocation;
import org.spectrum.sqlchecker.domain.rule.SqlRule;
import org.spectrum.sqlchecker.domain.rule.annotation.RuleMeta;
import org.spectrum.sqlchecker.domain.shared.enumeration.RuleCategory;
import org.spectrum.sqlchecker.domain.shared.enumeration.RuleType;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 检测魔法数字的规则
 * <p>
 * SQL 中的魔法数字（硬编码的数字常量）降低可读性，应该使用命名常量
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@Slf4j
@Component
@RuleMeta(
        id = "magic-numbers",
        name = "Magic numbers detected",
        description = "SQL 中包含魔法数字（硬编码的数字常量），降低可读性",
        type = RuleType.SUGGESTION,
        severity = SeverityLevel.INFO,
        tags = {"maintainability", "readability"},
        category = RuleCategory.MAINTAINABILITY
)
public class MagicNumbersRule implements SqlRule {

    /**
     * 魔法数字模式 - 匹配 = 或 > 或 < 等比较操作符后面的数字
     */
    private static final Pattern MAGIC_NUMBER_PATTERN = Pattern.compile(
            "[=<>!]\\s*\\b(\\d{2,})\\b"
    );

    @Override
    public RuleMeta getMeta() {
        return getClass().getAnnotation(RuleMeta.class);
    }

    @Override
    public Set<Class<?>> supportedNodeTypes() {
        return Set.of(Statement.class);
    }

    @Override
    public void visit(Object node, RuleContext context) {
        String sql = context.getSql();

        // 跳过 LIMIT/OFFSET 后的数字，这些通常是合理的
        String sqlWithoutLimit = sql.replaceAll("(?i)(LIMIT|OFFSET|TOP)\\s+\\d+", "");

        var matcher = MAGIC_NUMBER_PATTERN.matcher(sqlWithoutLimit);
        int count = 0;
        while (matcher.find()) {
            count++;
        }

        // 如果找到多个魔法数字
        if (count >= 2) {
            reportIssue(context, count);
        }
    }

    /**
     * 报告问题
     */
    private void reportIssue(RuleContext context, int count) {
        RuleIssue issue = RuleIssue.builder()
                .ruleId(getMeta().id())
                .ruleName(getMeta().name())
                .severity(getMeta().severity())
                .location(RuleLocation.of(context.getSqlId(), 1))
                .message(getMeta().description() + " (检测到 " + count + " 个魔法数字)")
                .suggestion("使用命名常量或参数代替硬编码数字")
                .build();

        context.reportIssue(issue);
        log.debug("Reported magic numbers for SQL: {}, count: {}", context.getSqlId(), count);
    }

    @Override
    public int getPriority() {
        return 75;
    }
}
