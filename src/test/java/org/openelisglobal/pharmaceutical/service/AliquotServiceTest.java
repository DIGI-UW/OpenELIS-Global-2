package org.openelisglobal.pharmaceutical.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.openelisglobal.pharmaceutical.dao.AliquotDAO;
import org.openelisglobal.pharmaceutical.dao.ChainOfCustodyEventDAO;
import org.openelisglobal.pharmaceutical.dao.PharmaceuticalSampleDAO;
import org.openelisglobal.pharmaceutical.valueholder.Aliquot;
import org.openelisglobal.pharmaceutical.valueholder.Aliquot.AliquotStatus;
import org.openelisglobal.pharmaceutical.valueholder.ChainOfCustodyEvent;
import org.openelisglobal.pharmaceutical.valueholder.PharmaceuticalSample;

/**
 * Unit tests for AliquotService. Tests aliquot creation, freeze-thaw
 * enforcement, retrieval blocking, and status updates.
 */
@RunWith(MockitoJUnitRunner.class)
public class AliquotServiceTest {

    @Mock
    private AliquotDAO aliquotDAO;

    @Mock
    private PharmaceuticalSampleDAO pharmaceuticalSampleDAO;

    @Mock
    private ChainOfCustodyEventDAO chainOfCustodyEventDAO;

    @InjectMocks
    private AliquotServiceImpl aliquotService;

    private PharmaceuticalSample parentSample;
    private Aliquot testAliquot;

    @Before
    public void setUp() {
        parentSample = new PharmaceuticalSample();
        parentSample.setId(1);
        parentSample.setUniqueSampleId("PHARMA-2025-00001");
        parentSample.setSampleName("Test Drug A");

        testAliquot = new Aliquot();
        testAliquot.setId(10);
        testAliquot.setParentSampleId(1);
        testAliquot.setAliquotCode("ALQ-00001-001-ABCD");
        testAliquot.setBarcode("ALQ-00001-001-ABCD");
        testAliquot.setStatus(AliquotStatus.AVAILABLE);
        testAliquot.setFreezeThawCount(0);
        testAliquot.setFreezeThawLimit(5);
        testAliquot.setVolumeWeight(1.0);
        testAliquot.setVolumeUnit("mL");
        testAliquot.setSysUserId("user1");
    }

    // ==================== Aliquot Creation Tests ====================

    @Test
    public void testCreateAliquot_CreatesAliquotWithCorrectParent() {
        // Arrange
        Aliquot newAliquot = new Aliquot();
        newAliquot.setVolumeWeight(0.5);
        newAliquot.setVolumeUnit("mL");
        newAliquot.setFreezeThawLimit(5);

        when(pharmaceuticalSampleDAO.get(1)).thenReturn(Optional.of(parentSample));
        when(aliquotDAO.findByParentSampleId(1)).thenReturn(Collections.emptyList());
        when(aliquotDAO.insert(any(Aliquot.class))).thenReturn(10);
        when(chainOfCustodyEventDAO.insert(any(ChainOfCustodyEvent.class))).thenReturn(1);

        // Act
        Aliquot result = aliquotService.createAliquot(1, newAliquot, "user1");

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Parent sample ID should match", Integer.valueOf(1), result.getParentSampleId());
        assertEquals("Status should be AVAILABLE", AliquotStatus.AVAILABLE, result.getStatus());
        assertEquals("Freeze-thaw count should be 0", Integer.valueOf(0), result.getFreezeThawCount());
        assertNotNull("Aliquot code should be generated", result.getAliquotCode());
        assertNotNull("Barcode should be generated", result.getBarcode());
    }

    @Test
    public void testCreateAliquot_GeneratesCorrectSequenceNumber() {
        // Arrange
        Aliquot existingAliquot1 = new Aliquot();
        existingAliquot1.setId(10);
        Aliquot existingAliquot2 = new Aliquot();
        existingAliquot2.setId(11);
        List<Aliquot> existingAliquots = Arrays.asList(existingAliquot1, existingAliquot2);

        Aliquot newAliquot = new Aliquot();
        newAliquot.setFreezeThawLimit(5);

        when(pharmaceuticalSampleDAO.get(1)).thenReturn(Optional.of(parentSample));
        when(aliquotDAO.findByParentSampleId(1)).thenReturn(existingAliquots);
        when(aliquotDAO.insert(any(Aliquot.class))).thenReturn(12);
        when(chainOfCustodyEventDAO.insert(any(ChainOfCustodyEvent.class))).thenReturn(1);

        // Act
        Aliquot result = aliquotService.createAliquot(1, newAliquot, "user1");

        // Assert
        // Third aliquot should have sequence 003 in the barcode
        assertTrue("Aliquot code should contain sequence 003", result.getAliquotCode().contains("-003-"));
    }

