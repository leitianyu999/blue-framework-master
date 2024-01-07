package com.leitianyu.blue.annotation;

import java.lang.annotation.*;

/**
 * @author leitianyu
 * @date 2024/1/5
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Configuration {

    //Bean name;default class name
    String value() default "";

}
