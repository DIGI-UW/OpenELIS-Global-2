package org.openelisglobal.analyzer.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerFieldService;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.analyzer.valueholder.AnalyzerField.FieldType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for AnalyzerFieldMappingRestController Following TDD
 * approach: Write tests BEFORE implementation
 * 
 * Task Reference: T036 Test Coverage Goal: >80%
 */
public class AnalyzerFieldMappingRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private AnalyzerFieldService analyzerFieldService;

    private ObjectMapper objectMapper;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
        jdbcTemplate = new JdbcTemplate(dataSource);
        // Clean up analyzer test data before each test
        cleanAnalyzerTestData();
    }

    /**
     * Clean up analyzer-related test data Note: Must delete in order due to foreign
     * key constraints
     */
    private void cleanAnalyzerTestData() {
        try {
            // Delete test-created analyzer data in order (respecting foreign keys)
            jdbcTemplate.execute("DELETE FROM analyzer_field_mapping WHERE id LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM analyzer_field WHERE id LIKE 'TEST-%'");
            // Delete analyzer_configuration first (references analyzer)
            jdbcTemplate.execute("DELETE FROM analyzer_configuration "
                    + "WHERE analyzer_id IN (SELECT id FROM analyzer WHERE name LIKE 'TEST-%')");
            // Then delete analyzer (legacy table, clean by name pattern)
            jdbcTemplate.execute("DELETE FROM analyzer WHERE name LIKE 'TEST-%'");

            // Ensure analyzer sequence is synchronized with existing data
            Integer maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM analyzer", Integer.class);
            jdbcTemplate.execute("SELECT setval('analyzer_seq', " + maxId + ", true)");
        } catch (Exception e) {
            System.out.println("Failed to clean analyzer test data: " + e.getMessage());
        }
    }

    /**
     * Helper method to create a test analyzer and field
     */
    private String[] createTestAnalyzerAndField() throws Exception {
        // Create analyzer
        String uniqueName = "TEST-Mapping-Test-" + System.currentTimeMillis();
        String createBody = "{\"name\":\"" + uniqueName
                + "\",\"analyzerType\":\"Chemistry Analyzer\",\"ipAddress\":\"192.168.1.100\","
                + "\"port\":5000,\"testUnitIds\":[]}";

        MvcResult createResult = mockMvc
                .perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated()).andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String analyzerId = responseBody.substring(responseBody.indexOf("\"id\":\"") + 6);
        analyzerId = analyzerId.substring(0, analyzerId.indexOf("\""));

        // Create analyzer field directly via service (no REST endpoint yet)
        Analyzer analyzer = analyzerService.get(analyzerId);
        AnalyzerField field = new AnalyzerField();
        field.setAnalyzer(analyzer);
        field.setFieldName("GLUCOSE");
        field.setFieldType(FieldType.NUMERIC);
        field.setUnit("mg/dL");
        field.setIsActive(true);
        String fieldId = analyzerFieldService.insert(field);

        return new String[] { analyzerId, fieldId };
    }

    /**
     * Test: GET /rest/analyzer/analyzers/{analyzerId}/mappings returns list of
     * mappings Task Reference: T036
     */
    @Test
    public void testGetMappings_WithAnalyzerId_ReturnsMappings() throws Exception {
        // Arrange: Create test analyzer and field
        String[] ids = createTestAnalyzerAndField();
        String analyzerId = ids[0];

        // Act & Assert: GET endpoint should return empty list (no mappings yet)
        mockMvc.perform(
                get("/rest/analyzer/analyzers/" + analyzerId + "/mappings").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    /**
     * Test: POST /rest/analyzer/analyzers/{analyzerId}/mappings creates mapping
     * with valid data Task Reference: T036
     */
    @Test
    public void testCreateMapping_WithValidData_ReturnsCreated() throws Exception {
        // Arrange: Create test analyzer and field
        String[] ids = createTestAnalyzerAndField();
        String analyzerId = ids[0];
        String fieldId = ids[1];

        // Create mapping form JSON
        String requestBody = "{\"analyzerFieldId\":\"" + fieldId + "\",\"openelisFieldId\":\"test-field-123\","
                + "\"openelisFieldType\":\"TEST\"," + "\"mappingType\":\"TEST_LEVEL\","
                + "\"isRequired\":false,\"isActive\":false}";

        // Act & Assert: POST endpoint should create mapping
        mockMvc.perform(post("/rest/analyzer/analyzers/" + analyzerId + "/mappings")
                .contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists()).andExpect(jsonPath("$.analyzerFieldId").value(fieldId))
                .andExpect(jsonPath("$.openelisFieldId").value("test-field-123"));
    }

    /**
     * Test: POST /rest/analyzer/analyzers/{analyzerId}/mappings with type
     * incompatibility returns bad request Task Reference: T036
     */
    @Test
    public void testCreateMapping_WithTypeIncompatibility_ReturnsBadRequest() throws Exception {
        // Arrange: Create test analyzer and field (NUMERIC type)
        String[] ids = createTestAnalyzerAndField();
        String analyzerId = ids[0];
        String fieldId = ids[1];

        // Create mapping with incompatible types (NUMERIC field → QUALITATIVE OpenELIS
        // field)
        String requestBody = "{\"analyzerFieldId\":\"" + fieldId + "\",\"openelisFieldId\":\"qualitative-field-123\","
                + "\"openelisFieldType\":\"QUALITATIVE\"," + "\"mappingType\":\"TEST_LEVEL\","
                + "\"isRequired\":false,\"isActive\":false}";

        // Act & Assert: POST endpoint should reject incompatible types
        mockMvc.perform(post("/rest/analyzer/analyzers/" + analyzerId + "/mappings")
                .contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isBadRequest());
    }

    /**
     * Test: PUT /rest/analyzer/analyzers/{analyzerId}/mappings/{mappingId} updates
     * mapping Task Reference: T036
     */
    @Test
    public void testUpdateMapping_WithValidData_ReturnsUpdated() throws Exception {
        // Arrange: Create test analyzer, field, and mapping
        String[] ids = createTestAnalyzerAndField();
        String analyzerId = ids[0];
        String fieldId = ids[1];

        // Create mapping first
        String createBody = "{\"analyzerFieldId\":\"" + fieldId + "\",\"openelisFieldId\":\"test-field-123\","
                + "\"openelisFieldType\":\"TEST\"," + "\"mappingType\":\"TEST_LEVEL\","
                + "\"isRequired\":false,\"isActive\":false}";

        MvcResult createResult = mockMvc
                .perform(post("/rest/analyzer/analyzers/" + analyzerId + "/mappings")
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated()).andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String mappingId = responseBody.substring(responseBody.indexOf("\"id\":\"") + 6);
        mappingId = mappingId.substring(0, mappingId.indexOf("\""));

        // Update mapping
        String updateBody = "{\"openelisFieldId\":\"updated-field-456\",\"isActive\":true}";

        // Act & Assert: PUT endpoint should update mapping
        mockMvc.perform(put("/rest/analyzer/analyzers/" + analyzerId + "/mappings/" + mappingId)
                .contentType(MediaType.APPLICATION_JSON).content(updateBody)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mappingId)).andExpect(jsonPath("$.isActive").value(true));
    }

    /**
     * Test: DELETE /rest/analyzer/analyzers/{analyzerId}/mappings/{mappingId}
     * deletes mapping Task Reference: T036
     */
    @Test
    public void testDeleteMapping_WithValidId_ReturnsNoContent() throws Exception {
        // Arrange: Create test analyzer, field, and mapping
        String[] ids = createTestAnalyzerAndField();
        String analyzerId = ids[0];
        String fieldId = ids[1];

        // Create mapping first
        String createBody = "{\"analyzerFieldId\":\"" + fieldId + "\",\"openelisFieldId\":\"test-field-123\","
                + "\"openelisFieldType\":\"TEST\"," + "\"mappingType\":\"TEST_LEVEL\","
                + "\"isRequired\":false,\"isActive\":false}";

        MvcResult createResult = mockMvc
                .perform(post("/rest/analyzer/analyzers/" + analyzerId + "/mappings")
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated()).andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String mappingId = responseBody.substring(responseBody.indexOf("\"id\":\"") + 6);
        mappingId = mappingId.substring(0, mappingId.indexOf("\""));

        // Act & Assert: DELETE endpoint should delete mapping
        mockMvc.perform(delete("/rest/analyzer/analyzers/" + analyzerId + "/mappings/" + mappingId)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());
    }
}
