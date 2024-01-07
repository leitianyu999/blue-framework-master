package com.leitianyu.blue.exception;



/**
 *
 * @author leitianyu
 * @date 2024-01-05
 */
public class BeanNotOfRequiredTypeException extends BeansException {

    public BeanNotOfRequiredTypeException() {
    }

    public BeanNotOfRequiredTypeException(String message) {
        super(message);
    }
}
