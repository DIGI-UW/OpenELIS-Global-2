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
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

@Configuration
@EnableTransactionManagement
public class BaseTestConfig {
    @Autowired
    private DataSource dataSource;

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
        PostgreSQLContainer<?> container = databaseContainer();
        startPostgreSql(container);
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(container.getJdbcUrl());
        dataSource.setUsername(container.getUsername());
        dataSource.setPassword(container.getPassword());
        System.setProperty("db.url", container.getJdbcUrl());
        System.setProperty("db.user", container.getUsername());
        System.setProperty("db.pass", container.getPassword());
        return dataSource;
    }

    @Bean
    @DependsOn("liquibase")
    @Profile("test")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        // JDBC fixture operations and Hibernate must join the same test transaction.
        emf.setDataSource(dataSource);
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        emf.setPersistenceXmlLocation("classpath:persistence/test-persistence.xml");
        return emf;
    }

    @Bean("transactionManager")
    @Primary
    @Profile("test")
    public PlatformTransactionManager getTransactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    protected PostgreSQLContainer<?> databaseContainer() {
        return postgreSqlContainer;
    }

    private void startPostgreSql(PostgreSQLContainer<?> container) {
        if (container != null && container.isRunning()) {
            return;
        }
        container.withCopyFileToContainer(MountableFile.forClasspathResource("postgre-db-init"),
                "/docker-entrypoint-initdb.d");
        container.withEnv("POSTGRES_INITDB_ARGS", "--auth-host=md5");
        container.withDatabaseName(DB_NAME);
        container.withUsername(USER);
        container.withPassword(PASSWORD);
        container.start();
    }
}
