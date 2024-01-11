package com.leitianyu.blue.web;

import com.sun.istack.internal.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public interface View {

    @Nullable
    default String getContentType() {
        return null;
    }

    void render(@Nullable Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception;
}
