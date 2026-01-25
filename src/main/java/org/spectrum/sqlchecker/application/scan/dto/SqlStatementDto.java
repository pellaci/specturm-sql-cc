package org.spectrum.sqlchecker.application.scan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.spectrum.sqlchecker.application.analysis.dto.ExplainAnalysisDto;
import org.spectrum.sqlchecker.application.analysis.dto.ExpertAnalysisDto;
import org.spectrum.sqlchecker.application.analysis.dto.StaticAnalysisDto;
import org.spectrum.sqlchecker.domain.shared.enumeration.ExplainEligibility;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlCategory;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlType;
import org.spectrum.sqlchecker.domain.shared.enumeration.ValidityStatus;

import java.util.List;

/**
 * SQL 语句 DTO
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlStatementDto {

    /**
     * SQL ID
     */
    private String id;

    /**
     * SQL 类型
     */
    private SqlType sqlType;

    /**
     * 原始 SQL
     */
    private String originalSql;

    /**
     * 抽象 SQL（参数化模板）
     */
    private String abstractSql;

    /**
     * SQL 分类
     */
    private SqlCategory category;

    /**
     * 规范化 SQL
     */
    private String normalizedSql;

    /**
     * Explain SQL
     */
    private String explainSql;

    /**
     * 合法性状态
     */
    private ValidityStatus validity;

    /**
     * Explain 可执行性
     */
    private ExplainEligibility explainEligibility;

    /**
     * 预处理错误原因
     */
    private String preprocessErrorReason;

    /**
     * SQL 哈希（用于去重）
     */
    private String sqlHash;

    /**
     * SQL 来源位置列表
     */
    private List<SqlLocationDto> locations;

    /**
     * 静态分析结果（可选，扫描后填充）
     */
    private StaticAnalysisDto staticAnalysis;

    /**
     * EXPLAIN 分析结果（可选，分析后填充）
     */
    private ExplainAnalysisDto explainAnalysis;

    /**
     * 专家分析结果（可选，分析后填充）
     */
    private ExpertAnalysisDto expertAnalysis;

    /**
     * 总体严重等级
     */
    private SeverityLevel severity;

    /**
     * 总体得分
     */
    private Integer score;

    /**
     * 得分说明
     */
    private String scoreExplanation;
}
