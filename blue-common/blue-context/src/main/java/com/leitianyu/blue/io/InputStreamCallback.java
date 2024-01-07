package com.leitianyu.blue.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author leitianyu
 * @date 2024/1/7
 */
@FunctionalInterface
public interface InputStreamCallback<T> {

    T doWithInputStream(InputStream stream) throws IOException;

}
