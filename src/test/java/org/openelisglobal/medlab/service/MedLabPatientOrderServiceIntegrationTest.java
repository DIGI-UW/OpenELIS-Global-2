package org.openelisglobal.medlab.service;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

/**
 * Integration tests for MedLabPatientOrderService. Tests the order-driven
 * workflow where: 1. createPatientOrder() creates ElectronicOrder (not Sample)
 * 2. recordSampleCollection() creates Sample and links it to the order
 *
 * <p>
 * These tests verify the fix for the order-to-sample deviation bug where
 * createPatientOrder was directly creating samples instead of orders.
 *
 * <p>
 * TODO: Requires proper test data setup with Liquibase seed data.
 */
@Rollback
public class MedLabPatientOrderServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MedLabPatientOrderService medLabPatientOrderService;

    @Autowired
    private ElectronicOrderService electronicOrderService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private OrderSampleLinkService orderSampleLinkService;

    @Autowired
    private org.openelisglobal.notebook.service.NotebookEntryService notebookEntryService;

    @Autowired
    private org.openelisglobal.notebook.service.NoteBookPageService noteBookPageService;

    @Autowired
    private org.openelisglobal.notebook.service.NotebookPageSampleService notebookPageSampleService;

    @Autowired
    private org.openelisglobal.sampleitem.service.SampleItemService sampleItemService;

    @Autowired
    private org.openelisglobal.analysis.service.AnalysisService analysisService;

    // Test data IDs from medlab-patient-order-test-data.xml
    private static final String TEST_PATIENT_ID = "8001";
    private static final String TEST_PATIENT_ID_2 = "8002";
    private static final String TEST_PATIENT_ID_3 = "8003";
    private static final String TEST_TEST_ID_1 = "8001";
    private static final String TEST_TEST_ID_2 = "8002";
    private static final Integer TEST_NOTEBOOK_ENTRY_ID = 8001;
    private static final Integer TEST_QC_PAGE_ID = 8003;
    private static final Integer TEST_ROUTING_PAGE_ID = 8004;
    // Use null for notebook page to avoid nested transaction issues
    private static final Integer TEST_NOTEBOOK_PAGE_ID = null;
    private static final String TEST_USER_ID = "1";

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/status-of-sample.xml");
        executeDataSetWithStateManagement("testdata/medlab-patient-order-test-data.xml");
    }

    /**
     * Helper method to pass a sample through QC and route it to INTERNAL_ANALYSIS.
     * This is required for samples to appear in getSamplesForProcessing().
     *
     * Manually creates NotebookPageSample records to simulate QC + routing
     * workflow.
     */
    private void passQCAndRouteToProcessing(String labNo, Integer sampleItemId, Integer notebookEntryId) {
        // Check for null parameters
        if (notebookEntryId == null) {
            throw new RuntimeException(
                    "notebookEntryId is null for sample: " + labNo + ". recordSampleCollection may have failed.");
        }
        if (sampleItemId == null) {
            throw new RuntimeException(
                    "sampleItemId is null for sample: " + labNo + ". recordSampleCollection may have failed.");
        }

        // Get notebook and its pages
        org.openelisglobal.notebook.valueholder.NotebookEntry entry = notebookEntryService.get(notebookEntryId);
        if (entry == null || entry.getNotebook() == null) {
            throw new RuntimeException("No notebook entry found for ID: " + notebookEntryId);
        }

        // Get notebook pages
        List<org.openelisglobal.notebook.valueholder.NoteBookPage> pages = noteBookPageService
                .getByNotebookId(entry.getNotebook().getId());

        org.openelisglobal.notebook.valueholder.NoteBookPage qcPage = null;
        org.openelisglobal.notebook.valueholder.NoteBookPage routingPage = null;

        for (org.openelisglobal.notebook.valueholder.NoteBookPage page : pages) {
            if ("medlab-quality-check".equals(page.getPageId()) || "quality-check".equals(page.getPageId())) {
                qcPage = page;
            } else if ("medlab-sample-routing".equals(page.getPageId()) || "sample-routing".equals(page.getPageId())) {
                routingPage = page;
            }
        }

        // Step 1: Create QC page sample with COMPLETED status (QC passed)
        if (qcPage != null) {
            notebookPageSampleService.createPageSampleForPage(qcPage.getId(), sampleItemId,
                    org.openelisglobal.notebook.valueholder.NotebookPageSample.Status.COMPLETED);
        }

        // Step 2: Create routing page sample with INTERNAL_ANALYSIS destination
        if (routingPage != null) {
            // Create the routing page sample using the service method (manages entity
            // properly)
            notebookPageSampleService.createPageSampleForPage(routingPage.getId(), sampleItemId,
                    org.openelisglobal.notebook.valueholder.NotebookPageSample.Status.COMPLETED);

            // Retrieve the created record and update its data field
            org.openelisglobal.notebook.valueholder.NotebookPageSample routingSample = notebookPageSampleService
                    .getByPageIdAndSampleItemId(routingPage.getId(), sampleItemId);

            if (routingSample != null) {
                // Set routing data
                java.util.Map<String, Object> routingData = new java.util.HashMap<>();
                routingData.put("destinationType", "INTERNAL_ANALYSIS");
                routingSample.setData(routingData);

                // Update the record
                notebookPageSampleService.update(routingSample);
            }
        }
    }

    /**
     * Helper method to complete processing for a sample item. Creates
     * NotebookPageSample record with COMPLETED status on processing page.
     */
    private void completeProcessing(Integer sampleItemId, Integer notebookEntryId) {
        org.openelisglobal.notebook.valueholder.NotebookEntry entry = notebookEntryService.get(notebookEntryId);
        if (entry == null || entry.getNotebook() == null) {
            throw new RuntimeException("No notebook entry found for ID: " + notebookEntryId);
        }

        List<org.openelisglobal.notebook.valueholder.NoteBookPage> pages = noteBookPageService
                .getByNotebookId(entry.getNotebook().getId());

        for (org.openelisglobal.notebook.valueholder.NoteBookPage page : pages) {
            if ("medlab-sample-processing".equals(page.getPageId()) || "sample-processing".equals(page.getPageId())) {
                notebookPageSampleService.createPageSampleForPage(page.getId(), sampleItemId,
                        org.openelisglobal.notebook.valueholder.NotebookPageSample.Status.COMPLETED);
                return;
            }
        }
    }

    /**
     * Helper method to complete testing for a sample item. Creates
     * NotebookPageSample record with COMPLETED status on Testing & Analyzer page.
     */
    private void completeTesting(Integer sampleItemId, Integer notebookEntryId) {
        org.openelisglobal.notebook.valueholder.NotebookEntry entry = notebookEntryService.get(notebookEntryId);
        if (entry == null || entry.getNotebook() == null) {
            throw new RuntimeException("No notebook entry found for ID: " + notebookEntryId);
        }

        List<org.openelisglobal.notebook.valueholder.NoteBookPage> pages = noteBookPageService
                .getByNotebookId(entry.getNotebook().getId());

        for (org.openelisglobal.notebook.valueholder.NoteBookPage page : pages) {
            if ("medlab-testing-analyzer".equals(page.getPageId()) || "testing-analyzer".equals(page.getPageId())) {
                notebookPageSampleService.createPageSampleForPage(page.getId(), sampleItemId,
                        org.openelisglobal.notebook.valueholder.NotebookPageSample.Status.COMPLETED);
                return;
            }
        }
    }

    // ==================== createPatientOrder Tests ====================

    @Test
    public void testCreatePatientOrder_CreatesElectronicOrder() {
        // Given
        String labNo = "TEST-ORDER-001";
        List<String> testIds = List.of(TEST_TEST_ID_1);

        // When
        Map<String, Object> result = medLabPatientOrderService.createPatientOrder(TEST_PATIENT_ID, labNo, "2026-01-09",
                "2026-01-09", "ROUTINE", testIds, null, TEST_NOTEBOOK_PAGE_ID, TEST_USER_ID);

        // Then
        assertTrue("Order creation should succeed", (Boolean) result.get("success"));
        assertNotNull("orderId should be returned", result.get("orderId"));
        assertEquals("PENDING_COLLECTION", result.get("status"));

        // Verify ElectronicOrder was created
        List<ElectronicOrder> orders = electronicOrderService.getElectronicOrdersByExternalId(labNo);
        assertFalse("ElectronicOrder should exist", orders.isEmpty());
        assertEquals(labNo, orders.get(0).getExternalId());
    }

    @Test
    public void testCreatePatientOrder_DoesNotCreateSample() {
        // Given
        String labNo = "TEST-ORDER-002";
        List<String> testIds = List.of(TEST_TEST_ID_1);

        // When
        Map<String, Object> result = medLabPatientOrderService.createPatientOrder(TEST_PATIENT_ID, labNo, "2026-01-09",
                "2026-01-09", "ROUTINE", testIds, null, TEST_NOTEBOOK_PAGE_ID, TEST_USER_ID);

        // Then
        assertTrue("Order creation should succeed", (Boolean) result.get("success"));

        // Verify Sample was NOT created
        Sample sample = sampleService.getSampleByAccessionNumber(labNo);
        assertNull("Sample should NOT exist after createPatientOrder", sample);
    }

    @Test
    public void testCreatePatientOrder_LinksToPatient() {
        // Given
        String labNo = "TEST-ORDER-003";
        List<String> testIds = List.of(TEST_TEST_ID_1);

        // When
        Map<String, Object> result = medLabPatientOrderService.createPatientOrder(TEST_PATIENT_ID, labNo, "2026-01-09",
                "2026-01-09", "ROUTINE", testIds, null, TEST_NOTEBOOK_PAGE_ID, TEST_USER_ID);

        // Then
        assertTrue("Order creation should succeed", (Boolean) result.get("success"));

        // Verify order is linked to patient
        List<ElectronicOrder> orders = electronicOrderService.getElectronicOrdersByExternalId(labNo);
        assertFalse(orders.isEmpty());
        assertNotNull("Order should have patient", orders.get(0).getPatient());
        assertEquals(TEST_PATIENT_ID, orders.get(0).getPatient().getId());
    }

    @Test
    public void testCreatePatientOrder_StoresTestRequirements() {
        // Given
        String labNo = "TEST-ORDER-004";
        List<String> testIds = List.of(TEST_TEST_ID_1, TEST_TEST_ID_2);

        // When
        Map<String, Object> result = medLabPatientOrderService.createPatientOrder(TEST_PATIENT_ID, labNo, "2026-01-09",
                "2026-01-09", "ROUTINE", testIds, null, TEST_NOTEBOOK_PAGE_ID, TEST_USER_ID);

        // Then
        assertTrue("Order creation should succeed", (Boolean) result.get("success"));

        // Verify test IDs are stored in order data
        List<ElectronicOrder> orders = electronicOrderService.getElectronicOrdersByExternalId(labNo);
        assertFalse(orders.isEmpty());
        String data = orders.get(0).getData();
        assertNotNull("Order data should not be null", data);
        assertTrue("Order data should contain testIds", data.contains("testIds"));
        assertTrue("Order data should contain test ID 1", data.contains(TEST_TEST_ID_1));
        assertTrue("Order data should contain test ID 2", data.contains(TEST_TEST_ID_2));
    }

    @Test
    public void testCreatePatientOrder_DuplicateLabNo_Fails() {
        // Given - EXISTING-ORDER-001 is already in test data
        String existingLabNo = "EXISTING-ORDER-001";
        List<String> testIds = List.of(TEST_TEST_ID_1);

        // When
        Map<String, Object> result = medLabPatientOrderService.createPatientOrder(TEST_PATIENT_ID, existingLabNo,
                "2026-01-09", "2026-01-09", "ROUTINE", testIds, null, TEST_NOTEBOOK_PAGE_ID, TEST_USER_ID);

        // Then
        assertFalse("Order creation should fail for duplicate labNo", (Boolean) result.get("success"));
        assertNotNull("Error message should be present", result.get("error"));
    }

    @Test
    public void testCreatePatientOrder_InvalidPatient_Fails() {
        // Given
        String labNo = "TEST-ORDER-005";
        String invalidPatientId = "99999";
        List<String> testIds = List.of(TEST_TEST_ID_1);

        // When - This may throw a transaction exception if the patient lookup fails
        // in a nested transaction
        try {
            Map<String, Object> result = medLabPatientOrderService.createPatientOrder(invalidPatientId, labNo,
                    "2026-01-09", "2026-01-09", "ROUTINE", testIds, null, TEST_NOTEBOOK_PAGE_ID, TEST_USER_ID);

            // Then - If we get a result, it should indicate failure
            assertFalse("Order creation should fail for invalid patient", (Boolean) result.get("success"));
        } catch (Exception e) {
            // Expected - invalid patient ID causes transaction to fail
            assertTrue("Exception should be related to patient or transaction",
                    e.getMessage() != null || e.getCause() != null);
        }
    }

    // ==================== recordSampleCollection Tests ====================

    @Test
    public void testRecordSampleCollection_CreatesSampleFromOrder() {
        // Given - First create an order
        String labNo = "TEST-COLLECT-001";
        List<String> testIds = List.of(TEST_TEST_ID_1);
        medLabPatientOrderService.createPatientOrder(TEST_PATIENT_ID, labNo, "2026-01-09", "2026-01-09", "ROUTINE",
                testIds, null, TEST_NOTEBOOK_PAGE_ID, TEST_USER_ID);

        // When - Record sample collection
        Map<String, Object> result = medLabPatientOrderService.recordSampleCollection(labNo, "8001", "tube", "10:30",
                "2026-01-09", "2", "5.0", "Collection notes", TEST_NOTEBOOK_PAGE_ID, TEST_USER_ID);

        // Then
        assertTrue("Sample collection should succeed: " + result.get("error"), (Boolean) result.get("success"));
        assertNotNull("sampleId should be returned", result.get("sampleId"));

        // Verify Sample was created
        Sample sample = sampleService.getSampleByAccessionNumber(labNo);
        assertNotNull("Sample should exist after recordSampleCollection", sample);
        assertEquals(labNo, sample.getAccessionNumber());
    }

    @Test
    public void testRecordSampleCollection_CreatesOrderSampleLink() {
        // Given - First create an order
        String labNo = "TEST-COLLECT-002";
        List<String> testIds = List.of(TEST_TEST_ID_1);
        Map<String, Object> orderResult = medLabPatientOrderService.createPatientOrder(TEST_PATIENT_ID, labNo,
                "2026-01-09", "2026-01-09", "ROUTINE", testIds, null, TEST_NOTEBOOK_PAGE_ID, TEST_USER_ID);

        // When - Record sample collection
        Map<String, Object> collectResult = medLabPatientOrderService.recordSampleCollection(labNo, "8001", "tube",
                "10:30", "2026-01-09", "2", "5.0", null, TEST_NOTEBOOK_PAGE_ID, TEST_USER_ID);

        // Then
        assertTrue("Sample collection should succeed", (Boolean) collectResult.get("success"));

        // Verify OrderSampleLink was created
        Sample sample = sampleService.getSampleByAccessionNumber(labNo);
        assertNotNull(sample);
        boolean hasOrder = orderSampleLinkService.hasOrderForSample(Integer.parseInt(sample.getId()));
        assertTrue("Sample should have order link", hasOrder);
    }

    @Test
    public void testRecordSampleCollection_NoOrder_Fails() {
        // Given - No order exists for this labNo
        String labNo = "NONEXISTENT-ORDER-001";

        // When
        Map<String, Object> result = medLabPatientOrderService.recordSampleCollection(labNo, "8001", "tube", "10:30",
                "2026-01-09", "2", "5.0", null, TEST_NOTEBOOK_PAGE_ID, TEST_USER_ID);

        // Then
        assertFalse("Sample collection should fail without order", (Boolean) result.get("success"));
        assertTrue("Error should mention order", result.get("error").toString().toLowerCase().contains("order"));
    }

    @Test
    public void testRecordSampleCollection_LinksSampleToPatient() {
        // Given - First create an order
        String labNo = "TEST-COLLECT-003";
        List<String> testIds = List.of(TEST_TEST_ID_1);
        medLabPatientOrderService.createPatientOrder(TEST_PATIENT_ID, labNo, "2026-01-09", "2026-01-09", "ROUTINE",
                testIds, null, TEST_NOTEBOOK_PAGE_ID, TEST_USER_ID);

        // When - Record sample collection
        Map<String, Object> result = medLabPatientOrderService.recordSampleCollection(labNo, "8001", "tube", "10:30",
                "2026-01-09", "2", "5.0", null, TEST_NOTEBOOK_PAGE_ID, TEST_USER_ID);

        // Then
        assertTrue("Sample collection should succeed", (Boolean) result.get("success"));
        assertEquals(TEST_PATIENT_ID, result.get("patientId").toString());
    }

    // ==================== Bulk Order Tests ====================

    @Test
    public void testBulkPatientOrders_CreatesMultipleOrders() {
        // Given
        List<Map<String, Object>> patients = new ArrayList<>();

        Map<String, Object> patient1 = new HashMap<>();
        patient1.put("patientId", TEST_PATIENT_ID_2);
        patient1.put("firstName", "Patient2");
        patient1.put("lastName", "OrderTest");
        patients.add(patient1);

        Map<String, Object> patient2 = new HashMap<>();
        patient2.put("patientId", TEST_PATIENT_ID_3);
        patient2.put("firstName", "Patient3");
        patient2.put("lastName", "BulkTest");
        patients.add(patient2);

        List<String> testIds = List.of(TEST_TEST_ID_1);
        String prefix = "BULK-TEST-";

        // When
        Map<String, Object> result = medLabPatientOrderService.createBulkPatientOrders(patients, prefix, testIds,
                "ROUTINE", null, TEST_NOTEBOOK_PAGE_ID, TEST_USER_ID);

        // Then
        assertTrue("Bulk order creation should succeed", (Boolean) result.get("success"));
        assertEquals(2, result.get("createdCount"));

        // Verify orders were created, not samples
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> createdOrders = (List<Map<String, Object>>) result.get("orders");
        assertEquals(2, createdOrders.size());

        for (Map<String, Object> orderInfo : createdOrders) {
            String labNo = (String) orderInfo.get("labNumber");
            assertNotNull("orderId should be present", orderInfo.get("orderId"));
            assertEquals("PENDING_COLLECTION", orderInfo.get("status"));

            // Verify ElectronicOrder exists
            List<ElectronicOrder> orders = electronicOrderService.getElectronicOrdersByExternalId(labNo);
            assertFalse("ElectronicOrder should exist for " + labNo, orders.isEmpty());

            // Verify Sample does NOT exist yet
            Sample sample = sampleService.getSampleByAccessionNumber(labNo);
            assertNull("Sample should NOT exist for " + labNo + " until collection", sample);
        }
    }

    // ==================== End-to-End Workflow Test ====================

    @Test
    public void testFullWorkflow_OrderThenCollect() {
        // Given
        String labNo = "WORKFLOW-001";
        List<String> testIds = List.of(TEST_TEST_ID_1, TEST_TEST_ID_2);

        // Step 1: Create order
        Map<String, Object> orderResult = medLabPatientOrderService.createPatientOrder(TEST_PATIENT_ID, labNo,
                "2026-01-09", "2026-01-09", "ROUTINE", testIds, null, TEST_NOTEBOOK_PAGE_ID, TEST_USER_ID);

        assertTrue("Order creation should succeed", (Boolean) orderResult.get("success"));
        String orderId = orderResult.get("orderId").toString();
        assertNotNull(orderId);

        // Verify: Order exists, Sample doesn't
        List<ElectronicOrder> orders = electronicOrderService.getElectronicOrdersByExternalId(labNo);
        assertEquals(1, orders.size());
        assertNull(sampleService.getSampleByAccessionNumber(labNo));

        // Step 2: Collect sample
        Map<String, Object> collectResult = medLabPatientOrderService.recordSampleCollection(labNo, "8001", "tube",
                "14:30", "2026-01-09", "2", "5.0", "Collected at clinic", TEST_NOTEBOOK_PAGE_ID, TEST_USER_ID);

        assertTrue("Sample collection should succeed", (Boolean) collectResult.get("success"));
        String sampleId = collectResult.get("sampleId").toString();
        assertNotNull(sampleId);

        // Verify: Sample now exists and is linked to order
        Sample sample = sampleService.getSampleByAccessionNumber(labNo);
        assertNotNull("Sample should exist", sample);
        assertEquals(labNo, sample.getAccessionNumber());

        // Verify: OrderSampleLink exists
        boolean hasLink = orderSampleLinkService.hasOrderForSample(Integer.parseInt(sample.getId()));
        assertTrue("OrderSampleLink should exist", hasLink);

        // Verify: Correct test count
        assertEquals(2, collectResult.get("testCount"));
    }

    // ==================== getSamplesForProcessing Hierarchy Tests
    // ====================

    @Test
    public void testGetSamplesForProcessing_ReturnsParentSamplesOnly_WhenNoChildren() {
        // Given - Create order, collect sample, pass QC + route to processing (no
        // aliquots created)
        String labNo = "TEST-PROCESSING-001";
        List<String> testIds = List.of(TEST_TEST_ID_1);

        // Create order with TEST_NOTEBOOK_ENTRY_ID and collection page ID
        medLabPatientOrderService.createPatientOrder(TEST_PATIENT_ID, labNo, "2026-01-09", "2026-01-09", "ROUTINE",
                testIds, TEST_NOTEBOOK_ENTRY_ID, 8002, TEST_USER_ID);

        // Record sample collection - returns sampleId
        Map<String, Object> collectResult = medLabPatientOrderService.recordSampleCollection(labNo, "8001", "tube",
                "10:30", "2026-01-09", "2", "5.0", null, 8002, TEST_USER_ID);

        // Get sampleItemId from the created sample
        String sampleId = collectResult.get("sampleId").toString();
        assertNotNull("Sample ID should be returned", sampleId);
        Sample createdSample = sampleService.get(sampleId);
        assertNotNull("Sample should exist", createdSample);
        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(createdSample.getId());
        assertFalse("Should have sample items", sampleItems.isEmpty());
        Integer sampleItemId = Integer.parseInt(sampleItems.get(0).getId());

        // Pass QC and route to INTERNAL_ANALYSIS (required for getSamplesForProcessing)
        passQCAndRouteToProcessing(labNo, sampleItemId, TEST_NOTEBOOK_ENTRY_ID);

        // When - Get samples for processing
        List<Map<String, Object>> samples = medLabPatientOrderService.getSamplesForProcessing(TEST_NOTEBOOK_ENTRY_ID);

        // Then
        assertFalse("Should return samples", samples.isEmpty());

        // Verify first sample has correct hierarchy fields (parent, no children)
        Map<String, Object> sample = samples.get(0);
        assertEquals(labNo, sample.get("labNo"));
        assertFalse("Should not have children initially", (Boolean) sample.get("hasChildren"));
        assertEquals(0, sample.get("childAliquotCount"));
        // Note: externalId is null for parent samples created via
        // recordSampleCollection - that's OK
    }

    @Test
    public void testGetSamplesForProcessing_IncludesChildSamples_WithHierarchyInfo() {
        // Given - Create order, collect sample, pass QC + route, create aliquots
        String labNo = "TEST-PROCESSING-002";
        List<String> testIds = List.of(TEST_TEST_ID_1);

        medLabPatientOrderService.createPatientOrder(TEST_PATIENT_ID, labNo, "2026-01-09", "2026-01-09", "ROUTINE",
                testIds, TEST_NOTEBOOK_ENTRY_ID, 8002, TEST_USER_ID);

        Map<String, Object> collectResult = medLabPatientOrderService.recordSampleCollection(labNo, "8001", "tube",
                "10:30", "2026-01-09", "2", "5.0", null, 8002, TEST_USER_ID);

        String sampleId = collectResult.get("sampleId").toString();
        Sample createdSample = sampleService.get(sampleId);
        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(createdSample.getId());
        Integer sampleItemId = Integer.parseInt(sampleItems.get(0).getId());

        // Pass QC and route to INTERNAL_ANALYSIS
        passQCAndRouteToProcessing(labNo, sampleItemId, TEST_NOTEBOOK_ENTRY_ID);

        // Create 2 aliquots for the parent sample
        medLabPatientOrderService.createAliquots(List.of(sampleItemId), 2, "ALQ-TEST", "cryovial", null, TEST_USER_ID);

        // When - Get samples for processing
        List<Map<String, Object>> samples = medLabPatientOrderService.getSamplesForProcessing(TEST_NOTEBOOK_ENTRY_ID);

        // Then - Should return parent + 2 children = 3 samples total
        assertEquals("Should return parent plus 2 children", 3, samples.size());

        // Find parent and child samples
        List<Map<String, Object>> parentSamples = samples.stream().filter(s -> !Boolean.TRUE.equals(s.get("isAliquot")))
                .toList();
        List<Map<String, Object>> childSamples = samples.stream().filter(s -> Boolean.TRUE.equals(s.get("isAliquot")))
                .toList();

        assertEquals("Should have 1 parent sample", 1, parentSamples.size());
        assertEquals("Should have 2 child samples", 2, childSamples.size());

        // Verify parent sample
        Map<String, Object> parent = parentSamples.get(0);
        assertEquals(labNo, parent.get("labNo"));
        assertTrue("Parent should have children", (Boolean) parent.get("hasChildren"));
        assertEquals(2, parent.get("childAliquotCount"));
        // Note: parent externalId is null for samples created via
        // recordSampleCollection - that's OK

        // Verify child samples have correct hierarchy information
        for (Map<String, Object> child : childSamples) {
            // Hierarchy fields
            assertTrue("Child isAliquot should be true", (Boolean) child.get("isAliquot"));
            assertEquals("Child nestingLevel should be 1", 1, child.get("nestingLevel"));
            assertEquals("Child parentSampleItemId should match parent", String.valueOf(sampleItemId),
                    child.get("parentSampleItemId"));
            // parentExternalId will be null since parent has no externalId - that's OK

            // Child-specific fields
            assertFalse("Child should not have children (1-level hierarchy)", (Boolean) child.get("hasChildren"));
            assertEquals("Child childAliquotCount should be 0", 0, child.get("childAliquotCount"));

            // Should have unique accession number (Lab No)
            assertNotNull("Child should have labNo", child.get("labNo"));
            assertNotNull("Child should have accessionNumber", child.get("accessionNumber"));

            // Should have externalId
            assertNotNull("Child should have externalId", child.get("externalId"));
            String childExternalId = (String) child.get("externalId");
            assertTrue("Child externalId should contain prefix", childExternalId.startsWith("ALQ-TEST"));

            // Patient info copied from parent
            assertEquals("Child should have parent's patient name", parent.get("patientName"),
                    child.get("patientName"));
            assertEquals("Child should have parent's patient ID", parent.get("patientId"), child.get("patientId"));

            // Note: containerType is null because aliquots were created without
            // notebookPageId
        }
    }

    @Test
    public void testGetSamplesForProcessing_ChildrenHaveUniqueLabNumbers() {
        // Given - Create order, collect sample, pass QC + route, create aliquots
        String labNo = "TEST-PROCESSING-003";
        List<String> testIds = List.of(TEST_TEST_ID_1);

        medLabPatientOrderService.createPatientOrder(TEST_PATIENT_ID, labNo, "2026-01-09", "2026-01-09", "ROUTINE",
                testIds, TEST_NOTEBOOK_ENTRY_ID, 8002, TEST_USER_ID);

        Map<String, Object> collectResult = medLabPatientOrderService.recordSampleCollection(labNo, "8001", "tube",
                "10:30", "2026-01-09", "2", "5.0", null, 8002, TEST_USER_ID);

        String sampleId = collectResult.get("sampleId").toString();
        Sample createdSample = sampleService.get(sampleId);
        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(createdSample.getId());
        Integer sampleItemId = Integer.parseInt(sampleItems.get(0).getId());

        // Pass QC and route to INTERNAL_ANALYSIS
        passQCAndRouteToProcessing(labNo, sampleItemId, TEST_NOTEBOOK_ENTRY_ID);

        // Create 3 aliquots
        medLabPatientOrderService.createAliquots(List.of(sampleItemId), 3, "UNIQUE-TEST", "microtube", null,
                TEST_USER_ID);

        // When
        List<Map<String, Object>> samples = medLabPatientOrderService.getSamplesForProcessing(TEST_NOTEBOOK_ENTRY_ID);

        // Then - Extract all Lab Numbers
        List<String> allLabNos = samples.stream().map(s -> (String) s.get("labNo"))
                .filter(labNumber -> labNumber != null).toList();

        assertEquals("Should have 4 total Lab Nos (1 parent + 3 children)", 4, allLabNos.size());

        // Verify all Lab Numbers are unique
        long uniqueCount = allLabNos.stream().distinct().count();
        assertEquals("All Lab Numbers should be unique", 4, uniqueCount);

        // Verify parent Lab No
        assertTrue("Parent Lab No should be in list", allLabNos.contains(labNo));

        // Verify children don't share parent's Lab No
        List<Map<String, Object>> childSamples = samples.stream().filter(s -> Boolean.TRUE.equals(s.get("isAliquot")))
                .toList();

        for (Map<String, Object> child : childSamples) {
            String childLabNo = (String) child.get("labNo");
            assertNotEquals("Child Lab No should differ from parent", labNo, childLabNo);
        }
    }

    @Test
    public void testGetSamplesForProcessing_ProcessingStatusAppliedToChildren() {
        // Given - Create order, collect sample, pass QC + route, create aliquots,
        // process one child
        String labNo = "TEST-PROCESSING-004";
        List<String> testIds = List.of(TEST_TEST_ID_1);

        medLabPatientOrderService.createPatientOrder(TEST_PATIENT_ID, labNo, "2026-01-09", "2026-01-09", "ROUTINE",
                testIds, TEST_NOTEBOOK_ENTRY_ID, 8002, TEST_USER_ID);

        Map<String, Object> collectResult = medLabPatientOrderService.recordSampleCollection(labNo, "8001", "tube",
                "10:30", "2026-01-09", "2", "5.0", null, 8002, TEST_USER_ID);

        String sampleId = collectResult.get("sampleId").toString();
        Sample createdSample = sampleService.get(sampleId);
        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(createdSample.getId());
        Integer sampleItemId = Integer.parseInt(sampleItems.get(0).getId());

        // Pass QC and route to INTERNAL_ANALYSIS
        passQCAndRouteToProcessing(labNo, sampleItemId, TEST_NOTEBOOK_ENTRY_ID);

        // Create 2 aliquots
        Map<String, Object> aliquotResult = medLabPatientOrderService.createAliquots(List.of(sampleItemId), 2,
                "PROC-TEST", "cryovial", null, TEST_USER_ID);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> createdChildren = (List<Map<String, Object>>) aliquotResult.get("children");
        Integer firstChildId = Integer.parseInt(createdChildren.get(0).get("id").toString());

        // Record processing for first child only
        medLabPatientOrderService.recordProcessing(List.of(firstChildId), "chemistry-centrifugation", "serum",
                "Processing notes", false, false, null, TEST_USER_ID);

        // When
        List<Map<String, Object>> samples = medLabPatientOrderService.getSamplesForProcessing(TEST_NOTEBOOK_ENTRY_ID);

        // Then
        List<Map<String, Object>> childSamples = samples.stream().filter(s -> Boolean.TRUE.equals(s.get("isAliquot")))
                .toList();

        assertEquals("Should have 2 child samples", 2, childSamples.size());

        // Find processed and unprocessed children
        long processedCount = childSamples.stream().filter(s -> "COMPLETED".equals(s.get("pageStatus"))).count();
        long pendingCount = childSamples.stream().filter(s -> "PENDING".equals(s.get("pageStatus"))).count();

        assertEquals("Should have 1 processed child", 1, processedCount);
        assertEquals("Should have 1 pending child", 1, pendingCount);

        // Verify processed child has processing data
        Map<String, Object> processedChild = childSamples.stream().filter(s -> "COMPLETED".equals(s.get("pageStatus")))
                .findFirst().orElseThrow();

        assertEquals("chemistry-centrifugation", processedChild.get("processingType"));
        assertEquals("serum", processedChild.get("derivedMaterial"));
    }

    @Test
    public void testRecordSampleCollection_CreatesAnalysisRecords() {
        // Given - Create order with 2 tests
        String labNo = "TEST-ANALYSIS-001";
        List<String> testIds = List.of(TEST_TEST_ID_1, TEST_TEST_ID_2);

        medLabPatientOrderService.createPatientOrder(TEST_PATIENT_ID, labNo, "2026-01-09", "2026-01-09", "ROUTINE",
                testIds, TEST_NOTEBOOK_ENTRY_ID, 8002, TEST_USER_ID);

        // When - Record sample collection
        Map<String, Object> collectResult = medLabPatientOrderService.recordSampleCollection(labNo, "8001", "tube",
                "10:30", "2026-01-09", "2", "5.0", null, 8002, TEST_USER_ID);

        // Then - Verify sample was created
        assertTrue("Sample collection should succeed", (Boolean) collectResult.get("success"));
        String sampleId = collectResult.get("sampleId").toString();
        assertNotNull("Sample ID should be returned", sampleId);
        assertEquals("Should have 2 tests", 2, collectResult.get("testCount"));

        // Then - Verify Analysis records were created
        Sample createdSample = sampleService.get(sampleId);
        assertNotNull("Sample should exist", createdSample);

        List<org.openelisglobal.analysis.valueholder.Analysis> analyses = analysisService
                .getAnalysesBySampleId(sampleId);
        assertNotNull("Analysis list should not be null", analyses);
        assertEquals("Should have 2 Analysis records (one per test)", 2, analyses.size());

        // Verify each analysis has correct test assignment
        List<String> analysisTestIds = analyses.stream().map(a -> a.getTest() != null ? a.getTest().getId() : null)
                .filter(id -> id != null).toList();

        assertEquals("Should have 2 test IDs from analyses", 2, analysisTestIds.size());
        assertTrue("Should contain test 1", analysisTestIds.contains(TEST_TEST_ID_1));
        assertTrue("Should contain test 2", analysisTestIds.contains(TEST_TEST_ID_2));

        // Verify analysis status is correct (Entered)
        for (org.openelisglobal.analysis.valueholder.Analysis analysis : analyses) {
            assertNotNull("Analysis should have a test", analysis.getTest());
            assertNotNull("Analysis should have a sample item", analysis.getSampleItem());
            assertNotNull("Analysis should have a status", analysis.getStatusId());
            assertEquals("Analysis type should be MANUAL", "MANUAL", analysis.getAnalysisType());
        }
    }

    @Test
    public void testCreateAliquots_NewAliquotsHavePendingStatus() {
        // Given - Create order, collect sample, pass QC + route to processing page
        String labNo = "TEST-ALIQUOT-STATUS";
        List<String> testIds = List.of(TEST_TEST_ID_1);

        medLabPatientOrderService.createPatientOrder(TEST_PATIENT_ID, labNo, "2026-01-09", "2026-01-09", "ROUTINE",
                testIds, TEST_NOTEBOOK_ENTRY_ID, 8002, TEST_USER_ID);

        Map<String, Object> collectResult = medLabPatientOrderService.recordSampleCollection(labNo, "8001", "tube",
                "10:30", "2026-01-09", "2", "5.0", null, 8002, TEST_USER_ID);

        String sampleId = collectResult.get("sampleId").toString();
        Sample createdSample = sampleService.get(sampleId);
        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(createdSample.getId());
        Integer sampleItemId = Integer.parseInt(sampleItems.get(0).getId());

        // Pass QC and route to INTERNAL_ANALYSIS
        passQCAndRouteToProcessing(labNo, sampleItemId, TEST_NOTEBOOK_ENTRY_ID);

        // When - Create 2 aliquots with notebookPageId specified (Sample Processing
        // page)
        Integer processingPageId = 8005; // medlab-sample-processing page from test data
        Map<String, Object> aliquotResult = medLabPatientOrderService.createAliquots(List.of(sampleItemId), 2,
                "ALQ-STATUS", "cryovial", processingPageId, TEST_USER_ID);

        // Then - Verify aliquots were created successfully
        assertTrue("Aliquot creation should succeed", (Boolean) aliquotResult.get("success"));
        assertEquals("Should create 2 aliquots", 2, aliquotResult.get("createdCount"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> createdChildren = (List<Map<String, Object>>) aliquotResult.get("children");
        assertEquals("Should have 2 children in result", 2, createdChildren.size());

        // Then - Verify that NotebookPageSample records for new aliquots have PENDING
        // status
        for (Map<String, Object> childInfo : createdChildren) {
            String childSampleItemId = childInfo.get("id").toString();

            // Get NotebookPageSample records for this child
            List<NotebookPageSample> pageSamples = notebookPageSampleService
                    .getBySampleItemId(Integer.parseInt(childSampleItemId));

            // Find the page sample for the processing page
            NotebookPageSample processingPageSample = pageSamples.stream()
                    .filter(nps -> nps.getNotebookPageId().equals(processingPageId)).findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "NotebookPageSample not found for child " + childSampleItemId + " on processing page"));

            // Assert that status is PENDING, not COMPLETED
            assertEquals("Newly created aliquot should have PENDING status", NotebookPageSample.Status.PENDING,
                    processingPageSample.getStatus());
        }

        // Also verify via getSamplesForProcessing API that pageStatus is PENDING
        List<Map<String, Object>> samples = medLabPatientOrderService.getSamplesForProcessing(TEST_NOTEBOOK_ENTRY_ID);
        List<Map<String, Object>> childSamples = samples.stream().filter(s -> Boolean.TRUE.equals(s.get("isAliquot")))
                .toList();

        // Filter to only the children we just created
        List<Map<String, Object>> newlyCreatedChildren = childSamples.stream()
                .filter(s -> s.get("externalId") != null && ((String) s.get("externalId")).startsWith("ALQ-STATUS"))
                .toList();

        assertEquals("Should find 2 newly created aliquots", 2, newlyCreatedChildren.size());

        for (Map<String, Object> child : newlyCreatedChildren) {
            assertEquals("Newly created aliquot should have pageStatus PENDING", "PENDING", child.get("pageStatus"));
        }
    }

    // ==================== getSamplesForResultEntry Tests ====================

    @Test
    public void testGetSamplesForResultEntry_TestedSampleAppearsInResultEntry() {
        // Given - Create order with ONE test, collect sample, pass QC, and complete
        // testing
        // NOTE: recordSampleCollection creates a separate sample_item for EACH test.
        // Each sample_item only shows its OWN analyses (not all analyses from the
        // Sample).
        // RULE: Any sample_item that has COMPLETED testing AND has analyses should
        // appear.
        String labNo = "RESULT-ENTRY-001";
        List<String> testIds = List.of(TEST_TEST_ID_1); // Single test = single sample_item

        medLabPatientOrderService.createPatientOrder(TEST_PATIENT_ID, labNo, "2026-01-09", "2026-01-09", "ROUTINE",
                testIds, TEST_NOTEBOOK_ENTRY_ID, 8002, TEST_USER_ID);

        Map<String, Object> collectResult = medLabPatientOrderService.recordSampleCollection(labNo, "8001", "tube",
                "10:30", "2026-01-09", "2", "5.0", null, 8002, TEST_USER_ID);

        String sampleId = collectResult.get("sampleId").toString();
        Sample createdSample = sampleService.get(sampleId);
        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(createdSample.getId());
        Integer sampleItemId = Integer.parseInt(sampleItems.get(0).getId());

        // Pass QC and route
        passQCAndRouteToProcessing(labNo, sampleItemId, TEST_NOTEBOOK_ENTRY_ID);

        // Complete processing
        completeProcessing(sampleItemId, TEST_NOTEBOOK_ENTRY_ID);

        // Complete testing - this sample_item has now been tested
        completeTesting(sampleItemId, TEST_NOTEBOOK_ENTRY_ID);

        // When - Get samples for result entry
        List<Map<String, Object>> resultEntrySamples = medLabPatientOrderService
                .getSamplesForResultEntry(TEST_NOTEBOOK_ENTRY_ID);

        // Then - Sample should appear because it has COMPLETED testing
        assertFalse("Tested sample should appear in Result Entry", resultEntrySamples.isEmpty());

        Map<String, Object> resultSample = resultEntrySamples.stream().filter(s -> labNo.equals(s.get("labNo")))
                .findFirst().orElse(null);

        assertNotNull("Sample " + labNo + " should be in Result Entry list", resultSample);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tests = (List<Map<String, Object>>) resultSample.get("tests");
        assertNotNull("Sample should have tests", tests);
        assertEquals("Sample_item should have 1 test (its own analysis)", 1, tests.size());
    }

    @Test
    public void testGetSamplesForResultEntry_AliquotTestedAppearsInResultEntry() {
        // Given - Create sample, create aliquot, ASSIGN TESTS to aliquot, then test it
        // RULE: If an aliquot has COMPLETED testing AND has analyses, it should appear
        // NOTE: Aliquots start with NO analyses - tests must be assigned to create them
        String labNo = "RESULT-ENTRY-002";
        List<String> testIds = List.of(TEST_TEST_ID_1); // Single test on parent

        medLabPatientOrderService.createPatientOrder(TEST_PATIENT_ID, labNo, "2026-01-09", "2026-01-09", "ROUTINE",
                testIds, TEST_NOTEBOOK_ENTRY_ID, 8002, TEST_USER_ID);

        Map<String, Object> collectResult = medLabPatientOrderService.recordSampleCollection(labNo, "8001", "tube",
                "10:30", "2026-01-09", "2", "5.0", null, 8002, TEST_USER_ID);

        String sampleId = collectResult.get("sampleId").toString();
        Sample createdSample = sampleService.get(sampleId);
        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(createdSample.getId());
        Integer parentSampleItemId = Integer.parseInt(sampleItems.get(0).getId());

        // Pass QC on the parent
        passQCAndRouteToProcessing(labNo, parentSampleItemId, TEST_NOTEBOOK_ENTRY_ID);

        // Complete processing on the parent
        completeProcessing(parentSampleItemId, TEST_NOTEBOOK_ENTRY_ID);

        // Create an aliquot
        Integer processingPageId = 8005;
        Map<String, Object> aliquotResult = medLabPatientOrderService.createAliquots(List.of(parentSampleItemId), 1,
                "ALQ-RESULT", "cryovial", processingPageId, TEST_USER_ID);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> createdChildren = (List<Map<String, Object>>) aliquotResult.get("children");
        Integer aliquotSampleItemId = Integer.parseInt(createdChildren.get(0).get("id").toString());

        // IMPORTANT: Assign tests to the aliquot to create Analysis records
        // Without this, the aliquot has no analyses and won't appear in Result Entry
        List<String> aliquotTestIds = List.of(TEST_TEST_ID_2); // Different test for aliquot
        medLabPatientOrderService.assignTestsToSamples(List.of(aliquotSampleItemId), aliquotTestIds, processingPageId,
                TEST_USER_ID);

        // Complete testing on the ALIQUOT only (not the parent)
        completeTesting(aliquotSampleItemId, TEST_NOTEBOOK_ENTRY_ID);

        // When - Get samples for result entry
        List<Map<String, Object>> resultEntrySamples = medLabPatientOrderService
                .getSamplesForResultEntry(TEST_NOTEBOOK_ENTRY_ID);

        // Then - The ALIQUOT should appear in Result Entry because IT has COMPLETED
        // testing AND has analyses
        assertFalse("Result Entry should not be empty when aliquot was tested", resultEntrySamples.isEmpty());

        // Verify the ALIQUOT appears (identified by its sampleItemId)
        Map<String, Object> testedAliquot = resultEntrySamples.stream().filter(s -> {
            String sampleItemIdStr = s.get("sampleItemId") != null ? s.get("sampleItemId").toString() : null;
            return aliquotSampleItemId.toString().equals(sampleItemIdStr);
        }).findFirst().orElse(null);

        assertNotNull("The TESTED aliquot should appear in Result Entry", testedAliquot);
        assertTrue("The sample should be flagged as an aliquot", Boolean.TRUE.equals(testedAliquot.get("isAliquot")));

        // Verify that aliquot has its OWN test (not parent's test)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> aliquotTests = (List<Map<String, Object>>) testedAliquot.get("tests");
        assertNotNull("Aliquot should have tests", aliquotTests);
        assertEquals("Aliquot should have 1 test (its own)", 1, aliquotTests.size());
        assertEquals("Aliquot should have TEST_TEST_ID_2", TEST_TEST_ID_2, aliquotTests.get(0).get("testId"));

        // Verify that the UNTESTED parent does NOT appear
        Map<String, Object> untestedParent = resultEntrySamples.stream().filter(s -> {
            String sampleItemIdStr = s.get("sampleItemId") != null ? s.get("sampleItemId").toString() : null;
            return parentSampleItemId.toString().equals(sampleItemIdStr);
        }).findFirst().orElse(null);

        assertNull("The UNTESTED parent should NOT appear in Result Entry", untestedParent);
    }

    @Test
    public void testGetSamplesForResultEntry_EachSampleItemShowsOwnAnalyses() {
        // Given - Create sample with analysis on parent, then create aliquot with its
        // own analysis
        // BUG: Currently, both parent and aliquot show ALL analyses (loaded by Sample
        // ID)
        // RULE: Each sample_item in Result Entry should only show analyses on THAT
        // sample_item
        String labNo = "RESULT-ENTRY-003";
        List<String> testIds = List.of(TEST_TEST_ID_1); // Only one test on parent

        // Create order and collect sample - this creates analysis on PARENT sample_item
        medLabPatientOrderService.createPatientOrder(TEST_PATIENT_ID, labNo, "2026-01-09", "2026-01-09", "ROUTINE",
                testIds, TEST_NOTEBOOK_ENTRY_ID, 8002, TEST_USER_ID);

        Map<String, Object> collectResult = medLabPatientOrderService.recordSampleCollection(labNo, "8001", "tube",
                "10:30", "2026-01-09", "2", "5.0", null, 8002, TEST_USER_ID);

        String sampleId = collectResult.get("sampleId").toString();
        Sample createdSample = sampleService.get(sampleId);
        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(createdSample.getId());
        Integer parentSampleItemId = Integer.parseInt(sampleItems.get(0).getId());

        // Verify parent has analysis
        SampleItem parentSampleItem = sampleItemService.get(parentSampleItemId.toString());
        List<org.openelisglobal.analysis.valueholder.Analysis> parentAnalyses = analysisService
                .getAnalysesBySampleItem(parentSampleItem);
        assertFalse("Parent should have at least one analysis", parentAnalyses.isEmpty());

        // Pass QC and complete processing on parent
        passQCAndRouteToProcessing(labNo, parentSampleItemId, TEST_NOTEBOOK_ENTRY_ID);
        completeProcessing(parentSampleItemId, TEST_NOTEBOOK_ENTRY_ID);

        // Create an aliquot (which has NO analyses initially)
        Integer processingPageId = 8005;
        Map<String, Object> aliquotResult = medLabPatientOrderService.createAliquots(List.of(parentSampleItemId), 1,
                "ALQ-SEPARATE", "cryovial", processingPageId, TEST_USER_ID);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> createdChildren = (List<Map<String, Object>>) aliquotResult.get("children");
        Integer aliquotSampleItemId = Integer.parseInt(createdChildren.get(0).get("id").toString());

        // Verify aliquot has NO analyses (it's a new container, no tests ordered on it)
        SampleItem aliquotSampleItem = sampleItemService.get(aliquotSampleItemId.toString());
        List<org.openelisglobal.analysis.valueholder.Analysis> aliquotAnalyses = analysisService
                .getAnalysesBySampleItem(aliquotSampleItem);
        assertTrue("Aliquot should have NO analyses (no tests ordered on it)", aliquotAnalyses.isEmpty());

        // Complete testing on BOTH parent and aliquot
        completeTesting(parentSampleItemId, TEST_NOTEBOOK_ENTRY_ID);
        completeTesting(aliquotSampleItemId, TEST_NOTEBOOK_ENTRY_ID);

        // When - Get samples for result entry
        List<Map<String, Object>> resultEntrySamples = medLabPatientOrderService
                .getSamplesForResultEntry(TEST_NOTEBOOK_ENTRY_ID);

        // Then - Parent should appear with its tests
        // Aliquot should NOT appear (no analyses = no results to enter)
        // FIX: Analyses are now loaded by sample_item, not by Sample ID

        // Find the parent entry
        Map<String, Object> parentEntry = resultEntrySamples.stream().filter(s -> {
            String sampleItemIdStr = s.get("sampleItemId") != null ? s.get("sampleItemId").toString() : null;
            return parentSampleItemId.toString().equals(sampleItemIdStr);
        }).findFirst().orElse(null);

        // Find the aliquot entry
        Map<String, Object> aliquotEntry = resultEntrySamples.stream().filter(s -> {
            String sampleItemIdStr = s.get("sampleItemId") != null ? s.get("sampleItemId").toString() : null;
            return aliquotSampleItemId.toString().equals(sampleItemIdStr);
        }).findFirst().orElse(null);

        // Parent should appear (has analyses)
        assertNotNull("Parent should appear in Result Entry (has analyses)", parentEntry);

        // Parent should have tests
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parentTests = (List<Map<String, Object>>) parentEntry.get("tests");
        assertNotNull("Parent should have tests list", parentTests);
        assertFalse("Parent should have at least one test", parentTests.isEmpty());

        // Aliquot should NOT appear (no analyses on this sample_item)
        // This is the key fix: each sample_item only shows its OWN analyses
        assertNull("Aliquot should NOT appear in Result Entry (no analyses on this sample_item)", aliquotEntry);
    }
}
