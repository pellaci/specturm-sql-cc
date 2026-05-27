package org.spectrum.sqlchecker.domain.shared.util;

import java.util.Locale;
import java.util.Map;

/**
 * 统一标签映射
 *
 * @author Spectrum SQL Checker
 * @since 1.1.0
 */
public final class LabelMapper {

    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("MYBATIS_XML_STATIC", "MyBatis XML 静态"),
            Map.entry("MYBATIS_XML_DYNAMIC", "MyBatis XML 动态"),
            Map.entry("MYBATIS_ANNOTATION", "MyBatis 注解"),
            Map.entry("JPA_NATIVE_QUERY", "JPA 原生查询"),
            Map.entry("STRING_CONCAT", "字符串拼接"),
            Map.entry("PLACEHOLDER_TEMPLATE", "占位符模板"),
            Map.entry("UNKNOWN", "未知"),
            Map.entry("VALID", "合法"),
            Map.entry("INVALID", "非法"),
            Map.entry("SUPPORTED", "可执行"),
            Map.entry("NOT_SUPPORTED", "不支持"),
            Map.entry("SKIPPED", "已跳过"),
            Map.entry("JAVA", "Java"),
            Map.entry("JAVASCRIPT", "JavaScript/TypeScript"),
            Map.entry("MYBATIS", "MyBatis XML"),
            Map.entry("JPA_ANNOTATION", "JPA 注解"),
            Map.entry("STRING_LITERAL", "字符串拼接"),
            Map.entry("SQL_FILE", "SQL 文件"),
            Map.entry("SELECT_STAR", "SELECT *"),
            Map.entry("SELECT_WITHOUT_WHERE", "SELECT 无 WHERE"),
            Map.entry("ORDER_BY_WITHOUT_LIMIT", "ORDER BY 无 LIMIT"),
            Map.entry("MISSING_INDEX", "缺失索引"),
            Map.entry("IMPLICIT_TYPE_CONVERSION", "隐式类型转换"),
            Map.entry("SUSPICIOUS_JOIN_ORDER", "可疑 JOIN 顺序"),
            Map.entry("CROSS_JOIN", "隐式/CROSS JOIN"),
            Map.entry("SUBQUERY_IN_SELECT", "SELECT 中子查询"),
            Map.entry("UNCORRELATED_SUBQUERY", "非相关子查询"),
            Map.entry("POTENTIAL_N_PLUS_ONE", "潜在 N+1"),
            Map.entry("SQL_INJECTION_RISK", "SQL 注入风险"),
            Map.entry("DYNAMIC_SQL", "动态 SQL"),
            Map.entry("SQL_SYNTAX_ERROR", "SQL 语法错误"),
            Map.entry("TOO_MANY_JOINS", "过多 JOIN"),
            Map.entry("LIKE_LEADING_WILDCARD", "前置通配符 LIKE"),
            Map.entry("FULL_TABLE_SCAN", "全表扫描"),
            Map.entry("NO_INDEX_USED", "未使用索引"),
            Map.entry("HIGH_ROWS", "扫描行数过多"),
            Map.entry("USING_TEMPORARY", "使用临时表"),
            Map.entry("USING_FILESORT", "使用文件排序")
    );

    private LabelMapper() {
    }

    public static String format(String raw) {
        if (raw == null || raw.isBlank()) {
            return "未知";
        }
        String normalized = raw.trim();
        String mapped = LABELS.get(normalized);
        if (mapped != null) {
            return mapped;
        }
        String[] parts = normalized.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }
}
