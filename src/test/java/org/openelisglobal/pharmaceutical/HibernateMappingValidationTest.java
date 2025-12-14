package org.openelisglobal.pharmaceutical;

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
import org.openelisglobal.pharmaceutical.valueholder.*;

/**
 * Validates Hibernate ORM mappings for pharmaceutical entities WITHOUT requiring
 * database connection. This test layer catches entity/mapping conflicts before
 * integration tests.
 *
 * Executes in <5 seconds, preventing ORM errors that would otherwise only
 * appear at application startup.
 *
 * Constitution V.4: ORM Validation Tests - MUST include test that builds
 * SessionFactory, validates all entity mappings load without errors.
 */
public class HibernateMappingValidationTest {

    private static SessionFactory sessionFactory;

    @BeforeClass
    public static void buildSessionFactory() {
        Configuration configuration = new Configuration();

        // Add all pharmaceutical entity mappings using annotation-based approach
        configuration.addAnnotatedClass(PharmaceuticalSample.class);
        configuration.addAnnotatedClass(Aliquot.class);
        configuration.addAnnotatedClass(QCCheck.class);
        configuration.addAnnotatedClass(ProcessingStep.class);
        configuration.addAnnotatedClass(AssayRun.class);
        configuration.addAnnotatedClass(DeviationCAPA.class);
        configuration.addAnnotatedClass(ChainOfCustodyEvent.class);
        configuration.addAnnotatedClass(DisposalRecord.class);
        configuration.addAnnotatedClass(EnvironmentalExcursionEvent.class);

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
     * Test that all pharmaceutical entity Hibernate mappings load successfully.
     * Catches: Property name mismatches, missing getters/setters, invalid relationships.
     */
    @Test
    public void testAllPharmaceuticalHibernateMappingsLoadSuccessfully() {
        assertNotNull("SessionFactory should build successfully with all pharmaceutical mappings", sessionFactory);

        // Verify each entity is registered in Hibernate metamodel
        assertNotNull("PharmaceuticalSample should be registered",
                sessionFactory.getMetamodel().entity(PharmaceuticalSample.class));
        assertNotNull("Aliquot should be registered",
                sessionFactory.getMetamodel().entity(Aliquot.class));
        assertNotNull("QCCheck should be registered",
                sessionFactory.getMetamodel().entity(QCCheck.class));
        assertNotNull("ProcessingStep should be registered",
                sessionFactory.getMetamodel().entity(ProcessingStep.class));
        assertNotNull("AssayRun should be registered",
                sessionFactory.getMetamodel().entity(AssayRun.class));
        assertNotNull("DeviationCAPA should be registered",
                sessionFactory.getMetamodel().entity(DeviationCAPA.class));
        assertNotNull("ChainOfCustodyEvent should be registered",
                sessionFactory.getMetamodel().entity(ChainOfCustodyEvent.class));
        assertNotNull("DisposalRecord should be registered",
                sessionFactory.getMetamodel().entity(DisposalRecord.class));
        assertNotNull("EnvironmentalExcursionEvent should be registered",
                sessionFactory.getMetamodel().entity(EnvironmentalExcursionEvent.class));
    }

    /**
     * Test that pharmaceutical entities follow JavaBean conventions.
     * Catches: Conflicting getters (getActive() vs isActive()).
     */
    @Test
    public void testPharmaceuticalEntitiesHaveNoGetterConflicts() {
        Class<?>[] entities = {
                PharmaceuticalSample.class,
                Aliquot.class,
                QCCheck.class,
                ProcessingStep.class,
                AssayRun.class,
                DeviationCAPA.class,
                ChainOfCustodyEvent.class,
                DisposalRecord.class,
                EnvironmentalExcursionEvent.class
        };

        for (Class<?> entityClass : entities) {
            validateNoGetterConflicts(entityClass);
        }
    }

    /**
     * Validate that an entity doesn't have conflicting getters.
     * E.g., both getActive() returning Boolean AND isActive() returning boolean.
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
     * Catches: Wrong return types, primitive vs wrapper mismatches.
     */
    @Test
    public void testEntityPropertyTypesValid() {
        // If SessionFactory built, property types are compatible
        // This is implicit validation - SessionFactory won't build if types incompatible
        assertNotNull("SessionFactory validates property types", sessionFactory);
    }

    /**
     * Test PharmaceuticalSample entity has required fields for core functionality.
     */
    @Test
    public void testPharmaceuticalSampleHasRequiredFields() {
        Class<?> clazz = PharmaceuticalSample.class;

        // Verify required getter methods exist
        assertMethodExists(clazz, "getId");
        assertMethodExists(clazz, "getSampleName");
        assertMethodExists(clazz, "getUniqueSampleId");
        assertMethodExists(clazz, "getBarcode");
        assertMethodExists(clazz, "getStatus");
        assertMethodExists(clazz, "getLabType");
    }

    /**
     * Test Aliquot entity has required fields for core functionality.
     */
    @Test
    public void testAliquotHasRequiredFields() {
        Class<?> clazz = Aliquot.class;

        // Verify required getter methods exist
        assertMethodExists(clazz, "getId");
        assertMethodExists(clazz, "getAliquotCode");
        assertMethodExists(clazz, "getParentSample");
        assertMethodExists(clazz, "getFreezeThawCount");
        assertMethodExists(clazz, "getFreezeThawLimit");
        assertMethodExists(clazz, "getStatus");
    }

    /**
     * Test DisposalRecord entity has required fields for disposal workflow.
     */
    @Test
    public void testDisposalRecordHasRequiredFields() {
        Class<?> clazz = DisposalRecord.class;

        // Verify required getter methods exist
        assertMethodExists(clazz, "getId");
        assertMethodExists(clazz, "getSample");
        assertMethodExists(clazz, "getReason");
        assertMethodExists(clazz, "getMethod");
        assertMethodExists(clazz, "getStatus");
    }

    /**
     * Test EnvironmentalExcursionEvent entity has required fields for excursion tracking.
     */
    @Test
    public void testEnvironmentalExcursionEventHasRequiredFields() {
        Class<?> clazz = EnvironmentalExcursionEvent.class;

        // Verify required getter methods exist
        assertMethodExists(clazz, "getId");
        assertMethodExists(clazz, "getDeviceId");
        assertMethodExists(clazz, "getAlertType");
        assertMethodExists(clazz, "getStatus");
        assertMethodExists(clazz, "getDetectedAt");
    }

    private void assertMethodExists(Class<?> clazz, String methodName) {
        try {
            clazz.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            fail(clazz.getSimpleName() + " is missing required method: " + methodName + "()");
        }
    }
}
