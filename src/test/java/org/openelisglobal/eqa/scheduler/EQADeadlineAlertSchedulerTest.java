package org.openelisglobal.eqa.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
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
import org.openelisglobal.eqa.dao.SampleEQADAO;
import org.openelisglobal.eqa.valueholder.EQAPriority;
import org.openelisglobal.eqa.valueholder.SampleEQA;

@RunWith(MockitoJUnitRunner.class)
public class EQADeadlineAlertSchedulerTest {

    @Mock
    private AlertService alertService;

    @Mock
    private SampleEQADAO sampleEQADAO;

    @InjectMocks
    private EQADeadlineAlertScheduler scheduler;

    @Test
    public void testCheckEQADeadlines_OverdueSample_CreatesCriticalAlert() {
        SampleEQA overdue = createSampleEQA(1L, 100L, Timestamp.from(Instant.now().minus(2, ChronoUnit.HOURS)));

        when(sampleEQADAO.findByDeadlineBefore(any(Timestamp.class))).thenReturn(Collections.singletonList(overdue));

        scheduler.checkEQADeadlines();

        verify(alertService).createAlert(eq(AlertType.EQA_DEADLINE), eq("SampleEQA"), eq(1L),
                eq(AlertSeverity.CRITICAL), anyString(), anyString());
    }

    @Test
    public void testCheckEQADeadlines_Within4Hours_CreatesCriticalAlert() {
        SampleEQA approaching = createSampleEQA(2L, 200L, Timestamp.from(Instant.now().plus(3, ChronoUnit.HOURS)));

        when(sampleEQADAO.findByDeadlineBefore(any(Timestamp.class)))
                .thenReturn(Collections.singletonList(approaching));

        scheduler.checkEQADeadlines();

        verify(alertService).createAlert(eq(AlertType.EQA_DEADLINE), eq("SampleEQA"), eq(2L),
                eq(AlertSeverity.CRITICAL), anyString(), anyString());
    }

    @Test
    public void testCheckEQADeadlines_Within24Hours_CreatesWarningAlert() {
        SampleEQA approaching = createSampleEQA(3L, 300L, Timestamp.from(Instant.now().plus(12, ChronoUnit.HOURS)));

        when(sampleEQADAO.findByDeadlineBefore(any(Timestamp.class)))
                .thenReturn(Collections.singletonList(approaching));

        scheduler.checkEQADeadlines();

        verify(alertService).createAlert(eq(AlertType.EQA_DEADLINE), eq("SampleEQA"), eq(3L), eq(AlertSeverity.WARNING),
                anyString(), anyString());
    }

    @Test
    public void testCheckEQADeadlines_Within72Hours_CreatesWarningAlert() {
        SampleEQA approaching = createSampleEQA(4L, 400L, Timestamp.from(Instant.now().plus(48, ChronoUnit.HOURS)));

        when(sampleEQADAO.findByDeadlineBefore(any(Timestamp.class)))
                .thenReturn(Collections.singletonList(approaching));

        scheduler.checkEQADeadlines();

        verify(alertService).createAlert(eq(AlertType.EQA_DEADLINE), eq("SampleEQA"), eq(4L), eq(AlertSeverity.WARNING),
                anyString(), anyString());
    }

    @Test
    public void testCheckEQADeadlines_NullDeadline_SkipsSample() {
        SampleEQA noDeadline = new SampleEQA();
        noDeadline.setId(5L);
        noDeadline.setSampleId(500L);
        noDeadline.setIsEqaSample(true);
        noDeadline.setEqaDeadline(null);

        when(sampleEQADAO.findByDeadlineBefore(any(Timestamp.class))).thenReturn(Collections.singletonList(noDeadline));

        scheduler.checkEQADeadlines();

        verify(alertService, never()).createAlert(any(AlertType.class), anyString(), anyLong(),
                any(AlertSeverity.class), anyString(), anyString());
    }

    @Test
    public void testCheckEQADeadlines_NotEqaSample_SkipsSample() {
        SampleEQA nonEqa = new SampleEQA();
        nonEqa.setId(6L);
        nonEqa.setSampleId(600L);
        nonEqa.setIsEqaSample(false);
        nonEqa.setEqaDeadline(Timestamp.from(Instant.now().plus(2, ChronoUnit.HOURS)));

        when(sampleEQADAO.findByDeadlineBefore(any(Timestamp.class))).thenReturn(Collections.singletonList(nonEqa));

        scheduler.checkEQADeadlines();

        verify(alertService, never()).createAlert(any(AlertType.class), anyString(), anyLong(),
                any(AlertSeverity.class), anyString(), anyString());
    }

    @Test
    public void testCheckSampleExpirations_Within1Day_CreatesCriticalAlert() {
        SampleEQA expiring = createSampleEQA(7L, 700L, Timestamp.from(Instant.now().plus(12, ChronoUnit.HOURS)));

        when(sampleEQADAO.findByDeadlineBefore(any(Timestamp.class))).thenReturn(Collections.singletonList(expiring));

        scheduler.checkSampleExpirations();

        verify(alertService).createAlert(eq(AlertType.SAMPLE_EXPIRATION), eq("SampleEQA"), eq(7L),
                eq(AlertSeverity.CRITICAL), anyString(), anyString());
    }

    @Test
    public void testCheckSampleExpirations_Within2Days_CreatesWarningAlert() {
        SampleEQA expiring = createSampleEQA(8L, 800L, Timestamp.from(Instant.now().plus(50, ChronoUnit.HOURS)));

        when(sampleEQADAO.findByDeadlineBefore(any(Timestamp.class))).thenReturn(Collections.singletonList(expiring));

        scheduler.checkSampleExpirations();

        verify(alertService).createAlert(eq(AlertType.SAMPLE_EXPIRATION), eq("SampleEQA"), eq(8L),
                eq(AlertSeverity.WARNING), anyString(), anyString());
    }

