package com.leitianyu.blue.annotation;

import java.lang.annotation.*;

/**
 * @author leitianyu
 * @date 2024/1/11
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Controller {

    // 网页导航
    String value() default "";


}
