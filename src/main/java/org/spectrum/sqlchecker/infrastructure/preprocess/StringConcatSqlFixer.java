package org.spectrum.sqlchecker.infrastructure.preprocess;

import org.spectrum.sqlchecker.domain.preprocess.service.SqlFixer;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlCategory;
import org.springframework.stereotype.Component;

/**
 * 字符串拼接 SQL 修复器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Component
public class StringConcatSqlFixer implements SqlFixer {

    @Override
    public boolean supports(SqlCategory category) {
        return category == SqlCategory.STRING_CONCAT
            || category == SqlCategory.PLACEHOLDER_TEMPLATE;
    }

    @Override
    public String fix(String sql) {
        if (sql == null) {
            return null;
        }
        String fixed = sql;
        fixed = fixed.replaceAll("\"\\s*\\+\\s*\"", "");
        fixed = fixed.replaceAll("'\\s*\\+\\s*'", "");
        fixed = fixed.replace("\\\"", "\"");
        return fixed.replaceAll("\\s+", " ").trim();
    }
}
