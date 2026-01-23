package org.spectrum.sqlchecker.domain.scanner.service.extractor;

import org.spectrum.sqlchecker.domain.shared.enumeration.SqlSourceType;
import org.spectrum.sqlchecker.domain.shared.exception.SqlExtractionException;
import org.spectrum.sqlchecker.domain.shared.valueobject.FileType;

import java.util.List;

/**
 * SQL 提取器接口
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public interface SqlExtractor {

    /**
     * 从内容中提取 SQL
     *
     * @param content 文件内容
     * @return 提取的 SQL 语句列表
     * @throws SqlExtractionException 提取失败
     */
    List<String> extract(String content) throws SqlExtractionException;

    /**
     * 获取提取器名称
     *
     * @return 名称
     */
    String getName();

    /**
     * 获取 SQL 来源类型
     *
     * @return 来源类型
     */
    SqlSourceType getSourceType();

    /**
     * 获取优先级（数值越小优先级越高）
     *
     * @return 优先级
     */
    default int getPriority() {
        return 100;
    }

    /**
     * 判断是否支持指定文件类型
     *
     * @param fileType 文件类型
     * @return 是否支持
     */
    default boolean supports(FileType fileType) {
        return false;
    }
}
