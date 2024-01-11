package com.leitianyu.blue.web;

import java.util.Objects;

/**
 * @author leitianyu
 * @date 2023/12/28
 */
public class Result {

    private final Boolean processed;
    private final Object returnObject;

    public Result(Boolean processed, Object returnObject) {
        this.processed = processed;
        this.returnObject = returnObject;
    }

    public Boolean getProcessed() {
        return processed;
    }

    public Object getReturnObject() {
        return returnObject;
    }

    @Override
    public String toString() {
        return "Result{" +
                "processed=" + processed +
                ", returnObject=" + returnObject +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Result result = (Result) o;
        return Objects.equals(processed, result.processed) && Objects.equals(returnObject, result.returnObject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processed, returnObject);
    }
}
