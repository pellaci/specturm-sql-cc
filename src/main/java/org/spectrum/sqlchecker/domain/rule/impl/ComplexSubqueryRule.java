package org.spectrum.sqlchecker.domain.rule.impl;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.statement.select.PlainSelect;
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
 * 检测复杂子查询的规则
 * <p>
 * 通过统计 SQL 中的括号嵌套深度来检测复杂查询
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Slf4j
@Component
@RuleMeta(
        id = "complex-subquery",
        name = "Complex subquery detected",
        description = "检测到复杂的嵌套结构，建议拆分或使用 CTE",
        type = RuleType.SUGGESTION,
        severity = SeverityLevel.INFO,
        tags = {"maintainability", "readability"},
        category = RuleCategory.MAINTAINABILITY
)
public class ComplexSubqueryRule implements SqlRule {

    /**
     * 括号嵌套深度阈值
     */
    private static final int NESTING_THRESHOLD = 3;

    @Override
    public RuleMeta getMeta() {
        return getClass().getAnnotation(RuleMeta.class);
    }

    @Override
    public Set<Class<?>> supportedNodeTypes() {
        return Set.of(PlainSelect.class);
    }

    @Override
    public void visit(Object node, RuleContext context) {
        if (node instanceof PlainSelect) {
            int depth = calculateNestingDepth(context.getSql());
            if (depth > NESTING_THRESHOLD) {
                reportIssue(context, depth);
            }
        }
    }

    /**
     * 计算括号嵌套深度
     */
    private int calculateNestingDepth(String sql) {
        int maxDepth = 0;
        int currentDepth = 0;

        for (char c : sql.toCharArray()) {
            if (c == '(') {
                currentDepth++;
                maxDepth = Math.max(maxDepth, currentDepth);
            } else if (c == ')') {
                currentDepth--;
            }
        }

        return maxDepth;
    }

    /**
     * 报告问题
     */
    private void reportIssue(RuleContext context, int depth) {
        RuleIssue issue = RuleIssue.builder()
                .ruleId(getMeta().id())
                .ruleName(getMeta().name())
                .severity(getMeta().severity())
                .location(RuleLocation.of(context.getSqlId(), 1))
                .message(getMeta().description() + " (嵌套深度: " + depth + ")")
                .suggestion("考虑使用 CTE (WITH 子句) 或拆分为多个查询")
                .build();

        context.reportIssue(issue);
        log.debug("Reported complex subquery issue for SQL: {}, depth: {}", context.getSqlId(), depth);
    }

    @Override
    public int getPriority() {
        return 50;
    }
}
