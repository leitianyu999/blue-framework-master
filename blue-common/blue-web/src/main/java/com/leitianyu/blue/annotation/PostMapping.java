package com.leitianyu.blue.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PostMapping {

    /**
     * URL mapping.
     */
    String value();

}
