package org.spectrum.sqlchecker.domain.shared.exception;

/**
 * 数据库连接异常
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public class ConnectionException extends BaseException {

    private static final String ERROR_CODE = "CONN_ERROR";

    public ConnectionException(String message) {
        super(ERROR_CODE, message);
    }

    public ConnectionException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}
