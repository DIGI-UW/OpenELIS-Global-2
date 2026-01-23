package org.openelisglobal.biorepository;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.biorepository.service.BioSampleService;
import org.openelisglobal.biorepository.service.ShipmentService;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.BiosafetyLevel;
import org.openelisglobal.biorepository.valueholder.Shipment;
import org.openelisglobal.biorepository.valueholder.Shipment.PackagingCondition;
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
 * Integration tests for BioSampleRestController.
 *
 * Tests verify: - GET /rest/biorepository/sample - list samples without
 * LazyInitializationException - GET /rest/biorepository/sample?shipmentId=X -
 * filter by shipment - GET /rest/biorepository/sample/{id} - get single sample
 * - POST /rest/biorepository/sample/register - register single sample - POST
 * /rest/biorepository/sample/validate-manifest-import - validate manifest -
 * POST /rest/biorepository/sample/register-bulk - bulk register samples
 */
public class BioSampleRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private BioSampleService bioSampleService;

    @Autowired
    private ShipmentService shipmentService;

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
    private Shipment testShipment;
    private Sample testSample;
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
            testUser.setLoginName("test_controller_user");
            testUser.setFirstName("Test");
            testUser.setLastName("Controller User");
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
            testSampleType.setDescription("Blood");
            testSampleType.setDomain("H");
            typeOfSampleService.save(testSampleType);
        }

        // Setup test shipment
        testShipment = createTestShipment("CTRL-SHIP-" + (System.currentTimeMillis() % 1000000));

        // Setup test sample
        testSample = createTestSample("CS" + (System.currentTimeMillis() % 100000000));
    }

    // ========== LIST SAMPLES TESTS ==========

    @Test
    public void testListSamples_ReturnsArray() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/biorepository/sample").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should be an array", responseJson.isArray());
        // Note: Cannot guarantee empty since other tests may have created samples
        // This test verifies the endpoint returns valid JSON array structure
    }

    @Test
    public void testListSamples_ReturnsData_WithValidJsonStructure() throws Exception {
        // Arrange - Create test data
        SampleItem sampleItem = createTestSampleItem(testSample, "CTRL-LIST-" + System.currentTimeMillis());
        BioSample bioSample = createTestBioSample(sampleItem, testShipment, BiosafetyLevel.BSL_2);

        // Act
        MvcResult result = mockMvc.perform(get("/rest/biorepository/sample").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        // Assert - Response is valid JSON array with samples
        String responseContent = result.getResponse().getContentAsString();
        assertNotNull("Response should not be null", responseContent);

        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should be an array", responseJson.isArray());
        assertTrue("Response should contain at least one sample", responseJson.size() >= 1);

        // Verify each sample has required fields (proves no serialization errors)
        for (JsonNode sample : responseJson) {
            assertTrue("Sample should have 'id' field", sample.has("id"));
            assertTrue("Sample should have 'biosafetyLevel' field", sample.has("biosafetyLevel"));
            assertTrue("Sample should have 'barcode' field", sample.has("barcode"));

            // Verify fields are not null/error strings
            assertFalse("Id should not be null", sample.get("id").isNull());
            assertFalse("BiosafetyLevel should not be null", sample.get("biosafetyLevel").isNull());
        }
    }

    @Test
    public void testListSamples_ReturnsEnrichedData_WithSampleTypeAndBarcode() throws Exception {
        // Arrange
        String uniqueBarcode = "CTRL-ENRICH-" + System.currentTimeMillis();
        SampleItem sampleItem = createTestSampleItem(testSample, uniqueBarcode);
        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_3);
        bioSample.setOriginLab("Test Origin Lab");
        bioSample.setProjectId("PROJ-123");
        bioSample.setPrincipalInvestigator("Dr. Test PI");
        bioSample.setShipment(testShipment);
        bioSample.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act
        MvcResult result = mockMvc.perform(get("/rest/biorepository/sample").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        // Find our test sample by looking for the unique barcode
        JsonNode targetSample = null;
        for (JsonNode sample : responseJson) {
            if (sample.has("barcode") && uniqueBarcode.equals(sample.get("barcode").asText())) {
                targetSample = sample;
                break;
            }
        }

        assertNotNull("Should find sample with unique barcode", targetSample);
        assertEquals("Barcode should match", uniqueBarcode, targetSample.get("barcode").asText());
        assertEquals("Biosafety level should be BSL_3", "BSL_3", targetSample.get("biosafetyLevel").asText());
        assertEquals("Origin lab should match", "Test Origin Lab", targetSample.get("originLab").asText());
        assertEquals("Project ID should match", "PROJ-123", targetSample.get("projectId").asText());
        assertEquals("PI should match", "Dr. Test PI", targetSample.get("principalInvestigator").asText());
        assertTrue("Sample should have sampleType object", targetSample.has("sampleType"));
        assertNotNull("SampleType should not be null", targetSample.get("sampleType"));
    }

    @Test
    public void testListSamples_FilterByShipment_ReturnsOnlyShipmentSamples() throws Exception {
        // Arrange - Create samples in different shipments
        Shipment shipment1 = createTestShipment("CTRL-SHIP1-" + System.currentTimeMillis());
        Shipment shipment2 = createTestShipment("CTRL-SHIP2-" + System.currentTimeMillis());

        SampleItem sampleItem1 = createTestSampleItem(testSample, "CTRL-S1-" + System.currentTimeMillis());
        SampleItem sampleItem2 = createTestSampleItem(testSample, "CTRL-S2-" + System.currentTimeMillis());

        createTestBioSample(sampleItem1, shipment1, BiosafetyLevel.BSL_1);
        createTestBioSample(sampleItem2, shipment2, BiosafetyLevel.BSL_2);

        // Act - Filter by shipment1
        MvcResult result = mockMvc.perform(get("/rest/biorepository/sample")
                .param("shipmentId", shipment1.getId().toString()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should be an array", responseJson.isArray());
        assertTrue("Response should contain at least one sample from shipment1", responseJson.size() >= 1);

        // Verify ALL samples belong to shipment1 (not shipment2)
        for (JsonNode sample : responseJson) {
            assertTrue("Each sample should have shipmentId field", sample.has("shipmentId"));
            assertFalse("ShipmentId should not be null", sample.get("shipmentId").isNull());
            assertEquals("All samples should belong to shipment1", shipment1.getId().intValue(),
                    sample.get("shipmentId").asInt());
        }
    }

    @Test
    public void testListSamples_WithLimit_RespectsLimit() throws Exception {
        // Arrange - Create multiple samples (more than the limit)
        for (int i = 0; i < 5; i++) {
            SampleItem sampleItem = createTestSampleItem(testSample,
                    "CTRL-LIMIT-" + i + "-" + System.currentTimeMillis());
            createTestBioSample(sampleItem, testShipment, BiosafetyLevel.BSL_1);
        }

        // Act - Request with limit=2
        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/sample").param("limit", "2").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should be an array", responseJson.isArray());

        // Verify exactly 2 samples returned (respecting the limit)
        assertEquals("Response should have exactly 2 samples when limit=2 and more exist", 2, responseJson.size());

        // Verify each returned sample has valid structure
        for (JsonNode sample : responseJson) {
            assertTrue("Sample should have 'id' field", sample.has("id"));
            assertTrue("Sample should have 'biosafetyLevel' field", sample.has("biosafetyLevel"));
        }
    }

    // ========== GET BY ID TESTS ==========

    @Test
    public void testGetBioSampleById_Found() throws Exception {
        // Arrange
        SampleItem sampleItem = createTestSampleItem(testSample, "CTRL-ID-" + System.currentTimeMillis());
        BioSample bioSample = createTestBioSample(sampleItem, testShipment, BiosafetyLevel.BSL_2);

        // Act
        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/sample/" + bioSample.getId()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertEquals("ID should match", bioSample.getId().intValue(), responseJson.get("id").asInt());
        assertEquals("Biosafety level should match", "BSL_2", responseJson.get("biosafetyLevel").asText());
    }

    @Test
    public void testGetBioSampleById_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/rest/biorepository/sample/999999").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ========== STATISTICS TESTS ==========

    @Test
    public void testGetBiosafetyStats_ReturnsAllLevels() throws Exception {
        // Arrange - Create samples with different biosafety levels
        SampleItem sampleItem = createTestSampleItem(testSample, "CTRL-STATS-" + System.currentTimeMillis());
        createTestBioSample(sampleItem, testShipment, BiosafetyLevel.BSL_2);

        // Act
        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/sample/stats/biosafety").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should have BSL_1 stat", responseJson.has("BSL_1"));
        assertTrue("Response should have BSL_2 stat", responseJson.has("BSL_2"));
        assertTrue("Response should have BSL_3 stat", responseJson.has("BSL_3"));
        assertTrue("Response should have BSL_4 stat", responseJson.has("BSL_4"));

        // Verify BSL_2 count is at least 1 since we just created one
        assertTrue("BSL_2 count should be at least 1 after creating sample", responseJson.get("BSL_2").asInt() >= 1);

        // Verify counts are non-negative integers
        assertTrue("BSL_1 count should be non-negative", responseJson.get("BSL_1").asInt() >= 0);
        assertTrue("BSL_3 count should be non-negative", responseJson.get("BSL_3").asInt() >= 0);
        assertTrue("BSL_4 count should be non-negative", responseJson.get("BSL_4").asInt() >= 0);
    }

    // ========== GENERATE BARCODE TESTS ==========

    @Test
    public void testGenerateBarcode_ReturnsValidBarcode() throws Exception {
        // Act
        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/sample/generate-barcode").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should have barcode field", responseJson.has("barcode"));
        String barcode = responseJson.get("barcode").asText();
        assertTrue("Barcode should start with BIO-", barcode.startsWith("BIO-"));
        assertTrue("Barcode should have date portion", barcode.length() > 12);
    }

    // ========== VALIDATE MANIFEST TESTS ==========

    @Test
    public void testValidateManifestImport_ValidData_ReturnsValid() throws Exception {
        // Arrange
        String requestBody = "{\"samples\":[{" + "\"externalId\":\"TEST-VALIDATE-" + System.currentTimeMillis() + "\","
                + "\"sampleType\":\"" + testSampleType.getId() + "\"," + "\"biosafetyLevel\":\"BSL_2\","
                + "\"originLab\":\"Test Lab\"" + "}]}";

        // Act
        MvcResult result = mockMvc
                .perform(post("/rest/biorepository/sample/validate-manifest-import")
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should have 'valid' field", responseJson.has("valid"));
        assertTrue("Validation should pass for valid data", responseJson.get("valid").asBoolean());

        // Verify row validation details
        assertTrue("Response should have 'rows' field", responseJson.has("rows"));
        JsonNode rows = responseJson.get("rows");
        assertEquals("Should have exactly 1 validated row", 1, rows.size());
        assertTrue("Row should have empty errors for valid data", rows.get(0).get("errors").isEmpty());
    }

    @Test
    public void testValidateManifestImport_DuplicateBarcode_ReturnsError() throws Exception {
        // Arrange - Create existing sample
        String existingBarcode = "CTRL-DUP-" + System.currentTimeMillis();
        SampleItem sampleItem = createTestSampleItem(testSample, existingBarcode);
        createTestBioSample(sampleItem, testShipment, BiosafetyLevel.BSL_1);

        String requestBody = "{\"samples\":[{" + "\"externalId\":\"" + existingBarcode + "\"," + "\"sampleType\":\""
                + testSampleType.getId() + "\"," + "\"biosafetyLevel\":\"BSL_2\"," + "\"originLab\":\"Test Lab\""
                + "}]}";

        // Act
        MvcResult result = mockMvc
                .perform(post("/rest/biorepository/sample/validate-manifest-import")
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should have 'rows' field", responseJson.has("rows"));

        JsonNode rows = responseJson.get("rows");
        assertTrue("Rows should be an array", rows.isArray());
        assertTrue("Should have at least one row", rows.size() > 0);

        JsonNode firstRow = rows.get(0);
        assertTrue("Row should have 'errors' field", firstRow.has("errors"));
        JsonNode errors = firstRow.get("errors");
        assertTrue("Should have errors for duplicate barcode", errors.size() > 0);
    }

    @Test
    public void testValidateManifestImport_MissingRequiredFields_ReturnsErrors() throws Exception {
        // Arrange - Missing externalId, sampleType, and biosafetyLevel
        String requestBody = "{\"samples\":[{\"originLab\":\"Test Lab\"}]}";

        // Act
        MvcResult result = mockMvc
                .perform(post("/rest/biorepository/sample/validate-manifest-import")
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should have 'rows' field", responseJson.has("rows"));

        JsonNode rows = responseJson.get("rows");
        assertTrue("Should have at least one row", rows.size() > 0);

        JsonNode firstRow = rows.get(0);
        JsonNode errors = firstRow.get("errors");
        assertTrue("Should have multiple errors for missing fields", errors.size() >= 3);
    }

    // ========== SEARCH ENDPOINT TESTS ==========

    @Test
    public void testGetByBiosafetyLevel_ReturnsMatchingSamples() throws Exception {
        // Arrange
        SampleItem sampleItem = createTestSampleItem(testSample, "CTRL-BSL-" + System.currentTimeMillis());
        createTestBioSample(sampleItem, testShipment, BiosafetyLevel.BSL_3);

        // Act
        MvcResult result = mockMvc.perform(get("/rest/biorepository/sample/by-biosafety-level")
                .param("biosafetyLevel", "BSL_3").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should be an array", responseJson.isArray());
        assertTrue("Should return at least one BSL_3 sample", responseJson.size() >= 1);

        // Verify ALL returned samples have BSL_3
        for (JsonNode sample : responseJson) {
            assertTrue("Sample should have biosafetyLevel field", sample.has("biosafetyLevel"));
            assertEquals("All returned samples should be BSL_3", "BSL_3", sample.get("biosafetyLevel").asText());
        }
    }

    @Test
    public void testGetByEthicsRef_ReturnsMatchingSamples() throws Exception {
        // Arrange
        String uniqueEthicsRef = "ETH-CTRL-" + System.currentTimeMillis();
        SampleItem sampleItem = createTestSampleItem(testSample, "CTRL-ETH-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample.setEthicsApprovalRef(uniqueEthicsRef);
        bioSample.setShipment(testShipment);
        bioSample.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act
        MvcResult result = mockMvc.perform(get("/rest/biorepository/sample/by-ethics-ref")
                .param("ethicsApprovalRef", uniqueEthicsRef).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should be an array", responseJson.isArray());
        assertEquals("Should find exactly 1 sample", 1, responseJson.size());
    }

    @Test
    public void testGetByPrincipalInvestigator_ReturnsMatchingSamples() throws Exception {
        // Arrange
        String uniquePI = "Dr. Unique Controller PI " + System.currentTimeMillis();
        SampleItem sampleItem = createTestSampleItem(testSample, "CTRL-PI-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample.setPrincipalInvestigator(uniquePI);
        bioSample.setShipment(testShipment);
        bioSample.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act
        MvcResult result = mockMvc.perform(get("/rest/biorepository/sample/by-pi")
                .param("principalInvestigator", uniquePI).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should be an array", responseJson.isArray());
        assertEquals("Should find exactly 1 sample", 1, responseJson.size());
    }

    // ========== SAMPLE SEARCH ENDPOINT TESTS ==========

    @Test
    public void testSearchSamples_ByBarcode_ReturnsMatchingSample() throws Exception {
        // Arrange - Create sample with unique barcode
        String uniqueBarcode = "SEARCH-BC-" + System.currentTimeMillis();
        SampleItem sampleItem = createTestSampleItem(testSample, uniqueBarcode);
        BioSample bioSample = createTestBioSample(sampleItem, testShipment, BiosafetyLevel.BSL_2);

        // Set to STORED status for retrieval search
        bioSample.setWorkflowStatus(BioSample.WorkflowStatus.STORED);
        bioSample.setSysUserId(testUser.getId().toString());
        bioSampleService.update(bioSample);

        // Act
        MvcResult result = mockMvc.perform(get("/rest/biorepository/sample/search").param("barcode", uniqueBarcode)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Response should be an array", responseJson.isArray());
        assertEquals("Should find exactly 1 sample by barcode", 1, responseJson.size());

        JsonNode foundSample = responseJson.get(0);
        assertEquals("Barcode should match", uniqueBarcode, foundSample.get("barcode").asText());
        assertEquals("BioSample ID should match", bioSample.getId().intValue(), foundSample.get("id").asInt());
        assertTrue("Should have sampleItemId", foundSample.has("sampleItemId"));
        assertEquals("SampleItemId should match", Integer.parseInt(sampleItem.getId()),
                foundSample.get("sampleItemId").asInt());
    }

    @Test
    public void testSearchSamples_ByBarcode_WithStatusFilter_ReturnsOnlyStoredSamples() throws Exception {
        // Arrange - Create two samples with same prefix but different statuses
        String baseBarcode = "SEARCH-STATUS-" + System.currentTimeMillis();
        String storedBarcode = baseBarcode + "-STORED";
        String registeredBarcode = baseBarcode + "-REG";

        // Create STORED sample
        SampleItem storedSampleItem = createTestSampleItem(testSample, storedBarcode);
        BioSample storedBioSample = createTestBioSample(storedSampleItem, testShipment, BiosafetyLevel.BSL_2);
        storedBioSample.setWorkflowStatus(BioSample.WorkflowStatus.STORED);
        storedBioSample.setSysUserId(testUser.getId().toString());
        bioSampleService.update(storedBioSample);

        // Create REGISTERED sample (default status)
        SampleItem registeredSampleItem = createTestSampleItem(testSample, registeredBarcode);
        createTestBioSample(registeredSampleItem, testShipment, BiosafetyLevel.BSL_2);

        // Act - Search for STORED sample only
        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/sample/search").param("barcode", storedBarcode)
                        .param("status", "STORED").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertEquals("Should find exactly 1 STORED sample", 1, responseJson.size());
        assertEquals("Found sample should have STORED status", "STORED",
                responseJson.get(0).get("workflowStatus").asText());

        // Act - Search for REGISTERED sample with STORED filter (should return empty)
        MvcResult filteredResult = mockMvc
                .perform(get("/rest/biorepository/sample/search").param("barcode", registeredBarcode)
                        .param("status", "STORED").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        JsonNode filteredJson = objectMapper.readTree(filteredResult.getResponse().getContentAsString());
        assertEquals("Should not find REGISTERED sample when filtering for STORED", 0, filteredJson.size());
    }

    @Test
    public void testSearchSamples_ByOriginLab_ReturnsMatchingSamples() throws Exception {
        // Arrange - Create samples with unique origin lab
        String uniqueOriginLab = "Unique Search Lab " + System.currentTimeMillis();
        String uniqueBarcode = "SEARCH-ORIGIN-" + System.currentTimeMillis();

        SampleItem sampleItem = createTestSampleItem(testSample, uniqueBarcode);
        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        bioSample.setOriginLab(uniqueOriginLab);
        bioSample.setShipment(testShipment);
        bioSample.setWorkflowStatus(BioSample.WorkflowStatus.STORED);
        bioSample.setSysUserId(testUser.getId().toString());
        BioSample saved = bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act
        MvcResult result = mockMvc.perform(get("/rest/biorepository/sample/search").param("originLab", uniqueOriginLab)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Response should be an array", responseJson.isArray());
        assertEquals("Should find exactly 1 sample by origin lab", 1, responseJson.size());

        JsonNode foundSample = responseJson.get(0);
        assertEquals("BioSample ID should match", saved.getId().intValue(), foundSample.get("id").asInt());
        assertEquals("Barcode should match", uniqueBarcode, foundSample.get("barcode").asText());
        assertEquals("OriginLab should match", uniqueOriginLab, foundSample.get("originLab").asText());
    }

    @Test
    public void testSearchSamples_ByOriginLab_WithStatusFilter_ReturnsOnlyMatchingStatus() throws Exception {
        // Arrange - Create sample with origin lab and STORED status
        String uniqueOriginLab = "Status Filter Lab " + System.currentTimeMillis();

        SampleItem sampleItem = createTestSampleItem(testSample, "ORIGIN-STAT-" + System.currentTimeMillis());
        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        bioSample.setOriginLab(uniqueOriginLab);
        bioSample.setShipment(testShipment);
        bioSample.setWorkflowStatus(BioSample.WorkflowStatus.STORED);
        bioSample.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act - Search with STORED filter (should find sample)
        MvcResult storedResult = mockMvc
                .perform(get("/rest/biorepository/sample/search").param("originLab", uniqueOriginLab)
                        .param("status", "STORED").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        JsonNode storedJson = objectMapper.readTree(storedResult.getResponse().getContentAsString());
        assertEquals("Should find 1 STORED sample by origin lab", 1, storedJson.size());

        // Act - Search with DISPOSED filter (should not find sample)
        MvcResult disposedResult = mockMvc
                .perform(get("/rest/biorepository/sample/search").param("originLab", uniqueOriginLab)
                        .param("status", "DISPOSED").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        JsonNode disposedJson = objectMapper.readTree(disposedResult.getResponse().getContentAsString());
        assertEquals("Should not find sample when filtering for DISPOSED", 0, disposedJson.size());
    }

    @Test
    public void testSearchSamples_ByProjectId_ReturnsMatchingSamples() throws Exception {
        // Arrange - Create sample with unique project ID
        String uniqueProjectId = "PROJ-SEARCH-" + System.currentTimeMillis();
        String uniqueBarcode = "SEARCH-PROJ-" + System.currentTimeMillis();

        SampleItem sampleItem = createTestSampleItem(testSample, uniqueBarcode);
        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        bioSample.setProjectId(uniqueProjectId);
        bioSample.setShipment(testShipment);
        bioSample.setWorkflowStatus(BioSample.WorkflowStatus.STORED);
        bioSample.setSysUserId(testUser.getId().toString());
        BioSample saved = bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act
        MvcResult result = mockMvc.perform(get("/rest/biorepository/sample/search").param("projectId", uniqueProjectId)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Response should be an array", responseJson.isArray());
        assertEquals("Should find exactly 1 sample by project ID", 1, responseJson.size());

        JsonNode foundSample = responseJson.get(0);
        assertEquals("BioSample ID should match", saved.getId().intValue(), foundSample.get("id").asInt());
        assertEquals("Barcode should match", uniqueBarcode, foundSample.get("barcode").asText());
        assertEquals("ProjectId should match", uniqueProjectId, foundSample.get("projectId").asText());
    }

    @Test
    public void testSearchSamples_ByProjectId_WithStatusFilter_ReturnsOnlyMatchingStatus() throws Exception {
        // Arrange - Create sample with project ID and specific status
        String uniqueProjectId = "PROJ-STATUS-" + System.currentTimeMillis();

        SampleItem sampleItem = createTestSampleItem(testSample, "PROJ-STAT-" + System.currentTimeMillis());
        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        bioSample.setProjectId(uniqueProjectId);
        bioSample.setShipment(testShipment);
        bioSample.setWorkflowStatus(BioSample.WorkflowStatus.STORED);
        bioSample.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act - Search with STORED filter
        MvcResult storedResult = mockMvc
                .perform(get("/rest/biorepository/sample/search").param("projectId", uniqueProjectId)
                        .param("status", "STORED").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        JsonNode storedJson = objectMapper.readTree(storedResult.getResponse().getContentAsString());
        assertEquals("Should find 1 STORED sample by project ID", 1, storedJson.size());

        // Act - Search with REGISTERED filter (should not find sample)
        MvcResult registeredResult = mockMvc
                .perform(get("/rest/biorepository/sample/search").param("projectId", uniqueProjectId)
                        .param("status", "REGISTERED").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        JsonNode registeredJson = objectMapper.readTree(registeredResult.getResponse().getContentAsString());
        assertEquals("Should not find sample when filtering for REGISTERED", 0, registeredJson.size());
    }

    @Test
    public void testSearchSamples_NoParams_ReturnsEmptyArray() throws Exception {
        // Act - Search without any parameters
        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/sample/search").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Response should be an array", responseJson.isArray());
        assertEquals("Should return empty array when no search params provided", 0, responseJson.size());
    }

    @Test
    public void testSearchSamples_NonExistentBarcode_ReturnsEmptyArray() throws Exception {
        // Act
        MvcResult result = mockMvc.perform(get("/rest/biorepository/sample/search")
                .param("barcode", "NON-EXISTENT-BARCODE-12345").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Response should be an array", responseJson.isArray());
        assertEquals("Should return empty array for non-existent barcode", 0, responseJson.size());
    }

    @Test
    public void testSearchSamples_ReturnsCorrectDTOStructure() throws Exception {
        // Arrange - Create sample with all fields populated
        String uniqueBarcode = "SEARCH-DTO-" + System.currentTimeMillis();

        SampleItem sampleItem = createTestSampleItem(testSample, uniqueBarcode);
        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        bioSample.setOriginLab("DTO Test Lab");
        bioSample.setProjectId("DTO-PROJ-001");
        bioSample.setShipment(testShipment);
        bioSample.setWorkflowStatus(BioSample.WorkflowStatus.STORED);
        bioSample.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act
        MvcResult result = mockMvc.perform(get("/rest/biorepository/sample/search").param("barcode", uniqueBarcode)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        // Assert - Verify DTO has all expected fields
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertEquals("Should find 1 sample", 1, responseJson.size());
        JsonNode foundSample = responseJson.get(0);

        // Verify essential fields for frontend
        assertTrue("Should have 'id' field (BioSample ID)", foundSample.has("id"));
        assertTrue("Should have 'sampleItemId' field", foundSample.has("sampleItemId"));
        assertTrue("Should have 'barcode' field", foundSample.has("barcode"));
        assertTrue("Should have 'sampleType' field", foundSample.has("sampleType"));
        assertTrue("Should have 'workflowStatus' field", foundSample.has("workflowStatus"));

        // Verify sampleType is nested object with description
        JsonNode sampleType = foundSample.get("sampleType");
        assertFalse("SampleType should not be null", sampleType.isNull());
        assertTrue("SampleType should have 'description' field", sampleType.has("description"));

        // Verify values
        assertEquals("Barcode should match", uniqueBarcode, foundSample.get("barcode").asText());
        assertEquals("WorkflowStatus should be STORED", "STORED", foundSample.get("workflowStatus").asText());
        assertEquals("BiosafetyLevel should be BSL_2", "BSL_2", foundSample.get("biosafetyLevel").asText());
    }

    @Test
    public void testSearchSamples_MultipleMatchesByOriginLab_ReturnsAllMatches() throws Exception {
        // Arrange - Create multiple samples with same origin lab
        String sharedOriginLab = "Shared Origin Lab " + System.currentTimeMillis();

        SampleItem sampleItem1 = createTestSampleItem(testSample, "MULTI-1-" + System.currentTimeMillis());
        BioSample bioSample1 = new BioSample();
        bioSample1.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        bioSample1.setOriginLab(sharedOriginLab);
        bioSample1.setShipment(testShipment);
        bioSample1.setWorkflowStatus(BioSample.WorkflowStatus.STORED);
        bioSample1.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem1, bioSample1);

        SampleItem sampleItem2 = createTestSampleItem(testSample, "MULTI-2-" + System.currentTimeMillis());
        BioSample bioSample2 = new BioSample();
        bioSample2.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        bioSample2.setOriginLab(sharedOriginLab);
        bioSample2.setShipment(testShipment);
        bioSample2.setWorkflowStatus(BioSample.WorkflowStatus.STORED);
        bioSample2.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem2, bioSample2);

        // Act
        MvcResult result = mockMvc.perform(get("/rest/biorepository/sample/search").param("originLab", sharedOriginLab)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Should find at least 2 samples", responseJson.size() >= 2);

        // Verify all returned samples have the correct origin lab
        for (JsonNode sample : responseJson) {
            assertEquals("All samples should have the shared origin lab", sharedOriginLab,
                    sample.get("originLab").asText());
        }
    }

    @Test
    public void testSearchSamples_MultipleMatchesByProjectId_ReturnsAllMatches() throws Exception {
        // Arrange - Create multiple samples with same project ID
        String sharedProjectId = "SHARED-PROJ-" + System.currentTimeMillis();

        SampleItem sampleItem1 = createTestSampleItem(testSample, "PROJ-MULTI-1-" + System.currentTimeMillis());
        BioSample bioSample1 = new BioSample();
        bioSample1.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        bioSample1.setProjectId(sharedProjectId);
        bioSample1.setShipment(testShipment);
        bioSample1.setWorkflowStatus(BioSample.WorkflowStatus.STORED);
        bioSample1.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem1, bioSample1);

        SampleItem sampleItem2 = createTestSampleItem(testSample, "PROJ-MULTI-2-" + System.currentTimeMillis());
        BioSample bioSample2 = new BioSample();
        bioSample2.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        bioSample2.setProjectId(sharedProjectId);
        bioSample2.setShipment(testShipment);
        bioSample2.setWorkflowStatus(BioSample.WorkflowStatus.STORED);
        bioSample2.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem2, bioSample2);

        // Act
        MvcResult result = mockMvc.perform(get("/rest/biorepository/sample/search").param("projectId", sharedProjectId)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Should find at least 2 samples", responseJson.size() >= 2);

        // Verify all returned samples have the correct project ID
        for (JsonNode sample : responseJson) {
            assertEquals("All samples should have the shared project ID", sharedProjectId,
                    sample.get("projectId").asText());
        }
    }

    @Test
    public void testSearchSamples_LimitParameter_RespectsLimit() throws Exception {
        // Arrange - Create multiple samples with same origin lab
        String sharedOriginLab = "Limit Test Lab " + System.currentTimeMillis();

        for (int i = 0; i < 5; i++) {
            SampleItem sampleItem = createTestSampleItem(testSample, "LIMIT-" + i + "-" + System.currentTimeMillis());
            BioSample bioSample = new BioSample();
            bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_2);
            bioSample.setOriginLab(sharedOriginLab);
            bioSample.setShipment(testShipment);
            bioSample.setWorkflowStatus(BioSample.WorkflowStatus.STORED);
            bioSample.setSysUserId(testUser.getId().toString());
            bioSampleService.createForSampleItem(sampleItem, bioSample);
        }

        // Act - Search with limit=2
        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/sample/search").param("originLab", sharedOriginLab)
                        .param("limit", "2").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertEquals("Should respect limit and return exactly 2 samples", 2, responseJson.size());
    }

    // ========== COUNT ENDPOINTS TESTS ==========

    @Test
    public void testCountByShipment_ReturnsCorrectCount() throws Exception {
        // Arrange
        Shipment newShipment = createTestShipment("CTRL-COUNT-" + System.currentTimeMillis());
        SampleItem sampleItem1 = createTestSampleItem(testSample, "CTRL-CNT1-" + System.currentTimeMillis());
        SampleItem sampleItem2 = createTestSampleItem(testSample, "CTRL-CNT2-" + System.currentTimeMillis());
        createTestBioSample(sampleItem1, newShipment, BiosafetyLevel.BSL_1);
        createTestBioSample(sampleItem2, newShipment, BiosafetyLevel.BSL_2);

        // Act
        MvcResult result = mockMvc.perform(get("/rest/biorepository/sample/count/by-shipment/" + newShipment.getId())
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should have 'count' field", responseJson.has("count"));
        assertEquals("Count should be 2", 2, responseJson.get("count").asInt());
    }

    @Test
    public void testExistsBySampleItemId_ReturnsCorrectResult() throws Exception {
        // Arrange
        SampleItem sampleItem = createTestSampleItem(testSample, "CTRL-EXISTS-" + System.currentTimeMillis());
        Integer sampleItemId = Integer.valueOf(sampleItem.getId());

        // Check before creating BioSample
        MvcResult resultBefore = mockMvc.perform(get("/rest/biorepository/sample/exists/by-sample-item/" + sampleItemId)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        JsonNode responseBefore = objectMapper.readTree(resultBefore.getResponse().getContentAsString());
        assertFalse("Should not exist before creation", responseBefore.get("exists").asBoolean());

        // Create BioSample
        createTestBioSample(sampleItem, testShipment, BiosafetyLevel.BSL_1);

        // Check after creating BioSample
        MvcResult resultAfter = mockMvc.perform(get("/rest/biorepository/sample/exists/by-sample-item/" + sampleItemId)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        JsonNode responseAfter = objectMapper.readTree(resultAfter.getResponse().getContentAsString());
        assertTrue("Should exist after creation", responseAfter.get("exists").asBoolean());
    }

    // ========== ORIGIN LAB AND PROJECT ID TESTS ==========

    @Test
    public void testOriginLabAndProjectId_SavedAndReturnedInListEndpoint() throws Exception {
        // Arrange - Create sample with originLab and projectId via service
        String uniqueBarcode = "ORIGIN-LAB-" + System.currentTimeMillis();
        String expectedOriginLab = "Test Origin Laboratory";
        String expectedProjectId = "PROJECT-2026-001";

        SampleItem sampleItem = createTestSampleItem(testSample, uniqueBarcode);

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        bioSample.setOriginLab(expectedOriginLab);
        bioSample.setProjectId(expectedProjectId);
        bioSample.setShipment(testShipment);
        bioSample.setSysUserId(testUser.getId().toString());
        BioSample saved = bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Verify saved correctly in database
        BioSample retrieved = bioSampleService.get(saved.getId());
        assertEquals("Origin lab should be persisted", expectedOriginLab, retrieved.getOriginLab());
        assertEquals("Project ID should be persisted", expectedProjectId, retrieved.getProjectId());

        // Act - Fetch via list endpoint to verify originLab is returned in DTO
        MvcResult listResult = mockMvc.perform(get("/rest/biorepository/sample")
                .param("shipmentId", testShipment.getId().toString()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert - Find our sample and verify originLab and projectId in response
        String listResponse = listResult.getResponse().getContentAsString();
        JsonNode listJson = objectMapper.readTree(listResponse);
        assertTrue("Response should be an array", listJson.isArray());

        JsonNode targetSample = null;
        for (JsonNode sample : listJson) {
            if (sample.has("barcode") && uniqueBarcode.equals(sample.get("barcode").asText())) {
                targetSample = sample;
                break;
            }
        }

        assertNotNull("Should find the sample by barcode in list response", targetSample);
        assertEquals("Origin lab should be returned in list DTO", expectedOriginLab,
                targetSample.get("originLab").asText());
        assertEquals("Project ID should be returned in list DTO", expectedProjectId,
                targetSample.get("projectId").asText());
    }

    // ========== ACCESSION NUMBER TESTS ==========

    @Test
    public void testAccessionNumber_ReturnedInListEndpoint() throws Exception {
        // Arrange - Create sample with known accession number
        String expectedAccessionNumber = "ACC" + System.currentTimeMillis();
        String uniqueBarcode = "BIO-ACC-" + System.currentTimeMillis();

        Sample sample = createTestSample(expectedAccessionNumber);
        SampleItem sampleItem = createTestSampleItem(sample, uniqueBarcode);
        createTestBioSample(sampleItem, testShipment, BiosafetyLevel.BSL_2);

        // Act - Fetch via list endpoint
        MvcResult result = mockMvc.perform(get("/rest/biorepository/sample").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert - Find our sample and verify accessionNumber in response
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should be an array", responseJson.isArray());

        JsonNode targetSample = null;
        for (JsonNode sampleNode : responseJson) {
            if (sampleNode.has("barcode") && uniqueBarcode.equals(sampleNode.get("barcode").asText())) {
                targetSample = sampleNode;
                break;
            }
        }

        assertNotNull("Should find the sample by barcode in list response", targetSample);
        assertTrue("Response should have 'accessionNumber' field", targetSample.has("accessionNumber"));
        assertEquals("Accession number should match the parent Sample's accession number", expectedAccessionNumber,
                targetSample.get("accessionNumber").asText());
    }

    // ========== WORKFLOW STATUS TESTS ==========

    @Test
    public void testListSamples_ReturnsWorkflowStatusField() throws Exception {
        // Arrange - Create test sample
        SampleItem sampleItem = createTestSampleItem(testSample, "CTRL-WF-" + System.currentTimeMillis());
        BioSample bioSample = createTestBioSample(sampleItem, testShipment, BiosafetyLevel.BSL_2);

        // Act
        MvcResult result = mockMvc.perform(get("/rest/biorepository/sample").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert - Response contains workflowStatus field
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        // Find our test sample
        JsonNode targetSample = null;
        for (JsonNode sample : responseJson) {
            if (sample.has("id") && bioSample.getId().equals(sample.get("id").asInt())) {
                targetSample = sample;
                break;
            }
        }

        assertNotNull("Should find test sample in response", targetSample);
        assertTrue("Sample should have workflowStatus field", targetSample.has("workflowStatus"));
        assertEquals("Default workflow status should be REGISTERED", "REGISTERED",
                targetSample.get("workflowStatus").asText());
    }

    @Test
    public void testListSamples_FilterByWorkflowStatus_ReturnsOnlyMatchingStatus() throws Exception {
        // Arrange - Create samples with different workflow statuses
        SampleItem sampleItem1 = createTestSampleItem(testSample, "CTRL-WF-REG-" + System.currentTimeMillis());
        BioSample bioSample1 = createTestBioSample(sampleItem1, testShipment, BiosafetyLevel.BSL_2);
        // bioSample1 has default REGISTERED status

        SampleItem sampleItem2 = createTestSampleItem(testSample, "CTRL-WF-PEND-" + System.currentTimeMillis());
        BioSample bioSample2 = createTestBioSample(sampleItem2, testShipment, BiosafetyLevel.BSL_2);
        // Update bioSample2 to PENDING_STORAGE
        bioSample2.setWorkflowStatus(BioSample.WorkflowStatus.PENDING_STORAGE);
        bioSample2.setSysUserId(testUser.getId().toString());
        bioSampleService.update(bioSample2);

        // Act - Filter by REGISTERED status
        MvcResult result = mockMvc.perform(get("/rest/biorepository/sample").param("workflowStatus", "REGISTERED")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        // Assert - Only REGISTERED samples returned
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        for (JsonNode sample : responseJson) {
            assertTrue("Each sample should have workflowStatus field", sample.has("workflowStatus"));
            assertEquals("All samples should have REGISTERED status", "REGISTERED",
                    sample.get("workflowStatus").asText());
        }

        // Verify bioSample2 (PENDING_STORAGE) is NOT in the response
        boolean foundPendingStorage = false;
        for (JsonNode sample : responseJson) {
            if (sample.has("id") && bioSample2.getId().equals(sample.get("id").asInt())) {
                foundPendingStorage = true;
                break;
            }
        }
        assertFalse("PENDING_STORAGE sample should not be in REGISTERED filter results", foundPendingStorage);
    }

    @Test
    public void testUpdateWorkflowStatus_Success() throws Exception {
        // Arrange - Create test sample
        SampleItem sampleItem = createTestSampleItem(testSample, "CTRL-WF-UPD-" + System.currentTimeMillis());
        BioSample bioSample = createTestBioSample(sampleItem, testShipment, BiosafetyLevel.BSL_2);

        String requestBody = objectMapper.writeValueAsString(java.util.Map.of("sampleItemIds",
                java.util.List.of(Integer.parseInt(sampleItem.getId())), "workflowStatus", "PENDING_STORAGE"));

        // Act
        MvcResult result = mockMvc
                .perform(put("/rest/biorepository/sample/workflow-status").contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("userSessionData", userSessionData).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        // Assert - Response indicates success
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Response should have success field", responseJson.has("success"));
        assertTrue("Update should succeed", responseJson.get("success").asBoolean());
        assertEquals("Updated count should be 1", 1, responseJson.get("updatedCount").asInt());
        assertEquals("New status should be PENDING_STORAGE", "PENDING_STORAGE", responseJson.get("newStatus").asText());

        // Verify the database was updated
        BioSample updatedBioSample = bioSampleService.get(bioSample.getId());
        assertEquals("Database should have PENDING_STORAGE status", BioSample.WorkflowStatus.PENDING_STORAGE,
                updatedBioSample.getWorkflowStatus());
    }

    @Test
    public void testUpdateWorkflowStatus_InvalidStatus_ReturnsBadRequest() throws Exception {
        // Arrange
        SampleItem sampleItem = createTestSampleItem(testSample, "CTRL-WF-INV-" + System.currentTimeMillis());
        createTestBioSample(sampleItem, testShipment, BiosafetyLevel.BSL_2);

        String requestBody = objectMapper.writeValueAsString(java.util.Map.of("sampleItemIds",
                java.util.List.of(Integer.parseInt(sampleItem.getId())), "workflowStatus", "INVALID_STATUS"));

        // Act & Assert
        mockMvc.perform(put("/rest/biorepository/sample/workflow-status").contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("userSessionData", userSessionData).content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateWorkflowStatus_NoSampleIds_ReturnsBadRequest() throws Exception {
        // Arrange
        String requestBody = objectMapper
                .writeValueAsString(java.util.Map.of("sampleItemIds", java.util.List.of(), "workflowStatus", "STORED"));

        // Act & Assert
        mockMvc.perform(put("/rest/biorepository/sample/workflow-status").contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("userSessionData", userSessionData).content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateWorkflowStatus_BulkUpdate_UpdatesMultipleSamples() throws Exception {
        // Arrange - Create multiple test samples
        SampleItem sampleItem1 = createTestSampleItem(testSample, "CTRL-WF-BLK1-" + System.currentTimeMillis());
        SampleItem sampleItem2 = createTestSampleItem(testSample, "CTRL-WF-BLK2-" + System.currentTimeMillis());
        BioSample bioSample1 = createTestBioSample(sampleItem1, testShipment, BiosafetyLevel.BSL_2);
        BioSample bioSample2 = createTestBioSample(sampleItem2, testShipment, BiosafetyLevel.BSL_2);

        String requestBody = objectMapper.writeValueAsString(java.util.Map.of("sampleItemIds",
                java.util.List.of(Integer.parseInt(sampleItem1.getId()), Integer.parseInt(sampleItem2.getId())),
                "workflowStatus", "STORED"));

        // Act
        MvcResult result = mockMvc
                .perform(put("/rest/biorepository/sample/workflow-status").contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("userSessionData", userSessionData).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Update should succeed", responseJson.get("success").asBoolean());
        assertEquals("Updated count should be 2", 2, responseJson.get("updatedCount").asInt());

        // Verify both samples were updated in database
        BioSample updatedBioSample1 = bioSampleService.get(bioSample1.getId());
        BioSample updatedBioSample2 = bioSampleService.get(bioSample2.getId());
        assertEquals("Sample 1 should have STORED status", BioSample.WorkflowStatus.STORED,
                updatedBioSample1.getWorkflowStatus());
        assertEquals("Sample 2 should have STORED status", BioSample.WorkflowStatus.STORED,
                updatedBioSample2.getWorkflowStatus());
    }

    // ========== GET BY BARCODE TESTS (Manual Disposal Lookup) ==========

    @Test
    public void testGetByBarcode_Found_ReturnsSampleDetails() throws Exception {
        // Arrange - Create sample with known barcode
        String uniqueBarcode = "BARCODE-LOOKUP-" + System.currentTimeMillis();
        SampleItem sampleItem = createTestSampleItem(testSample, uniqueBarcode);
        BioSample bioSample = createTestBioSample(sampleItem, testShipment, BiosafetyLevel.BSL_2);

        // Update to STORED status (typical state for manual disposal)
        bioSample.setWorkflowStatus(BioSample.WorkflowStatus.STORED);
        bioSample.setOriginLab("Origin Lab for Barcode Test");
        bioSample.setProjectId("PROJ-BC-001");
        bioSample.setSysUserId(testUser.getId().toString());
        bioSampleService.update(bioSample);

        // Act
        MvcResult result = mockMvc.perform(
                get("/rest/biorepository/sample/by-barcode/" + uniqueBarcode).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        // Verify essential fields are present and correct
        assertTrue("Response should have 'id' field", responseJson.has("id"));
        assertEquals("BioSample ID should match", bioSample.getId().intValue(), responseJson.get("id").asInt());

        assertTrue("Response should have 'barcode' field", responseJson.has("barcode"));
        assertEquals("Barcode should match", uniqueBarcode, responseJson.get("barcode").asText());

        assertTrue("Response should have 'sampleItemId' field", responseJson.has("sampleItemId"));
        assertEquals("SampleItemId should match", Integer.parseInt(sampleItem.getId()),
                responseJson.get("sampleItemId").asInt());

        assertTrue("Response should have 'workflowStatus' field", responseJson.has("workflowStatus"));
        assertEquals("WorkflowStatus should be STORED", "STORED", responseJson.get("workflowStatus").asText());

        assertTrue("Response should have 'biosafetyLevel' field", responseJson.has("biosafetyLevel"));
        assertEquals("BiosafetyLevel should be BSL_2", "BSL_2", responseJson.get("biosafetyLevel").asText());

        assertTrue("Response should have 'originLab' field", responseJson.has("originLab"));
        assertEquals("OriginLab should match", "Origin Lab for Barcode Test", responseJson.get("originLab").asText());

        assertTrue("Response should have 'projectId' field", responseJson.has("projectId"));
        assertEquals("ProjectId should match", "PROJ-BC-001", responseJson.get("projectId").asText());

        // Verify sampleType is present as nested object
        assertTrue("Response should have 'sampleType' field", responseJson.has("sampleType"));
        assertFalse("SampleType should not be null", responseJson.get("sampleType").isNull());
        assertTrue("SampleType should have 'id' field", responseJson.get("sampleType").has("id"));
        assertTrue("SampleType should have 'description' field", responseJson.get("sampleType").has("description"));
    }

    @Test
    public void testGetByBarcode_NotFound_Returns404() throws Exception {
        // Arrange - Use non-existent barcode
        String nonExistentBarcode = "NON-EXISTENT-BARCODE-" + System.currentTimeMillis();

        // Act
        MvcResult result = mockMvc.perform(get("/rest/biorepository/sample/by-barcode/" + nonExistentBarcode)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound()).andReturn();

        // Assert - Response should contain error message
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Response should have 'error' field", responseJson.has("error"));
        assertTrue("Error message should mention barcode",
                responseJson.get("error").asText().contains(nonExistentBarcode));
    }

    @Test
    public void testGetByBarcode_SampleWithoutBioSampleExtension_ReturnsRegisteredStatus() throws Exception {
        // Arrange - Create SampleItem without BioSample extension
        String uniqueBarcode = "NO-EXT-" + System.currentTimeMillis();
        SampleItem sampleItem = createTestSampleItem(testSample, uniqueBarcode);
        // Note: NOT creating BioSample extension

        // Act
        MvcResult result = mockMvc.perform(
                get("/rest/biorepository/sample/by-barcode/" + uniqueBarcode).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert - Should return with REGISTERED status even without BioSample
        // extension
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Response should have 'barcode' field", responseJson.has("barcode"));
        assertEquals("Barcode should match", uniqueBarcode, responseJson.get("barcode").asText());

        assertTrue("Response should have 'sampleItemId' field", responseJson.has("sampleItemId"));
        assertEquals("SampleItemId should match", Integer.parseInt(sampleItem.getId()),
                responseJson.get("sampleItemId").asInt());

        assertTrue("Response should have 'workflowStatus' field", responseJson.has("workflowStatus"));
        assertEquals("WorkflowStatus should default to REGISTERED", "REGISTERED",
                responseJson.get("workflowStatus").asText());

        // BioSample ID should be null or absent since no extension exists
        // (Jackson may omit null values depending on configuration)
        if (responseJson.has("id")) {
            assertTrue("BioSample ID should be null when no extension", responseJson.get("id").isNull());
        }
        // Either way, having no BioSample ID is correct behavior
    }

    @Test
    public void testGetByBarcode_DisposedSample_ReturnsDisposedStatus() throws Exception {
        // Arrange - Create sample and dispose it
        String uniqueBarcode = "DISPOSED-BC-" + System.currentTimeMillis();
        SampleItem sampleItem = createTestSampleItem(testSample, uniqueBarcode);
        BioSample bioSample = createTestBioSample(sampleItem, testShipment, BiosafetyLevel.BSL_2);

        // Set to DISPOSED status
        bioSample.setWorkflowStatus(BioSample.WorkflowStatus.DISPOSED);
        bioSample.setSysUserId(testUser.getId().toString());
        bioSampleService.update(bioSample);

        // Act
        MvcResult result = mockMvc.perform(
                get("/rest/biorepository/sample/by-barcode/" + uniqueBarcode).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert - Should return the sample with DISPOSED status
        // (frontend should check status and display appropriate message)
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Response should have 'workflowStatus' field", responseJson.has("workflowStatus"));
        assertEquals("WorkflowStatus should be DISPOSED", "DISPOSED", responseJson.get("workflowStatus").asText());
    }

    @Test
    public void testGetByBarcode_WithRetentionPolicy_ReturnsRetentionDetails() throws Exception {
        // Arrange - Create sample with retention expiry date
        String uniqueBarcode = "RETENTION-BC-" + System.currentTimeMillis();
        SampleItem sampleItem = createTestSampleItem(testSample, uniqueBarcode);
        BioSample bioSample = createTestBioSample(sampleItem, testShipment, BiosafetyLevel.BSL_2);

        // Set retention expiry date
        java.sql.Date expiryDate = java.sql.Date.valueOf("2026-12-31");
        bioSample.setRetentionExpiryDate(expiryDate);
        bioSample.setWorkflowStatus(BioSample.WorkflowStatus.STORED);
        bioSample.setSysUserId(testUser.getId().toString());
        bioSampleService.update(bioSample);

        // Act
        MvcResult result = mockMvc.perform(
                get("/rest/biorepository/sample/by-barcode/" + uniqueBarcode).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Response should have 'retentionExpiryDate' field", responseJson.has("retentionExpiryDate"));
        assertFalse("RetentionExpiryDate should not be null", responseJson.get("retentionExpiryDate").isNull());
    }

    @Test
    public void testGetByBarcode_WithAccessionNumber_ReturnsAccessionNumber() throws Exception {
        // Arrange - Create sample with specific accession number
        String expectedAccession = "ACC-BC-" + System.currentTimeMillis();
        String uniqueBarcode = "ACC-BARCODE-" + System.currentTimeMillis();

        Sample sample = createTestSample(expectedAccession);
        SampleItem sampleItem = createTestSampleItem(sample, uniqueBarcode);
        createTestBioSample(sampleItem, testShipment, BiosafetyLevel.BSL_2);

        // Act
        MvcResult result = mockMvc.perform(
                get("/rest/biorepository/sample/by-barcode/" + uniqueBarcode).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Response should have 'accessionNumber' field", responseJson.has("accessionNumber"));
        assertEquals("AccessionNumber should match", expectedAccession, responseJson.get("accessionNumber").asText());

        assertTrue("Response should have 'sampleId' field", responseJson.has("sampleId"));
        assertFalse("SampleId should not be null", responseJson.get("sampleId").isNull());
    }

    // ========== HELPER METHODS ==========

    private Sample createTestSample(String accessionNumber) {
        Sample sample = new Sample();
        sample.setAccessionNumber(accessionNumber);
        sample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));
        sample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
        sample.setDomain("H");
        sample.setSysUserId(testUser.getId().toString());
        return sampleService.save(sample);
    }

    private SampleItem createTestSampleItem(Sample sample, String externalId) {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sample);
        sampleItem.setExternalId(externalId);
        sampleItem.setTypeOfSample(testSampleType);
        sampleItem.setSortOrder("1");
        sampleItem.setQuantity(10.0);
        sampleItem.setCollectionDate(new Timestamp(System.currentTimeMillis()));
        sampleItem.setStatusId("1");
        sampleItem.setSysUserId(testUser.getId().toString());
        return sampleItemService.save(sampleItem);
    }

    private Shipment createTestShipment(String deliveryReference) {
        Shipment shipment = new Shipment();
        shipment.setDeliveryReference(deliveryReference);
        shipment.setSenderName("Test Sender");
        shipment.setSenderOrganization("Test Organization");
        shipment.setReceiver(testUser);
        shipment.setPackagingCondition(PackagingCondition.INTACT);
        shipment.setReceptionTimestamp(new Timestamp(System.currentTimeMillis()));
        shipment.setSysUserId(testUser.getId().toString());
        return shipmentService.receiveShipment(shipment);
    }

    private BioSample createTestBioSample(SampleItem sampleItem, Shipment shipment, BiosafetyLevel level) {
        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(level);
        bioSample.setShipment(shipment);
        bioSample.setRequiredTempMin(new BigDecimal("-80.0"));
        bioSample.setRequiredTempMax(new BigDecimal("-70.0"));
        bioSample.setSysUserId(testUser.getId().toString());
        return bioSampleService.createForSampleItem(sampleItem, bioSample);
    }
}
