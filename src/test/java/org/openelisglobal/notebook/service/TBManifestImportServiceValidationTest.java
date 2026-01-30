package org.openelisglobal.notebook.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.notebook.service.TBManifestImportService.ParseError;
import org.openelisglobal.notebook.service.TBManifestImportService.ParsedManifest;
import org.openelisglobal.notebook.service.TBManifestImportService.TBManifestRow;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.test.service.DepartmentSampleTypeService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

/**
 * Unit tests for TBManifestImportService.validateSpecimenTypesForEntry()
 * method. Tests department-based sample type validation for manifest imports.
 */
@RunWith(MockitoJUnitRunner.class)
public class TBManifestImportServiceValidationTest {

    @Mock
    private NotebookEntryService notebookEntryService;

    @Mock
    private DepartmentSampleTypeService departmentSampleTypeService;

    @Mock
    private TypeOfSampleService typeOfSampleService;

    @Mock
    private NoteBook notebook;

    @InjectMocks
    private TBManifestImportServiceImpl tbManifestImportService;

    private static final Integer ENTRY_ID = 1;
    private static final String DEPT_ID_1 = "100";
    private static final String DEPT_ID_2 = "200";

    private TypeOfSample sputumType;
    private TypeOfSample balType;
    private TypeOfSample bloodType;
    private TypeOfSample urineType;
    private TestSection dept1;
    private TestSection dept2;
    private NotebookEntry entry;

    @Before
    public void setUp() {
        // Set up sample types
        sputumType = new TypeOfSample();
        sputumType.setId("10");
        sputumType.setDescription("Sputum");

        balType = new TypeOfSample();
        balType.setId("20");
        balType.setDescription("BAL");

        bloodType = new TypeOfSample();
        bloodType.setId("30");
        bloodType.setDescription("Blood");

        urineType = new TypeOfSample();
        urineType.setId("40");
        urineType.setDescription("Urine");

        // Set up departments (test sections)
        dept1 = new TestSection();
        dept1.setId(DEPT_ID_1);
        dept1.setTestSectionName("TB Laboratory");

        dept2 = new TestSection();
        dept2.setId(DEPT_ID_2);
        dept2.setTestSectionName("General Lab");

        // Set up default notebook with one department
        Set<TestSection> departments = new HashSet<>();
        departments.add(dept1);
        when(notebook.getDepartments()).thenReturn(departments);

        // Set up notebook entry linked to notebook
        entry = new NotebookEntry();
        entry.setId(ENTRY_ID);
        entry.setNotebook(notebook);
    }

    @Test
    public void testValidateSpecimenTypesForEntry_AllowedTypes() {
        // Arrange
        when(notebookEntryService.getMatch("id", ENTRY_ID)).thenReturn(Optional.of(entry));
        when(departmentSampleTypeService.getSampleTypesForDepartments(anyList()))
                .thenReturn(Arrays.asList(sputumType, balType));

        ParsedManifest manifest = createManifest(
                createRow(2, "Sputum", 1),
                createRow(3, "BAL", 2));

        // Act
        List<ParseError> errors = tbManifestImportService.validateSpecimenTypesForEntry(ENTRY_ID, manifest);

        // Assert
        assertTrue("Should have no errors for allowed specimen types", errors.isEmpty());
        verify(departmentSampleTypeService).getSampleTypesForDepartments(anyList());
    }

    @Test
    public void testValidateSpecimenTypesForEntry_DisallowedType() {
        // Arrange
        when(notebookEntryService.getMatch("id", ENTRY_ID)).thenReturn(Optional.of(entry));
        when(departmentSampleTypeService.getSampleTypesForDepartments(anyList()))
                .thenReturn(Arrays.asList(sputumType, balType)); // Only Sputum and BAL allowed

        ParsedManifest manifest = createManifest(
                createRow(2, "Sputum", 1),
                createRow(3, "Blood", 1)); // Blood not allowed

        // Act
        List<ParseError> errors = tbManifestImportService.validateSpecimenTypesForEntry(ENTRY_ID, manifest);

        // Assert
        assertEquals("Should have 1 error for disallowed specimen type", 1, errors.size());
        assertEquals("Error should be on row 3", 3, errors.get(0).rowNumber());
        assertEquals("Error column should be specimenType", "specimenType", errors.get(0).column());
        assertTrue("Error message should mention not allowed for linked departments",
                errors.get(0).message().contains("not allowed for the linked departments"));
    }

