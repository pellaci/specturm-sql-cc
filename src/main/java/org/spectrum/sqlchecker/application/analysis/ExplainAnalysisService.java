package org.spectrum.sqlchecker.application.analysis;

import org.spectrum.sqlchecker.application.analysis.dto.ExplainAnalysisDto;
import org.spectrum.sqlchecker.domain.shared.exception.AnalysisException;

/**
 * EXPLAIN 分析服务
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public interface ExplainAnalysisService {

    /**
     * 分析 SQL 执行计划
     *
     * @param sqlId SQL ID
     * @param sql SQL 语句
     * @param connectionId 数据库连接 ID
     * @return 分析结果
     * @throws AnalysisException 分析失败
     */
    ExplainAnalysisDto analyze(String sqlId, String sql, String connectionId);

    /**
     * 使用默认连接分析 SQL 执行计划
     *
     * @param sqlId SQL ID
     * @param sql SQL 语句
     * @return 分析结果
     * @throws AnalysisException 分析失败
     */
    ExplainAnalysisDto analyzeWithDefaultConnection(String sqlId, String sql);
}
