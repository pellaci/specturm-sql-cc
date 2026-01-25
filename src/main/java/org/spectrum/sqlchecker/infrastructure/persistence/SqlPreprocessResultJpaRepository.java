package org.spectrum.sqlchecker.infrastructure.persistence;

import org.spectrum.sqlchecker.domain.preprocess.model.SqlPreprocessResult;
import org.spectrum.sqlchecker.domain.preprocess.repository.SqlPreprocessResultRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * SQL 预处理结果仓储实现
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Repository
public interface SqlPreprocessResultJpaRepository
    extends JpaRepository<SqlPreprocessResult, Long>, SqlPreprocessResultRepository {

    /**
     * 根据 SQL ID 查找
     *
     * @param sqlId SQL ID
     * @return 预处理结果
     */
    @Override
    Optional<SqlPreprocessResult> findBySqlId(String sqlId);

    /**
     * 保存预处理结果
     *
     * @param result 预处理结果
     */
    @Override
    default void saveEntity(SqlPreprocessResult result) {
        saveAndFlush(result);
    }
}
