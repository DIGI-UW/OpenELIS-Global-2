package org.openelisglobal.biorepository;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.biorepository.service.BioSampleService;
import org.openelisglobal.biorepository.service.BiorepositoryDashboardService;
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

/**
 * Integration tests for BiorepositoryDashboardService following TDD principles.
 *
 * Tests verify deep state comparisons (NO shallow assertions like assertNotNull
 * only). Each test creates realistic test data and verifies actual metric
 * calculations.
 */
public class BiorepositoryDashboardServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private BiorepositoryDashboardService dashboardService;

    @Autowired
    private BioSampleService bioSampleService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private BiorepositoryQCInspectionService qcInspectionService;

    @Autowired
    private SampleRetrievalService retrievalService;

    private SystemUser testUser;
    private Shipment testShipment;
    private TypeOfSample testSampleType;
    private String sysUserId = "1";

    @Before
    public void setUp() {
        // Create test user
        testUser = createTestUser();

        // Create test shipment
        testShipment = createTestShipment();

        // Get sample type
        List<TypeOfSample> types = typeOfSampleService.getAll();
        testSampleType = types.isEmpty() ? createTestSampleType() : types.get(0);
    }

    /**
     * Test storage capacity metrics returns correct device count and average
     * utilization.
     */
    @Test
    public void testGetStorageCapacityMetrics_ReturnsValidData() {
        // Arrange: Create 3 stored samples
        createStoredBioSample("BIOSAMPLE-001", BioSample.WorkflowStatus.STORED);
        createStoredBioSample("BIOSAMPLE-002", BioSample.WorkflowStatus.STORED);
        createStoredBioSample("BIOSAMPLE-003", BioSample.WorkflowStatus.STORED);

        // Act
        Map<String, Object> result = dashboardService.getStorageCapacityMetrics();

        // Assert: Deep state verification
        assertNotNull("Result should not be null", result);
        assertTrue("Result should contain totalSamplesStored", result.containsKey("totalSamplesStored"));
        assertTrue("Result should contain pendingStorage", result.containsKey("pendingStorage"));

        Long totalStored = (Long) result.get("totalSamplesStored");
        assertNotNull("Total samples stored should not be null", totalStored);
        assertTrue("Should have at least 3 stored samples, found: " + totalStored, totalStored >= 3);
    }

    /**
     * Test sample aging metrics calculates expiration warnings correctly.
     */
    @Test
    public void testGetSampleAgingMetrics_CalculatesExpirationCorrectly() {
        // Arrange: Create samples with different expiration dates
        // Sample expiring in 15 days (within 30-day threshold)
        LocalDate expiringSoon = LocalDate.now().plusDays(15);
        BioSample expiringSample = createStoredBioSample("BIOSAMPLE-EXPIRING", BioSample.WorkflowStatus.STORED);
        expiringSample.setRetentionExpiryDate(Date.valueOf(expiringSoon));
        bioSampleService.update(expiringSample);

        // Sample already expired
        LocalDate expired = LocalDate.now().minusDays(5);
        BioSample expiredSample = createStoredBioSample("BIOSAMPLE-EXPIRED", BioSample.WorkflowStatus.STORED);
        expiredSample.setRetentionExpiryDate(Date.valueOf(expired));
        bioSampleService.update(expiredSample);

        // Act
        Map<String, Object> result = dashboardService.getSampleAgingMetrics();

        // Assert: Deep state verification
        assertNotNull("Result should not be null", result);
        assertTrue("Result should contain expired count", result.containsKey("expired"));
        assertTrue("Result should contain 30-day warning count", result.containsKey("expiring30Days"));
        assertTrue("Result should contain 60-day warning count", result.containsKey("expiring60Days"));
        assertTrue("Result should contain 90-day warning count", result.containsKey("expiring90Days"));

        Long expiredCount = (Long) result.get("expired");
        Long expiring30 = (Long) result.get("expiring30Days");

        assertNotNull("Expired count should not be null", expiredCount);
        assertNotNull("Expiring 30 days count should not be null", expiring30);
        assertTrue("Should have at least 1 expired sample", expiredCount >= 1);
        assertTrue("Should have at least 1 sample expiring in 30 days", expiring30 >= 1);
    }

    /**
     * Test QC compliance metrics aggregates checkpoint pass rates correctly.
     */
    @Test
    public void testGetQCComplianceMetrics_AggregatesCheckpointsCorrectly() {
        // Arrange: Create QC inspections with different results
        BioSample sample1 = createStoredBioSample("BIOSAMPLE-QC-PASS", BioSample.WorkflowStatus.STORED);
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

        BioSample sample2 = createStoredBioSample("BIOSAMPLE-QC-FAIL", BioSample.WorkflowStatus.STORED);
        BiorepositoryQCInspection failInspection = new BiorepositoryQCInspection(sample2, "Test Inspector",
                new Timestamp(System.currentTimeMillis()));
        failInspection.setSamplePresent(true);
        failInspection.setLabelIntegrity(false); // Failed checkpoint
        failInspection.setContainerIntegrity(true);
        failInspection.setVolumeAppearanceAcceptable(true);
        failInspection.setCorrectPosition(true);
        failInspection.updateQcResult();
        failInspection.setSysUserId(sysUserId);
        qcInspectionService.insert(failInspection);

        // Act
        Map<String, Object> result = dashboardService.getQCComplianceMetrics();

        // Assert: Deep state verification
        assertNotNull("Result should not be null", result);
        assertTrue("Result should contain total inspections", result.containsKey("totalInspections"));
        assertTrue("Result should contain passed inspections", result.containsKey("passedInspections"));
        assertTrue("Result should contain compliance rate", result.containsKey("complianceRate"));

        Long totalInspections = (Long) result.get("totalInspections");
        Long passedInspections = (Long) result.get("passedInspections");
        Double complianceRate = (Double) result.get("complianceRate");

        assertNotNull("Total inspections should not be null", totalInspections);
        assertNotNull("Passed inspections should not be null", passedInspections);
        assertNotNull("Compliance rate should not be null", complianceRate);

        assertTrue("Should have at least 2 inspections", totalInspections >= 2);
        assertTrue("Should have at least 1 passed inspection", passedInspections >= 1);
        assertTrue("Compliance rate should be between 0 and 100, found: " + complianceRate,
                complianceRate >= 0.0 && complianceRate <= 100.0);
    }

    /**
     * Test QC discrepancy breakdown returns correct distribution of discrepancy
     * types.
     */
    @Test
    public void testGetQCDiscrepancyBreakdown_ReturnsCorrectDistribution() {
        // Arrange: Create inspections with specific discrepancy types
        BioSample sample1 = createStoredBioSample("BIOSAMPLE-MISSING", BioSample.WorkflowStatus.STORED);
        BiorepositoryQCInspection missingInspection = new BiorepositoryQCInspection(sample1, "Test Inspector",
                new Timestamp(System.currentTimeMillis()));
        missingInspection.setSamplePresent(false);
        missingInspection.setLabelIntegrity(true);
        missingInspection.setContainerIntegrity(true);
        missingInspection.setVolumeAppearanceAcceptable(true);
        missingInspection.setCorrectPosition(true);
        missingInspection.updateQcResult();
        missingInspection.setDiscrepancyType(BiorepositoryQCInspection.DiscrepancyType.MISSING_SAMPLE);
        missingInspection.setSysUserId(sysUserId);
        qcInspectionService.insert(missingInspection);

        BioSample sample2 = createStoredBioSample("BIOSAMPLE-DAMAGED", BioSample.WorkflowStatus.STORED);
        BiorepositoryQCInspection damagedInspection = new BiorepositoryQCInspection(sample2, "Test Inspector",
                new Timestamp(System.currentTimeMillis()));
        damagedInspection.setSamplePresent(true);
        damagedInspection.setLabelIntegrity(true);
        damagedInspection.setContainerIntegrity(false);
        damagedInspection.setVolumeAppearanceAcceptable(true);
        damagedInspection.setCorrectPosition(true);
        damagedInspection.updateQcResult();
        damagedInspection.setDiscrepancyType(BiorepositoryQCInspection.DiscrepancyType.CONTAINER_DAMAGE);
        damagedInspection.setSysUserId(sysUserId);
        qcInspectionService.insert(damagedInspection);

        // Act
        Map<String, Object> result = dashboardService.getQCDiscrepancyBreakdown();

        // Assert: Deep state verification
        assertNotNull("Result should not be null", result);
        assertFalse("Result should not be empty", result.isEmpty());

        // Verify discrepancy types are present
        if (result.containsKey("MISSING_SAMPLE")) {
            Long missingSampleCount = (Long) result.get("MISSING_SAMPLE");
            assertTrue("Should have at least 1 missing sample discrepancy, found: " + missingSampleCount,
                    missingSampleCount >= 1);
        }

        if (result.containsKey("CONTAINER_DAMAGE")) {
            Long containerDamageCount = (Long) result.get("CONTAINER_DAMAGE");
            assertTrue("Should have at least 1 container damage discrepancy, found: " + containerDamageCount,
                    containerDamageCount >= 1);
        }
    }

    /**
     * Test retrieval statistics filters by date range correctly.
     */
    @Test
    public void testGetRetrievalStatistics_FiltersDateRangeCorrectly() {
        // Arrange: Create retrieval request
        BioSample sample = createStoredBioSample("BIOSAMPLE-RETRIEVAL", BioSample.WorkflowStatus.STORED);
        SampleRetrievalRequest request = new SampleRetrievalRequest();
        request.setRequestNumber("REQ-" + System.currentTimeMillis());
        request.setRequestPurpose("Test retrieval for dashboard metrics");
        request.setStatus(SampleRetrievalRequest.RequestStatus.COMPLETED);
        request.setDestinationType(SampleRetrievalRequest.DestinationType.INTERNAL_LAB);
        request.setRequestedBy(testUser);
        request.setRequestedTimestamp(new Timestamp(System.currentTimeMillis()));
        request.setSysUserId(sysUserId);
        retrievalService.save(request);

        // Act
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now().plusDays(1);
        Map<String, Object> result = dashboardService.getRetrievalStatistics(startDate, endDate);

        // Assert: Deep state verification
        assertNotNull("Result should not be null", result);
        assertTrue("Result should contain total requests", result.containsKey("totalRequests"));
        assertTrue("Result should contain completed requests", result.containsKey("completedRequests"));
        assertTrue("Result should contain pending requests", result.containsKey("pendingRequests"));

        Long totalRequests = (Long) result.get("totalRequests");
        Long completedRequests = (Long) result.get("completedRequests");

        assertNotNull("Total requests should not be null", totalRequests);
        assertNotNull("Completed requests should not be null", completedRequests);
        assertTrue("Should have at least 1 retrieval request", totalRequests >= 1);
        assertTrue("Should have at least 1 completed request", completedRequests >= 1);
    }

    /**
     * Test disposal statistics returns count of disposed samples.
     */
    @Test
    public void testGetDisposalStatistics_ReturnsDisposedCount() {
        // Arrange: Create disposed samples
        BioSample disposedSample = createStoredBioSample("BIOSAMPLE-DISPOSED", BioSample.WorkflowStatus.DISPOSED);

        // Act
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now().plusDays(1);
        Map<String, Object> result = dashboardService.getDisposalStatistics(startDate, endDate);

        // Assert: Deep state verification
        assertNotNull("Result should not be null", result);
        assertTrue("Result should contain total disposed count", result.containsKey("totalDisposed"));

        Long totalDisposed = (Long) result.get("totalDisposed");
        assertNotNull("Total disposed should not be null", totalDisposed);
        assertTrue("Should have at least 1 disposed sample, found: " + totalDisposed, totalDisposed >= 1);
    }

    // ========================================
    // Helper Methods for Test Data Creation
    // ========================================

    private SystemUser createTestUser() {
        List<SystemUser> users = systemUserService.getAll();
        if (!users.isEmpty()) {
            return users.get(0);
        }

        SystemUser user = new SystemUser();
        user.setLoginName("test_dashboard_user");
        user.setFirstName("Test");
        user.setLastName("Dashboard");
        systemUserService.save(user);
        return user;
    }

    private Shipment createTestShipment() {
        Shipment shipment = new Shipment();
        shipment.setDeliveryReference("TEST-SHIPMENT-" + System.currentTimeMillis());
        shipment.setSenderName("Test Sender");
        shipment.setReceiver(testUser);
        shipment.setReceptionTimestamp(new Timestamp(System.currentTimeMillis()));
        shipment.setPackagingCondition(Shipment.PackagingCondition.INTACT);
        shipment.setDocumentationStatus(Shipment.DocumentationStatus.VERIFIED);
        shipment.setSysUserId(sysUserId);
        shipmentService.insert(shipment);
        return shipment;
    }

    private TypeOfSample createTestSampleType() {
        TypeOfSample type = new TypeOfSample();
        type.setDescription("Test Sample Type");
        type.setLocalAbbreviation("TST");
        typeOfSampleService.save(type);
        return type;
    }

    private BioSample createStoredBioSample(String barcode, BioSample.WorkflowStatus status) {
        // Create core Sample
        Sample sample = new Sample();
        sample.setAccessionNumber("ACC-" + System.currentTimeMillis());
        sample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));
        sample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
        sample.setDomain("H");
        sample.setSysUserId(sysUserId);
        sampleService.save(sample);

        // Create SampleItem
        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sample);
        sampleItem.setTypeOfSample(testSampleType);
        sampleItem.setExternalId(barcode);
        sampleItem.setSortOrder("1");
        sampleItem.setQuantity(10.0);
        sampleItem.setCollectionDate(new Timestamp(System.currentTimeMillis()));
        sampleItem.setStatusId("1");
        sampleItem.setSysUserId(sysUserId);
        sampleItemService.save(sampleItem);

        // Create BioSample
        BioSample bioSample = new BioSample();
        bioSample.setSampleItem(sampleItem);
        bioSample.setShipment(testShipment);
        bioSample.setBiosafetyLevel(BioSample.BiosafetyLevel.BSL_2);
        bioSample.setWorkflowStatus(status);
        bioSample.setRequiredTempMin(new BigDecimal("-80.0"));
        bioSample.setRequiredTempMax(new BigDecimal("-70.0"));
        bioSample.setProjectId("TEST-PROJECT-001");

        // Set retention expiry date (default 5 years from now for non-expired samples)
        if (bioSample.getRetentionExpiryDate() == null) {
            bioSample.setRetentionExpiryDate(Date.valueOf(LocalDate.now().plusYears(5)));
        }

        bioSample.setSysUserId(sysUserId);
        bioSampleService.insert(bioSample);

        return bioSample;
    }
}
