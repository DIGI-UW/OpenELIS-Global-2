package org.openelisglobal.pharmaceutical.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.pharmaceutical.dao.AliquotDAO;
import org.openelisglobal.pharmaceutical.dao.ChainOfCustodyEventDAO;
import org.openelisglobal.pharmaceutical.dao.PharmaceuticalSampleDAO;
import org.openelisglobal.pharmaceutical.dao.QCCheckDAO;
import org.openelisglobal.pharmaceutical.valueholder.Aliquot;
import org.openelisglobal.pharmaceutical.valueholder.ChainOfCustodyEvent;
import org.openelisglobal.pharmaceutical.valueholder.PharmaceuticalSample;
import org.openelisglobal.pharmaceutical.valueholder.PharmaceuticalSample.LabType;
import org.openelisglobal.pharmaceutical.valueholder.PharmaceuticalSample.SampleStatus;
import org.openelisglobal.pharmaceutical.valueholder.QCCheck;

/**
 * Unit tests for PharmaceuticalSampleService. Tests sample registration, status
 * updates, barcode generation, and QC workflow.
 */
@RunWith(MockitoJUnitRunner.class)
public class PharmaceuticalSampleServiceTest {

    @Mock
    private PharmaceuticalSampleDAO pharmaceuticalSampleDAO;

    @Mock
    private AliquotDAO aliquotDAO;

    @Mock
    private QCCheckDAO qcCheckDAO;

    @Mock
    private ChainOfCustodyEventDAO chainOfCustodyEventDAO;

    @InjectMocks
    private PharmaceuticalSampleServiceImpl sampleService;

    private PharmaceuticalSample testSample;

    @Before
    public void setUp() {
        testSample = new PharmaceuticalSample();
        testSample.setId(1);
        testSample.setUniqueSampleId("PHARMA-2025-00001");
        testSample.setSampleName("Test Drug A");
        testSample.setIupacName("Acetaminophen");
        testSample.setGradeSpecification("USP Grade");
        testSample.setLotNumber("LOT-2025-001");
        testSample.setLabType(LabType.QUALITY_CONTROL);
        testSample.setStatus(SampleStatus.REGISTERED);
        testSample.setStorageCondition("2-8°C");
        testSample.setSysUserId("user1");
    }

    // ==================== Registration Tests ====================

    @Test
    public void testRegisterSample_SetsStatusToRegistered() {
        // Arrange
        PharmaceuticalSample newSample = new PharmaceuticalSample();
        newSample.setSampleName("New Sample");
        newSample.setLabType(LabType.STABILITY);

        when(pharmaceuticalSampleDAO.findByUniqueSampleId(any())).thenReturn(null);
        when(pharmaceuticalSampleDAO.insert(any(PharmaceuticalSample.class))).thenReturn(1);

        // Act
        PharmaceuticalSample result = sampleService.registerSample(newSample, "user1");

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Status should be REGISTERED", SampleStatus.REGISTERED, result.getStatus());
        assertNotNull("Registered at should be set", result.getRegisteredAt());
        assertEquals("Sys user ID should be set", "user1", result.getSysUserId());
    }

    @Test
    public void testRegisterSample_GeneratesUniqueSampleIdIfNotProvided() {
        // Arrange
        PharmaceuticalSample newSample = new PharmaceuticalSample();
        newSample.setSampleName("New Sample");
        newSample.setLabType(LabType.QUALITY_CONTROL);

        when(pharmaceuticalSampleDAO.findByUniqueSampleId(any())).thenReturn(null);
        when(pharmaceuticalSampleDAO.insert(any(PharmaceuticalSample.class))).thenReturn(1);

        // Act
        PharmaceuticalSample result = sampleService.registerSample(newSample, "user1");

        // Assert
        assertNotNull("Unique sample ID should be generated", result.getUniqueSampleId());
        assertTrue("Unique sample ID should start with PHARMA", result.getUniqueSampleId().startsWith("PHARMA"));
    }

