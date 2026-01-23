package org.spectrum.sqlchecker.domain.shared.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 抽象 SQL（参数化模板）值对象
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Getter
@EqualsAndHashCode
public class AbstractSql {

    private final String value;

    public AbstractSql(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("抽象 SQL 不能为空");
        }
        this.value = normalizeSql(value);
    }

    /**
     * 标准化 SQL（去除多余空格、换行）
     */
    private String normalizeSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    /**
     * 是否为 SELECT 语句
     */
    public boolean isSelect() {
        return value.toUpperCase().startsWith("SELECT");
    }

    @Override
    public String toString() {
        return value;
    }
}
