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
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;

@RunWith(MockitoJUnitRunner.class)
public class TestConfigurationHandlerTest {

    @Mock
    private TestService testService;

    @Mock
    private TestSectionService testSectionService;

    @Mock
    private LocalizationService localizationService;

    @Mock
    private TypeOfSampleService typeOfSampleService;

    @Mock
    private TypeOfSampleTestService typeOfSampleTestService;

    @Mock
    private UnitOfMeasureService unitOfMeasureService;

    @Mock
    private DisplayListService displayListService;

    @InjectMocks
    private TestConfigurationHandler handler;

    private TestSection testSection;
    private TypeOfSample sampleType;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        testSection = new TestSection();
        testSection.setId("1");
        testSection.setTestSectionName("Hematology");

        sampleType = new TypeOfSample();
        sampleType.setId("100");
        sampleType.setDescription("Whole Blood");
    }

    @Test
    public void testGetDomainName() {
        assertEquals("tests", handler.getDomainName());
    }

    @Test
    public void testGetFileExtension() {
        assertEquals("csv", handler.getFileExtension());
    }

    @Test
    public void testProcessConfiguration_NewTests() throws Exception {
        // Given
        String csv = "testName,testSection,sampleType,loinc,isActive\n"
                + "Complete Blood Count,Hematology,Whole Blood,58410-2,Y\n"
                + "Hemoglobin,Hematology,Whole Blood,718-7,Y\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        List<TypeOfSample> allSampleTypes = new ArrayList<>();
        allSampleTypes.add(sampleType);

        // Mock services
        when(testSectionService.getTestSectionByName("Hematology")).thenReturn(testSection);
        when(testService.getTestByDescription(anyString())).thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1", "2", "3", "4");
        when(testService.insert(any(org.openelisglobal.test.valueholder.Test.class))).thenReturn("1", "2");
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(allSampleTypes);
        when(typeOfSampleTestService.getTypeOfSampleTestsForTest(anyString())).thenReturn(Collections.emptyList());
        when(typeOfSampleTestService.insert(any(TypeOfSampleTest.class))).thenReturn("1", "2");
        when(unitOfMeasureService.getAll()).thenReturn(Collections.emptyList());

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then
            verify(testService, times(2)).insert(any(org.openelisglobal.test.valueholder.Test.class));
            verify(typeOfSampleTestService, times(2)).insert(any(TypeOfSampleTest.class));
        }
    }

    @Test
    public void testProcessConfiguration_UpdateExistingTest() throws Exception {
        // Given
        String csv = "testName,testSection,sampleType,loinc,isActive\n"
                + "Complete Blood Count,Hematology,Whole Blood,58410-2,Y\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        org.openelisglobal.test.valueholder.Test existingTest = new org.openelisglobal.test.valueholder.Test();
        existingTest.setId("1");
        existingTest.setDescription("Complete Blood Count");

        List<TypeOfSample> allSampleTypes = new ArrayList<>();
        allSampleTypes.add(sampleType);

        // Mock services
        when(testSectionService.getTestSectionByName("Hematology")).thenReturn(testSection);
        when(testService.getTestByDescription("Complete Blood Count")).thenReturn(existingTest);
        when(testService.update(any(org.openelisglobal.test.valueholder.Test.class))).thenReturn(existingTest);
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(allSampleTypes);
        when(typeOfSampleTestService.getTypeOfSampleTestsForTest("1")).thenReturn(Collections.emptyList());
        when(typeOfSampleTestService.insert(any(TypeOfSampleTest.class))).thenReturn("1");
        when(unitOfMeasureService.getAll()).thenReturn(Collections.emptyList());

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then - should update, not insert
            verify(testService, never()).insert(any(org.openelisglobal.test.valueholder.Test.class));
            verify(testService, times(1)).update(any(org.openelisglobal.test.valueholder.Test.class));
        }
    }

    @Test
    public void testProcessConfiguration_EmptyFile_ThrowsException() throws Exception {
        // Given
        String csv = "";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Expect
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Test configuration file test.csv is empty");

        // When
        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testProcessConfiguration_MissingTestNameColumn_ThrowsException() throws Exception {
        // Given
        String csv = "testSection,sampleType\n" + "Hematology,Whole Blood\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Expect
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Test configuration file test.csv must have a 'testName' column");

        // When
        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testProcessConfiguration_MissingTestSectionColumn_ThrowsException() throws Exception {
        // Given
        String csv = "testName,sampleType\n" + "Complete Blood Count,Whole Blood\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Expect
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Test configuration file test.csv must have a 'testSection' column");

        // When
        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testProcessConfiguration_TestSectionNotFound() throws Exception {
        // Given
        String csv = "testName,testSection,sampleType\n" + "Complete Blood Count,NonExistent,Whole Blood\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock services
        when(testSectionService.getTestSectionByName("NonExistent")).thenReturn(null);

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then - should not insert anything as test section was not found
            verify(testService, never()).insert(any(org.openelisglobal.test.valueholder.Test.class));
        }
    }

    @Test
    public void testProcessConfiguration_MultipleSampleTypes() throws Exception {
        // Given - sample types separated by |
        String csv = "testName,testSection,sampleType\n" + "Glucose,Biochemistry,Serum|Plasma|Whole Blood\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        TestSection biochemistrySection = new TestSection();
        biochemistrySection.setId("2");
        biochemistrySection.setTestSectionName("Biochemistry");

        TypeOfSample serum = new TypeOfSample();
        serum.setId("101");
        serum.setDescription("Serum");

        TypeOfSample plasma = new TypeOfSample();
        plasma.setId("102");
        plasma.setDescription("Plasma");

        TypeOfSample wholeBlood = new TypeOfSample();
        wholeBlood.setId("100");
        wholeBlood.setDescription("Whole Blood");

        List<TypeOfSample> allSampleTypes = new ArrayList<>();
        allSampleTypes.add(serum);
        allSampleTypes.add(plasma);
        allSampleTypes.add(wholeBlood);

        // Mock services
        when(testSectionService.getTestSectionByName("Biochemistry")).thenReturn(biochemistrySection);
        when(testService.getTestByDescription("Glucose")).thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1", "2");
        when(testService.insert(any(org.openelisglobal.test.valueholder.Test.class))).thenReturn("1");
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(allSampleTypes);
        when(typeOfSampleTestService.getTypeOfSampleTestsForTest("1")).thenReturn(Collections.emptyList());
        when(typeOfSampleTestService.insert(any(TypeOfSampleTest.class))).thenReturn("1", "2", "3");
        when(unitOfMeasureService.getAll()).thenReturn(Collections.emptyList());

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then - should create 3 sample type mappings
            verify(testService, times(1)).insert(any(org.openelisglobal.test.valueholder.Test.class));
            verify(typeOfSampleTestService, times(3)).insert(any(TypeOfSampleTest.class));
        }
    }

    @Test
    public void testProcessConfiguration_MinimalColumns() throws Exception {
        // Given - only required columns
        String csv = "testName,testSection\n" + "Simple Test,Hematology\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock services
        when(testSectionService.getTestSectionByName("Hematology")).thenReturn(testSection);
        when(testService.getTestByDescription("Simple Test")).thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1", "2");
        when(testService.insert(any(org.openelisglobal.test.valueholder.Test.class))).thenReturn("1");
        when(unitOfMeasureService.getAll()).thenReturn(Collections.emptyList());

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then
            verify(testService, times(1)).insert(any(org.openelisglobal.test.valueholder.Test.class));
        }
    }

    @Test
    public void testProcessConfiguration_QuotedFields() throws Exception {
        // Given
        String csv = "testName,testSection,englishName,frenchName\n"
                + "\"Test, With Comma\",Hematology,\"Test, With Comma\",\"Test, Avec Virgule\"\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock services
        when(testSectionService.getTestSectionByName("Hematology")).thenReturn(testSection);
        when(testService.getTestByDescription("Test, With Comma")).thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1", "2");
        when(testService.insert(any(org.openelisglobal.test.valueholder.Test.class))).thenReturn("1");
        when(unitOfMeasureService.getAll()).thenReturn(Collections.emptyList());

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then
            verify(testService, times(1)).insert(any(org.openelisglobal.test.valueholder.Test.class));
        }
    }

    @Test
    public void testProcessConfiguration_SkipExistingSampleTypeMapping() throws Exception {
        // Given
        String csv = "testName,testSection,sampleType\n" + "Hemoglobin,Hematology,Whole Blood\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        org.openelisglobal.test.valueholder.Test existingTest = new org.openelisglobal.test.valueholder.Test();
        existingTest.setId("1");
        existingTest.setDescription("Hemoglobin");

        // Existing mapping
        TypeOfSampleTest existingMapping = new TypeOfSampleTest();
        existingMapping.setTestId("1");
        existingMapping.setTypeOfSampleId("100");
        List<TypeOfSampleTest> existingMappings = new ArrayList<>();
        existingMappings.add(existingMapping);

        List<TypeOfSample> allSampleTypes = new ArrayList<>();
        allSampleTypes.add(sampleType);

        // Mock services
        when(testSectionService.getTestSectionByName("Hematology")).thenReturn(testSection);
        when(testService.getTestByDescription("Hemoglobin")).thenReturn(existingTest);
        when(testService.update(any(org.openelisglobal.test.valueholder.Test.class))).thenReturn(existingTest);
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(allSampleTypes);
        when(typeOfSampleTestService.getTypeOfSampleTestsForTest("1")).thenReturn(existingMappings);
        when(unitOfMeasureService.getAll()).thenReturn(Collections.emptyList());

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then - should not create new mapping as it already exists
            verify(typeOfSampleTestService, never()).insert(any(TypeOfSampleTest.class));
        }
    }
}
