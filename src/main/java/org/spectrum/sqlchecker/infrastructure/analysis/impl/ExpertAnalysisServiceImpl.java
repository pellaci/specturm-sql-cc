package org.spectrum.sqlchecker.infrastructure.analysis.impl;

import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.application.analysis.ExpertAnalysisService;
import org.spectrum.sqlchecker.application.analysis.dto.ExpertAnalysisDto;
import org.spectrum.sqlchecker.application.analysis.dto.Recommendation;
import org.spectrum.sqlchecker.application.analysis.dto.StaticAnalysisDto;
import org.spectrum.sqlchecker.application.analysis.dto.ExplainAnalysisDto;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;
import org.spectrum.sqlchecker.domain.shared.exception.AnalysisException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 专家分析服务实现
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Slf4j
@Service
public class ExpertAnalysisServiceImpl implements ExpertAnalysisService {

    @Override
    public ExpertAnalysisDto analyze(String sqlId, String sql, String sqlType) {
        List<Recommendation> recommendations = new ArrayList<>();
        int score = 100;

        String upperSql = sql.toUpperCase();

        // 分析 SQL 并给出建议
        if (upperSql.contains("SELECT *")) {
            recommendations.add(Recommendation.builder()
                    .type("COLUMN_SPECIFICATION")
                    .title("避免使用 SELECT *")
                    .description("查询所有列会增加网络传输和内存消耗")
                    .suggestion("明确列出需要的列名")
                    .expectedBenefit("减少 30-50% 的数据传输量")
                    .priority(4)
                    .build());
            score -= 15;
        }

        if (upperSql.contains("ORDER BY") && !upperSql.contains("LIMIT")) {
            recommendations.add(Recommendation.builder()
                    .type("PAGINATION")
                    .title("添加 LIMIT 分页")
                    .description("大数据量排序可能导致性能问题")
                    .suggestion("在 ORDER BY 后添加 LIMIT 限制返回行数")
                    .expectedBenefit("避免大结果集排序，提升响应速度")
                    .priority(3)
                    .build());
            score -= 10;
        }

        return ExpertAnalysisDto.builder()
                .sqlId(sqlId)
                .recommendations(recommendations)
                .score(Math.max(0, score))
                .severity(score < 70 ? SeverityLevel.WARNING : SeverityLevel.INFO)
                .durationMs(100)
                .build();
    }

    @Override
    public ExpertAnalysisDto analyzeWithContext(
            String sqlId,
            String sql,
            String sqlType,
            StaticAnalysisDto staticAnalysis,
            ExplainAnalysisDto explainAnalysis) {

        List<Recommendation> recommendations = new ArrayList<>();
        int score = 100;

        // 结合静态分析和执行计划结果
        if (staticAnalysis != null && !staticAnalysis.getIssues().isEmpty()) {
            recommendations.add(Recommendation.builder()
                    .type("STATIC_ANALYSIS")
                    .title("修复静态分析发现的问题")
                    .description("静态分析发现 " + staticAnalysis.getIssues().size() + " 个问题")
                    .suggestion("查看静态分析详情并逐个修复")
                    .expectedBenefit("提升代码质量和可维护性")
                    .priority(5)
                    .build());
            score -= 20;
        }

        if (explainAnalysis != null && explainAnalysis.hasIssues()) {
            recommendations.add(Recommendation.builder()
                    .type("EXECUTION_PLAN")
                    .title("优化执行计划")
                    .description("执行计划分析发现 " + explainAnalysis.getIssues().size() + " 个问题")
                    .suggestion("考虑添加索引或重写查询")
                    .expectedBenefit("显著提升查询性能")
                    .priority(5)
                    .build());
            score -= 30;
        }

        return ExpertAnalysisDto.builder()
                .sqlId(sqlId)
                .recommendations(recommendations)
                .score(Math.max(0, score))
                .severity(score < 60 ? SeverityLevel.CRITICAL : score < 80 ? SeverityLevel.WARNING : SeverityLevel.INFO)
                .durationMs(150)
                .build();
    }

    @Override
    public List<ExpertAnalysisDto> analyzeBatch(List<String> sqls) {
        List<ExpertAnalysisDto> results = new ArrayList<>();
        for (int i = 0; i < sqls.size(); i++) {
            results.add(analyze("sql-" + i, sqls.get(i), "SELECT"));
        }
        return results;
    }
}
