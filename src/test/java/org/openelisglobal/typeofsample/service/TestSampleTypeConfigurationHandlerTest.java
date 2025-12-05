package org.openelisglobal.typeofsample.service;

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
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;

@RunWith(MockitoJUnitRunner.class)
public class TestSampleTypeConfigurationHandlerTest {

    @Mock
    private TestService testService;

    @Mock
    private TypeOfSampleService typeOfSampleService;

    @Mock
    private TypeOfSampleTestService typeOfSampleTestService;

    @Mock
    private DisplayListService displayListService;

    @InjectMocks
    private TestSampleTypeConfigurationHandler handler;

    private org.openelisglobal.test.valueholder.Test testEntity;
    private TypeOfSample sampleType;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        testEntity = new org.openelisglobal.test.valueholder.Test();
        testEntity.setId("1");
        testEntity.setDescription("Test Description");

        sampleType = new TypeOfSample();
        sampleType.setId("100");
        sampleType.setDescription("Whole Blood");
        sampleType.setLocalAbbreviation("WB");
    }

    @Test
    public void testGetDomainName() {
        assertEquals("test-sample-types", handler.getDomainName());
    }

    @Test
    public void testGetFileExtension() {
        assertEquals("csv", handler.getFileExtension());
    }

    @Test
    public void testProcessConfiguration_NewMappings() throws Exception {
        // Given
        String csv = "testName,sampleType\n" + "Complete Blood Count,Whole Blood\n" + "Hemoglobin,Serum\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        org.openelisglobal.test.valueholder.Test test1 = new org.openelisglobal.test.valueholder.Test();
        test1.setId("1");
        org.openelisglobal.test.valueholder.Test test2 = new org.openelisglobal.test.valueholder.Test();
        test2.setId("2");

        TypeOfSample sampleType1 = new TypeOfSample();
        sampleType1.setId("100");
        sampleType1.setDescription("Whole Blood");

        TypeOfSample sampleType2 = new TypeOfSample();
        sampleType2.setId("101");
        sampleType2.setDescription("Serum");

        List<TypeOfSample> allSampleTypes = new ArrayList<>();
        allSampleTypes.add(sampleType1);
        allSampleTypes.add(sampleType2);

        // Mock test service
        when(testService.getTestByLocalizedName("Complete Blood Count")).thenReturn(test1);
        when(testService.getTestByLocalizedName("Hemoglobin")).thenReturn(test2);

        // Mock sample type service
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(allSampleTypes);

        // Mock no existing mappings
        when(typeOfSampleTestService.getTypeOfSampleTestsForTest(anyString())).thenReturn(Collections.emptyList());
        when(typeOfSampleTestService.insert(any(TypeOfSampleTest.class))).thenReturn("1", "2");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then
            verify(typeOfSampleTestService, times(2)).insert(any(TypeOfSampleTest.class));
        }
    }

    @Test
    public void testProcessConfiguration_SkipExistingMappings() throws Exception {
        // Given
        String csv = "testName,sampleType\n" + "Complete Blood Count,Whole Blood\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        org.openelisglobal.test.valueholder.Test test1 = new org.openelisglobal.test.valueholder.Test();
        test1.setId("1");

        TypeOfSample sampleType1 = new TypeOfSample();
        sampleType1.setId("100");
        sampleType1.setDescription("Whole Blood");

        List<TypeOfSample> allSampleTypes = new ArrayList<>();
        allSampleTypes.add(sampleType1);

        // Existing mapping
        TypeOfSampleTest existingMapping = new TypeOfSampleTest();
        existingMapping.setTestId("1");
        existingMapping.setTypeOfSampleId("100");
        List<TypeOfSampleTest> existingMappings = new ArrayList<>();
        existingMappings.add(existingMapping);

        // Mock test service
        when(testService.getTestByLocalizedName("Complete Blood Count")).thenReturn(test1);

        // Mock sample type service
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(allSampleTypes);

        // Mock existing mapping
        when(typeOfSampleTestService.getTypeOfSampleTestsForTest("1")).thenReturn(existingMappings);

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then - should not insert as mapping already exists
            verify(typeOfSampleTestService, never()).insert(any(TypeOfSampleTest.class));
        }
    }

    @Test
    public void testProcessConfiguration_EmptyFile_ThrowsException() throws Exception {
        // Given
        String csv = "";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Expect
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Test-sample type configuration file test.csv is empty");

        // When
        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testProcessConfiguration_MissingTestNameColumn_ThrowsException() throws Exception {
        // Given
        String csv = "sampleType\n" + "Whole Blood\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Expect
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Test-sample type configuration file test.csv must have a 'testName' column");

        // When
        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testProcessConfiguration_MissingSampleTypeColumn_ThrowsException() throws Exception {
        // Given
        String csv = "testName\n" + "Complete Blood Count\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Expect
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Test-sample type configuration file test.csv must have a 'sampleType' column");

        // When
        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testProcessConfiguration_TestNotFound() throws Exception {
        // Given
        String csv = "testName,sampleType\n" + "NonExistent Test,Whole Blood\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        TypeOfSample sampleType1 = new TypeOfSample();
        sampleType1.setId("100");
        sampleType1.setDescription("Whole Blood");

        List<TypeOfSample> allSampleTypes = new ArrayList<>();
        allSampleTypes.add(sampleType1);

        // Mock test service to return null
        when(testService.getTestByLocalizedName("NonExistent Test")).thenReturn(null);
        when(testService.getTestByDescription("NonExistent Test")).thenReturn(null);
        when(testService.getTestByName("NonExistent Test")).thenReturn(null);

        // Mock sample type service
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(allSampleTypes);

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then - should not insert anything as test was not found
            verify(typeOfSampleTestService, never()).insert(any(TypeOfSampleTest.class));
        }
    }

    @Test
    public void testProcessConfiguration_SampleTypeNotFound() throws Exception {
        // Given
        String csv = "testName,sampleType\n" + "Complete Blood Count,NonExistent Sample Type\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        org.openelisglobal.test.valueholder.Test test1 = new org.openelisglobal.test.valueholder.Test();
        test1.setId("1");

        // Mock test service
        when(testService.getTestByLocalizedName("Complete Blood Count")).thenReturn(test1);

        // Mock sample type service to return empty list
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(Collections.emptyList());

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then - should not insert anything as sample type was not found
            verify(typeOfSampleTestService, never()).insert(any(TypeOfSampleTest.class));
        }
    }

    @Test
    public void testProcessConfiguration_QuotedFields() throws Exception {
        // Given
        String csv = "testName,sampleType\n" + "\"Test, With Comma\",\"Sample Type, With Comma\"\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        org.openelisglobal.test.valueholder.Test test1 = new org.openelisglobal.test.valueholder.Test();
        test1.setId("1");

        TypeOfSample sampleType1 = new TypeOfSample();
        sampleType1.setId("100");
        sampleType1.setDescription("Sample Type, With Comma");

        List<TypeOfSample> allSampleTypes = new ArrayList<>();
        allSampleTypes.add(sampleType1);

        // Mock test service
        when(testService.getTestByLocalizedName("Test, With Comma")).thenReturn(test1);

        // Mock sample type service
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(allSampleTypes);

        // Mock no existing mappings
        when(typeOfSampleTestService.getTypeOfSampleTestsForTest("1")).thenReturn(Collections.emptyList());
        when(typeOfSampleTestService.insert(any(TypeOfSampleTest.class))).thenReturn("1");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then
            verify(typeOfSampleTestService, times(1)).insert(any(TypeOfSampleTest.class));
        }
    }

    @Test
    public void testProcessConfiguration_EmptyLinesIgnored() throws Exception {
        // Given
        String csv = "testName,sampleType\n" + "Test 1,Whole Blood\n" + "\n" + "Test 2,Serum\n" + "\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        org.openelisglobal.test.valueholder.Test test1 = new org.openelisglobal.test.valueholder.Test();
        test1.setId("1");
        org.openelisglobal.test.valueholder.Test test2 = new org.openelisglobal.test.valueholder.Test();
        test2.setId("2");

        TypeOfSample sampleType1 = new TypeOfSample();
        sampleType1.setId("100");
        sampleType1.setDescription("Whole Blood");

        TypeOfSample sampleType2 = new TypeOfSample();
        sampleType2.setId("101");
        sampleType2.setDescription("Serum");

        List<TypeOfSample> allSampleTypes = new ArrayList<>();
        allSampleTypes.add(sampleType1);
        allSampleTypes.add(sampleType2);

        // Mock test service
        when(testService.getTestByLocalizedName("Test 1")).thenReturn(test1);
        when(testService.getTestByLocalizedName("Test 2")).thenReturn(test2);

        // Mock sample type service
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(allSampleTypes);

        // Mock no existing mappings
        when(typeOfSampleTestService.getTypeOfSampleTestsForTest(anyString())).thenReturn(Collections.emptyList());
        when(typeOfSampleTestService.insert(any(TypeOfSampleTest.class))).thenReturn("1", "2");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then - should only process 2 entries despite empty lines
            verify(typeOfSampleTestService, times(2)).insert(any(TypeOfSampleTest.class));
        }
    }

    @Test
    public void testProcessConfiguration_CaseInsensitiveHeaders() throws Exception {
        // Given
        String csv = "TESTNAME,SampleType\n" + "Complete Blood Count,Whole Blood\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        org.openelisglobal.test.valueholder.Test test1 = new org.openelisglobal.test.valueholder.Test();
        test1.setId("1");

        TypeOfSample sampleType1 = new TypeOfSample();
        sampleType1.setId("100");
        sampleType1.setDescription("Whole Blood");

        List<TypeOfSample> allSampleTypes = new ArrayList<>();
        allSampleTypes.add(sampleType1);

        // Mock test service
        when(testService.getTestByLocalizedName("Complete Blood Count")).thenReturn(test1);

        // Mock sample type service
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(allSampleTypes);

        // Mock no existing mappings
        when(typeOfSampleTestService.getTypeOfSampleTestsForTest("1")).thenReturn(Collections.emptyList());
        when(typeOfSampleTestService.insert(any(TypeOfSampleTest.class))).thenReturn("1");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then
            verify(typeOfSampleTestService, times(1)).insert(any(TypeOfSampleTest.class));
        }
    }

    @Test
    public void testProcessConfiguration_MultipleTestsToSameSampleType() throws Exception {
        // Given
        String csv = "testName,sampleType\n" + "Test 1,Whole Blood\n" + "Test 2,Whole Blood\n" + "Test 3,Whole Blood\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        org.openelisglobal.test.valueholder.Test test1 = new org.openelisglobal.test.valueholder.Test();
        test1.setId("1");
        org.openelisglobal.test.valueholder.Test test2 = new org.openelisglobal.test.valueholder.Test();
        test2.setId("2");
        org.openelisglobal.test.valueholder.Test test3 = new org.openelisglobal.test.valueholder.Test();
        test3.setId("3");

        TypeOfSample sampleType1 = new TypeOfSample();
        sampleType1.setId("100");
        sampleType1.setDescription("Whole Blood");

        List<TypeOfSample> allSampleTypes = new ArrayList<>();
        allSampleTypes.add(sampleType1);

        // Mock test service
        when(testService.getTestByLocalizedName("Test 1")).thenReturn(test1);
        when(testService.getTestByLocalizedName("Test 2")).thenReturn(test2);
        when(testService.getTestByLocalizedName("Test 3")).thenReturn(test3);

        // Mock sample type service
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(allSampleTypes);

        // Mock no existing mappings
        when(typeOfSampleTestService.getTypeOfSampleTestsForTest(anyString())).thenReturn(Collections.emptyList());
        when(typeOfSampleTestService.insert(any(TypeOfSampleTest.class))).thenReturn("1", "2", "3");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then
            verify(typeOfSampleTestService, times(3)).insert(any(TypeOfSampleTest.class));
        }
    }

    @Test
    public void testProcessConfiguration_SameTestToMultipleSampleTypes() throws Exception {
        // Given
        String csv = "testName,sampleType\n" + "Glucose,Serum\n" + "Glucose,Plasma\n" + "Glucose,Whole Blood\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        org.openelisglobal.test.valueholder.Test glucoseTest = new org.openelisglobal.test.valueholder.Test();
        glucoseTest.setId("1");

        TypeOfSample serum = new TypeOfSample();
        serum.setId("100");
        serum.setDescription("Serum");

        TypeOfSample plasma = new TypeOfSample();
        plasma.setId("101");
        plasma.setDescription("Plasma");

        TypeOfSample wholeBlood = new TypeOfSample();
        wholeBlood.setId("102");
        wholeBlood.setDescription("Whole Blood");

        List<TypeOfSample> allSampleTypes = new ArrayList<>();
        allSampleTypes.add(serum);
        allSampleTypes.add(plasma);
        allSampleTypes.add(wholeBlood);

        // Mock test service - always return same test
        when(testService.getTestByLocalizedName("Glucose")).thenReturn(glucoseTest);

        // Mock sample type service
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(allSampleTypes);

        // Mock no existing mappings
        when(typeOfSampleTestService.getTypeOfSampleTestsForTest("1")).thenReturn(Collections.emptyList());
        when(typeOfSampleTestService.insert(any(TypeOfSampleTest.class))).thenReturn("1", "2", "3");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then - should create 3 mappings for the same test to different sample types
            verify(typeOfSampleTestService, times(3)).insert(any(TypeOfSampleTest.class));
        }
    }

    @Test
    public void testProcessConfiguration_FindByLocalAbbreviation() throws Exception {
        // Given
        String csv = "testName,sampleType\n" + "Complete Blood Count,WB\n"; // WB is abbreviation for Whole Blood

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        org.openelisglobal.test.valueholder.Test test1 = new org.openelisglobal.test.valueholder.Test();
        test1.setId("1");

        TypeOfSample sampleType1 = new TypeOfSample();
        sampleType1.setId("100");
        sampleType1.setDescription("Whole Blood");
        sampleType1.setLocalAbbreviation("WB");

        List<TypeOfSample> allSampleTypes = new ArrayList<>();
        allSampleTypes.add(sampleType1);

        // Mock test service
        when(testService.getTestByLocalizedName("Complete Blood Count")).thenReturn(test1);

        // Mock sample type service
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(allSampleTypes);

        // Mock no existing mappings
        when(typeOfSampleTestService.getTypeOfSampleTestsForTest("1")).thenReturn(Collections.emptyList());
        when(typeOfSampleTestService.insert(any(TypeOfSampleTest.class))).thenReturn("1");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then
            verify(typeOfSampleTestService, times(1)).insert(any(TypeOfSampleTest.class));
        }
    }
}
