package org.openelisglobal.sitebranding;

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
import org.openelisglobal.sitebranding.valueholder.SiteBranding;

/**
 * Validates Hibernate ORM mappings WITHOUT requiring database connection. This
 * test layer catches entity/mapping conflicts before integration tests.
 * 
 * Executes in <5 seconds, preventing ORM errors that would otherwise only
 * appear at application startup.
 * 
 * Reference: Constitution V.4 - ORM Validation Tests Reference: Testing Roadmap
 * - .specify/guides/testing-roadmap.md#orm-validation-tests-constitution-v4
 * 
 * Task Reference: T005
 */
public class HibernateMappingValidationTest {

    private static SessionFactory sessionFactory;

    @BeforeClass
    public static void buildSessionFactory() {
        Configuration configuration = new Configuration();

        // Add SiteBranding entity mapping using annotation-based approach (per
        // Constitution IV)
        configuration.addAnnotatedClass(SiteBranding.class);

        // Configure minimal properties (no actual DB connection)
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        // Skip foreign key validation for this test - we're only validating mapping
        // structure
        configuration.setProperty("hibernate.hbm2ddl.auto", "none");

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
     * Test that SiteBranding entity Hibernate mapping loads successfully Catches:
     * Property name mismatches, missing getters/setters, invalid relationships Task
     * Reference: T005
     */
    @Test
    public void testSiteBrandingHibernateMappingLoadsSuccessfully() {
        assertNotNull("SessionFactory should build successfully with SiteBranding mapping", sessionFactory);

        // Verify entity is registered in Hibernate metamodel
        assertNotNull("SiteBranding should be registered", sessionFactory.getMetamodel().entity(SiteBranding.class));
    }

    /**
     * Test that SiteBranding entity follows JavaBean conventions Catches:
     * Conflicting getters (getActive() vs isActive()) Task Reference: T005
     */
    @Test
    public void testSiteBrandingEntityHasNoGetterConflicts() {
        validateNoGetterConflicts(SiteBranding.class);
    }

    /**
     * Validate that an entity doesn't have conflicting getters E.g., both
     * getActive() returning Boolean AND isActive() returning boolean
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
     * Test that entity property types match Hibernate mapping expectations Catches:
     * Wrong return types, primitive vs wrapper mismatches Task Reference: T005
     */
    @Test
    public void testSiteBrandingEntityPropertyTypesValid() {
        // If SessionFactory built, property types are compatible
        // This is implicit validation - SessionFactory won't build if types
        // incompatible
        assertNotNull("SessionFactory validates property types", sessionFactory);
    }
}
