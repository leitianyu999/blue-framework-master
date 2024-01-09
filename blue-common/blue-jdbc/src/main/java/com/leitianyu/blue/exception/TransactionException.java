package com.leitianyu.blue.exception;

/**
 * aop相关报错
 *
 * @author leitianyu
 * @date 2024/1/9
 */
public class TransactionException extends DataAccessException {

    public TransactionException() {
    }

    public TransactionException(String message) {
        super(message);
    }

    public TransactionException(Throwable cause) {
        super(cause);
    }

    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }

}
