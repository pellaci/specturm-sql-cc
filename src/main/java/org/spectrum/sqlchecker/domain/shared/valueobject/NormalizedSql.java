package org.spectrum.sqlchecker.domain.shared.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 规范化 SQL（值对象）
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Getter
@EqualsAndHashCode
public class NormalizedSql {

    private final String value;

    public NormalizedSql(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("规范化 SQL 不能为空");
        }
        this.value = normalize(value);
    }

    private String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
