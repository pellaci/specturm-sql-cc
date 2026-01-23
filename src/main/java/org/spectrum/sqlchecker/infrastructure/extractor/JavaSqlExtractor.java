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
        String upper = sql.toUpperCase().trim();
        return upper.startsWith("SELECT")
                || upper.startsWith("INSERT")
                || upper.startsWith("UPDATE")
                || upper.startsWith("DELETE")
                || upper.startsWith("CREATE")
                || upper.startsWith("ALTER")
                || upper.startsWith("DROP")
                || upper.startsWith("TRUNCATE")
                || upper.startsWith("REPLACE")
                || upper.startsWith("WITH")
                || upper.startsWith("CALL")
                || upper.startsWith("SHOW")
                || upper.startsWith("DESC")
                || upper.startsWith("DESCRIBE");
    }
}
