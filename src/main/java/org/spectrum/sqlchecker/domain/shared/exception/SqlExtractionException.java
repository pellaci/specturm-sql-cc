package org.spectrum.sqlchecker.domain.shared.exception;

/**
 * SQL 提取异常
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public class SqlExtractionException extends BaseException {

    private static final String ERROR_CODE = "SQL_EXTRACTION_ERROR";

    public SqlExtractionException(String message) {
        super(ERROR_CODE, message);
    }

    public SqlExtractionException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}
