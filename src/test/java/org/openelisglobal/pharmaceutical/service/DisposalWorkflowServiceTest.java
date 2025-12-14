package org.openelisglobal.pharmaceutical.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.pharmaceutical.dao.ChainOfCustodyEventDAO;
import org.openelisglobal.pharmaceutical.dao.DisposalRecordDAO;
import org.openelisglobal.pharmaceutical.dao.PharmaceuticalSampleDAO;
import org.openelisglobal.pharmaceutical.valueholder.ChainOfCustodyEvent;
import org.openelisglobal.pharmaceutical.valueholder.DisposalRecord;
import org.openelisglobal.pharmaceutical.valueholder.DisposalRecord.DisposalMethod;
import org.openelisglobal.pharmaceutical.valueholder.DisposalRecord.DisposalReason;
import org.openelisglobal.pharmaceutical.valueholder.DisposalRecord.DisposalStatus;
import org.openelisglobal.pharmaceutical.valueholder.PharmaceuticalSample;
import org.openelisglobal.pharmaceutical.valueholder.PharmaceuticalSample.SampleStatus;

/**
 * Unit tests for DisposalWorkflowService. Tests disposal request creation,
 * approval flow, rejection, execution, and certificate generation.
 */
@RunWith(MockitoJUnitRunner.class)
public class DisposalWorkflowServiceTest {

    @Mock
    private DisposalRecordDAO disposalRecordDAO;

    @Mock
    private PharmaceuticalSampleDAO pharmaceuticalSampleDAO;

    @Mock
    private ChainOfCustodyEventDAO chainOfCustodyEventDAO;

    @InjectMocks
    private DisposalWorkflowServiceImpl disposalService;

    private PharmaceuticalSample testSample;
    private DisposalRecord pendingDisposal;
    private DisposalRecord approvedDisposal;

    @Before
    public void setUp() {
        testSample = new PharmaceuticalSample();
        testSample.setId(1);
        testSample.setUniqueSampleId("PHARMA-2025-00001");
        testSample.setSampleName("Test Drug A");
        testSample.setStatus(SampleStatus.IN_STORAGE);

        pendingDisposal = new DisposalRecord();
        pendingDisposal.setId(10);
        pendingDisposal.setSampleId(1);
        pendingDisposal.setReason(DisposalReason.EXPIRED);
        pendingDisposal.setMethod(DisposalMethod.INCINERATION);
        pendingDisposal.setStatus(DisposalStatus.PENDING_APPROVAL);
        pendingDisposal.setRequestedBy("user1");
        pendingDisposal.setRequestedAt(new Timestamp(System.currentTimeMillis()));
        pendingDisposal.setSysUserId("user1");

        approvedDisposal = new DisposalRecord();
        approvedDisposal.setId(11);
        approvedDisposal.setSampleId(1);
        approvedDisposal.setReason(DisposalReason.EXPIRED);
        approvedDisposal.setMethod(DisposalMethod.INCINERATION);
        approvedDisposal.setStatus(DisposalStatus.APPROVED);
        approvedDisposal.setRequestedBy("user1");
        approvedDisposal.setApprovedBy("approver1");
        approvedDisposal.setApprovedAt(new Timestamp(System.currentTimeMillis()));
        approvedDisposal.setSysUserId("approver1");
    }

    // ==================== Request Disposal Tests ====================

    @Test
    public void testRequestDisposal_CreatesRecordWithPendingStatus() {
        // Arrange
        when(pharmaceuticalSampleDAO.get(1)).thenReturn(Optional.of(testSample));
        when(disposalRecordDAO.findBySampleId(1)).thenReturn(Collections.emptyList());
        when(disposalRecordDAO.insert(any(DisposalRecord.class))).thenReturn(10);
        when(chainOfCustodyEventDAO.insert(any(ChainOfCustodyEvent.class))).thenReturn(1);

        // Act
        DisposalRecord result = disposalService.requestDisposal(
                1,
                DisposalReason.EXPIRED,
                DisposalMethod.INCINERATION,
                "Sample expired on 2025-01-01",
                "user1");

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Status should be PENDING_APPROVAL", DisposalStatus.PENDING_APPROVAL, result.getStatus());
        assertEquals("Reason should match", DisposalReason.EXPIRED, result.getReason());
        assertEquals("Method should match", DisposalMethod.INCINERATION, result.getMethod());
        assertNotNull("Requested at should be set", result.getRequestedAt());
        assertEquals("Requested by should match", "user1", result.getRequestedBy());
    }

