package com.leitianyu.blue.context;

import com.sun.istack.internal.Nullable;

import java.util.List;

/**
 * @author leitianyu
 * @date 2024/1/7
 */
public interface ConfigurableApplicationContext extends ApplicationContext{

    /**
     * findBeanDefinitions
     */
    List<BeanDefinition> findBeanDefinitions(Class<?> type);

    /**
     * findBeanDefinition
     */
    @Nullable
    BeanDefinition findBeanDefinition(Class<?> type);

    /**
     * findBeanDefinition
     */
    @Nullable
    BeanDefinition findBeanDefinition(String name);

    /**
     * findBeanDefinition
     */
    @Nullable
    BeanDefinition findBeanDefinition(String name, Class<?> requiredType);

    /**
     * 代理类实现AoP
     */
    Object createBeanAsEarlySingleton(BeanDefinition def);

}
