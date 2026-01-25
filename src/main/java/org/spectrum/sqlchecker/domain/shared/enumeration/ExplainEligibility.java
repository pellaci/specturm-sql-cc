package org.spectrum.sqlchecker.domain.shared.enumeration;

/**
 * EXPLAIN 可执行性
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public enum ExplainEligibility {

    /**
     * 支持 EXPLAIN
     */
    SUPPORTED,

    /**
     * 不支持 EXPLAIN
     */
    NOT_SUPPORTED,

    /**
     * 跳过 EXPLAIN
     */
    SKIPPED
}
