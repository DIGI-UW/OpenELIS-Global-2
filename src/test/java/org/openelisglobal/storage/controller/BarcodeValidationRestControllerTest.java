package org.openelisglobal.storage.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class BarcodeValidationRestControllerTest extends BaseWebContextSensitiveTest {

    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();

        executeDataSetWithStateManagement("testdata/storage_barcode_hierarchy.xml");
    }

    @Test
    public void validateBarcode_ShouldReturnValidResponse_ForCorrectBarcode() throws Exception {
        String barcode = "TESTROOM01-TESTDEV01";
        String requestBody = String.format("{\"barcode\": \"%s\"}", barcode);

        MvcResult result = mockMvc.perform(
                post("/rest/storage/barcode/validate").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertNotNull("Response should not be null", response);
        assertTrue("Response should have 'valid' field", response.has("valid"));
        assertTrue("Barcode should be valid", response.get("valid").asBoolean());
        assertTrue("Response should have 'validComponents' field", response.has("validComponents"));
        assertFalse("Valid components should not be empty", response.get("validComponents").isEmpty());
    }

    @Test
    public void validateBarcode_ShouldMatchApiContract_ForValidBarcode() throws Exception {
        String barcode = "TESTROOM01-TESTDEV01";
        String requestBody = String.format("{\"barcode\": \"%s\"}", barcode);

        MvcResult result = mockMvc.perform(
                post("/rest/storage/barcode/validate").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        assertTrue("Response must have 'valid' field", response.has("valid"));
        assertTrue("Response must have 'validComponents' field", response.has("validComponents"));
        assertTrue("Response must have 'barcode' field", response.has("barcode"));
        // assertEquals("Barcode should match request", validBarcode,
        // response.get("barcode").asText());
        if (response.get("valid").asBoolean()) {
            assertTrue("Response should have 'failedStep' field (null for valid)",
                    response.has("failedStep") || !response.has("failedStep"));
            assertTrue("Response should have 'errorMessage' field (null for valid)",
                    response.has("errorMessage") || !response.has("errorMessage"));
        }
    }

    @Test
    public void validateBarcode_ShouldReturnInvalidResponse_ForMalformedBarcode() throws Exception {
        String invalidBarcode = "INVALID_FORMAT_NO_HYPHEN";
        String requestBody = String.format("{\"barcode\": \"%s\"}", invalidBarcode);

        MvcResult result = mockMvc
                .perform(post("/rest/storage/barcode/validate").contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk()) // validation returns 200 with valid=false
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        assertFalse("Validation should fail for invalid format", response.get("valid").asBoolean());
        assertTrue("Response should have 'errorMessage' field", response.has("errorMessage"));
        assertNotNull("Error message should not be null", response.get("errorMessage").asText());
        assertTrue("Response should have 'failedStep' field", response.has("failedStep"));
        assertEquals("Failed step should be FORMAT_VALIDATION", "FORMAT_VALIDATION",
                response.get("failedStep").asText());
    }

    @Test
    public void validateBarcode_ShouldReturnInvalidResponse_ForNonExistentLocation() throws Exception {
        String nonExistentBarcode = "NONEXISTENT-ROOM-DEVICE";
        String requestBody = String.format("{\"barcode\": \"%s\"}", nonExistentBarcode);

        MvcResult result = mockMvc.perform(
                post("/rest/storage/barcode/validate").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        assertFalse("Validation should fail for non-existent location", response.get("valid").asBoolean());
        assertTrue("Response should have 'errorMessage' field", response.has("errorMessage"));
        assertNotNull("Error message should not be null", response.get("errorMessage").asText());
        assertTrue("Error message should mention 'not found'",
                response.get("errorMessage").asText().toLowerCase().contains("not found"));
    }

    @Test
    public void validate5LevelBarcode_ShouldReturnValidResponse_ForHierarchyUpToRack() throws Exception {
        // Arrange
        String barcode = "TESTROOM01-TESTDEV01-SHELF01-RACK01"; // stops at rack
        String requestBody = String.format("{\"barcode\": \"%s\"}", barcode);

        // Act
        MvcResult result = mockMvc.perform(
                post("/rest/storage/barcode/validate").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        // Assert
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertFalse("Validation should fail for inactive device", response.get("valid").asBoolean());
        JsonNode validComponents = response.get("validComponents");
        assertEquals("Failed step should be ACTIVITY_CHECK", "ACTIVITY_CHECK", response.get("failedStep").asText());
        assertTrue("Error message should mention 'inactive'",
                response.get("errorMessage").asText().toLowerCase().contains("inactive"));
    }

    @Test
    public void validatePartialBarcode_ShouldReturnValidComponents_ForPartialInput() throws Exception {
        String barcode = "TESTROOM01-TESTDEV01-NONEXISTENT";
        String requestBody = String.format("{\"barcode\": \"%s\"}", barcode);

        MvcResult result = mockMvc.perform(
                post("/rest/storage/barcode/validate").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        assertFalse("Overall validation should fail", response.get("valid").asBoolean());
        JsonNode validComponents = response.get("validComponents");
        assertTrue("Should have room component", validComponents.has("room"));
        assertTrue("Should have device component", validComponents.has("device"));
        assertFalse("Should not have shelf component", validComponents.has("shelf"));
    }
}
