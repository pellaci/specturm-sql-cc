package org.spectrum.sqlchecker.infrastructure.preprocess;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.spectrum.sqlchecker.domain.preprocess.service.SqlValidator;
import org.springframework.stereotype.Component;

/**
 * 基于 JSqlParser 的校验器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Component
public class JSqlParserValidator implements SqlValidator {

    @Override
    public ValidationResult validate(String sql) {
        if (sql == null || sql.isBlank()) {
            return new ValidationResult(false, "SQL 为空");
        }
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement == null) {
                return new ValidationResult(false, "SQL 无法解析");
            }
            return new ValidationResult(true, null);
        } catch (Exception ex) {
            return new ValidationResult(false, ex.getMessage());
        }
    }
}
