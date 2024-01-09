package com.leitianyu.blue.exception;

/**
 * aop相关报错
 *
 * @author leitianyu
 * @date 2024/1/9
 */
public class DataAccessException extends NestedRuntimeException {

    public DataAccessException() {
    }

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(Throwable cause) {
        super(cause);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
