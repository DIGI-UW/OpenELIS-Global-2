package org.openelisglobal.analyzer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerActivationRecord;
import org.openelisglobal.analyzer.valueholder.AnalyzerMapping;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingConfirmation;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;

public class AnalyzerMappingOrmValidationTest {

    @Test(timeout = 5000)
    public void siteBindingMappingsBuildWithoutDatabaseAccess() {
        Configuration configuration = new Configuration();
        configuration.addAnnotatedClass(Analyzer.class);
        configuration.addAnnotatedClass(AnalyzerActivationRecord.class);
        configuration.addAnnotatedClass(AnalyzerProfileBinding.class);
        configuration.addAnnotatedClass(AnalyzerSiteBinding.class);
        configuration.addAnnotatedClass(AnalyzerMappingConfirmation.class);
        configuration.addAnnotatedClass(AnalyzerMapping.class);
        configuration.addAnnotatedClass(AnalyzerMappingTest.class);
        configuration.addAnnotatedClass(AnalyzerMappingResult.class);
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        configuration.setProperty("hibernate.hbm2ddl.auto", "none");

        StandardServiceRegistryBuilder registry = new StandardServiceRegistryBuilder()
                .applySettings(configuration.getProperties());
        try (SessionFactory sessionFactory = configuration.buildSessionFactory(registry.build())) {
            assertNotNull(sessionFactory.getMetamodel().entity(AnalyzerSiteBinding.class));
            assertNotNull(sessionFactory.getMetamodel().entity(AnalyzerActivationRecord.class));
            assertNotNull(sessionFactory.getMetamodel().entity(AnalyzerMappingConfirmation.class));
            assertNotNull(sessionFactory.getMetamodel().entity(AnalyzerMapping.class));
            assertNotNull(sessionFactory.getMetamodel().entity(AnalyzerMappingTest.class));
            assertNotNull(sessionFactory.getMetamodel().entity(AnalyzerMappingResult.class));
            assertEquals(AnalyzerMapping.class, sessionFactory.getMetamodel().entity(Analyzer.class)
                    .getAttribute("siteBindingRevision").getJavaType());
            assertEquals(AnalyzerActivationRecord.class, sessionFactory.getMetamodel().entity(Analyzer.class)
                    .getAttribute("latestActivationRecord").getJavaType());
        }
    }
}
