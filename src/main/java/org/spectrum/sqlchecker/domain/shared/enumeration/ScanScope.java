package org.spectrum.sqlchecker.domain.shared.enumeration;

/**
 * 扫描范围
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public enum ScanScope {

    /**
     * 全部
     */
    FULL,

    /**
     * 仅 src 目录
     */
    SRC_ONLY,

    /**
     * 仅 main 源码
     */
    MAIN_ONLY,

    /**
     * 自定义路径
     */
    CUSTOM
}
