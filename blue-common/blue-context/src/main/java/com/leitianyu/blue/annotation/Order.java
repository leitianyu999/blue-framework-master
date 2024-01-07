package com.leitianyu.blue.annotation;

import java.lang.annotation.*;

/**
 * @author leitianyu
 * @date 2024/1/5
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Order {

    //优先级
    int value();


}
