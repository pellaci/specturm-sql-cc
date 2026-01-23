package org.spectrum.sqlchecker.infrastructure.persistence;

import org.spectrum.sqlchecker.domain.scanner.model.SourceFile;
import org.spectrum.sqlchecker.domain.scanner.repository.SourceFileRepository;
import org.spectrum.sqlchecker.domain.shared.valueobject.FilePath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 源文件仓储实现
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Repository
public interface SourceFileJpaRepository extends JpaRepository<SourceFile, Long>, SourceFileRepository {

    /**
     * 根据文件路径查找
     *
     * @param path 文件路径
     * @return 源文件
     */
    @Override
    default Optional<SourceFile> findByPath(FilePath path) {
        return findByPathValue(path.getValue());
    }

    /**
     * 根据路径值查找
     *
     * @param pathValue 路径值
     * @return 源文件
     */
    Optional<SourceFile> findByPathValue(String pathValue);

    /**
     * 保存源文件
     *
     * @param sourceFile 源文件
     */
    @Override
    default void saveEntity(SourceFile sourceFile) {
        saveAndFlush(sourceFile);
    }

    /**
     * 批量保存
     *
     * @param sourceFiles 源文件列表
     */
    @Override
    default void saveAll(List<SourceFile> sourceFiles) {
        saveAllAndFlush(sourceFiles);
    }

    /**
     * 判断文件是否存在
     *
     * @param path 文件路径
     * @return 是否存在
     */
    @Override
    default boolean existsByPath(FilePath path) {
        return findByPath(path).isPresent();
    }
}
