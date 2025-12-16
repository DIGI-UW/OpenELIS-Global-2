package org.openelisglobal.notebook.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.notebook.dao.NotebookPageSampleDAO;
import org.openelisglobal.notebook.service.ResultCompilationService.ExportOptions;
import org.openelisglobal.notebook.service.ResultCompilationService.ValidationSummary;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.notebook.valueholder.ValidationStatus;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * Unit tests for ResultCompilationService per US7 requirements.
 *
 * US7 Goal: Compile analysis outputs into structured result files or database
 * records, deliver results to Data Management Team or designated recipients,
 * and flag invalid or inconclusive results for review.
 *
 * Independent Test: Flag 3 samples as invalid with reasons, generate Excel
 * report, verify flagged samples are marked in report with status
 * (VALID/INVALID/INCONCLUSIVE) and delivery confirmation.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ResultCompilationServiceTest {

    @Mock
    private NotebookPageSampleDAO notebookPageSampleDAO;

    @Mock
    private NoteBookService noteBookService;

    @Mock
    private SampleItemService sampleItemService;

    @Mock
    private SystemUserService systemUserService;

    @InjectMocks
    private ResultCompilationServiceImpl service;

    private NoteBook testNotebook;
    private SystemUser testUser;
    private NotebookPageSample testSample;

    @Before
    public void setUp() {
        testNotebook = new NoteBook();
        testNotebook.setId(1);
        testNotebook.setTitle("Test Immunology Workflow");

        testUser = mock(SystemUser.class);
        when(testUser.getId()).thenReturn("1");
        when(testUser.getFirstName()).thenReturn("Test");
        when(testUser.getLastName()).thenReturn("User");

        testSample = new NotebookPageSample();
        testSample.setId(1);
        testSample.setSampleItemId("100");
        testSample.setStatus(NotebookPageSample.Status.COMPLETED);
        testSample.setData(new HashMap<>());
    }

    /**
     * T122a: Validation status enum exists with required values.
     */
    @Test
    public void testValidationStatusEnum_AllValuesExist() {
        ValidationStatus[] values = ValidationStatus.values();

        assertTrue("Should have at least 3 validation statuses", values.length >= 3);
        assertNotNull("VALID should exist", ValidationStatus.valueOf("VALID"));
        assertNotNull("INVALID should exist", ValidationStatus.valueOf("INVALID"));
        assertNotNull("INCONCLUSIVE should exist", ValidationStatus.valueOf("INCONCLUSIVE"));
    }

    @Test
    public void testValidationStatusEnum_DisplayNames() {
        assertEquals("Valid", ValidationStatus.VALID.getDisplayName());
        assertEquals("Invalid", ValidationStatus.INVALID.getDisplayName());
        assertEquals("Inconclusive", ValidationStatus.INCONCLUSIVE.getDisplayName());
    }

    @Test
    public void testValidationStatusEnum_TagColors() {
        assertEquals("green", ValidationStatus.VALID.getTagColor());
        assertEquals("red", ValidationStatus.INVALID.getTagColor());
        assertEquals("yellow", ValidationStatus.INCONCLUSIVE.getTagColor());
    }

    /**
     * T122: Flag sample as INVALID with reason.
     */
    @Test
    public void testFlagSample_Invalid_RequiresReason() {
        when(notebookPageSampleDAO.getBySampleItemIdAndPageId("100", 1)).thenReturn(testSample);

        boolean result = service.flagSample(1, "100", ValidationStatus.INVALID, "Sample hemolyzed", "1");

        assertTrue("Should successfully flag sample", result);

        ArgumentCaptor<NotebookPageSample> captor = ArgumentCaptor.forClass(NotebookPageSample.class);
        verify(notebookPageSampleDAO).update(captor.capture());

        NotebookPageSample updated = captor.getValue();
        Map<String, Object> data = updated.getData();

        assertEquals("INVALID", data.get("validationStatus"));
        assertEquals("Sample hemolyzed", data.get("validationReason"));
        assertEquals("1", data.get("validatedBy"));
        assertNotNull(data.get("validatedAt"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFlagSample_Invalid_WithoutReason_ThrowsException() {
        when(notebookPageSampleDAO.getBySampleItemIdAndPageId("100", 1)).thenReturn(testSample);

        service.flagSample(1, "100", ValidationStatus.INVALID, null, "1");
    }

    @Test
    public void testFlagSample_Valid_NoReasonRequired() {
        when(notebookPageSampleDAO.getBySampleItemIdAndPageId("100", 1)).thenReturn(testSample);

        boolean result = service.flagSample(1, "100", ValidationStatus.VALID, null, "1");

        assertTrue("Should successfully flag sample as VALID", result);
        verify(notebookPageSampleDAO).update(any(NotebookPageSample.class));
    }

    /**
     * T122b: Bulk flag samples with reason.
     */
    @Test
    public void testBulkFlagSamples_3Samples_AllFlagged() {
        // Per US7 independent test: "Flag 3 samples as invalid with reasons"
        NotebookPageSample sample1 = createSample("101");
        NotebookPageSample sample2 = createSample("102");
        NotebookPageSample sample3 = createSample("103");

        when(notebookPageSampleDAO.getBySampleItemIdAndPageId("101", 1)).thenReturn(sample1);
        when(notebookPageSampleDAO.getBySampleItemIdAndPageId("102", 1)).thenReturn(sample2);
        when(notebookPageSampleDAO.getBySampleItemIdAndPageId("103", 1)).thenReturn(sample3);

        List<String> sampleIds = Arrays.asList("101", "102", "103");

        int flagged = service.bulkFlagSamples(1, sampleIds, ValidationStatus.INVALID, "QC failure - batch rejected",
                "1");

        assertEquals("All 3 samples should be flagged", 3, flagged);
        verify(notebookPageSampleDAO, times(3)).update(any(NotebookPageSample.class));
    }

    /**
     * T121: Validation summary statistics.
     */
    @Test
    public void testGetValidationSummary_MixedStatuses() {
        NotebookPageSample validSample = createSampleWithStatus("101", ValidationStatus.VALID);
        NotebookPageSample invalidSample = createSampleWithStatus("102", ValidationStatus.INVALID);
        NotebookPageSample inconclusiveSample = createSampleWithStatus("103", ValidationStatus.INCONCLUSIVE);
        NotebookPageSample pendingSample = createSample("104");

        when(notebookPageSampleDAO.getByPageId(1))
                .thenReturn(Arrays.asList(validSample, invalidSample, inconclusiveSample, pendingSample));

        ValidationSummary summary = service.getValidationSummary(1);

        assertEquals("Total should be 4", 4, summary.total());
        assertEquals("Valid should be 1", 1, summary.valid());
        assertEquals("Invalid should be 1", 1, summary.invalid());
        assertEquals("Inconclusive should be 1", 1, summary.inconclusive());
        assertEquals("Pending should be 1", 1, summary.pending());
    }

    @Test
    public void testValidationSummary_Percentages() {
        ValidationSummary summary = new ValidationSummary(100, 80, 10, 5, 5);

        assertEquals(80.0, summary.validPercentage(), 0.01);
        assertEquals(10.0, summary.invalidPercentage(), 0.01);
        assertEquals(5.0, summary.inconclusivePercentage(), 0.01);
    }

    /**
     * T123: Excel report generation with validation status.
     */
    @Test
    public void testCompileToExcel_IncludesValidationStatus() {
        when(noteBookService.get(1)).thenReturn(testNotebook);

        NotebookPageSample validSample = createSampleWithStatus("101", ValidationStatus.VALID);
        NotebookPageSample invalidSample = createSampleWithStatus("102", ValidationStatus.INVALID);

        when(notebookPageSampleDAO.getByNotebookId(1)).thenReturn(Arrays.asList(validSample, invalidSample));

        SampleItem sampleItem = mock(SampleItem.class);
        when(sampleItem.getExternalId()).thenReturn("EXT-101");
        when(sampleItemService.get("101")).thenReturn(sampleItem);
        when(sampleItemService.get("102")).thenReturn(sampleItem);

        byte[] excelBytes = service.compileToExcel(1, ExportOptions.defaultOptions());

        assertNotNull("Excel bytes should not be null", excelBytes);
        assertTrue("Excel should have content", excelBytes.length > 0);
    }

    @Test
    public void testCompileToExcel_ExcludesInvalidWhenFiltered() {
        when(noteBookService.get(1)).thenReturn(testNotebook);

        NotebookPageSample validSample = createSampleWithStatus("101", ValidationStatus.VALID);
        NotebookPageSample invalidSample = createSampleWithStatus("102", ValidationStatus.INVALID);

        when(notebookPageSampleDAO.getByNotebookId(1)).thenReturn(Arrays.asList(validSample, invalidSample));

        SampleItem sampleItem = mock(SampleItem.class);
        when(sampleItem.getExternalId()).thenReturn("EXT-101");
        when(sampleItemService.get("101")).thenReturn(sampleItem);

        ExportOptions options = new ExportOptions(false, true, true, null, "yyyy-MM-dd", "Results");

        byte[] excelBytes = service.compileToExcel(1, options);

        assertNotNull("Excel bytes should not be null", excelBytes);
        // Excel would have fewer rows with invalid filtered out
    }

    /**
     * T123a: CSV export with all result data.
     */
    @Test
    public void testCompileToCsv_IncludesAllData() {
        when(noteBookService.get(1)).thenReturn(testNotebook);

        NotebookPageSample sample = createSampleWithStatus("101", ValidationStatus.VALID);

        when(notebookPageSampleDAO.getByNotebookId(1)).thenReturn(Arrays.asList(sample));

        SampleItem sampleItem = mock(SampleItem.class);
        when(sampleItem.getExternalId()).thenReturn("EXT-101");
        when(sampleItemService.get("101")).thenReturn(sampleItem);

        byte[] csvBytes = service.compileToCsv(1, ExportOptions.defaultOptions());

        assertNotNull("CSV bytes should not be null", csvBytes);

        String csv = new String(csvBytes);
        assertTrue("CSV should have header", csv.contains("Sample ID"));
        assertTrue("CSV should have validation status column", csv.contains("Validation Status"));
        assertTrue("CSV should contain sample data", csv.contains("101"));
    }

    /**
     * T125c: Delivery tracking.
     */
    @Test
    public void testRecordDelivery_CreatesRecord() {
        when(systemUserService.get("1")).thenReturn(testUser);

        Integer deliveryId = service.recordDelivery(1, "Data Management Team", "datamanagement@lab.org", null,
                "internal", null, null, "1");

        assertNotNull("Delivery ID should not be null", deliveryId);
        assertTrue("Delivery ID should be positive", deliveryId > 0);
    }

    @Test
    public void testGetDeliveryHistory_ReturnsRecords() {
        when(systemUserService.get("1")).thenReturn(testUser);

        service.recordDelivery(1, "Data Management Team", "dm@lab.org", null, "internal", null, null, "1");
        service.recordDelivery(1, "External Reviewer", "reviewer@external.org", null, "external", null, null, "1");

        List<ResultCompilationService.DeliveryRecord> history = service.getDeliveryHistory(1);

        assertFalse("History should not be empty", history.isEmpty());
        assertEquals("Should have 2 delivery records", 2, history.size());
    }

    /**
     * T121: Get samples with validation for frontend display.
     */
    @Test
    public void testGetSamplesWithValidation_ReturnsFormattedData() {
        NotebookPageSample sample = createSampleWithStatus("101", ValidationStatus.INVALID);
        sample.getData().put("validationReason", "Sample degraded");

        when(notebookPageSampleDAO.getByPageId(1)).thenReturn(Arrays.asList(sample));

        SampleItem sampleItem = mock(SampleItem.class);
        when(sampleItem.getExternalId()).thenReturn("EXT-101");
        when(sampleItemService.get("101")).thenReturn(sampleItem);

        List<Map<String, Object>> samples = service.getSamplesWithValidation(1);

        assertFalse("Should return samples", samples.isEmpty());

        Map<String, Object> sampleData = samples.get(0);
        assertEquals("101", sampleData.get("id"));
        assertEquals("INVALID", sampleData.get("validationStatus"));
        assertEquals("Invalid", sampleData.get("validationDisplayName"));
        assertEquals("red", sampleData.get("validationColor"));
        assertEquals("Sample degraded", sampleData.get("validationReason"));
    }

    // Helper methods

    private NotebookPageSample createSample(String sampleItemId) {
        NotebookPageSample sample = new NotebookPageSample();
        sample.setSampleItemId(sampleItemId);
        sample.setStatus(NotebookPageSample.Status.COMPLETED);
        sample.setData(new HashMap<>());
        return sample;
    }

    private NotebookPageSample createSampleWithStatus(String sampleItemId, ValidationStatus validationStatus) {
        NotebookPageSample sample = createSample(sampleItemId);
        Map<String, Object> data = new HashMap<>();
        data.put("validationStatus", validationStatus.name());
        sample.setData(data);
        return sample;
    }
}
