package com.leitianyu.blue.annotation;

import java.lang.annotation.*;

/**
 *
 * @author leitianyu
 * @date 2024-01-05
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {

    //Bean name;default method name
    String value() default "";

    //init
    String initMethod() default "";

    //destroy
    String destroyMethod() default "";


}
