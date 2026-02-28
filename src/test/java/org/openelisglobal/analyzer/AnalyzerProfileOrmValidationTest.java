package org.openelisglobal.analyzer;

import static org.junit.Assert.assertNotNull;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openelisglobal.analyzer.valueholder.AnalyzerLabUnit;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfile;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileApplication;

/**
 * ORM validation test for AnalyzerProfile, AnalyzerProfileApplication,
 * AnalyzerLabUnit. Must run in &lt;5s without DB per Constitution V.4.
 */
public class AnalyzerProfileOrmValidationTest {

    private static SessionFactory sessionFactory;

    @BeforeClass
    public static void buildSessionFactory() {
        Configuration configuration = new Configuration();
        configuration.addAnnotatedClass(AnalyzerProfile.class);
        configuration.addAnnotatedClass(AnalyzerProfileApplication.class);
        configuration.addAnnotatedClass(AnalyzerLabUnit.class);
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        configuration.setProperty("hibernate.hbm2ddl.auto", "none");
        sessionFactory = configuration.buildSessionFactory(
                new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build());
    }

    @AfterClass
    public static void closeSessionFactory() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Test
    public void testAnalyzerProfileMappingsLoadSuccessfully() {
        assertNotNull("AnalyzerProfile should be registered",
                sessionFactory.getMetamodel().entity(AnalyzerProfile.class));
        assertNotNull("AnalyzerProfileApplication should be registered",
                sessionFactory.getMetamodel().entity(AnalyzerProfileApplication.class));
        assertNotNull("AnalyzerLabUnit should be registered",
                sessionFactory.getMetamodel().entity(AnalyzerLabUnit.class));
    }
}
