package org.spectrum.sqlchecker.domain.rule.impl;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.OrderByElement;
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
 * 检测 ORDER BY 缺少 LIMIT 的规则
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Slf4j
@Component
@RuleMeta(
        id = "orderby-without-limit",
        name = "ORDER BY without LIMIT",
        description = "ORDER BY 缺少 LIMIT 可能导致大结果集排序",
        type = RuleType.PROBLEM,
        severity = SeverityLevel.WARNING,
        tags = {"performance"},
        category = RuleCategory.PERFORMANCE
)
public class OrderByWithoutLimitRule implements SqlRule {

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
        if (node instanceof PlainSelect select) {
            checkSelect(select, context);
        }
    }

    /**
     * 检查 SELECT 是否有 ORDER BY 但没有 LIMIT
     */
    private void checkSelect(PlainSelect select, RuleContext context) {
        // 没有 ORDER BY 则放过
        if (select.getOrderByElements() == null || select.getOrderByElements().isEmpty()) {
            return;
        }

        // 有 LIMIT 则放过
        if (select.getLimit() != null) {
            return;
        }

        // 检查是否是 ORDER BY 数字（如 ORDER BY 1）
        if (isOrderByPosition(select.getOrderByElements())) {
            return;
        }

        // 报告问题
        RuleIssue issue = RuleIssue.builder()
                .ruleId(getMeta().id())
                .ruleName(getMeta().name())
                .severity(getMeta().severity())
                .location(RuleLocation.of(context.getSqlId(), 1))
                .message(getMeta().description())
                .suggestion("添加 LIMIT 子句限制结果集数量")
                .build();

        context.reportIssue(issue);
        log.debug("Reported ORDER BY without LIMIT issue for SQL: {}", context.getSqlId());
    }

    /**
     * 检查是否是按位置排序（ORDER BY 1, 2）
     */
    private boolean isOrderByPosition(java.util.List<OrderByElement> orderByElements) {
        for (OrderByElement element : orderByElements) {
            if (element.getExpression() instanceof net.sf.jsqlparser.expression.LongValue) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getPriority() {
        return 25;
    }
}