    @Test
    public void testValidateSpecimenTypesForEntry_MultipleDepartments() {
        // Arrange - Entry linked to notebook with multiple departments
        Set<TestSection> multiDepts = new HashSet<>();
        multiDepts.add(dept1);
        multiDepts.add(dept2);
        when(notebook.getDepartments()).thenReturn(multiDepts);

        when(notebookEntryService.getMatch("id", ENTRY_ID)).thenReturn(Optional.of(entry));
        // Union of departments allows Sputum, BAL, and Blood
        when(departmentSampleTypeService.getSampleTypesForDepartments(anyList()))
                .thenReturn(Arrays.asList(sputumType, balType, bloodType));

        ParsedManifest manifest = createManifest(createRow(2, "Sputum", 1), createRow(3, "Blood", 1),
                createRow(4, "BAL", 1));

        // Act
        List<ParseError> errors = tbManifestImportService.validateSpecimenTypesForEntry(ENTRY_ID, manifest);

        // Assert
        assertTrue("Should have no errors - union of departments allows all types", errors.isEmpty());
    }

    @Test
    public void testValidateSpecimenTypesForEntry_NoDepartmentsLinked() {
        // Arrange - Notebook with no departments linked
        when(notebook.getDepartments()).thenReturn(Collections.emptySet());

        when(notebookEntryService.getMatch("id", ENTRY_ID)).thenReturn(Optional.of(entry));

        // Mock global validation fallback - Sputum exists in global type_of_sample
        TypeOfSample globalSputum = new TypeOfSample();
        globalSputum.setDescription("Sputum");
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(any(), eq(true))).thenReturn(globalSputum);

        ParsedManifest manifest = createManifest(createRow(2, "Sputum", 1));

        // Act
        List<ParseError> errors = tbManifestImportService.validateSpecimenTypesForEntry(ENTRY_ID, manifest);

