package org.openelisglobal.typeofsample.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

@RunWith(MockitoJUnitRunner.class)
public class TypeOfSampleConfigurationHandlerTest {

    @Mock
    private TypeOfSampleService typeOfSampleService;

    @Mock
    private LocalizationService localizationService;

    @Mock
    private DisplayListService displayListService;

    @InjectMocks
    private TypeOfSampleConfigurationHandler handler;

    private TypeOfSample existingSampleType;
    private Localization existingLocalization;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        existingLocalization = new Localization();
        existingLocalization.setId("1");
        existingLocalization.setEnglish("Whole Blood");
        existingLocalization.setFrench("Sang Total");

        existingSampleType = new TypeOfSample();
        existingSampleType.setId("1");
        existingSampleType.setDescription("Whole Blood");
        existingSampleType.setLocalAbbreviation("WB");
        existingSampleType.setDomain("H");
        existingSampleType.setActive(true);
        existingSampleType.setSortOrder(1);
        existingSampleType.setLocalization(existingLocalization);
    }

    @Test
    public void testGetDomainName() {
        assertEquals("sample-types", handler.getDomainName());
    }

    @Test
    public void testGetFileExtension() {
        assertEquals("csv", handler.getFileExtension());
    }

    @Test
    public void testProcessConfiguration_NewSampleTypes() throws Exception {
        // Given
        String csv = "description,localAbbreviation,domain,isActive,sortOrder,englishName,frenchName\n"
                + "Whole Blood,WB,H,Y,1,Whole Blood,Sang Total\n" + "Serum,SER,H,Y,2,Serum,Sérum\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock services
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(Collections.emptyList());
        when(typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain(anyString(), anyString())).thenReturn(null);
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(any(TypeOfSample.class), anyBoolean()))
                .thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1", "2");
        when(typeOfSampleService.insert(any(TypeOfSample.class))).thenReturn("1", "2");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then
            verify(localizationService, times(2)).insert(any(Localization.class));
            verify(typeOfSampleService, times(2)).insert(any(TypeOfSample.class));
        }
    }

    @Test
    public void testProcessConfiguration_UpdateExistingSampleType() throws Exception {
        // Given
        String csv = "description,localAbbreviation,domain,isActive,sortOrder,englishName,frenchName\n"
                + "Whole Blood Updated,WB,H,Y,1,Whole Blood Updated,Sang Total Modifié\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock services - sample type already exists
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(Collections.singletonList(existingSampleType));
        when(typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain("WB", "H")).thenReturn(existingSampleType);
        when(typeOfSampleService.update(any(TypeOfSample.class))).thenReturn(existingSampleType);

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then - should update, not insert
            verify(typeOfSampleService, never()).insert(any(TypeOfSample.class));
            verify(typeOfSampleService, times(1)).update(any(TypeOfSample.class));
        }
    }

    @Test
    public void testProcessConfiguration_EmptyFile_ThrowsException() throws Exception {
        // Given
        String csv = "";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Expect
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Sample type configuration file test.csv is empty");

        // When
        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testProcessConfiguration_MissingDescriptionColumn_ThrowsException() throws Exception {
        // Given
        String csv = "localAbbreviation,domain\n" + "WB,H\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Expect
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Sample type configuration file test.csv must have a 'description' column");

        // When
        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testProcessConfiguration_MissingLocalAbbreviationColumn_ThrowsException() throws Exception {
        // Given
        String csv = "description,domain\n" + "Whole Blood,H\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Expect
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Sample type configuration file test.csv must have a 'localAbbreviation' column");

        // When
        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testProcessConfiguration_MinimalColumns() throws Exception {
        // Given - only required columns
        String csv = "description,localAbbreviation\n" + "Whole Blood,WB\n" + "Serum,SER\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock services
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(Collections.emptyList());
        when(typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain(anyString(), anyString())).thenReturn(null);
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(any(TypeOfSample.class), anyBoolean()))
                .thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1", "2");
        when(typeOfSampleService.insert(any(TypeOfSample.class))).thenReturn("1", "2");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then
            verify(typeOfSampleService, times(2)).insert(any(TypeOfSample.class));
        }
    }

    @Test
    public void testProcessConfiguration_DefaultDomain() throws Exception {
        // Given - no domain column, should default to "H"
        String csv = "description,localAbbreviation\n" + "Whole Blood,WB\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock services
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(Collections.emptyList());
        when(typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain("WB", "H")).thenReturn(null);
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(any(TypeOfSample.class), anyBoolean()))
                .thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1");
        when(typeOfSampleService.insert(any(TypeOfSample.class))).thenReturn("1");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then - should check with default domain "H"
            verify(typeOfSampleService).getTypeOfSampleByLocalAbbrevAndDomain("WB", "H");
        }
    }

    @Test
    public void testProcessConfiguration_QuotedFields() throws Exception {
        // Given
        String csv = "description,localAbbreviation,englishName,frenchName\n"
                + "\"Sample, With Comma\",SWC,\"Sample, With Comma\",\"Échantillon, Avec Virgule\"\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock services
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(Collections.emptyList());
        when(typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain("SWC", "H")).thenReturn(null);
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(any(TypeOfSample.class), anyBoolean()))
                .thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1");
        when(typeOfSampleService.insert(any(TypeOfSample.class))).thenReturn("1");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then
            verify(typeOfSampleService, times(1)).insert(any(TypeOfSample.class));
        }
    }

    @Test
    public void testProcessConfiguration_EmptyLinesIgnored() throws Exception {
        // Given
        String csv = "description,localAbbreviation\n" + "Whole Blood,WB\n" + "\n" + "Serum,SER\n" + "\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock services
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(Collections.emptyList());
        when(typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain(anyString(), anyString())).thenReturn(null);
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(any(TypeOfSample.class), anyBoolean()))
                .thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1", "2");
        when(typeOfSampleService.insert(any(TypeOfSample.class))).thenReturn("1", "2");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then - should only process 2 entries despite empty lines
            verify(typeOfSampleService, times(2)).insert(any(TypeOfSample.class));
        }
    }

    @Test
    public void testProcessConfiguration_CaseInsensitiveHeaders() throws Exception {
        // Given
        String csv = "DESCRIPTION,LocalAbbreviation,DOMAIN\n" + "Whole Blood,WB,H\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock services
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(Collections.emptyList());
        when(typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain("WB", "H")).thenReturn(null);
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(any(TypeOfSample.class), anyBoolean()))
                .thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1");
        when(typeOfSampleService.insert(any(TypeOfSample.class))).thenReturn("1");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then
            verify(typeOfSampleService, times(1)).insert(any(TypeOfSample.class));
        }
    }

    @Test
    public void testProcessConfiguration_BooleanValues_YN() throws Exception {
        // Given
        String csv = "description,localAbbreviation,isActive\n" + "Whole Blood,WB,Y\n" + "Serum,SER,N\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock services
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(Collections.emptyList());
        when(typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain(anyString(), anyString())).thenReturn(null);
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(any(TypeOfSample.class), anyBoolean()))
                .thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1", "2");
        when(typeOfSampleService.insert(any(TypeOfSample.class))).thenReturn("1", "2");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then
            verify(typeOfSampleService, times(2)).insert(any(TypeOfSample.class));
        }
    }

    @Test
    public void testProcessConfiguration_BooleanValues_TrueFalse() throws Exception {
        // Given
        String csv = "description,localAbbreviation,isActive\n" + "Whole Blood,WB,true\n" + "Serum,SER,false\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock services
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(Collections.emptyList());
        when(typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain(anyString(), anyString())).thenReturn(null);
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(any(TypeOfSample.class), anyBoolean()))
                .thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1", "2");
        when(typeOfSampleService.insert(any(TypeOfSample.class))).thenReturn("1", "2");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then
            verify(typeOfSampleService, times(2)).insert(any(TypeOfSample.class));
        }
    }

    @Test
    public void testProcessConfiguration_SortOrderAutoAssigned() throws Exception {
        // Given - no sortOrder column
        String csv = "description,localAbbreviation\n" + "Whole Blood,WB\n" + "Serum,SER\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Existing sample types with sort orders
        TypeOfSample existing1 = new TypeOfSample();
        existing1.setSortOrder(5);
        TypeOfSample existing2 = new TypeOfSample();
        existing2.setSortOrder(10);
        List<TypeOfSample> existingList = new ArrayList<>();
        existingList.add(existing1);
        existingList.add(existing2);

        // Mock services
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(existingList);
        when(typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain(anyString(), anyString())).thenReturn(null);
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(any(TypeOfSample.class), anyBoolean()))
                .thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1", "2");
        when(typeOfSampleService.insert(any(TypeOfSample.class))).thenReturn("1", "2");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then - sort orders should start at 11 (max existing + 1)
            verify(typeOfSampleService, times(2)).insert(any(TypeOfSample.class));
        }
    }

    @Test
    public void testProcessConfiguration_AllFields() throws Exception {
        // Given
        String csv = "description,localAbbreviation,domain,isActive,sortOrder,englishName,frenchName\n"
                + "Whole Blood,WB,H,Y,1,Whole Blood,Sang Total\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock services
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(Collections.emptyList());
        when(typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain("WB", "H")).thenReturn(null);
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(any(TypeOfSample.class), anyBoolean()))
                .thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1");
        when(typeOfSampleService.insert(any(TypeOfSample.class))).thenReturn("1");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then
            verify(localizationService, times(1)).insert(any(Localization.class));
            verify(typeOfSampleService, times(1)).insert(any(TypeOfSample.class));
        }
    }

    @Test
    public void testProcessConfiguration_MissingDescriptionValue() throws Exception {
        // Given - row with empty description
        String csv = "description,localAbbreviation\n" + ",WB\n" + "Serum,SER\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock services
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(Collections.emptyList());
        when(typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain("SER", "H")).thenReturn(null);
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(any(TypeOfSample.class), anyBoolean()))
                .thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1");
        when(typeOfSampleService.insert(any(TypeOfSample.class))).thenReturn("1");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then - should only process 1 entry (skip row with missing description)
            verify(typeOfSampleService, times(1)).insert(any(TypeOfSample.class));
        }
    }

    @Test
    public void testProcessConfiguration_MissingLocalAbbreviationValue() throws Exception {
        // Given - row with empty localAbbreviation
        String csv = "description,localAbbreviation\n" + "Whole Blood,\n" + "Serum,SER\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock services
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(Collections.emptyList());
        when(typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain("SER", "H")).thenReturn(null);
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(any(TypeOfSample.class), anyBoolean()))
                .thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1");
        when(typeOfSampleService.insert(any(TypeOfSample.class))).thenReturn("1");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then - should only process 1 entry (skip row with missing abbreviation)
            verify(typeOfSampleService, times(1)).insert(any(TypeOfSample.class));
        }
    }

    @Test
    public void testProcessConfiguration_DifferentDomains() throws Exception {
        // Given - same abbreviation but different domains
        String csv = "description,localAbbreviation,domain\n" + "Human Blood,HB,H\n" + "Animal Blood,HB,A\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        // Mock services
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(Collections.emptyList());
        when(typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain("HB", "H")).thenReturn(null);
        when(typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain("HB", "A")).thenReturn(null);
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(any(TypeOfSample.class), anyBoolean()))
                .thenReturn(null);
        when(localizationService.insert(any(Localization.class))).thenReturn("1", "2");
        when(typeOfSampleService.insert(any(TypeOfSample.class))).thenReturn("1", "2");

        // Execute with mocked static DisplayListService
        try (MockedStatic<DisplayListService> mockedDisplayList = Mockito.mockStatic(DisplayListService.class)) {
            mockedDisplayList.when(DisplayListService::getInstance).thenReturn(displayListService);

            // When
            handler.processConfiguration(inputStream, "test.csv");

            // Then - both should be created (different domains)
            verify(typeOfSampleService, times(2)).insert(any(TypeOfSample.class));
        }
    }
}
