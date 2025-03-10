package org.openelisglobal.config;

import javax.sql.DataSource;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
// import org.springframework.retry.annotation.Backoff;
// import org.springframework.retry.annotation.EnableRetry;
// import org.springframework.retry.annotation.Retryable;

@Configuration
// @EnableRetry
public class DatabaseConfig {

    @Bean(destroyMethod = "close")
    public DataSource dataSource() {
        JndiDataSourceLookup dsLookup = new JndiDataSourceLookup();
        dsLookup.setResourceRef(true);
        LogEvent.logDebug(this.getClass().getSimpleName(), "dataSource()", "creating datasource...");

        DataSource dataSource = dsLookup.getDataSource("jdbc/LimsDS");
        return dataSource;
    }

    // @Order(Ordered.HIGHEST_PRECEDENCE)
    // private class RetryableDataSourceBeanPostProcessor implements
    // BeanPostProcessor {
    // @Override
    // public Object postProcessBeforeInitialization(Object bean, String beanName)
    // throws BeansException {
    // if (bean instanceof DataSource) {
    // bean = new RetryableDataSource((DataSource) bean);
    // }
    // return bean;
    // }

    // @Override
    // public Object postProcessAfterInitialization(Object bean, String beanName)
    // throws BeansException {
    // return bean;
    // }
    // }

    // class RetryableDataSource extends AbstractDataSource {

    // private DataSource delegate;

    // public RetryableDataSource(DataSource delegate) {
    // this.delegate = delegate;
    // }

    // @Override
    // @Retryable(maxAttempts = 10, backoff = @Backoff(multiplier = 2.3, maxDelay =
    // 30000))
    // public Connection getConnection() throws SQLException {
    // LogEvent.logDebug(this.getClass().getSimpleName(), "getConnection()",
    // "attempting connection to the database...");
    // return delegate.getConnection();
    // }

    // @Override
    // @Retryable(maxAttempts = 10, backoff = @Backoff(multiplier = 2.3, maxDelay =
    // 30000))
    // public Connection getConnection(String username, String password) throws
    // SQLException {
    // return delegate.getConnection(username, password);
    // }
    // }
}
