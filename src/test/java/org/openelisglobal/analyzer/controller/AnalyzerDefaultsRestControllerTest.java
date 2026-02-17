package org.openelisglobal.analyzer.controller;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for analyzer defaults API endpoints.
 *
 * <p>
 * Tests the filesystem-backed default configuration loading feature for
 * GenericASTM and GenericHL7 plugins.
 *
 * <p>
 */
public class AnalyzerDefaultsRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    /**
     * Test that GET /rest/analyzer/defaults returns list of available templates.
     *
     * <p>
     * Expected response format: [ {"id": "astm/mindray-ba88a", "protocol": "ASTM",
     * "analyzerName": "Mindray BA-88A"}, {"id": "hl7/mindray-bc2000", "protocol":
     * "HL7", "analyzerName": "Mindray BC2000"} ]
     */
    @Test
    public void testGetDefaults_ReturnsListOfAvailableTemplates() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/rest/analyzer/defaults").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$[0]").exists());

        // Additional validation: check at least one ASTM and one HL7 template exists
        MvcResult result = mockMvc.perform(get("/rest/analyzer/defaults").contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue("Response should contain ASTM templates", responseBody.contains("\"protocol\":\"ASTM\""));
        assertTrue("Response should contain HL7 templates", responseBody.contains("\"protocol\":\"HL7\""));
    }

    /**
     * Test that GET /rest/analyzer/defaults/{protocol}/{name} returns template
     * JSON.
     *
     * <p>
     * Tests loading a specific template (e.g., mindray-bc2000).
     */
    @Test
    public void testGetDefaultConfig_ValidTemplate_ReturnsJson() throws Exception {
        // Act & Assert: Load BC2000 default config
        // Schema v2: $schema is a URL, protocol is an object {name, version}
        mockMvc.perform(get("/rest/analyzer/defaults/hl7/mindray-bc2000").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.$schema").exists())
                .andExpect(jsonPath("$.analyzer_name").value("Mindray BC2000"))
                .andExpect(jsonPath("$.protocol.name").value("HL7"))
                .andExpect(jsonPath("$.identifier_pattern").value("MINDRAY.*BC.?2000"))
                .andExpect(jsonPath("$.default_test_mappings").isArray());
    }

    /**
     * Test that GET /rest/analyzer/defaults/{protocol}/{name} returns 404 for
     * non-existent template.
     */
    @Test
    public void testGetDefaultConfig_NonExistentTemplate_Returns404() throws Exception {
        // Act & Assert
        mockMvc.perform(
                get("/rest/analyzer/defaults/hl7/non-existent-analyzer").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").exists());
    }

    /**
     * Test path traversal prevention: ../ should be rejected.
     */
    @Test
    public void testGetDefaultConfig_PathTraversalAttempt_Returns400() throws Exception {
        // Act & Assert: Path traversal rejected (400 or 404)
        mockMvc.perform(get("/rest/analyzer/defaults/hl7/../../../etc/passwd").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    /**
     * Test invalid protocol: only 'astm' and 'hl7' allowed.
     */
    @Test
    public void testGetDefaultConfig_InvalidProtocol_Returns400() throws Exception {
        // Act & Assert: Attempt invalid protocol
        MvcResult result = mockMvc
                .perform(get("/rest/analyzer/defaults/sql-injection/test").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists()).andReturn();
        assertTrue("Error message should mention protocol",
                result.getResponse().getContentAsString().contains("protocol"));
    }

    /**
     * Test that special characters in filename are rejected.
     */
    @Test
    public void testGetDefaultConfig_SpecialCharactersInFilename_Returns400() throws Exception {
        // Act & Assert: Special characters rejected (400 or 404)
        mockMvc.perform(get("/rest/analyzer/defaults/hl7/test;rm -rf").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    /**
     * Test that absolute paths are rejected.
     */
    @Test
    public void testGetDefaultConfig_AbsolutePath_Returns400() throws Exception {
        // Act & Assert: Invalid path rejected (400 or 404)
        mockMvc.perform(get("/rest/analyzer/defaults/hl7//etc/shadow").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    /**
     * Test that defaults API works with analyzer-defaults directory structure.
     *
     * <p>
     * Validates expected directory structure: - analyzer-defaults/astm/*.json -
     * analyzer-defaults/hl7/*.json
     */
    @Test
    public void testGetDefaults_RespectsDirectoryStructure_AstmAndHl7Separated() throws Exception {
        // Act: Get defaults list
        MvcResult result = mockMvc.perform(get("/rest/analyzer/defaults").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        String responseBody = result.getResponse().getContentAsString();

        // Assert: ASTM templates should have 'astm/' prefix
        assertTrue("ASTM templates should be under astm/ directory", responseBody.contains("\"id\":\"astm/"));

        // Assert: HL7 templates should have 'hl7/' prefix
        assertTrue("HL7 templates should be under hl7/ directory", responseBody.contains("\"id\":\"hl7/"));
    }
}
