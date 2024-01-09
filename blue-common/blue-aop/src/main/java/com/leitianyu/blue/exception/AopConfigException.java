package com.leitianyu.blue.exception;

/**
 * aop相关报错
 *
 * @author leitianyu
 * @date 2024/1/9
 */
public class AopConfigException extends NestedRuntimeException{

    public AopConfigException() {
        super();
    }

    public AopConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public AopConfigException(String message) {
        super(message);
    }

    public AopConfigException(Throwable cause) {
        super(cause);
    }

}
