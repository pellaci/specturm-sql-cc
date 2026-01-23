package org.spectrum.sqlchecker.domain.rule.impl;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
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
 * 检测 NOT IN 的规则
 * <p>
 * NOT IN 对 NULL 值处理有问题，且性能通常不如 NOT EXISTS
 * <p>
 * 参考：
 * - https://blog.sqlora.com/en/not-in-vs-not-exists-the-negation-battle-joelkallmanday/
 * - https://www.baeldung.com/sql/in-vs-exists
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@Slf4j
@Component
@RuleMeta(
        id = "not-in",
        name = "NOT IN with potential NULL issue",
        description = "NOT IN 对 NULL 值处理有问题，且性能通常不如 NOT EXISTS",
        type = RuleType.PROBLEM,
        severity = SeverityLevel.WARNING,
        tags = {"performance", "correctness", "null"},
        category = RuleCategory.PERFORMANCE
)
public class NotInRule implements SqlRule {

    @Override
    public RuleMeta getMeta() {
        return getClass().getAnnotation(RuleMeta.class);
    }

    @Override
    public Set<Class<?>> supportedNodeTypes() {
        return Set.of(NotExpression.class);
    }

    @Override
    public void visit(Object node, RuleContext context) {
        if (node instanceof NotExpression notExpression) {
            checkNotExpression(notExpression, context);
        }
    }

    /**
     * 检查 NOT 表达式
     */
    private void checkNotExpression(NotExpression notExpression, RuleContext context) {
        // 检查是否是 NOT IN
        String exprStr = notExpression.toString().toUpperCase();
        if (exprStr.contains("NOT IN")) {
            reportIssue(context);
        }
    }

    /**
     * 报告问题
     */
    private void reportIssue(RuleContext context) {
        RuleIssue issue = RuleIssue.builder()
                .ruleId(getMeta().id())
                .ruleName(getMeta().name())
                .severity(getMeta().severity())
                .location(RuleLocation.of(context.getSqlId(), 1))
                .message(getMeta().description())
                .suggestion("考虑改写为 NOT EXISTS 或 LEFT JOIN ... WHERE ... IS NULL")
                .build();

        context.reportIssue(issue);
        log.debug("Reported NOT IN for SQL: {}", context.getSqlId());
    }

    @Override
    public int getPriority() {
        return 40;
    }
}
