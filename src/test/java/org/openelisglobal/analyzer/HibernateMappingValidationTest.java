package org.openelisglobal.analyzer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

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
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerError;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;
import org.openelisglobal.analyzer.valueholder.AnalyzerType;
import org.openelisglobal.analyzer.valueholder.CustomFieldType;
import org.openelisglobal.analyzer.valueholder.FileImportConfiguration;
import org.openelisglobal.analyzer.valueholder.QualitativeResultMapping;
import org.openelisglobal.analyzer.valueholder.SerialPortConfiguration;
import org.openelisglobal.analyzer.valueholder.UnitMapping;
import org.openelisglobal.analyzer.valueholder.ValidationRuleConfiguration;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;

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
        configuration.addAnnotatedClass(Analyzer.class); // Migrated in Phase 1
        configuration.addAnnotatedClass(AnalyzerType.class); // Type/Instance separation
        configuration.addAnnotatedClass(AnalyzerField.class); // Migrated in Phase 2A
        configuration.addAnnotatedClass(AnalyzerResults.class); // Migrated in Phase 2B
        configuration.addAnnotatedClass(AnalyzerTestMapping.class); // Migrated in Phase 2C
        configuration.addAnnotatedClass(AnalyzerFieldMapping.class); // Migrated in Phase 3
        // AnalyzerConfiguration removed: merged into Analyzer entity
        configuration.addAnnotatedClass(AnalyzerError.class);
        configuration.addAnnotatedClass(CustomFieldType.class);
        configuration.addAnnotatedClass(FileImportConfiguration.class);
        configuration.addAnnotatedClass(ValidationRuleConfiguration.class);
        configuration.addAnnotatedClass(SerialPortConfiguration.class); //
        configuration.addAnnotatedClass(QualitativeResultMapping.class); // Migrated to annotations
        configuration.addAnnotatedClass(UnitMapping.class); // Migrated to annotations

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
    public void testAnalyzerMappingsLoadSuccessfully() {
        // Verify each entity is registered in Hibernate metamodel
        assertNotNull("Analyzer should be registered", sessionFactory.getMetamodel().entity(Analyzer.class)); // Phase 1
        assertNotNull("AnalyzerType should be registered", sessionFactory.getMetamodel().entity(AnalyzerType.class));
        // AnalyzerConfiguration removed: merged into Analyzer entity
        assertNotNull("AnalyzerField should be registered", sessionFactory.getMetamodel().entity(AnalyzerField.class));
        assertNotNull("AnalyzerResults should be registered",
                sessionFactory.getMetamodel().entity(AnalyzerResults.class)); // Phase 2B
        assertNotNull("AnalyzerTestMapping should be registered",
                sessionFactory.getMetamodel().entity(AnalyzerTestMapping.class)); // Phase 2C
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
                sessionFactory.getMetamodel().entity(SerialPortConfiguration.class)); //
        assertNotNull("FileImportConfiguration should be registered",
                sessionFactory.getMetamodel().entity(FileImportConfiguration.class)); //
    }

    /**
     * Test that analyzer entities follow JavaBean conventions Catches: Conflicting
     * getters (getActive() vs isActive()) within a SINGLE entity.
     * 
     * Note: Hibernate requires consistent getter conventions per-entity, but
     * different entities can use different conventions.
     */
    @Test
    public void testAnalyzerEntitiesHaveNoGetterConflicts() {
        Class<?>[] entities = { Analyzer.class, AnalyzerType.class, AnalyzerField.class, AnalyzerResults.class,
                AnalyzerTestMapping.class, AnalyzerFieldMapping.class, QualitativeResultMapping.class,
                UnitMapping.class, AnalyzerError.class, CustomFieldType.class, ValidationRuleConfiguration.class,
                SerialPortConfiguration.class, FileImportConfiguration.class };

        for (Class<?> entityClass : entities) {
            // Check each entity independently for getter conflicts
            Map<String, Set<String>> getterMap = new HashMap<>();

            for (Method method : entityClass.getMethods()) {
                String methodName = method.getName();
                if (methodName.startsWith("get") && method.getParameterCount() == 0) {
                    String propertyName = methodName.substring(3);
                    getterMap.computeIfAbsent(propertyName, k -> new HashSet<>()).add("get" + propertyName);
                } else if (methodName.startsWith("is") && method.getParameterCount() == 0
                        && method.getReturnType() == boolean.class) {
                    String propertyName = methodName.substring(2);
                    getterMap.computeIfAbsent(propertyName, k -> new HashSet<>()).add("is" + propertyName);
                }
            }

            // Check for conflicts within this entity
            for (Map.Entry<String, Set<String>> entry : getterMap.entrySet()) {
                Set<String> getters = entry.getValue();
                if (getters.size() > 1) {
                    fail(entityClass.getSimpleName() + ": Property " + entry.getKey()
                            + " should not have conflicting getters: " + getters);
                }
            }
        }
    }
}
