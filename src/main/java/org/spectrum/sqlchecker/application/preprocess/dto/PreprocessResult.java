package org.spectrum.sqlchecker.application.preprocess.dto;

import lombok.Builder;
import lombok.Data;
import org.spectrum.sqlchecker.domain.shared.enumeration.ExplainEligibility;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlCategory;
import org.spectrum.sqlchecker.domain.shared.enumeration.ValidityStatus;

/**
 * 预处理结果
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
public class PreprocessResult {

    /**
     * SQL ID
     */
    private String sqlId;

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
     * 错误原因
     */
    private String errorReason;
}
