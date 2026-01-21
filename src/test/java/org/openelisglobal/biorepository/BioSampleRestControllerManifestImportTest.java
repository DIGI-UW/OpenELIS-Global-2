package org.openelisglobal.biorepository;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.biorepository.controller.rest.dto.ManifestImportRequest;
import org.openelisglobal.biorepository.controller.rest.dto.SampleRegistrationDTO;
import org.openelisglobal.biorepository.service.BioSampleService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for BioSampleRestController manifest import endpoints.
 * Tests the validate-manifest-import and register-bulk endpoints.
 */
public class BioSampleRestControllerManifestImportTest extends BaseWebContextSensitiveTest {

    @Autowired
    private BioSampleService bioSampleService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private SystemUserService systemUserService;

    private ObjectMapper objectMapper;
    private SystemUser testUser;
    private TypeOfSample testSampleType;
    private UserSessionData userSessionData;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();

        // Setup user session data for authentication
        userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);

        // Setup test user
        testUser = systemUserService.get("1");
        if (testUser == null) {
            testUser = new SystemUser();
            testUser.setLoginName("test_manifest_user");
            testUser.setFirstName("Test");
            testUser.setLastName("Manifest User");
            testUser.setIsActive("Y");
            testUser.setSysUserId("1");
            systemUserService.save(testUser);
        }

        // Setup test sample type
        var sampleTypes = typeOfSampleService.getAll();
        if (!sampleTypes.isEmpty()) {
            testSampleType = sampleTypes.get(0);
        } else {
            testSampleType = new TypeOfSample();
            testSampleType.setDescription("Serum");
            testSampleType.setDomain("H");
            typeOfSampleService.save(testSampleType);
        }
    }

    // ========== VALIDATE MANIFEST IMPORT TESTS ==========

    @Test
    public void testValidateManifestImport_ValidSamples_ReturnsValid() throws Exception {
        // Arrange
        ManifestImportRequest request = new ManifestImportRequest();
        List<SampleRegistrationDTO> samples = new ArrayList<>();

        SampleRegistrationDTO sample1 = createValidSampleDTO("VALID-IMPORT-" + System.currentTimeMillis());
        samples.add(sample1);

        request.setSamples(samples);

        // Act
        MvcResult result = mockMvc
                .perform(post("/rest/biorepository/sample/validate-manifest-import")
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseContent);

        assertTrue("Response should have 'valid' field", response.has("valid"));
        assertTrue("Validation should pass for valid samples", response.get("valid").asBoolean());
        assertEquals("Invalid count should be 0", 0, response.get("invalidCount").asInt());
        assertTrue("Response should have 'rows' field", response.has("rows"));
        assertEquals("Should have 1 row result", 1, response.get("rows").size());

        JsonNode firstRow = response.get("rows").get(0);
        assertTrue("First row should be valid", firstRow.get("valid").asBoolean());
        assertEquals("First row should have no errors", 0, firstRow.get("errors").size());
    }

    @Test
    public void testValidateManifestImport_MultipleSamples_AllValid() throws Exception {
        // Arrange
        ManifestImportRequest request = new ManifestImportRequest();
        List<SampleRegistrationDTO> samples = new ArrayList<>();

        long timestamp = System.currentTimeMillis();
        for (int i = 0; i < 3; i++) {
            samples.add(createValidSampleDTO("MULTI-VALID-" + timestamp + "-" + i));
        }

        request.setSamples(samples);

        // Act
        MvcResult result = mockMvc
                .perform(post("/rest/biorepository/sample/validate-manifest-import")
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andReturn();

        // Assert
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        assertTrue("All samples should be valid", response.get("valid").asBoolean());
        assertEquals("Invalid count should be 0", 0, response.get("invalidCount").asInt());
        assertEquals("Should have 3 row results", 3, response.get("rows").size());

        for (JsonNode row : response.get("rows")) {
            assertTrue("Each row should be valid", row.get("valid").asBoolean());
        }
    }

    @Test
    public void testValidateManifestImport_MissingExternalId_ReturnsError() throws Exception {
        // Arrange
        ManifestImportRequest request = new ManifestImportRequest();
        List<SampleRegistrationDTO> samples = new ArrayList<>();

        SampleRegistrationDTO sample = createValidSampleDTO(null); // Missing external ID
        samples.add(sample);
        request.setSamples(samples);

        // Act
        MvcResult result = mockMvc
                .perform(post("/rest/biorepository/sample/validate-manifest-import")
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andReturn();

        // Assert
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        assertFalse("Validation should fail", response.get("valid").asBoolean());
        assertEquals("Invalid count should be 1", 1, response.get("invalidCount").asInt());

        JsonNode firstRow = response.get("rows").get(0);
        assertFalse("Row should be invalid", firstRow.get("valid").asBoolean());
        assertTrue("Row should have errors", firstRow.get("errors").size() > 0);

        String errorMessage = firstRow.get("errors").get(0).asText();
        assertTrue("Error should mention external ID", errorMessage.toLowerCase().contains("external id"));
    }

    @Test
    public void testValidateManifestImport_MissingSampleType_ReturnsError() throws Exception {
        // Arrange
        ManifestImportRequest request = new ManifestImportRequest();
        List<SampleRegistrationDTO> samples = new ArrayList<>();

        SampleRegistrationDTO sample = new SampleRegistrationDTO();
        sample.setExternalId("MISSING-TYPE-" + System.currentTimeMillis());
        sample.setBiosafetyLevel("BSL_1");
        sample.setOriginLab("Test Lab");
        // Missing sample type
        samples.add(sample);
        request.setSamples(samples);

        // Act
        MvcResult result = mockMvc
                .perform(post("/rest/biorepository/sample/validate-manifest-import")
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andReturn();

        // Assert
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        assertFalse("Validation should fail", response.get("valid").asBoolean());

        JsonNode firstRow = response.get("rows").get(0);
        assertFalse("Row should be invalid", firstRow.get("valid").asBoolean());

        boolean foundSampleTypeError = false;
        for (JsonNode error : firstRow.get("errors")) {
            if (error.asText().toLowerCase().contains("sample type")) {
                foundSampleTypeError = true;
                break;
            }
        }
        assertTrue("Error should mention sample type", foundSampleTypeError);
    }

    @Test
    public void testValidateManifestImport_InvalidBiosafetyLevel_ReturnsError() throws Exception {
        // Arrange
        ManifestImportRequest request = new ManifestImportRequest();
        List<SampleRegistrationDTO> samples = new ArrayList<>();

        SampleRegistrationDTO sample = createValidSampleDTO("INVALID-BSL-" + System.currentTimeMillis());
        sample.setBiosafetyLevel("BSL_5"); // Invalid - only BSL_1 to BSL_4 exist
        samples.add(sample);
        request.setSamples(samples);

        // Act
        MvcResult result = mockMvc
                .perform(post("/rest/biorepository/sample/validate-manifest-import")
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andReturn();

        // Assert
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        assertFalse("Validation should fail", response.get("valid").asBoolean());

        JsonNode firstRow = response.get("rows").get(0);
        assertFalse("Row should be invalid", firstRow.get("valid").asBoolean());

        boolean foundBslError = false;
        for (JsonNode error : firstRow.get("errors")) {
            if (error.asText().toLowerCase().contains("biosafety")) {
                foundBslError = true;
                break;
            }
        }
        assertTrue("Error should mention biosafety level", foundBslError);
    }

    @Test
    public void testValidateManifestImport_DuplicateBarcodeInManifest_ReturnsError() throws Exception {
        // Arrange
        ManifestImportRequest request = new ManifestImportRequest();
        List<SampleRegistrationDTO> samples = new ArrayList<>();

        String duplicateBarcode = "DUPLICATE-BC-" + System.currentTimeMillis();

        SampleRegistrationDTO sample1 = createValidSampleDTO(duplicateBarcode);
        SampleRegistrationDTO sample2 = createValidSampleDTO(duplicateBarcode); // Same barcode

        samples.add(sample1);
        samples.add(sample2);
        request.setSamples(samples);

        // Act
        MvcResult result = mockMvc
                .perform(post("/rest/biorepository/sample/validate-manifest-import")
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andReturn();

        // Assert
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        assertFalse("Validation should fail due to duplicate", response.get("valid").asBoolean());
        assertEquals("Invalid count should be at least 1", 1, response.get("invalidCount").asInt());

        // Second row should have the duplicate error
        JsonNode secondRow = response.get("rows").get(1);
        assertFalse("Second row should be invalid", secondRow.get("valid").asBoolean());

        boolean foundDuplicateError = false;
        for (JsonNode error : secondRow.get("errors")) {
            if (error.asText().toLowerCase().contains("duplicate")) {
                foundDuplicateError = true;
                break;
            }
        }
        assertTrue("Error should mention duplicate barcode", foundDuplicateError);
    }

    @Test
    public void testValidateManifestImport_BarcodeExistsInDatabase_ReturnsError() throws Exception {
        // Arrange - Create an existing sample in the database
        String existingBarcode = "EXISTING-BC-" + System.currentTimeMillis();
        createExistingSampleItem(existingBarcode);

        ManifestImportRequest request = new ManifestImportRequest();
        List<SampleRegistrationDTO> samples = new ArrayList<>();

        SampleRegistrationDTO sample = createValidSampleDTO(existingBarcode); // Same barcode as existing
        samples.add(sample);
        request.setSamples(samples);

        // Act
        MvcResult result = mockMvc
                .perform(post("/rest/biorepository/sample/validate-manifest-import")
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andReturn();

        // Assert
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        assertFalse("Validation should fail - barcode exists", response.get("valid").asBoolean());

        JsonNode firstRow = response.get("rows").get(0);
        assertFalse("Row should be invalid", firstRow.get("valid").asBoolean());

        boolean foundExistsError = false;
        for (JsonNode error : firstRow.get("errors")) {
            if (error.asText().toLowerCase().contains("already exists")) {
                foundExistsError = true;
                break;
            }
        }
        assertTrue("Error should mention barcode already exists", foundExistsError);
    }

    @Test
    public void testValidateManifestImport_MissingOriginLab_ReturnsError() throws Exception {
        // Arrange
        ManifestImportRequest request = new ManifestImportRequest();
        List<SampleRegistrationDTO> samples = new ArrayList<>();

        SampleRegistrationDTO sample = new SampleRegistrationDTO();
        sample.setExternalId("NO-LAB-" + System.currentTimeMillis());
        sample.setSampleType(testSampleType.getId());
        sample.setBiosafetyLevel("BSL_1");
        // Missing origin lab
        samples.add(sample);
        request.setSamples(samples);

        // Act
        MvcResult result = mockMvc
                .perform(post("/rest/biorepository/sample/validate-manifest-import")
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andReturn();

        // Assert
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        assertFalse("Validation should fail", response.get("valid").asBoolean());

        JsonNode firstRow = response.get("rows").get(0);
        boolean foundOriginLabError = false;
        for (JsonNode error : firstRow.get("errors")) {
            if (error.asText().toLowerCase().contains("origin lab")) {
                foundOriginLabError = true;
                break;
            }
        }
        assertTrue("Error should mention origin lab", foundOriginLabError);
    }

    @Test
    public void testValidateManifestImport_EmptySamplesList_ReturnsError() throws Exception {
        // Arrange
        ManifestImportRequest request = new ManifestImportRequest();
        request.setSamples(new ArrayList<>());

        // Act
        MvcResult result = mockMvc
                .perform(post("/rest/biorepository/sample/validate-manifest-import")
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andReturn();

        // Assert
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        assertFalse("Validation should fail for empty list", response.get("valid").asBoolean());
    }

    // ========== REGISTER BULK TESTS ==========

    @Test
    public void testRegisterBulk_ValidSamples_RegistersSuccessfully() throws Exception {
        // Arrange
        ManifestImportRequest request = new ManifestImportRequest();
        List<SampleRegistrationDTO> samples = new ArrayList<>();

        long timestamp = System.currentTimeMillis();
        String barcode1 = "BULK-REG-" + timestamp + "-1";
        String barcode2 = "BULK-REG-" + timestamp + "-2";

        samples.add(createValidSampleDTO(barcode1));
        samples.add(createValidSampleDTO(barcode2));
        request.setSamples(samples);

        // Act
        MvcResult result = mockMvc
                .perform(post("/rest/biorepository/sample/register-bulk").contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("userSessionData", userSessionData)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        // Assert
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        assertTrue("Response should indicate success", response.get("success").asBoolean());
        assertEquals("Should register 2 samples", 2, response.get("registeredCount").asInt());
        assertTrue("Response should have 'samples' array", response.has("samples"));
        assertEquals("Should have 2 registered samples", 2, response.get("samples").size());

        // Verify each registered sample has expected fields
        for (JsonNode registeredSample : response.get("samples")) {
            assertTrue("Sample should have 'id'", registeredSample.has("id"));
            assertTrue("Sample should have 'barcode'", registeredSample.has("barcode"));
            assertTrue("Sample should have 'sampleItemId'", registeredSample.has("sampleItemId"));
            assertTrue("Sample should have 'sampleId'", registeredSample.has("sampleId"));

            assertNotNull("ID should not be null", registeredSample.get("id"));
            assertTrue("ID should be positive", registeredSample.get("id").asInt() > 0);
        }

        // Verify samples actually exist in database
        assertTrue("First barcode should exist after registration", bioSampleService.barcodeExists(barcode1));
        assertTrue("Second barcode should exist after registration", bioSampleService.barcodeExists(barcode2));
    }

    @Test
    public void testRegisterBulk_WithAllOptionalFields_RegistersSuccessfully() throws Exception {
        // Arrange
        ManifestImportRequest request = new ManifestImportRequest();
        List<SampleRegistrationDTO> samples = new ArrayList<>();

        SampleRegistrationDTO sample = createValidSampleDTO("FULL-FIELDS-" + System.currentTimeMillis());
        sample.setConsentId("CONSENT-123");
        sample.setEthicsApprovalRef("ETH-2026-001");
        sample.setMtaReference("MTA-2026-001");
        sample.setPrincipalInvestigator("Dr. Test Investigator");
        sample.setPreservationMedium("EDTA");
        sample.setArrivalCondition("Good");
        sample.setSpecialHandling("Handle with care");
        sample.setCollectionDate("2026-01-15");
        // Add projectId - critical for retention policy calculation
        sample.setProjectId("PROJ-TEST-001");
        // originLab is already set by createValidSampleDTO

        samples.add(sample);
        request.setSamples(samples);

        // Act
        MvcResult result = mockMvc.perform(post("/rest/biorepository/sample/register-bulk")
                .contentType(MediaType.APPLICATION_JSON).sessionAttr("userSessionData", userSessionData)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk()).andReturn();

        // Assert
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        assertTrue("Registration should succeed", response.get("success").asBoolean());
        assertEquals("Should register 1 sample", 1, response.get("registeredCount").asInt());

        // Verify the sample was actually registered with all fields
        JsonNode registeredSample = response.get("samples").get(0);
        Integer bioSampleId = registeredSample.get("id").asInt();
        assertNotNull("BioSample ID should not be null", bioSampleId);

        var bioSample = bioSampleService.get(bioSampleId);
        assertNotNull("BioSample should exist in database", bioSample);
        assertEquals("Ethics approval should be stored", "ETH-2026-001", bioSample.getEthicsApprovalRef());
        assertEquals("MTA reference should be stored", "MTA-2026-001", bioSample.getMtaReference());
        assertEquals("Consent ID should be stored", "CONSENT-123", bioSample.getConsentId());
        assertEquals("PI should be stored", "Dr. Test Investigator", bioSample.getPrincipalInvestigator());
        assertEquals("Preservation medium should be stored", "EDTA", bioSample.getPreservationMedium());
        assertEquals("Arrival condition should be stored", "Good", bioSample.getArrivalCondition());
        assertEquals("Special handling should be stored", "Handle with care", bioSample.getSpecialHandling());
        // Verify originLab and projectId are persisted - critical for retention policy
        assertEquals("Origin lab should be stored", "Test Integration Lab", bioSample.getOriginLab());
        assertEquals("Project ID should be stored", "PROJ-TEST-001", bioSample.getProjectId());
    }

    @Test
    public void testRegisterBulk_EmptySamplesList_ReturnsBadRequest() throws Exception {
        // Arrange
        ManifestImportRequest request = new ManifestImportRequest();
        request.setSamples(new ArrayList<>());

        // Act
        MvcResult result = mockMvc
                .perform(post("/rest/biorepository/sample/register-bulk").contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("userSessionData", userSessionData)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()).andReturn();

        // Assert
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        assertFalse("Registration should fail", response.get("success").asBoolean());
        assertTrue("Error message should be present", response.has("error"));
    }

    @Test
    public void testRegisterBulk_InvalidSampleType_ReturnsError() throws Exception {
        // Arrange
        ManifestImportRequest request = new ManifestImportRequest();
        List<SampleRegistrationDTO> samples = new ArrayList<>();

        SampleRegistrationDTO sample = new SampleRegistrationDTO();
        sample.setExternalId("INVALID-TYPE-" + System.currentTimeMillis());
        sample.setSampleType("NonExistentSampleType12345");
        sample.setBiosafetyLevel("BSL_1");
        sample.setOriginLab("Test Lab");

        samples.add(sample);
        request.setSamples(samples);

        // Act
        MvcResult result = mockMvc
                .perform(post("/rest/biorepository/sample/register-bulk").contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("userSessionData", userSessionData)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError()).andReturn();

        // Assert
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        assertFalse("Registration should fail", response.get("success").asBoolean());
        assertTrue("Error message should be present", response.has("error"));
        assertTrue("Error should mention sample type",
                response.get("error").asText().toLowerCase().contains("sample type"));
    }

    // ========== HELPER METHODS ==========

    private SampleRegistrationDTO createValidSampleDTO(String externalId) {
        SampleRegistrationDTO dto = new SampleRegistrationDTO();
        dto.setExternalId(externalId);
        dto.setSampleType(testSampleType.getId());
        dto.setBiosafetyLevel("BSL_2");
        dto.setOriginLab("Test Integration Lab");
        dto.setCollectionDate("2026-01-15");
        return dto;
    }

    private void createExistingSampleItem(String barcode) {
        // Create Sample (accession level)
        Sample sample = new Sample();
        sample.setAccessionNumber("ACC-" + System.currentTimeMillis());
        sample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));
        sample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
        sample.setDomain("H");
        sample.setSysUserId(testUser.getId().toString());
        Sample savedSample = sampleService.save(sample);

        // Create SampleItem with the barcode
        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(savedSample);
        sampleItem.setExternalId(barcode);
        sampleItem.setTypeOfSample(testSampleType);
        sampleItem.setSortOrder("1");
        sampleItem.setStatusId("1");
        sampleItem.setSysUserId(testUser.getId().toString());
        sampleItemService.save(sampleItem);
    }
}
