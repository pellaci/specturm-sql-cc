package org.spectrum.sqlchecker.domain.scanner.repository;

import org.spectrum.sqlchecker.domain.shared.valueobject.FilePath;
import org.spectrum.sqlchecker.domain.shared.enumeration.ProjectType;

import java.io.File;
import java.util.List;

/**
 * 文件扫描器仓储接口
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public interface FileScannerRepository {

    /**
     * 扫描目录获取源文件列表
     *
     * @param rootPath 根路径
     * @param includes 包含模式列表
     * @param excludes 排除模式列表
     * @return 文件列表
     */
    List<File> scanFiles(String rootPath, List<String> includes, List<String> excludes);

    /**
     * 读取文件内容
     *
     * @param file 文件
     * @return 文件内容
     */
    String readFileContent(File file);

    /**
     * 检测项目类型
     *
     * @param rootPath 项目根路径
     * @return 项目类型
     */
    ProjectType detectProjectType(String rootPath);

    /**
     * 检查路径是否存在且可读
     *
     * @param path 路径
     * @return 是否可访问
     */
    boolean isAccessible(String path);

    /**
     * 获取文件行数
     *
     * @param file 文件
     * @return 行数
     */
    int getLineCount(File file);
}