    @Test
    public void testRequestDisposal_UpdatesSampleStatusToPendingDisposal() {
        // Arrange
        when(pharmaceuticalSampleDAO.get(1)).thenReturn(Optional.of(testSample));
        when(disposalRecordDAO.findBySampleId(1)).thenReturn(Collections.emptyList());
        when(disposalRecordDAO.insert(any(DisposalRecord.class))).thenReturn(10);
        when(chainOfCustodyEventDAO.insert(any(ChainOfCustodyEvent.class))).thenReturn(1);

        // Act
        disposalService.requestDisposal(1, DisposalReason.EXPIRED, DisposalMethod.INCINERATION, "Expired", "user1");

        // Assert
        ArgumentCaptor<PharmaceuticalSample> sampleCaptor = ArgumentCaptor.forClass(PharmaceuticalSample.class);
        verify(pharmaceuticalSampleDAO).update(sampleCaptor.capture());
        assertEquals("Sample status should be PENDING_DISPOSAL",
                SampleStatus.PENDING_DISPOSAL, sampleCaptor.getValue().getStatus());
    }

    @Test(expected = org.openelisglobal.common.exception.LIMSRuntimeException.class)
    public void testRequestDisposal_ThrowsExceptionForSampleInTesting() {
        // Arrange
        testSample.setStatus(SampleStatus.IN_TESTING);
        when(pharmaceuticalSampleDAO.get(1)).thenReturn(Optional.of(testSample));
        when(disposalRecordDAO.findBySampleId(1)).thenReturn(Collections.emptyList());

        // Act - should throw exception
        disposalService.requestDisposal(1, DisposalReason.EXPIRED, DisposalMethod.INCINERATION, "Expired", "user1");
    }

    @Test(expected = org.openelisglobal.common.exception.LIMSRuntimeException.class)
    public void testRequestDisposal_ThrowsExceptionForNonExistentSample() {
        // Arrange
        when(pharmaceuticalSampleDAO.get(999)).thenReturn(Optional.empty());
        when(disposalRecordDAO.findBySampleId(999)).thenReturn(Collections.emptyList());

        // Act - should throw exception
        disposalService.requestDisposal(999, DisposalReason.EXPIRED, DisposalMethod.INCINERATION, "Expired", "user1");
    }

    // ==================== Approval Tests ====================

    @Test
    public void testApproveDisposal_UpdatesStatusToApproved() {
        // Arrange
        when(disposalRecordDAO.get(10)).thenReturn(Optional.of(pendingDisposal));
        when(chainOfCustodyEventDAO.insert(any(ChainOfCustodyEvent.class))).thenReturn(1);

        // Act
        DisposalRecord result = disposalService.approveDisposal(10, "approver1");

        // Assert
        assertEquals("Status should be APPROVED", DisposalStatus.APPROVED, result.getStatus());
        assertNotNull("Approved at should be set", result.getApprovedAt());
        assertEquals("Approved by should match", "approver1", result.getApprovedBy());
        verify(disposalRecordDAO).update(any(DisposalRecord.class));
    }

    @Test(expected = org.openelisglobal.common.exception.LIMSRuntimeException.class)
    public void testApproveDisposal_ThrowsExceptionForNonPendingRecord() {
        // Arrange
        pendingDisposal.setStatus(DisposalStatus.COMPLETED);
        when(disposalRecordDAO.get(10)).thenReturn(Optional.of(pendingDisposal));

        // Act - should throw exception
        disposalService.approveDisposal(10, "approver1");
    }

    // ==================== Rejection Tests ====================

    @Test
    public void testRejectDisposal_UpdatesStatusToRejected() {
        // Arrange
        when(disposalRecordDAO.get(10)).thenReturn(Optional.of(pendingDisposal));
        when(pharmaceuticalSampleDAO.get(1)).thenReturn(Optional.of(testSample));

        // Act
        DisposalRecord result = disposalService.rejectDisposal(10, "Insufficient documentation", "approver1");

        // Assert
        assertEquals("Status should be REJECTED", DisposalStatus.REJECTED, result.getStatus());
        assertTrue("Justification should contain rejection reason",
                result.getJustification().contains("Rejection reason"));
        verify(disposalRecordDAO).update(any(DisposalRecord.class));
    }

    @Test
    public void testRejectDisposal_RestoresSampleStatusToInStorage() {
        // Arrange
        testSample.setStatus(SampleStatus.PENDING_DISPOSAL);
        when(disposalRecordDAO.get(10)).thenReturn(Optional.of(pendingDisposal));
        when(pharmaceuticalSampleDAO.get(1)).thenReturn(Optional.of(testSample));

        // Act
        disposalService.rejectDisposal(10, "Insufficient documentation", "approver1");

        // Assert
        ArgumentCaptor<PharmaceuticalSample> sampleCaptor = ArgumentCaptor.forClass(PharmaceuticalSample.class);
        verify(pharmaceuticalSampleDAO).update(sampleCaptor.capture());
        assertEquals("Sample status should be IN_STORAGE", SampleStatus.IN_STORAGE,
                sampleCaptor.getValue().getStatus());
    }

