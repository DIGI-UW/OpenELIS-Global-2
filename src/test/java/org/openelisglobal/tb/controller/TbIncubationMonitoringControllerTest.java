package org.openelisglobal.tb.controller;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.tb.controller.rest.TbIncubationMonitoringController;
import org.openelisglobal.tb.service.TbCultureReadingService;
import org.openelisglobal.tb.service.TbCultureReadingService.IncubationSummary;
import org.openelisglobal.tb.valueholder.TbCultureReading;
import org.openelisglobal.tb.valueholder.TbEnums.CultureResult;
import org.openelisglobal.tb.valueholder.TbEnums.GrowthObservation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for TbIncubationMonitoringController. Uses Mockito to mock the
 * service layer.
 */
@RunWith(MockitoJUnitRunner.class)
public class TbIncubationMonitoringControllerTest {

    @Mock
    private TbCultureReadingService tbCultureReadingService;

    @InjectMocks
    private TbIncubationMonitoringController controller;

    // ==================== getIncubatingSamples Tests ====================

    @Test
    public void testGetIncubatingSamples_ReturnsListOfSamples() {
        // Arrange
        TbCultureReading sample1 = new TbCultureReading();
        sample1.setId(1);
        sample1.setWeekNumber(3);
        sample1.setGrowthObservation(GrowthObservation.NO_GROWTH);

        TbCultureReading sample2 = new TbCultureReading();
        sample2.setId(2);
        sample2.setWeekNumber(5);
        sample2.setGrowthObservation(GrowthObservation.GROWTH_DETECTED);

        List<TbCultureReading> mockSamples = Arrays.asList(sample1, sample2);
        when(tbCultureReadingService.findIncubatingSamples()).thenReturn(mockSamples);

        // Act
        ResponseEntity<List<TbCultureReading>> response = controller.getIncubatingSamples();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(tbCultureReadingService).findIncubatingSamples();
    }

