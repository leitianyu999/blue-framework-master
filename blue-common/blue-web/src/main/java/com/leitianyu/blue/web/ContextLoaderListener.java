package com.leitianyu.blue.web;

import com.leitianyu.blue.context.AnnotationConfigApplicationContext;
import com.leitianyu.blue.context.ApplicationContext;
import com.leitianyu.blue.exception.NestedRuntimeException;
import com.leitianyu.blue.io.PropertyResolver;
import com.leitianyu.blue.web.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Servlet监听器
 *
 * @author leitianyu
 * @date 2024/1/11 19:18
 */
public class ContextLoaderListener implements ServletContextListener {

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("init {}.", getClass().getName());
        ServletContext servletContext = sce.getServletContext();
        WebMvcConfiguration.setServletContext(servletContext);

        PropertyResolver propertyResolver = WebUtils.createPropertyResolver();
        String encoding = propertyResolver.getProperty("${summer.web.character-encoding:UTF-8}");
        servletContext.setRequestCharacterEncoding(encoding);
        servletContext.setResponseCharacterEncoding(encoding);
        // web-xml配置
        ApplicationContext applicationContext = createApplicationContext(servletContext.getInitParameter("configuration"), propertyResolver);
        // register filters:
        WebUtils.registerFilters(servletContext);
        // register DispatcherServlet:
        WebUtils.registerDispatcherServlet(servletContext, propertyResolver);

        servletContext.setAttribute("applicationContext", applicationContext);
    }

    ApplicationContext createApplicationContext(String configClassName, PropertyResolver propertyResolver) {
        logger.info("init ApplicationContext by configuration: {}", configClassName);
        if (configClassName == null || configClassName.isEmpty()) {
            throw new NestedRuntimeException("Cannot init ApplicationContext for missing init param name: configuration");
        }
        Class<?> configClass;
        try {
            configClass = Class.forName(configClassName);
        } catch (ClassNotFoundException e) {
            throw new NestedRuntimeException("Could not load class from init param 'configuration': " + configClassName);
        }
        return new AnnotationConfigApplicationContext(configClass, propertyResolver);
    }
}
