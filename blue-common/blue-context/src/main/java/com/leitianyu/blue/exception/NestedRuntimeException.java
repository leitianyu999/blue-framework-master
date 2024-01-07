package com.leitianyu.blue.exception;



/**
 *
 * @author leitianyu
 * @date 2024-01-05
 */
public class NestedRuntimeException extends RuntimeException {

    public NestedRuntimeException() {
    }

    public NestedRuntimeException(String message) {
        super(message);
    }

    public NestedRuntimeException(Throwable cause) {
        super(cause);
    }

    public NestedRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
