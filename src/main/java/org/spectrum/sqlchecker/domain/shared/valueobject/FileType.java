package org.spectrum.sqlchecker.domain.shared.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 文件类型值对象
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Getter
@EqualsAndHashCode
public class FileType {

    private final String extension;

    private FileType(String extension) {
        this.extension = extension.toLowerCase();
    }

    /**
     * 从文件路径创建
     */
    public static FileType fromPath(String path) {
        if (path == null || path.isBlank()) {
            return new FileType("");
        }
        int lastDot = path.lastIndexOf('.');
        if (lastDot >= 0) {
            return new FileType(path.substring(lastDot + 1));
        }
        return new FileType("");
    }

    /**
     * Java 文件类型
     */
    public static FileType java() {
        return new FileType("java");
    }

    /**
     * XML 文件类型
     */
    public static FileType xml() {
        return new FileType("xml");
    }

    /**
     * SQL 文件类型
     */
    public static FileType sql() {
        return new FileType("sql");
    }

    /**
     * 是否为 Java 文件
     */
    public boolean isJava() {
        return "java".equals(extension);
    }

    /**
     * 是否为 XML 文件
     */
    public boolean isXml() {
        return "xml".equals(extension);
    }

    /**
     * 是否为 SQL 文件
     */
    public boolean isSql() {
        return "sql".equals(extension);
    }

    @Override
    public String toString() {
        return extension;
    }
}
