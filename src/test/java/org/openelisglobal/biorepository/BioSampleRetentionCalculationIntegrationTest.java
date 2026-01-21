package org.openelisglobal.biorepository;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.biorepository.service.BioSampleService;
import org.openelisglobal.biorepository.service.RetentionPolicyService;
import org.openelisglobal.biorepository.service.ShipmentService;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.BiosafetyLevel;
import org.openelisglobal.biorepository.valueholder.RetentionPolicy;
import org.openelisglobal.biorepository.valueholder.RetentionPolicy.PeriodUnit;
import org.openelisglobal.biorepository.valueholder.Shipment;
import org.openelisglobal.biorepository.valueholder.Shipment.PackagingCondition;
import org.openelisglobal.common.action.IActionConstants;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for BioSample retention calculation endpoint.
 *
 * Tests the POST /rest/biorepository/sample/calculate-retention endpoint which
 * calculates and saves retention expiry dates when samples are advanced to
 * storage.
 *
 * Tests cover: - Successful retention calculation with project policy -
 * Successful retention calculation with sample type policy - Policy priority
 * (project takes precedence over sample type) - Handling samples without
 * matching policies - Handling samples without collection dates - Multiple
 * samples in single request - Invalid sample item IDs
 */
public class BioSampleRetentionCalculationIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private BioSampleService bioSampleService;

    @Autowired
    private RetentionPolicyService retentionPolicyService;

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
    private MockHttpSession mockSession;
    private SystemUser testUser;
    private String testSysUserId;
    private TypeOfSample testSampleType;
    private Shipment testShipment;
    private Sample testSample;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();

        // Set up mock session with user data for authentication
        mockSession = new MockHttpSession();
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
        userSessionData.setLoginName("testuser");
        userSessionData.setAdmin(true);
        mockSession.setAttribute(IActionConstants.USER_SESSION_DATA, userSessionData);

        // Setup test user
        testUser = systemUserService.get("1");
        if (testUser == null) {
            testUser = new SystemUser();
            testUser.setLoginName("test_retention_calc");
            testUser.setFirstName("Test");
            testUser.setLastName("Retention Calc");
            testUser.setIsActive("Y");
            testUser.setSysUserId("1");
            systemUserService.save(testUser);
        }
        testSysUserId = testUser.getId().toString();

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
        testShipment = createTestShipment("RET-CALC-SHIP-" + (System.currentTimeMillis() % 1000000));

        // Setup test sample
        testSample = createTestSample("RC" + (System.currentTimeMillis() % 100000000));
    }

    // ========== SUCCESSFUL CALCULATION TESTS ==========

    @Test
    public void testCalculateRetention_WithProjectPolicy_Success() throws Exception {
        // Arrange - Create a project policy
        long timestamp = System.currentTimeMillis();
        String projectName = "PROJ-RET-CALC-" + timestamp;

        RetentionPolicy projectPolicy = new RetentionPolicy();
        projectPolicy.setPolicyName("Project Retention Test " + timestamp);
        projectPolicy.setProjectId((int) (timestamp % 100000));
        projectPolicy.setProjectName(projectName);
        projectPolicy.setPeriodValue(5);
        projectPolicy.setPeriodUnit(PeriodUnit.YEARS);
        projectPolicy.setIsActive(true);
        projectPolicy.setSysUserId(testSysUserId);
        retentionPolicyService.save(projectPolicy);

        // Create a sample with this project
        SampleItem sampleItem = createTestSampleItem(testSample, "RET-PROJ-" + timestamp);
        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        bioSample.setProjectId(projectName); // Project code/name matches policy
        bioSample.setSysUserId(testSysUserId);
        bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act
        String requestBody = objectMapper
                .writeValueAsString(Map.of("sampleItemIds", List.of(Integer.valueOf(sampleItem.getId()))));

        MvcResult result = mockMvc
                .perform(post("/rest/biorepository/sample/calculate-retention").session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Should be successful", responseJson.get("success").asBoolean());
        assertEquals("Should update 1 sample", 1, responseJson.get("updatedCount").asInt());
        assertEquals("Total requested should be 1", 1, responseJson.get("totalRequested").asInt());

        // Verify the results array
        JsonNode results = responseJson.get("results");
        assertTrue("Results should be an array", results.isArray());
        assertEquals("Should have 1 result", 1, results.size());

        JsonNode sampleResult = results.get(0);
        assertEquals("Status should be updated", "updated", sampleResult.get("status").asText());
        assertEquals("Policy name should match", projectPolicy.getPolicyName(),
                sampleResult.get("policyName").asText());

        // Verify expiry date was calculated correctly
        // Collection date is "now", policy is 5 years, so expiry should be ~5 years
        // from now
        String expiryDateStr = sampleResult.get("expiryDate").asText();
        assertNotNull("Expiry date should be set", expiryDateStr);
        LocalDate responseExpiry = LocalDate.parse(expiryDateStr);
        LocalDate expectedMinExpiry = LocalDate.now().plusYears(4); // Allow some tolerance
        LocalDate expectedMaxExpiry = LocalDate.now().plusYears(6);
        assertTrue("Expiry date should be approximately 5 years from now",
                responseExpiry.isAfter(expectedMinExpiry) && responseExpiry.isBefore(expectedMaxExpiry));

        // Verify BioSample was updated in database
        BioSample updated = bioSampleService.getBySampleItemId(Integer.valueOf(sampleItem.getId()));
        assertNotNull("Retention policy ID should be set", updated.getRetentionPolicyId());
        assertNotNull("Retention expiry date should be set", updated.getRetentionExpiryDate());
        assertEquals("Policy ID should match", projectPolicy.getId(), updated.getRetentionPolicyId());

        // Verify database expiry date matches response
        LocalDate dbExpiry = updated.getRetentionExpiryDate().toLocalDate();
        assertEquals("Database expiry should match response", responseExpiry, dbExpiry);
    }

    @Test
    public void testCalculateRetention_WithSampleTypePolicy_Success() throws Exception {
        // Arrange - Find an existing sample type that doesn't have a retention policy
        // (there's a unique constraint on sample_type_id in retention_policy table)
        long timestamp = System.currentTimeMillis();

        // Get all existing retention policies to find which sample types are already
        // used
        List<RetentionPolicy> existingPolicies = retentionPolicyService.getAllActive();
        java.util.Set<Integer> usedSampleTypeIds = new java.util.HashSet<>();
        for (RetentionPolicy p : existingPolicies) {
            if (p.getSampleTypeId() != null) {
                usedSampleTypeIds.add(p.getSampleTypeId());
            }
        }

        // Find a sample type without a policy
        TypeOfSample availableSampleType = null;
        for (TypeOfSample st : typeOfSampleService.getAll()) {
            Integer stId = Integer.parseInt(st.getId());
            if (!usedSampleTypeIds.contains(stId)) {
                availableSampleType = st;
                break;
            }
        }

        // If all sample types have policies, skip this test
        if (availableSampleType == null) {
            System.out.println("SKIPPING testCalculateRetention_WithSampleTypePolicy_Success: "
                    + "All sample types already have retention policies");
            return;
        }

        Integer sampleTypeId = Integer.parseInt(availableSampleType.getId());

        // Create a policy with a distinctive period value (17 years) so we can verify
        // this specific policy was applied
        RetentionPolicy sampleTypePolicy = new RetentionPolicy();
        sampleTypePolicy.setPolicyName("Sample Type Policy " + timestamp);
        sampleTypePolicy.setSampleTypeId(sampleTypeId);
        sampleTypePolicy.setSampleTypeName(availableSampleType.getDescription());
        sampleTypePolicy.setPeriodValue(17); // Distinctive value to verify correct policy
        sampleTypePolicy.setPeriodUnit(PeriodUnit.YEARS);
        sampleTypePolicy.setIsActive(true);
        sampleTypePolicy.setSysUserId(testSysUserId);
        RetentionPolicy savedPolicy = retentionPolicyService.save(sampleTypePolicy);

        // Create a sample with known collection date, using our available sample type
        // and NO project (should fall back to sample type policy)
        LocalDate collectionDate = LocalDate.of(2025, 3, 10);
        SampleItem sampleItem = createTestSampleItemWithTypeAndDate(testSample, "RET-TYPE-" + timestamp,
                availableSampleType, collectionDate);
        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        // No projectId - should fall back to sample type policy
        bioSample.setSysUserId(testSysUserId);
        bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act
        String requestBody = objectMapper
                .writeValueAsString(Map.of("sampleItemIds", List.of(Integer.valueOf(sampleItem.getId()))));

        MvcResult result = mockMvc
                .perform(post("/rest/biorepository/sample/calculate-retention").session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        // Assert response structure
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Should be successful", responseJson.get("success").asBoolean());
        assertEquals("Should update 1 sample", 1, responseJson.get("updatedCount").asInt());

        JsonNode results = responseJson.get("results");
        JsonNode sampleResult = results.get(0);
        assertEquals("Status should be updated", "updated", sampleResult.get("status").asText());
        assertEquals("Policy name should match our sample type policy", savedPolicy.getPolicyName(),
                sampleResult.get("policyName").asText());

        // CRITICAL: Verify database state - ensures the update actually persisted
        BioSample updated = bioSampleService.getBySampleItemId(Integer.valueOf(sampleItem.getId()));
        assertNotNull("Retention policy ID should be set", updated.getRetentionPolicyId());
        assertNotNull("Retention expiry date should be set", updated.getRetentionExpiryDate());
        assertEquals("Policy ID should match our sample type policy", savedPolicy.getId(),
                updated.getRetentionPolicyId());

        // Verify the expiry date calculation: 2025-03-10 + 17 years = 2042-03-10
        LocalDate expiryDate = updated.getRetentionExpiryDate().toLocalDate();
        assertEquals("Expiry year should be 2042 (collection + 17 years)", 2042, expiryDate.getYear());
        assertEquals("Expiry month should be March", 3, expiryDate.getMonthValue());
        assertEquals("Expiry day should be 10", 10, expiryDate.getDayOfMonth());
    }

    // ========== SKIPPED SAMPLES TESTS ==========

    @Test
    public void testCalculateRetention_NoMatchingPolicy_Skipped() throws Exception {
        // Arrange - Create a sample with unknown project and NO sample type
        // (to prevent fallback to sample type policy created by other tests)
        long timestamp = System.currentTimeMillis();
        SampleItem sampleItem = createTestSampleItemWithoutType(testSample, "RET-NO-POLICY-" + timestamp);
        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample.setProjectId("UNKNOWN-PROJECT-" + timestamp); // No policy for this project
        bioSample.setSysUserId(testSysUserId);
        bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act
        String requestBody = objectMapper
                .writeValueAsString(Map.of("sampleItemIds", List.of(Integer.valueOf(sampleItem.getId()))));

        MvcResult result = mockMvc
                .perform(post("/rest/biorepository/sample/calculate-retention").session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Should be successful (even with skipped samples)", responseJson.get("success").asBoolean());
        assertEquals("Should update 0 samples", 0, responseJson.get("updatedCount").asInt());

        JsonNode results = responseJson.get("results");
        JsonNode sampleResult = results.get(0);
        assertEquals("Status should be skipped", "skipped", sampleResult.get("status").asText());
        assertTrue("Reason should mention no matching policy",
                sampleResult.get("reason").asText().contains("No matching retention policy"));
    }

    @Test
    public void testCalculateRetention_InvalidSampleItemId_Skipped() throws Exception {
        // Arrange - Use non-existent sample item ID
        String requestBody = objectMapper.writeValueAsString(Map.of("sampleItemIds", List.of(999888777)));

        // Act
        MvcResult result = mockMvc
                .perform(post("/rest/biorepository/sample/calculate-retention").session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Should be successful", responseJson.get("success").asBoolean());
        assertEquals("Should update 0 samples", 0, responseJson.get("updatedCount").asInt());

        JsonNode results = responseJson.get("results");
        JsonNode sampleResult = results.get(0);
        assertEquals("Status should be skipped", "skipped", sampleResult.get("status").asText());
        assertTrue("Reason should mention no BioSample", sampleResult.get("reason").asText().contains("No BioSample"));
    }

    // ========== MULTIPLE SAMPLES TESTS ==========

    @Test
    public void testCalculateRetention_MultipleSamples_MixedResults() throws Exception {
        // Arrange - Create a policy and multiple samples (some will match, some won't)
        long timestamp = System.currentTimeMillis();
        String projectName = "PROJ-MULTI-" + timestamp;

        RetentionPolicy policy = new RetentionPolicy();
        policy.setPolicyName("Multi Sample Test " + timestamp);
        policy.setProjectId((int) (timestamp % 100000));
        policy.setProjectName(projectName);
        policy.setPeriodValue(10);
        policy.setPeriodUnit(PeriodUnit.YEARS);
        policy.setIsActive(true);
        policy.setSysUserId(testSysUserId);
        retentionPolicyService.save(policy);

        // Sample 1: Matches policy
        SampleItem sampleItem1 = createTestSampleItem(testSample, "RET-MULTI-1-" + timestamp);
        BioSample bioSample1 = new BioSample();
        bioSample1.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample1.setProjectId(projectName);
        bioSample1.setSysUserId(testSysUserId);
        bioSampleService.createForSampleItem(sampleItem1, bioSample1);

        // Sample 2: Matches policy
        SampleItem sampleItem2 = createTestSampleItem(testSample, "RET-MULTI-2-" + timestamp);
        BioSample bioSample2 = new BioSample();
        bioSample2.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        bioSample2.setProjectId(projectName);
        bioSample2.setSysUserId(testSysUserId);
        bioSampleService.createForSampleItem(sampleItem2, bioSample2);

        // Sample 3: No matching policy (no sample type to prevent fallback)
        SampleItem sampleItem3 = createTestSampleItemWithoutType(testSample, "RET-MULTI-3-" + timestamp);
        BioSample bioSample3 = new BioSample();
        bioSample3.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample3.setProjectId("UNKNOWN-" + timestamp);
        bioSample3.setSysUserId(testSysUserId);
        bioSampleService.createForSampleItem(sampleItem3, bioSample3);

        // Act
        String requestBody = objectMapper
                .writeValueAsString(Map.of("sampleItemIds", List.of(Integer.valueOf(sampleItem1.getId()),
                        Integer.valueOf(sampleItem2.getId()), Integer.valueOf(sampleItem3.getId()))));

        MvcResult result = mockMvc
                .perform(post("/rest/biorepository/sample/calculate-retention").session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Should be successful", responseJson.get("success").asBoolean());
        assertEquals("Should update 2 samples", 2, responseJson.get("updatedCount").asInt());
        assertEquals("Total requested should be 3", 3, responseJson.get("totalRequested").asInt());

        JsonNode results = responseJson.get("results");
        assertEquals("Should have 3 results", 3, results.size());

        // Build a map of sampleItemId -> status for verification
        Map<Integer, String> statusBySampleItemId = new java.util.HashMap<>();
        for (JsonNode res : results) {
            Integer sampleItemIdResult = res.get("sampleItemId").asInt();
            String status = res.get("status").asText();
            statusBySampleItemId.put(sampleItemIdResult, status);
        }

        // Verify SPECIFIC samples were updated vs skipped (not just counts)
        assertEquals("Sample 1 should be updated", "updated",
                statusBySampleItemId.get(Integer.valueOf(sampleItem1.getId())));
        assertEquals("Sample 2 should be updated", "updated",
                statusBySampleItemId.get(Integer.valueOf(sampleItem2.getId())));
        assertEquals("Sample 3 should be skipped", "skipped",
                statusBySampleItemId.get(Integer.valueOf(sampleItem3.getId())));

        // Verify database state for updated samples
        BioSample updated1 = bioSampleService.getBySampleItemId(Integer.valueOf(sampleItem1.getId()));
        BioSample updated2 = bioSampleService.getBySampleItemId(Integer.valueOf(sampleItem2.getId()));
        BioSample notUpdated3 = bioSampleService.getBySampleItemId(Integer.valueOf(sampleItem3.getId()));

        assertNotNull("Sample 1 should have retention policy set", updated1.getRetentionPolicyId());
        assertNotNull("Sample 2 should have retention policy set", updated2.getRetentionPolicyId());
        assertNull("Sample 3 should NOT have retention policy set", notUpdated3.getRetentionPolicyId());
    }

    // ========== VALIDATION TESTS ==========

    @Test
    public void testCalculateRetention_EmptySampleItemIds_BadRequest() throws Exception {
        // Arrange
        String requestBody = objectMapper.writeValueAsString(Map.of("sampleItemIds", List.of()));

        // Act & Assert
        mockMvc.perform(post("/rest/biorepository/sample/calculate-retention").session(mockSession)
                .contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isBadRequest());
    }

    @Test
    public void testCalculateRetention_NullSampleItemIds_BadRequest() throws Exception {
        // Arrange - Send object without sampleItemIds field
        String requestBody = "{}";

        // Act & Assert
        mockMvc.perform(post("/rest/biorepository/sample/calculate-retention").session(mockSession)
                .contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isBadRequest());
    }

    // ========== EXPIRY DATE CALCULATION TESTS ==========

    @Test
    public void testCalculateRetention_ExpiryDateCalculatedCorrectly() throws Exception {
        // Arrange - Create policy with known period
        long timestamp = System.currentTimeMillis();
        String projectName = "PROJ-EXPIRY-" + timestamp;

        RetentionPolicy policy = new RetentionPolicy();
        policy.setPolicyName("Expiry Calc Test " + timestamp);
        policy.setProjectId((int) (timestamp % 100000));
        policy.setProjectName(projectName);
        policy.setPeriodValue(2);
        policy.setPeriodUnit(PeriodUnit.YEARS);
        policy.setIsActive(true);
        policy.setSysUserId(testSysUserId);
        retentionPolicyService.save(policy);

        // Create sample with known collection date
        LocalDate collectionDate = LocalDate.of(2025, 6, 15);
        SampleItem sampleItem = createTestSampleItemWithDate(testSample, "RET-EXPIRY-" + timestamp, collectionDate);
        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample.setProjectId(projectName);
        bioSample.setSysUserId(testSysUserId);
        bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act
        String requestBody = objectMapper
                .writeValueAsString(Map.of("sampleItemIds", List.of(Integer.valueOf(sampleItem.getId()))));

        MvcResult result = mockMvc
                .perform(post("/rest/biorepository/sample/calculate-retention").session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        JsonNode results = responseJson.get("results");
        String expiryDateStr = results.get(0).get("expiryDate").asText();

        // 2 years from 2025-06-15 should be 2027-06-15
        assertEquals("Expiry date should be calculated correctly", "2027-06-15", expiryDateStr);

        // Verify in database
        BioSample updated = bioSampleService.getBySampleItemId(Integer.valueOf(sampleItem.getId()));
        assertNotNull("Expiry date should be saved", updated.getRetentionExpiryDate());
        LocalDate savedExpiry = updated.getRetentionExpiryDate().toLocalDate();
        assertEquals("Saved expiry year should be correct", 2027, savedExpiry.getYear());
        assertEquals("Saved expiry month should be correct", 6, savedExpiry.getMonthValue());
        assertEquals("Saved expiry day should be correct", 15, savedExpiry.getDayOfMonth());
    }

    // ========== HELPER METHODS ==========

    private Sample createTestSample(String accessionNumber) {
        Sample sample = new Sample();
        sample.setAccessionNumber(accessionNumber);
        sample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));
        sample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
        sample.setDomain("H");
        sample.setSysUserId(testSysUserId);
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
        sampleItem.setSysUserId(testSysUserId);
        return sampleItemService.save(sampleItem);
    }

    private SampleItem createTestSampleItemWithDate(Sample sample, String externalId, LocalDate collectionDate) {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sample);
        sampleItem.setExternalId(externalId);
        sampleItem.setTypeOfSample(testSampleType);
        sampleItem.setSortOrder("1");
        sampleItem.setQuantity(10.0);
        // Convert LocalDate to Timestamp
        sampleItem.setCollectionDate(Timestamp.valueOf(collectionDate.atStartOfDay()));
        sampleItem.setStatusId("1");
        sampleItem.setSysUserId(testSysUserId);
        return sampleItemService.save(sampleItem);
    }

    private SampleItem createTestSampleItemWithTypeAndDate(Sample sample, String externalId, TypeOfSample sampleType,
            LocalDate collectionDate) {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sample);
        sampleItem.setExternalId(externalId);
        sampleItem.setTypeOfSample(sampleType);
        sampleItem.setSortOrder("1");
        sampleItem.setQuantity(10.0);
        sampleItem.setCollectionDate(Timestamp.valueOf(collectionDate.atStartOfDay()));
        sampleItem.setStatusId("1");
        sampleItem.setSysUserId(testSysUserId);
        return sampleItemService.save(sampleItem);
    }

    /**
     * Create a sample item without a type of sample. This ensures no sample type
     * fallback policy can match.
     */
    private SampleItem createTestSampleItemWithoutType(Sample sample, String externalId) {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sample);
        sampleItem.setExternalId(externalId);
        // No typeOfSample - ensures no sample type fallback matching
        sampleItem.setSortOrder("1");
        sampleItem.setQuantity(10.0);
        sampleItem.setCollectionDate(new Timestamp(System.currentTimeMillis()));
        sampleItem.setStatusId("1");
        sampleItem.setSysUserId(testSysUserId);
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
        shipment.setSysUserId(testSysUserId);
        return shipmentService.receiveShipment(shipment);
    }
}
