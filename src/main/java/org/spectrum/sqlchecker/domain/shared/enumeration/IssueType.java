package org.spectrum.sqlchecker.domain.shared.enumeration;

/**
 * 静态分析问题类型
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public enum IssueType {

    // SELECT 相关
    SELECT_STAR,
    SELECT_WITHOUT_WHERE,
    ORDER_BY_WITHOUT_LIMIT,

    // 索引相关
    MISSING_INDEX,
    IMPLICIT_TYPE_CONVERSION,

    // JOIN 相关
    SUSPICIOUS_JOIN_ORDER,
    CROSS_JOIN,

    // 子查询相关
    SUBQUERY_IN_SELECT,
    UNCORRELATED_SUBQUERY,

    // N+1 相关
    POTENTIAL_N_PLUS_ONE,

    // 安全相关
    SQL_INJECTION_RISK,
    DYNAMIC_SQL,

    // 其他
    UNKNOWN,
    SQL_SYNTAX_ERROR,
    TOO_MANY_JOINS,
    LIKE_LEADING_WILDCARD
}
