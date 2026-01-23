package org.spectrum.sqlchecker.domain.rule.impl;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
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
 * 检测多列 OR 条件的规则
 * <p>
 * 在不同列上使用 OR 可能导致索引失效，考虑使用 UNION 重写
 * <p>
 * 参考：
 * - https://www.cybertec-postgresql.com/en/avoid-or-for-better-query-performance/
 * - https://medium.com/@washimkarpankaj1/why-or-conditions-can-hurt-sql-performance-and-what-to-do-instead-31ab2d18bf25
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@Slf4j
@Component
@RuleMeta(
        id = "multi-column-or",
        name = "Multi-column OR condition",
        description = "在不同列上使用 OR 条件可能导致索引失效",
        type = RuleType.PROBLEM,
        severity = SeverityLevel.WARNING,
        tags = {"performance", "index"},
        category = RuleCategory.PERFORMANCE
)
public class MultiColumnOrRule implements SqlRule {

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
     * 检查 SELECT 中的 OR 条件
     */
    private void checkSelect(PlainSelect select, RuleContext context) {
        if (select.getWhere() == null) {
            return;
        }

        // 简化检测：统计 WHERE 子句中的 OR 数量
        String whereClause = select.getWhere().toString();
        int orCount = countOrOccurrences(whereClause);

        // 如果有多个 OR，可能是多列 OR
        if (orCount > 0) {
            // 进一步检查是否是不同列的 OR
            String sql = context.getSql().toUpperCase();

            // 简单启发式：如果 OR 之间有不同的列名
            if (hasMultiColumnOr(sql, whereClause)) {
                reportIssue(context, orCount);
            }
        }
    }

    /**
     * 统计 OR 出现次数
     */
    private int countOrOccurrences(String whereClause) {
        return whereClause.split(" OR ", -1).length - 1;
    }

    /**
     * 检查是否是多列 OR
     */
    private boolean hasMultiColumnOr(String sql, String whereClause) {
        // 简化的启发式检测
        // 如果 OR 连接的条件中有不同的列引用模式
        String upperWhere = whereClause.toUpperCase();

        // 查找形如 "col1 = ... OR col2 = ..." 的模式
        // 这里做简化处理
        return upperWhere.contains(" OR ") &&
               !upperWhere.matches(".*(\\w+)\\s*=.*OR\\s+\\1\\s*=.*");
    }

    /**
     * 报告问题
     */
    private void reportIssue(RuleContext context, int orCount) {
        RuleIssue issue = RuleIssue.builder()
                .ruleId(getMeta().id())
                .ruleName(getMeta().name())
                .severity(getMeta().severity())
                .location(RuleLocation.of(context.getSqlId(), 1))
                .message(getMeta().description() + " (检测到 " + orCount + " 个 OR 条件)")
                .suggestion("考虑使用 UNION ALL 或索引优化来提升性能")
                .build();

        context.reportIssue(issue);
        log.debug("Reported multi-column OR for SQL: {}, count: {}", context.getSqlId(), orCount);
    }

    @Override
    public int getPriority() {
        return 35;
    }
}
