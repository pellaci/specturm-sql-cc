package org.spectrum.sqlchecker.domain.preprocess.service;

/**
 * SQL 合法性校验器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public interface SqlValidator {

    /**
     * 校验 SQL
     *
     * @param sql SQL 文本
     * @return 校验结果
     */
    ValidationResult validate(String sql);

    /**
     * 校验结果
     */
    record ValidationResult(boolean valid, String errorReason) {
    }
}
