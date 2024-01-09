package com.leitianyu.blue.jdbc.tx;

import com.sun.istack.internal.Nullable;

import java.sql.Connection;

/**
 * @author leitianyu
 * @date 2024/1/9
 */
public class TransactionalUtils {

    @Nullable
    public static Connection getCurrentConnection() {
        TransactionStatus ts = DataSourceTransactionManager.transactionStatus.get();
        return ts == null ? null : ts.connection;
    }
}
