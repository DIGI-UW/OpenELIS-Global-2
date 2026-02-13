package org.openelisglobal.analyzer.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.form.MappingPreviewForm;
import org.openelisglobal.analyzer.service.AnalyzerMappingPreviewService;
import org.openelisglobal.analyzer.service.AppliedMapping;
import org.openelisglobal.analyzer.service.EntityPreview;
import org.openelisglobal.analyzer.service.MappingPreviewResult;
import org.openelisglobal.analyzer.service.ParsedField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for AnalyzerMappingPreviewRestController
 * 
 * 
 * Uses BaseWebContextSensitiveTest for integration testing with full Spring
 * context
 */
public class AnalyzerMappingPreviewRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalyzerMappingPreviewService analyzerMappingPreviewService;

    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
    }

    /**
     * Test: Preview mapping with valid message returns structured response
     */
    @Test
    public void testPreviewMapping_WithValidMessage_ReturnsStructuredResponse() throws Exception {
        // Arrange: Valid ASTM message
        String analyzerId = "1";
        String astmMessage = "H|\\^&|||PSM^Micro^2.0|...\nP|1||...\nO|1||...\nR|1|^GLUCOSE^...|123|mg/dL|...";

        MappingPreviewForm form = new MappingPreviewForm();
        form.setAstmMessage(astmMessage);
        form.setIncludeDetailedParsing(false);
        form.setValidateAllMappings(false);

        // Mock service response
        MappingPreviewResult mockResult = new MappingPreviewResult();
        List<ParsedField> parsedFields = new ArrayList<>();
        ParsedField field = new ParsedField();
        field.setFieldName("GLUCOSE");
        field.setAstmRef("R|1");
        field.setRawValue("123");
        field.setFieldType("NUMERIC");
        parsedFields.add(field);
        mockResult.setParsedFields(parsedFields);

        List<AppliedMapping> appliedMappings = new ArrayList<>();
        AppliedMapping mapping = new AppliedMapping();
        mapping.setAnalyzerFieldName("GLUCOSE");
        mapping.setOpenelisFieldId("TEST-001");
        mapping.setOpenelisFieldType("TEST");
        mapping.setMappedValue("123");
        mapping.setMappingId("MAPPING-001");
        appliedMappings.add(mapping);
        mockResult.setAppliedMappings(appliedMappings);

        EntityPreview entityPreview = new EntityPreview();
        List<Map<String, Object>> tests = new ArrayList<>();
        Map<String, Object> test = new HashMap<>();
        test.put("id", "TEST-001");
        test.put("name", "GLUCOSE");
        tests.add(test);
        entityPreview.setTests(tests);
        mockResult.setEntityPreview(entityPreview);

        mockResult.setWarnings(new ArrayList<>());
        mockResult.setErrors(new ArrayList<>());

        // Note: Using real service since we're in integration test context
        // The service will be called with real implementation
        // For this test, we'll verify the endpoint structure and response format

        // Act & Assert
        MvcResult result = mockMvc
                .perform(post("/rest/analyzer/analyzers/{id}/preview-mapping", analyzerId)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.parsedFields").exists())
                .andExpect(jsonPath("$.appliedMappings").exists()).andExpect(jsonPath("$.entityPreview").exists())
                .andExpect(jsonPath("$.warnings").exists()).andExpect(jsonPath("$.errors").exists()).andReturn();

        // Verify response structure
        String responseBody = result.getResponse().getContentAsString();
        assert responseBody != null && !responseBody.isEmpty();
    }

    /**
     * Test: Preview mapping with large message returns bad request
     */
    @Test
    public void testPreviewMapping_WithLargeMessage_ReturnsBadRequest() throws Exception {
        // Arrange: Message exceeding 10KB limit
        String analyzerId = "1";
        String largeMessage = "x".repeat(11 * 1024); // Exceeds 10KB

        MappingPreviewForm form = new MappingPreviewForm();
        form.setAstmMessage(largeMessage);

        // Act & Assert: Oversized messages now return 413 PAYLOAD_TOO_LARGE
        mockMvc.perform(post("/rest/analyzer/analyzers/{id}/preview-mapping", analyzerId)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(form)))
                .andExpect(status().is(413)).andExpect(jsonPath("$.error").exists());
    }

    /**
     * Test: Preview mapping with null message returns bad request
     */
    @Test
    public void testPreviewMapping_WithNullMessage_ReturnsBadRequest() throws Exception {
        // Arrange: Invalid request (null astmMessage)
        String analyzerId = "1";

        MappingPreviewForm form = new MappingPreviewForm();
        form.setAstmMessage(null); // Null message

        // Act & Assert: Validation should fail
        mockMvc.perform(post("/rest/analyzer/analyzers/{id}/preview-mapping", analyzerId)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isBadRequest());
    }
}
