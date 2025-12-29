package org.openelisglobal.organization;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openelisglobal.organization.valueholder.OrganizationSampleType;

/**
 * ORM Validation Test for OrganizationSampleType entity. Validates entity
 * structure and property access patterns. Constitution V.4 compliance.
 */
public class OrganizationSampleTypeORMValidationTest {

    @Test
    public void testOrganizationSampleTypeEntityCreation() {
        OrganizationSampleType entity = new OrganizationSampleType();
        assertNotNull("Entity should be created", entity);
    }

    @Test
    public void testIdProperty() {
        OrganizationSampleType entity = new OrganizationSampleType();
        entity.setId("100");
        assertEquals("ID should be set correctly", "100", entity.getId());
    }

    @Test
    public void testOrganizationIdProperty() {
        OrganizationSampleType entity = new OrganizationSampleType();
        entity.setOrganizationId("1");
        assertEquals("OrganizationId should be set correctly", "1", entity.getOrganizationId());
    }

    @Test
    public void testSampleTypeIdProperty() {
        OrganizationSampleType entity = new OrganizationSampleType();
        entity.setSampleTypeId("10");
        assertEquals("SampleTypeId should be set correctly", "10", entity.getSampleTypeId());
    }

    @Test
    public void testIsActiveProperty_DefaultValue() {
        OrganizationSampleType entity = new OrganizationSampleType();
        assertTrue("isActive should default to true", entity.getIsActive());
    }

    @Test
    public void testIsActiveProperty_SetFalse() {
        OrganizationSampleType entity = new OrganizationSampleType();
        entity.setIsActive(false);
        assertFalse("isActive should be false", entity.getIsActive());
    }

    @Test
    public void testSortOrderProperty_DefaultValue() {
        OrganizationSampleType entity = new OrganizationSampleType();
        assertEquals("sortOrder should default to 0", Integer.valueOf(0), entity.getSortOrder());
    }

    @Test
    public void testSortOrderProperty_SetValue() {
        OrganizationSampleType entity = new OrganizationSampleType();
        entity.setSortOrder(5);
        assertEquals("sortOrder should be set correctly", Integer.valueOf(5), entity.getSortOrder());
    }

    @Test
    public void testNoGetterConflicts() {
        // Verify no isActive/getActive conflicts - getIsActive is the correct accessor
        OrganizationSampleType entity = new OrganizationSampleType();
        entity.setIsActive(true);
        // This should compile and work without conflicts
        assertTrue("getIsActive should return true", entity.getIsActive());
    }

    @Test
    public void testFullEntityPopulation() {
        OrganizationSampleType entity = new OrganizationSampleType();
        entity.setId("100");
        entity.setOrganizationId("1");
        entity.setSampleTypeId("10");
        entity.setIsActive(true);
        entity.setSortOrder(3);

        assertEquals("100", entity.getId());
        assertEquals("1", entity.getOrganizationId());
        assertEquals("10", entity.getSampleTypeId());
        assertTrue(entity.getIsActive());
        assertEquals(Integer.valueOf(3), entity.getSortOrder());
    }
}
