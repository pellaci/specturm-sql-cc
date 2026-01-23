package org.spectrum.sqlchecker.domain.scanner.repository;

import org.spectrum.sqlchecker.domain.scanner.model.SqlStatement;
import org.spectrum.sqlchecker.domain.shared.valueobject.SqlHash;

import java.util.List;
import java.util.Optional;

/**
 * SQL 语句仓储接口
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public interface SqlStatementRepository {

    /**
     * 保存 SQL 语句
     *
     * @param sqlStatement SQL 语句
     */
    void saveEntity(SqlStatement sqlStatement);

    /**
     * 批量保存
     *
     * @param sqlStatements SQL 语句列表
     */
    void saveAll(List<SqlStatement> sqlStatements);

    /**
     * 根据 ID 查找
     *
     * @param id SQL ID
     * @return SQL 语句
     */
    Optional<SqlStatement> findById(String id);

    /**
     * 根据哈希查找（去重用）
     *
     * @param hash SQL 哈希
     * @return SQL 语句
     */
    Optional<SqlStatement> findByHash(SqlHash hash);

    /**
     * 根据扫描 ID 查找所有 SQL
     *
     * @param scanId 扫描 ID
     * @return SQL 语句列表
     */
    List<SqlStatement> findByScanId(String scanId);

    /**
     * 根据文件 ID 查找所有 SQL
     *
     * @param fileId 文件 ID
     * @return SQL 语句列表
     */
    List<SqlStatement> findByFileId(String fileId);

    /**
     * 删除扫描相关的所有 SQL
     *
     * @param scanId 扫描 ID
     */
    void deleteByScanId(String scanId);

    /**
     * 判断指定哈希的 SQL 是否存在
     *
     * @param hash SQL 哈希
     * @return 是否存在
     */
    boolean existsByHash(SqlHash hash);
}
