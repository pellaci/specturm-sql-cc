package org.spectrum.sqlchecker.domain.scanner.repository;

import org.spectrum.sqlchecker.domain.scanner.model.SourceFile;
import org.spectrum.sqlchecker.domain.shared.valueobject.FilePath;

import java.util.List;
import java.util.Optional;

/**
 * 源文件仓储接口
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public interface SourceFileRepository {

    /**
     * 保存源文件
     *
     * @param sourceFile 源文件
     */
    void saveEntity(SourceFile sourceFile);

    /**
     * 批量保存
     *
     * @param sourceFiles 源文件列表
     */
    void saveAll(List<SourceFile> sourceFiles);

    /**
     * 根据路径查找
     *
     * @param path 文件路径
     * @return 源文件
     */
    Optional<SourceFile> findByPath(FilePath path);

    /**
     * 根据扫描 ID 查找所有文件
     *
     * @param scanId 扫描 ID
     * @return 源文件列表
     */
    List<SourceFile> findByScanId(String scanId);

    /**
     * 根据仓库 ID 查找所有文件
     *
     * @param repoId 仓库 ID
     * @return 源文件列表
     */
    List<SourceFile> findByRepoId(String repoId);

    /**
     * 判断文件是否存在
     *
     * @param path 文件路径
     * @return 是否存在
     */
    boolean existsByPath(FilePath path);

    /**
     * 删除扫描相关的所有文件
     *
     * @param scanId 扫描 ID
     */
    void deleteByScanId(String scanId);
}
