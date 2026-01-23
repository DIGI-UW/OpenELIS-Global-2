package org.openelisglobal.biorepository;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.biorepository.service.RetentionPolicyService;
import org.openelisglobal.biorepository.valueholder.RetentionPolicy;
import org.openelisglobal.biorepository.valueholder.RetentionPolicy.PeriodUnit;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for RetentionPolicyRestController.
 *
 * Tests cover: - GET /rest/biorepository/retention-policies - list all active
 * policies - GET /rest/biorepository/retention-policies/{id} - get single
 * policy - GET /rest/biorepository/retention-policies/template - get CSV
 * template - DTO field mapping verification
 */
public class RetentionPolicyRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private RetentionPolicyService retentionPolicyService;

    @Autowired
    private SystemUserService systemUserService;

    private ObjectMapper objectMapper;
    private SystemUser testUser;
    private String testSysUserId;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();

        // Setup test user
        testUser = systemUserService.get("1");
        if (testUser == null) {
            testUser = new SystemUser();
            testUser.setLoginName("test_retention_controller");
            testUser.setFirstName("Test");
            testUser.setLastName("Retention Controller");
            testUser.setIsActive("Y");
            testUser.setSysUserId("1");
            systemUserService.save(testUser);
        }
        testSysUserId = testUser.getId().toString();
    }

    // ========== GET ALL POLICIES TESTS ==========

    @Test
    public void testGetAllPolicies_ReturnsJsonArray() throws Exception {
        // Act
        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/retention-policies").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should be an array", responseJson.isArray());
    }

    @Test
    public void testGetAllPolicies_ReturnsActivePolices() throws Exception {
        // Arrange - Create a test policy
        long timestamp = System.currentTimeMillis();
        RetentionPolicy policy = createAndSavePolicy("GET All Test " + timestamp, null, null, 5, PeriodUnit.YEARS);

        // Act
        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/retention-policies").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should be an array", responseJson.isArray());

        // Find our test policy
        boolean found = false;
        for (JsonNode node : responseJson) {
            if (node.has("policyName") && node.get("policyName").asText().equals(policy.getPolicyName())) {
                found = true;
                assertEquals("Period value should match", 5, node.get("periodValue").asInt());
                assertEquals("Period unit should match", "YEARS", node.get("periodUnit").asText());
                break;
            }
        }
        assertTrue("Should find created policy in response", found);
    }

    @Test
    public void testGetAllPolicies_ExcludesInactivePolicies() throws Exception {
        // Arrange - Create an inactive policy
        long timestamp = System.currentTimeMillis();
        String policyName = "Inactive Policy Test " + timestamp;

        RetentionPolicy policy = new RetentionPolicy();
        policy.setPolicyName(policyName);
        policy.setProjectId((int) (timestamp % 100000));
        policy.setPeriodValue(5);
        policy.setPeriodUnit(PeriodUnit.YEARS);
        policy.setIsActive(false); // Inactive
        policy.setSysUserId(testSysUserId);
        retentionPolicyService.save(policy);

        // Act
        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/retention-policies").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert - Inactive policy should not be in response
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        boolean found = false;
        for (JsonNode node : responseJson) {
            if (node.has("policyName") && node.get("policyName").asText().equals(policyName)) {
                found = true;
                break;
            }
        }
        assertFalse("Inactive policy should NOT be in response", found);
    }

    // ========== GET SINGLE POLICY TESTS ==========

    @Test
    public void testGetPolicy_Found_ReturnsCorrectFields() throws Exception {
        // Arrange
        long timestamp = System.currentTimeMillis();
        RetentionPolicy policy = createAndSavePolicy("GET Single Test " + timestamp, null, null, 10, PeriodUnit.YEARS);

        // Act
        MvcResult result = mockMvc.perform(
                get("/rest/biorepository/retention-policies/" + policy.getId()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertEquals("ID should match", policy.getId().intValue(), responseJson.get("id").asInt());
        assertEquals("Policy name should match", policy.getPolicyName(), responseJson.get("policyName").asText());
        assertEquals("Period value should match", 10, responseJson.get("periodValue").asInt());
        assertEquals("Period unit should match", "YEARS", responseJson.get("periodUnit").asText());
        assertEquals("Period display should match", "10 years", responseJson.get("periodDisplay").asText());
        assertTrue("Should be active", responseJson.get("isActive").asBoolean());
    }

    @Test
    public void testGetPolicy_NotFound_Returns404() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/rest/biorepository/retention-policies/999999").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ========== TEMPLATE ENDPOINT TESTS ==========

    @Test
    public void testGetCsvTemplate_ReturnsValidCsv() throws Exception {
        // Act
        MvcResult result = mockMvc.perform(get("/rest/biorepository/retention-policies/template"))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String contentType = result.getResponse().getContentType();
        assertTrue("Content type should be CSV", contentType != null && contentType.contains("text/csv"));

        String disposition = result.getResponse().getHeader("Content-Disposition");
        assertTrue("Content disposition should specify filename",
                disposition != null && disposition.contains("retention_policy_template.csv"));

        String csvContent = result.getResponse().getContentAsString();
        assertTrue("CSV should have header row", csvContent.contains("Policy Name,Project,Sample Type,Period"));
        assertTrue("CSV should have example data", csvContent.contains("years"));
    }

    // ========== DTO FIELD TESTS ==========

    @Test
    public void testGetPolicy_IncludesAllDtoFields() throws Exception {
        // Arrange
        long timestamp = System.currentTimeMillis();
        RetentionPolicy policy = new RetentionPolicy();
        policy.setPolicyName("DTO Fields Test " + timestamp);
        policy.setProjectId((int) (timestamp % 100000));
        policy.setProjectName("DTO Test Project");
        policy.setSampleTypeId(123);
        policy.setSampleTypeName("Test Sample Type");
        policy.setPeriodValue(18);
        policy.setPeriodUnit(PeriodUnit.MONTHS);
        policy.setDescription("Test description for DTO");
        policy.setIsActive(true);
        policy.setSysUserId(testSysUserId);
        retentionPolicyService.save(policy);

        // Act
        MvcResult result = mockMvc.perform(
                get("/rest/biorepository/retention-policies/" + policy.getId()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertEquals("ID should match", policy.getId().intValue(), responseJson.get("id").asInt());
        assertEquals("Policy name should match", "DTO Fields Test " + timestamp,
                responseJson.get("policyName").asText());
        assertEquals("Project ID should match", policy.getProjectId().intValue(),
                responseJson.get("projectId").asInt());
        assertEquals("Project name should match", "DTO Test Project", responseJson.get("projectName").asText());
        assertEquals("Sample type ID should match", 123, responseJson.get("sampleTypeId").asInt());
        assertEquals("Sample type name should match", "Test Sample Type", responseJson.get("sampleTypeName").asText());
        assertEquals("Period value should match", 18, responseJson.get("periodValue").asInt());
        assertEquals("Period unit should match", "MONTHS", responseJson.get("periodUnit").asText());
        assertEquals("Period display should match", "18 months", responseJson.get("periodDisplay").asText());
        assertTrue("Should be active", responseJson.get("isActive").asBoolean());
        assertEquals("Description should match", "Test description for DTO", responseJson.get("description").asText());
    }

    @Test
    public void testGetPolicy_WithProjectOnly() throws Exception {
        // Arrange
        long timestamp = System.currentTimeMillis();
        RetentionPolicy policy = new RetentionPolicy();
        policy.setPolicyName("Project Only Test " + timestamp);
        policy.setProjectId((int) (timestamp % 100000));
        policy.setProjectName("Project Only Project");
        // No sample type
        policy.setPeriodValue(10);
        policy.setPeriodUnit(PeriodUnit.YEARS);
        policy.setIsActive(true);
        policy.setSysUserId(testSysUserId);
        retentionPolicyService.save(policy);

        // Act
        MvcResult result = mockMvc.perform(
                get("/rest/biorepository/retention-policies/" + policy.getId()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertEquals("Project ID should match", policy.getProjectId().intValue(),
                responseJson.get("projectId").asInt());
        assertFalse("Sample type ID should be null",
                responseJson.has("sampleTypeId") && !responseJson.get("sampleTypeId").isNull());
    }

    @Test
    public void testGetPolicy_WithSampleTypeOnly() throws Exception {
        // Arrange
        long timestamp = System.currentTimeMillis();
        Integer sampleTypeId = (int) (timestamp % 100000) + 50000;

        RetentionPolicy policy = new RetentionPolicy();
        policy.setPolicyName("Sample Type Only Test " + timestamp);
        // No project
        policy.setSampleTypeId(sampleTypeId);
        policy.setSampleTypeName("Sample Type Only");
        policy.setPeriodValue(5);
        policy.setPeriodUnit(PeriodUnit.YEARS);
        policy.setIsActive(true);
        policy.setSysUserId(testSysUserId);
        retentionPolicyService.save(policy);

        // Act
        MvcResult result = mockMvc.perform(
                get("/rest/biorepository/retention-policies/" + policy.getId()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertEquals("Sample type ID should match", sampleTypeId.intValue(), responseJson.get("sampleTypeId").asInt());
        assertFalse("Project ID should be null",
                responseJson.has("projectId") && !responseJson.get("projectId").isNull());
    }

    // ========== PERIOD DISPLAY TESTS ==========

    @Test
    public void testPeriodDisplay_AllUnits() throws Exception {
        // Test YEARS
        RetentionPolicy yearsPolicy = createAndSavePolicy("Years Test " + System.currentTimeMillis(), null, null, 5,
                PeriodUnit.YEARS);
        MvcResult yearsResult = mockMvc.perform(get("/rest/biorepository/retention-policies/" + yearsPolicy.getId())
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();
        JsonNode yearsJson = objectMapper.readTree(yearsResult.getResponse().getContentAsString());
        assertEquals("Years display should be correct", "5 years", yearsJson.get("periodDisplay").asText());

        // Test MONTHS
        RetentionPolicy monthsPolicy = createAndSavePolicy("Months Test " + System.currentTimeMillis(), null, null, 18,
                PeriodUnit.MONTHS);
        MvcResult monthsResult = mockMvc.perform(get("/rest/biorepository/retention-policies/" + monthsPolicy.getId())
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();
        JsonNode monthsJson = objectMapper.readTree(monthsResult.getResponse().getContentAsString());
        assertEquals("Months display should be correct", "18 months", monthsJson.get("periodDisplay").asText());

        // Test DAYS
        RetentionPolicy daysPolicy = createAndSavePolicy("Days Test " + System.currentTimeMillis(), null, null, 30,
                PeriodUnit.DAYS);
        MvcResult daysResult = mockMvc.perform(get("/rest/biorepository/retention-policies/" + daysPolicy.getId())
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();
        JsonNode daysJson = objectMapper.readTree(daysResult.getResponse().getContentAsString());
        assertEquals("Days display should be correct", "30 days", daysJson.get("periodDisplay").asText());
    }

    @Test
    public void testPeriodDisplay_SingularForms() throws Exception {
        // Test 1 year
        RetentionPolicy yearPolicy = createAndSavePolicy("Single Year " + System.currentTimeMillis(), null, null, 1,
                PeriodUnit.YEARS);
        MvcResult yearResult = mockMvc.perform(get("/rest/biorepository/retention-policies/" + yearPolicy.getId())
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();
        JsonNode yearJson = objectMapper.readTree(yearResult.getResponse().getContentAsString());
        assertEquals("Single year display should be correct", "1 year", yearJson.get("periodDisplay").asText());

        // Test 1 month
        RetentionPolicy monthPolicy = createAndSavePolicy("Single Month " + System.currentTimeMillis(), null, null, 1,
                PeriodUnit.MONTHS);
        MvcResult monthResult = mockMvc.perform(get("/rest/biorepository/retention-policies/" + monthPolicy.getId())
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();
        JsonNode monthJson = objectMapper.readTree(monthResult.getResponse().getContentAsString());
        assertEquals("Single month display should be correct", "1 month", monthJson.get("periodDisplay").asText());

        // Test 1 day
        RetentionPolicy dayPolicy = createAndSavePolicy("Single Day " + System.currentTimeMillis(), null, null, 1,
                PeriodUnit.DAYS);
        MvcResult dayResult = mockMvc.perform(get("/rest/biorepository/retention-policies/" + dayPolicy.getId())
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();
        JsonNode dayJson = objectMapper.readTree(dayResult.getResponse().getContentAsString());
        assertEquals("Single day display should be correct", "1 day", dayJson.get("periodDisplay").asText());
    }

    // ========== HELPER METHODS ==========

    private RetentionPolicy createAndSavePolicy(String name, Integer projectId, Integer sampleTypeId, int periodValue,
            PeriodUnit periodUnit) {
        RetentionPolicy policy = new RetentionPolicy();
        policy.setPolicyName(name);
        if (projectId != null) {
            policy.setProjectId(projectId);
            policy.setProjectName("Project " + projectId);
        }
        if (sampleTypeId != null) {
            policy.setSampleTypeId(sampleTypeId);
            policy.setSampleTypeName("Sample Type " + sampleTypeId);
        }
        policy.setPeriodValue(periodValue);
        policy.setPeriodUnit(periodUnit);
        policy.setIsActive(true);
        policy.setSysUserId(testSysUserId);
        return retentionPolicyService.save(policy);
    }
}
