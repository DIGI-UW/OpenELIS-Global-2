package org.openelisglobal.config;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import javax.sql.DataSource;
import org.hibernate.cfg.AvailableSettings;
import org.openelisglobal.module.runtime.persistence.ModuleEntityRegistrar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class HibernateConfig {

    static JpaTransactionManager transactionManager;
    static LocalContainerEntityManagerFactoryBean emf;

    @Autowired
    private DataSource dataSource;

    /**
     * Built after the core and module changelogs have run. Module entities join the
     * single persistence unit through the registrar, and Hibernate loads them
     * through the module class loaders ({@code hibernate.classLoaders}); with no
     * modules installed both are empty and nothing changes.
     */
    @Bean
    @DependsOn({ "liquibase", "moduleLiquibase" })
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(ModuleEntityRegistrar moduleEntityRegistrar) {
        if (emf == null) {
            emf = new LocalContainerEntityManagerFactoryBean();
            emf.setDataSource(dataSource);
            emf.setPersistenceXmlLocation("classpath:persistence/persistence.xml");
            emf.setPersistenceUnitPostProcessors(moduleEntityRegistrar);
            List<ClassLoader> moduleClassLoaders = moduleEntityRegistrar.classLoaders();
            if (!moduleClassLoaders.isEmpty()) {
                emf.getJpaPropertyMap().put(AvailableSettings.CLASSLOADERS, moduleClassLoaders);
            }
            // activate this once we migrate away from hbm.xmls and persistence.xml
            // emf.setPackagesToScan("org.openelisglobal");
        }

        return emf;
    }

    @Bean("transactionManager")
    @Primary
    public PlatformTransactionManager getTransactionManager(EntityManagerFactory entityManagerFactory) {
        if (transactionManager == null) {
            transactionManager = new JpaTransactionManager();
            transactionManager.setEntityManagerFactory(entityManagerFactory);
        }
        return transactionManager;
    }
}
