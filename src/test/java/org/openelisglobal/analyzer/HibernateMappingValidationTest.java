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
import org.openelisglobal.analyzer.valueholder.AnalyzerActivationRecord;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingConfirmation;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;
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
        configuration.addAnnotatedClass(Analyzer.class);
        configuration.addAnnotatedClass(AnalyzerActivationRecord.class);
        configuration.addAnnotatedClass(AnalyzerProfileBinding.class);
        configuration.addAnnotatedClass(AnalyzerSiteBinding.class);
        configuration.addAnnotatedClass(AnalyzerSiteBindingConfirmation.class);
        configuration.addAnnotatedClass(AnalyzerSiteBindingRevision.class);
        configuration.addAnnotatedClass(AnalyzerSiteBindingTest.class);
        configuration.addAnnotatedClass(AnalyzerSiteBindingResult.class);
        configuration.addAnnotatedClass(AnalyzerResults.class);

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
        assertNotNull("Analyzer should be registered", sessionFactory.getMetamodel().entity(Analyzer.class));
        assertNotNull("AnalyzerActivationRecord should be registered",
                sessionFactory.getMetamodel().entity(AnalyzerActivationRecord.class));
        assertNotNull("AnalyzerProfileBinding should be registered",
                sessionFactory.getMetamodel().entity(AnalyzerProfileBinding.class));
        assertNotNull("AnalyzerSiteBinding should be registered",
                sessionFactory.getMetamodel().entity(AnalyzerSiteBinding.class));
        assertNotNull("AnalyzerSiteBindingConfirmation should be registered",
                sessionFactory.getMetamodel().entity(AnalyzerSiteBindingConfirmation.class));
        assertNotNull("AnalyzerSiteBindingRevision should be registered",
                sessionFactory.getMetamodel().entity(AnalyzerSiteBindingRevision.class));
        assertNotNull("AnalyzerSiteBindingTest should be registered",
                sessionFactory.getMetamodel().entity(AnalyzerSiteBindingTest.class));
        assertNotNull("AnalyzerSiteBindingResult should be registered",
                sessionFactory.getMetamodel().entity(AnalyzerSiteBindingResult.class));
        assertNotNull("AnalyzerResults should be registered",
                sessionFactory.getMetamodel().entity(AnalyzerResults.class));
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
        Class<?>[] entities = { Analyzer.class, AnalyzerActivationRecord.class, AnalyzerProfileBinding.class,
                AnalyzerSiteBinding.class, AnalyzerSiteBindingConfirmation.class, AnalyzerSiteBindingRevision.class,
                AnalyzerSiteBindingTest.class, AnalyzerSiteBindingResult.class, AnalyzerResults.class };

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