        // Assert
        assertTrue("Should fall back to global validation and pass", errors.isEmpty());
        // Should NOT call departmentSampleTypeService since no departments linked
        verify(departmentSampleTypeService, never()).getSampleTypesForDepartments(anyList());
        // Should call global validation
        verify(typeOfSampleService).getTypeOfSampleByDescriptionAndDomain(any(), eq(true));
    }

    @Test
    public void testValidateSpecimenTypesForEntry_EmptyManifest() {
        // Arrange
        when(notebookEntryService.getMatch("id", ENTRY_ID)).thenReturn(Optional.of(entry));
        when(departmentSampleTypeService.getSampleTypesForDepartments(anyList()))
                .thenReturn(Arrays.asList(sputumType));

        ParsedManifest manifest = createManifest(); // Empty manifest

        // Act
        List<ParseError> errors = tbManifestImportService.validateSpecimenTypesForEntry(ENTRY_ID, manifest);

        // Assert
        assertTrue("Should have no errors for empty manifest", errors.isEmpty());
    }

    @Test
    public void testValidateSpecimenTypesForEntry_EntryNotFound() {
        // Arrange
        when(notebookEntryService.getMatch("id", ENTRY_ID)).thenReturn(Optional.empty());

        // Mock global validation fallback
        TypeOfSample globalSputum = new TypeOfSample();
        globalSputum.setDescription("Sputum");
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(any(), eq(true)))
                .thenReturn(globalSputum);

        ParsedManifest manifest = createManifest(createRow(2, "Sputum", 1));

        // Act
        List<ParseError> errors = tbManifestImportService.validateSpecimenTypesForEntry(ENTRY_ID, manifest);

        // Assert - falls back to global validation
        assertTrue("Should fall back to global validation when entry not found", errors.isEmpty());
        verify(departmentSampleTypeService, never()).getSampleTypesForDepartments(anyList());
    }

    @Test
    public void testValidateSpecimenTypesForEntry_CaseInsensitive() {
        // Arrange
        when(notebookEntryService.getMatch("id", ENTRY_ID)).thenReturn(Optional.of(entry));
        when(departmentSampleTypeService.getSampleTypesForDepartments(anyList()))
                .thenReturn(Arrays.asList(sputumType)); // "Sputum" in DB

        // Manifest has different casing
        ParsedManifest manifest = createManifest(
                createRow(2, "SPUTUM", 1), // Uppercase
                createRow(3, "sputum", 1)); // Lowercase

        // Act
        List<ParseError> errors = tbManifestImportService.validateSpecimenTypesForEntry(ENTRY_ID, manifest);

        // Assert
        assertTrue("Should be case-insensitive and have no errors", errors.isEmpty());
    }

    @Test
    public void testValidateSpecimenTypesForEntry_NullNotebook() {
        // Arrange - Entry with null notebook
        NotebookEntry entryNoNotebook = new NotebookEntry();
        entryNoNotebook.setId(ENTRY_ID);
        entryNoNotebook.setNotebook(null);

        when(notebookEntryService.getMatch("id", ENTRY_ID)).thenReturn(Optional.of(entryNoNotebook));

        // Mock global validation fallback
        TypeOfSample globalSputum = new TypeOfSample();
        globalSputum.setDescription("Sputum");
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(any(), eq(true))).thenReturn(globalSputum);

        ParsedManifest manifest = createManifest(createRow(2, "Sputum", 1));

        // Act
        List<ParseError> errors = tbManifestImportService.validateSpecimenTypesForEntry(ENTRY_ID, manifest);

        // Assert
        assertTrue("Should fall back to global validation when notebook is null", errors.isEmpty());
        verify(departmentSampleTypeService, never()).getSampleTypesForDepartments(anyList());
        verify(typeOfSampleService).getTypeOfSampleByDescriptionAndDomain(any(), eq(true));
    }

    @Test
    public void testValidateSpecimenTypesForEntry_MultipleInvalidTypes() {
        // Arrange
        when(notebookEntryService.getMatch("id", ENTRY_ID)).thenReturn(Optional.of(entry));
        when(departmentSampleTypeService.getSampleTypesForDepartments(anyList()))
                .thenReturn(Arrays.asList(sputumType)); // Only Sputum allowed

        ParsedManifest manifest = createManifest(
                createRow(2, "Blood", 1), // Invalid
                createRow(3, "Sputum", 1), // Valid
                createRow(4, "Urine", 1), // Invalid
                createRow(5, "BAL", 1)); // Invalid

        // Act
        List<ParseError> errors = tbManifestImportService.validateSpecimenTypesForEntry(ENTRY_ID, manifest);

        // Assert
        assertEquals("Should have 3 errors for invalid specimen types", 3, errors.size());

        // Verify error row numbers
        Set<Integer> errorRows = new HashSet<>();
        for (ParseError error : errors) {
            errorRows.add(error.rowNumber());
        }
        assertTrue("Row 2 (Blood) should have error", errorRows.contains(2));
        assertTrue("Row 4 (Urine) should have error", errorRows.contains(4));
        assertTrue("Row 5 (BAL) should have error", errorRows.contains(5));
        assertFalse("Row 3 (Sputum) should NOT have error", errorRows.contains(3));
    }

    @Test
    public void testValidateSpecimenTypesForEntry_MixedValidInvalid() {
        // Arrange
        when(notebookEntryService.getMatch("id", ENTRY_ID)).thenReturn(Optional.of(entry));
        when(departmentSampleTypeService.getSampleTypesForDepartments(anyList()))
                .thenReturn(Arrays.asList(sputumType, balType)); // Sputum and BAL allowed

        ParsedManifest manifest = createManifest(
                createRow(2, "Sputum", 1), // Valid
                createRow(3, "Blood", 1), // Invalid
                createRow(4, "BAL", 1), // Valid
                createRow(5, "Urine", 1), // Invalid
                createRow(6, "Sputum", 2)); // Valid

        // Act
        List<ParseError> errors = tbManifestImportService.validateSpecimenTypesForEntry(ENTRY_ID, manifest);

        // Assert
        assertEquals("Should have exactly 2 errors (Blood and Urine)", 2, errors.size());

        // Find errors by row
        ParseError bloodError = errors.stream().filter(e -> e.rowNumber() == 3).findFirst().orElse(null);
        ParseError urineError = errors.stream().filter(e -> e.rowNumber() == 5).findFirst().orElse(null);

        assertNotNull("Should have error for Blood on row 3", bloodError);
        assertTrue("Blood error should mention 'Blood'", bloodError.message().contains("Blood"));
        assertTrue("Blood error should mention 'not allowed for the linked departments'",
                bloodError.message().contains("not allowed for the linked departments"));

        assertNotNull("Should have error for Urine on row 5", urineError);
        assertTrue("Urine error should mention 'Urine'", urineError.message().contains("Urine"));
    }

    // Helper methods

    private TBManifestRow createRow(int rowNumber, String specimenType, int numOfSamples) {
        return new TBManifestRow(rowNumber, "TB-" + rowNumber, // sampleId
                specimenType, // specimenType
                "Good", // specimenQuality
                "DOC-" + rowNumber, // documentNumber
                "Hospital A", // referringFacility
                "Patient " + rowNumber, // patientName
                "30", // patientAge
                "M", // patientSex
                "PAT-" + rowNumber, // patientId
                null, // studyId
                null, // patientAddress
                null, // patientPhone
                null, // physicianPhone
                "Yes", // consentStatus
                "New", // treatmentHistory
                "Yes", // culture
                "Yes", // smearMicroscopy
                null, // genexpert
                null, // identification
                null, // dstFirstLine
                null, // dstSecondLine
                null, // intendedMethod
                "Lab A", // receivedSite
                "2025-01-01", // receivedDate
                "10:00", // receivedTime
                numOfSamples);
    }

    private ParsedManifest createManifest(TBManifestRow... rows) {
        return new ParsedManifest(Arrays.asList(rows), Collections.emptyList());
    }

    private List<String> anyList() {
        return any();
    }
}
