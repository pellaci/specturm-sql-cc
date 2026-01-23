package org.spectrum.sqlchecker.domain.rule.impl;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
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
 * 检测 IN 子查询的规则
 * <p>
 * IN 子查询通常可以改写为 JOIN 以获得更好的性能
 * <p>
 * 参考：
 * - https://www.baeldung.com/sql/in-vs-exists
 * - https://dev.mysql.com/doc/refman/8.3/en/subquery-optimization-with-exists.html
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@Slf4j
@Component
@RuleMeta(
        id = "in-subquery",
        name = "IN with subquery",
        description = "IN 子查询通常可以改写为 JOIN 以获得更好的性能",
        type = RuleType.SUGGESTION,
        severity = SeverityLevel.INFO,
        tags = {"performance", "subquery"},
        category = RuleCategory.PERFORMANCE
)
public class InSubqueryRule implements SqlRule {

    @Override
    public RuleMeta getMeta() {
        return getClass().getAnnotation(RuleMeta.class);
    }

    @Override
    public Set<Class<?>> supportedNodeTypes() {
        return Set.of(InExpression.class);
    }

    @Override
    public void visit(Object node, RuleContext context) {
        if (node instanceof InExpression inExpression) {
            checkInExpression(inExpression, context);
        }
    }

    /**
     * 检查 IN 表达式
     */
    private void checkInExpression(InExpression inExpression, RuleContext context) {
        // 检查右侧是否是子查询
        Object rightItem = inExpression.getRightExpression();

        // 如果是子查询（在 JSqlParser 4.7 中用 ParenthesedSelect 或类似表示）
        String rightStr = rightItem.toString();
        if (rightStr.toUpperCase().startsWith("SELECT")) {
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
                .suggestion("考虑改写为 INNER JOIN 或 EXISTS 子查询")
                .build();

        context.reportIssue(issue);
        log.debug("Reported IN subquery for SQL: {}", context.getSqlId());
    }

    @Override
    public int getPriority() {
        return 55;
    }
}
