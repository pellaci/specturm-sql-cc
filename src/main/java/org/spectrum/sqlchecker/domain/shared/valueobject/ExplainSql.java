package org.spectrum.sqlchecker.domain.shared.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Explain SQL（值对象）
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Getter
@EqualsAndHashCode
public class ExplainSql {

    private final String value;

    public ExplainSql(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Explain SQL 不能为空");
        }
        this.value = value.trim();
    }
}
