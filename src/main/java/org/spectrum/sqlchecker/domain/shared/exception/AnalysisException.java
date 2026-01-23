package org.spectrum.sqlchecker.domain.shared.exception;

/**
 * 分析异常
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public class AnalysisException extends BaseException {

    private static final String ERROR_CODE = "ANALYSIS_ERROR";

    public AnalysisException(String message) {
        super(ERROR_CODE, message);
    }

    public AnalysisException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}
