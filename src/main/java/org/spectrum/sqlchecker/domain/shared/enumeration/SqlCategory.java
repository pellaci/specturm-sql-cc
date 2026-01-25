package org.spectrum.sqlchecker.domain.shared.enumeration;

/**
 * SQL 分类
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public enum SqlCategory {

    /**
     * MyBatis XML（静态）
     */
    MYBATIS_XML_STATIC,

    /**
     * MyBatis XML（动态）
     */
    MYBATIS_XML_DYNAMIC,

    /**
     * MyBatis 注解
     */
    MYBATIS_ANNOTATION,

    /**
     * JPA Native Query
     */
    JPA_NATIVE_QUERY,

    /**
     * 字符串拼接
     */
    STRING_CONCAT,

    /**
     * 占位符模板
     */
    PLACEHOLDER_TEMPLATE,

    /**
     * 未知
     */
    UNKNOWN
}
