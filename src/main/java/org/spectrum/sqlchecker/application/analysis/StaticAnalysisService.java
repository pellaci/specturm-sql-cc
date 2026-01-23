package org.spectrum.sqlchecker.application.analysis;

import org.spectrum.sqlchecker.application.analysis.dto.ExplainAnalysisDto;
import org.spectrum.sqlchecker.application.analysis.dto.ExpertAnalysisDto;
import org.spectrum.sqlchecker.application.analysis.dto.StaticAnalysisDto;
import org.spectrum.sqlchecker.domain.shared.exception.AnalysisException;

import java.util.List;

/**
 * 静态分析服务
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public interface StaticAnalysisService {

    /**
     * 分析 SQL 语句
     *
     * @param sqlId SQL ID
     * @param sql SQL 语句
     * @return 分析结果
     * @throws AnalysisException 分析失败
     */
    StaticAnalysisDto analyze(String sqlId, String sql);

    /**
     * 批量分析 SQL 语句
     *
     * @param sqls SQL 语句列表
     * @return 分析结果列表
     */
    List<StaticAnalysisDto> analyzeBatch(List<String> sqls);

    /**
     * 检查 SQL 语法
     *
     * @param sql SQL 语句
     * @return 是否有效
     */
    boolean isValidSyntax(String sql);
}
