package com.leitianyu.blue.web.utils;

import com.leitianyu.blue.context.ApplicationContext;
import com.leitianyu.blue.context.ApplicationContextUtils;
import com.leitianyu.blue.io.PropertyResolver;
import com.leitianyu.blue.utils.ClassPathUtils;
import com.leitianyu.blue.utils.YamlUtils;
import com.leitianyu.blue.web.DispatcherServlet;
import com.leitianyu.blue.web.FilterRegistrationBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.util.*;

public class WebUtils {

    public static final String DEFAULT_PARAM_VALUE = "\0\t\0\t\0";

    static final Logger logger = LoggerFactory.getLogger(WebUtils.class);

    static final String CONFIG_APP_YAML = "/application.yml";
    static final String CONFIG_APP_PROP = "/application.properties";

    public static void registerDispatcherServlet(ServletContext servletContext, PropertyResolver properyResolver) {
        DispatcherServlet dispatcherServlet = new DispatcherServlet(ApplicationContextUtils.getRequiredApplicationContext(), properyResolver);
        logger.info("register servlet {} for URL '/'", dispatcherServlet.getClass().getName());
        // 注入dispatcherServlet
        javax.servlet.ServletRegistration.Dynamic dispatcherReg = servletContext.addServlet("dispatcherServlet", dispatcherServlet);
        dispatcherReg.addMapping("/");
        dispatcherReg.setLoadOnStartup(0);
    }

    public static void registerFilters(ServletContext servletContext) {
        ApplicationContext applicationContext = ApplicationContextUtils.getRequiredApplicationContext();
        // 已经按order排序好
        for (FilterRegistrationBean filterRegBean : applicationContext.getBeans(FilterRegistrationBean.class)) {
            List<String> urlPatterns = filterRegBean.getUrlPatterns();
            if (urlPatterns == null || urlPatterns.isEmpty()) {
                throw new IllegalArgumentException("No url patterns for {}" + filterRegBean.getClass().getName());
            }
            Filter filter = Objects.requireNonNull(filterRegBean.getFilter(), "FilterRegistrationBean.getFilter() must not return null.");
            logger.info("register filter '{}' {} for URLs: {}", filterRegBean.getName(), filter.getClass().getName(), String.join(", ", urlPatterns));
            FilterRegistration.Dynamic filterReg = servletContext.addFilter(filterRegBean.getName(), filter);
            filterReg.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, String.join("", urlPatterns));
        }
    }

    /**
     * Try load property resolver from /application.yml or /application.properties.
     */
    public static PropertyResolver createPropertyResolver() {
        final Properties props = new Properties();
        // try load application.yml:
        try {
            Map<String, Object> ymlMap = YamlUtils.loadYamlAsPlainMap(CONFIG_APP_YAML);
            logger.info("load config: {}", CONFIG_APP_YAML);
            for (String key : ymlMap.keySet()) {
                Object value = ymlMap.get(key);
                if (value instanceof String) {
                    String strValue = (String)value;
                    props.put(key, strValue);
                }
            }
        } catch (UncheckedIOException e) {
            if (e.getCause() instanceof FileNotFoundException) {
                // try load application.properties:
                ClassPathUtils.readInputStream(CONFIG_APP_PROP, (input) -> {
                    logger.info("load config: {}", CONFIG_APP_PROP);
                    props.load(input);
                    return true;
                });
            }
        }
        return new PropertyResolver(props);
    }
}
