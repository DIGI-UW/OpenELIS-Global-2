package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class QcStatusServiceTest {

    @Mock
    private QcStatusDataProvider dataProvider;

    private QcStatusServiceImpl service;

    @Before
    public void setUp() {
        service = new QcStatusServiceImpl(dataProvider);
        service.setClock(Clock.fixed(Instant.parse("2026-05-03T09:30:00Z"), ZoneId.of("UTC")));
    }

    @Test
    public void resolveStatus_NoControlLot_ReturnsNotRun() {
        when(dataProvider.findControlLot("test-1", "inst-1")).thenReturn(Optional.empty());

        assertEquals(QcStatus.NOT_RUN, service.resolveStatus("test-1", "inst-1"));
    }

    @Test
    public void resolveStatus_NoLatestResult_ReturnsNotRun() {
        when(dataProvider.findControlLot("test-1", "inst-1"))
                .thenReturn(Optional.of(new QcControlLotSnapshot("lot-1", true, QcFrequencyType.DAILY, null, null, null)));
        when(dataProvider.findLatestResult("lot-1")).thenReturn(Optional.empty());

        assertEquals(QcStatus.NOT_RUN, service.resolveStatus("test-1", "inst-1"));
    }

    @Test
    public void resolveStatus_InactiveControlLot_ReturnsFailed() {
        when(dataProvider.findControlLot("test-1", "inst-1")).thenReturn(
                Optional.of(new QcControlLotSnapshot("lot-1", false, QcFrequencyType.DAILY, null, null, null)));
        when(dataProvider.findLatestResult("lot-1"))
                .thenReturn(Optional.of(new QcResultSnapshot(QcResultStatus.ACCEPTED, LocalDateTime.now())));

        assertEquals(QcStatus.FAILED, service.resolveStatus("test-1", "inst-1"));
    }

    @Test
    public void resolveStatus_RejectedLatestResult_ReturnsFailed() {
        when(dataProvider.findControlLot("test-1", "inst-1"))
                .thenReturn(Optional.of(new QcControlLotSnapshot("lot-1", true, QcFrequencyType.DAILY, null, null, null)));
        when(dataProvider.findLatestResult("lot-1"))
                .thenReturn(Optional.of(new QcResultSnapshot(QcResultStatus.REJECTED, LocalDateTime.now())));

        assertEquals(QcStatus.FAILED, service.resolveStatus("test-1", "inst-1"));
    }

    @Test
    public void resolveStatus_DailyAcceptedToday_ReturnsPass() {
        when(dataProvider.findControlLot("test-1", "inst-1"))
                .thenReturn(Optional.of(new QcControlLotSnapshot("lot-1", true, QcFrequencyType.DAILY, null, null, null)));
        when(dataProvider.findLatestResult("lot-1"))
                .thenReturn(Optional.of(new QcResultSnapshot(QcResultStatus.ACCEPTED, LocalDateTime.of(2026, 5, 3, 7, 0))));
        when(dataProvider.findLatestAcceptedResult("lot-1"))
                .thenReturn(Optional.of(new QcResultSnapshot(QcResultStatus.ACCEPTED, LocalDateTime.of(2026, 5, 3, 7, 0))));

        assertEquals(QcStatus.PASS, service.resolveStatus("test-1", "inst-1"));
    }

    @Test
    public void resolveStatus_CustomHoursExpired_ReturnsOverdue() {
        when(dataProvider.findControlLot("test-1", "inst-1")).thenReturn(
                Optional.of(new QcControlLotSnapshot("lot-1", true, QcFrequencyType.CUSTOM_HOURS, 4, null, null)));
        when(dataProvider.findLatestResult("lot-1"))
                .thenReturn(Optional.of(new QcResultSnapshot(QcResultStatus.ACCEPTED, LocalDateTime.of(2026, 5, 3, 4, 0))));
        when(dataProvider.findLatestAcceptedResult("lot-1")).thenReturn(
                Optional.of(new QcResultSnapshot(QcResultStatus.ACCEPTED, LocalDateTime.of(2026, 5, 3, 4, 0))));

        assertEquals(QcStatus.OVERDUE, service.resolveStatus("test-1", "inst-1"));
    }

    @Test
    public void resolveStatus_PerShiftAcceptedWithinCurrentShift_ReturnsPass() {
        when(dataProvider.findControlLot("test-1", "inst-1"))
                .thenReturn(Optional.of(
                        new QcControlLotSnapshot("lot-1", true, QcFrequencyType.PER_SHIFT, null, LocalTime.of(8, 0),
                                LocalTime.of(16, 0))));
        when(dataProvider.findLatestResult("lot-1"))
                .thenReturn(Optional.of(new QcResultSnapshot(QcResultStatus.ACCEPTED, LocalDateTime.of(2026, 5, 3, 8, 15))));
        when(dataProvider.findLatestAcceptedResult("lot-1"))
                .thenReturn(Optional.of(new QcResultSnapshot(QcResultStatus.ACCEPTED, LocalDateTime.of(2026, 5, 3, 8, 15))));

        assertEquals(QcStatus.PASS, service.resolveStatus("test-1", "inst-1"));
    }
}
