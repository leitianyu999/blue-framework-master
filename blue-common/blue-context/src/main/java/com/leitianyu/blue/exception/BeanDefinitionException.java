package com.leitianyu.blue.exception;



/**
 *
 * @author leitianyu
 * @date 2024-01-05
 */
public class BeanDefinitionException extends BeansException {

    public BeanDefinitionException() {
    }

    public BeanDefinitionException(String message) {
        super(message);
    }

    public BeanDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeanDefinitionException(Throwable cause) {
        super(cause);
    }
}
