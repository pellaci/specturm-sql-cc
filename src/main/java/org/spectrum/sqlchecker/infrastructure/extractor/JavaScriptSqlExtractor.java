package org.spectrum.sqlchecker.infrastructure.extractor;

import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.domain.scanner.service.extractor.SqlExtractor;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlSourceType;
import org.spectrum.sqlchecker.domain.shared.exception.SqlExtractionException;
import org.spectrum.sqlchecker.domain.shared.valueobject.FileType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * JavaScript/TypeScript SQL 提取器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Slf4j
@Component
public class JavaScriptSqlExtractor implements SqlExtractor {

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
        extractQuotedSqls(content, sqls, '`');
    }

    /**
     * 提取字符串中的 SQL
     */
    private void extractStringSqls(String content, List<String> sqls) {
        extractQuotedSqls(content, sqls, '"', '\'');
    }

    private void extractQuotedSqls(String content, List<String> sqls, char... quoteChars) {
        for (int i = 0; i < content.length(); i++) {
            char quote = content.charAt(i);
            if (!isTargetQuote(quote, quoteChars)) {
                continue;
            }

            int start = i + 1;
            StringBuilder literal = new StringBuilder();
            i = readLiteral(content, start, quote, literal);
            if (i >= content.length()) {
                break;
            }

            String sql = literal.toString().trim();
            if (isValidSql(sql)) {
                sqls.add(sql);
            }
        }
    }

    private boolean isTargetQuote(char quote, char[] quoteChars) {
        for (char quoteChar : quoteChars) {
            if (quote == quoteChar) {
                return true;
            }
        }
        return false;
    }

    private int readLiteral(String content, int start, char quote, StringBuilder literal) {
        int i = start;
        while (i < content.length()) {
            char c = content.charAt(i);
            if (c == '\\' && i + 1 < content.length()) {
                literal.append(c);
                literal.append(content.charAt(i + 1));
                i += 2;
                continue;
            }
            if (c == quote) {
                return i;
            }
            literal.append(c);
            i++;
        }
        return content.length();
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
