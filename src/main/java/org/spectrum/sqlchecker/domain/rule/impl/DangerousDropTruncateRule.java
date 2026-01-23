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
 * 检测 DROP/TRUNCATE 表的规则
 * <p>
 * DROP TABLE 和 TRUNCATE TABLE 是危险操作，应该谨慎使用
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@Slf4j
@Component
@RuleMeta(
        id = "dangerous-drop-truncate",
        name = "DROP/TRUNCATE TABLE detected",
        description = "DROP TABLE 和 TRUNCATE TABLE 是危险操作，会导致数据丢失",
        type = RuleType.PROBLEM,
        severity = SeverityLevel.CRITICAL,
        tags = {"security", "dangerous"},
        category = RuleCategory.SECURITY
)
public class DangerousDropTruncateRule implements SqlRule {

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
        String sqlTrimmed = sql.trim();

        // 检测 DROP TABLE
        if (sqlTrimmed.startsWith("DROP TABLE") ||
            sqlTrimmed.startsWith("DROP   TABLE") ||
            sqlTrimmed.contains("DROP TABLE")) {
            reportIssue(context, "DROP TABLE");
        }

        // 检测 TRUNCATE TABLE
        if (sqlTrimmed.startsWith("TRUNCATE TABLE") ||
            sqlTrimmed.startsWith("TRUNCATE   TABLE") ||
            sqlTrimmed.contains("TRUNCATE TABLE")) {
            reportIssue(context, "TRUNCATE TABLE");
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
                .message("检测到危险操作: " + operation)
                .suggestion("确保这是预期的操作，考虑使用事务保护")
                .build();

        context.reportIssue(issue);
        log.warn("Reported dangerous operation {} for SQL: {}", operation, context.getSqlId());
    }

    @Override
    public int getPriority() {
        return 5; // 最高优先级
    }
}
