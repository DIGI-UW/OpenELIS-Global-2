package org.openelisglobal.reports.adhoc.service;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.reports.adhoc.dto.AvailableFieldsDTO;
import org.openelisglobal.reports.adhoc.dto.ReportFieldDTO;

public class AdHocFieldDefinitionServiceTest {

    private AdHocFieldDefinitionService fieldDefinitionService;

    @Before
    public void setUp() {
        fieldDefinitionService = new AdHocFieldDefinitionServiceImpl();
    }

    @Test
    public void testGetAvailableFields_ReturnsNonEmpty() {
        AvailableFieldsDTO fields = fieldDefinitionService.getAvailableFields();

        assertNotNull(fields);
        assertFalse(fields.getPatientFields().isEmpty());
        assertFalse(fields.getSampleFields().isEmpty());
    }

    @Test
    public void testGetPatientFields_ContainsExpectedFields() {
        List<ReportFieldDTO> patientFields = fieldDefinitionService.getPatientFields();

        assertNotNull(patientFields);
        assertTrue(patientFields.size() >= 5);

        List<String> fieldIds = patientFields.stream().map(ReportFieldDTO::getFieldId).toList();
        assertTrue(fieldIds.contains("patient.nationalId"));
        assertTrue(fieldIds.contains("patient.gender"));
        assertTrue(fieldIds.contains("patient.firstName"));
        assertTrue(fieldIds.contains("patient.lastName"));
    }

    @Test
    public void testGetSampleFields_ContainsExpectedFields() {
        List<ReportFieldDTO> sampleFields = fieldDefinitionService.getSampleFields();

        assertNotNull(sampleFields);
        assertTrue(sampleFields.size() >= 5);

        List<String> fieldIds = sampleFields.stream().map(ReportFieldDTO::getFieldId).toList();
        assertTrue(fieldIds.contains("sample.accessionNumber"));
        assertTrue(fieldIds.contains("sample.collectionDate"));
        assertTrue(fieldIds.contains("sample.statusId"));
    }

    @Test
    public void testGetFieldById_ValidPatientField_ReturnsField() {
        ReportFieldDTO field = fieldDefinitionService.getFieldById("patient.nationalId");

        assertNotNull(field);
        assertEquals("patient.nationalId", field.getFieldId());
        assertEquals("patient", field.getEntityName());
        assertNotNull(field.getPropertyPath());
    }

    @Test
    public void testGetFieldById_ValidSampleField_ReturnsField() {
        ReportFieldDTO field = fieldDefinitionService.getFieldById("sample.accessionNumber");

        assertNotNull(field);
        assertEquals("sample.accessionNumber", field.getFieldId());
        assertEquals("sample", field.getEntityName());
        assertNotNull(field.getPropertyPath());
    }

    @Test
    public void testGetFieldById_InvalidField_ReturnsNull() {
        ReportFieldDTO field = fieldDefinitionService.getFieldById("invalid.field");

        assertNull(field);
    }

    @Test
    public void testValidateFieldIds_AllValid_ReturnsTrue() {
        List<String> fieldIds = Arrays.asList("patient.nationalId", "patient.gender", "sample.accessionNumber");

        boolean isValid = fieldDefinitionService.validateFieldIds(fieldIds);

        assertTrue(isValid);
    }

    @Test
    public void testValidateFieldIds_SomeInvalid_ReturnsFalse() {
        List<String> fieldIds = Arrays.asList("patient.nationalId", "invalid.field", "sample.accessionNumber");

        boolean isValid = fieldDefinitionService.validateFieldIds(fieldIds);

        assertFalse(isValid);
    }

    @Test
    public void testValidateFieldIds_Empty_ReturnsFalse() {
        List<String> fieldIds = Arrays.asList();

        boolean isValid = fieldDefinitionService.validateFieldIds(fieldIds);

        assertFalse(isValid);
    }

    @Test
    public void testValidateFieldIds_Null_ReturnsFalse() {
        boolean isValid = fieldDefinitionService.validateFieldIds(null);

        assertFalse(isValid);
    }

    @Test
    public void testFieldsHaveProperOperators() {
        ReportFieldDTO stringField = fieldDefinitionService.getFieldById("patient.nationalId");
        ReportFieldDTO dateField = fieldDefinitionService.getFieldById("sample.collectionDate");

        assertNotNull(stringField.getOperators());
        assertFalse(stringField.getOperators().isEmpty());

        assertNotNull(dateField.getOperators());
        assertFalse(dateField.getOperators().isEmpty());

        assertTrue(dateField.getOperators().stream().anyMatch(op -> op == ReportFieldDTO.FilterOperator.BETWEEN));
    }
}
