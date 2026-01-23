package org.spectrum.sqlchecker.domain.shared.enumeration;

/**
 * 项目类型
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public enum ProjectType {

    /**
     * Maven 项目
     */
    MAVEN,

    /**
     * Gradle 项目
     */
    GRADLE,

    /**
     * Node.js 项目
     */
    NODEJS,

    /**
     * Python 项目
     */
    PYTHON,

    /**
     * 普通项目
     */
    GENERIC,

    /**
     * 自动检测
     */
    AUTO
}