    @Test
    public void testCreateAliquot_RecordsChainOfCustodyEvent() {
        // Arrange
        Aliquot newAliquot = new Aliquot();
        newAliquot.setFreezeThawLimit(5);

        when(pharmaceuticalSampleDAO.get(1)).thenReturn(Optional.of(parentSample));
        when(aliquotDAO.findByParentSampleId(1)).thenReturn(Collections.emptyList());
        when(aliquotDAO.insert(any(Aliquot.class))).thenReturn(10);
        when(chainOfCustodyEventDAO.insert(any(ChainOfCustodyEvent.class))).thenReturn(1);

        // Act
        aliquotService.createAliquot(1, newAliquot, "user1");

        // Assert
        ArgumentCaptor<ChainOfCustodyEvent> eventCaptor = ArgumentCaptor.forClass(ChainOfCustodyEvent.class);
        verify(chainOfCustodyEventDAO).insert(eventCaptor.capture());

        ChainOfCustodyEvent capturedEvent = eventCaptor.getValue();
        assertEquals("Action should be ALIQUOTED", ChainOfCustodyEvent.CustodyAction.ALIQUOTED,
                capturedEvent.getAction());
    }

    @Test(expected = org.openelisglobal.common.exception.LIMSRuntimeException.class)
    public void testCreateAliquot_ThrowsExceptionForNonExistentParent() {
        // Arrange
        Aliquot newAliquot = new Aliquot();
        when(pharmaceuticalSampleDAO.get(999)).thenReturn(Optional.empty());

        // Act - should throw exception
        aliquotService.createAliquot(999, newAliquot, "user1");
    }

    // ==================== Freeze-Thaw Tests ====================

    @Test
    public void testRecordFreezeThaw_IncrementsCount() {
        // Arrange
        testAliquot.setFreezeThawCount(2);
        when(aliquotDAO.get(10)).thenReturn(Optional.of(testAliquot));
        when(chainOfCustodyEventDAO.insert(any(ChainOfCustodyEvent.class))).thenReturn(1);

        // Act
        Aliquot result = aliquotService.recordFreezeThaw(10, "user2");

        // Assert
        assertEquals("Freeze-thaw count should be incremented to 3", Integer.valueOf(3), result.getFreezeThawCount());
        verify(aliquotDAO).update(any(Aliquot.class));
    }

    @Test
    public void testRecordFreezeThaw_MarksAsExhaustedWhenLimitExceeded() {
        // Arrange
        testAliquot.setFreezeThawCount(4); // At limit of 5
        testAliquot.setFreezeThawLimit(5);
        when(aliquotDAO.get(10)).thenReturn(Optional.of(testAliquot));
        when(chainOfCustodyEventDAO.insert(any(ChainOfCustodyEvent.class))).thenReturn(1);

        // Act
        Aliquot result = aliquotService.recordFreezeThaw(10, "user2");

        // Assert
        assertEquals("Freeze-thaw count should be 5", Integer.valueOf(5), result.getFreezeThawCount());
        assertEquals("Status should be EXHAUSTED", AliquotStatus.EXHAUSTED, result.getStatus());
        // Update called twice - once for count, once for status
        verify(aliquotDAO, times(2)).update(any(Aliquot.class));
    }

    @Test
    public void testRecordFreezeThaw_RecordsCustodyEvent() {
        // Arrange
        testAliquot.setFreezeThawCount(1);
        when(aliquotDAO.get(10)).thenReturn(Optional.of(testAliquot));
        when(chainOfCustodyEventDAO.insert(any(ChainOfCustodyEvent.class))).thenReturn(1);

        // Act
        aliquotService.recordFreezeThaw(10, "user2");

        // Assert
        ArgumentCaptor<ChainOfCustodyEvent> eventCaptor = ArgumentCaptor.forClass(ChainOfCustodyEvent.class);
        verify(chainOfCustodyEventDAO).insert(eventCaptor.capture());

        ChainOfCustodyEvent capturedEvent = eventCaptor.getValue();
        assertEquals("Action should be RETRIEVED", ChainOfCustodyEvent.CustodyAction.RETRIEVED,
                capturedEvent.getAction());
        assertTrue("Comments should mention freeze-thaw count",
                capturedEvent.getComments().contains("Freeze-thaw cycle recorded"));
    }

    @Test(expected = org.openelisglobal.common.exception.LIMSRuntimeException.class)
    public void testRecordFreezeThaw_ThrowsExceptionForNonExistentAliquot() {
        // Arrange
        when(aliquotDAO.get(999)).thenReturn(Optional.empty());

        // Act - should throw exception
        aliquotService.recordFreezeThaw(999, "user2");
    }

    // ==================== Freeze-Thaw Limit Check Tests ====================

    @Test
    public void testIsFreezeThawLimitExceeded_ReturnsFalseWhenUnderLimit() {
        // Arrange
        testAliquot.setFreezeThawCount(2);
        testAliquot.setFreezeThawLimit(5);
        when(aliquotDAO.get(10)).thenReturn(Optional.of(testAliquot));

        // Act
        boolean result = aliquotService.isFreezeThawLimitExceeded(10);

        // Assert
        assertFalse("Should return false when under limit", result);
    }

    @Test
    public void testIsFreezeThawLimitExceeded_ReturnsTrueWhenAtLimit() {
        // Arrange
        testAliquot.setFreezeThawCount(5);
        testAliquot.setFreezeThawLimit(5);
        when(aliquotDAO.get(10)).thenReturn(Optional.of(testAliquot));

        // Act
        boolean result = aliquotService.isFreezeThawLimitExceeded(10);

        // Assert
        assertTrue("Should return true when at limit", result);
    }

