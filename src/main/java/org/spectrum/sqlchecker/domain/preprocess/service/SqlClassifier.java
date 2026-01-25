package org.spectrum.sqlchecker.domain.preprocess.service;

import org.spectrum.sqlchecker.domain.shared.enumeration.SqlCategory;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlSourceType;

/**
 * SQL 分类器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public interface SqlClassifier {

    /**
     * 分类 SQL
     *
     * @param sql SQL 文本
     * @param sourceType 来源类型
     * @return 分类结果
     */
    SqlCategory classify(String sql, SqlSourceType sourceType);
}
