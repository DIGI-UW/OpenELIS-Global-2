package org.openelisglobal.analyzer.valueholder;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class FileImportConfigurationMappingTest {

    private static SessionFactory sessionFactory;

    @BeforeClass
    public static void setUpSessionFactory() {
        Configuration configuration = new Configuration();
        configuration.addAnnotatedClass(FileImportConfiguration.class);
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
    public void testFileImportConfigurationMapping_IncludesFileFormatField() {
        assertNotNull("FileImportConfiguration should be mapped",
                sessionFactory.getMetamodel().entity(FileImportConfiguration.class));
        assertTrue("fileFormat attribute should be present",
                sessionFactory.getMetamodel().entity(FileImportConfiguration.class).getAttributes().stream()
                        .anyMatch(attribute -> "fileFormat".equals(attribute.getName())));
    }
}
