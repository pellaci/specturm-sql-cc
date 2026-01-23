package org.spectrum.sqlchecker.domain.shared.exception;

/**
 * 扫描异常
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public class ScanException extends BaseException {

    private static final String ERROR_CODE = "SCAN_ERROR";

    public ScanException(String message) {
        super(ERROR_CODE, message);
    }

    public ScanException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }

    public ScanException(String message, Object... args) {
        super(ERROR_CODE, message, args);
    }
}
