package org.openelisglobal.eqa.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.eqa.service.EQAResultService;
import org.openelisglobal.eqa.service.EQAStatisticsService;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.openelisglobal.eqa.valueholder.EQAResult;
import org.openelisglobal.eqa.valueholder.EQASubmissionMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class EQAResultRestControllerTest {

    @Mock
    private EQAResultService resultService;

    @Mock
    private EQAStatisticsService statisticsService;

    @InjectMocks
    private EQAResultRestController controller;

    @Test
    public void testSubmitResult_ValidData_ReturnsOk() {
        EQAResult result = createResult(1L, 10L, 20L, new BigDecimal("5.5"));
        when(resultService.submitResult(eq(1L), eq(10L), eq(20L), any(BigDecimal.class),
                eq(EQASubmissionMethod.MANUAL))).thenReturn(result);

        Map<String, Object> body = new HashMap<>();
        body.put("organizationId", 10);
        body.put("testId", 20);
        body.put("resultValue", 5.5);

        ResponseEntity<?> response = controller.submitResult(1L, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        assertNotNull(responseBody);
        assertEquals(1L, responseBody.get("id"));
    }

    @Test
    public void testSubmitResult_MissingFields_ReturnsBadRequest() {
        Map<String, Object> body = new HashMap<>();
        body.put("organizationId", 10);

        ResponseEntity<?> response = controller.submitResult(1L, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testBatchImport_AllValid_ReturnsSuccessCount() {
        EQAResult result = createResult(1L, 10L, 20L, new BigDecimal("5.5"));
        when(resultService.submitResult(anyLong(), anyLong(), anyLong(), any(BigDecimal.class),
                eq(EQASubmissionMethod.FILE_UPLOAD))).thenReturn(result);

        List<Map<String, Object>> rows = Arrays.asList(createRow(10, 20, 5.5), createRow(11, 20, 6.0));

        ResponseEntity<?> response = controller.batchImportResults(1L, rows);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals(2, body.get("successCount"));
        assertEquals(0, body.get("errorCount"));
    }

    @Test
    public void testBatchImport_SomeInvalid_ReportsErrors() {
        EQAResult result = createResult(1L, 10L, 20L, new BigDecimal("5.5"));
        when(resultService.submitResult(anyLong(), anyLong(), anyLong(), any(BigDecimal.class),
                eq(EQASubmissionMethod.FILE_UPLOAD))).thenReturn(result);

        Map<String, Object> invalidRow = new HashMap<>();
        invalidRow.put("organizationId", 10);
        // missing testId and resultValue

        List<Map<String, Object>> rows = Arrays.asList(createRow(10, 20, 5.5), invalidRow);

        ResponseEntity<?> response = controller.batchImportResults(1L, rows);

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals(1, body.get("successCount"));
        assertEquals(1, body.get("errorCount"));
    }

    @Test
    public void testGetResults_ReturnsResultList() {
        List<EQAResult> results = Arrays.asList(createResult(1L, 10L, 20L, new BigDecimal("5.5")),
                createResult(2L, 11L, 20L, new BigDecimal("6.0")));
        when(resultService.findByDistributionId(1L)).thenReturn(results);

        ResponseEntity<Map<String, Object>> response = controller.getResults(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.get("totalCount"));
    }

    @Test
    public void testGetResults_EmptyList() {
        when(resultService.findByDistributionId(1L)).thenReturn(Collections.emptyList());

        ResponseEntity<Map<String, Object>> response = controller.getResults(1L);

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(0, body.get("totalCount"));
    }

    @Test
    public void testGetStatistics_WithEnoughParticipants() {
        List<EQAResult> results = Arrays.asList(createResult(1L, 10L, 20L, new BigDecimal("5.0")),
                createResult(2L, 11L, 20L, new BigDecimal("6.0")), createResult(3L, 12L, 20L, new BigDecimal("5.5")),
                createResult(4L, 13L, 20L, new BigDecimal("5.8")), createResult(5L, 14L, 20L, new BigDecimal("5.2")));
        when(resultService.findByDistributionId(1L)).thenReturn(results);
        when(statisticsService.calculateMean(any())).thenReturn(new BigDecimal("5.5"));
        when(statisticsService.calculateStandardDeviation(any(), any())).thenReturn(new BigDecimal("0.37417"));

        ResponseEntity<Map<String, Object>> response = controller.getStatistics(1L);

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(5, body.get("participantCount"));
        assertTrue((Boolean) body.get("hasEnoughParticipants"));
        assertNotNull(body.get("mean"));
        assertNotNull(body.get("standardDeviation"));
    }

    @Test
    public void testGetStatistics_TooFewParticipants() {
        List<EQAResult> results = Arrays.asList(createResult(1L, 10L, 20L, new BigDecimal("5.0")),
                createResult(2L, 11L, 20L, new BigDecimal("6.0")));
        when(resultService.findByDistributionId(1L)).thenReturn(results);
        when(statisticsService.calculateMean(any())).thenReturn(new BigDecimal("5.5"));
        when(statisticsService.calculateStandardDeviation(any(), any())).thenReturn(new BigDecimal("0.707"));

        ResponseEntity<Map<String, Object>> response = controller.getStatistics(1L);

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.get("participantCount"));
        assertEquals(false, body.get("hasEnoughParticipants"));
    }

    private EQAResult createResult(Long id, Long orgId, Long testId, BigDecimal value) {
        EQAResult r = new EQAResult();
        r.setId(id);
        r.setParticipantOrganizationId(orgId);
        r.setTestId(testId);
        r.setResultValue(value);
        r.setSubmissionMethod(EQASubmissionMethod.MANUAL);
        r.setSubmissionDate(new Timestamp(System.currentTimeMillis()));
        r.setIsLateSubmission(false);
        r.setPerformanceStatus(EQAPerformanceStatus.ACCEPTABLE);
        r.setZScore(BigDecimal.ZERO);
        return r;
    }

    private Map<String, Object> createRow(int orgId, int testId, double value) {
        Map<String, Object> row = new HashMap<>();
        row.put("organizationId", orgId);
        row.put("testId", testId);
        row.put("resultValue", value);
        return row;
    }
}
