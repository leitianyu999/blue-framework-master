package com.leitianyu.blue.jdbc.tx;

import java.sql.Connection;

/**
 * @author leitianyu
 * @date 2024/1/9
 */
public class TransactionStatus {

    final Connection connection;


    public TransactionStatus(Connection connection) { this.connection = connection; }

}
