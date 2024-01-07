package com.leitianyu.blue.exception;


/**
 *
 * @author leitianyu
 * @date 2024-01-05
 */
public class BeanCreationException extends BeansException {

    public BeanCreationException() {
    }

    public BeanCreationException(String message) {
        super(message);
    }

    public BeanCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeanCreationException(Throwable cause) {
        super(cause);
    }
}
