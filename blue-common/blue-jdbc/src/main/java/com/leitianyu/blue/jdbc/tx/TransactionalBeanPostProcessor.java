package com.leitianyu.blue.jdbc.tx;

import com.leitianyu.blue.annotation.Transactional;
import com.leitianyu.blue.aop.AnnotationProxyBeanPostProcessor;

/**
 * @author leitianyu
 * @date 2024/1/9
 */
public class TransactionalBeanPostProcessor extends AnnotationProxyBeanPostProcessor<Transactional> {
}
