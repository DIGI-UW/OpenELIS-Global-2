package org.openelisglobal.notebook.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.notebook.form.ManifestImportForm;
import org.openelisglobal.notebook.service.ManifestImportService.ManifestImportResult;
import org.openelisglobal.notebook.service.ManifestImportService.ManifestRow;
import org.openelisglobal.notebook.service.ManifestImportService.ParseError;
import org.openelisglobal.notebook.service.ManifestImportService.ParsedManifest;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

/**
 * Unit tests for ManifestImportService (User Story 2). Tests CSV parsing,
 * sample type validation, and bulk sample creation.
 */
@RunWith(MockitoJUnitRunner.class)
public class ManifestImportServiceTest {

    @Mock
    private TypeOfSampleService typeOfSampleService;

    @Mock
    private SampleService sampleService;

    @Mock
    private SampleItemService sampleItemService;

    @Mock
    private NoteBookService noteBookService;

    @Mock
    private NotebookSampleEntryService notebookSampleEntryService;

    @InjectMocks
    private ManifestImportServiceImpl manifestImportService;

    private ManifestImportForm defaultColumnMapping;
    private NoteBook testNotebook;

    @Before
    public void setUp() {
        // Default column mapping
        defaultColumnMapping = new ManifestImportForm();
        defaultColumnMapping.setGroupIdColumn("group_id");
        defaultColumnMapping.setSampleTypeColumn("sample_type");
        defaultColumnMapping.setCollectionDateColumn("collection_date");
        defaultColumnMapping.setVolumeColumn("volume");
        defaultColumnMapping.setNumOfSamplesColumn("num_of_samples");
        defaultColumnMapping.setNotesColumn("notes");
        defaultColumnMapping.setDateFormat("yyyy-MM-dd");

        // Create test notebook with pages
        testNotebook = new NoteBook();
        testNotebook.setId(100);
        testNotebook.setIsTemplate(false);
        List<NoteBookPage> pages = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            NoteBookPage page = new NoteBookPage();
            page.setId(100 + i);
            page.setOrder(i);
            pages.add(page);
        }
        testNotebook.setPages(pages);
    }

    // =====================================================================
    // CSV Parsing Tests
    // =====================================================================

    /**
     * Test: Parse valid CSV with standard columns
     */
    @Test
    public void testParseManifestCsv_ValidCsv_ReturnsRows() {
        String csv = "group_id,sample_type,collection_date,volume,num_of_samples,notes\n"
                + "GRP-001,Whole Blood,2024-01-15,5.0,10,Batch A\n" + "GRP-002,Serum,2024-01-15,3.0,15,Batch B\n";

        InputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        ParsedManifest result = manifestImportService.parseManifestCsv(input, defaultColumnMapping);

        assertNotNull(result);
        assertEquals(2, result.rows().size());
        assertEquals(0, result.errors().size());

        ManifestRow row1 = result.rows().get(0);
        assertEquals("GRP-001", row1.groupId());
        assertEquals("Whole Blood", row1.sampleType());
        assertEquals("2024-01-15", row1.collectionDate());
        assertEquals("5.0", row1.volume());
        assertEquals(10, row1.numOfSamples());
        assertEquals("Batch A", row1.notes());

        ManifestRow row2 = result.rows().get(1);
        assertEquals("GRP-002", row2.groupId());
        assertEquals(15, row2.numOfSamples());
    }

    /**
     * Test: Parse CSV with different column order
     */
    @Test
    public void testParseManifestCsv_DifferentColumnOrder_MapsCorrectly() {
        String csv = "notes,num_of_samples,volume,collection_date,sample_type,group_id\n"
                + "Test notes,20,4.5,2024-02-01,Plasma,GRP-003\n";

        InputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        ParsedManifest result = manifestImportService.parseManifestCsv(input, defaultColumnMapping);

        assertEquals(1, result.rows().size());
        ManifestRow row = result.rows().get(0);
        assertEquals("GRP-003", row.groupId());
        assertEquals("Plasma", row.sampleType());
        assertEquals(20, row.numOfSamples());
        assertEquals("Test notes", row.notes());
    }

    /**
     * Test: Parse CSV with missing optional columns
     */
    @Test
    public void testParseManifestCsv_MissingOptionalColumns_SetsDefaults() {
        String csv = "group_id,sample_type,num_of_samples\n" + "GRP-001,Whole Blood,5\n";

        InputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        ParsedManifest result = manifestImportService.parseManifestCsv(input, defaultColumnMapping);

        assertEquals(1, result.rows().size());
        ManifestRow row = result.rows().get(0);
        assertEquals("GRP-001", row.groupId());
        assertEquals(5, row.numOfSamples());
        assertNull(row.volume());
        assertNull(row.notes());
    }

    /**
     * Test: Parse CSV with invalid num_of_samples
     */
    @Test
    public void testParseManifestCsv_InvalidNumOfSamples_ReturnsError() {
        String csv = "group_id,sample_type,num_of_samples\n" + "GRP-001,Whole Blood,invalid\n";

        InputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        ParsedManifest result = manifestImportService.parseManifestCsv(input, defaultColumnMapping);

        assertEquals(0, result.rows().size());
        assertEquals(1, result.errors().size());
        ParseError error = result.errors().get(0);
        assertEquals(2, error.rowNumber()); // Row 2 (1-indexed, header is row 1)
        assertEquals("num_of_samples", error.column());
        assertTrue(error.message().contains("Invalid"));
    }

    /**
     * Test: Parse CSV with missing required group_id
     */
    @Test
    public void testParseManifestCsv_MissingGroupId_ReturnsError() {
        String csv = "group_id,sample_type,num_of_samples\n" + ",Whole Blood,10\n";

        InputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        ParsedManifest result = manifestImportService.parseManifestCsv(input, defaultColumnMapping);

        assertEquals(0, result.rows().size());
        assertEquals(1, result.errors().size());
        ParseError error = result.errors().get(0);
        assertEquals("group_id", error.column());
        assertTrue(error.message().toLowerCase().contains("required"));
    }

    /**
     * Test: Parse empty CSV
     */
    @Test
    public void testParseManifestCsv_EmptyCsv_ReturnsEmptyList() {
        String csv = "group_id,sample_type,num_of_samples\n";

        InputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        ParsedManifest result = manifestImportService.parseManifestCsv(input, defaultColumnMapping);

        assertNotNull(result);
        assertEquals(0, result.rows().size());
        assertEquals(0, result.errors().size());
    }

    // =====================================================================
    // Sample Type Validation Tests
    // =====================================================================

    /**
     * Test: Valid sample types pass validation
     */
    @Test
    public void testValidateSampleTypes_ValidTypes_NoErrors() {
        List<ManifestRow> rows = Arrays.asList(
                new ManifestRow(2, "GRP-001", "Whole Blood", "2024-01-15", "5.0", 10, ""),
                new ManifestRow(3, "GRP-002", "Serum", "2024-01-15", "3.0", 15, ""));
        ParsedManifest manifest = new ParsedManifest(rows, new ArrayList<>());

        TypeOfSample wholeBlood = new TypeOfSample();
        wholeBlood.setId("1");
        wholeBlood.setDescription("Whole Blood");

        TypeOfSample serum = new TypeOfSample();
        serum.setId("2");
        serum.setDescription("Serum");

        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(
                argThat(tos -> tos != null && "Whole Blood".equalsIgnoreCase(tos.getDescription())), eq(true)))
                .thenReturn(wholeBlood);
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(
                argThat(tos -> tos != null && "Serum".equalsIgnoreCase(tos.getDescription())), eq(true)))
                .thenReturn(serum);

        List<ParseError> errors = manifestImportService.validateSampleTypes(manifest);

        assertEquals(0, errors.size());
    }

    /**
     * Test: Invalid sample type returns error
     */
    @Test
    public void testValidateSampleTypes_InvalidType_ReturnsError() {
        List<ManifestRow> rows = Arrays
                .asList(new ManifestRow(2, "GRP-001", "InvalidType", "2024-01-15", "5.0", 10, ""));
        ParsedManifest manifest = new ParsedManifest(rows, new ArrayList<>());

        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(any(), eq(true))).thenReturn(null);

        List<ParseError> errors = manifestImportService.validateSampleTypes(manifest);

        assertEquals(1, errors.size());
        ParseError error = errors.get(0);
        assertEquals(2, error.rowNumber());
        assertEquals("sample_type", error.column());
        assertTrue(error.message().contains("InvalidType"));
    }

    // =====================================================================
    // External ID Generation Tests
    // =====================================================================

    /**
     * Test: Generate external ID with standard format
     */
    @Test
    public void testGenerateExternalId_StandardFormat() {
        String id1 = manifestImportService.generateExternalId("GRP-001", 1);
        String id2 = manifestImportService.generateExternalId("GRP-001", 10);
        String id3 = manifestImportService.generateExternalId("GRP-001", 100);

        assertEquals("GRP-001-001", id1);
        assertEquals("GRP-001-010", id2);
        assertEquals("GRP-001-100", id3);
    }

    /**
     * Test: Generate external ID handles special characters in group ID
     */
    @Test
    public void testGenerateExternalId_SpecialCharacters() {
        String id = manifestImportService.generateExternalId("BATCH_2024/A", 5);

        assertEquals("BATCH_2024/A-005", id);
    }

    // =====================================================================
    // Sample Creation Tests
    // =====================================================================

    /**
     * Test: Create samples from manifest with num_of_samples According to spec: 4
     * rows with num_of_samples 10, 15, 20, 5 = 50 total
     */
    @Test
    public void testCreateSamplesFromManifest_CreatesCorrectCount() {
        List<ManifestRow> rows = Arrays.asList(
                new ManifestRow(2, "GRP-001", "Whole Blood", "2024-01-15", "5.0", 10, ""),
                new ManifestRow(3, "GRP-002", "Serum", "2024-01-15", "3.0", 15, ""),
                new ManifestRow(4, "GRP-003", "Plasma", "2024-01-15", "4.0", 20, ""),
                new ManifestRow(5, "GRP-004", "Whole Blood", "2024-01-15", "5.0", 5, ""));
        ParsedManifest manifest = new ParsedManifest(rows, new ArrayList<>());

        TypeOfSample wholeBlood = new TypeOfSample();
        wholeBlood.setId("1");
        wholeBlood.setDescription("Whole Blood");

        TypeOfSample serum = new TypeOfSample();
        serum.setId("2");
        serum.setDescription("Serum");

        TypeOfSample plasma = new TypeOfSample();
        plasma.setId("3");
        plasma.setDescription("Plasma");

        when(noteBookService.get(100)).thenReturn(testNotebook);
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(
                argThat(tos -> tos != null && "Whole Blood".equalsIgnoreCase(tos.getDescription())), eq(true)))
                .thenReturn(wholeBlood);
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(
                argThat(tos -> tos != null && "Serum".equalsIgnoreCase(tos.getDescription())), eq(true)))
                .thenReturn(serum);
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(
                argThat(tos -> tos != null && "Plasma".equalsIgnoreCase(tos.getDescription())), eq(true)))
                .thenReturn(plasma);

        // Mock sample creation - return a unique string ID based on external ID hash
        when(sampleItemService.insert(any(SampleItem.class))).thenAnswer(invocation -> {
            SampleItem item = invocation.getArgument(0);
            return String.valueOf(Math.abs(item.getExternalId().hashCode()));
        });

        ManifestImportResult result = manifestImportService.createSamplesForEntry(100, manifest, "testUser");

        // 10 + 15 + 20 + 5 = 50 total samples
        assertEquals(50, result.totalRequested());
        assertEquals(50, result.totalCreated());
        assertEquals(0, result.errors().size());
        verify(sampleItemService, times(50)).insert(any(SampleItem.class));
    }

    /**
     * Test: Created samples have correct external_id pattern
     */
    @Test
    public void testCreateSamplesFromManifest_CorrectExternalIdPattern() {
        List<ManifestRow> rows = Arrays
                .asList(new ManifestRow(2, "GRP-001", "Whole Blood", "2024-01-15", "5.0", 3, ""));
        ParsedManifest manifest = new ParsedManifest(rows, new ArrayList<>());

        TypeOfSample wholeBlood = new TypeOfSample();
        wholeBlood.setId("1");
        wholeBlood.setDescription("Whole Blood");

        when(noteBookService.get(100)).thenReturn(testNotebook);
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(any(), eq(true))).thenReturn(wholeBlood);

        List<SampleItem> capturedItems = new ArrayList<>();
        when(sampleItemService.insert(any(SampleItem.class))).thenAnswer(invocation -> {
            SampleItem item = invocation.getArgument(0);
            capturedItems.add(item);
            return "1000";
        });

        manifestImportService.createSamplesForEntry(100, manifest, "testUser");

        assertEquals(3, capturedItems.size());
        assertEquals("GRP-001-001", capturedItems.get(0).getExternalId());
        assertEquals("GRP-001-002", capturedItems.get(1).getExternalId());
        assertEquals("GRP-001-003", capturedItems.get(2).getExternalId());
    }

    /**
     * Test: Samples are linked to notebook after creation
     */
    @Test
    public void testCreateSamplesFromManifest_LinksSamplesToNotebook() {
        List<ManifestRow> rows = Arrays
                .asList(new ManifestRow(2, "GRP-001", "Whole Blood", "2024-01-15", "5.0", 2, ""));
        ParsedManifest manifest = new ParsedManifest(rows, new ArrayList<>());

        TypeOfSample wholeBlood = new TypeOfSample();
        wholeBlood.setId("1");
        wholeBlood.setDescription("Whole Blood");

        when(noteBookService.get(100)).thenReturn(testNotebook);
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(any(), eq(true))).thenReturn(wholeBlood);
        when(sampleItemService.insert(any(SampleItem.class))).thenReturn("1000");

        manifestImportService.createSamplesForEntry(100, manifest, "testUser");

        // Verify samples are linked to notebook
        verify(notebookSampleEntryService).linkSamplesToNotebook(eq(100), anyList());
    }

    /**
     * Test: Create samples to non-existent notebook returns error
     */
    @Test
    public void testCreateSamplesFromManifest_NonExistentNotebook_ReturnsError() {
        List<ManifestRow> rows = Arrays
                .asList(new ManifestRow(2, "GRP-001", "Whole Blood", "2024-01-15", "5.0", 10, ""));
        ParsedManifest manifest = new ParsedManifest(rows, new ArrayList<>());

        when(noteBookService.get(999)).thenReturn(null);

        ManifestImportResult result = manifestImportService.createSamplesForEntry(999, manifest, "testUser");

        assertEquals(0, result.totalCreated());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).message().toLowerCase().contains("notebook"));
    }

    /**
     * Test: Row with num_of_samples = 0 is skipped
     */
    @Test
    public void testCreateSamplesFromManifest_ZeroSamples_Skipped() {
        List<ManifestRow> rows = Arrays.asList(new ManifestRow(2, "GRP-001", "Whole Blood", "2024-01-15", "5.0", 0, ""),
                new ManifestRow(3, "GRP-002", "Serum", "2024-01-15", "3.0", 5, ""));
        ParsedManifest manifest = new ParsedManifest(rows, new ArrayList<>());

        TypeOfSample wholeBlood = new TypeOfSample();
        wholeBlood.setId("1");
        wholeBlood.setDescription("Whole Blood");

        TypeOfSample serum = new TypeOfSample();
        serum.setId("2");
        serum.setDescription("Serum");

        when(noteBookService.get(100)).thenReturn(testNotebook);
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(
                argThat(tos -> tos != null && "Whole Blood".equalsIgnoreCase(tos.getDescription())), eq(true)))
                .thenReturn(wholeBlood);
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(
                argThat(tos -> tos != null && "Serum".equalsIgnoreCase(tos.getDescription())), eq(true)))
                .thenReturn(serum);
        when(sampleItemService.insert(any(SampleItem.class))).thenReturn("1000");

        ManifestImportResult result = manifestImportService.createSamplesForEntry(100, manifest, "testUser");

        // Only 5 samples from second row, first row skipped
        assertEquals(5, result.totalRequested());
        assertEquals(5, result.totalCreated());
        verify(sampleItemService, times(5)).insert(any(SampleItem.class));
    }
}
