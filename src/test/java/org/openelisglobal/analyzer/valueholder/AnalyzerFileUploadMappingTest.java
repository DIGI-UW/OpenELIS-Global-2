package org.openelisglobal.analyzer.valueholder;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * ORM validation for AnalyzerFileUpload and AnalyzerRun (M2). Verifies mappings
 * load without database.
 */
public class AnalyzerFileUploadMappingTest {

    private static SessionFactory sessionFactory;

    @BeforeClass
    public static void setUpSessionFactory() {
        Configuration configuration = new Configuration();
        configuration.addAnnotatedClass(AnalyzerFileUpload.class);
        configuration.addAnnotatedClass(AnalyzerRun.class);
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        configuration.setProperty("hibernate.hbm2ddl.auto", "none");

        sessionFactory = configuration.buildSessionFactory(
                new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build());
    }

    @AfterClass
    public static void tearDownSessionFactory() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Test
    public void testAnalyzerFileUploadMapping_Loads() {
        assertNotNull("AnalyzerFileUpload should be mapped",
                sessionFactory.getMetamodel().entity(AnalyzerFileUpload.class));
        assertTrue("AnalyzerFileUpload should have fileHashSha256",
                sessionFactory.getMetamodel().entity(AnalyzerFileUpload.class).getAttributes().stream()
                        .anyMatch(a -> "fileHashSha256".equals(a.getName())));
    }

    @Test
    public void testAnalyzerRunMapping_Loads() {
        assertNotNull("AnalyzerRun should be mapped", sessionFactory.getMetamodel().entity(AnalyzerRun.class));
        assertTrue("AnalyzerRun should have customPreviewData", sessionFactory.getMetamodel().entity(AnalyzerRun.class)
                .getAttributes().stream().anyMatch(a -> "customPreviewData".equals(a.getName())));
    }
}
