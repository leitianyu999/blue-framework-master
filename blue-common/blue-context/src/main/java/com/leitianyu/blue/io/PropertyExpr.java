package com.leitianyu.blue.io;

import java.util.Objects;

/**
 * 配置列
 *
 * @author leitianyu
 * @date 2024/1/7
 */
public class PropertyExpr {

    private final String key;

    private final String defaultValue;


    public PropertyExpr(String key, String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public String getKey() {
        return key;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String toString() {
        return "PropertyExpr{" +
                "key='" + key + '\'' +
                ", defaultValue='" + defaultValue + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PropertyExpr propertyExpr = (PropertyExpr) o;
        return Objects.equals(key, propertyExpr.key) &&
                Objects.equals(defaultValue, propertyExpr.defaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, defaultValue);
    }

}
