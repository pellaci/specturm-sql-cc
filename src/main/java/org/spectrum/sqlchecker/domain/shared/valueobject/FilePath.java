package org.spectrum.sqlchecker.domain.shared.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 文件路径值对象
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Getter
@EqualsAndHashCode
public class FilePath {

    private final String value;

    public FilePath(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        this.value = normalizePath(value);
    }

    /**
     * 标准化路径
     */
    private String normalizePath(String path) {
        return path.replace('\\', '/');
    }

    /**
     * 获取文件名
     */
    public String getFileName() {
        int lastSlash = value.lastIndexOf('/');
        return lastSlash >= 0 ? value.substring(lastSlash + 1) : value;
    }

    /**
     * 获取扩展名
     */
    public String getExtension() {
        int lastDot = value.lastIndexOf('.');
        return lastDot >= 0 ? value.substring(lastDot + 1) : "";
    }

    /**
     * 获取父目录路径
     */
    public String getParentPath() {
        int lastSlash = value.lastIndexOf('/');
        return lastSlash >= 0 ? value.substring(0, lastSlash) : "";
    }

    /**
     * 是否为 Java 文件
     */
    public boolean isJava() {
        return "java".equalsIgnoreCase(getExtension());
    }

    /**
     * 是否为 XML 文件
     */
    public boolean isXml() {
        return "xml".equalsIgnoreCase(getExtension());
    }

    /**
     * 是否为 SQL 文件
     */
    public boolean isSql() {
        return "sql".equalsIgnoreCase(getExtension());
    }

    @Override
    public String toString() {
        return value;
    }
}