    // ==================== Execution Tests ====================

    @Test
    public void testExecuteDisposal_UpdatesStatusToCompleted() {
        // Arrange
        when(disposalRecordDAO.get(11)).thenReturn(Optional.of(approvedDisposal));
        when(pharmaceuticalSampleDAO.get(1)).thenReturn(Optional.of(testSample));
        when(chainOfCustodyEventDAO.insert(any(ChainOfCustodyEvent.class))).thenReturn(1);

        // Act
        DisposalRecord result = disposalService.executeDisposal(11, "witness1", "Disposed properly", "executor1");

        // Assert
        assertEquals("Status should be COMPLETED", DisposalStatus.COMPLETED, result.getStatus());
        assertNotNull("Executed at should be set", result.getExecutedAt());
        assertEquals("Executed by should match", "executor1", result.getExecutedBy());
        assertEquals("Witness ID should match", "witness1", result.getWitnessId());
    }

    @Test
    public void testExecuteDisposal_UpdatesSampleStatusToDisposed() {
        // Arrange
        when(disposalRecordDAO.get(11)).thenReturn(Optional.of(approvedDisposal));
        when(pharmaceuticalSampleDAO.get(1)).thenReturn(Optional.of(testSample));
        when(chainOfCustodyEventDAO.insert(any(ChainOfCustodyEvent.class))).thenReturn(1);

        // Act
        disposalService.executeDisposal(11, "witness1", "Disposed properly", "executor1");

        // Assert
        ArgumentCaptor<PharmaceuticalSample> sampleCaptor = ArgumentCaptor.forClass(PharmaceuticalSample.class);
        verify(pharmaceuticalSampleDAO).update(sampleCaptor.capture());
        assertEquals("Sample status should be DISPOSED",
                SampleStatus.DISPOSED, sampleCaptor.getValue().getStatus());
    }

    @Test(expected = org.openelisglobal.common.exception.LIMSRuntimeException.class)
    public void testExecuteDisposal_ThrowsExceptionForPendingRecord() {
        // Arrange
        when(disposalRecordDAO.get(10)).thenReturn(Optional.of(pendingDisposal));

        // Act - should throw exception
        disposalService.executeDisposal(10, "witness1", "Notes", "executor1");
    }

    @Test
    public void testExecuteDisposal_AllowsExecutionOfScheduledDisposal() {
        // Arrange
        approvedDisposal.setStatus(DisposalStatus.SCHEDULED);
        when(disposalRecordDAO.get(11)).thenReturn(Optional.of(approvedDisposal));
        when(pharmaceuticalSampleDAO.get(1)).thenReturn(Optional.of(testSample));
        when(chainOfCustodyEventDAO.insert(any(ChainOfCustodyEvent.class))).thenReturn(1);

        // Act
        DisposalRecord result = disposalService.executeDisposal(11, "witness1", "Notes", "executor1");

        // Assert
        assertEquals("Status should be COMPLETED", DisposalStatus.COMPLETED, result.getStatus());
    }

    // ==================== Scheduling Tests ====================

    @Test
    public void testScheduleDisposal_UpdatesStatusToScheduled() {
        // Arrange
        Timestamp scheduledDate = new Timestamp(System.currentTimeMillis() + 86400000);
        when(disposalRecordDAO.get(11)).thenReturn(Optional.of(approvedDisposal));

        // Act
        DisposalRecord result = disposalService.scheduleDisposal(11, scheduledDate, "user1");

        // Assert
        assertEquals("Status should be SCHEDULED", DisposalStatus.SCHEDULED, result.getStatus());
        assertEquals("Scheduled at should match", scheduledDate, result.getScheduledAt());
    }

    @Test(expected = org.openelisglobal.common.exception.LIMSRuntimeException.class)
    public void testScheduleDisposal_ThrowsExceptionForPendingRecord() {
        // Arrange
        Timestamp scheduledDate = new Timestamp(System.currentTimeMillis() + 86400000);
        when(disposalRecordDAO.get(10)).thenReturn(Optional.of(pendingDisposal));

        // Act - should throw exception
        disposalService.scheduleDisposal(10, scheduledDate, "user1");
    }

    // ==================== Can Request Disposal Tests ====================

    @Test
    public void testCanRequestDisposal_ReturnsTrueForStoredSample() {
        // Arrange
        testSample.setStatus(SampleStatus.IN_STORAGE);
        when(pharmaceuticalSampleDAO.get(1)).thenReturn(Optional.of(testSample));
        when(disposalRecordDAO.findBySampleId(1)).thenReturn(Collections.emptyList());

        // Act
        boolean result = disposalService.canRequestDisposal(1);

        // Assert
        assertTrue("Should allow disposal request for stored sample", result);
    }

