package org.openelisglobal.test;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openelisglobal.test.valueholder.DepartmentSampleType;

/**
 * ORM Validation Test for DepartmentSampleType entity. Validates entity
 * structure and property access patterns. Constitution V.4 compliance.
 */
public class DepartmentSampleTypeORMValidationTest {

    @Test
    public void testDepartmentSampleTypeEntityCreation() {
        DepartmentSampleType entity = new DepartmentSampleType();
        assertNotNull("Entity should be created", entity);
    }

    @Test
    public void testIdProperty() {
        DepartmentSampleType entity = new DepartmentSampleType();
        entity.setId("100");
        assertEquals("ID should be set correctly", "100", entity.getId());
    }

    @Test
    public void testTestSectionIdProperty() {
        DepartmentSampleType entity = new DepartmentSampleType();
        entity.setTestSectionId("1");
        assertEquals("TestSectionId should be set correctly", "1", entity.getTestSectionId());
    }

    @Test
    public void testSampleTypeIdProperty() {
        DepartmentSampleType entity = new DepartmentSampleType();
        entity.setSampleTypeId("10");
        assertEquals("SampleTypeId should be set correctly", "10", entity.getSampleTypeId());
    }

    @Test
    public void testIsActiveProperty_DefaultValue() {
        DepartmentSampleType entity = new DepartmentSampleType();
        assertTrue("isActive should default to true", entity.getIsActive());
    }

    @Test
    public void testIsActiveProperty_SetFalse() {
        DepartmentSampleType entity = new DepartmentSampleType();
        entity.setIsActive(false);
        assertFalse("isActive should be false", entity.getIsActive());
    }

    @Test
    public void testSortOrderProperty_DefaultValue() {
        DepartmentSampleType entity = new DepartmentSampleType();
        assertEquals("sortOrder should default to 0", Integer.valueOf(0), entity.getSortOrder());
    }

    @Test
    public void testSortOrderProperty_SetValue() {
        DepartmentSampleType entity = new DepartmentSampleType();
        entity.setSortOrder(5);
        assertEquals("sortOrder should be set correctly", Integer.valueOf(5), entity.getSortOrder());
    }

    @Test
    public void testNoGetterConflicts() {
        // Verify no isActive/getActive conflicts - getIsActive is the correct accessor
        DepartmentSampleType entity = new DepartmentSampleType();
        entity.setIsActive(true);
        // This should compile and work without conflicts
        assertTrue("getIsActive should return true", entity.getIsActive());
    }

    @Test
    public void testFullEntityPopulation() {
        DepartmentSampleType entity = new DepartmentSampleType();
        entity.setId("100");
        entity.setTestSectionId("1");
        entity.setSampleTypeId("10");
        entity.setIsActive(true);
        entity.setSortOrder(3);

        assertEquals("100", entity.getId());
        assertEquals("1", entity.getTestSectionId());
        assertEquals("10", entity.getSampleTypeId());
        assertTrue(entity.getIsActive());
        assertEquals(Integer.valueOf(3), entity.getSortOrder());
    }
}
