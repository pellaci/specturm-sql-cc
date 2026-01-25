package org.spectrum.sqlchecker.domain.preprocess.repository;

import org.spectrum.sqlchecker.domain.preprocess.model.SqlPreprocessResult;

import java.util.Optional;

/**
 * SQL 预处理结果仓储接口
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public interface SqlPreprocessResultRepository {

    /**
     * 保存预处理结果
     *
     * @param result 预处理结果
     */
    void saveEntity(SqlPreprocessResult result);

    /**
     * 根据 SQL ID 查找
     *
     * @param sqlId SQL ID
     * @return 预处理结果
     */
    Optional<SqlPreprocessResult> findBySqlId(String sqlId);
}