    @Test
    public void testCanRequestDisposal_ReturnsFalseForSampleInTesting() {
        // Arrange
        testSample.setStatus(SampleStatus.IN_TESTING);
        when(pharmaceuticalSampleDAO.get(1)).thenReturn(Optional.of(testSample));
        when(disposalRecordDAO.findBySampleId(1)).thenReturn(Collections.emptyList());

        // Act
        boolean result = disposalService.canRequestDisposal(1);

        // Assert
        assertFalse("Should not allow disposal request for sample in testing", result);
    }

    @Test
    public void testCanRequestDisposal_ReturnsFalseForSamplePendingDisposal() {
        // Arrange
        testSample.setStatus(SampleStatus.PENDING_DISPOSAL);
        when(pharmaceuticalSampleDAO.get(1)).thenReturn(Optional.of(testSample));
        when(disposalRecordDAO.findBySampleId(1)).thenReturn(Collections.emptyList());

        // Act
        boolean result = disposalService.canRequestDisposal(1);

        // Assert
        assertFalse("Should not allow disposal request for sample pending disposal", result);
    }

    @Test
    public void testCanRequestDisposal_ReturnsFalseForExistingPendingRecord() {
        // Arrange
        testSample.setStatus(SampleStatus.IN_STORAGE);
        when(pharmaceuticalSampleDAO.get(1)).thenReturn(Optional.of(testSample));
        when(disposalRecordDAO.findBySampleId(1)).thenReturn(Arrays.asList(pendingDisposal));

        // Act
        boolean result = disposalService.canRequestDisposal(1);

        // Assert
        assertFalse("Should not allow disposal request when pending record exists", result);
    }

    @Test
    public void testCanRequestDisposal_ReturnsFalseForNonExistentSample() {
        // Arrange
        when(pharmaceuticalSampleDAO.get(999)).thenReturn(Optional.empty());

        // Act
        boolean result = disposalService.canRequestDisposal(999);

        // Assert
        assertFalse("Should return false for non-existent sample", result);
    }

    // ==================== Certificate Generation Tests ====================

    @Test
    public void testGenerateDisposalCertificate_GeneratesValidCertificate() {
        // Arrange
        DisposalRecord completedDisposal = new DisposalRecord();
        completedDisposal.setId(12);
        completedDisposal.setSampleId(1);
        completedDisposal.setReason(DisposalReason.EXPIRED);
        completedDisposal.setMethod(DisposalMethod.INCINERATION);
        completedDisposal.setStatus(DisposalStatus.COMPLETED);
        completedDisposal.setRequestedBy("user1");
        completedDisposal.setRequestedAt(new Timestamp(System.currentTimeMillis()));
        completedDisposal.setApprovedBy("approver1");
        completedDisposal.setApprovedAt(new Timestamp(System.currentTimeMillis()));
        completedDisposal.setExecutedBy("executor1");
        completedDisposal.setExecutedAt(new Timestamp(System.currentTimeMillis()));
        completedDisposal.setWitnessId("witness1");

        when(disposalRecordDAO.get(12)).thenReturn(Optional.of(completedDisposal));
        when(pharmaceuticalSampleDAO.get(1)).thenReturn(Optional.of(testSample));

        // Act
        String certificate = disposalService.generateDisposalCertificate(12);

        // Assert
        assertNotNull("Certificate should not be null", certificate);
        assertTrue("Certificate should contain header", certificate.contains("DISPOSAL CERTIFICATE"));
        assertTrue("Certificate should contain disposal ID", certificate.contains("DISP-12"));
        assertTrue("Certificate should contain sample info", certificate.contains(testSample.getSampleName()));
        assertTrue("Certificate should contain method", certificate.contains("INCINERATION"));
        assertTrue("Certificate should contain witness", certificate.contains("witness1"));
    }

    @Test(expected = org.openelisglobal.common.exception.LIMSRuntimeException.class)
    public void testGenerateDisposalCertificate_ThrowsExceptionForIncompleteDisposal() {
        // Arrange
        when(disposalRecordDAO.get(10)).thenReturn(Optional.of(pendingDisposal));

        // Act - should throw exception
        disposalService.generateDisposalCertificate(10);
    }

    // ==================== Query Tests ====================

    @Test
    public void testFindPendingApprovals_ReturnsPendingRecords() {
        // Arrange
        List<DisposalRecord> pendingRecords = Arrays.asList(pendingDisposal);
        when(disposalRecordDAO.findPendingApprovals()).thenReturn(pendingRecords);

        // Act
        List<DisposalRecord> result = disposalService.findPendingApprovals();

        // Assert
        assertEquals("Should return 1 pending record", 1, result.size());
        assertEquals("Status should be PENDING_APPROVAL", DisposalStatus.PENDING_APPROVAL, result.get(0).getStatus());
    }
}
