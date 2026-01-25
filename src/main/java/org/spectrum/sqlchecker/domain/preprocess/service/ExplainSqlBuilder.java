package org.spectrum.sqlchecker.domain.preprocess.service;

import org.spectrum.sqlchecker.domain.shared.enumeration.ExplainEligibility;

/**
 * Explain SQL 构建器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public interface ExplainSqlBuilder {

    /**
     * 生成 Explain SQL
     *
     * @param sql SQL 文本
     * @return 构建结果
     */
    ExplainBuildResult build(String sql);

    /**
     * 构建结果
     */
    record ExplainBuildResult(String sql, ExplainEligibility eligibility, String reason, boolean changed) {
    }
}
