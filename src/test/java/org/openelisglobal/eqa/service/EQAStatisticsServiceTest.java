package org.openelisglobal.eqa.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.eqa.dao.EQAResultDAO;
import org.openelisglobal.eqa.valueholder.EQADistribution;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.openelisglobal.eqa.valueholder.EQAResult;
import org.openelisglobal.eqa.valueholder.EQASubmissionMethod;

@RunWith(MockitoJUnitRunner.class)
public class EQAStatisticsServiceTest {

    @Mock
    private EQAResultDAO eqaResultDAO;

    @InjectMocks
    private EQAStatisticsServiceImpl statisticsService;

    @Test
    public void testCalculateMean_WithValidValues() {
        List<BigDecimal> values = Arrays.asList(new BigDecimal("10.0"), new BigDecimal("20.0"), new BigDecimal("30.0"),
                new BigDecimal("40.0"), new BigDecimal("50.0"));

        BigDecimal mean = statisticsService.calculateMean(values);

        assertEquals("Mean of 10,20,30,40,50 should be 30", 0, new BigDecimal("30.00000").compareTo(mean));
    }

    @Test
    public void testCalculateMean_WithEmptyList_ReturnsZero() {
        BigDecimal mean = statisticsService.calculateMean(Collections.emptyList());
        assertEquals("Mean of empty list should be 0", 0, BigDecimal.ZERO.compareTo(mean));
    }

    @Test
    public void testCalculateStandardDeviation_WithKnownValues() {
        List<BigDecimal> values = Arrays.asList(new BigDecimal("2.0"), new BigDecimal("4.0"), new BigDecimal("4.0"),
                new BigDecimal("4.0"), new BigDecimal("5.0"), new BigDecimal("5.0"), new BigDecimal("7.0"),
                new BigDecimal("9.0"));
        BigDecimal mean = new BigDecimal("5.0");

        BigDecimal sd = statisticsService.calculateStandardDeviation(values, mean);

        assertNotNull("SD should not be null", sd);
        // Sample SD (n-1 denominator) for this dataset: sqrt(32/7) ≈ 2.13809
        assertEquals("SD should be approximately 2.13809", 0, new BigDecimal("2.13809").compareTo(sd));
    }

    @Test
    public void testCalculateStandardDeviation_WithSingleValue_ReturnsZero() {
        List<BigDecimal> values = Arrays.asList(new BigDecimal("5.0"));
        BigDecimal sd = statisticsService.calculateStandardDeviation(values, new BigDecimal("5.0"));
        assertEquals("SD of single value should be 0", 0, BigDecimal.ZERO.compareTo(sd));
    }

    @Test
    public void testCalculateZScore_WithValidInputs() {
        BigDecimal value = new BigDecimal("75.0");
        BigDecimal mean = new BigDecimal("70.0");
        BigDecimal sd = new BigDecimal("5.0");

        BigDecimal zScore = statisticsService.calculateZScore(value, mean, sd);

        assertEquals("Z-score should be 1.0", 0, new BigDecimal("1.00000").compareTo(zScore));
    }

    @Test
    public void testCalculateZScore_WithNegativeDeviation() {
        BigDecimal value = new BigDecimal("60.0");
        BigDecimal mean = new BigDecimal("70.0");
        BigDecimal sd = new BigDecimal("5.0");

        BigDecimal zScore = statisticsService.calculateZScore(value, mean, sd);

        assertEquals("Z-score should be -2.0", 0, new BigDecimal("-2.00000").compareTo(zScore));
    }

    @Test
    public void testCalculateZScore_WithZeroSD_ReturnsZero() {
        BigDecimal value = new BigDecimal("75.0");
        BigDecimal mean = new BigDecimal("70.0");
        BigDecimal sd = BigDecimal.ZERO;

        BigDecimal zScore = statisticsService.calculateZScore(value, mean, sd);

        assertEquals("Z-score with SD=0 should be 0", 0, BigDecimal.ZERO.compareTo(zScore));
    }

