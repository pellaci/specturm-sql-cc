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
import java.util.regex.Pattern;

/**
 * 检测硬编码敏感信息的规则
 * <p>
 * SQL 中不应该包含硬编码的密码、密钥等敏感信息
 *
 * @author Spectrum SQL Checker
 * @since 2.1.0
 */
@Slf4j
@Component
@RuleMeta(
        id = "hardcoded-secrets",
        name = "Hardcoded secrets detected",
        description = "SQL 中包含疑似硬编码的敏感信息（密码、密钥等）",
        type = RuleType.PROBLEM,
        severity = SeverityLevel.CRITICAL,
        tags = {"security", "secrets"},
        category = RuleCategory.SECURITY
)
public class HardcodedSecretsRule implements SqlRule {

    /**
     * 疑似敏感信息的模式
     */
    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(password\\s*=\\s*['\"][^'\"]+['\"]|" +
            "pwd\\s*=\\s*['\"][^'\"]+['\"]|" +
            "secret\\s*=\\s*['\"][^'\"]+['\"]|" +
            "key\\s*=\\s*['\"][^'\"]+['\"]|" +
            "token\\s*=\\s*['\"][^'\"]+['\"]|" +
            "api[_-]?key\\s*=\\s*['\"][^'\"]+['\"])",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 常见密码字段名
     */
    private static final Pattern PASSWORD_FIELD_PATTERN = Pattern.compile(
            "(password|passwd|pwd)\\s*=\\s*['\"]",
            Pattern.CASE_INSENSITIVE
    );

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

        // 检查是否包含敏感信息模式
        if (SECRET_PATTERN.matcher(sql).find()) {
            reportIssue(context, "疑似敏感信息（password/key/token 等）");
        }

        // 检查是否在字符串值中有密码字段
        if (PASSWORD_FIELD_PATTERN.matcher(sql).find()) {
            // 检查等号后面是否有引号包裹的值（可能是密码）
            if (sql.matches("(?i).*(password|passwd|pwd)\\s*=\\s*['\"][^'\"]{4,}['\"].*")) {
                reportIssue(context, "疑似硬编码密码");
            }
        }
    }

    /**
     * 报告问题
     */
    private void reportIssue(RuleContext context, String detail) {
        RuleIssue issue = RuleIssue.builder()
                .ruleId(getMeta().id())
                .ruleName(getMeta().name())
                .severity(getMeta().severity())
                .location(RuleLocation.of(context.getSqlId(), 1))
                .message(getMeta().description() + " - " + detail)
                .suggestion("使用参数化查询或环境变量，不要在 SQL 中硬编码敏感信息")
                .build();

        context.reportIssue(issue);
        log.warn("Reported hardcoded secrets for SQL: {}, detail: {}", context.getSqlId(), detail);
    }

    @Override
    public int getPriority() {
        return 8;
    }
}
