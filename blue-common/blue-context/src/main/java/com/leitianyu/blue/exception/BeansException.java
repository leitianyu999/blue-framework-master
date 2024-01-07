package com.leitianyu.blue.exception;



/**
 *
 * @author leitianyu
 * @date 2024-01-05
 */
public class BeansException extends NestedRuntimeException {

    public BeansException() {
    }

    public BeansException(String message) {
        super(message);
    }

    public BeansException(Throwable cause) {
        super(cause);
    }

    public BeansException(String message, Throwable cause) {
        super(message, cause);
    }
}
