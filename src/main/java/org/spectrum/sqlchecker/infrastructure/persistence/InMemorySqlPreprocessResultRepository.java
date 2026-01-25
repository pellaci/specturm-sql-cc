package org.spectrum.sqlchecker.infrastructure.persistence;

import org.spectrum.sqlchecker.domain.preprocess.model.SqlPreprocessResult;
import org.spectrum.sqlchecker.domain.preprocess.repository.SqlPreprocessResultRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 内存版 SQL 预处理结果仓储（用于无持久化环境）
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Repository
@ConditionalOnMissingBean(SqlPreprocessResultRepository.class)
public class InMemorySqlPreprocessResultRepository implements SqlPreprocessResultRepository {

    private final ConcurrentMap<String, SqlPreprocessResult> storage = new ConcurrentHashMap<>();

    @Override
    public void saveEntity(SqlPreprocessResult result) {
        if (result == null || result.getSqlId() == null) {
            return;
        }
        storage.put(result.getSqlId(), result);
    }

    @Override
    public Optional<SqlPreprocessResult> findBySqlId(String sqlId) {
        if (sqlId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(sqlId));
    }
}
