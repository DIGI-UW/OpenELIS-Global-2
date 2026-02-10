package org.openelisglobal.eqa.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
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
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class EQAAlertRestControllerTest {

    @Mock
    private AlertService alertService;

    @InjectMocks
    private EQAAlertRestController controller;

    @Test
    public void testGetAlertsDashboard_ReturnsAllAlerts() {
        List<Alert> alerts = Arrays.asList(
                createAlert(1L, AlertType.EQA_DEADLINE, AlertSeverity.WARNING, AlertStatus.OPEN,
                        "Deadline approaching"),
                createAlert(2L, AlertType.SAMPLE_EXPIRATION, AlertSeverity.CRITICAL, AlertStatus.OPEN,
                        "Sample expiring"));

        when(alertService.getAlertsByEntity(null, null)).thenReturn(alerts);

        ResponseEntity<Map<String, Object>> response = controller.getAlertsDashboard(null, null, null, null, 0, 25);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.get("totalCount"));
        assertEquals(0, body.get("page"));
        assertEquals(25, body.get("pageSize"));
        List<?> resultAlerts = (List<?>) body.get("alerts");
        assertEquals(2, resultAlerts.size());
    }

    @Test
    public void testGetAlertsDashboard_FiltersByType() {
        List<Alert> alerts = Arrays.asList(
                createAlert(1L, AlertType.EQA_DEADLINE, AlertSeverity.WARNING, AlertStatus.OPEN, "Deadline"),
                createAlert(2L, AlertType.SAMPLE_EXPIRATION, AlertSeverity.CRITICAL, AlertStatus.OPEN, "Expiration"));

        when(alertService.getAlertsByEntity(null, null)).thenReturn(alerts);

        ResponseEntity<Map<String, Object>> response = controller.getAlertsDashboard("EQA_DEADLINE", null, null, null,
                0, 25);

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.get("totalCount"));
        List<?> resultAlerts = (List<?>) body.get("alerts");
        assertEquals(1, resultAlerts.size());
    }

    @Test
    public void testGetAlertsDashboard_FiltersBySeverity() {
        List<Alert> alerts = Arrays.asList(
                createAlert(1L, AlertType.EQA_DEADLINE, AlertSeverity.WARNING, AlertStatus.OPEN, "Warning"),
                createAlert(2L, AlertType.EQA_DEADLINE, AlertSeverity.CRITICAL, AlertStatus.OPEN, "Critical"));

        when(alertService.getAlertsByEntity(null, null)).thenReturn(alerts);

        ResponseEntity<Map<String, Object>> response = controller.getAlertsDashboard(null, "CRITICAL", null, null, 0,
                25);

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.get("totalCount"));
    }

    @Test
    public void testGetAlertsDashboard_FiltersByStatus() {
        List<Alert> alerts = Arrays.asList(
                createAlert(1L, AlertType.EQA_DEADLINE, AlertSeverity.WARNING, AlertStatus.OPEN, "Open"),
                createAlert(2L, AlertType.EQA_DEADLINE, AlertSeverity.WARNING, AlertStatus.RESOLVED, "Resolved"));

        when(alertService.getAlertsByEntity(null, null)).thenReturn(alerts);

        ResponseEntity<Map<String, Object>> response = controller.getAlertsDashboard(null, null, "OPEN", null, 0, 25);

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.get("totalCount"));
    }

    @Test
    public void testGetAlertsDashboard_FiltersBySearchText() {
        List<Alert> alerts = Arrays.asList(
                createAlert(1L, AlertType.EQA_DEADLINE, AlertSeverity.WARNING, AlertStatus.OPEN,
                        "Deadline approaching for sample ABC"),
                createAlert(2L, AlertType.SAMPLE_EXPIRATION, AlertSeverity.CRITICAL, AlertStatus.OPEN,
                        "Sample XYZ expiring"));

        when(alertService.getAlertsByEntity(null, null)).thenReturn(alerts);

        ResponseEntity<Map<String, Object>> response = controller.getAlertsDashboard(null, null, null, "ABC", 0, 25);

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.get("totalCount"));
    }

    @Test
    public void testGetAlertsDashboard_Pagination() {
        List<Alert> alerts = Arrays.asList(
                createAlert(1L, AlertType.EQA_DEADLINE, AlertSeverity.WARNING, AlertStatus.OPEN, "Alert 1"),
                createAlert(2L, AlertType.EQA_DEADLINE, AlertSeverity.WARNING, AlertStatus.OPEN, "Alert 2"),
                createAlert(3L, AlertType.EQA_DEADLINE, AlertSeverity.WARNING, AlertStatus.OPEN, "Alert 3"));

        when(alertService.getAlertsByEntity(null, null)).thenReturn(alerts);

        ResponseEntity<Map<String, Object>> response = controller.getAlertsDashboard(null, null, null, null, 0, 2);

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(3, body.get("totalCount"));
        List<?> resultAlerts = (List<?>) body.get("alerts");
        assertEquals(2, resultAlerts.size());
    }

    @Test
    public void testGetAlertsDashboard_SecondPage() {
        List<Alert> alerts = Arrays.asList(
                createAlert(1L, AlertType.EQA_DEADLINE, AlertSeverity.WARNING, AlertStatus.OPEN, "Alert 1"),
                createAlert(2L, AlertType.EQA_DEADLINE, AlertSeverity.WARNING, AlertStatus.OPEN, "Alert 2"),
                createAlert(3L, AlertType.EQA_DEADLINE, AlertSeverity.WARNING, AlertStatus.OPEN, "Alert 3"));

        when(alertService.getAlertsByEntity(null, null)).thenReturn(alerts);

        ResponseEntity<Map<String, Object>> response = controller.getAlertsDashboard(null, null, null, null, 1, 2);

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(3, body.get("totalCount"));
        List<?> resultAlerts = (List<?>) body.get("alerts");
        assertEquals(1, resultAlerts.size());
    }

    @Test
    public void testGetAlertsDashboard_NullAlertsReturnsEmpty() {
        when(alertService.getAlertsByEntity(null, null)).thenReturn(null);

        ResponseEntity<Map<String, Object>> response = controller.getAlertsDashboard(
                null, null, null, null, 0, 25);

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(0, body.get("totalCount"));
        List<?> resultAlerts = (List<?>) body.get("alerts");
        assertTrue(resultAlerts.isEmpty());
    }

    @Test
    public void testGetAlertsSummary_ReturnsCounts() {
        List<Alert> alerts = Arrays.asList(
                createAlert(1L, AlertType.EQA_DEADLINE, AlertSeverity.CRITICAL, AlertStatus.OPEN, "EQA critical"),
                createAlert(2L, AlertType.EQA_DEADLINE, AlertSeverity.WARNING, AlertStatus.OPEN, "EQA warning"),
                createAlert(3L, AlertType.STAT_OVERDUE, AlertSeverity.WARNING, AlertStatus.OPEN, "STAT overdue"),
                createAlert(4L, AlertType.SAMPLE_EXPIRATION, AlertSeverity.CRITICAL, AlertStatus.OPEN, "Expiring"),
                createAlert(5L, AlertType.EQA_DEADLINE, AlertSeverity.WARNING, AlertStatus.RESOLVED, "Resolved"));

        when(alertService.getAlertsByEntity(null, null)).thenReturn(alerts);

        ResponseEntity<Map<String, Object>> response = controller.getAlertsSummary();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(2L, body.get("criticalAlerts"));
        assertEquals(2L, body.get("eqaDeadlines"));
        assertEquals(1L, body.get("statOverdue"));
        assertEquals(1L, body.get("sampleExpiration"));
        assertEquals(4L, body.get("totalOpen"));
    }

    @Test
    public void testGetAlertsSummary_EmptyAlerts_ReturnsZeros() {
        when(alertService.getAlertsByEntity(null, null)).thenReturn(Collections.emptyList());

        ResponseEntity<Map<String, Object>> response = controller.getAlertsSummary();

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(0L, body.get("criticalAlerts"));
        assertEquals(0L, body.get("eqaDeadlines"));
        assertEquals(0L, body.get("statOverdue"));
        assertEquals(0L, body.get("sampleExpiration"));
        assertEquals(0L, body.get("totalOpen"));
    }

    @Test
    public void testAcknowledgeAlert_ValidAlert_ReturnsOk() {
        Alert alert = createAlert(1L, AlertType.EQA_DEADLINE, AlertSeverity.WARNING, AlertStatus.OPEN, "Test");
        when(alertService.getAlertsByEntity(null, null)).thenReturn(Collections.singletonList(alert));

        Map<String, String> body = new HashMap<>();
        body.put("comment", "Acknowledged");

        ResponseEntity<?> response = controller.acknowledgeAlert(1L, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(alertService).acknowledgeAlert(eq(1L), isNull());
        verify(alertService).resolveAlert(eq(1L), isNull(), eq("Acknowledged"));
    }

    @Test
    public void testAcknowledgeAlert_NotFound_Returns404() {
        when(alertService.getAlertsByEntity(null, null)).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = controller.acknowledgeAlert(999L, null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testAcknowledgeAlert_CriticalWithoutComment_ReturnsBadRequest() {
        Alert criticalAlert = createAlert(1L, AlertType.EQA_DEADLINE, AlertSeverity.CRITICAL, AlertStatus.OPEN,
                "Critical");
        when(alertService.getAlertsByEntity(null, null)).thenReturn(Collections.singletonList(criticalAlert));

        ResponseEntity<?> response = controller.acknowledgeAlert(1L, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(alertService, never()).acknowledgeAlert(anyLong(), any());
    }

    @Test
    public void testAcknowledgeAlert_CriticalWithEmptyComment_ReturnsBadRequest() {
        Alert criticalAlert = createAlert(1L, AlertType.EQA_DEADLINE, AlertSeverity.CRITICAL, AlertStatus.OPEN,
                "Critical");
        when(alertService.getAlertsByEntity(null, null)).thenReturn(Collections.singletonList(criticalAlert));

        Map<String, String> body = new HashMap<>();
        body.put("comment", "   ");

        ResponseEntity<?> response = controller.acknowledgeAlert(1L, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(alertService, never()).acknowledgeAlert(anyLong(), any());
    }

    @Test
    public void testAcknowledgeAlert_CriticalWithComment_ReturnsOk() {
        Alert criticalAlert = createAlert(1L, AlertType.EQA_DEADLINE, AlertSeverity.CRITICAL, AlertStatus.OPEN,
                "Critical");
        when(alertService.getAlertsByEntity(null, null)).thenReturn(Collections.singletonList(criticalAlert));

        Map<String, String> body = new HashMap<>();
        body.put("comment", "Issue investigated and resolved");

        ResponseEntity<?> response = controller.acknowledgeAlert(1L, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(alertService).acknowledgeAlert(eq(1L), isNull());
        verify(alertService).resolveAlert(eq(1L), isNull(), eq("Issue investigated and resolved"));
    }

    @Test
    public void testAcknowledgeAlert_WithoutComment_AcknowledgesOnly() {
        Alert alert = createAlert(1L, AlertType.EQA_DEADLINE, AlertSeverity.WARNING, AlertStatus.OPEN, "Warning");
        when(alertService.getAlertsByEntity(null, null)).thenReturn(Collections.singletonList(alert));

        ResponseEntity<?> response = controller.acknowledgeAlert(1L, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(alertService).acknowledgeAlert(eq(1L), isNull());
        verify(alertService, never()).resolveAlert(anyLong(), any(), anyString());
    }

    @Test
    public void testAcknowledgeAlert_AlreadyAcknowledged_SkipsAcknowledge() {
        Alert alert = createAlert(1L, AlertType.EQA_DEADLINE, AlertSeverity.WARNING, AlertStatus.ACKNOWLEDGED,
                "Already ack'd");
        when(alertService.getAlertsByEntity(null, null)).thenReturn(Collections.singletonList(alert));

        Map<String, String> body = new HashMap<>();
        body.put("comment", "Resolving");

        ResponseEntity<?> response = controller.acknowledgeAlert(1L, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(alertService, never()).acknowledgeAlert(anyLong(), any());
        verify(alertService).resolveAlert(eq(1L), isNull(), eq("Resolving"));
    }

    private Alert createAlert(Long id, AlertType type, AlertSeverity severity, AlertStatus status, String message) {
        Alert alert = new Alert();
        alert.setId(id);
        alert.setAlertType(type);
        alert.setSeverity(severity);
        alert.setStatus(status);
        alert.setMessage(message);
        alert.setAlertEntityType("SampleEQA");
        alert.setAlertEntityId(id * 100);
        alert.setStartTime(OffsetDateTime.now());
        return alert;
    }
}
