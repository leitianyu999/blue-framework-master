package com.leitianyu.blue.jdbc;

import com.sun.istack.internal.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface RowMapper<T> {

    @Nullable
    T mapRow(ResultSet rs, int rowNum) throws SQLException;

}
