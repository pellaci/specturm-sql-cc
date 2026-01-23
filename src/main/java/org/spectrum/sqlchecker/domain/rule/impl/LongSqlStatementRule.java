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
 * 检测过长 SQL 语句的规则
 * <p>
 * 过长的 SQL 语句难以维护和调试，应该拆分为多个小查询或使用视图/CTE
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@Slf4j
@Component
@RuleMeta(
        id = "long-sql-statement",
        name = "Long SQL statement",
        description = "SQL 语句过长，难以维护和调试",
        type = RuleType.SUGGESTION,
        severity = SeverityLevel.INFO,
        tags = {"maintainability", "readability"},
        category = RuleCategory.MAINTAINABILITY
)
public class LongSqlStatementRule implements SqlRule {

    /**
     * SQL 长度阈值（字符数）
     */
    private static final int LENGTH_THRESHOLD = 500;

    /**
     * SQL 行数阈值
     */
    private static final int LINE_THRESHOLD = 20;

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
        String sql = context.getSql();

        int length = sql.length();
        int lines = sql.split("\n").length;

        // 检查长度
        if (length > LENGTH_THRESHOLD) {
            reportIssue(context, length, lines, "字符数超过 " + LENGTH_THRESHOLD);
        }
        // 检查行数
        else if (lines > LINE_THRESHOLD) {
            reportIssue(context, length, lines, "行数超过 " + LINE_THRESHOLD);
        }
    }

    /**
     * 报告问题
     */
    private void reportIssue(RuleContext context, int length, int lines, String reason) {
        RuleIssue issue = RuleIssue.builder()
                .ruleId(getMeta().id())
                .ruleName(getMeta().name())
                .severity(getMeta().severity())
                .location(RuleLocation.of(context.getSqlId(), 1))
                .message(getMeta().description() + " (" + reason + ": " + length + " 字符, " + lines + " 行)")
                .suggestion("考虑拆分为多个查询、使用视图或 CTE (WITH 子句)")
                .build();

        context.reportIssue(issue);
        log.debug("Reported long SQL for SQL: {}, length: {}, lines: {}", context.getSqlId(), length, lines);
    }

    @Override
    public int getPriority() {
        return 70;
    }
}