    @Test
    public void testIsFreezeThawLimitExceeded_ReturnsTrueWhenOverLimit() {
        // Arrange
        testAliquot.setFreezeThawCount(7);
        testAliquot.setFreezeThawLimit(5);
        when(aliquotDAO.get(10)).thenReturn(Optional.of(testAliquot));

        // Act
        boolean result = aliquotService.isFreezeThawLimitExceeded(10);

        // Assert
        assertTrue("Should return true when over limit", result);
    }

    // ==================== Status Update Tests ====================

    @Test
    public void testUpdateStatus_UpdatesStatusCorrectly() {
        // Arrange
        when(aliquotDAO.get(10)).thenReturn(Optional.of(testAliquot));
        when(chainOfCustodyEventDAO.insert(any(ChainOfCustodyEvent.class))).thenReturn(1);

        // Act
        Aliquot result = aliquotService.updateStatus(10, AliquotStatus.IN_USE, "user2");

        // Assert
        assertEquals("Status should be IN_USE", AliquotStatus.IN_USE, result.getStatus());
        assertEquals("Sys user ID should be updated", "user2", result.getSysUserId());
        verify(aliquotDAO).update(any(Aliquot.class));
    }

    @Test
    public void testUpdateStatus_RecordsCustodyEventWithStatusChange() {
        // Arrange
        testAliquot.setStatus(AliquotStatus.AVAILABLE);
        when(aliquotDAO.get(10)).thenReturn(Optional.of(testAliquot));
        when(chainOfCustodyEventDAO.insert(any(ChainOfCustodyEvent.class))).thenReturn(1);

        // Act
        aliquotService.updateStatus(10, AliquotStatus.RESERVED, "user2");

        // Assert
        ArgumentCaptor<ChainOfCustodyEvent> eventCaptor = ArgumentCaptor.forClass(ChainOfCustodyEvent.class);
        verify(chainOfCustodyEventDAO).insert(eventCaptor.capture());

        ChainOfCustodyEvent capturedEvent = eventCaptor.getValue();
        assertTrue("Comments should mention status change", capturedEvent.getComments().contains("Status changed"));
    }

    // ==================== Query Tests ====================

    @Test
    public void testFindByParentSampleId_ReturnsAllAliquotsForSample() {
        // Arrange
        Aliquot aliquot2 = new Aliquot();
        aliquot2.setId(11);
        aliquot2.setParentSampleId(1);
        List<Aliquot> expectedAliquots = Arrays.asList(testAliquot, aliquot2);
        when(aliquotDAO.findByParentSampleId(1)).thenReturn(expectedAliquots);

        // Act
        List<Aliquot> result = aliquotService.findByParentSampleId(1);

        // Assert
        assertEquals("Should return 2 aliquots", 2, result.size());
    }

    @Test
    public void testFindAvailableByParentSample_ReturnsOnlyAvailableAliquots() {
        // Arrange
        List<Aliquot> availableAliquots = Arrays.asList(testAliquot);
        when(aliquotDAO.findAvailableByParentSample(1)).thenReturn(availableAliquots);

        // Act
        List<Aliquot> result = aliquotService.findAvailableByParentSample(1);

        // Assert
        assertEquals("Should return 1 available aliquot", 1, result.size());
        assertEquals("Status should be AVAILABLE", AliquotStatus.AVAILABLE, result.get(0).getStatus());
    }

    @Test
    public void testFindExceedingFreezeThawLimit_ReturnsExceededAliquots() {
        // Arrange
        testAliquot.setFreezeThawCount(6);
        testAliquot.setFreezeThawLimit(5);
        List<Aliquot> exceededAliquots = Arrays.asList(testAliquot);
        when(aliquotDAO.findExceedingFreezeThawLimit()).thenReturn(exceededAliquots);

        // Act
        List<Aliquot> result = aliquotService.findExceedingFreezeThawLimit();

        // Assert
        assertEquals("Should return 1 aliquot with exceeded limit", 1, result.size());
    }

    // ==================== Barcode Generation Tests ====================

    @Test
    public void testGenerateAliquotBarcode_ReturnsValidFormat() {
        // Act
        String barcode = aliquotService.generateAliquotBarcode(1, 5);

        // Assert
        assertNotNull("Barcode should not be null", barcode);
        assertTrue("Barcode should start with ALQ-", barcode.startsWith("ALQ-"));
        assertTrue("Barcode should contain parent sample ID", barcode.contains("00001"));
        assertTrue("Barcode should contain sequence number", barcode.contains("-005-"));
    }

    @Test
    public void testGenerateAliquotBarcode_HandlesLargeSequenceNumbers() {
        // Act
        String barcode = aliquotService.generateAliquotBarcode(12345, 99);

        // Assert
        assertNotNull("Barcode should not be null", barcode);
        assertTrue("Barcode should contain formatted parent ID", barcode.contains("12345"));
        assertTrue("Barcode should contain formatted sequence", barcode.contains("-099-"));
    }
}
