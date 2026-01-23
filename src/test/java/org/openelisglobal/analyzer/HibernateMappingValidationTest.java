package org.openelisglobal.analyzer;

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
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration;
import org.openelisglobal.analyzer.valueholder.AnalyzerError;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;
import org.openelisglobal.analyzer.valueholder.CustomFieldType;
import org.openelisglobal.analyzer.valueholder.QualitativeResultMapping;
import org.openelisglobal.analyzer.valueholder.UnitMapping;
import org.openelisglobal.analyzer.valueholder.ValidationRuleConfiguration;
import org.openelisglobal.analyzer.valueholder.SerialPortConfiguration;

/**
 * Validates Hibernate ORM mappings WITHOUT requiring database connection. This
 * test layer catches entity/mapping conflicts before integration tests.
 * 
 * Executes in <5 seconds, preventing ORM errors that would otherwise only
 * appear at application startup.
 * 
 * Reference: [Testing Roadmap - ORM Validation
 * Tests](.specify/guides/testing-roadmap.md#orm-validation-tests-constitution-v4)
 * 
 * Constitution V.4 Requirement: MUST execute in <5 seconds, MUST NOT require
 * database connection
 */
public class HibernateMappingValidationTest {

    private static SessionFactory sessionFactory;

    @BeforeClass
    public static void buildSessionFactory() {
        Configuration configuration = new Configuration();

        // Annotation-based entities (no XML entity references)
        configuration.addAnnotatedClass(AnalyzerConfiguration.class);
        configuration.addAnnotatedClass(AnalyzerError.class);
        configuration.addAnnotatedClass(CustomFieldType.class);
        configuration.addAnnotatedClass(ValidationRuleConfiguration.class);
        configuration.addAnnotatedClass(SerialPortConfiguration.class); // Task Reference: T022, M2

        // XML-mapped entities (analyzer data model uses XML-only to match legacy
        // pattern)
        configuration.addResource("hibernate/hbm/Analyzer.hbm.xml");
        configuration.addResource("hibernate/hbm/AnalyzerField.hbm.xml");
        configuration.addResource("hibernate/hbm/AnalyzerFieldMapping.hbm.xml");
        configuration.addResource("hibernate/hbm/QualitativeResultMapping.hbm.xml");
        configuration.addResource("hibernate/hbm/UnitMapping.hbm.xml");

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
     * Test that all analyzer entity Hibernate mappings load successfully Catches:
     * Property name mismatches, missing getters/setters, invalid relationships
     */
    @Test
    public void testAllAnalyzerHibernateMappingsLoadSuccessfully() {
        assertNotNull("SessionFactory should build successfully with all analyzer mappings", sessionFactory);

        // Verify each entity is registered in Hibernate metamodel
        assertNotNull("AnalyzerConfiguration should be registered",
                sessionFactory.getMetamodel().entity(AnalyzerConfiguration.class));
        assertNotNull("AnalyzerField should be registered", sessionFactory.getMetamodel().entity(AnalyzerField.class));
        assertNotNull("AnalyzerFieldMapping should be registered",
                sessionFactory.getMetamodel().entity(AnalyzerFieldMapping.class));
        assertNotNull("QualitativeResultMapping should be registered",
                sessionFactory.getMetamodel().entity(QualitativeResultMapping.class));
        assertNotNull("UnitMapping should be registered", sessionFactory.getMetamodel().entity(UnitMapping.class));
        assertNotNull("AnalyzerError should be registered", sessionFactory.getMetamodel().entity(AnalyzerError.class));
        assertNotNull("CustomFieldType should be registered",
                sessionFactory.getMetamodel().entity(CustomFieldType.class));
        assertNotNull("ValidationRuleConfiguration should be registered",
                sessionFactory.getMetamodel().entity(ValidationRuleConfiguration.class));
        assertNotNull("SerialPortConfiguration should be registered",
                sessionFactory.getMetamodel().entity(SerialPortConfiguration.class)); // Task Reference: T022, M2
    }

    /**
     * Test that analyzer entities follow JavaBean conventions Catches: Conflicting
     * getters (getActive() vs isActive())
     */
    @Test
    public void testAnalyzerEntitiesHaveNoGetterConflicts() {
        Class<?>[] entities = { AnalyzerConfiguration.class, AnalyzerField.class, AnalyzerFieldMapping.class,
                QualitativeResultMapping.class, UnitMapping.class, AnalyzerError.class, CustomFieldType.class,
                ValidationRuleConfiguration.class, SerialPortConfiguration.class }; // Task Reference: T022, M2

        for (Class<?> entityClass : entities) {
            validateNoGetterConflicts(entityClass);
        }
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
     * Wrong return types, primitive vs wrapper mismatches
     */
    @Test
    public void testEntityPropertyTypesValid() {
        // If SessionFactory built, property types are compatible
        // This is implicit validation - SessionFactory won't build if types
        // incompatible
        assertNotNull("SessionFactory validates property types", sessionFactory);
    }
}
