package com.leitianyu.blue.context;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

import java.util.Objects;

/**
 * 获取ApplicationContext
 *
 * @author leitianyu
 * @date 2024/1/9
 */
public class ApplicationContextUtils {

    private static ApplicationContext applicationContext = null;

    @NotNull
    public static ApplicationContext getRequiredApplicationContext() {
        return Objects.requireNonNull(getApplicationContext(), "ApplicationContext is not set.");
    }

    @Nullable
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    static void setApplicationContext(ApplicationContext ctx) {
        applicationContext = ctx;
    }

}
