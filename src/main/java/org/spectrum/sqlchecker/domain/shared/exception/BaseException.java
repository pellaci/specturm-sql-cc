package org.spectrum.sqlchecker.domain.shared.exception;

import lombok.Getter;

/**
 * 基础异常
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Getter
public abstract class BaseException extends RuntimeException {

    private final String errorCode;
    private final Object[] args;

    /**
     * 构造异常
     *
     * @param errorCode 错误码
     * @param message 错误消息
     * @param args 参数
     */
    protected BaseException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    /**
     * 构造异常（带原因）
     *
     * @param errorCode 错误码
     * @param message 错误消息
     * @param cause 原因
     * @param args 参数
     */
    protected BaseException(String errorCode, String message, Throwable cause, Object... args) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = args;
    }
}
