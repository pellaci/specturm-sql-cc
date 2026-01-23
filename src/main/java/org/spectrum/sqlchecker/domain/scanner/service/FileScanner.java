package org.spectrum.sqlchecker.domain.scanner.service;

import org.spectrum.sqlchecker.domain.scanner.model.SourceFile;
import org.spectrum.sqlchecker.domain.shared.enumeration.ProjectType;
import org.spectrum.sqlchecker.domain.shared.valueobject.FilePath;

import java.io.File;
import java.util.List;

/**
 * 文件扫描器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public interface FileScanner {

    /**
     * 扫描文件
     *
     * @param rootPath 根路径
     * @param includes 包含模式
     * @param excludes 排除模式
     * @return 源文件列表
     */
    List<SourceFile> scan(String rootPath, List<String> includes, List<String> excludes);

    /**
     * 读取源文件
     *
     * @param file 文件
     * @return 源文件
     */
    SourceFile readSourceFile(File file);

    /**
     * 扫描文件获取原始 File 对象
     *
     * @param rootPath 根路径
     * @param includes 包含模式
     * @param excludes 排除模式
     * @return File 对象列表
     */
    List<File> scanFiles(String rootPath, List<String> includes, List<String> excludes);

    /**
     * 检测项目类型
     *
     * @param rootPath 根路径
     * @return 项目类型
     */
    ProjectType detectProjectType(String rootPath);
}
