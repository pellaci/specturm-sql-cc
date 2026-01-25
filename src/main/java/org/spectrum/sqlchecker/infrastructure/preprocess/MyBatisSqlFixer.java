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

    @Override
    public boolean supports(SqlCategory category) {
        return category == SqlCategory.MYBATIS_XML_DYNAMIC
            || category == SqlCategory.MYBATIS_XML_STATIC
            || category == SqlCategory.MYBATIS_ANNOTATION;
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
        return fixed.replaceAll("\\s+", " ").trim();
    }
}
