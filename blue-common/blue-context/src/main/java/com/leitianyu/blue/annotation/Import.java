package com.leitianyu.blue.annotation;

import java.lang.annotation.*;

/**
 * @author leitianyu
 * @date 2024/1/5
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Import {

    //注入特定Bean
    Class<?>[] value();


}
