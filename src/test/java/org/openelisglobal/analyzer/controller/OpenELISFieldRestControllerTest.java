package org.openelisglobal.analyzer.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.form.OpenELISFieldForm;
import org.openelisglobal.analyzer.service.OpenELISFieldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for OpenELISFieldRestController.
 * 
 * Test Coverage Goal: >80%
 * 
 * Note: Using BaseWebContextSensitiveTest pattern since @WebMvcTest
 * dependencies not available. @WebMvcTest would be preferred for unit-level
 * controller testing.
 */
public class OpenELISFieldRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private OpenELISFieldService openELISFieldService;

    @Autowired
    private DataSource dataSource;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        jdbcTemplate = new JdbcTemplate(dataSource);
        cleanTestData();
    }

    /**
     * Clean up test data.
     */
    private void cleanTestData() {
        try {
            // Clean up any test-created tests
            jdbcTemplate.execute("DELETE FROM test WHERE description LIKE 'TEST-%'");
        } catch (Exception e) {
            System.out.println("Failed to clean test data: " + e.getMessage());
        }
    }

    @Test
    public void testCreateField_WithValidData_ReturnsCreated() throws Exception {
        // Arrange
        OpenELISFieldForm form = new OpenELISFieldForm();
        form.setEntityType(OpenELISFieldForm.EntityType.TEST);
        form.setFieldName("TEST-Glucose Test " + System.currentTimeMillis());
        form.setTestCode("TEST-GLUCOSE-" + System.currentTimeMillis());
        form.setDescription("Test glucose field");
        form.setLoincCode("2345-7");
        form.setResultType("NUMERIC");

        String requestBody = objectMapper.writeValueAsString(form);

        // Act & Assert
        mockMvc.perform(
                post("/rest/analyzer/openelis-fields").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.field").exists()).andExpect(jsonPath("$.field.name").value(form.getFieldName()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void testCreateField_WithDuplicateName_ReturnsConflict() throws Exception {
        // Arrange - create a test first
        OpenELISFieldForm existingForm = new OpenELISFieldForm();
        existingForm.setEntityType(OpenELISFieldForm.EntityType.TEST);
        String duplicateName = "TEST-Duplicate Test " + System.currentTimeMillis();
        existingForm.setFieldName(duplicateName);
        existingForm.setTestCode("TEST-DUP-" + System.currentTimeMillis());
        existingForm.setDescription("Duplicate test");
        existingForm.setResultType("NUMERIC");

        // Create the first test
        String existingId = openELISFieldService.createField(existingForm);
        assert existingId != null;

        // Try to create another with the same name
        OpenELISFieldForm duplicateForm = new OpenELISFieldForm();
        duplicateForm.setEntityType(OpenELISFieldForm.EntityType.TEST);
        duplicateForm.setFieldName(duplicateName);
        duplicateForm.setTestCode("TEST-DUP2-" + System.currentTimeMillis());
        duplicateForm.setDescription("Another duplicate test");
        duplicateForm.setResultType("NUMERIC");

        String requestBody = objectMapper.writeValueAsString(duplicateForm);

        // Act & Assert
        mockMvc.perform(
                post("/rest/analyzer/openelis-fields").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void testCreateField_WithInvalidData_ReturnsBadRequest() throws Exception {
        // Arrange - form with missing required fields
        OpenELISFieldForm form = new OpenELISFieldForm();
        // Missing entityType and fieldName
        form.setDescription("Test description");

        String requestBody = objectMapper.writeValueAsString(form);

        // Act & Assert
        mockMvc.perform(
                post("/rest/analyzer/openelis-fields").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
    }

    @Test
    public void testValidateField_WithUniqueField_ReturnsTrue() throws Exception {
        // Arrange
        OpenELISFieldForm form = new OpenELISFieldForm();
        form.setEntityType(OpenELISFieldForm.EntityType.TEST);
        form.setFieldName("TEST-Validate Test " + System.currentTimeMillis());
        form.setTestCode("TEST-VALIDATE-" + System.currentTimeMillis());
        form.setDescription("Validation test");
        form.setResultType("NUMERIC");

        String requestBody = objectMapper.writeValueAsString(form);

        // Act & Assert
        mockMvc.perform(post("/rest/analyzer/openelis-fields/validate").contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)).andExpect(status().isOk()).andExpect(jsonPath("$.isUnique").value(true));
    }

    @Test
    public void testGetField_WithValidId_ReturnsField() throws Exception {
        // Arrange - create a test first
        OpenELISFieldForm form = new OpenELISFieldForm();
        form.setEntityType(OpenELISFieldForm.EntityType.TEST);
        form.setFieldName("TEST-Get Test " + System.currentTimeMillis());
        form.setTestCode("TEST-GET-" + System.currentTimeMillis());
        form.setDescription("Get test");
        form.setResultType("NUMERIC");

        String fieldId = openELISFieldService.createField(form);
        assert fieldId != null;

        // Act & Assert
        mockMvc.perform(get("/rest/analyzer/openelis-fields/" + fieldId).param("entityType", "TEST"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(fieldId))
                .andExpect(jsonPath("$.name").exists()).andExpect(jsonPath("$.entityType").value("TEST"));
    }

    @Test
    public void testGetField_WithInvalidId_ReturnsNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/rest/analyzer/openelis-fields/INVALID-ID").param("entityType", "TEST"))
                .andExpect(status().isNotFound());
    }
}
