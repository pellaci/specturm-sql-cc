package org.spectrum.sqlchecker.infrastructure.preprocess;

import lombok.RequiredArgsConstructor;
import org.spectrum.sqlchecker.domain.preprocess.service.ExplainSqlBuilder;
import org.spectrum.sqlchecker.domain.shared.enumeration.ExplainEligibility;
import org.spectrum.sqlchecker.infrastructure.analysis.explain.ExplainSqlPreprocessor;
import org.springframework.stereotype.Component;

/**
 * 默认 Explain SQL 构建器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class DefaultExplainSqlBuilder implements ExplainSqlBuilder {

    private final ExplainSqlPreprocessor preprocessor;

    @Override
    public ExplainBuildResult build(String sql) {
        if (sql == null || sql.isBlank()) {
            return new ExplainBuildResult(null, ExplainEligibility.SKIPPED, "SQL 为空", false);
        }

        ExplainSqlPreprocessor.PreprocessResult preprocess = preprocessor.preprocess(sql);
        if (preprocess.skipped()) {
            return new ExplainBuildResult(null, ExplainEligibility.NOT_SUPPORTED, preprocess.reason(), preprocess.changed());
        }

        return new ExplainBuildResult(preprocess.sql(), ExplainEligibility.SUPPORTED, null, preprocess.changed());
    }
}
