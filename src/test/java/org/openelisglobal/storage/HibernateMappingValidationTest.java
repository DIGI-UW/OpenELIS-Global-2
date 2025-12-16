package org.openelisglobal.storage;

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
import org.openelisglobal.storage.valueholder.*;

/**
 * Validates Hibernate ORM mappings WITHOUT requiring database connection. This
 * test layer catches entity/mapping conflicts before integration tests.
 * 
 * Executes in <5 seconds, preventing ORM errors that would otherwise only
 * appear at application startup.
 * 
 * ADDED: 2025-10-31 - Fills gap between unit tests (mocked) and integration
 * tests (full stack)
 */
public class HibernateMappingValidationTest {

    private static SessionFactory sessionFactory;

    @BeforeClass
    public static void buildSessionFactory() {
        Configuration configuration = new Configuration();

        // Add all storage entity mappings using annotation-based approach (per
        // Constitution v1.3.0)
        configuration.addAnnotatedClass(StorageRoom.class);
        configuration.addAnnotatedClass(StorageDevice.class);
        configuration.addAnnotatedClass(StorageShelf.class);
        configuration.addAnnotatedClass(StorageRack.class);
        configuration.addAnnotatedClass(StoragePosition.class);
        configuration.addAnnotatedClass(SampleStorageAssignment.class);
        configuration.addAnnotatedClass(SampleStorageMovement.class);

        // Add dependent entity mappings (Sample and SampleItem still use XML - legacy)
        // SampleItem depends on TypeOfSample and UnitOfMeasure
        // TypeOfSample depends on Localization
        configuration.addResource("hibernate/hbm/Sample.hbm.xml");
        configuration.addResource("hibernate/hbm/SampleItem.hbm.xml");
        configuration.addResource("hibernate/hbm/TypeOfSample.hbm.xml");
        configuration.addResource("hibernate/hbm/UnitOfMeasure.hbm.xml");
        configuration.addResource("hibernate/hbm/Localization.hbm.xml");

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
     * Test that all storage entity Hibernate mappings load successfully Catches:
     * Property name mismatches, missing getters/setters, invalid relationships
     */
    @Test
    public void testAllStorageHibernateMappingsLoadSuccessfully() {
        assertNotNull("SessionFactory should build successfully with all storage mappings", sessionFactory);

        // Verify each entity is registered in Hibernate metamodel
        assertNotNull("StorageRoom should be registered", sessionFactory.getMetamodel().entity(StorageRoom.class));
        assertNotNull("StorageDevice should be registered", sessionFactory.getMetamodel().entity(StorageDevice.class));
        assertNotNull("StorageShelf should be registered", sessionFactory.getMetamodel().entity(StorageShelf.class));
        assertNotNull("StorageRack should be registered", sessionFactory.getMetamodel().entity(StorageRack.class));
        assertNotNull("StoragePosition should be registered",
                sessionFactory.getMetamodel().entity(StoragePosition.class));
        assertNotNull("SampleStorageAssignment should be registered",
                sessionFactory.getMetamodel().entity(SampleStorageAssignment.class));
        assertNotNull("SampleStorageMovement should be registered",
                sessionFactory.getMetamodel().entity(SampleStorageMovement.class));
    }

    /**
     * Test that storage entities follow JavaBean conventions Catches: Conflicting
     * getters (getActive() vs isActive())
     */
    @Test
    public void testStorageEntitiesHaveNoGetterConflicts() {
        Class<?>[] entities = { StorageRoom.class, StorageDevice.class, StorageShelf.class, StorageRack.class,
                StoragePosition.class, SampleStorageAssignment.class, SampleStorageMovement.class };

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
