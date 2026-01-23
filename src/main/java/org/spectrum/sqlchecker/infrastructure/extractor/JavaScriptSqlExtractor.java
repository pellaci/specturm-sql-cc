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
 * JavaScript/TypeScript SQL 提取器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Slf4j
@Component
public class JavaScriptSqlExtractor implements SqlExtractor {

    // 匹配模板字符串中的 SQL
    private static final Pattern TEMPLATE_SQL_PATTERN = Pattern.compile(
            "`([^`]*?(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP)[^`]*?)`",
            Pattern.CASE_INSENSITIVE
    );

    // 匹配字符串中的 SQL
    private static final Pattern STRING_SQL_PATTERN = Pattern.compile(
            "['\"]([^'\"]*?(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP)[^'\"]*?)['\"]",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public List<String> extract(String content) throws SqlExtractionException {
        List<String> sqls = new ArrayList<>();

        try {
            // 提取模板字符串中的 SQL
            extractTemplateSqls(content, sqls);

            // 提取字符串中的 SQL
            extractStringSqls(content, sqls);

            log.debug("Extracted {} SQL statements from JavaScript content", sqls.size());
            return sqls;

        } catch (Exception e) {
            throw new SqlExtractionException("Failed to extract SQL from JavaScript content", e);
        }
    }

    @Override
    public String getName() {
        return "JavaScriptSqlExtractor";
    }

    @Override
    public SqlSourceType getSourceType() {
        return SqlSourceType.JAVASCRIPT;
    }

    @Override
    public boolean supports(FileType fileType) {
        String ext = fileType.getExtension();
        return "js".equals(ext) || "ts".equals(ext);
    }

    /**
     * 提取模板字符串中的 SQL
     */
    private void extractTemplateSqls(String content, List<String> sqls) {
        Matcher matcher = TEMPLATE_SQL_PATTERN.matcher(content);
        while (matcher.find()) {
            String sql = matcher.group(1).trim();
            if (isValidSql(sql)) {
                sqls.add(sql);
            }
        }
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
                || upper.startsWith("SHOW");
    }
}
