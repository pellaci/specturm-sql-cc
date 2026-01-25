package org.spectrum.sqlchecker.infrastructure.analysis.explain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Preprocess SQL for EXPLAIN by handling MyBatis placeholders conservatively.
 */
@Slf4j
@Component
public class ExplainSqlPreprocessor {

    // 匹配 /* INCLUDE:xxx */ 模式（MyBatis include 标签占位符）
    private static final Pattern INCLUDE_PLACEHOLDER_PATTERN =
            Pattern.compile("/\\*\\s*INCLUDE:[^*]+\\*/");

    // 匹配 /* BIND:xxx=yyy */ 模式（MyBatis bind 标签占位符）
    private static final Pattern BIND_PLACEHOLDER_PATTERN =
            Pattern.compile("/\\*\\s*BIND:[^*]+\\*/");

    // 匹配 ${...}（MyBatis 文本替换，占位符不可安全替换）
    private static final Pattern DOLLAR_PLACEHOLDER_PATTERN =
            Pattern.compile("\\$\\{[^}]+}");

    // 匹配 IN #{...} 或 IN (#{...})
    private static final Pattern IN_PLACEHOLDER_PAREN_PATTERN =
            Pattern.compile("(?i)\\bin\\s*\\(\\s*#\\{[^}]+}\\s*\\)");

    private static final Pattern IN_PLACEHOLDER_PATTERN =
            Pattern.compile("(?i)\\bin\\s*#\\{[^}]+}");

    private static final Pattern HASH_PLACEHOLDER_PATTERN =
            Pattern.compile("#\\{[^}]+}");

    public PreprocessResult preprocess(String sql) {
        if (sql == null) {
            return new PreprocessResult(null, false, null, false);
        }

        String processed = sql;
        boolean changed = false;

        String trimmed = processed.trim();
        String upper = trimmed.toUpperCase(Locale.ROOT);

        // 仅对常见可解释语句执行计划
        if (!(upper.startsWith("SELECT")
                || upper.startsWith("WITH")
                || upper.startsWith("EXPLAIN")
                || upper.startsWith("UPDATE")
                || upper.startsWith("DELETE")
                || upper.startsWith("INSERT")
                || upper.startsWith("REPLACE"))) {
            return new PreprocessResult(processed, true, "仅对可执行 EXPLAIN 的 DML/查询语句执行", false);
        }

        // 先处理 INCLUDE/BIND 占位符
        if (INCLUDE_PLACEHOLDER_PATTERN.matcher(processed).find()) {
            processed = INCLUDE_PLACEHOLDER_PATTERN.matcher(processed).replaceAll("*");
            changed = true;
            log.debug("SQL 包含 INCLUDE 占位符，已替换为 *");
        }

        if (BIND_PLACEHOLDER_PATTERN.matcher(processed).find()) {
            processed = BIND_PLACEHOLDER_PATTERN.matcher(processed).replaceAll("");
            changed = true;
            log.debug("SQL 包含 BIND 占位符，已移除");
        }

        // ${} 不可安全替换，直接跳过 EXPLAIN
        if (DOLLAR_PLACEHOLDER_PATTERN.matcher(processed).find()) {
            return new PreprocessResult(processed, true, "包含 ${} 文本占位符，已跳过 EXPLAIN", changed);
        }

        // 处理 IN 占位符
        if (IN_PLACEHOLDER_PAREN_PATTERN.matcher(processed).find()) {
            processed = IN_PLACEHOLDER_PAREN_PATTERN.matcher(processed).replaceAll("IN (1)");
            changed = true;
        }
        if (IN_PLACEHOLDER_PATTERN.matcher(processed).find()) {
            processed = IN_PLACEHOLDER_PATTERN.matcher(processed).replaceAll("IN (1)");
            changed = true;
        }

        // 替换其余 #{...} 占位符为常量 1
        if (HASH_PLACEHOLDER_PATTERN.matcher(processed).find()) {
            processed = HASH_PLACEHOLDER_PATTERN.matcher(processed).replaceAll("1");
            changed = true;
        }

        return new PreprocessResult(processed, false, null, changed);
    }

    public record PreprocessResult(String sql, boolean skipped, String reason, boolean changed) {
    }
}
