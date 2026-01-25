package org.spectrum.sqlchecker.domain.preprocess.service;

import org.spectrum.sqlchecker.domain.shared.enumeration.SqlCategory;

/**
 * SQL 修复器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public interface SqlFixer {

    /**
     * 是否支持该分类
     *
     * @param category SQL 分类
     * @return 是否支持
     */
    boolean supports(SqlCategory category);

    /**
     * 修复 SQL
     *
     * @param sql 原始 SQL
     * @return 修复后的 SQL
     */
    String fix(String sql);
}
