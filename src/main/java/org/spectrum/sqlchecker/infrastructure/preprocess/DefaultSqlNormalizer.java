package org.spectrum.sqlchecker.infrastructure.preprocess;

import org.spectrum.sqlchecker.domain.preprocess.service.SqlNormalizer;
import org.springframework.stereotype.Component;

/**
 * 默认 SQL 规范化器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Component
public class DefaultSqlNormalizer implements SqlNormalizer {

    @Override
    public String normalize(String sql) {
        if (sql == null) {
            return null;
        }
        return sql.replaceAll("\\s+", " ").trim();
    }
}
