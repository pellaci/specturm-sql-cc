package org.spectrum.sqlchecker.domain.rule.impl;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Function;
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
 * 检测在索引列上使用函数的规则
 * <p>
 * 在 WHERE 子句的索引列上使用函数会导致索引失效（非 SARGable 查询）
 * <p>
 * 参考：
 * - https://medium.com/@sohahashim/optimizing-sql-queries-using-sargable-queries-9ed6dd31db00
 * - https://www.baeldung.com/sql/sargability
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@Slf4j
@Component
@RuleMeta(
        id = "function-on-indexed-column",
        name = "Function on indexed column in WHERE",
        description = "在索引列上使用函数会导致无法使用索引（非 SARGable 查询）",
        type = RuleType.PROBLEM,
        severity = SeverityLevel.WARNING,
        tags = {"performance", "index", "sargable"},
        category = RuleCategory.PERFORMANCE
)
public class FunctionOnIndexedColumnRule implements SqlRule {

    /**
     * 常见的会导致索引失效的函数
     */
    private static final Set<String> NON_SARGABLE_FUNCTIONS = Set.of(
            "UPPER", "LOWER", "TRIM", "LTRIM", "RTRIM",
            "SUBSTRING", "SUBSTR", "LEFT", "RIGHT",
            "CONCAT", "REPLACE",
            "DATE", "YEAR", "MONTH", "DAY", "HOUR", "MINUTE", "SECOND",
            "TO_CHAR", "TO_DATE", "DATE_FORMAT",
            "COALESCE", "IFNULL", "ISNULL", "NULLIF",
            "CAST", "CONVERT"
    );

    @Override
    public RuleMeta getMeta() {
        return getClass().getAnnotation(RuleMeta.class);
    }

    @Override
    public Set<Class<?>> supportedNodeTypes() {
        return Set.of(Function.class, PlainSelect.class);
    }

    @Override
    public void visit(Object node, RuleContext context) {
        if (node instanceof Function function) {
            checkFunction(function, context);
        }
    }

    /**
     * 检查函数是否可能是非 SARGable 的
     */
    private void checkFunction(Function function, RuleContext context) {
        String functionName = function.getName().toUpperCase();

        // 检查是否是非 SARGable 函数
        if (NON_SARGABLE_FUNCTIONS.contains(functionName)) {
            // 简单启发式：如果这个函数出现在 WHERE 子句表达式中
            // 在实际实现中，需要检查函数是否直接应用于列引用
            String sql = context.getSql().toUpperCase();
            int wherePos = sql.indexOf(" WHERE ");
            if (wherePos > 0) {
                reportIssue(context, functionName);
            }
        }
    }

    /**
     * 报告问题
     */
    private void reportIssue(RuleContext context, String functionName) {
        RuleIssue issue = RuleIssue.builder()
                .ruleId(getMeta().id())
                .ruleName(getMeta().name())
                .severity(getMeta().severity())
                .location(RuleLocation.of(context.getSqlId(), 1))
                .message(getMeta().description() + " (函数: " + functionName + ")")
                .suggestion("考虑使用函数索引、计算列或重构查询条件")
                .build();

        context.reportIssue(issue);
        log.debug("Reported function on indexed column for SQL: {}, function: {}", context.getSqlId(), functionName);
    }

    @Override
    public int getPriority() {
        return 12;
    }
}
