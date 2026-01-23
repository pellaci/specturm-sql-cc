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
 * 检测多列 IN 的规则
 * <p>
 * 多列 IN 语法在某些数据库中不支持，可读性也较差
 * 应该改写为多个单列 IN 或 JOIN
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@Slf4j
@Component
@RuleMeta(
        id = "multi-column-in",
        name = "Multi-column IN detected",
        description = "多列 IN 语法可读性差，建议改写为 JOIN 或多个单列 IN",
        type = RuleType.SUGGESTION,
        severity = SeverityLevel.INFO,
        tags = {"readability", "portability"},
        category = RuleCategory.BEST_PRACTICE
)
public class MultiColumnInRule implements SqlRule {

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
        String sql = context.getSql().toUpperCase();

        // 检测 (col1, col2) IN (...) 模式
        // 这是一个简化的检测
        if (sql.contains(") IN (SELECT") || sql.contains(") IN\\s+(SELECT")) {
            // 检查是否有逗号在括号内，表示多列
            String beforeIn = sql.substring(0, sql.indexOf(" IN "));
            if (beforeIn.contains("(") && beforeIn.substring(beforeIn.lastIndexOf("(")).contains(",")) {
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
                .suggestion("考虑改写为 JOIN 或使用多个单列 IN 条件")
                .build();

        context.reportIssue(issue);
        log.debug("Reported multi-column IN for SQL: {}", context.getSqlId());
    }

    @Override
    public int getPriority() {
        return 65;
    }
}
