package org.spectrum.sqlchecker.domain.shared.enumeration;

/**
 * SQL 来源类型
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public enum SqlSourceType {

    /**
     * Java 代码
     */
    JAVA,

    /**
     * JavaScript/TypeScript 代码
     */
    JAVASCRIPT,

    /**
     * MyBatis XML
     */
    MYBATIS,

    /**
     * MyBatis 注解
     */
    MYBATIS_ANNOTATION,

    /**
     * JPA @Query 注解
     */
    JPA_ANNOTATION,

    /**
     * 字符串拼接
     */
    STRING_LITERAL,

    /**
     * SQL 文件
     */
    SQL_FILE
}
