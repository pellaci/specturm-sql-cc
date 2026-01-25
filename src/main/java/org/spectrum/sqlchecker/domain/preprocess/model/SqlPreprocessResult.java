package org.spectrum.sqlchecker.domain.preprocess.model;

import lombok.Builder;
import lombok.Getter;
import org.spectrum.sqlchecker.domain.shared.entity.Entity;
import org.spectrum.sqlchecker.domain.shared.enumeration.ExplainEligibility;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlCategory;
import org.spectrum.sqlchecker.domain.shared.enumeration.ValidityStatus;
import org.spectrum.sqlchecker.domain.shared.valueobject.ExplainSql;
import org.spectrum.sqlchecker.domain.shared.valueobject.NormalizedSql;

import java.time.Instant;

/**
 * SQL 预处理结果实体
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Getter
@Builder
public class SqlPreprocessResult extends Entity {

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
    private NormalizedSql normalizedSql;

    /**
     * Explain SQL
     */
    private ExplainSql explainSql;

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

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 便捷构造
     */
    public static SqlPreprocessResult of(String sqlId,
                                         SqlCategory category,
                                         String normalizedSql,
                                         String explainSql,
                                         ValidityStatus validity,
                                         ExplainEligibility eligibility,
                                         String errorReason) {
        return SqlPreprocessResult.builder()
            .sqlId(sqlId)
            .category(category)
            .normalizedSql(new NormalizedSql(normalizedSql))
            .explainSql(explainSql != null && !explainSql.isBlank() ? new ExplainSql(explainSql) : null)
            .validity(validity)
            .explainEligibility(eligibility)
            .errorReason(errorReason)
            .createdAt(Instant.now())
            .build();
    }

    /**
     * 获取规范化 SQL 字符串
     */
    public String getNormalizedSqlValue() {
        return normalizedSql != null ? normalizedSql.getValue() : null;
    }

    /**
     * 获取 Explain SQL 字符串
     */
    public String getExplainSqlValue() {
        return explainSql != null ? explainSql.getValue() : null;
    }
}
