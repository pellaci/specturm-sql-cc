package org.spectrum.sqlchecker.infrastructure.preprocess;

import org.spectrum.sqlchecker.domain.preprocess.service.SqlFixer;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlCategory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * MyBatis SQL 修复器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Component
public class MyBatisSqlFixer implements SqlFixer {

    private static final Pattern XML_TAG_PATTERN =
        Pattern.compile("</?(if|choose|when|otherwise|foreach|trim|where|set)\\b[^>]*>", Pattern.CASE_INSENSITIVE);

    private static final Pattern INCLUDE_PLACEHOLDER_PATTERN =
        Pattern.compile("/\\*\\s*INCLUDE:[^*]+\\*/");

    private static final Pattern BIND_PLACEHOLDER_PATTERN =
        Pattern.compile("/\\*\\s*BIND:[^*]+\\*/");

    private static final Pattern IN_HASH_PLACEHOLDER_PAREN_PATTERN =
        Pattern.compile("(?i)\\bin\\s*\\(\\s*#\\{[^}]+}\\s*\\)");

    private static final Pattern IN_HASH_PLACEHOLDER_PATTERN =
        Pattern.compile("(?i)\\bin\\s*#\\{[^}]+}");

    private static final Pattern HASH_PLACEHOLDER_PATTERN =
        Pattern.compile("#\\{[^}]+}");

    private static final Pattern STRUCTURAL_WHERE_PARAMETER_PATTERN =
        Pattern.compile("(?i)\\bwhere\\s*\\?(?=\\s|$)");

    private static final Pattern STRUCTURAL_AND_PARAMETER_PATTERN =
        Pattern.compile("(?i)\\band\\s*\\?(?=\\s|$)");

    private static final Pattern STRUCTURAL_OR_PARAMETER_PATTERN =
        Pattern.compile("(?i)\\bor\\s*\\?(?=\\s|$)");

    private static final Pattern VALUES_PARAMETER_PATTERN =
        Pattern.compile("(?i)\\bvalues\\s*\\?(?=\\s|$)");

    private static final Pattern CASE_PARAMETER_PATTERN =
        Pattern.compile("(?i)\\bcase\\s*\\?\\s*end\\b");

    private static final Pattern SET_PARAMETER_PATTERN =
        Pattern.compile("(?i)\\bset\\s*\\?(?=\\s*(where\\b|$))");

    private static final Pattern DUPLICATE_COMMA_PATTERN =
        Pattern.compile("(?<!['\"])\\s*,\\s*,+\\s*");

    private static final Pattern FROM_AND_CONDITION_PATTERN =
        Pattern.compile("(?i)\\bfrom\\s+([`\\w.]+)\\s+and\\s+");

    @Override
    public boolean supports(SqlCategory category) {
        return category == SqlCategory.MYBATIS_XML_DYNAMIC
            || category == SqlCategory.MYBATIS_XML_STATIC
            || category == SqlCategory.MYBATIS_ANNOTATION
            || category == SqlCategory.PLACEHOLDER_TEMPLATE;
    }

    @Override
    public String fix(String sql) {
        if (sql == null) {
            return null;
        }
        String fixed = sql;
        fixed = INCLUDE_PLACEHOLDER_PATTERN.matcher(fixed).replaceAll("*");
        fixed = BIND_PLACEHOLDER_PATTERN.matcher(fixed).replaceAll("");
        fixed = XML_TAG_PATTERN.matcher(fixed).replaceAll(" ");
        fixed = IN_HASH_PLACEHOLDER_PAREN_PATTERN.matcher(fixed).replaceAll("IN (1)");
        fixed = IN_HASH_PLACEHOLDER_PATTERN.matcher(fixed).replaceAll("IN (1)");
        fixed = HASH_PLACEHOLDER_PATTERN.matcher(fixed).replaceAll("1");
        fixed = DUPLICATE_COMMA_PATTERN.matcher(fixed).replaceAll(", ");
        fixed = FROM_AND_CONDITION_PATTERN.matcher(fixed).replaceAll("FROM $1 WHERE ");
        fixed = normalizeTemplateFragments(fixed);
        return fixed.replaceAll("\\s+", " ").trim();
    }

    private String normalizeTemplateFragments(String sql) {
        String fixed = sql;
        fixed = CASE_PARAMETER_PATTERN.matcher(fixed).replaceAll("1");
        fixed = VALUES_PARAMETER_PATTERN.matcher(fixed).replaceAll("VALUES (1)");
        fixed = SET_PARAMETER_PATTERN.matcher(fixed).replaceAll("SET template_value = 1");
        fixed = STRUCTURAL_WHERE_PARAMETER_PATTERN.matcher(fixed).replaceAll("WHERE 1 = 1");
        fixed = STRUCTURAL_AND_PARAMETER_PATTERN.matcher(fixed).replaceAll("AND 1 = 1");
        fixed = STRUCTURAL_OR_PARAMETER_PATTERN.matcher(fixed).replaceAll("OR 1 = 1");
        return fixed;
    }
}