    @Test
    public void testCheckSampleExpirations_NullDeadline_SkipsSample() {
        SampleEQA noDeadline = new SampleEQA();
        noDeadline.setId(9L);
        noDeadline.setSampleId(900L);
        noDeadline.setEqaDeadline(null);

        when(sampleEQADAO.findByDeadlineBefore(any(Timestamp.class))).thenReturn(Collections.singletonList(noDeadline));

        scheduler.checkSampleExpirations();

        verify(alertService, never()).createAlert(any(AlertType.class), anyString(), anyLong(),
                any(AlertSeverity.class), anyString(), anyString());
    }

    @Test
    public void testEscalateUnacknowledgedAlerts_CriticalOpenOver4Hours_CreatesEscalation() {
        Alert critical = new Alert();
        critical.setId(10L);
        critical.setStatus(AlertStatus.OPEN);
        critical.setSeverity(AlertSeverity.CRITICAL);
        critical.setStartTime(OffsetDateTime.now().minus(5, ChronoUnit.HOURS));
        critical.setAcknowledgedAt(null);
        critical.setMessage("Test critical alert");

        when(alertService.getAlertsByEntity("SampleEQA", null)).thenReturn(Collections.singletonList(critical));

        scheduler.escalateUnacknowledgedAlerts();

        verify(alertService).createAlert(eq(AlertType.CRITICAL_UNACKNOWLEDGED), eq("SampleEQA"), eq(10L),
                eq(AlertSeverity.CRITICAL), anyString(), anyString());
    }

    @Test
    public void testEscalateUnacknowledgedAlerts_CriticalOpenUnder4Hours_NoEscalation() {
        Alert recent = new Alert();
        recent.setId(11L);
        recent.setStatus(AlertStatus.OPEN);
        recent.setSeverity(AlertSeverity.CRITICAL);
        recent.setStartTime(OffsetDateTime.now().minus(2, ChronoUnit.HOURS));
        recent.setAcknowledgedAt(null);
        recent.setMessage("Recent critical alert");

        when(alertService.getAlertsByEntity("SampleEQA", null)).thenReturn(Collections.singletonList(recent));

        scheduler.escalateUnacknowledgedAlerts();

        verify(alertService, never()).createAlert(any(AlertType.class), anyString(), anyLong(),
                any(AlertSeverity.class), anyString(), anyString());
    }

    @Test
    public void testEscalateUnacknowledgedAlerts_AcknowledgedAlert_NoEscalation() {
        Alert acknowledged = new Alert();
        acknowledged.setId(12L);
        acknowledged.setStatus(AlertStatus.ACKNOWLEDGED);
        acknowledged.setSeverity(AlertSeverity.CRITICAL);
        acknowledged.setStartTime(OffsetDateTime.now().minus(5, ChronoUnit.HOURS));
        acknowledged.setAcknowledgedAt(OffsetDateTime.now().minus(1, ChronoUnit.HOURS));
        acknowledged.setMessage("Acknowledged critical alert");

        when(alertService.getAlertsByEntity("SampleEQA", null)).thenReturn(Collections.singletonList(acknowledged));

        scheduler.escalateUnacknowledgedAlerts();

        verify(alertService, never()).createAlert(any(AlertType.class), anyString(), anyLong(),
                any(AlertSeverity.class), anyString(), anyString());
    }

    @Test
    public void testEscalateUnacknowledgedAlerts_NullAlertList_NoException() {
        when(alertService.getAlertsByEntity("SampleEQA", null)).thenReturn(null);

        scheduler.escalateUnacknowledgedAlerts();

        verify(alertService, never()).createAlert(
                any(AlertType.class), anyString(),
                anyLong(), any(AlertSeverity.class),
                anyString(), anyString());
    }

    @Test
    public void testCheckEQADeadlines_EmptyList_NoAlerts() {
        when(sampleEQADAO.findByDeadlineBefore(any(Timestamp.class)))
                .thenReturn(Collections.emptyList());

        scheduler.checkEQADeadlines();

        verify(alertService, never()).createAlert(
                any(AlertType.class), anyString(),
                anyLong(), any(AlertSeverity.class),
                anyString(), anyString());
    }

    @Test
    public void testCheckEQADeadlines_MultipleSamples_GeneratesMultipleAlerts() {
        SampleEQA overdue = createSampleEQA(20L, 2000L, Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS)));
        SampleEQA approaching = createSampleEQA(21L, 2100L, Timestamp.from(Instant.now().plus(10, ChronoUnit.HOURS)));

        when(sampleEQADAO.findByDeadlineBefore(any(Timestamp.class))).thenReturn(Arrays.asList(overdue, approaching));

        scheduler.checkEQADeadlines();

        verify(alertService).createAlert(eq(AlertType.EQA_DEADLINE), eq("SampleEQA"), eq(20L),
                eq(AlertSeverity.CRITICAL), anyString(), anyString());
        verify(alertService).createAlert(eq(AlertType.EQA_DEADLINE), eq("SampleEQA"), eq(21L),
                eq(AlertSeverity.WARNING), anyString(), anyString());
    }

    private SampleEQA createSampleEQA(Long id, Long sampleId, Timestamp deadline) {
        SampleEQA sample = new SampleEQA();
        sample.setId(id);
        sample.setSampleId(sampleId);
        sample.setIsEqaSample(true);
        sample.setEqaDeadline(deadline);
        sample.setEqaPriority(EQAPriority.STANDARD);
        return sample;
    }
}
