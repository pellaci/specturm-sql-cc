package org.spectrum.sqlchecker.domain.rule.impl;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.statement.Statement;
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
 * 检测 DELETE/UPDATE 无 WHERE 的规则
 * <p>
 * DELETE 或 UPDATE 语句缺少 WHERE 条件会影响所有行
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@Slf4j
@Component
@RuleMeta(
        id = "delete-update-no-where",
        name = "DELETE/UPDATE without WHERE",
        description = "DELETE 或 UPDATE 语句缺少 WHERE 条件会影响所有行",
        type = RuleType.PROBLEM,
        severity = SeverityLevel.CRITICAL,
        tags = {"dangerous", "correctness"},
        category = RuleCategory.BEST_PRACTICE
)
public class DeleteUpdateNoWhereRule implements SqlRule {

    @Override
    public RuleMeta getMeta() {
        return getClass().getAnnotation(RuleMeta.class);
    }

    @Override
    public Set<Class<?>> supportedNodeTypes() {
        return Set.of(Statement.class);
    }

    @Override
    public void visit(Object node, RuleContext context) {
        String sql = context.getSql().toUpperCase().trim();

        // 检查 DELETE
        if (sql.startsWith("DELETE FROM") || sql.startsWith("DELETE   FROM")) {
            // 检查是否有 WHERE
            if (!sql.contains(" WHERE ")) {
                reportIssue(context, "DELETE");
            }
        }

        // 检查 UPDATE
        if (sql.startsWith("UPDATE ")) {
            // 检查是否有 WHERE
            if (!sql.contains(" WHERE ")) {
                reportIssue(context, "UPDATE");
            }
        }
    }

    /**
     * 报告问题
     */
    private void reportIssue(RuleContext context, String operation) {
        RuleIssue issue = RuleIssue.builder()
                .ruleId(getMeta().id())
                .ruleName(getMeta().name())
                .severity(getMeta().severity())
                .location(RuleLocation.of(context.getSqlId(), 1))
                .message(operation + " 语句缺少 WHERE 条件，将影响所有行！")
                .suggestion("添加 WHERE 条件或使用 LIMIT 限制影响的行数")
                .build();

        context.reportIssue(issue);
        log.warn("Reported {} without WHERE for SQL: {}", operation, context.getSqlId());
    }

    @Override
    public int getPriority() {
        return 2; // 极高优先级，可能导致灾难性后果
    }
}
