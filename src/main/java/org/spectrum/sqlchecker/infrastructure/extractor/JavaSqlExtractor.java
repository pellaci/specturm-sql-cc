package org.spectrum.sqlchecker.infrastructure.extractor;

import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.domain.scanner.service.extractor.SqlExtractor;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlSourceType;
import org.spectrum.sqlchecker.domain.shared.exception.SqlExtractionException;
import org.spectrum.sqlchecker.domain.shared.valueobject.FileType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java SQL 提取器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Slf4j
@Component
public class JavaSqlExtractor implements SqlExtractor {

    // 匹配字符串中的 SQL
    private static final Pattern STRING_SQL_PATTERN = Pattern.compile(
            "\"([^\"]*?(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|TRUNCATE|REPLACE)[^\"]*?)\"",
            Pattern.CASE_INSENSITIVE
    );

    // 匹配 @Query 注解
    private static final Pattern QUERY_ANNOTATION_PATTERN = Pattern.compile(
            "@Query\\s*\\(\\s*value\\s*=\\s*\"([^\"]*?)\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // 匹配 Native 查询
    private static final Pattern NATIVE_QUERY_PATTERN = Pattern.compile(
            "nativeQuery\\s*=\\s*true",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public List<String> extract(String content) throws SqlExtractionException {
        List<String> sqls = new ArrayList<>();

        try {
            // 提取字符串中的 SQL
            extractStringSqls(content, sqls);

            // 提取 @Query 注解中的 SQL
            extractQueryAnnotationSqls(content, sqls);

            log.debug("Extracted {} SQL statements from Java content", sqls.size());
            return sqls;

        } catch (Exception e) {
            throw new SqlExtractionException("Failed to extract SQL from Java content", e);
        }
    }

    @Override
    public String getName() {
        return "JavaSqlExtractor";
    }

    @Override
    public SqlSourceType getSourceType() {
        return SqlSourceType.JAVA;
    }

    @Override
    public boolean supports(FileType fileType) {
        return fileType.isJava();
    }

    /**
     * 提取字符串中的 SQL
     */
    private void extractStringSqls(String content, List<String> sqls) {
        Matcher matcher = STRING_SQL_PATTERN.matcher(content);
        while (matcher.find()) {
            String sql = matcher.group(1).trim();
            if (isValidSql(sql)) {
                sqls.add(sql);
            }
        }
    }

    /**
     * 提取 @Query 注解中的 SQL
     */
    private void extractQueryAnnotationSqls(String content, List<String> sqls) {
        Matcher queryMatcher = QUERY_ANNOTATION_PATTERN.matcher(content);
        Matcher nativeMatcher = NATIVE_QUERY_PATTERN.matcher(content);

        boolean hasNativeQuery = nativeMatcher.find();

        while (queryMatcher.find()) {
            String sql = queryMatcher.group(1).trim();
            // 只提取 native query
            if (hasNativeQuery && isValidSql(sql)) {
                sqls.add(sql);
            }
        }
    }

    /**
     * 检查是否是有效的 SQL
     */
    private boolean isValidSql(String sql) {
        if (sql == null || sql.length() < 10) {
            return false;
        }

        String trimmed = sql.trim();
        String upper = trimmed.toUpperCase();

        // 排除 Redis key 或缓存 key 模式 (如 "update:sku:enlandy:")
        if (looksLikeCacheKey(trimmed)) {
            return false;
        }

        // SQL 关键字后必须跟空白字符，表示有后续的表名/字段等
        return matchesSqlPattern(upper, "SELECT ")
                || matchesSqlPattern(upper, "INSERT ")
                || matchesSqlPattern(upper, "UPDATE ")
                || matchesSqlPattern(upper, "DELETE ")
                || matchesSqlPattern(upper, "CREATE ")
                || matchesSqlPattern(upper, "ALTER ")
                || matchesSqlPattern(upper, "DROP ")
                || matchesSqlPattern(upper, "TRUNCATE ")
                || matchesSqlPattern(upper, "REPLACE ")
                || matchesSqlPattern(upper, "WITH ")
                || matchesSqlPattern(upper, "CALL ")
                || matchesSqlPattern(upper, "SHOW ")
                || matchesSqlPattern(upper, "DESC ")
                || matchesSqlPattern(upper, "DESCRIBE ");
    }

    /**
     * 检查字符串是否以指定的 SQL 关键字模式开头
     */
    private boolean matchesSqlPattern(String upper, String keyword) {
        return upper.startsWith(keyword);
    }

    /**
     * 检查是否看起来像缓存 key（如 Redis key）
     * 缓存 key 通常是冒号分隔的标识符，如 "update:sku:enlandy:"
     */
    private boolean looksLikeCacheKey(String str) {
        // 如果字符串包含冒号且没有空格，很可能是缓存 key
        if (str.contains(":") && !str.contains(" ")) {
            return true;
        }
        // 如果冒号数量超过空格数量，也可能是缓存 key 模式
        long colonCount = str.chars().filter(ch -> ch == ':').count();
        long spaceCount = str.chars().filter(ch -> ch == ' ').count();
        if (colonCount > 2 && colonCount > spaceCount) {
            return true;
        }
        return false;
    }
}
