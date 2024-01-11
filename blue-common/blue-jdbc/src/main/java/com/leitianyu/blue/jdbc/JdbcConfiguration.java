package com.leitianyu.blue.jdbc;

import com.leitianyu.blue.annotation.Autowired;
import com.leitianyu.blue.annotation.Bean;
import com.leitianyu.blue.annotation.Configuration;
import com.leitianyu.blue.annotation.Value;
import com.leitianyu.blue.jdbc.tx.DataSourceTransactionManager;
import com.leitianyu.blue.jdbc.tx.PlatformTransactionManager;
import com.leitianyu.blue.jdbc.tx.TransactionalBeanPostProcessor;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * @author leitianyu
 * @date 2024/1/9
 */
@Configuration
public class JdbcConfiguration {

    // 连接池建立
    @Bean(destroyMethod = "close")
    DataSource dataSource(
            // properties:
            @Value("${blue.datasource.url}") String url, //
            @Value("${blue.datasource.username}") String username, //
            @Value("${blue.datasource.password}") String password, //
            @Value("${blue.datasource.driver-class-name:}") String driver, //
            @Value("${blue.datasource.maximum-pool-size:20}") int maximumPoolSize, //
            @Value("${blue.datasource.minimum-pool-size:1}") int minimumPoolSize, //
            @Value("${blue.datasource.connection-timeout:30000}") int connTimeout //
    ) {
        HikariConfig config = new HikariConfig();
        config.setAutoCommit(false);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        if (driver != null) {
            config.setDriverClassName(driver);
        }
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumPoolSize);
        config.setConnectionTimeout(connTimeout);
        return new HikariDataSource(config);
    }

    @Bean
    JdbcTemplate jdbcTemplate(@Autowired DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    TransactionalBeanPostProcessor transactionalBeanPostProcessor() {
        return new TransactionalBeanPostProcessor();
    }

    @Bean
    PlatformTransactionManager platformTransactionManager(@Autowired DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

}
