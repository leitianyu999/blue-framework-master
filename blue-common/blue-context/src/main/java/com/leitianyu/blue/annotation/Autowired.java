package com.leitianyu.blue.annotation;


import java.lang.annotation.*;

/**
 *
 * @author leitianyu
 * @date 2024-01-05
 */
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {

    //是否强制注入
    boolean value() default true;

    //Bean name
    String name() default "";

}
