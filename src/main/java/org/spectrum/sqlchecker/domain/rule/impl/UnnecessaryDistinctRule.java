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
 * 检测 DISTINCT 的规则
 * <p>
 * 过度使用 DISTINCT 可能掩盖表设计问题或 JOIN 条件缺失
 * <p>
 * 参考：
 * - https://www.navicat.com/en/company/aboutus/blog/what-does-distinct-do-in-sql
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@Slf4j
@Component
@RuleMeta(
        id = "unnecessary-distinct",
        name = "Unnecessary DISTINCT",
        description = "过度使用 DISTINCT 可能掩盖表设计问题或 JOIN 条件缺失",
        type = RuleType.SUGGESTION,
        severity = SeverityLevel.INFO,
        tags = {"performance", "readability"},
        category = RuleCategory.PERFORMANCE
)
public class UnnecessaryDistinctRule implements SqlRule {

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
     * 检查 SELECT 是否有 DISTINCT
     */
    private void checkSelect(PlainSelect select, RuleContext context) {
        // 检查是否有 DISTINCT
        if (select.getDistinct() == null) {
            return;
        }

        // 检查是否有 JOIN - DISTINCT + JOIN 可能表示 JOIN 条件不完整
        boolean hasJoin = select.getJoins() != null && !select.getJoins().isEmpty();
        // 检查是否有 GROUP BY - DISTINCT + GROUP BY 通常是冗余的
        boolean hasGroupBy = select.getGroupBy() != null;

        if (hasJoin || hasGroupBy) {
            reportIssue(context, hasJoin, hasGroupBy);
        }
    }

    /**
     * 报告问题
     */
    private void reportIssue(RuleContext context, boolean hasJoin, boolean hasGroupBy) {
        String reason = hasJoin ? "与 JOIN 一起使用" : (hasGroupBy ? "与 GROUP BY 一起使用" : "");
        RuleIssue issue = RuleIssue.builder()
                .ruleId(getMeta().id())
                .ruleName(getMeta().name())
                .severity(getMeta().severity())
                .location(RuleLocation.of(context.getSqlId(), 1))
                .message(getMeta().description() + " - " + reason)
                .suggestion(hasJoin ? "检查 JOIN 条件是否完整" : "DISTINCT 与 GROUP BY 冗余，移除其中一个")
                .build();

        context.reportIssue(issue);
        log.debug("Reported unnecessary DISTINCT for SQL: {}", context.getSqlId());
    }

    @Override
    public int getPriority() {
        return 60;
    }
}
