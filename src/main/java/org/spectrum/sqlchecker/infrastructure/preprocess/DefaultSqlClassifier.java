package org.spectrum.sqlchecker.infrastructure.preprocess;

import org.spectrum.sqlchecker.domain.preprocess.service.SqlClassifier;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlCategory;
import org.spectrum.sqlchecker.domain.shared.enumeration.SqlSourceType;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 默认 SQL 分类器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Component
public class DefaultSqlClassifier implements SqlClassifier {

    private static final Pattern MYBATIS_DYNAMIC_PATTERN =
        Pattern.compile("<(if|choose|when|otherwise|foreach|trim|where|set)\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public SqlCategory classify(String sql, SqlSourceType sourceType) {
        String content = sql == null ? "" : sql;

        if (sourceType == SqlSourceType.MYBATIS) {
            return MYBATIS_DYNAMIC_PATTERN.matcher(content).find()
                ? SqlCategory.MYBATIS_XML_DYNAMIC
                : SqlCategory.MYBATIS_XML_STATIC;
        }

        if (sourceType == SqlSourceType.MYBATIS_ANNOTATION) {
            return SqlCategory.MYBATIS_ANNOTATION;
        }

        if (sourceType == SqlSourceType.JPA_ANNOTATION) {
            return SqlCategory.JPA_NATIVE_QUERY;
        }

        if (sourceType == SqlSourceType.STRING_LITERAL) {
            if (content.contains("#{") || content.contains("${")) {
                return SqlCategory.PLACEHOLDER_TEMPLATE;
            }
            return SqlCategory.STRING_CONCAT;
        }

        if (content.contains("#{") || content.contains("${")) {
            return SqlCategory.PLACEHOLDER_TEMPLATE;
        }

        return SqlCategory.UNKNOWN;
    }
}
