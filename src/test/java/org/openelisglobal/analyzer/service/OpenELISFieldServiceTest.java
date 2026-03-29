package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.form.OpenELISFieldForm;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.test.dao.TestDAO;
import org.openelisglobal.test.service.TestService;

/**
 * Unit tests for OpenELISFieldService.
 * 
 * Tests field creation, uniqueness validation, and error handling.
 */
@RunWith(MockitoJUnitRunner.class)
public class OpenELISFieldServiceTest {

    @Mock
    private TestService testService;

    @Mock
    private TestDAO testDAO;

    @Mock
    private LocalizationService localizationService;

    @InjectMocks
    private OpenELISFieldServiceImpl openELISFieldService;

    private OpenELISFieldForm testForm;

    @Before
    public void setUp() {
        testForm = new OpenELISFieldForm();
        testForm.setEntityType(OpenELISFieldForm.EntityType.TEST);
        testForm.setFieldName("Glucose Test");
        testForm.setTestCode("GLUCOSE");
        testForm.setDescription("Blood glucose test");
        testForm.setLoincCode("2345-7");
        testForm.setResultType("NUMERIC");
    }

    @Test
    public void testCreateField_WithValidData_PersistsField() {
        // Arrange
        when(testService.getTestByDescription(anyString())).thenReturn(null);
        when(testService.getTestsByLoincCode(anyString())).thenReturn(java.util.Collections.emptyList());
        when(testDAO.getTestsByName(anyString())).thenReturn(java.util.Collections.emptyList());
        when(localizationService.insert(any())).thenReturn("LOC-123");
        
        org.openelisglobal.test.valueholder.Test createdTest = new org.openelisglobal.test.valueholder.Test();
        createdTest.setId("TEST-123");
        when(testService.insert(any(org.openelisglobal.test.valueholder.Test.class))).thenReturn("TEST-123");

        // Act
        String fieldId = openELISFieldService.createField(testForm);

        // Assert
        assertNotNull("Field ID should not be null", fieldId);
        assertEquals("TEST-123", fieldId);
        verify(testService).insert(any(org.openelisglobal.test.valueholder.Test.class));
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testCreateField_WithDuplicateName_ThrowsException() {
        // Arrange
        org.openelisglobal.test.valueholder.Test existingTest = new org.openelisglobal.test.valueholder.Test();
        existingTest.setId("EXISTING-123");
        when(testService.getTestByDescription(anyString())).thenReturn(existingTest);

        // Act
        openELISFieldService.createField(testForm);

        // Assert - exception should be thrown
    }

    @Test
    public void testValidateFieldUniqueness_WithExistingName_ReturnsFalse() {
        // Arrange
        org.openelisglobal.test.valueholder.Test existingTest = new org.openelisglobal.test.valueholder.Test();
        existingTest.setId("EXISTING-123");
        when(testService.getTestByDescription(anyString())).thenReturn(existingTest);

        // Act
        boolean isUnique = openELISFieldService.validateFieldUniqueness(testForm);

        // Assert
        assertFalse("Field should not be unique if name already exists", isUnique);
    }

    @Test
    public void testValidateFieldUniqueness_WithNewName_ReturnsTrue() {
        // Arrange
        when(testService.getTestByDescription(anyString())).thenReturn(null);
        when(testDAO.getTestsByName(anyString())).thenReturn(java.util.Collections.emptyList());
        when(testService.getTestsByLoincCode(anyString())).thenReturn(java.util.Collections.emptyList());

        // Act
        boolean isUnique = openELISFieldService.validateFieldUniqueness(testForm);

        // Assert
        assertTrue("Field should be unique if name does not exist", isUnique);
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testCreateField_WithDuplicateTestCode_ThrowsException() {
        // Arrange
        when(testService.getTestByDescription(anyString())).thenReturn(null);
        org.openelisglobal.test.valueholder.Test existingTest = new org.openelisglobal.test.valueholder.Test();
        existingTest.setId("EXISTING-123");
        existingTest.setLocalCode("GLUCOSE");
        when(testDAO.getTestsByName(anyString())).thenReturn(java.util.Collections.singletonList(existingTest));

        // Act
        openELISFieldService.createField(testForm);

        // Assert - exception should be thrown
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testCreateField_WithDuplicateLoincCode_ThrowsException() {
        // Arrange
        when(testService.getTestByDescription(anyString())).thenReturn(null);
        when(testDAO.getTestsByName(anyString())).thenReturn(java.util.Collections.emptyList());
        org.openelisglobal.test.valueholder.Test existingTest = new org.openelisglobal.test.valueholder.Test();
        existingTest.setId("EXISTING-123");
        existingTest.setLoinc("2345-7");
        when(testService.getTestsByLoincCode(anyString())).thenReturn(java.util.Collections.singletonList(existingTest));

        // Act
        openELISFieldService.createField(testForm);

        // Assert - exception should be thrown
    }
}