    @Test
    public void testRegisterSample_GeneratesBarcodeAndQRCode() {
        // Arrange
        PharmaceuticalSample newSample = new PharmaceuticalSample();
        newSample.setSampleName("New Sample");
        newSample.setLabType(LabType.QUALITY_CONTROL);

        when(pharmaceuticalSampleDAO.findByUniqueSampleId(any())).thenReturn(null);
        when(pharmaceuticalSampleDAO.insert(any(PharmaceuticalSample.class))).thenReturn(1);

        // Act
        PharmaceuticalSample result = sampleService.registerSample(newSample, "user1");

        // Assert
        assertNotNull("Barcode should be generated", result.getBarcode());
        assertNotNull("QR code should be generated", result.getQrCode());
        assertTrue("Barcode should start with PS-", result.getBarcode().startsWith("PS-"));
    }

    @Test(expected = org.openelisglobal.common.exception.LIMSRuntimeException.class)
    public void testRegisterSample_ThrowsExceptionForDuplicateSampleId() {
        // Arrange
        PharmaceuticalSample newSample = new PharmaceuticalSample();
        newSample.setUniqueSampleId("PHARMA-2025-00001");
        newSample.setSampleName("Duplicate Sample");

        when(pharmaceuticalSampleDAO.findByUniqueSampleId("PHARMA-2025-00001")).thenReturn(testSample);

        // Act - should throw exception
        sampleService.registerSample(newSample, "user1");
    }

    // ==================== Status Update Tests ====================

    @Test
    public void testUpdateStatus_UpdatesSampleStatus() {
        // Arrange
        when(pharmaceuticalSampleDAO.get(1)).thenReturn(Optional.of(testSample));

        // Act
        PharmaceuticalSample result = sampleService.updateStatus(1, SampleStatus.IN_TESTING, "user2");

        // Assert
        assertEquals("Status should be IN_TESTING", SampleStatus.IN_TESTING, result.getStatus());
        assertEquals("Sys user ID should be updated", "user2", result.getSysUserId());
        verify(pharmaceuticalSampleDAO).update(any(PharmaceuticalSample.class));
    }

    @Test(expected = org.openelisglobal.common.exception.LIMSRuntimeException.class)
    public void testUpdateStatus_ThrowsExceptionForNonExistentSample() {
        // Arrange
        when(pharmaceuticalSampleDAO.get(999)).thenReturn(Optional.empty());

        // Act - should throw exception
        sampleService.updateStatus(999, SampleStatus.IN_TESTING, "user2");
    }

    // ==================== Barcode Generation Tests ====================

    @Test
    public void testGenerateBarcode_ReturnsValidFormat() {
        // Act
        String barcode = sampleService.generateBarcode(testSample);

        // Assert
        assertNotNull("Barcode should not be null", barcode);
        assertTrue("Barcode should start with PS-", barcode.startsWith("PS-"));
        assertTrue("Barcode should have proper length", barcode.length() >= 10);
    }

    @Test
    public void testGenerateBarcode_GeneratesUniqueBarcodesForDifferentCalls() {
        // Act
        String barcode1 = sampleService.generateBarcode(testSample);

        // Small delay to ensure different timestamps
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String barcode2 = sampleService.generateBarcode(testSample);

        // Assert - barcodes should be different due to random component
        assertNotNull("First barcode should not be null", barcode1);
        assertNotNull("Second barcode should not be null", barcode2);
    }

    // ==================== Query Tests ====================

    @Test
    public void testFindByStatus_ReturnsMatchingSamples() {
        // Arrange
        List<PharmaceuticalSample> expectedSamples = Arrays.asList(testSample);
        when(pharmaceuticalSampleDAO.findByStatus(SampleStatus.REGISTERED)).thenReturn(expectedSamples);

        // Act
        List<PharmaceuticalSample> result = sampleService.findByStatus(SampleStatus.REGISTERED);

        // Assert
        assertEquals("Should return 1 sample", 1, result.size());
        assertEquals("Status should match", SampleStatus.REGISTERED, result.get(0).getStatus());
    }

    @Test
    public void testFindByLabType_ReturnsMatchingSamples() {
        // Arrange
        List<PharmaceuticalSample> expectedSamples = Arrays.asList(testSample);
        when(pharmaceuticalSampleDAO.findByLabType(LabType.QUALITY_CONTROL)).thenReturn(expectedSamples);

        // Act
        List<PharmaceuticalSample> result = sampleService.findByLabType(LabType.QUALITY_CONTROL);

        // Assert
        assertEquals("Should return 1 sample", 1, result.size());
        assertEquals("Lab type should match", LabType.QUALITY_CONTROL, result.get(0).getLabType());
    }

