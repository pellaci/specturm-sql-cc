package org.spectrum.sqlchecker.domain.rule.impl;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.statement.select.AllColumns;
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
 * 检测 SELECT * 规则
 * <p>
 * 使用 AST 精准检测，避免字符串匹配的误报
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Slf4j
@Component
@RuleMeta(
        id = "select-star",
        name = "Avoid SELECT *",
        description = "使用 SELECT * 会查询所有列，可能造成不必要的 I/O 和网络开销",
        type = RuleType.PROBLEM,
        severity = SeverityLevel.CRITICAL,
        tags = {"performance", "readability"},
        category = RuleCategory.PERFORMANCE
)
public class SelectStarRule implements SqlRule {

    @Override
    public RuleMeta getMeta() {
        return getClass().getAnnotation(RuleMeta.class);
    }

    @Override
    public Set<Class<?>> supportedNodeTypes() {
        return Set.of(PlainSelect.class, AllColumns.class);
    }

    @Override
    public void visit(Object node, RuleContext context) {
        if (node instanceof PlainSelect select) {
            checkSelect(select, context);
        } else if (node instanceof AllColumns) {
            reportAllColumns(context);
        }
    }

    /**
     * 检查 PlainSelect 是否包含 SELECT *
     */
    private void checkSelect(PlainSelect select, RuleContext context) {
        if (select.getSelectItems() == null) {
            return;
        }

        // 检查是否包含 AllColumns (即 *)
        boolean hasStar = select.getSelectItems().stream()
                .anyMatch(AllColumns.class::isInstance);

        if (hasStar) {
            reportAllColumns(context);
        }
    }

    /**
     * 报告 SELECT * 问题
     */
    private void reportAllColumns(RuleContext context) {
        RuleIssue issue = RuleIssue.builder()
                .ruleId(getMeta().id())
                .ruleName(getMeta().name())
                .severity(getMeta().severity())
                .location(RuleLocation.of(context.getSqlId(), 1))
                .message(getMeta().description())
                .suggestion("明确列出需要的列名，减少数据传输")
                .build();

        context.reportIssue(issue);
        log.debug("Reported SELECT * issue for SQL: {}", context.getSqlId());
    }

    @Override
    public int getPriority() {
        return 10; // 高优先级
    }
}
