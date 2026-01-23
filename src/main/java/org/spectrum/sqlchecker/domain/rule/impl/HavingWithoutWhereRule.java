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
 * 检测 HAVING 而无 WHERE 的规则
 * <p>
 * 在可以过滤原始数据的情况下使用 HAVING 而不是 WHERE 会降低性能
 * <p>
 * 参考：
 * - https://www.navicat.com/en/company/aboutus/blog/what-is-the-difference-between-where-and-having-in-sql
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@Slf4j
@Component
@RuleMeta(
        id = "having-without-where",
        name = "HAVING without WHERE",
        description = "在可以过滤原始数据的情况下使用 HAVING 而不是 WHERE 会降低性能",
        type = RuleType.PROBLEM,
        severity = SeverityLevel.WARNING,
        tags = {"performance"},
        category = RuleCategory.PERFORMANCE
)
public class HavingWithoutWhereRule implements SqlRule {

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
     * 检查 HAVING 使用情况
     */
    private void checkSelect(PlainSelect select, RuleContext context) {
        // 有 GROUP BY 但没有 WHERE
        if (select.getGroupBy() != null && select.getWhere() == null) {
            // 检查 HAVING 子句内容
            String sql = context.getSql().toUpperCase();
            if (sql.contains("HAVING")) {
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
                .suggestion("考虑将可以在聚合前过滤的条件移到 WHERE 子句")
                .build();

        context.reportIssue(issue);
        log.debug("Reported HAVING without WHERE for SQL: {}", context.getSqlId());
    }

    @Override
    public int getPriority() {
        return 45;
    }
}
