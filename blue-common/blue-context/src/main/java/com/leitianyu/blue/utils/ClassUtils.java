package com.leitianyu.blue.utils;

import com.leitianyu.blue.annotation.Bean;
import com.leitianyu.blue.annotation.Component;
import com.leitianyu.blue.exception.BeanDefinitionException;
import com.sun.istack.internal.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author leitianyu
 * @date 2024/1/7
 */
public class ClassUtils {


    /**
     * 递归查找Annotation
     *
     * @author leitianyu
     * @date 2024-01-07
     */
    public static <A extends Annotation> A findAnnotation(Class<?> target, Class<A> annoClass) {
        A targetAnnotation = target.getAnnotation(annoClass);
        for (Annotation anno : target.getAnnotations()) {
            Class<? extends Annotation> annoType = anno.annotationType();
            if (!annoType.getPackage().getName().equals("java.lang.annotation")) {
                A found = findAnnotation(annoType, annoClass);
                if (found != null) {
                    if (targetAnnotation != null) {
                        throw new BeanDefinitionException("Duplicate @" + annoClass.getSimpleName() + " found on class " + target.getSimpleName());
                    }
                    targetAnnotation = found;
                }
            }
        }
        return targetAnnotation;
    }


    /**
     * 获取Annotation
     *
     * @author leitianyu
     * @date 2024/1/7 14:37
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A getAnnotation(Annotation[] annos, Class<A> annoClass) {
        for (Annotation anno : annos) {
            if (annoClass.isInstance(anno)) {
                return (A) anno;
            }
        }
        return null;
    }


    /**
     * get bean name
     *
     * @author leitianyu
     * @date 2024/1/7 14:39
     */
    public static String getBeanName(Method method) {
        Bean bean = method.getAnnotation(Bean.class);
        String name = bean.value();
        if (name.isEmpty()) {
            name = method.getName();
        }
        return name;
    }

    /**
     * get bean name
     *
     * @author leitianyu
     * @date 2024/1/7 14:39
     */
    public static String getBeanName(Class<?> clazz) {
        String name = "";
        Component component = clazz.getAnnotation(Component.class);
        if (component != null) {
            //找到component
            name = component.value();
        } else {
            //未找到component
            for (Annotation anno : clazz.getAnnotations()) {
                if (findAnnotation(anno.annotationType(), Component.class) != null) {
                    try {
                        name = (String) anno.annotationType().getMethod("value").invoke(anno);
                    } catch (ReflectiveOperationException e) {
                        throw new BeanDefinitionException("Cannot get annotation value.", e);
                    }
                }
            }
        }

        if (name.isEmpty()) {
            //首字母小写
            name = clazz.getSimpleName();
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }

        return name;
    }


    /**
     * get method by @PostConstruct or @PreDestroy
     * Not search in super
     *
     * @author leitianyu
     * @date 2024/1/7 14:39
     */
    @Nullable
    public static Method findAnnotationMethod(Class<?> clazz, Class<? extends  Annotation> annoClass) {
        List<Method> methodList = Arrays.stream(clazz.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(annoClass)).map(m -> {
            if (m.getParameterCount() != 0) {
                throw new BeanDefinitionException(
                        String.format("Method '%s' with @%s must not have argument: %s", m.getName(), annoClass.getSimpleName(), clazz.getName()));
            }
            return m;
        }).collect(Collectors.toList());

        if (methodList.isEmpty()) {
            return null;
        }
        if (methodList.size() == 1) {
            return methodList.get(0);
        }
        throw new BeanDefinitionException(String.format("Multiple methods with @%s found in class: %s", annoClass.getSimpleName(), clazz.getName()));
    }


    /**
     * get method by methodName
     *
     * @author leitianyu
     * @date 2024/1/7 14:39
     */
    public static Method getNamedMethod(Class<?> clazz, String methodName) {
        try {
            return clazz.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            throw new BeanDefinitionException(String.format("Method '%s' not found in class: %s", methodName, clazz.getName()));
        }
    }


}
