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
 * 检测 INSERT 未指定列的规则
 * <p>
 * INSERT 未指定列名（INSERT INTO table VALUES ...）是脆弱的写法，
 * 表结构变化会导致查询失败
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@Slf4j
@Component
@RuleMeta(
        id = "insert-without-columns",
        name = "INSERT without column list",
        description = "INSERT 未指定列名，表结构变化会导致查询失败",
        type = RuleType.PROBLEM,
        severity = SeverityLevel.WARNING,
        tags = {"maintainability", "best-practice"},
        category = RuleCategory.BEST_PRACTICE
)
public class InsertWithoutColumnsRule implements SqlRule {

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

        // 检查 INSERT INTO ... VALUES ... 但没有列名列表
        // 正常应该是: INSERT INTO table (col1, col2) VALUES (...)
        // 问题模式: INSERT INTO table VALUES (...) (没有括号包围的列名)
        if (sql.startsWith("INSERT INTO") || sql.startsWith("INSERT   INTO")) {
            // 移除 INSERT INTO 部分后的内容
            String afterInsertInto = sql.replaceAll("(?i)^INSERT\\s+INTO\\s+(\\w+)", "");

            // 如果接下来直接是 VALUES 或 SELECT，说明没有列名
            if (afterInsertInto.trim().startsWith("VALUES") ||
                afterInsertInto.trim().startsWith("SELECT")) {
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
                .suggestion("明确指定列名：INSERT INTO table (col1, col2, ...) VALUES (...)")
                .build();

        context.reportIssue(issue);
        log.debug("Reported INSERT without columns for SQL: {}", context.getSqlId());
    }

    @Override
    public int getPriority() {
        return 38;
    }
}
