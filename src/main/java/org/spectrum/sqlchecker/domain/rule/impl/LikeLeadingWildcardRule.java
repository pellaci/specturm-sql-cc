package org.spectrum.sqlchecker.domain.rule.impl;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
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
 * 检测 LIKE 以通配符开头的规则
 * <p>
 * 使用 AST 的 LikeExpression 节点精确检测
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Slf4j
@Component
@RuleMeta(
        id = "like-leading-wildcard",
        name = "Avoid LIKE with leading wildcard",
        description = "LIKE 以通配符开头无法使用索引，会导致全表扫描",
        type = RuleType.PROBLEM,
        severity = SeverityLevel.WARNING,
        tags = {"performance", "index"},
        category = RuleCategory.PERFORMANCE
)
public class LikeLeadingWildcardRule implements SqlRule {

    @Override
    public RuleMeta getMeta() {
        return getClass().getAnnotation(RuleMeta.class);
    }

    @Override
    public Set<Class<?>> supportedNodeTypes() {
        return Set.of(LikeExpression.class);
    }

    @Override
    public void visit(Object node, RuleContext context) {
        if (node instanceof LikeExpression likeExpr) {
            checkLikeExpression(likeExpr, context);
        }
    }

    /**
     * 检查 LIKE 表达式
     */
    private void checkLikeExpression(LikeExpression likeExpr, RuleContext context) {
        Expression right = likeExpr.getRightExpression();

        if (right instanceof StringValue stringValue) {
            String value = stringValue.getValue();

            // 检查是否以通配符开头（排除纯 "%" 的情况）
            if (value.startsWith("%") && value.length() > 1) {
                reportIssue(context);
            }
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
                .suggestion("考虑使用全文索引、反转索引或后缀索引")
                .build();

        context.reportIssue(issue);
        log.debug("Reported LIKE leading wildcard issue for SQL: {}", context.getSqlId());
    }

    @Override
    public int getPriority() {
        return 15;
    }
}
