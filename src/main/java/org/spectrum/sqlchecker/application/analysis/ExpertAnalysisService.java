package org.spectrum.sqlchecker.application.analysis;

import org.spectrum.sqlchecker.application.analysis.dto.ExpertAnalysisDto;
import org.spectrum.sqlchecker.application.analysis.dto.StaticAnalysisDto;
import org.spectrum.sqlchecker.application.analysis.dto.ExplainAnalysisDto;
import org.spectrum.sqlchecker.domain.shared.exception.AnalysisException;

import java.util.List;

/**
 * 专家分析服务
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public interface ExpertAnalysisService {

    /**
     * 分析 SQL 语句
     *
     * @param sqlId SQL ID
     * @param sql SQL 语句
     * @param sqlType SQL 类型
     * @return 分析结果
     * @throws AnalysisException 分析失败
     */
    ExpertAnalysisDto analyze(String sqlId, String sql, String sqlType);

    /**
     * 综合分析（结合静态和执行计划分析）
     *
     * @param sqlId SQL ID
     * @param sql SQL 语句
     * @param sqlType SQL 类型
     * @param staticAnalysis 静态分析结果
     * @param explainAnalysis 执行计划分析结果
     * @return 分析结果
     */
    ExpertAnalysisDto analyzeWithContext(
            String sqlId,
            String sql,
            String sqlType,
            StaticAnalysisDto staticAnalysis,
            ExplainAnalysisDto explainAnalysis);

    /**
     * 批量分析 SQL 语句
     *
     * @param sqls SQL 语句列表
     * @return 分析结果列表
     */
    List<ExpertAnalysisDto> analyzeBatch(List<String> sqls);
}
