package org.spectrum.sqlchecker.domain.shared.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 原始 SQL 值对象
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Getter
@EqualsAndHashCode
public class OriginalSql {

    private final String value;

    public OriginalSql(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("原始 SQL 不能为空");
        }
        this.value = value;
    }

    /**
     * 获取 SQL 前缀（用于快速判断类型）
     */
    public String getPrefix() {
        String trimmed = value.trim();
        int firstSpace = trimmed.indexOf(' ');
        if (firstSpace > 0) {
            return trimmed.substring(0, firstSpace).toUpperCase();
        }
        return trimmed.toUpperCase();
    }

    @Override
    public String toString() {
        return value;
    }
}
