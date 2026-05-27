package org.spectrum.sqlchecker.application.preprocess.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.application.preprocess.SqlPreprocessService;
import org.spectrum.sqlchecker.application.preprocess.dto.PreprocessRequest;
import org.spectrum.sqlchecker.application.preprocess.dto.PreprocessResult;
import org.spectrum.sqlchecker.domain.preprocess.model.SqlPreprocessResult;
import org.spectrum.sqlchecker.domain.preprocess.repository.SqlPreprocessResultRepository;
import org.spectrum.sqlchecker.domain.preprocess.service.ExplainSqlBuilder;
import org.spectrum.sqlchecker.domain.preprocess.service.SqlClassifier;
import org.spectrum.sqlchecker.domain.preprocess.service.SqlFixer;
import org.spectrum.sqlchecker.domain.preprocess.service.SqlNormalizer;
import org.spectrum.sqlchecker.domain.preprocess.service.SqlValidator;
import org.spectrum.sqlchecker.domain.shared.enumeration.ExplainEligibility;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlCategory;
import org.spectrum.sqlchecker.domain.shared.enumeration.ValidityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * SQL 预处理服务实现
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlPreprocessServiceImpl implements SqlPreprocessService {

    private final SqlClassifier classifier;
    private final SqlNormalizer normalizer;
    private final SqlValidator validator;
    private final ExplainSqlBuilder explainSqlBuilder;
    private final List<SqlFixer> fixers;
    @Autowired(required = false)
    private SqlPreprocessResultRepository repository;

    @Override
    public PreprocessResult preprocess(PreprocessRequest request) {
        if (request == null || request.getOriginalSql() == null || request.getOriginalSql().isBlank()) {
            return PreprocessResult.builder()
                .sqlId(request != null ? request.getSqlId() : null)
                .validity(ValidityStatus.UNKNOWN)
                .explainEligibility(ExplainEligibility.SKIPPED)
                .errorReason("SQL 为空")
                .build();
        }

        String originalSql = request.getOriginalSql();
        SqlCategory category = classifier.classify(originalSql, request.getSourceType());
        String normalizedSql = normalizer.normalize(originalSql);
        boolean hasUnsafeTextSubstitution = containsUnsafeTextSubstitution(originalSql);

        SqlValidator.ValidationResult validation = validator.validate(normalizedSql);
        String candidateSql = normalizedSql;
        String errorReason = validation.errorReason();

        if (!validation.valid()) {
            candidateSql = applyFixers(category, candidateSql);
            validation = validator.validate(candidateSql);
            errorReason = validation.valid() ? null : validation.errorReason();
        }

        if (!validation.valid() && hasUnsafeTextSubstitution) {
            errorReason = unsafeTextSubstitutionReason();
        }

        ExplainEligibility eligibility = ExplainEligibility.SKIPPED;
        String explainSql = null;
        if (request.isExplainEnabled()) {
            ExplainSqlBuilder.ExplainBuildResult buildResult = explainSqlBuilder.build(candidateSql);
            explainSql = buildResult.sql();
            eligibility = buildResult.eligibility();
            if (errorReason == null && buildResult.reason() != null) {
                errorReason = buildResult.reason();
            }
            if (hasUnsafeTextSubstitution && buildResult.reason() != null) {
                errorReason = buildResult.reason() + "；请改用 #{} 参数绑定或白名单映射";
            }
        }

        ValidityStatus validity = resolveValidity(validation.valid(), hasUnsafeTextSubstitution);
        PreprocessResult result = PreprocessResult.builder()
            .sqlId(request.getSqlId())
            .category(category)
            .normalizedSql(candidateSql)
            .explainSql(explainSql)
            .validity(validity)
            .explainEligibility(eligibility)
            .errorReason(errorReason)
            .build();

        if (repository != null) {
            repository.saveEntity(SqlPreprocessResult.of(
                request.getSqlId(),
                category,
                candidateSql,
                explainSql,
                validity,
                eligibility,
                errorReason
            ));
        }

        return result;
    }

    private String applyFixers(SqlCategory category, String sql) {
        if (sql == null || sql.isBlank()) {
            return sql;
        }
        String current = sql;
        for (SqlFixer fixer : fixers) {
            if (fixer.supports(category)) {
                current = fixer.fix(current);
            }
        }
        return current;
    }

    private boolean containsUnsafeTextSubstitution(String sql) {
        return sql != null && sql.contains("${");
    }

    private ValidityStatus resolveValidity(boolean valid, boolean hasUnsafeTextSubstitution) {
        if (hasUnsafeTextSubstitution) {
            return ValidityStatus.UNKNOWN;
        }
        return valid ? ValidityStatus.VALID : ValidityStatus.INVALID;
    }

    private String unsafeTextSubstitutionReason() {
        return "包含 ${} 文本占位符，无法安全静态归一化；请改用 #{} 参数绑定或白名单映射";
    }
}
