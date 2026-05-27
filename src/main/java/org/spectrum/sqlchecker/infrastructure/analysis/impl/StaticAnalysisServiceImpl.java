package org.spectrum.sqlchecker.infrastructure.analysis.impl;

import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.application.analysis.StaticAnalysisService;
import org.spectrum.sqlchecker.application.analysis.dto.StaticAnalysisDto;
import org.spectrum.sqlchecker.application.analysis.dto.StaticIssue;
import org.spectrum.sqlchecker.domain.shared.enumeration.IssueType;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;
import org.spectrum.sqlchecker.domain.shared.exception.AnalysisException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 静态分析服务实现
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Slf4j
@Service
public class StaticAnalysisServiceImpl implements StaticAnalysisService {

    @Override
    public StaticAnalysisDto analyze(String sqlId, String sql) {
        List<StaticIssue> issues = new ArrayList<>();
        int score = 100;

        // 简单规则检查
        String upperSql = sql.toUpperCase();

        // 检查 SELECT *
        if (upperSql.contains("SELECT *")) {
            issues.add(StaticIssue.builder()
                    .type(IssueType.SELECT_STAR)
                    .severity(SeverityLevel.WARNING)
                    .message("使用了 SELECT *，可能导致不必要的列查询")
                    .suggestion("明确列出需要的列名")
                    .build());
            score -= 10;
        }

        // 检查没有 WHERE 子句
        if (upperSql.contains("SELECT") && !upperSql.contains("WHERE") && !upperSql.contains("LIMIT")) {
            issues.add(StaticIssue.builder()
                    .type(IssueType.SELECT_WITHOUT_WHERE)
                    .severity(SeverityLevel.WARNING)
                    .message("缺少 WHERE 子句可能导致全表扫描")
                    .suggestion("添加 WHERE 条件或 LIMIT 限制")
                    .build());
            score -= 20;
        }

        // 检查 LIKE 以通配符开头
        if (upperSql.matches(".*LIKE\\s+['\"]%.*")) {
            issues.add(StaticIssue.builder()
                    .type(IssueType.LIKE_LEADING_WILDCARD)
                    .severity(SeverityLevel.CRITICAL)
                    .message("LIKE 以通配符开头无法使用索引")
                    .suggestion("考虑使用全文索引或反向索引")
                    .build());
            score -= 30;
        }

        return StaticAnalysisDto.builder()
                .sqlId(sqlId)
                .severity(calculateSeverity(issues))
                .issues(issues)
                .score(Math.max(0, score))
                .durationMs(50)
                .build();
    }

    @Override
    public List<StaticAnalysisDto> analyzeBatch(List<String> sqls) {
        List<StaticAnalysisDto> results = new ArrayList<>();
        for (int i = 0; i < sqls.size(); i++) {
            results.add(analyze("sql-" + i, sqls.get(i)));
        }
        return results;
    }

    @Override
    public boolean isValidSyntax(String sql) {
        try {
            // 简单语法检查
            return sql != null && !sql.isBlank() &&
                    (sql.toUpperCase().startsWith("SELECT") ||
                     sql.toUpperCase().startsWith("INSERT") ||
                     sql.toUpperCase().startsWith("UPDATE") ||
                     sql.toUpperCase().startsWith("DELETE"));
        } catch (Exception e) {
            return false;
        }
    }

    private SeverityLevel calculateSeverity(List<StaticIssue> issues) {
        return issues.stream()
                .anyMatch(i -> i.getSeverity() == SeverityLevel.CRITICAL)
                ? SeverityLevel.CRITICAL
                : !issues.isEmpty() ? SeverityLevel.WARNING : SeverityLevel.INFO;
    }
}
