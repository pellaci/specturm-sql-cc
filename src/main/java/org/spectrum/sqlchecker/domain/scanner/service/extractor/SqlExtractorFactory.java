package org.spectrum.sqlchecker.domain.scanner.service.extractor;

import org.spectrum.sqlchecker.domain.shared.valueobject.FileType;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * SQL 提取器工厂
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Component
public class SqlExtractorFactory {

    private final List<SqlExtractor> extractors;

    public SqlExtractorFactory(List<SqlExtractor> extractors) {
        this.extractors = extractors;
        // 按优先级排序
        this.extractors.sort(Comparator.comparingInt(SqlExtractor::getPriority));
    }

    /**
     * 获取指定文件类型的提取器
     *
     * @param fileType 文件类型
     * @return SQL 提取器，如果没有找到则返回 null
     */
    public SqlExtractor getExtractor(FileType fileType) {
        return extractors.stream()
            .filter(extractor -> extractor.supports(fileType))
            .findFirst()
            .orElse(null);
    }

    /**
     * 获取所有提取器
     *
     * @return 提取器列表
     */
    public List<SqlExtractor> getAllExtractors() {
        return List.copyOf(extractors);
    }
}