    @Test
    public void testGetIncubatingSamples_ReturnsEmptyListWhenNoSamples() {
        // Arrange
        when(tbCultureReadingService.findIncubatingSamples()).thenReturn(Arrays.asList());

        // Act
        ResponseEntity<List<TbCultureReading>> response = controller.getIncubatingSamples();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    // ==================== getSampleReadings Tests ====================

    @Test
    public void testGetSampleReadings_ReturnsReadingsForSample() {
        // Arrange
        String sampleItemId = "123";
        TbCultureReading reading1 = new TbCultureReading();
        reading1.setWeekNumber(1);
        reading1.setGrowthObservation(GrowthObservation.NO_GROWTH);

        TbCultureReading reading2 = new TbCultureReading();
        reading2.setWeekNumber(2);
        reading2.setGrowthObservation(GrowthObservation.NO_GROWTH);

        when(tbCultureReadingService.findBySampleItemId(sampleItemId)).thenReturn(Arrays.asList(reading1, reading2));

        // Act
        ResponseEntity<List<TbCultureReading>> response = controller.getSampleReadings(sampleItemId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        verify(tbCultureReadingService).findBySampleItemId(sampleItemId);
    }

    // ==================== getSamplesByResult Tests ====================

    @Test
    public void testGetSamplesByResult_ReturnsPositiveSamples() {
        // Arrange
        TbCultureReading positiveSample = new TbCultureReading();
        positiveSample.setId(1);
        positiveSample.setCultureResult(CultureResult.POSITIVE);
        positiveSample.setPositiveWeek(4);

        when(tbCultureReadingService.findByCultureResult(CultureResult.POSITIVE))
                .thenReturn(Arrays.asList(positiveSample));

        // Act
        ResponseEntity<List<TbCultureReading>> response = controller.getSamplesByResult(CultureResult.POSITIVE);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(CultureResult.POSITIVE, response.getBody().get(0).getCultureResult());
    }

    @Test
    public void testGetSamplesByResult_ReturnsNegativeSamples() {
        // Arrange
        TbCultureReading negativeSample = new TbCultureReading();
        negativeSample.setId(1);
        negativeSample.setCultureResult(CultureResult.NEGATIVE);

        when(tbCultureReadingService.findByCultureResult(CultureResult.NEGATIVE))
                .thenReturn(Arrays.asList(negativeSample));

        // Act
        ResponseEntity<List<TbCultureReading>> response = controller.getSamplesByResult(CultureResult.NEGATIVE);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(CultureResult.NEGATIVE, response.getBody().get(0).getCultureResult());
    }

    // ==================== getCulturePositiveSamples Tests ====================

    @Test
    public void testGetCulturePositiveSamples_ReturnsOnlyPositive() {
        // Arrange
        TbCultureReading positiveSample = new TbCultureReading();
        positiveSample.setId(1);
        positiveSample.setCultureResult(CultureResult.POSITIVE);

        when(tbCultureReadingService.findCulturePositiveSamples()).thenReturn(Arrays.asList(positiveSample));

        // Act
        ResponseEntity<List<TbCultureReading>> response = controller.getCulturePositiveSamples();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    // ==================== getIncubationSummary Tests ====================

    @Test
    public void testGetIncubationSummary_ReturnsSummaryStats() {
        // Arrange
        IncubationSummary mockSummary = new IncubationSummary(10, 6, 4, 3, 2);
        when(tbCultureReadingService.getIncubationSummary()).thenReturn(mockSummary);

        // Act
        ResponseEntity<IncubationSummary> response = controller.getIncubationSummary();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10, response.getBody().totalIncubating());
        assertEquals(6, response.getBody().week1to4());
        assertEquals(4, response.getBody().week5to8());
        assertEquals(3, response.getBody().positive());
        assertEquals(2, response.getBody().negative());
    }

    // ==================== getSamplesPendingReading Tests ====================

    @Test
    public void testGetSamplesPendingReading_ReturnsSampleIds() {
        // Arrange
        Integer weekNumber = 4;
        List<String> pendingSampleIds = Arrays.asList("101", "102", "103");
        when(tbCultureReadingService.findSampleItemIdsWithoutReadingForWeek(weekNumber)).thenReturn(pendingSampleIds);

        // Act
        ResponseEntity<List<String>> response = controller.getSamplesPendingReading(weekNumber);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().size());
    }

    // ==================== Additional Controller Behavior Tests
    // ====================

    @Test
    public void testGetIncubatingSamples_VerifiesServiceIsCalled() {
        // Arrange
        when(tbCultureReadingService.findIncubatingSamples()).thenReturn(Arrays.asList());

        // Act
        controller.getIncubatingSamples();

        // Assert - verify the service method was called exactly once
        verify(tbCultureReadingService, times(1)).findIncubatingSamples();
    }

    @Test
    public void testGetSampleReadings_PassesCorrectSampleId() {
        // Arrange
        String expectedSampleId = "SAMPLE-999";
        when(tbCultureReadingService.findBySampleItemId(expectedSampleId)).thenReturn(Arrays.asList());

        // Act
        controller.getSampleReadings(expectedSampleId);

        // Assert - verify the exact sample ID was passed to service
        verify(tbCultureReadingService).findBySampleItemId(expectedSampleId);
    }

    @Test
    public void testGetSamplesByResult_PassesCorrectResultType() {
        // Arrange
        when(tbCultureReadingService.findByCultureResult(CultureResult.CONTAMINATED))
                .thenReturn(Arrays.asList());

        // Act
        controller.getSamplesByResult(CultureResult.CONTAMINATED);

        // Assert - verify exact enum value was passed
        verify(tbCultureReadingService).findByCultureResult(CultureResult.CONTAMINATED);
    }

    @Test
    public void testGetSamplesPendingReading_PassesCorrectWeekNumber() {
        // Arrange
        Integer expectedWeek = 7;
        when(tbCultureReadingService.findSampleItemIdsWithoutReadingForWeek(expectedWeek)).thenReturn(Arrays.asList());

        // Act
        controller.getSamplesPendingReading(expectedWeek);

        // Assert - verify the exact week was passed
        verify(tbCultureReadingService).findSampleItemIdsWithoutReadingForWeek(expectedWeek);
    }

    @Test
    public void testGetIncubatingSamples_ReturnsActualDataFromService() {
        // Arrange - create sample with specific data
        TbCultureReading sample = new TbCultureReading();
        sample.setId(42);
        sample.setWeekNumber(6);
        sample.setGrowthObservation(GrowthObservation.CONTAMINATED);
        sample.setCultureResult(CultureResult.CONTAMINATED);

        when(tbCultureReadingService.findIncubatingSamples()).thenReturn(Arrays.asList(sample));

        // Act
        ResponseEntity<List<TbCultureReading>> response = controller.getIncubatingSamples();

        // Assert - verify the actual data is returned, not just size
        TbCultureReading returnedSample = response.getBody().get(0);
        assertEquals(Integer.valueOf(42), returnedSample.getId());
        assertEquals(Integer.valueOf(6), returnedSample.getWeekNumber());
        assertEquals(GrowthObservation.CONTAMINATED, returnedSample.getGrowthObservation());
        assertEquals(CultureResult.CONTAMINATED, returnedSample.getCultureResult());
    }

    @Test
    public void testGetIncubationSummary_ReturnsAllFieldsCorrectly() {
        // Arrange - use distinct values to ensure each field is mapped correctly
        IncubationSummary mockSummary = new IncubationSummary(100, 25, 75, 15, 10);
        when(tbCultureReadingService.getIncubationSummary()).thenReturn(mockSummary);

        // Act
        ResponseEntity<IncubationSummary> response = controller.getIncubationSummary();

        // Assert - verify each field is correctly mapped (distinct values prevent false
        // positives)
        assertEquals(100, response.getBody().totalIncubating());
        assertEquals(25, response.getBody().week1to4());
        assertEquals(75, response.getBody().week5to8());
        assertEquals(15, response.getBody().positive());
        assertEquals(10, response.getBody().negative());
    }
}
