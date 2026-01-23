package org.spectrum.sqlchecker.infrastructure.rule;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitor;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Table;
import org.spectrum.sqlchecker.domain.rule.RuleContext;
import org.spectrum.sqlchecker.domain.rule.SqlRule;

import java.util.List;

/**
 * 统一规则访问者
 * <p>
 * 实现 JSqlParser 的各种 Visitor 接口，在遍历 AST 时分发到注册的规则。
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Slf4j
public class RuleVisitor implements StatementVisitor, SelectVisitor, FromItemVisitor, ExpressionVisitor, SelectItemVisitor {

    private final RuleContext context;
    private final RuleRegistry registry;

    public RuleVisitor(RuleContext context, RuleRegistry registry) {
        this.context = context;
        this.registry = registry;
    }

    // ==================== Statement Visitor 方法 ====================

    @Override
    public void visit(Select select) {
        dispatchToRules(select);
        if (select.getSelectBody() != null) {
            select.getSelectBody().accept(this);
        }
    }

    // ==================== Select Visitor 方法 ====================

    @Override
    public void visit(PlainSelect plainSelect) {
        dispatchToRules(plainSelect);

        // 遍历 FROM
        if (plainSelect.getFromItem() != null) {
            plainSelect.getFromItem().accept(this);
        }

        // 遍历 JOIN
        if (plainSelect.getJoins() != null) {
            for (Object join : plainSelect.getJoins()) {
                dispatchToRules(join);
                if (join instanceof Join) {
                    Join j = (Join) join;
                    if (j.getRightItem() != null) {
                        j.getRightItem().accept(this);
                    }
                }
            }
        }

        // 遍历 WHERE
        if (plainSelect.getWhere() != null) {
            plainSelect.getWhere().accept(this);
        }

        // 遍历 SELECT 项
        if (plainSelect.getSelectItems() != null) {
            for (SelectItem item : plainSelect.getSelectItems()) {
                item.accept(this);
            }
        }

        // 遍历 LIMIT
        if (plainSelect.getLimit() != null) {
            dispatchToRules(plainSelect.getLimit());
        }

        // 遍历 ORDER BY
        if (plainSelect.getOrderByElements() != null) {
            for (OrderByElement element : plainSelect.getOrderByElements()) {
                dispatchToRules(element);
            }
        }
    }

    @Override
    public void visit(SetOperationList setOpList) {
        dispatchToRules(setOpList);
    }

    // ==================== FromItem Visitor 方法 ====================

    @Override
    public void visit(Table table) {
        dispatchToRules(table);
    }

    @Override
    public void visit(ParenthesedFromItem parenthesedFromItem) {
        dispatchToRules(parenthesedFromItem);
        if (parenthesedFromItem.getFromItem() != null) {
            parenthesedFromItem.getFromItem().accept(this);
        }
    }

    @Override
    public void visit(LateralSubSelect lateralSubSelect) {
        dispatchToRules(lateralSubSelect);
    }

    // ==================== Expression Visitor 方法（核心） ====================

    @Override
    public void visit(LikeExpression likeExpression) {
        dispatchToRules(likeExpression);
    }

    @Override
    public void visit(AndExpression andExpression) {
        dispatchToRules(andExpression);
        visitBinary(andExpression);
    }

    @Override
    public void visit(OrExpression orExpression) {
        dispatchToRules(orExpression);
        visitBinary(orExpression);
    }

    @Override
    public void visit(EqualsTo equalsTo) {
        dispatchToRules(equalsTo);
        visitBinary(equalsTo);
    }

    @Override
    public void visit(NotExpression notExpression) {
        dispatchToRules(notExpression);
        if (notExpression.getExpression() != null) {
            notExpression.getExpression().accept(this);
        }
    }

    @Override
    public void visit(InExpression inExpression) {
        dispatchToRules(inExpression);
    }

    @Override
    public void visit(IsNullExpression isNullExpression) {
        dispatchToRules(isNullExpression);
    }

    @Override
    public void visit(Between betweenExpression) {
        dispatchToRules(betweenExpression);
    }

    @Override
    public void visit(Function function) {
        dispatchToRules(function);
    }

    @Override
    public void visit(AllColumns allColumns) {
        dispatchToRules(allColumns);
    }

    @Override
    public void visit(AllTableColumns allTableColumns) {
        dispatchToRules(allTableColumns);
    }

    @Override
    public void visit(ExpressionList expressionList) {
        dispatchToRules(expressionList);
    }

    @Override
    public void visit(SelectItem selectItem) {
        dispatchToRules(selectItem);
    }

    @Override
    public void visit(Parenthesis parenthesis) {
        dispatchToRules(parenthesis);
        if (parenthesis.getExpression() != null) {
            parenthesis.getExpression().accept(this);
        }
    }

    @Override
    public void visit(StringValue stringValue) {
        dispatchToRules(stringValue);
    }

    @Override
    public void visit(LongValue longValue) {
        dispatchToRules(longValue);
    }

    @Override
    public void visit(DoubleValue doubleValue) {
        dispatchToRules(doubleValue);
    }

    @Override
    public void visit(DateValue dateValue) {
        dispatchToRules(dateValue);
    }

    @Override
    public void visit(TimestampValue timestampValue) {
        dispatchToRules(timestampValue);
    }

    @Override
    public void visit(TimeValue timeValue) {
        dispatchToRules(timeValue);
    }

    @Override
    public void visit(NullValue nullValue) {
        dispatchToRules(nullValue);
    }

    @Override
    public void visit(ExistsExpression existsExpression) {
        dispatchToRules(existsExpression);
    }

    @Override
    public void visit(CaseExpression caseExpression) {
        dispatchToRules(caseExpression);
    }

    @Override
    public void visit(WhenClause whenClause) {
        dispatchToRules(whenClause);
    }

    @Override
    public void visit(AnyComparisonExpression anyComparisonExpression) {
        dispatchToRules(anyComparisonExpression);
    }

    @Override
    public void visit(Concat concat) {
        dispatchToRules(concat);
    }

    @Override
    public void visit(Matches matches) {
        dispatchToRules(matches);
    }

    @Override
    public void visit(GreaterThan greaterThan) {
        dispatchToRules(greaterThan);
        visitBinary(greaterThan);
    }

    @Override
    public void visit(GreaterThanEquals greaterThanEquals) {
        dispatchToRules(greaterThanEquals);
        visitBinary(greaterThanEquals);
    }

    @Override
    public void visit(MinorThan minorThan) {
        dispatchToRules(minorThan);
        visitBinary(minorThan);
    }

    @Override
    public void visit(MinorThanEquals minorThanEquals) {
        dispatchToRules(minorThanEquals);
        visitBinary(minorThanEquals);
    }

    @Override
    public void visit(NotEqualsTo notEqualsTo) {
        dispatchToRules(notEqualsTo);
        visitBinary(notEqualsTo);
    }

    @Override
    public void visit(Addition addition) {
        dispatchToRules(addition);
    }

    @Override
    public void visit(Division division) {
        dispatchToRules(division);
    }

    @Override
    public void visit(Multiplication multiplication) {
        dispatchToRules(multiplication);
    }

    @Override
    public void visit(Subtraction subtraction) {
        dispatchToRules(subtraction);
    }

    @Override
    public void visit(Modulo modulo) {
        dispatchToRules(modulo);
    }

    @Override
    public void visit(BitwiseAnd bitwiseAnd) {
        dispatchToRules(bitwiseAnd);
    }

    @Override
    public void visit(BitwiseOr bitwiseOr) {
        dispatchToRules(bitwiseOr);
    }

    @Override
    public void visit(BitwiseXor bitwiseXor) {
        dispatchToRules(bitwiseXor);
    }

    @Override
    public void visit(SignedExpression signedExpression) {
        dispatchToRules(signedExpression);
    }

    @Override
    public void visit(JdbcParameter jdbcParameter) {
        dispatchToRules(jdbcParameter);
    }

    @Override
    public void visit(JdbcNamedParameter jdbcNamedParameter) {
        dispatchToRules(jdbcNamedParameter);
    }

    @Override
    public void visit(HexValue hexValue) {
        dispatchToRules(hexValue);
    }

    @Override
    public void visit(IntervalExpression intervalExpression) {
        dispatchToRules(intervalExpression);
    }

    @Override
    public void visit(CastExpression castExpression) {
        dispatchToRules(castExpression);
    }

    @Override
    public void visit(ArrayExpression arrayExpression) {
        dispatchToRules(arrayExpression);
    }

    @Override
    public void visit(IntegerDivision integerDivision) {
        dispatchToRules(integerDivision);
    }

    @Override
    public void visit(JsonExpression jsonExpression) {
        dispatchToRules(jsonExpression);
    }

    @Override
    public void visit(JsonFunction jsonFunction) {
        dispatchToRules(jsonFunction);
    }

    @Override
    public void visit(JsonAggregateFunction jsonAggregateFunction) {
        dispatchToRules(jsonAggregateFunction);
    }

    @Override
    public void visit(AnalyticExpression analyticExpression) {
        dispatchToRules(analyticExpression);
    }

    @Override
    public void visit(MySQLGroupConcat mySQLGroupConcat) {
        dispatchToRules(mySQLGroupConcat);
    }

    @Override
    public void visit(OracleHierarchicalExpression oracleHierarchicalExpression) {
        dispatchToRules(oracleHierarchicalExpression);
    }

    @Override
    public void visit(OracleHint oracleHint) {
        dispatchToRules(oracleHint);
    }

    @Override
    public void visit(TimezoneExpression timezoneExpression) {
        dispatchToRules(timezoneExpression);
    }

    @Override
    public void visit(KeepExpression keepExpression) {
        dispatchToRules(keepExpression);
    }

    @Override
    public void visit(Limit limit) {
        dispatchToRules(limit);
    }

    @Override
    public void visit(Offset offset) {
        dispatchToRules(offset);
    }

    @Override
    public void visit(Fetch fetch) {
        dispatchToRules(fetch);
    }

    @Override
    public void visit(RowConstructor rowConstructor) {
        dispatchToRules(rowConstructor);
    }

    @Override
    public void visit(IsBooleanExpression isBooleanExpression) {
        dispatchToRules(isBooleanExpression);
    }

    @Override
    public void visit(ExtractExpression extractExpression) {
        dispatchToRules(extractExpression);
    }

    @Override
    public void visit(LateralView lateralView) {
        dispatchToRules(lateralView);
    }

    @Override
    public void visit(Values values) {
        dispatchToRules(values);
    }

    @Override
    public void visit(First first) {
        dispatchToRules(first);
    }

    @Override
    public void visit(SimilarToExpression similarToExpression) {
        dispatchToRules(similarToExpression);
    }

    @Override
    public void visit(ArrayConstructor arrayConstructor) {
        dispatchToRules(arrayConstructor);
    }

    @Override
    public void visit(OverlapsCondition overlapsCondition) {
        dispatchToRules(overlapsCondition);
    }

    @Override
    public void visit(OracleNamedFunctionParameter oracleNamedFunctionParameter) {
        dispatchToRules(oracleNamedFunctionParameter);
    }

    @Override
    public void visit(TableFunction tableFunction) {
        dispatchToRules(tableFunction);
    }

    @Override
    public void visit(UserVariable userVariable) {
        dispatchToRules(userVariable);
    }

    @Override
    public void visit(NumericBind numericBind) {
        dispatchToRules(numericBind);
    }

    @Override
    public void visit(XMLSerializeExpr xmlSerializeExpr) {
        dispatchToRules(xmlSerializeExpr);
    }

    @Override
    public void visit(TimeKeyExpression timeKeyExpression) {
        dispatchToRules(timeKeyExpression);
    }

    @Override
    public void visit(RegExpMatchOperator regExpMatchOperator) {
        dispatchToRules(regExpMatchOperator);
    }

    @Override
    public void visit(JsonOperator jsonOperator) {
        dispatchToRules(jsonOperator);
    }

    @Override
    public void visit(OrderByClause orderByClause) {
        dispatchToRules(orderByClause);
    }

    @Override
    public void visit(PartitionByClause partitionByClause) {
        dispatchToRules(partitionByClause);
    }

    @Override
    public void visit(RowGetExpression rowGetExpression) {
        dispatchToRules(rowGetExpression);
    }

    @Override
    public void visit(NextValExpression nextValExpression) {
        dispatchToRules(nextValExpression);
    }

    @Override
    public void visit(CollateExpression collateExpression) {
        dispatchToRules(collateExpression);
    }

    @Override
    public void visit(ConnectByRootOperator connectByRootOperator) {
        dispatchToRules(connectByRootOperator);
    }

    // ==================== 辅助方法 ====================

    /**
     * 将节点分发给注册的规则
     */
    private void dispatchToRules(Object node) {
        if (node == null) {
            return;
        }

        List<SqlRule> rules = registry.getRulesForNode(node.getClass());

        for (SqlRule rule : rules) {
            try {
                rule.visit(node, context);
            } catch (Exception e) {
                log.warn("Rule {} threw exception while visiting {}: {}",
                        rule.getMeta().id(), node.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    /**
     * 处理二元表达式
     */
    private void visitBinary(BinaryExpression expr) {
        if (expr.getLeftExpression() != null) {
            expr.getLeftExpression().accept(this);
        }
        if (expr.getRightExpression() != null) {
            expr.getRightExpression().accept(this);
        }
    }
}
