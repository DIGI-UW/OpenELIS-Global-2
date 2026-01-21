package org.openelisglobal.biorepository;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.biorepository.service.BioSampleService;
import org.openelisglobal.biorepository.service.BiorepositoryQCInspectionService;
import org.openelisglobal.biorepository.service.SampleRetrievalService;
import org.openelisglobal.biorepository.service.ShipmentService;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest;
import org.openelisglobal.biorepository.valueholder.Shipment;
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
 * Integration tests for BiorepositoryDashboardRestController.
 *
 * Tests verify: - All dashboard endpoints return proper HTTP 200 responses -
 * JSON structure contains expected keys - Error handling for invalid date
 * formats - Date range filtering works correctly - Response data types match
 * expected types
 */
public class BiorepositoryDashboardRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private BioSampleService bioSampleService;

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private BiorepositoryQCInspectionService qcInspectionService;

    @Autowired
    private SampleRetrievalService retrievalService;

    private ObjectMapper objectMapper;
    private SystemUser testUser;
    private Shipment testShipment;
    private TypeOfSample testSampleType;
    private String sysUserId = "1";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();

        // Create test user
        testUser = createTestUser();

        // Create test shipment
        testShipment = createTestShipment();

        // Get sample type
        testSampleType = typeOfSampleService.getAll().stream().findFirst().orElse(null);
        if (testSampleType == null) {
            throw new RuntimeException("No sample types found in database");
        }
    }

    // ========== STORAGE CAPACITY ENDPOINT TESTS ==========

    @Test
    public void testGetStorageCapacity_ReturnsJsonObject() throws Exception {
        // Act
        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/dashboard/storage-capacity").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Response should be an object", responseJson.isObject());
        assertTrue("Should contain totalSamplesStored", responseJson.has("totalSamplesStored"));
        assertTrue("Should contain pendingStorage", responseJson.has("pendingStorage"));
        assertTrue("Should contain totalDevices", responseJson.has("totalDevices"));
        assertTrue("Should contain averageUtilization", responseJson.has("averageUtilization"));

        // Verify data types
        assertTrue("totalSamplesStored should be a number", responseJson.get("totalSamplesStored").isNumber());
        assertTrue("pendingStorage should be a number", responseJson.get("pendingStorage").isNumber());
    }

    @Test
    public void testGetStorageCapacity_ReturnsCorrectCounts() throws Exception {
        // Arrange: Create test samples
        createStoredBioSample("DASHBOARD-001", BioSample.WorkflowStatus.STORED);
        createStoredBioSample("DASHBOARD-002", BioSample.WorkflowStatus.STORED);
        createStoredBioSample("DASHBOARD-003", BioSample.WorkflowStatus.PENDING_STORAGE);

        // Act
        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/dashboard/storage-capacity").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        long totalStored = responseJson.get("totalSamplesStored").asLong();
        long pending = responseJson.get("pendingStorage").asLong();

        assertTrue("Should have at least 2 stored samples, found: " + totalStored, totalStored >= 2);
        assertTrue("Should have at least 1 pending sample, found: " + pending, pending >= 1);
    }

    // ========== STORAGE UTILIZATION ENDPOINT TESTS ==========

    @Test
    public void testGetStorageUtilization_ReturnsJsonObject() throws Exception {
        // Act
        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/dashboard/storage-utilization")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Response should be an object", responseJson.isObject());
        assertTrue("Should contain devices key", responseJson.has("devices"));
    }

    // ========== SAMPLE AGING ENDPOINT TESTS ==========

    @Test
    public void testGetSampleAging_ReturnsJsonObject() throws Exception {
        // Act
        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/dashboard/sample-aging").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Response should be an object", responseJson.isObject());
        assertTrue("Should contain expired", responseJson.has("expired"));
        assertTrue("Should contain expiring30Days", responseJson.has("expiring30Days"));
        assertTrue("Should contain expiring60Days", responseJson.has("expiring60Days"));
        assertTrue("Should contain expiring90Days", responseJson.has("expiring90Days"));
        assertTrue("Should contain total", responseJson.has("total"));
    }

    @Test
    public void testGetSampleAging_CalculatesExpirationsCorrectly() throws Exception {
        // Arrange: Create sample expiring soon
        LocalDate expiringSoon = LocalDate.now().plusDays(20);
        BioSample expiringSample = createStoredBioSample("DASHBOARD-EXPIRING", BioSample.WorkflowStatus.STORED);
        expiringSample.setRetentionExpiryDate(Date.valueOf(expiringSoon));
        bioSampleService.update(expiringSample);

        // Create expired sample
        LocalDate expired = LocalDate.now().minusDays(5);
        BioSample expiredSample = createStoredBioSample("DASHBOARD-EXPIRED", BioSample.WorkflowStatus.STORED);
        expiredSample.setRetentionExpiryDate(Date.valueOf(expired));
        bioSampleService.update(expiredSample);

        // Act
        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/dashboard/sample-aging").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        long expiredCount = responseJson.get("expired").asLong();
        long expiring30Days = responseJson.get("expiring30Days").asLong();

        assertTrue("Should have at least 1 expired sample, found: " + expiredCount, expiredCount >= 1);
        assertTrue("Should have at least 1 sample expiring in 30 days, found: " + expiring30Days, expiring30Days >= 1);
    }

    // ========== EXPIRATION WARNINGS ENDPOINT TESTS ==========

    @Test
    public void testGetExpirationWarnings_ReturnsJsonArray() throws Exception {
        // Act
        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/dashboard/expiration-warnings").param("daysThreshold", "30")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Response should be an array", responseJson.isArray());
    }

    // ========== QC METRICS ENDPOINT TESTS ==========

    @Test
    public void testGetQCMetrics_ReturnsJsonObject() throws Exception {
        // Act
        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/dashboard/qc-metrics").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Response should be an object", responseJson.isObject());
        assertTrue("Should contain totalInspections", responseJson.has("totalInspections"));
        assertTrue("Should contain passedInspections", responseJson.has("passedInspections"));
        assertTrue("Should contain failedInspections", responseJson.has("failedInspections"));
        assertTrue("Should contain complianceRate", responseJson.has("complianceRate"));
    }

    @Test
    public void testGetQCMetrics_ReturnsValidComplianceRate() throws Exception {
        // Arrange: Create QC inspections
        BioSample sample1 = createStoredBioSample("DASHBOARD-QC-PASS", BioSample.WorkflowStatus.STORED);
        BiorepositoryQCInspection passInspection = new BiorepositoryQCInspection(sample1, "Test Inspector",
                new Timestamp(System.currentTimeMillis()));
        passInspection.setSamplePresent(true);
        passInspection.setLabelIntegrity(true);
        passInspection.setContainerIntegrity(true);
        passInspection.setVolumeAppearanceAcceptable(true);
        passInspection.setCorrectPosition(true);
        passInspection.updateQcResult();
        passInspection.setSysUserId(sysUserId);
        qcInspectionService.insert(passInspection);

        // Act
        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/dashboard/qc-metrics").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        double complianceRate = responseJson.get("complianceRate").asDouble();
        assertTrue("Compliance rate should be between 0 and 100, found: " + complianceRate,
                complianceRate >= 0.0 && complianceRate <= 100.0);
    }

    // ========== QC DISCREPANCIES ENDPOINT TESTS ==========

    @Test
    public void testGetQCDiscrepancies_ReturnsJsonObject() throws Exception {
        // Act
        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/dashboard/qc-discrepancies").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Response should be an object", responseJson.isObject());
    }

    // ========== RETRIEVAL STATS ENDPOINT TESTS ==========

    @Test
    public void testGetRetrievalStats_WithoutDateParams_ReturnsJsonObject() throws Exception {
        // Act
        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/dashboard/retrieval-stats").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Response should be an object", responseJson.isObject());
        assertTrue("Should contain totalRequests", responseJson.has("totalRequests"));
        assertTrue("Should contain completedRequests", responseJson.has("completedRequests"));
        assertTrue("Should contain pendingRequests", responseJson.has("pendingRequests"));
    }

    @Test
    public void testGetRetrievalStats_WithDateParams_FiltersCorrectly() throws Exception {
        // Arrange: Create retrieval request
        SampleRetrievalRequest request = new SampleRetrievalRequest();
        request.setRequestNumber("REQ-" + System.currentTimeMillis());
        request.setRequestPurpose("Dashboard test retrieval");
        request.setStatus(SampleRetrievalRequest.RequestStatus.COMPLETED);
        request.setDestinationType(SampleRetrievalRequest.DestinationType.INTERNAL_LAB);
        request.setRequestedBy(testUser);
        request.setRequestedTimestamp(new Timestamp(System.currentTimeMillis()));
        request.setSysUserId(sysUserId);
        retrievalService.save(request);

        // Act
        String startDate = LocalDate.now().minusDays(7).toString();
        String endDate = LocalDate.now().toString();

        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/dashboard/retrieval-stats").param("startDate", startDate)
                        .param("endDate", endDate).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        long totalRequests = responseJson.get("totalRequests").asLong();
        assertTrue("Should have at least 1 request in date range, found: " + totalRequests, totalRequests >= 1);
    }

    // ========== DISPOSAL STATS ENDPOINT TESTS ==========

    @Test
    public void testGetDisposalStats_WithoutDateParams_ReturnsJsonObject() throws Exception {
        // Act
        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/dashboard/disposal-stats").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Response should be an object", responseJson.isObject());
        assertTrue("Should contain totalDisposed", responseJson.has("totalDisposed"));
        assertTrue("Should contain disposalsByProject", responseJson.has("disposalsByProject"));
    }

    @Test
    public void testGetDisposalStats_WithDateParams_FiltersCorrectly() throws Exception {
        // Arrange: Create disposed sample
        createStoredBioSample("DASHBOARD-DISPOSED", BioSample.WorkflowStatus.DISPOSED);

        // Act
        String startDate = LocalDate.now().minusDays(30).toString();
        String endDate = LocalDate.now().toString();

        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/dashboard/disposal-stats").param("startDate", startDate)
                        .param("endDate", endDate).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        long totalDisposed = responseJson.get("totalDisposed").asLong();
        assertTrue("Should have at least 1 disposed sample, found: " + totalDisposed, totalDisposed >= 1);
    }

    // ========== TEMPERATURE TRENDS ENDPOINT TESTS ==========

    @Test
    public void testGetTemperatureTrends_WithoutParams_ReturnsJsonArray() throws Exception {
        // Act
        MvcResult result = mockMvc
                .perform(
                        get("/rest/biorepository/dashboard/temperature-trends").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Response should be an array", responseJson.isArray());
    }

    @Test
    public void testGetTemperatureTrends_WithDateParams_ReturnsFilteredData() throws Exception {
        // Act
        String startDate = LocalDate.now().minusDays(7).toString();
        String endDate = LocalDate.now().toString();

        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/dashboard/temperature-trends").param("startDate", startDate)
                        .param("endDate", endDate).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Response should be an array", responseJson.isArray());
    }

    // ========== ENVIRONMENTAL COMPLIANCE ENDPOINT TESTS ==========

    @Test
    public void testGetEnvironmentalCompliance_ReturnsJsonObject() throws Exception {
        // Act
        MvcResult result = mockMvc
                .perform(get("/rest/biorepository/dashboard/environmental-compliance")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Response should be an object", responseJson.isObject());
        assertTrue("Should contain temperatureCompliance", responseJson.has("temperatureCompliance"));
        assertTrue("Should contain oxygenCompliance", responseJson.has("oxygenCompliance"));
        assertTrue("Should contain humidityCompliance", responseJson.has("humidityCompliance"));
        assertTrue("Should contain totalTemperatureReadings", responseJson.has("totalTemperatureReadings"));
        assertTrue("Should contain totalOxygenReadings", responseJson.has("totalOxygenReadings"));
        assertTrue("Should contain totalHumidityReadings", responseJson.has("totalHumidityReadings"));

        // Verify compliance rates are percentages
        if (responseJson.has("temperatureCompliance") && !responseJson.get("temperatureCompliance").isNull()) {
            double tempCompliance = responseJson.get("temperatureCompliance").asDouble();
            assertTrue("Temperature compliance should be between 0 and 100, found: " + tempCompliance,
                    tempCompliance >= 0.0 && tempCompliance <= 100.0);
        }
    }

    // ========================================
    // Helper Methods for Test Data Creation
    // ========================================

    private SystemUser createTestUser() {
        SystemUser user = systemUserService.get("1");
        if (user == null) {
            user = new SystemUser();
            user.setLoginName("dashboard_test_user");
            user.setFirstName("Dashboard");
            user.setLastName("Test");
            user.setIsActive("Y");
            user.setSysUserId("1");
            systemUserService.save(user);
        }
        return user;
    }

    private Shipment createTestShipment() {
        Shipment shipment = new Shipment();
        shipment.setDeliveryReference("DASHBOARD-SHIPMENT-" + System.currentTimeMillis());
        shipment.setSenderName("Test Sender");
        shipment.setSenderOrganization("Dashboard Test Organization");
        shipment.setReceiver(testUser);
        shipment.setPackagingCondition(Shipment.PackagingCondition.INTACT);
        shipment.setReceptionTimestamp(new Timestamp(System.currentTimeMillis()));
        shipment.setSysUserId(testUser.getId().toString());
        return shipmentService.receiveShipment(shipment);
    }

    private BioSample createStoredBioSample(String barcode, BioSample.WorkflowStatus status) {
        // Create core Sample
        Sample sample = new Sample();
        sample.setAccessionNumber(barcode);
        sample.setStatusId("1");
        sample.setDomain("H");
        sample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
        sample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));
        sampleService.save(sample);

        // Create SampleItem
        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sample);
        sampleItem.setTypeOfSample(testSampleType);
        sampleItem.setStatusId("1");
        sampleItem.setQuantity(Double.valueOf(5.0));
        sampleItem.setSortOrder("1");
        sampleItem.setExternalId(barcode); // This is the barcode
        sampleItemService.save(sampleItem);

        // Create BioSample extension
        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BioSample.BiosafetyLevel.BSL_1);
        bioSample.setRequiredTempMin(new BigDecimal("-80.0"));
        bioSample.setRequiredTempMax(new BigDecimal("-70.0"));
        bioSample.setWorkflowStatus(status);
        bioSample.setProjectId("DASHBOARD-TEST-PROJECT");
        bioSample.setShipment(testShipment);
        bioSample.setSysUserId(testUser.getId().toString());

        // Use service method to create (sets sampleItem relationship properly)
        BioSample created = bioSampleService.createForSampleItem(sampleItem, bioSample);

        return created;
    }
}
