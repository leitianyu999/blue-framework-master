package com.leitianyu.blue.aop;

import com.leitianyu.blue.context.*;
import com.leitianyu.blue.exception.AopConfigException;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * @author leitianyu
 * @date 2024/1/9
 */
public class AnnotationProxyBeanPostProcessor<A extends Annotation> implements BeanPostProcessor {

    Map<String, Object> originBeans = new HashMap<>();
    Class<A> annotationClass;
    public AnnotationProxyBeanPostProcessor() {
        this.annotationClass = getParameterizedType();
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        Class<?> beanClass = bean.getClass();

        // has class-level @Annotation?
        A anno = beanClass.getAnnotation(annotationClass);
        if (anno != null) {
            String handlerName;
            try {
                handlerName = (String) anno.annotationType().getMethod("value").invoke(anno);
            } catch (ReflectiveOperationException e) {
                throw new AopConfigException(String.format("@%s must have value() returned String type.", this.annotationClass.getSimpleName()), e);
            }
            Object proxy = createProxy(beanClass, bean, handlerName);
            originBeans.put(beanName, bean);
            return proxy;
        } else {
            return bean;
        }
    }


    Object createProxy(Class<?> beanClass, Object bean, String handlerName) {
        ConfigurableApplicationContext context = (ConfigurableApplicationContext)ApplicationContextUtils.getRequiredApplicationContext();

        BeanDefinition def = context.findBeanDefinition(handlerName);
        if (def == null) {
            throw new AopConfigException(String.format("@%s proxy handler '%s' not found.", this.annotationClass.getSimpleName(), handlerName));
        }

        Object handlerBean = def.getInstance();
        if (handlerBean == null) {
            handlerBean = context.createBeanAsEarlySingleton(def);
        }
        if (handlerBean instanceof InvocationHandler) {
            InvocationHandler handler =  (InvocationHandler)handlerBean;
            return ProxyResolver.getInstance().createProxy(bean, handler);
        } else {
            throw new AopConfigException(String.format("@%s proxy handler '%s' is not type of %s.", this.annotationClass.getSimpleName(), handlerName,
                    InvocationHandler.class.getName()));
        }
    }


    @Override
    public Object postProcessOnSetProperty(Object bean, String beanName) {
        Object origin = this.originBeans.get(beanName);
        return origin != null ? origin : bean;
    }

    /**
     * 获取注解
     *
     * @author leitianyu
     * @date 2024/1/9 16:33
     */
    @SuppressWarnings("unchecked")
    private Class<A> getParameterizedType() {
        Type type = getClass().getGenericSuperclass();
        if (!(type instanceof ParameterizedType)) {
            throw new IllegalArgumentException("Class " + getClass().getName() + " does not have parameterized type.");
        }
        ParameterizedType pt = (ParameterizedType) type;
        Type[] types = pt.getActualTypeArguments();
        if (types.length != 1) {
            throw new IllegalArgumentException("Class " + getClass().getName() + " has more than 1 parameterized types.");
        }
        Type r = types[0];
        if (!(r instanceof Class<?>)) {
            throw new IllegalArgumentException("Class " + getClass().getName() + " does not have parameterized type of class.");
        }
        return (Class<A>) r;
    }

}
