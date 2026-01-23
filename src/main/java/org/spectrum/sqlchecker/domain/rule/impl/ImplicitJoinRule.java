package org.spectrum.sqlchecker.domain.rule.impl;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.schema.Table;
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
 * 检测隐式 JOIN 的规则
 * <p>
 * 检测 FROM 子句中使用逗号连接多个表的情况，建议使用显式 JOIN
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Slf4j
@Component
@RuleMeta(
        id = "implicit-join",
        name = "Implicit JOIN detected",
        description = "检测到隐式 JOIN (逗号连接表)，建议使用显式 JOIN",
        type = RuleType.PROBLEM,
        severity = SeverityLevel.WARNING,
        tags = {"readability", "best-practice"},
        category = RuleCategory.BEST_PRACTICE
)
public class ImplicitJoinRule implements SqlRule {

    private static final int MULTI_TABLE_THRESHOLD = 1;

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
     * 检查 SELECT 是否有隐式 JOIN
     */
    private void checkSelect(PlainSelect select, RuleContext context) {
        // 有显式 JOIN 则放过
        if (select.getJoins() != null && !select.getJoins().isEmpty()) {
            return;
        }

        // 检查 FROM 项是否是多表逗号连接
        if (!(select.getFromItem() instanceof Table)) {
            return;
        }

        // 检查是否有其他表（通过逗号连接）
        // 注意：JSqlParser 4.7 中，逗号连接的表会被解析为多个 JOIN 项
        // 但类型是 INNER JOIN 且没有 ON 条件
        if (select.getJoins() != null) {
            for (Object joinObj : select.getJoins()) {
                if (joinObj instanceof net.sf.jsqlparser.statement.select.Join join) {
                    // 检查是否是没有 ON 条件的 JOIN（即隐式 JOIN）
                    if (join.isSimple() && join.getOnExpression() == null) {
                        reportIssue(context);
                        return;
                    }
                }
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
                .suggestion("使用显式 JOIN ... ON 语法，提高可读性并避免笛卡尔积")
                .build();

        context.reportIssue(issue);
        log.debug("Reported implicit JOIN issue for SQL: {}", context.getSqlId());
    }

    @Override
    public int getPriority() {
        return 30;
    }
}