    @Test
    public void testClassifyPerformance_Acceptable() {
        BigDecimal zScore = new BigDecimal("1.5");
        EQAPerformanceStatus status = statisticsService.classifyPerformance(zScore);
        assertEquals("Z-score 1.5 should be ACCEPTABLE", EQAPerformanceStatus.ACCEPTABLE, status);
    }

    @Test
    public void testClassifyPerformance_Questionable() {
        BigDecimal zScore = new BigDecimal("2.5");
        EQAPerformanceStatus status = statisticsService.classifyPerformance(zScore);
        assertEquals("Z-score 2.5 should be QUESTIONABLE", EQAPerformanceStatus.QUESTIONABLE, status);
    }

    @Test
    public void testClassifyPerformance_Unacceptable() {
        BigDecimal zScore = new BigDecimal("3.5");
        EQAPerformanceStatus status = statisticsService.classifyPerformance(zScore);
        assertEquals("Z-score 3.5 should be UNACCEPTABLE", EQAPerformanceStatus.UNACCEPTABLE, status);
    }

    @Test
    public void testClassifyPerformance_NegativeZScore_Questionable() {
        BigDecimal zScore = new BigDecimal("-2.5");
        EQAPerformanceStatus status = statisticsService.classifyPerformance(zScore);
        assertEquals("Z-score -2.5 should be QUESTIONABLE", EQAPerformanceStatus.QUESTIONABLE, status);
    }

    @Test
    public void testClassifyPerformance_ExactBoundary_Questionable() {
        BigDecimal zScore = new BigDecimal("2.0");
        EQAPerformanceStatus status = statisticsService.classifyPerformance(zScore);
        assertEquals("Z-score exactly 2.0 should be QUESTIONABLE", EQAPerformanceStatus.QUESTIONABLE, status);
    }

    @Test
    public void testClassifyPerformance_ExactBoundary_Unacceptable() {
        BigDecimal zScore = new BigDecimal("3.0");
        EQAPerformanceStatus status = statisticsService.classifyPerformance(zScore);
        assertEquals("Z-score exactly 3.0 should be UNACCEPTABLE", EQAPerformanceStatus.UNACCEPTABLE, status);
    }

    @Test
    public void testClassifyPerformance_NullZScore_ReturnsNull() {
        EQAPerformanceStatus status = statisticsService.classifyPerformance(null);
        assertNull("Null Z-score should return null status", status);
    }

    @Test
    public void testCalculateAndUpdateStatistics_WithEnoughParticipants() {
        EQADistribution distribution = new EQADistribution();
        distribution.setId(1L);

        List<EQAResult> results = Arrays.asList(createResult(1L, new BigDecimal("10.0")),
                createResult(2L, new BigDecimal("12.0")), createResult(3L, new BigDecimal("11.0")),
                createResult(4L, new BigDecimal("13.0")), createResult(5L, new BigDecimal("14.0")));

        when(eqaResultDAO.findByDistributionId(1L)).thenReturn(results);
        when(eqaResultDAO.update(any(EQAResult.class))).thenAnswer(i -> i.getArgument(0));

        statisticsService.calculateAndUpdateStatistics(1L);

        verify(eqaResultDAO, times(5)).update(any(EQAResult.class));
        for (EQAResult result : results) {
            assertNotNull("Z-score should be calculated", result.getZScore());
            assertNotNull("Performance status should be set", result.getPerformanceStatus());
        }
    }

    @Test
    public void testCalculateAndUpdateStatistics_WithTooFewParticipants_DoesNotCalculate() {
        List<EQAResult> results = Arrays.asList(createResult(1L, new BigDecimal("10.0")),
                createResult(2L, new BigDecimal("12.0")));

        when(eqaResultDAO.findByDistributionId(1L)).thenReturn(results);

        statisticsService.calculateAndUpdateStatistics(1L);

        verify(eqaResultDAO, times(0)).update(any(EQAResult.class));
    }

    private EQAResult createResult(Long orgId, BigDecimal value) {
        EQAResult result = new EQAResult();
        result.setParticipantOrganizationId(orgId);
        result.setResultValue(value);
        result.setSubmissionMethod(EQASubmissionMethod.MANUAL);
        result.setSubmissionDate(new Timestamp(System.currentTimeMillis()));
        return result;
    }
}
