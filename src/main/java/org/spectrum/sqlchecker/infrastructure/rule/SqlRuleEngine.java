package org.spectrum.sqlchecker.infrastructure.rule;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.spectrum.sqlchecker.domain.rule.RuleConfig;
import org.spectrum.sqlchecker.domain.rule.RuleContext;
import org.spectrum.sqlchecker.domain.rule.RuleIssue;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SQL 规则引擎
 * <p>
 * 使用 JSqlParser 解析 SQL 并通过 RuleVisitor 遍历 AST，
 * 将检测任务分发给注册的规则。
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Slf4j
@Component
public class SqlRuleEngine {

    private final RuleRegistry ruleRegistry;

    /**
     * 规则配置
     */
    private RuleConfig ruleConfig;

    public SqlRuleEngine(RuleRegistry ruleRegistry) {
        this.ruleRegistry = ruleRegistry;
    }

    /**
     * 设置规则配置
     *
     * @param ruleConfig 规则配置
     */
    public void setRuleConfig(RuleConfig ruleConfig) {
        this.ruleConfig = ruleConfig;
        this.ruleRegistry.setRuleConfig(ruleConfig);
    }

    /**
     * 分析 SQL 并返回问题列表
     *
     * @param sqlId SQL 标识符
     * @param sql   SQL 语句
     * @return 检测到的问题列表
     */
    public List<RuleIssue> analyze(String sqlId, String sql) {
        if (sql == null || sql.isBlank()) {
            log.warn("Empty SQL provided for analysis: {}", sqlId);
            return List.of();
        }

        RuleContext context = RuleContext.builder()
                .sqlId(sqlId)
                .sql(sql.trim())
                .ruleConfig(ruleConfig)
                .build();

        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            context.setStatement(stmt);

            // 创建规则访问者并遍历 AST
            RuleVisitor visitor = new RuleVisitor(context, ruleRegistry);
            stmt.accept(visitor);

        } catch (JSQLParserException e) {
            String error = String.format("Failed to parse SQL: %s - %s", sqlId, e.getMessage());
            log.debug(error);
            context.addParsingError(error);
        }

        return Collections.unmodifiableList(context.getIssues());
    }

    /**
     * 分析 SQL 并返回问题列表（自动生成 ID）
     *
     * @param sql SQL 语句
     * @return 检测到的问题列表
     */
    public List<RuleIssue> analyze(String sql) {
        String sqlId = UUID.randomUUID().toString().substring(0, 8);
        return analyze(sqlId, sql);
    }

    /**
     * 批量分析 SQL
     *
     * @param sqls SQL 映射（ID -> SQL）
     * @return 分析结果映射（ID -> 问题列表）
     */
    public Map<String, List<RuleIssue>> analyzeBatch(Map<String, String> sqls) {
        if (sqls == null || sqls.isEmpty()) {
            return Map.of();
        }

        Map<String, List<RuleIssue>> results = new ConcurrentHashMap<>();

        sqls.entrySet().parallelStream().forEach(entry -> {
            String sqlId = entry.getKey();
            String sql = entry.getValue();
            results.put(sqlId, analyze(sqlId, sql));
        });

        return results;
    }

    /**
     * 批量分析 SQL 列表（自动生成 ID）
     *
     * @param sqlList SQL 列表
     * @return 分析结果映射（ID -> 问题列表）
     */
    public Map<String, List<RuleIssue>> analyzeBatch(List<String> sqlList) {
        if (sqlList == null || sqlList.isEmpty()) {
            return Map.of();
        }

        AtomicInteger index = new AtomicInteger(0);
        Map<String, String> sqlMap = new ConcurrentHashMap<>();

        sqlList.forEach(sql -> {
            String sqlId = "sql-" + index.getAndIncrement();
            sqlMap.put(sqlId, sql);
        });

        return analyzeBatch(sqlMap);
    }

    /**
     * 获取规则注册表
     *
     * @return 规则注册表
     */
    public RuleRegistry getRuleRegistry() {
        return ruleRegistry;
    }

    /**
     * 获取已注册规则的数量
     *
     * @return 规则数量
     */
    public int getRegisteredRuleCount() {
        return ruleRegistry.getRuleCount();
    }
}
