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

/**
 * 检测 NULL 比较问题的规则
 * <p>
 * 使用 = NULL 或 != NULL 是错误的，应该使用 IS NULL 和 IS NOT NULL
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@Slf4j
@Component
@RuleMeta(
        id = "null-comparison",
        name = "Incorrect NULL comparison",
        description = "使用 = NULL 或 != NULL 是错误的，应该使用 IS NULL 和 IS NOT NULL",
        type = RuleType.PROBLEM,
        severity = SeverityLevel.CRITICAL,
        tags = {"correctness", "null"},
        category = RuleCategory.BEST_PRACTICE
)
public class NullComparisonRule implements SqlRule {

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
        String upperSql = sql.toUpperCase();

        // 检测 = NULL
        if (upperSql.contains("= NULL") || upperSql.contains("=\\s+NULL")) {
            reportIssue(context, "= NULL");
        }

        // 检测 != NULL 或 <> NULL
        if (upperSql.contains("!= NULL") || upperSql.contains("<> NULL")) {
            reportIssue(context, "!= NULL / <> NULL");
        }
    }

    /**
     * 报告问题
     */
    private void reportIssue(RuleContext context, String pattern) {
        RuleIssue issue = RuleIssue.builder()
                .ruleId(getMeta().id())
                .ruleName(getMeta().name())
                .severity(getMeta().severity())
                .location(RuleLocation.of(context.getSqlId(), 1))
                .message(getMeta().description() + " (" + pattern + ")")
                .suggestion("使用 IS NULL 或 IS NOT NULL 代替 = NULL 或 != NULL")
                .build();

        context.reportIssue(issue);
        log.debug("Reported NULL comparison issue for SQL: {}, pattern: {}", context.getSqlId(), pattern);
    }

    @Override
    public int getPriority() {
        return 3; // 高优先级，这是逻辑错误
    }
}
