package org.spectrum.sqlchecker.infrastructure.scan;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 扫描辅助工具
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public final class SqlScanSupport {

    private SqlScanSupport() {}

    private static final Pattern STRING_SQL_PATTERN = Pattern.compile(
            "\"([^\"$]{0,}?\\b(?:SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|TRUNCATE|REPLACE)\\b[^\"$]{4,}?)\"",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern STRING_LITERAL_PATTERN = Pattern.compile(
            "\"((?:\\\\.|[^\"\\\\])*)\"",
            Pattern.DOTALL
    );
    private static final Pattern ANNOTATION_START_PATTERN = Pattern.compile(
            "@(Select|Insert|Update|Delete|SelectKey|Query)\\s*\\(",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NATIVE_QUERY_PATTERN = Pattern.compile(
            "nativeQuery\\s*=\\s*true",
            Pattern.CASE_INSENSITIVE
    );

    public static List<SqlCandidate> extractSqlFromJavaWithLocations(String content) {
        List<SqlCandidate> candidates = new ArrayList<>();
        Matcher matcher = STRING_SQL_PATTERN.matcher(content);
        while (matcher.find()) {
            String sql = matcher.group(1).trim();
            if (looksLikeSql(sql)) {
                int line = countLineNumber(content, matcher.start(1));
                candidates.add(new SqlCandidate(sql, line));
            }
        }
        return candidates;
    }

    public static List<SqlCandidate> extractSqlFromMyBatisAnnotations(String content) {
        List<SqlCandidate> candidates = new ArrayList<>();
        Matcher matcher = ANNOTATION_START_PATTERN.matcher(content);
        while (matcher.find()) {
            String name = matcher.group(1).toLowerCase();
            if ("query".equals(name)) {
                continue;
            }
            int parenStart = matcher.end() - 1;
            int parenEnd = findMatchingParen(content, parenStart);
            if (parenEnd == -1) {
                continue;
            }
            String args = content.substring(parenStart + 1, parenEnd);
            List<StringLiteral> literals = extractStringLiterals(args, parenStart + 1, content);
            String sql = joinLiterals(literals);
            if (!sql.isBlank() && looksLikeSql(sql)) {
                int line = literals.isEmpty() ? countLineNumber(content, matcher.start()) : literals.get(0).line();
                candidates.add(new SqlCandidate(sql, line));
            }
        }
        return candidates;
    }

    public static List<SqlCandidate> extractSqlFromJpaAnnotations(String content) {
        List<SqlCandidate> candidates = new ArrayList<>();
        Matcher matcher = ANNOTATION_START_PATTERN.matcher(content);
        while (matcher.find()) {
            String name = matcher.group(1).toLowerCase();
            if (!"query".equals(name)) {
                continue;
            }
            int parenStart = matcher.end() - 1;
            int parenEnd = findMatchingParen(content, parenStart);
            if (parenEnd == -1) {
                continue;
            }
            String args = content.substring(parenStart + 1, parenEnd);
            if (!NATIVE_QUERY_PATTERN.matcher(args).find()) {
                continue;
            }
            List<StringLiteral> literals = extractStringLiterals(args, parenStart + 1, content);
            String sql = joinLiterals(literals);
            if (!sql.isBlank() && looksLikeSql(sql)) {
                int line = literals.isEmpty() ? countLineNumber(content, matcher.start()) : literals.get(0).line();
                candidates.add(new SqlCandidate(sql, line));
            }
        }
        return candidates;
    }

    public static List<SqlCandidate> extractSqlFromSqlFile(String content) {
        List<SqlCandidate> statements = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return statements;
        }

        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int line = 1;
        int statementStartLine = 1;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (!inSingleQuote && !inDoubleQuote) {
                if (c == '-' && i + 1 < content.length() && content.charAt(i + 1) == '-') {
                    i += 2;
                    while (i < content.length() && content.charAt(i) != '\n') {
                        i++;
                    }
                    if (i < content.length() && content.charAt(i) == '\n') {
                        line++;
                    }
                    continue;
                }
                if (c == '#') {
                    i++;
                    while (i < content.length() && content.charAt(i) != '\n') {
                        i++;
                    }
                    if (i < content.length() && content.charAt(i) == '\n') {
                        line++;
                    }
                    continue;
                }
                if (c == '/' && i + 1 < content.length() && content.charAt(i + 1) == '*') {
                    i += 2;
                    while (i + 1 < content.length() && !(content.charAt(i) == '*' && content.charAt(i + 1) == '/')) {
                        if (content.charAt(i) == '\n') {
                            line++;
                        }
                        i++;
                    }
                    i++;
                    continue;
                }
            }

            if (c == '\n') {
                line++;
            }

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }

            if (!inSingleQuote && !inDoubleQuote && c == ';') {
                String sql = current.toString().trim();
                if (!sql.isEmpty() && looksLikeSql(sql)) {
                    statements.add(new SqlCandidate(sql, statementStartLine));
                }
                current.setLength(0);
                statementStartLine = line;
                continue;
            }

            if (current.length() == 0 && !Character.isWhitespace(c)) {
                statementStartLine = line;
            }
            current.append(c);
        }

        String remainder = current.toString().trim();
        if (!remainder.isEmpty() && looksLikeSql(remainder)) {
            statements.add(new SqlCandidate(remainder, statementStartLine));
        }

        return statements;
    }

    public static boolean looksLikeSql(String sql) {
        if (sql == null || sql.length() < 10) {
            return false;
        }

        String trimmed = sql.trim();
        String upper = trimmed.toUpperCase();

        // 排除 Redis key 或缓存 key 模式 (如 "update:sku:show:3")
        if (looksLikeCacheKey(trimmed)) {
            return false;
        }

        // SQL 关键字后必须跟空格
        return upper.startsWith("SELECT ") || upper.startsWith("INSERT ") ||
                upper.startsWith("UPDATE ") || upper.startsWith("DELETE ") ||
                upper.startsWith("CREATE ") || upper.startsWith("ALTER ") ||
                upper.startsWith("DROP ") || upper.startsWith("TRUNCATE ") ||
                upper.startsWith("REPLACE ") || upper.startsWith("WITH ") ||
                upper.startsWith("CALL ") || upper.startsWith("SHOW ") ||
                upper.startsWith("DESC ") || upper.startsWith("DESCRIBE ");
    }

    /**
     * 检查是否看起来像缓存 key（如 Redis key）
     */
    private static boolean looksLikeCacheKey(String str) {
        if (str.contains(":") && !str.contains(" ")) {
            return true;
        }
        long colonCount = str.chars().filter(ch -> ch == ':').count();
        long spaceCount = str.chars().filter(ch -> ch == ' ').count();
        if (colonCount > 2 && colonCount > spaceCount) {
            return true;
        }
        return false;
    }

    public static String abstractSql(String sql) {
        String normalized = sql.trim();
        normalized = normalized.replaceAll("'([^'\\\\]|\\\\.)*'", "?");
        normalized = normalized.replaceAll("\"([^\"\\\\]|\\\\.)*\"", "?");
        normalized = normalized.replaceAll("\\b\\d+(?:\\.\\d+)?\\b", "?");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized.isEmpty() ? sql.trim() : normalized;
    }

    private static int findMatchingParen(String content, int openIndex) {
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = openIndex; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }
            if (inSingleQuote || inDoubleQuote) {
                continue;
            }
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static List<StringLiteral> extractStringLiterals(String args, int baseOffset, String fullContent) {
        List<StringLiteral> literals = new ArrayList<>();
        Matcher matcher = STRING_LITERAL_PATTERN.matcher(args);
        while (matcher.find()) {
            String value = unescapeJavaString(matcher.group(1));
            int line = countLineNumber(fullContent, baseOffset + matcher.start(1));
            literals.add(new StringLiteral(value, line));
        }
        return literals;
    }

    private static String joinLiterals(List<StringLiteral> literals) {
        StringBuilder sb = new StringBuilder();
        for (StringLiteral literal : literals) {
            if (!literal.value().isBlank()) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(literal.value().trim());
            }
        }
        return sb.toString();
    }

    private static String unescapeJavaString(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(i + 1);
                switch (next) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default -> sb.append(next);
                }
                i++;
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public static int countLineNumber(String content, int index) {
        int line = 1;
        for (int i = 0; i < index && i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    public record SqlCandidate(String sql, int line) {}

    public record StringLiteral(String value, int line) {}
}
