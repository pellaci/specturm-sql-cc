package org.spectrum.sqlchecker.infrastructure.persistence;

import org.spectrum.sqlchecker.domain.scanner.model.SqlStatement;
import org.spectrum.sqlchecker.domain.scanner.repository.SqlStatementRepository;
import org.spectrum.sqlchecker.domain.shared.valueobject.SqlHash;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * SQL 语句仓储实现
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Repository
public interface SqlStatementJpaRepository extends JpaRepository<SqlStatement, Long>, SqlStatementRepository {

    /**
     * 根据 SQL Hash 查找
     *
     * @param hash SQL Hash
     * @return SQL 语句
     */
    @Override
    default Optional<SqlStatement> findByHash(SqlHash hash) {
        // 使用哈希值作为字符串 ID 查找
        return findById(hash.getValue());
    }

    /**
     * 保存 SQL 语句
     *
     * @param sqlStatement SQL 语句
     */
    @Override
    default void saveEntity(SqlStatement sqlStatement) {
        saveAndFlush(sqlStatement);
    }

    /**
     * 批量保存
     *
     * @param sqlStatements SQL 语句列表
     */
    @Override
    default void saveAll(List<SqlStatement> sqlStatements) {
        saveAllAndFlush(sqlStatements);
    }

    /**
     * 根据 ID 查找
     *
     * @param id SQL ID
     * @return SQL 语句
     */
    @Override
    default Optional<SqlStatement> findById(String id) {
        // 尝试解析为 Long ID
        try {
            Long longId = Long.parseLong(id);
            // 转换为 JpaRepository 类型来调用 findById
            return ((JpaRepository<SqlStatement, Long>) this).findById(longId);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
