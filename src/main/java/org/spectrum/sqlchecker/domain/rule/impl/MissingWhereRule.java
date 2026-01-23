package org.spectrum.sqlchecker.domain.rule.impl;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.statement.select.Limit;
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
 * 检测缺少 WHERE 的 SELECT 规则
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Slf4j
@Component
@RuleMeta(
        id = "missing-where",
        name = "SELECT without WHERE clause",
        description = "SELECT 语句缺少 WHERE 条件可能导致全表扫描",
        type = RuleType.PROBLEM,
        severity = SeverityLevel.WARNING,
        tags = {"performance"},
        category = RuleCategory.PERFORMANCE
)
public class MissingWhereRule implements SqlRule {

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
     * 检查 SELECT 是否缺少 WHERE
     */
    private void checkSelect(PlainSelect select, RuleContext context) {
        // 有 LIMIT 则放过
        Limit limit = select.getLimit();
        if (limit != null) {
            return;
        }

        // 有 WHERE 则放过
        if (select.getWhere() != null) {
            return;
        }

        // 报告问题
        RuleIssue issue = RuleIssue.builder()
                .ruleId(getMeta().id())
                .ruleName(getMeta().name())
                .severity(getMeta().severity())
                .location(RuleLocation.of(context.getSqlId(), 1))
                .message(getMeta().description())
                .suggestion("添加 WHERE 条件或使用 LIMIT 限制结果集")
                .build();

        context.reportIssue(issue);
        log.debug("Reported missing WHERE issue for SQL: {}", context.getSqlId());
    }

    @Override
    public int getPriority() {
        return 20;
    }
}
