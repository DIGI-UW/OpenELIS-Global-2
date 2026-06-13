package org.openelisglobal;

import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

@Configuration
@EnableTransactionManagement
public class BaseTestConfig {
    @Autowired
    private DataSource dataSource;

    static LocalContainerEntityManagerFactoryBean emf;
    static JpaTransactionManager transactionManager;

    private static final String PASSWORD = "clinlims";

    private static final String USER = "clinlims";

    private static final String DB_NAME = "clinlims";

    @SuppressWarnings("rawtypes")
    private static PostgreSQLContainer postgreSqlContainer = new PostgreSQLContainer("postgres:14.4");

    @Bean("liquibase")
    @Profile("test")
    public SpringLiquibase testLiquibase() {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setChangeLog("classpath:liquibase/base-changelog.xml");
        liquibase.setDataSource(dataSource);
        liquibase.setContexts("test");
        return liquibase;
    }

    @Bean
    @Profile("test")
    public DataSource testDataSource() throws IOException {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        startPostgreSql();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(postgreSqlContainer.getJdbcUrl());
        dataSource.setUsername(postgreSqlContainer.getUsername());
        dataSource.setPassword(postgreSqlContainer.getPassword());
        // Safety net for the per-test transactional fixture loads (#3711): if any
        // connection waits more than 20s on a table lock, fail fast with a legible
        // "lock timeout" error instead of hanging the whole suite. Passed as a libpq
        // startup option so it applies to EVERY connection from this DataSource
        // (Hibernate, DBUnit, and the ensure*/async helpers alike).
        java.util.Properties connectionProps = new java.util.Properties();
        connectionProps.setProperty("options", "-c lock_timeout=20000");
        dataSource.setConnectionProperties(connectionProps);
        System.setProperty("db.url", postgreSqlContainer.getJdbcUrl());
        System.setProperty("db.user", postgreSqlContainer.getUsername());
        System.setProperty("db.pass", postgreSqlContainer.getPassword());
        return dataSource;
    }

    @Bean
    @DependsOn("liquibase")
    @Profile("test")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        if (emf == null) {
            emf = new LocalContainerEntityManagerFactoryBean();
            // Bind Hibernate to the Spring-managed DataSource (mirrors the main
            // HibernateConfig) so the JPA/Hibernate session, services-under-test,
            // and DBUnit fixture loads all share ONE connection per test
            // transaction — the prerequisite for per-test rollback isolation.
            emf.setDataSource(dataSource);
            emf.setPersistenceXmlLocation("classpath:persistence/test-persistence.xml");
        }
        return emf;
    }

    @Bean("transactionManager")
    @Primary
    @Profile("test")
    public PlatformTransactionManager getTransactionManager(EntityManagerFactory entityManagerFactory) {
        if (transactionManager == null) {
            transactionManager = new JpaTransactionManager();
            transactionManager.setEntityManagerFactory(entityManagerFactory);
            // Expose the DataSource on the tx manager so it registers the bound
            // connection with TransactionSynchronizationManager — this is what
            // makes DataSourceUtils.getConnection(dataSource) return the SAME
            // connection the JPA transaction owns, letting DBUnit fixtures load
            // inside (and roll back with) the test transaction.
            transactionManager.setDataSource(dataSource);
        }
        return transactionManager;
    }

    private void startPostgreSql() {
        if (postgreSqlContainer != null && postgreSqlContainer.isRunning()) {
            return;
        }
        postgreSqlContainer.withCopyFileToContainer(MountableFile.forClasspathResource("postgre-db-init"),
                "/docker-entrypoint-initdb.d");
        postgreSqlContainer.withEnv("POSTGRES_INITDB_ARGS", "--auth-host=md5");
        postgreSqlContainer.withDatabaseName(DB_NAME);
        postgreSqlContainer.withUsername(USER);
        postgreSqlContainer.withPassword(PASSWORD);
        postgreSqlContainer.start();
    }
}
