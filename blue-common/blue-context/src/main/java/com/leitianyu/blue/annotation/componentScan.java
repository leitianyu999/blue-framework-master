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
public @interface componentScan {


    //scan path;default current package
    String[] value() default {};



}
