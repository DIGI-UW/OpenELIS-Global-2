package org.openelisglobal.test.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.test.valueholder.TestSection;

@RunWith(MockitoJUnitRunner.class)
public class TestSectionConfigurationHandlerTest {

    @Mock
    private TestSectionService testSectionService;

    @Mock
    private LocalizationService localizationService;

    @Mock
    private DisplayListService displayListService;

    @InjectMocks
    private TestSectionConfigurationHandler handler;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        // Common setup if needed
    }

    @Test
    public void testGetDomainName() {
        assertEquals("test-sections", handler.getDomainName());
    }

    @Test
    public void testGetFileExtension() {
        assertEquals("csv", handler.getFileExtension());
    }

    @Test
    public void testProcessConfiguration_NewTestSections() throws Exception {
        // Given
        String csv = "testSectionName,description,isActive,sortOrder,englishName,frenchName\n"
                + "Hematology,Hematology Department,Y,1,Hematology,Hématologie\n"
                + "Biochemistry,Biochemistry Department,Y,2,Biochemistry,Biochimie\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock services
        when(testSectionService.getAllTestSections()).thenReturn(Collections.emptyList());
        when(testSectionService.getTestSectionByName(anyString())).thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1", "2");
        when(testSectionService.insert(any(TestSection.class))).thenReturn("1", "2");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test-sections.csv");

            // Then
            verify(testSectionService, times(2)).insert(any(TestSection.class));
            verify(localizationService, times(2)).insert(any(Localization.class));
        }
    }

    @Test
    public void testProcessConfiguration_UpdateExistingTestSection() throws Exception {
        // Given
        String csv = "testSectionName,description,isActive,sortOrder\n" + "Hematology,Updated Description,Y,1\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        TestSection existingSection = new TestSection();
        existingSection.setId("1");
        existingSection.setTestSectionName("Hematology");
        existingSection.setDescription("Old Description");

        // Mock services
        when(testSectionService.getAllTestSections()).thenReturn(Collections.emptyList());
        when(testSectionService.getTestSectionByName("Hematology")).thenReturn(existingSection);
        when(testSectionService.update(any(TestSection.class))).thenReturn(existingSection);

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test-sections.csv");

            // Then - should update, not insert
            verify(testSectionService, never()).insert(any(TestSection.class));
            verify(testSectionService, times(1)).update(any(TestSection.class));
        }
    }

    @Test
    public void testProcessConfiguration_EmptyFile_ThrowsException() throws Exception {
        // Given
        String csv = "";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Expect
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Test section configuration file test-sections.csv is empty");

        // When
        handler.processConfiguration(inputStream, "test-sections.csv");
    }

    @Test
    public void testProcessConfiguration_MissingTestSectionNameColumn_ThrowsException() throws Exception {
        // Given
        String csv = "description,isActive\n" + "Some Description,Y\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Expect
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Test section configuration file test-sections.csv must have a 'testSectionName' column");

        // When
        handler.processConfiguration(inputStream, "test-sections.csv");
    }

    @Test
    public void testProcessConfiguration_MinimalColumns() throws Exception {
        // Given - only required column
        String csv = "testSectionName\n" + "Hematology\n" + "Biochemistry\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock services
        when(testSectionService.getAllTestSections()).thenReturn(Collections.emptyList());
        when(testSectionService.getTestSectionByName(anyString())).thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1", "2");
        when(testSectionService.insert(any(TestSection.class))).thenReturn("1", "2");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test-sections.csv");

            // Then
            verify(testSectionService, times(2)).insert(any(TestSection.class));
        }
    }

    @Test
    public void testProcessConfiguration_QuotedFields() throws Exception {
        // Given
        String csv = "testSectionName,description,englishName,frenchName\n"
                + "\"Section, With Comma\",\"Description, With Comma\",\"Section, With Comma\",\"Section, Avec Virgule\"\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock services
        when(testSectionService.getAllTestSections()).thenReturn(Collections.emptyList());
        when(testSectionService.getTestSectionByName("Section, With Comma")).thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1");
        when(testSectionService.insert(any(TestSection.class))).thenReturn("1");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test-sections.csv");

            // Then
            verify(testSectionService, times(1)).insert(any(TestSection.class));
        }
    }

    @Test
    public void testProcessConfiguration_SkipEmptyRows() throws Exception {
        // Given
        String csv = "testSectionName,description\n" + "Hematology,Hematology Department\n" + "\n"
                + "Biochemistry,Biochemistry Department\n" + "\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock services
        when(testSectionService.getAllTestSections()).thenReturn(Collections.emptyList());
        when(testSectionService.getTestSectionByName(anyString())).thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1", "2");
        when(testSectionService.insert(any(TestSection.class))).thenReturn("1", "2");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test-sections.csv");

            // Then - should only insert 2 sections (empty rows skipped)
            verify(testSectionService, times(2)).insert(any(TestSection.class));
        }
    }

    @Test
    public void testProcessConfiguration_SkipRowWithMissingTestSectionName() throws Exception {
        // Given - row with empty testSectionName
        String csv = "testSectionName,description\n" + ",Empty Name\n" + "Hematology,Hematology Department\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock services
        when(testSectionService.getAllTestSections()).thenReturn(Collections.emptyList());
        when(testSectionService.getTestSectionByName("Hematology")).thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1");
        when(testSectionService.insert(any(TestSection.class))).thenReturn("1");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test-sections.csv");

            // Then - should only insert 1 section (first row skipped due to empty name)
            verify(testSectionService, times(1)).insert(any(TestSection.class));
        }
    }

    @Test
    public void testProcessConfiguration_CaseInsensitiveHeaders() throws Exception {
        // Given
        String csv = "TESTSECTIONNAME,DESCRIPTION,ISACTIVE\n" + "Hematology,Hematology Department,Y\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock services
        when(testSectionService.getAllTestSections()).thenReturn(Collections.emptyList());
        when(testSectionService.getTestSectionByName("Hematology")).thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1");
        when(testSectionService.insert(any(TestSection.class))).thenReturn("1");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test-sections.csv");

            // Then
            verify(testSectionService, times(1)).insert(any(TestSection.class));
        }
    }

    @Test
    public void testProcessConfiguration_IsExternalFlag() throws Exception {
        // Given
        String csv = "testSectionName,isExternal\n" + "External Lab,Y\n" + "Internal Lab,N\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock services
        when(testSectionService.getAllTestSections()).thenReturn(Collections.emptyList());
        when(testSectionService.getTestSectionByName(anyString())).thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1", "2");
        when(testSectionService.insert(any(TestSection.class))).thenReturn("1", "2");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test-sections.csv");

            // Then
            verify(testSectionService, times(2)).insert(any(TestSection.class));
        }
    }

    @Test
    public void testProcessConfiguration_AutoSortOrder() throws Exception {
        // Given - existing sections with sort orders
        String csv = "testSectionName\n" + "New Section 1\n" + "New Section 2\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        TestSection existing1 = new TestSection();
        existing1.setSortOrderInt(5);
        TestSection existing2 = new TestSection();
        existing2.setSortOrderInt(10);
        List<TestSection> existingList = new ArrayList<>();
        existingList.add(existing1);
        existingList.add(existing2);

        // Mock services
        when(testSectionService.getAllTestSections()).thenReturn(existingList);
        when(testSectionService.getTestSectionByName(anyString())).thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1", "2");
        when(testSectionService.insert(any(TestSection.class))).thenReturn("1", "2");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test-sections.csv");

            // Then - sort order should start from max + 1 = 11
            verify(testSectionService, times(2)).insert(any(TestSection.class));
        }
    }

    @Test
    public void testProcessConfiguration_UpdateLocalization() throws Exception {
        // Given
        String csv = "testSectionName,englishName,frenchName\n" + "Hematology,New English,Nouveau Français\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        Localization existingLocalization = new Localization();
        existingLocalization.setId("1");
        existingLocalization.setEnglish("Old English");
        existingLocalization.setFrench("Vieux Français");

        TestSection existingSection = new TestSection();
        existingSection.setId("1");
        existingSection.setTestSectionName("Hematology");
        existingSection.setLocalization(existingLocalization);

        // Mock services
        when(testSectionService.getAllTestSections()).thenReturn(Collections.emptyList());
        when(testSectionService.getTestSectionByName("Hematology")).thenReturn(existingSection);
        when(testSectionService.update(any(TestSection.class))).thenReturn(existingSection);

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test-sections.csv");

            // Then
            verify(localizationService, times(1)).update(any(Localization.class));
        }
    }
}
