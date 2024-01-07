package com.leitianyu.blue.annotation;

import java.lang.annotation.*;

/**
 *
 * @author leitianyu
 * @date 2024-01-05
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Component {

    //bean name;default class name
    String value() default "";

}