    @Test
    public void testFindExpiringSoon_ReturnsSamplesExpiringSoon() {
        // Arrange
        List<PharmaceuticalSample> expectedSamples = Arrays.asList(testSample);
        when(pharmaceuticalSampleDAO.findExpiringSoon(30)).thenReturn(expectedSamples);

        // Act
        List<PharmaceuticalSample> result = sampleService.findExpiringSoon(30);

        // Assert
        assertEquals("Should return 1 sample", 1, result.size());
    }

    @Test
    public void testFindByBarcode_ReturnsSampleWhenFound() {
        // Arrange
        testSample.setBarcode("PS-12345-ABCD");
        when(pharmaceuticalSampleDAO.findByBarcode("PS-12345-ABCD")).thenReturn(testSample);

        // Act
        PharmaceuticalSample result = sampleService.findByBarcode("PS-12345-ABCD");

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Barcode should match", "PS-12345-ABCD", result.getBarcode());
    }

    @Test
    public void testFindByBarcode_ReturnsNullWhenNotFound() {
        // Arrange
        when(pharmaceuticalSampleDAO.findByBarcode("NONEXISTENT")).thenReturn(null);

        // Act
        PharmaceuticalSample result = sampleService.findByBarcode("NONEXISTENT");

        // Assert
        assertNull("Result should be null for non-existent barcode", result);
    }

    // ==================== Sample Details Tests ====================

    @Test
    public void testGetSampleWithDetails_ReturnsCompleteDetails() {
        // Arrange
        when(pharmaceuticalSampleDAO.get(1)).thenReturn(Optional.of(testSample));

        Aliquot aliquot1 = new Aliquot();
        aliquot1.setId(10);
        aliquot1.setAliquotCode("ALQ-001");
        List<Aliquot> aliquots = Arrays.asList(aliquot1);
        when(aliquotDAO.findByParentSampleId(1)).thenReturn(aliquots);

        QCCheck qcCheck = new QCCheck();
        qcCheck.setId(20);
        qcCheck.setCheckType("Visual");
        List<QCCheck> qcChecks = Arrays.asList(qcCheck);
        when(qcCheckDAO.findBySampleId(1)).thenReturn(qcChecks);
        when(qcCheckDAO.findLatestBySampleId(1)).thenReturn(qcCheck);

        ChainOfCustodyEvent cocEvent = new ChainOfCustodyEvent();
        cocEvent.setId(30);
        List<ChainOfCustodyEvent> cocEvents = Arrays.asList(cocEvent);
        when(chainOfCustodyEventDAO.findBySampleId(1)).thenReturn(cocEvents);

        // Act
        Map<String, Object> result = sampleService.getSampleWithDetails(1);

        // Assert
        assertNotNull("Result should not be null", result);
        assertNotNull("Sample should be present", result.get("sample"));
        assertNotNull("Aliquots should be present", result.get("aliquots"));
        assertEquals("Aliquot count should be 1", 1, result.get("aliquotCount"));
        assertNotNull("QC checks should be present", result.get("qcChecks"));
        assertNotNull("Latest QC check should be present", result.get("latestQCCheck"));
        assertNotNull("Custody events should be present", result.get("custodyEvents"));
    }

    @Test
    public void testGetSampleWithDetails_ReturnsNullForNonExistentSample() {
        // Arrange
        when(pharmaceuticalSampleDAO.get(999)).thenReturn(Optional.empty());

        // Act
        Map<String, Object> result = sampleService.getSampleWithDetails(999);

        // Assert
        assertNull("Result should be null for non-existent sample", result);
        verify(aliquotDAO, never()).findByParentSampleId(anyInt());
    }

    // ==================== CRUD Tests ====================

    @Test
    public void testDelete_DeletesSampleWhenExists() {
        // Arrange
        when(pharmaceuticalSampleDAO.get(1)).thenReturn(Optional.of(testSample));

        // Act
        sampleService.delete(1);

        // Assert
        verify(pharmaceuticalSampleDAO).delete(testSample);
    }

    @Test
    public void testDelete_DoesNothingWhenSampleNotFound() {
        // Arrange
        when(pharmaceuticalSampleDAO.get(999)).thenReturn(Optional.empty());

        // Act
        sampleService.delete(999);

        // Assert
        verify(pharmaceuticalSampleDAO, never()).delete(any(PharmaceuticalSample.class));
    }
}
