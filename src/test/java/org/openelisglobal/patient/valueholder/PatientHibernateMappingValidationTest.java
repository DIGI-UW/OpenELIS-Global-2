package org.openelisglobal.patient.valueholder;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class PatientHibernateMappingValidationTest {

    private static SessionFactory sessionFactory;

    @BeforeClass
    public static void buildSessionFactory() {
        Configuration configuration = new Configuration();
        // Patient uses XML-based Hibernate mapping, not JPA annotations
        // Person is also needed since Patient.hbm.xml references it
        configuration.addResource("hibernate/hbm/Person.hbm.xml");
        configuration.addResource("hibernate/hbm/Patient.hbm.xml");
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        configuration.setProperty("hibernate.hbm2ddl.auto", "none");
        // Disable Hibernate Search for this test
        configuration.setProperty("hibernate.search.enabled", "false");

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
    public void testPatientHibernateMappingLoadsSuccessfully() {
        assertNotNull("SessionFactory should build successfully", sessionFactory);
        assertNotNull("Patient should be registered", sessionFactory.getMetamodel().entity(Patient.class));
    }

    @Test
    public void testPatientHasMergeTrackingFields() throws Exception {
        Patient patient = new Patient();

        Field mergedIntoField = patient.getClass().getDeclaredField("mergedIntoPatientId");
        assertNotNull("Patient should have mergedIntoPatientId field", mergedIntoField);

        Field isMergedField = patient.getClass().getDeclaredField("isMerged");
        assertNotNull("Patient should have isMerged field", isMergedField);

        Field mergeDateField = patient.getClass().getDeclaredField("mergeDate");
        assertNotNull("Patient should have mergeDate field", mergeDateField);
    }

    @Test
    public void testPatientMergeFieldsHaveGettersAndSetters() throws Exception {
        Patient patient = new Patient();

        assertNotNull("Patient should have getMergedIntoPatientId()",
                patient.getClass().getMethod("getMergedIntoPatientId"));
        assertNotNull("Patient should have setMergedIntoPatientId()",
                patient.getClass().getMethod("setMergedIntoPatientId", String.class));

        assertNotNull("Patient should have getIsMerged()", patient.getClass().getMethod("getIsMerged"));
        assertNotNull("Patient should have setIsMerged()", patient.getClass().getMethod("setIsMerged", Boolean.class));

        assertNotNull("Patient should have getMergeDate()", patient.getClass().getMethod("getMergeDate"));
        assertNotNull("Patient should have setMergeDate()",
                patient.getClass().getMethod("setMergeDate", java.sql.Timestamp.class));
    }
}
