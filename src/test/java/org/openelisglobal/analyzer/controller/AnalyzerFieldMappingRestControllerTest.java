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
            // Identify analyzer IDs created by tests
            String analyzerIdSubquery = "(SELECT id FROM analyzer WHERE name LIKE 'TEST-%')";
            String analyzerFieldSubquery = "(SELECT id FROM analyzer_field WHERE analyzer_id IN " + analyzerIdSubquery
                    + ")";

            // Delete dependent mappings first (respecting FK constraints)
            jdbcTemplate.execute(
                    "DELETE FROM qualitative_result_mapping WHERE analyzer_field_id IN " + analyzerFieldSubquery);
            jdbcTemplate.execute("DELETE FROM unit_mapping WHERE analyzer_field_id IN " + analyzerFieldSubquery);
            jdbcTemplate
                    .execute("DELETE FROM analyzer_field_mapping WHERE analyzer_field_id IN " + analyzerFieldSubquery);
            jdbcTemplate.execute("DELETE FROM analyzer_field_mapping WHERE analyzer_id IN " + analyzerIdSubquery);

            // Delete analyzer fields
            jdbcTemplate.execute("DELETE FROM analyzer_field WHERE analyzer_id IN " + analyzerIdSubquery);

            // Finally delete the analyzers
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

        // Verify analyzer was created
        Analyzer analyzer = analyzerService.get(analyzerId);
        if (analyzer == null) {
            throw new RuntimeException("Failed to create test analyzer - analyzer is null");
        }

        // Create analyzer field directly via service (no REST endpoint yet)
        AnalyzerField field = new AnalyzerField();
        field.setAnalyzer(analyzer);
        field.setFieldName("GLUCOSE");
        field.setFieldType(FieldType.NUMERIC);
        field.setUnit("mg/dL");
        field.setIsActive(true);
        String fieldId = analyzerFieldService.insert(field);

        // Verify field was created
        if (fieldId == null || fieldId.trim().isEmpty()) {
            throw new RuntimeException("Failed to create test analyzer field - fieldId is null or empty");
        }

        AnalyzerField createdField = analyzerFieldService.get(fieldId);
        if (createdField == null) {
            throw new RuntimeException("Failed to create test analyzer field - field not found after insert");
        }

        return new String[] { analyzerId, fieldId };
    }

    /**
     * Test: GET /rest/analyzer/analyzers/{analyzerId}/mappings returns list of
     * mappings
     * 
     * This test verifies that the mappings endpoint returns a direct array (not
     * wrapped in data object), which matches frontend expectations.
     */
    @Test
    public void testGetMappings_WithAnalyzerId_ReturnsMappings() throws Exception {
        // Arrange: Create test analyzer and field
        String[] ids = createTestAnalyzerAndField();
        String analyzerId = ids[0];

        // Act & Assert: GET endpoint should return empty list (no mappings yet)
        // Note: Mappings endpoint returns direct array, not wrapped in { data: {...} }
        mockMvc.perform(
                get("/rest/analyzer/analyzers/" + analyzerId + "/mappings").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0)); // Empty array initially
    }

    /**
     * Test: GET /rest/analyzer/analyzers/{analyzerId}/mappings response format
     * matches frontend expectations
     * 
     * This test verifies that when mappings exist, they are returned in the correct
     * format that the frontend expects: direct array of mapping objects with all
     * required fields.
     * 
     */
    @Test
    public void testGetMappings_WithExistingMappings_ReturnsCorrectFormat() throws Exception {
        // Arrange: Create test analyzer, field, and mapping
        String[] ids = createTestAnalyzerAndField();
        String analyzerId = ids[0];
        String fieldId = ids[1];

        // Create mapping
        String requestBody = "{\"analyzerFieldId\":\"" + fieldId + "\",\"openelisFieldId\":\"test-field-123\","
                + "\"openelisFieldType\":\"TEST\"," + "\"mappingType\":\"TEST_LEVEL\","
                + "\"isRequired\":false,\"isActive\":true}";

        MvcResult createResult = mockMvc
                .perform(post("/rest/analyzer/analyzers/" + analyzerId + "/mappings")
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isCreated()).andReturn();

        // Act & Assert: GET endpoint should return mappings in correct format
        mockMvc.perform(
                get("/rest/analyzer/analyzers/" + analyzerId + "/mappings").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // Verify direct array response (not wrapped in data object)
                .andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$.length()").value(1))
                // Verify mapping object structure matches frontend expectations
                .andExpect(jsonPath("$[0].id").exists()).andExpect(jsonPath("$[0].analyzerFieldId").value(fieldId))
                .andExpect(jsonPath("$[0].analyzerFieldName").exists())
                .andExpect(jsonPath("$[0].analyzerFieldType").exists())
                .andExpect(jsonPath("$[0].openelisFieldId").value("test-field-123"))
                .andExpect(jsonPath("$[0].openelisFieldType").value("TEST"))
                .andExpect(jsonPath("$[0].mappingType").exists()).andExpect(jsonPath("$[0].isRequired").value(false))
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    /**
     * Test: POST /rest/analyzer/analyzers/{analyzerId}/mappings creates mapping
     * with valid data
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
     * incompatibility returns bad request
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
     * mapping
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
     * deletes mapping
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

    /**
     * Helper method to create a test analyzer with a field and mapping
     */
    private String[] createTestAnalyzerWithMapping() throws Exception {
        // Create source analyzer
        String[] ids = createTestAnalyzerAndField();
        String analyzerId = ids[0];
        String fieldId = ids[1];

        // Create mapping
        String requestBody = "{\"analyzerFieldId\":\"" + fieldId + "\",\"openelisFieldId\":\"test-field-123\","
                + "\"openelisFieldType\":\"TEST\"," + "\"mappingType\":\"TEST_LEVEL\","
                + "\"isRequired\":false,\"isActive\":true}";

        MvcResult createResult = mockMvc
                .perform(post("/rest/analyzer/analyzers/" + analyzerId + "/mappings")
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isCreated()).andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String mappingId = responseBody.substring(responseBody.indexOf("\"id\":\"") + 6);
        mappingId = mappingId.substring(0, mappingId.indexOf("\""));

        return new String[] { analyzerId, fieldId, mappingId };
    }

    /**
     * Test: POST /rest/analyzer/analyzers/{targetId}/copy-mappings with valid
     * request returns copy results
     */
    @Test
    public void testCopyMappings_WithValidRequest_ReturnsCopyResults() throws Exception {
        // Arrange: Create source analyzer with mapping
        String[] sourceIds = createTestAnalyzerWithMapping();
        String sourceAnalyzerId = sourceIds[0];

        // Create target analyzer with field (no mappings yet)
        String[] targetIds = createTestAnalyzerAndField();
        String targetAnalyzerId = targetIds[0];

        // Create request body
        String requestBody = "{\"sourceAnalyzerId\":\"" + sourceAnalyzerId + "\","
                + "\"overwriteExisting\":true,\"skipIncompatible\":true}";

        // Act & Assert: POST endpoint should copy mappings
        mockMvc.perform(post("/rest/analyzer/analyzers/" + targetAnalyzerId + "/copy-mappings")
                .contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isOk())
                .andExpect(jsonPath("$.copiedCount").exists()).andExpect(jsonPath("$.skippedCount").exists())
                .andExpect(jsonPath("$.warnings").isArray()).andExpect(jsonPath("$.conflicts").isArray())
                .andExpect(jsonPath("$.copiedCount").exists());
    }

    /**
     * Test: POST /rest/analyzer/analyzers/{targetId}/copy-mappings with no source
     * mappings returns bad request
     */
    @Test
    public void testCopyMappings_WithNoSourceMappings_ReturnsBadRequest() throws Exception {
        // Arrange: Create source analyzer with NO mappings
        String[] sourceIds = createTestAnalyzerAndField();
        String sourceAnalyzerId = sourceIds[0];

        // Create target analyzer
        String[] targetIds = createTestAnalyzerAndField();
        String targetAnalyzerId = targetIds[0];

        // Create request body
        String requestBody = "{\"sourceAnalyzerId\":\"" + sourceAnalyzerId + "\"}";

        // Act & Assert: POST endpoint should return bad request (source has no
        // mappings)
        mockMvc.perform(post("/rest/analyzer/analyzers/" + targetAnalyzerId + "/copy-mappings")
                .contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    /**
     * Test: POST /rest/analyzer/analyzers/{targetId}/copy-mappings with missing
     * sourceAnalyzerId returns bad request
     */
    @Test
    public void testCopyMappings_WithMissingSourceAnalyzerId_ReturnsBadRequest() throws Exception {
        // Arrange: Create target analyzer
        String[] targetIds = createTestAnalyzerAndField();
        String targetAnalyzerId = targetIds[0];

        // Create request body without sourceAnalyzerId
        String requestBody = "{\"overwriteExisting\":true}";

        // Act & Assert: POST endpoint should return bad request
        mockMvc.perform(post("/rest/analyzer/analyzers/" + targetAnalyzerId + "/copy-mappings")
                .contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("sourceAnalyzerId is required"));
    }
}
