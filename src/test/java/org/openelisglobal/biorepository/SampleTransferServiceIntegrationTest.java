package org.openelisglobal.biorepository;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.biorepository.service.BioSampleService;
import org.openelisglobal.biorepository.service.SampleTransferService;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.BiosafetyLevel;
import org.openelisglobal.biorepository.valueholder.SampleTransferItem;
import org.openelisglobal.biorepository.valueholder.SampleTransferItem.ItemStatus;
import org.openelisglobal.biorepository.valueholder.SampleTransferRequest;
import org.openelisglobal.biorepository.valueholder.SampleTransferRequest.TransferStatus;
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
 * Integration tests for SampleTransferService.
 *
 * Tests the internal lab transfer workflow: - Creating transfer requests from
 * origin labs - Accepting/rejecting individual items - Bulk accept/reject
 * operations - Transfer request cancellation - Status queries
 */
public class SampleTransferServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleTransferService transferService;

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

    private SystemUser testUser;
    private TypeOfSample testSampleType;
    private Sample testSample;

    @Before
    public void setUp() {
        testUser = systemUserService.get("1");
        if (testUser == null) {
            testUser = new SystemUser();
            testUser.setLoginName("test_transfer_user");
            testUser.setFirstName("Test");
            testUser.setLastName("Transfer User");
            testUser.setIsActive("Y");
            testUser.setSysUserId("1");
            systemUserService.save(testUser);
        }

        var sampleTypes = typeOfSampleService.getAll();
        if (!sampleTypes.isEmpty()) {
            testSampleType = sampleTypes.get(0);
        } else {
            testSampleType = new TypeOfSample();
            testSampleType.setDescription("Serum");
            testSampleType.setDomain("H");
            typeOfSampleService.save(testSampleType);
        }

        testSample = createTestSample("TR" + (System.currentTimeMillis() % 100000000));
    }

    // ========== CREATE TRANSFER REQUEST TESTS ==========

    @Test
    public void testCreateTransferRequest_SingleItem_Success() {
        // Arrange
        String externalId = "XFER-SINGLE-" + System.currentTimeMillis();
        SampleItem sampleItem = createTestSampleItem(testSample, externalId);
        Integer sampleItemId = Integer.valueOf(sampleItem.getId());
        String sourceLab = "Medical Laboratory";
        String notes = "Test transfer request";

        // Act
        SampleTransferRequest request = transferService.createTransferRequest(sourceLab, Arrays.asList(sampleItemId),
                notes, testUser.getId().toString());

        // Assert - verify specific values, not just existence
        assertNotNull("Request ID should be generated", request.getId());
        assertEquals("Source lab should be 'Medical Laboratory'", "Medical Laboratory", request.getSourceLab());
        assertEquals("Destination should be 'BIOREPOSITORY'", "BIOREPOSITORY", request.getDestinationLab());
        assertEquals("Status should be PENDING", TransferStatus.PENDING, request.getStatus());
        assertEquals("Request notes should match", notes, request.getRequestNotes());
        assertEquals("Item count should be exactly 1", 1, request.getTotalItemCount());
        assertEquals("Accepted count should be 0", 0, request.getAcceptedItemCount());
        assertEquals("Rejected count should be 0", 0, request.getRejectedItemCount());
        assertEquals("Requested by user ID should match", testUser.getId(), request.getRequestedBy().getId());

        // Verify the item was linked correctly
        SampleTransferItem item = request.getItems().get(0);
        assertEquals("Item sample item ID should match", sampleItemId, Integer.valueOf(item.getSampleItem().getId()));
        assertEquals("Item status should be PENDING", ItemStatus.PENDING, item.getStatus());
        assertNull("Item rejection reason should be null", item.getRejectionReason());
        assertNull("Item BioSample should be null (not yet accepted)", item.getBioSample());
    }

    @Test
    public void testCreateTransferRequest_MultipleItems_Success() {
        // Arrange
        String externalId1 = "XFER-MULTI1-" + System.currentTimeMillis();
        String externalId2 = "XFER-MULTI2-" + System.currentTimeMillis();
        String externalId3 = "XFER-MULTI3-" + System.currentTimeMillis();
        SampleItem item1 = createTestSampleItem(testSample, externalId1);
        SampleItem item2 = createTestSampleItem(testSample, externalId2);
        SampleItem item3 = createTestSampleItem(testSample, externalId3);
        String sourceLab = "Pathology";

        List<Integer> sampleItemIds = Arrays.asList(Integer.valueOf(item1.getId()), Integer.valueOf(item2.getId()),
                Integer.valueOf(item3.getId()));

        // Act
        SampleTransferRequest request = transferService.createTransferRequest(sourceLab, sampleItemIds,
                "Bulk transfer from Pathology", testUser.getId().toString());

        // Assert - verify exact counts
        assertEquals("Item count should be exactly 3", 3, request.getTotalItemCount());
        assertEquals("Accepted count should be 0", 0, request.getAcceptedItemCount());
        assertEquals("Rejected count should be 0", 0, request.getRejectedItemCount());
        assertEquals("Items list size should be 3", 3, request.getItems().size());

        // Verify each item is correctly linked to its SampleItem
        List<String> expectedExternalIds = Arrays.asList(externalId1, externalId2, externalId3);
        for (SampleTransferItem transferItem : request.getItems()) {
            assertEquals("Item status should be PENDING", ItemStatus.PENDING, transferItem.getStatus());
            assertNotNull("Item should have SampleItem", transferItem.getSampleItem());
            String actualExternalId = transferItem.getSampleItem().getExternalId();
            assertTrue("External ID should be one of the expected values: " + actualExternalId,
                    expectedExternalIds.contains(actualExternalId));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTransferRequest_InvalidSampleItem_ThrowsException() {
        Integer invalidId = 999999;
        transferService.createTransferRequest("Medical Lab", Arrays.asList(invalidId), "Invalid transfer",
                testUser.getId().toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTransferRequest_EmptySourceLab_ThrowsException() {
        SampleItem sampleItem = createTestSampleItem(testSample, "XFER-EMPTY-" + System.currentTimeMillis());
        transferService.createTransferRequest("", Arrays.asList(Integer.valueOf(sampleItem.getId())), "Empty source",
                testUser.getId().toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTransferRequest_EmptySampleItemList_ThrowsException() {
        transferService.createTransferRequest("Medical Lab", Arrays.asList(), "Empty items",
                testUser.getId().toString());
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateTransferRequest_DuplicatePendingTransfer_ThrowsException() {
        // Arrange - create a pending transfer for a sample item
        SampleItem sampleItem = createTestSampleItem(testSample, "XFER-DUP-" + System.currentTimeMillis());
        Integer sampleItemId = Integer.valueOf(sampleItem.getId());
        transferService.createTransferRequest("Lab A", Arrays.asList(sampleItemId), "First transfer",
                testUser.getId().toString());

        // Act - should throw exception
        transferService.createTransferRequest("Lab B", Arrays.asList(sampleItemId), "Second transfer",
                testUser.getId().toString());
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateTransferRequest_AlreadyInBiorepository_ThrowsException() {
        // Arrange - create a sample item that already has a BioSample
        SampleItem sampleItem = createTestSampleItem(testSample, "XFER-EXISTS-" + System.currentTimeMillis());
        BioSample existingBioSample = new BioSample();
        existingBioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        existingBioSample.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem, existingBioSample);

        // Act - should throw exception
        transferService.createTransferRequest("Medical Lab", Arrays.asList(Integer.valueOf(sampleItem.getId())),
                "Should fail", testUser.getId().toString());
    }

    // ========== ACCEPT ITEM TESTS ==========

    @Test
    public void testAcceptItem_Success_CreatesBioSample() {
        // Arrange
        String externalId = "XFER-ACCEPT-" + System.currentTimeMillis();
        SampleItem sampleItem = createTestSampleItem(testSample, externalId);
        Integer sampleItemId = Integer.valueOf(sampleItem.getId());

        SampleTransferRequest request = transferService.createTransferRequest("Hematology", Arrays.asList(sampleItemId),
                "Transfer for acceptance", testUser.getId().toString());
        Integer itemId = request.getItems().get(0).getId();

        // Verify no BioSample exists before acceptance
        assertFalse("BioSample should not exist before acceptance",
                bioSampleService.existsBySampleItemId(sampleItemId));

        BioSample bioSampleInput = new BioSample();
        bioSampleInput.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        bioSampleInput.setEthicsApprovalRef("ETH-ACCEPT-001");
        bioSampleInput.setPrincipalInvestigator("Dr. Test");

        // Act
        SampleTransferItem acceptedItem = transferService.acceptItem(itemId, bioSampleInput,
                testUser.getId().toString());

        // Assert - item status
        assertEquals("Item status should be ACCEPTED", ItemStatus.ACCEPTED, acceptedItem.getStatus());
        assertNull("Item rejection reason should be null", acceptedItem.getRejectionReason());

        // Assert - BioSample was created with correct values
        BioSample createdBioSample = acceptedItem.getBioSample();
        assertNotNull("BioSample should be created", createdBioSample);
        assertNotNull("BioSample should have ID", createdBioSample.getId());
        assertEquals("BioSample biosafety level should be BSL_2", BiosafetyLevel.BSL_2,
                createdBioSample.getBiosafetyLevel());
        assertEquals("BioSample ethics ref should match", "ETH-ACCEPT-001", createdBioSample.getEthicsApprovalRef());
        assertEquals("BioSample PI should match", "Dr. Test", createdBioSample.getPrincipalInvestigator());
        assertEquals("BioSample should link to correct SampleItem", sampleItemId,
                Integer.valueOf(createdBioSample.getSampleItem().getId()));

        // Assert - BioSample can be retrieved independently
        assertTrue("BioSample should exist for SampleItem", bioSampleService.existsBySampleItemId(sampleItemId));
        BioSample retrievedBioSample = bioSampleService.getBySampleItemId(sampleItemId);
        assertEquals("Retrieved BioSample ID should match", createdBioSample.getId(), retrievedBioSample.getId());

        // Assert - request status updated (single item request should be ACCEPTED)
        SampleTransferRequest updatedRequest = transferService.get(request.getId());
        assertEquals("Request status should be ACCEPTED", TransferStatus.ACCEPTED, updatedRequest.getStatus());
        assertNotNull("Processed timestamp should be set", updatedRequest.getProcessedTimestamp());
        assertEquals("Accepted count should be 1", 1, updatedRequest.getAcceptedItemCount());
        assertEquals("Rejected count should be 0", 0, updatedRequest.getRejectedItemCount());
    }

    @Test
    public void testAcceptItem_PartialAcceptance_RequestStaysPending() {
        // Arrange - create request with 2 items
        SampleItem item1 = createTestSampleItem(testSample, "XFER-PART1-" + System.currentTimeMillis());
        SampleItem item2 = createTestSampleItem(testSample, "XFER-PART2-" + System.currentTimeMillis());
        SampleTransferRequest request = transferService.createTransferRequest("Chemistry",
                Arrays.asList(Integer.valueOf(item1.getId()), Integer.valueOf(item2.getId())), "Partial transfer",
                testUser.getId().toString());

        Integer itemId1 = request.getItems().get(0).getId();

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);

        // Act - accept only first item
        transferService.acceptItem(itemId1, bioSample, testUser.getId().toString());

        // Assert - request should still be PENDING (not all items processed)
        SampleTransferRequest updatedRequest = transferService.get(request.getId());
        assertEquals("Request status should be PENDING (not all items processed)", TransferStatus.PENDING,
                updatedRequest.getStatus());
        assertEquals("Accepted count should be 1", 1, updatedRequest.getAcceptedItemCount());
        assertEquals("Rejected count should be 0", 0, updatedRequest.getRejectedItemCount());
        assertNull("Processed timestamp should still be null", updatedRequest.getProcessedTimestamp());
    }

    @Test(expected = IllegalStateException.class)
    public void testAcceptItem_AlreadyAccepted_ThrowsException() {
        // Arrange
        SampleItem sampleItem = createTestSampleItem(testSample, "XFER-AACC-" + System.currentTimeMillis());
        SampleTransferRequest request = transferService.createTransferRequest("Lab",
                Arrays.asList(Integer.valueOf(sampleItem.getId())), "Already accepted test",
                testUser.getId().toString());
        Integer itemId = request.getItems().get(0).getId();

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        transferService.acceptItem(itemId, bioSample, testUser.getId().toString());

        // Act - try to accept again
        transferService.acceptItem(itemId, bioSample, testUser.getId().toString());
    }

    // ========== REJECT ITEM TESTS ==========

    @Test
    public void testRejectItem_Success_NoBioSampleCreated() {
        // Arrange
        String externalId = "XFER-REJECT-" + System.currentTimeMillis();
        SampleItem sampleItem = createTestSampleItem(testSample, externalId);
        Integer sampleItemId = Integer.valueOf(sampleItem.getId());

        SampleTransferRequest request = transferService.createTransferRequest("Microbiology",
                Arrays.asList(sampleItemId), "Transfer for rejection", testUser.getId().toString());
        Integer itemId = request.getItems().get(0).getId();
        String rejectionReason = "Sample does not meet biosafety requirements";

        // Act
        SampleTransferItem rejectedItem = transferService.rejectItem(itemId, rejectionReason,
                testUser.getId().toString());

        // Assert - item status
        assertEquals("Item status should be REJECTED", ItemStatus.REJECTED, rejectedItem.getStatus());
        assertEquals("Rejection reason should match exactly", rejectionReason, rejectedItem.getRejectionReason());
        assertNull("BioSample should NOT be created for rejected item", rejectedItem.getBioSample());

        // Assert - no BioSample exists in database
        assertFalse("BioSample should not exist for rejected sample",
                bioSampleService.existsBySampleItemId(sampleItemId));

        // Assert - request status updated
        SampleTransferRequest updatedRequest = transferService.get(request.getId());
        assertEquals("Request status should be REJECTED", TransferStatus.REJECTED, updatedRequest.getStatus());
        assertEquals("Accepted count should be 0", 0, updatedRequest.getAcceptedItemCount());
        assertEquals("Rejected count should be 1", 1, updatedRequest.getRejectedItemCount());
    }

    @Test(expected = IllegalStateException.class)
    public void testRejectItem_AlreadyRejected_ThrowsException() {
        // Arrange
        SampleItem sampleItem = createTestSampleItem(testSample, "XFER-AREJ-" + System.currentTimeMillis());
        SampleTransferRequest request = transferService.createTransferRequest("Lab",
                Arrays.asList(Integer.valueOf(sampleItem.getId())), "Already rejected test",
                testUser.getId().toString());
        Integer itemId = request.getItems().get(0).getId();

        transferService.rejectItem(itemId, "First rejection", testUser.getId().toString());

        // Act - try to reject again
        transferService.rejectItem(itemId, "Second rejection", testUser.getId().toString());
    }

    // ========== ACCEPT ALL TESTS ==========

    @Test
    public void testAcceptAll_Success_AllBioSamplesCreated() {
        // Arrange
        String ext1 = "XFER-ALL1-" + System.currentTimeMillis();
        String ext2 = "XFER-ALL2-" + System.currentTimeMillis();
        SampleItem item1 = createTestSampleItem(testSample, ext1);
        SampleItem item2 = createTestSampleItem(testSample, ext2);
        Integer itemId1 = Integer.valueOf(item1.getId());
        Integer itemId2 = Integer.valueOf(item2.getId());

        SampleTransferRequest request = transferService.createTransferRequest("Immunology",
                Arrays.asList(itemId1, itemId2), "Bulk accept transfer", testUser.getId().toString());

        BioSample template = new BioSample();
        template.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        template.setEthicsApprovalRef("ETH-BULK-001");
        template.setPrincipalInvestigator("Dr. Smith");

        // Act
        SampleTransferRequest result = transferService.acceptAll(request.getId(), template,
                testUser.getId().toString());

        // Assert - request status
        assertEquals("Request status should be ACCEPTED", TransferStatus.ACCEPTED, result.getStatus());
        assertEquals("Accepted count should be 2", 2, result.getAcceptedItemCount());
        assertEquals("Rejected count should be 0", 0, result.getRejectedItemCount());
        assertNotNull("Processed timestamp should be set", result.getProcessedTimestamp());

        // Assert - all items accepted with correct BioSamples
        for (SampleTransferItem item : result.getItems()) {
            assertEquals("Item status should be ACCEPTED", ItemStatus.ACCEPTED, item.getStatus());
            assertNotNull("BioSample should be created", item.getBioSample());
            assertEquals("BioSample biosafety level should match template", BiosafetyLevel.BSL_2,
                    item.getBioSample().getBiosafetyLevel());
            assertEquals("BioSample ethics ref should match template", "ETH-BULK-001",
                    item.getBioSample().getEthicsApprovalRef());
            assertEquals("BioSample PI should match template", "Dr. Smith",
                    item.getBioSample().getPrincipalInvestigator());
        }

        // Assert - BioSamples exist in database
        assertTrue("BioSample should exist for item 1", bioSampleService.existsBySampleItemId(itemId1));
        assertTrue("BioSample should exist for item 2", bioSampleService.existsBySampleItemId(itemId2));
    }

    @Test(expected = IllegalStateException.class)
    public void testAcceptAll_NoPendingItems_ThrowsException() {
        // Arrange - create and accept all items first
        SampleItem sampleItem = createTestSampleItem(testSample, "XFER-NOPEND-" + System.currentTimeMillis());
        SampleTransferRequest request = transferService.createTransferRequest("Lab",
                Arrays.asList(Integer.valueOf(sampleItem.getId())), "No pending test", testUser.getId().toString());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        transferService.acceptAll(request.getId(), bioSample, testUser.getId().toString());

        // Act - try to accept all again
        transferService.acceptAll(request.getId(), bioSample, testUser.getId().toString());
    }

    // ========== REJECT ALL TESTS ==========

    @Test
    public void testRejectAll_Success_NoBioSamplesCreated() {
        // Arrange
        SampleItem item1 = createTestSampleItem(testSample, "XFER-RALL1-" + System.currentTimeMillis());
        SampleItem item2 = createTestSampleItem(testSample, "XFER-RALL2-" + System.currentTimeMillis());
        Integer itemId1 = Integer.valueOf(item1.getId());
        Integer itemId2 = Integer.valueOf(item2.getId());

        SampleTransferRequest request = transferService.createTransferRequest("Other Lab",
                Arrays.asList(itemId1, itemId2), "Bulk reject transfer", testUser.getId().toString());

        String rejectionReason = "Insufficient documentation for all samples";

        // Act
        SampleTransferRequest result = transferService.rejectAll(request.getId(), rejectionReason,
                testUser.getId().toString());

        // Assert - request status
        assertEquals("Request status should be REJECTED", TransferStatus.REJECTED, result.getStatus());
        assertEquals("Accepted count should be 0", 0, result.getAcceptedItemCount());
        assertEquals("Rejected count should be 2", 2, result.getRejectedItemCount());
        assertEquals("Request rejection reason should match", rejectionReason, result.getRejectionReason());

        // Assert - all items rejected
        for (SampleTransferItem item : result.getItems()) {
            assertEquals("Item status should be REJECTED", ItemStatus.REJECTED, item.getStatus());
            assertEquals("Item rejection reason should match", rejectionReason, item.getRejectionReason());
            assertNull("BioSample should NOT be created", item.getBioSample());
        }

        // Assert - no BioSamples in database
        assertFalse("BioSample should not exist for item 1", bioSampleService.existsBySampleItemId(itemId1));
        assertFalse("BioSample should not exist for item 2", bioSampleService.existsBySampleItemId(itemId2));
    }

    // ========== CANCEL REQUEST TESTS ==========

    @Test
    public void testCancelRequest_Success() {
        // Arrange
        SampleItem sampleItem = createTestSampleItem(testSample, "XFER-CANCEL-" + System.currentTimeMillis());
        Integer sampleItemId = Integer.valueOf(sampleItem.getId());
        SampleTransferRequest request = transferService.createTransferRequest("Source Lab", Arrays.asList(sampleItemId),
                "Transfer to cancel", testUser.getId().toString());

        // Verify pending transfer exists
        assertTrue("Should have pending transfer before cancel", transferService.hasPendingTransfer(sampleItemId));

        // Act
        SampleTransferRequest result = transferService.cancelRequest(request.getId(), testUser.getId().toString());

        // Assert
        assertEquals("Request status should be CANCELLED", TransferStatus.CANCELLED, result.getStatus());
        assertNotNull("Processed timestamp should be set", result.getProcessedTimestamp());

        // Verify pending transfer no longer exists (item is now rejected due to cancel)
        assertFalse("Should not have pending transfer after cancel", transferService.hasPendingTransfer(sampleItemId));

        // Verify no BioSample created
        assertFalse("BioSample should not exist", bioSampleService.existsBySampleItemId(sampleItemId));
    }

    @Test(expected = IllegalStateException.class)
    public void testCancelRequest_AlreadyAccepted_ThrowsException() {
        // Arrange - create and accept a request
        SampleItem sampleItem = createTestSampleItem(testSample, "XFER-CANCEL2-" + System.currentTimeMillis());
        SampleTransferRequest request = transferService.createTransferRequest("Lab X",
                Arrays.asList(Integer.valueOf(sampleItem.getId())), "Transfer to accept then cancel",
                testUser.getId().toString());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        transferService.acceptAll(request.getId(), bioSample, testUser.getId().toString());

        // Act - should throw exception
        transferService.cancelRequest(request.getId(), testUser.getId().toString());
    }

    // ========== QUERY TESTS ==========

    @Test
    public void testGetPendingRequests_ReturnsOnlyPending() {
        // Arrange - create pending requests with unique source lab
        String uniqueLab = "PendingTestLab-" + System.currentTimeMillis();
        SampleItem item1 = createTestSampleItem(testSample, "XFER-PEND1-" + System.currentTimeMillis());
        SampleItem item2 = createTestSampleItem(testSample, "XFER-PEND2-" + System.currentTimeMillis());
        transferService.createTransferRequest(uniqueLab, Arrays.asList(Integer.valueOf(item1.getId())), "Pending 1",
                testUser.getId().toString());
        transferService.createTransferRequest(uniqueLab, Arrays.asList(Integer.valueOf(item2.getId())), "Pending 2",
                testUser.getId().toString());

        // Act
        List<SampleTransferRequest> pending = transferService.getPendingRequests(100);

        // Assert - all returned should be PENDING
        for (SampleTransferRequest req : pending) {
            assertEquals("All returned requests must have PENDING status", TransferStatus.PENDING, req.getStatus());
        }

        // Find our specific requests by source lab and verify they're in the list
        long ourPendingCount = pending.stream().filter(r -> uniqueLab.equals(r.getSourceLab())).count();
        assertEquals("Should find exactly 2 pending requests from our test lab", 2, ourPendingCount);
    }

    @Test
    public void testGetBySourceLab_ReturnsOnlyFromSpecifiedLab() {
        // Arrange - create requests from different labs
        String uniqueSourceLab = "UniqueSourceLab-" + System.currentTimeMillis();
        String otherLab = "OtherLab-" + System.currentTimeMillis();

        SampleItem item1 = createTestSampleItem(testSample, "XFER-SRC1-" + System.currentTimeMillis());
        SampleItem item2 = createTestSampleItem(testSample, "XFER-SRC2-" + System.currentTimeMillis());
        SampleItem item3 = createTestSampleItem(testSample, "XFER-SRC3-" + System.currentTimeMillis());

        transferService.createTransferRequest(uniqueSourceLab, Arrays.asList(Integer.valueOf(item1.getId())),
                "From unique lab 1", testUser.getId().toString());
        transferService.createTransferRequest(uniqueSourceLab, Arrays.asList(Integer.valueOf(item2.getId())),
                "From unique lab 2", testUser.getId().toString());
        transferService.createTransferRequest(otherLab, Arrays.asList(Integer.valueOf(item3.getId())), "From other lab",
                testUser.getId().toString());

        // Act
        List<SampleTransferRequest> fromUniqueLab = transferService.getBySourceLab(uniqueSourceLab);
        List<SampleTransferRequest> fromOtherLab = transferService.getBySourceLab(otherLab);

        // Assert - verify exact source lab matches
        assertEquals("Should find exactly 2 requests from unique lab", 2, fromUniqueLab.size());
        for (SampleTransferRequest req : fromUniqueLab) {
            assertEquals("Source lab must match exactly", uniqueSourceLab, req.getSourceLab());
        }

        assertEquals("Should find exactly 1 request from other lab", 1, fromOtherLab.size());
        assertEquals("Source lab must match exactly", otherLab, fromOtherLab.get(0).getSourceLab());
    }

    @Test
    public void testHasPendingTransfer_AccurateTracking() {
        // Arrange
        SampleItem sampleItem = createTestSampleItem(testSample, "XFER-HASPEND-" + System.currentTimeMillis());
        Integer sampleItemId = Integer.valueOf(sampleItem.getId());

        // Assert - initially no pending transfer
        assertFalse("Should have no pending transfer initially", transferService.hasPendingTransfer(sampleItemId));

        // Create pending transfer
        SampleTransferRequest request = transferService.createTransferRequest("Test Lab", Arrays.asList(sampleItemId),
                "Pending transfer", testUser.getId().toString());

        // Assert - now has pending transfer
        assertTrue("Should have pending transfer after creation", transferService.hasPendingTransfer(sampleItemId));

        // Accept the transfer
        Integer itemId = request.getItems().get(0).getId();
        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        transferService.acceptItem(itemId, bioSample, testUser.getId().toString());

        // Assert - no longer pending (accepted)
        assertFalse("Should not have pending transfer after acceptance",
                transferService.hasPendingTransfer(sampleItemId));
    }

    @Test
    public void testPartiallyAcceptedStatus_MixedDecisions() {
        // Arrange - create request with 2 items
        SampleItem item1 = createTestSampleItem(testSample, "XFER-PACC1-" + System.currentTimeMillis());
        SampleItem item2 = createTestSampleItem(testSample, "XFER-PACC2-" + System.currentTimeMillis());
        Integer itemId1 = Integer.valueOf(item1.getId());
        Integer itemId2 = Integer.valueOf(item2.getId());

        SampleTransferRequest request = transferService.createTransferRequest("Mixed Lab",
                Arrays.asList(itemId1, itemId2), "Partial acceptance test", testUser.getId().toString());

        Integer transferItemId1 = request.getItems().get(0).getId();
        Integer transferItemId2 = request.getItems().get(1).getId();

        // Act - accept first, reject second
        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        transferService.acceptItem(transferItemId1, bioSample, testUser.getId().toString());
        transferService.rejectItem(transferItemId2, "Does not meet criteria", testUser.getId().toString());

        // Assert - verify final state
        SampleTransferRequest result = transferService.get(request.getId());
        assertEquals("Request status should be PARTIALLY_ACCEPTED", TransferStatus.PARTIALLY_ACCEPTED,
                result.getStatus());
        assertEquals("Accepted count should be exactly 1", 1, result.getAcceptedItemCount());
        assertEquals("Rejected count should be exactly 1", 1, result.getRejectedItemCount());
        assertNotNull("Processed timestamp should be set", result.getProcessedTimestamp());

        // Verify correct item was accepted/rejected
        for (SampleTransferItem item : result.getItems()) {
            if (item.getId().equals(transferItemId1)) {
                assertEquals("First item should be ACCEPTED", ItemStatus.ACCEPTED, item.getStatus());
                assertNotNull("Accepted item should have BioSample", item.getBioSample());
            } else {
                assertEquals("Second item should be REJECTED", ItemStatus.REJECTED, item.getStatus());
                assertNull("Rejected item should NOT have BioSample", item.getBioSample());
                assertEquals("Rejection reason should match", "Does not meet criteria", item.getRejectionReason());
            }
        }

        // Verify BioSample existence
        assertTrue("BioSample should exist for accepted item", bioSampleService.existsBySampleItemId(itemId1));
        assertFalse("BioSample should NOT exist for rejected item", bioSampleService.existsBySampleItemId(itemId2));
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
}
