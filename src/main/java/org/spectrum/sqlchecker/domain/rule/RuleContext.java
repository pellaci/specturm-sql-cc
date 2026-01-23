package org.spectrum.sqlchecker.domain.rule;

import lombok.Builder;
import lombok.Getter;
import net.sf.jsqlparser.statement.Statement;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则执行上下文
 * <p>
 * 在规则分析过程中传递信息，收集检测到的问题
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Getter
@Builder
public class RuleContext {

    /**
     * SQL ID
     */
    private final String sqlId;

    /**
     * 原始 SQL 语句
     */
    private final String sql;

    /**
     * 解析后的 Statement
     */
    private Statement statement;

    /**
     * 规则配置
     */
    private RuleConfig ruleConfig;

    /**
     * 收集的问题列表
     */
    @Builder.Default
    private final List<RuleIssue> issues = new ArrayList<>();

    /**
     * 解析错误信息列表
     */
    @Builder.Default
    private final List<String> parsingErrors = new ArrayList<>();

    /**
     * 报告问题
     * <p>
     * 规则通过此方法报告检测到的问题。如果规则被禁用或在例外列表中，问题将被忽略。
     *
     * @param issue 规则问题
     */
    public void reportIssue(RuleIssue issue) {
        if (ruleConfig != null && !ruleConfig.isRuleEnabled(issue.getRuleId())) {
            return;
        }

        if (ruleConfig != null && ruleConfig.isInExceptions(issue.getRuleId(), sql)) {
            return;
        }

        issues.add(issue);
    }

    /**
     * 添加解析错误
     *
     * @param error 错误信息
     */
    public void addParsingError(String error) {
        parsingErrors.add(error);
    }

    /**
     * 是否有解析错误
     *
     * @return 是否有解析错误
     */
    public boolean hasParsingErrors() {
        return !parsingErrors.isEmpty();
    }

    /**
     * 是否有问题
     *
     * @return 是否有问题
     */
    public boolean hasIssues() {
        return !issues.isEmpty();
    }

    /**
     * 获取问题数量
     *
     * @return 问题数量
     */
    public int getIssueCount() {
        return issues.size();
    }
}
