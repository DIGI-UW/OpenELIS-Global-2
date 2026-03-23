/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.sampleitem;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;
import org.junit.Test;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleitem.valueholder.SampleItemAliquotRelationship;

/**
 * ORM Validation Test for SampleItemAliquotRelationship entity (Constitution
 * V.4)
 *
 * <p>
 * Purpose: Validates that SampleItemAliquotRelationship entity structure is
 * correct and the XML mapping file exists. This entity uses XML mapping
 * (SampleItemAliquotRelationship.hbm.xml) for consistency with SampleItem which
 * also uses XML mapping.
 *
 * <p>
 * Requirements: - MUST execute in <5 seconds - MUST NOT require database
 * connection - MUST validate entity structure - MUST verify XML mapping file
 * exists
 *
 * <p>
 * Context: This entity tracks aliquoting metadata including sequence numbers,
 * quantity transferred, and FHIR integration details.
 *
 * <p>
 * Related: Feature 001-sample-management
 *
 * @see <a href="../../../specs/001-sample-management/spec.md">Feature
 *      Specification</a>
 * @see <a href="../../../.specify/guides/testing-roadmap.md">Testing Roadmap -
 *      ORM Validation Tests</a>
 */
public class SampleItemAliquotRelationshipOrmValidationTest {

    /**
     * Test that SampleItemAliquotRelationship XML mapping file exists.
     *
     * <p>
     * This test validates that the Hibernate XML mapping file is present in the
     * classpath.
     */
    @Test
    public void testSampleItemAliquotRelationshipHibernateXmlMappingExists() {
        // Act: Try to load the XML mapping file from classpath
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("hibernate/hbm/SampleItemAliquotRelationship.hbm.xml");

        // Assert: File should exist
        assertNotNull("SampleItemAliquotRelationship.hbm.xml should exist in hibernate/hbm/ directory", inputStream);
    }

    /**
     * Test that SampleItemAliquotRelationship entity has all required fields.
     *
     * <p>
     * This validates the entity structure matches what the XML mapping expects.
     */
    @Test
    public void testSampleItemAliquotRelationshipEntityHasRequiredFields() {
        // Arrange: Create instance
        SampleItemAliquotRelationship entity = new SampleItemAliquotRelationship();

        // Assert: All required fields exist via reflection
        Class<?> clazz = SampleItemAliquotRelationship.class;

        // Check id field
        assertFieldExists(clazz, "id", Long.class);

        // Check parentSampleItem field
        assertFieldExists(clazz, "parentSampleItem", SampleItem.class);

        // Check childSampleItem field
        assertFieldExists(clazz, "childSampleItem", SampleItem.class);

        // Check sequenceNumber field
        assertFieldExists(clazz, "sequenceNumber", Integer.class);

        // Check quantityTransferred field
        assertFieldExists(clazz, "quantityTransferred", BigDecimal.class);

        // Check notes field
        assertFieldExists(clazz, "notes", String.class);

        // Check fhirUuid field
        assertFieldExists(clazz, "fhirUuid", UUID.class);

        // Check createdDate field
        assertFieldExists(clazz, "createdDate", Timestamp.class);
    }

    /**
     * Test that SampleItemAliquotRelationship getters and setters work correctly.
     *
     * <p>
     * This validates JavaBean conventions are followed.
     */
    @Test
    public void testSampleItemAliquotRelationshipGettersAndSetters() {
        // Arrange
        SampleItemAliquotRelationship entity = new SampleItemAliquotRelationship();
        Long id = 123L;
        Integer sequenceNumber = 1;
        BigDecimal quantity = new BigDecimal("5.500");
        String notes = "Test notes";
        UUID uuid = UUID.randomUUID();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SampleItem parent = new SampleItem();
        SampleItem child = new SampleItem();

        // Act
        entity.setId(id);
        entity.setSequenceNumber(sequenceNumber);
        entity.setQuantityTransferred(quantity);
        entity.setNotes(notes);
        entity.setFhirUuid(uuid);
        entity.setCreatedDate(timestamp);
        entity.setParentSampleItem(parent);
        entity.setChildSampleItem(child);

        // Assert
        assertTrue("getId should return the set value", id.equals(entity.getId()));
        assertTrue("getSequenceNumber should return the set value", sequenceNumber.equals(entity.getSequenceNumber()));
        assertTrue("getQuantityTransferred should return the set value",
                quantity.equals(entity.getQuantityTransferred()));
        assertTrue("getNotes should return the set value", notes.equals(entity.getNotes()));
        assertTrue("getFhirUuid should return the set value", uuid.equals(entity.getFhirUuid()));
        assertTrue("getCreatedDate should return the set value", timestamp.equals(entity.getCreatedDate()));
        assertTrue("getParentSampleItem should return the set value", parent == entity.getParentSampleItem());
        assertTrue("getChildSampleItem should return the set value", child == entity.getChildSampleItem());
    }

    /**
     * Helper method to assert a field exists with the expected type.
     */
    private void assertFieldExists(Class<?> clazz, String fieldName, Class<?> expectedType) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            assertTrue("Field " + fieldName + " should be of type " + expectedType.getSimpleName(),
                    expectedType.isAssignableFrom(field.getType()));
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Field " + fieldName + " should exist in " + clazz.getName());
        }
    }
}
