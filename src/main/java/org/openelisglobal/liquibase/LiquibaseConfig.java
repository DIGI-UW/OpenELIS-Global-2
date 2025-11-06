package org.openelisglobal.liquibase;

import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiquibaseConfig {

    @Autowired
    private DataSource dataSource;

    @Value("${spring.liquibase.contexts:default}")
    private String contexts;

    @Bean("liquibase")
    public SpringLiquibase liquibase() {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setChangeLog("classpath:liquibase/base-changelog.xml");
        liquibase.setDataSource(dataSource);
        liquibase.setContexts(contexts);
        return liquibase;
    }
}
