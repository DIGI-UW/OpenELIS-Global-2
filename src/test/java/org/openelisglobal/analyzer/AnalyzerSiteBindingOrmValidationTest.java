package org.openelisglobal.analyzer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingConfirmation;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerType;

public class AnalyzerSiteBindingOrmValidationTest {

    @Test(timeout = 5000)
    public void siteBindingMappingsBuildWithoutDatabaseAccess() {
        Configuration configuration = new Configuration();
        configuration.addAnnotatedClass(Analyzer.class);
        configuration.addAnnotatedClass(AnalyzerType.class);
        configuration.addAnnotatedClass(AnalyzerProfileBinding.class);
        configuration.addAnnotatedClass(AnalyzerSiteBinding.class);
        configuration.addAnnotatedClass(AnalyzerSiteBindingConfirmation.class);
        configuration.addAnnotatedClass(AnalyzerSiteBindingRevision.class);
        configuration.addAnnotatedClass(AnalyzerSiteBindingTest.class);
        configuration.addAnnotatedClass(AnalyzerSiteBindingResult.class);
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        configuration.setProperty("hibernate.hbm2ddl.auto", "none");

        StandardServiceRegistryBuilder registry = new StandardServiceRegistryBuilder()
                .applySettings(configuration.getProperties());
        try (SessionFactory sessionFactory = configuration.buildSessionFactory(registry.build())) {
            assertNotNull(sessionFactory.getMetamodel().entity(AnalyzerSiteBinding.class));
            assertNotNull(sessionFactory.getMetamodel().entity(AnalyzerSiteBindingConfirmation.class));
            assertNotNull(sessionFactory.getMetamodel().entity(AnalyzerSiteBindingRevision.class));
            assertNotNull(sessionFactory.getMetamodel().entity(AnalyzerSiteBindingTest.class));
            assertNotNull(sessionFactory.getMetamodel().entity(AnalyzerSiteBindingResult.class));
            assertEquals(AnalyzerSiteBindingRevision.class, sessionFactory.getMetamodel().entity(Analyzer.class)
                    .getAttribute("siteBindingRevision").getJavaType());
        }
    }
}
