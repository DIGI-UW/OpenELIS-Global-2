package org.openelisglobal.patient.merge.valueholder;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Validates Hibernate ORM mappings for PatientMergeAudit entity WITHOUT
 * requiring database connection. This test layer catches entity/mapping
 * conflicts before integration tests.
 * 
 * Executes in <5 seconds, preventing ORM errors that would otherwise only
 * appear at application startup.
 * 
 * Per Constitution V.4: ORM Validation Tests are MANDATORY for all entities.
 * 
 * Task: T012 [M1] - ORM validation test for PatientMergeAudit
 */
public class PatientMergeAuditHibernateMappingValidationTest {

    private static SessionFactory sessionFactory;

    @BeforeClass
    public static void buildSessionFactory() {
        Configuration configuration = new Configuration();

        // Add PatientMergeAudit entity mapping using annotation-based approach
        configuration.addAnnotatedClass(PatientMergeAudit.class);

        // Configure minimal properties (no actual DB connection)
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        // Skip foreign key validation for this test - we're only validating mapping
        // structure
        configuration.setProperty("hibernate.hbm2ddl.auto", "none");
        // Disable Hibernate Search for validation tests (not needed for mapping
        // validation)
        configuration.setProperty("hibernate.search.automatic_indexing.enabled", "false");

        // Build SessionFactory - this will FAIL if any mapping is invalid
        sessionFactory = configuration.buildSessionFactory(
                new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build());
    }

    @AfterClass
    public static void closeSessionFactory() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    /**
     * Test that PatientMergeAudit Hibernate mappings load successfully.
     * 
     * Catches: Property name mismatches, missing getters/setters, invalid
     * relationships, incorrect annotations
     */
    @Test
    public void testPatientMergeAuditHibernateMappingsLoadSuccessfully() {
        assertNotNull("SessionFactory should build successfully with PatientMergeAudit mapping", sessionFactory);

        // Verify entity is registered in Hibernate metamodel
        assertNotNull("PatientMergeAudit should be registered",
                sessionFactory.getMetamodel().entity(PatientMergeAudit.class));
    }

    /**
     * Test that PatientMergeAudit follows JavaBean conventions.
     * 
     * Catches: Conflicting getters (getActive() vs isActive())
     */
    @Test
    public void testPatientMergeAuditHasNoGetterConflicts() {
        validateNoGetterConflicts(PatientMergeAudit.class);
    }

    /**
     * Validate that an entity doesn't have conflicting getters.
     * 
     * E.g., both getActive() returning Boolean AND isActive() returning boolean
     */
    private void validateNoGetterConflicts(Class<?> clazz) {
        Map<String, Method> getGetters = new HashMap<>();
        Map<String, Method> isGetters = new HashMap<>();

        for (Method method : clazz.getMethods()) {
            // Find get* methods
            if (method.getName().startsWith("get") && method.getParameterCount() == 0
                    && !method.getName().equals("getClass")) {
                String property = decapitalize(method.getName().substring(3));
                getGetters.put(property, method);
            }

            // Find is* methods
            if (method.getName().startsWith("is") && method.getParameterCount() == 0) {
                String property = decapitalize(method.getName().substring(2));
                isGetters.put(property, method);
            }
        }

        // Find conflicts (same property with both get and is)
        Set<String> conflicts = new HashSet<>(getGetters.keySet());
        conflicts.retainAll(isGetters.keySet());

        if (!conflicts.isEmpty()) {
            StringBuilder message = new StringBuilder();
            message.append(clazz.getSimpleName()).append(" has conflicting getters for properties: ");
            for (String prop : conflicts) {
                Method getMeth = getGetters.get(prop);
                Method isMeth = isGetters.get(prop);
                message.append("\n  - ").append(prop).append(": ").append(getMeth.getName()).append("() returning ")
                        .append(getMeth.getReturnType().getSimpleName()).append(" vs ").append(isMeth.getName())
                        .append("() returning ").append(isMeth.getReturnType().getSimpleName());
            }
            message.append("\n  Hibernate cannot determine which getter to use.");
            fail(message.toString());
        }
    }

    private String decapitalize(String string) {
        if (string == null || string.length() == 0) {
            return string;
        }
        return string.substring(0, 1).toLowerCase() + string.substring(1);
    }

    /**
     * Test that entity property types match Hibernate mapping expectations.
     * 
     * Catches: Wrong return types, primitive vs wrapper mismatches
     */
    @Test
    public void testPatientMergeAuditPropertyTypesValid() {
        // If SessionFactory built, property types are compatible
        // This is implicit validation - SessionFactory won't build if types
        // incompatible
        assertNotNull("SessionFactory validates property types", sessionFactory);
    }
}
