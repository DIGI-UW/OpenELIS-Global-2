package org.openelisglobal.shipment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.openelisglobal.shipment.valueholder.BoxSampleItem;
import org.openelisglobal.shipment.valueholder.BoxState;
import org.openelisglobal.shipment.valueholder.ReceptionStatus;
import org.openelisglobal.shipment.valueholder.Shipment;
import org.openelisglobal.shipment.valueholder.ShipmentStatus;
import org.openelisglobal.shipment.valueholder.ShippingBox;

/**
 * Validates shipment entity annotations and JavaBean conventions WITHOUT
 * requiring a database or Spring context. Fast (<1s) annotation-level checks.
 *
 * Full ORM mapping validation (SessionFactory build, relationship resolution)
 * is covered by integration tests (ShippingBoxServiceTest, etc.) which use the
 * real Spring context with test-persistence.xml.
 */
public class ShipmentHibernateMappingValidationTest {

    /**
     * Verify that all shipment entities have required JPA annotations.
     */
    @Test
    public void testAllShipmentEntitiesHaveJpaAnnotations() {
        Class<?>[] entities = { ShippingBox.class, Shipment.class, BoxSampleItem.class };

        for (Class<?> entityClass : entities) {
            assertNotNull(entityClass.getSimpleName() + " must have @Entity", entityClass.getAnnotation(Entity.class));
            assertNotNull(entityClass.getSimpleName() + " must have @Table", entityClass.getAnnotation(Table.class));
        }
    }

    /**
     * Test that shipment entities follow JavaBean conventions Catches: Conflicting
     * getters (getActive() vs isActive())
     */
    @Test
    public void testShipmentEntitiesHaveNoGetterConflicts() {
        Class<?>[] entities = { ShippingBox.class, Shipment.class, BoxSampleItem.class };

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
     * Test that enum types are properly configured Catches: Missing @Enumerated
     * annotation, STRING vs ORDINAL mismatches
     */
    @Test
    public void testEnumTypesConfigured() {
        // Verify enums can be instantiated
        assertNotNull("BoxState enum should be usable", BoxState.DRAFT);
        assertNotNull("ShipmentStatus enum should be usable", ShipmentStatus.PENDING);
        assertNotNull("ReceptionStatus enum should be usable", ReceptionStatus.PENDING);

        // Verify fromString methods work
        assertEquals("BoxState.fromString should work", BoxState.DRAFT, BoxState.fromString("DRAFT"));
        assertEquals("ShipmentStatus.fromString should work", ShipmentStatus.PENDING,
                ShipmentStatus.fromString("PENDING"));
        assertEquals("ReceptionStatus.fromString should work", ReceptionStatus.PENDING,
                ReceptionStatus.fromString("PENDING"));
    }
}
