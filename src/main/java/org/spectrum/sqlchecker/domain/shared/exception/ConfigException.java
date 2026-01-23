package org.spectrum.sqlchecker.domain.shared.exception;

/**
 * 配置异常
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public class ConfigException extends BaseException {

    private static final String ERROR_CODE = "CONFIG_ERROR";

    public ConfigException(String message) {
        super(ERROR_CODE, message);
    }

    public ConfigException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}
