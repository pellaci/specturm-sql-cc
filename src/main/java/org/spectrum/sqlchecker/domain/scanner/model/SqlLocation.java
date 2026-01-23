package org.spectrum.sqlchecker.domain.scanner.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.spectrum.sqlchecker.domain.shared.entity.Entity;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlSourceType;
import org.spectrum.sqlchecker.domain.shared.valueobject.FilePath;

/**
 * SQL 位置实体
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class SqlLocation extends Entity {

    /**
     * 文件路径
     */
    private FilePath filePath;

    /**
     * 起始行号
     */
    private int startLine;

    /**
     * 结束行号
     */
    private int endLine;

    /**
     * 起始列号
     */
    private int startColumn;

    /**
     * 结束列号
     */
    private int endColumn;

    /**
     * 类名
     */
    private String className;

    /**
     * 方法名
     */
    private String methodName;

    /**
     * 来源类型
     */
    private SqlSourceType sourceType;

    /**
     * 获取位置描述字符串
     *
     * @return 位置描述，如 "com/example/Mapper.java:25:10"
     */
    public String getLocationString() {
        return String.format("%s:%d:%d", filePath.getValue(), startLine, startColumn);
    }

    /**
     * 获取短位置描述（仅文件名和行号）
     *
     * @return 短位置描述，如 "Mapper.java:25"
     */
    public String getShortLocationString() {
        return String.format("%s:%d", filePath.getFileName(), startLine);
    }

    /**
     * 静态工厂方法
     */
    public static SqlLocation of(FilePath filePath, int line, int column, SqlSourceType sourceType) {
        return SqlLocation.builder()
            .filePath(filePath)
            .startLine(line)
            .endLine(line)
            .startColumn(column)
            .endColumn(column)
            .sourceType(sourceType)
            .build();
    }

    @Override
    public String toString() {
        return getLocationString();
    }
}
