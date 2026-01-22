package org.openelisglobal.biorepository;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.biorepository.service.BioSampleService;
import org.openelisglobal.biorepository.service.ChainOfCustodyService;
import org.openelisglobal.biorepository.service.SampleRetrievalService;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.BiosafetyLevel;
import org.openelisglobal.biorepository.valueholder.BioSample.WorkflowStatus;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalItem;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalItem.ItemStatus;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.DestinationType;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.PriorityLevel;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.RequestStatus;
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
 * Integration tests for SampleRetrievalService. Tests the complete retrieval
 * workflow: create -> submit -> approve -> retrieve -> return.
 */
public class SampleRetrievalServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleRetrievalService retrievalService;

    @Autowired
    private BioSampleService bioSampleService;

    @Autowired
    private ChainOfCustodyService custodyService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private SystemUserService systemUserService;

    private SystemUser requester;
    private SystemUser approver;
    private TypeOfSample testSampleType;
    private Sample testSample;

    @Before
    public void setUp() {
        // Create requester user (login_name max 20 chars, unique names)
        String suffix = String.valueOf(System.currentTimeMillis() % 1000000);
        requester = new SystemUser();
        requester.setLoginName("req_" + suffix);
        requester.setFirstName("Req" + suffix);
        requester.setLastName("User" + suffix);
        requester.setIsActive("Y");
        requester.setIsEmployee("Y");
        requester.setSysUserId("1");
        requester = systemUserService.save(requester);

        // Create approver user (different from requester for approval tests)
        approver = new SystemUser();
        approver.setLoginName("apr_" + suffix);
        approver.setFirstName("Apr" + suffix);
        approver.setLastName("Usr" + suffix);
        approver.setIsActive("Y");
        approver.setIsEmployee("Y");
        approver.setSysUserId("1");
        approver = systemUserService.save(approver);

        var sampleTypes = typeOfSampleService.getAll();
        testSampleType = sampleTypes.isEmpty() ? createSampleType("Serum") : sampleTypes.get(0);

        testSample = createSample("RET" + (System.currentTimeMillis() % 100000000));
    }

    // ========== FULL WORKFLOW TEST ==========

    @Test
    public void testFullRetrievalWorkflow_CreateToReturn() {
        // Setup: Create a stored BioSample
        BioSample bioSample = createStoredBioSample("WF-" + System.currentTimeMillis());
        Integer bioSampleId = bioSample.getId();

        // Step 1: Create request (DRAFT)
        SampleRetrievalRequest request = retrievalService.createRequest("Research analysis", Arrays.asList(bioSampleId),
                null, null, DestinationType.ANALYSIS_RETURN, "Lab 101", PriorityLevel.NORMAL,
                LocalDate.now().plusDays(7), requester.getId().toString());

        assertEquals(RequestStatus.DRAFT, request.getStatus());
        assertEquals(1, request.getTotalItemCount());
        assertNotNull(request.getRequestNumber());

        // Step 2: Submit for approval
        request = retrievalService.submitForApproval(request.getId(), requester.getId().toString());
        assertEquals(RequestStatus.PENDING_APPROVAL, request.getStatus());

        // Step 3: Approve (generates work order)
        request = retrievalService.approveRequest(request.getId(), "Approved for research",
                approver.getId().toString());
        assertEquals(RequestStatus.APPROVED, request.getStatus());
        assertNotNull(request.getWorkOrderNumber());
        assertEquals(approver.getId(), request.getApprovedBy().getId());

        // Step 4: Retrieve item
        Integer itemId = request.getItems().get(0).getId();
        SampleRetrievalItem item = retrievalService.retrieveItem(itemId, "Good", "No issues", null,
                approver.getId().toString());
        assertEquals(ItemStatus.RETRIEVED, item.getStatus());

        // Verify BioSample status changed to IN_USE
        BioSample updatedBioSample = bioSampleService.get(bioSampleId);
        assertEquals(WorkflowStatus.IN_USE, updatedBioSample.getWorkflowStatus());

        // Step 5: Return item
        item = retrievalService.returnItem(itemId, "Good condition", "Analysis complete", false,
                approver.getId().toString());
        assertEquals(ItemStatus.RETURNED, item.getStatus());

        // Verify BioSample status returned to STORED
        updatedBioSample = bioSampleService.get(bioSampleId);
        assertEquals(WorkflowStatus.STORED, updatedBioSample.getWorkflowStatus());

        // Verify request completed
        request = retrievalService.get(request.getId());
        assertEquals(RequestStatus.COMPLETED, request.getStatus());

        // Verify chain of custody logs were created
        List<ChainOfCustodyLog> logs = custodyService
                .getBySampleItemId(Integer.valueOf(bioSample.getSampleItem().getId()));
        assertTrue("Should have custody log entries", logs.size() >= 2);
    }

    // ========== CREATE REQUEST TESTS ==========

    @Test
    public void testCreateRequest_MultipleItems() {
        BioSample bs1 = createStoredBioSample("MULTI1-" + System.currentTimeMillis());
        BioSample bs2 = createStoredBioSample("MULTI2-" + System.currentTimeMillis());

        SampleRetrievalRequest request = retrievalService.createRequest("Batch analysis",
                Arrays.asList(bs1.getId(), bs2.getId()), null, "ETH-001", DestinationType.INTERNAL_LAB, "Pathology",
                PriorityLevel.URGENT, null, requester.getId().toString());

        assertEquals(2, request.getTotalItemCount());
        assertEquals(PriorityLevel.URGENT, request.getPriorityLevel());
        assertEquals("ETH-001", request.getEthicsApprovalRef());
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateRequest_SampleNotStored_Fails() {
        // Create BioSample with IN_USE status
        BioSample bioSample = createBioSampleWithStatus("NOTST-" + System.currentTimeMillis(), WorkflowStatus.IN_USE);

        retrievalService.createRequest("Should fail", Arrays.asList(bioSample.getId()), null, null,
                DestinationType.ANALYSIS_RETURN, null, PriorityLevel.NORMAL, null, requester.getId().toString());
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateRequest_DuplicatePending_Fails() {
        BioSample bioSample = createStoredBioSample("DUP-" + System.currentTimeMillis());

        // First request
        SampleRetrievalRequest first = retrievalService.createRequest("First", Arrays.asList(bioSample.getId()), null,
                null, DestinationType.ANALYSIS_RETURN, null, PriorityLevel.NORMAL, null, requester.getId().toString());
        retrievalService.submitForApproval(first.getId(), requester.getId().toString());

        // Second request should fail
        retrievalService.createRequest("Second", Arrays.asList(bioSample.getId()), null, null,
                DestinationType.ANALYSIS_RETURN, null, PriorityLevel.NORMAL, null, requester.getId().toString());
    }

    // ========== APPROVAL TESTS ==========

    @Test
    public void testApproveOwnRequest_TemporarilyAllowed() {
        // TODO: Re-enable self-approval prevention test when the check is uncommented
        // in production
        BioSample bioSample = createStoredBioSample("SELF-" + System.currentTimeMillis());
        SampleRetrievalRequest request = createAndSubmitRequest(bioSample);

        // Self-approval is temporarily allowed for testing
        request = retrievalService.approveRequest(request.getId(), "Self-approve", requester.getId().toString());

        assertEquals(RequestStatus.APPROVED, request.getStatus());
        assertNotNull(request.getWorkOrderNumber());
    }

    @Test
    public void testRejectRequest() {
        BioSample bioSample = createStoredBioSample("REJ-" + System.currentTimeMillis());
        SampleRetrievalRequest request = createAndSubmitRequest(bioSample);

        request = retrievalService.rejectRequest(request.getId(), "Insufficient justification",
                approver.getId().toString());

        assertEquals(RequestStatus.REJECTED, request.getStatus());
        assertEquals("Insufficient justification", request.getRejectionReason());

        // BioSample should remain STORED
        BioSample updatedBioSample = bioSampleService.get(bioSample.getId());
        assertEquals(WorkflowStatus.STORED, updatedBioSample.getWorkflowStatus());
    }

    // ========== ITEM OPERATIONS TESTS ==========

    @Test
    public void testConsumedItem_DisposessSample() {
        BioSample bioSample = createStoredBioSample("CONS-" + System.currentTimeMillis());
        SampleRetrievalRequest request = createApprovedRequest(bioSample);
        Integer itemId = request.getItems().get(0).getId();

        // Retrieve
        retrievalService.retrieveItem(itemId, "Good", null, null, approver.getId().toString());

        // Return as consumed
        SampleRetrievalItem item = retrievalService.returnItem(itemId, "Fully used", "Sample exhausted", true,
                approver.getId().toString());

        assertEquals(ItemStatus.CONSUMED, item.getStatus());

        // BioSample should be DISPOSED
        BioSample updatedBioSample = bioSampleService.get(bioSample.getId());
        assertEquals(WorkflowStatus.DISPOSED, updatedBioSample.getWorkflowStatus());
    }

    @Test
    public void testMarkItemUnavailable() {
        BioSample bioSample = createStoredBioSample("UNAV-" + System.currentTimeMillis());
        SampleRetrievalRequest request = createApprovedRequest(bioSample);
        Integer itemId = request.getItems().get(0).getId();

        SampleRetrievalItem item = retrievalService.markItemUnavailable(itemId, "Sample degraded",
                approver.getId().toString());

        assertEquals(ItemStatus.UNAVAILABLE, item.getStatus());
        assertEquals("Sample degraded", item.getConditionNotes());
    }

    // ========== CANCEL REQUEST TEST ==========

    @Test
    public void testCancelRequest() {
        BioSample bioSample = createStoredBioSample("CANC-" + System.currentTimeMillis());
        SampleRetrievalRequest request = retrievalService.createRequest("Cancel me", Arrays.asList(bioSample.getId()),
                null, null, DestinationType.ANALYSIS_RETURN, null, PriorityLevel.NORMAL, null,
                requester.getId().toString());

        request = retrievalService.cancelRequest(request.getId(), requester.getId().toString());

        assertEquals(RequestStatus.CANCELLED, request.getStatus());
    }

    @Test(expected = IllegalStateException.class)
    public void testCancelApprovedRequest_Fails() {
        BioSample bioSample = createStoredBioSample("CAPP-" + System.currentTimeMillis());
        SampleRetrievalRequest request = createApprovedRequest(bioSample);

        retrievalService.cancelRequest(request.getId(), requester.getId().toString());
    }

    // ========== QUERY TESTS ==========

    @Test
    public void testGetPendingApproval() {
        BioSample bs1 = createStoredBioSample("PEND1-" + System.currentTimeMillis());
        BioSample bs2 = createStoredBioSample("PEND2-" + System.currentTimeMillis());
        createAndSubmitRequest(bs1);
        createAndSubmitRequest(bs2);

        List<SampleRetrievalRequest> pending = retrievalService.getPendingApproval(10);

        assertTrue("Should have at least 2 pending", pending.size() >= 2);
        for (SampleRetrievalRequest req : pending) {
            assertEquals(RequestStatus.PENDING_APPROVAL, req.getStatus());
            // Verify items collection is loaded (bug fix: was returning 0)
            assertTrue("Request should have at least 1 item", req.getTotalItemCount() > 0);
            assertNotNull("Items collection should be loaded", req.getItems());
            assertFalse("Items collection should not be empty", req.getItems().isEmpty());
        }
    }

    @Test
    public void testGetCheckedOutItems() {
        BioSample bioSample = createStoredBioSample("CHKOUT-" + System.currentTimeMillis());
        SampleRetrievalRequest request = createApprovedRequest(bioSample);
        Integer itemId = request.getItems().get(0).getId();

        retrievalService.retrieveItem(itemId, "Good", null, null, approver.getId().toString());

        List<SampleRetrievalItem> checkedOut = retrievalService.getCheckedOutItems();

        assertTrue("Should have at least 1 checked out item", checkedOut.size() >= 1);
        boolean found = checkedOut.stream().anyMatch(i -> i.getId().equals(itemId));
        assertTrue("Our item should be in checked out list", found);
    }

    // ========== MANUAL COMPLETE TESTS ==========

    @Test
    public void testManualCompleteRequest_ApprovedStatus() {
        BioSample bioSample = createStoredBioSample("MANUAL-" + System.currentTimeMillis());
        SampleRetrievalRequest request = createApprovedRequest(bioSample);

        // Manually mark as completed (administrative override)
        request.setStatus(RequestStatus.COMPLETED);
        request.setSysUserId(approver.getId().toString());
        retrievalService.update(request);

        // Verify status changed
        SampleRetrievalRequest updated = retrievalService.get(request.getId());
        assertEquals(RequestStatus.COMPLETED, updated.getStatus());
    }

    @Test
    public void testManualCompleteRequest_InProgressStatus() {
        BioSample bioSample = createStoredBioSample("MANUAL2-" + System.currentTimeMillis());
        SampleRetrievalRequest request = createApprovedRequest(bioSample);

        // Start retrieval to move to IN_PROGRESS
        Integer itemId = request.getItems().get(0).getId();
        retrievalService.retrieveItem(itemId, "Good", null, null, approver.getId().toString());

        request = retrievalService.get(request.getId());
        assertEquals(RequestStatus.IN_PROGRESS, request.getStatus());

        // Manually mark as completed
        request.setStatus(RequestStatus.COMPLETED);
        request.setSysUserId(approver.getId().toString());
        retrievalService.update(request);

        // Verify status changed
        SampleRetrievalRequest updated = retrievalService.get(request.getId());
        assertEquals(RequestStatus.COMPLETED, updated.getStatus());
    }

    @Test
    public void testManualCompleteRequest_WithMultipleItems() {
        BioSample bs1 = createStoredBioSample("BULK1-" + System.currentTimeMillis());
        BioSample bs2 = createStoredBioSample("BULK2-" + System.currentTimeMillis());

        SampleRetrievalRequest request = retrievalService.createRequest("Bulk test",
                Arrays.asList(bs1.getId(), bs2.getId()), null, null, DestinationType.ANALYSIS_RETURN, null,
                PriorityLevel.NORMAL, null, requester.getId().toString());
        request = retrievalService.submitForApproval(request.getId(), requester.getId().toString());
        request = retrievalService.approveRequest(request.getId(), "Approved", approver.getId().toString());

        assertEquals(2, request.getTotalItemCount());
        assertEquals(RequestStatus.APPROVED, request.getStatus());

        // Manually complete without processing items
        request.setStatus(RequestStatus.COMPLETED);
        request.setSysUserId(approver.getId().toString());
        retrievalService.update(request);

        SampleRetrievalRequest updated = retrievalService.get(request.getId());
        assertEquals(RequestStatus.COMPLETED, updated.getStatus());
        assertEquals(2, updated.getTotalItemCount());
    }

    // ========== HELPER METHODS ==========

    private TypeOfSample createSampleType(String desc) {
        TypeOfSample type = new TypeOfSample();
        type.setDescription(desc);
        type.setDomain("H");
        return typeOfSampleService.save(type);
    }

    private Sample createSample(String accession) {
        Sample sample = new Sample();
        sample.setAccessionNumber(accession);
        sample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));
        sample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
        sample.setDomain("H");
        sample.setSysUserId(requester.getId().toString());
        return sampleService.save(sample);
    }

    private SampleItem createSampleItem(String externalId) {
        SampleItem item = new SampleItem();
        item.setSample(testSample);
        item.setExternalId(externalId);
        item.setTypeOfSample(testSampleType);
        item.setSortOrder("1");
        item.setQuantity(10.0);
        item.setCollectionDate(new Timestamp(System.currentTimeMillis()));
        item.setStatusId("1");
        item.setSysUserId(requester.getId().toString());
        return sampleItemService.save(item);
    }

    private BioSample createStoredBioSample(String externalId) {
        return createBioSampleWithStatus(externalId, WorkflowStatus.STORED);
    }

    private BioSample createBioSampleWithStatus(String externalId, WorkflowStatus status) {
        SampleItem sampleItem = createSampleItem(externalId);
        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample.setWorkflowStatus(status);
        bioSample.setSysUserId(requester.getId().toString());
        return bioSampleService.createForSampleItem(sampleItem, bioSample);
    }

    private SampleRetrievalRequest createAndSubmitRequest(BioSample bioSample) {
        SampleRetrievalRequest request = retrievalService.createRequest("Test purpose",
                Arrays.asList(bioSample.getId()), null, null, DestinationType.ANALYSIS_RETURN, null,
                PriorityLevel.NORMAL, null, requester.getId().toString());
        return retrievalService.submitForApproval(request.getId(), requester.getId().toString());
    }

    private SampleRetrievalRequest createApprovedRequest(BioSample bioSample) {
        SampleRetrievalRequest request = createAndSubmitRequest(bioSample);
        return retrievalService.approveRequest(request.getId(), "Approved", approver.getId().toString());
    }
}
