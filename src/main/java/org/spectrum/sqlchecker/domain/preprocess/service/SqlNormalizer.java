package org.spectrum.sqlchecker.domain.preprocess.service;

/**
 * SQL 规范化器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public interface SqlNormalizer {

    /**
     * 规范化 SQL
     *
     * @param sql 原始 SQL
     * @return 规范化后的 SQL
     */
    String normalize(String sql);
}
