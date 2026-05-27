package org.spectrum.sqlchecker.application.preprocess.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spectrum.sqlchecker.application.preprocess.dto.PreprocessRequest;
import org.spectrum.sqlchecker.application.preprocess.dto.PreprocessResult;
import org.spectrum.sqlchecker.domain.shared.enumeration.ExplainEligibility;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlSourceType;
import org.spectrum.sqlchecker.domain.shared.enumeration.ValidityStatus;
import org.spectrum.sqlchecker.infrastructure.analysis.explain.ExplainSqlPreprocessor;
import org.spectrum.sqlchecker.infrastructure.preprocess.DefaultExplainSqlBuilder;
import org.spectrum.sqlchecker.infrastructure.preprocess.DefaultSqlClassifier;
import org.spectrum.sqlchecker.infrastructure.preprocess.DefaultSqlNormalizer;
import org.spectrum.sqlchecker.infrastructure.preprocess.JSqlParserValidator;
import org.spectrum.sqlchecker.infrastructure.preprocess.MyBatisSqlFixer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SqlPreprocessServiceImpl 测试")
class SqlPreprocessServiceImplTest {

    private final SqlPreprocessServiceImpl service = new SqlPreprocessServiceImpl(
            new DefaultSqlClassifier(),
            new DefaultSqlNormalizer(),
            new JSqlParserValidator(),
            new DefaultExplainSqlBuilder(new ExplainSqlPreprocessor()),
            List.of(new MyBatisSqlFixer())
    );

    @Test
    @DisplayName("MyBatis #{} 参数不应被计入不可解析 SQL")
    void should_parse_mybatis_hash_parameters_after_fixing() {
        PreprocessResult result = service.preprocess(PreprocessRequest.builder()
                .sqlId("sql-1")
                .originalSql("SELECT id FROM users WHERE status = #{status,jdbcType=INTEGER}")
                .sourceType(SqlSourceType.MYBATIS)
                .build());

        assertThat(result.getValidity()).isEqualTo(ValidityStatus.VALID);
        assertThat(result.getNormalizedSql()).contains("status = 1");
        assertThat(result.getErrorReason()).isNull();
    }

    @Test
    @DisplayName("${} 动态拼接应该保留为明确风险原因，并跳过 EXPLAIN")
    void should_keep_dollar_substitution_as_explicit_risk_reason() {
        PreprocessResult result = service.preprocess(PreprocessRequest.builder()
                .sqlId("sql-2")
                .originalSql("SELECT id FROM users WHERE status = #{status} ORDER BY ${orderBy}")
                .sourceType(SqlSourceType.MYBATIS)
                .explainEnabled(true)
                .build());

        assertThat(result.getValidity()).isEqualTo(ValidityStatus.UNKNOWN);
        assertThat(result.getExplainEligibility()).isEqualTo(ExplainEligibility.NOT_SUPPORTED);
        assertThat(result.getErrorReason()).contains("${}");
    }
}
