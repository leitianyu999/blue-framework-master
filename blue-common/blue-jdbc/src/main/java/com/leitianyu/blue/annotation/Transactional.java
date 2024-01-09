package com.leitianyu.blue.annotation;

import java.lang.annotation.*;

/**
 * @author leitianyu
 * @date 2024/1/9
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Transactional {

    String value() default "platformTransactionManager";

}
