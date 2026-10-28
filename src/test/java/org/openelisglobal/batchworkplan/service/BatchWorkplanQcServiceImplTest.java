package org.openelisglobal.batchworkplan.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.batchworkplan.dao.BatchWorkplanQcDAO;
import org.openelisglobal.batchworkplan.dao.BatchWorkplanQcReleaseDAO;
import org.openelisglobal.batchworkplan.form.BatchQcStatusResponse;
import org.openelisglobal.batchworkplan.form.ControlLotQcResponse;
import org.openelisglobal.batchworkplan.valueholder.BatchQcStatus;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.openelisglobal.systemuser.service.SystemUserService;

@RunWith(MockitoJUnitRunner.class)
public class BatchWorkplanQcServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("Africa/Nairobi");
    private static final ZonedDateTime NOW = ZonedDateTime.of(2026, 10, 28, 14, 0, 0, 0, ZONE);

    @Mock
    private BatchWorkplanQcDAO qcDAO;

    @Mock
    private BatchWorkplanQcReleaseDAO releaseDAO;

    @Mock
    private SystemUserService systemUserService;

    @InjectMocks
    private BatchWorkplanQcServiceImpl service;

    private final BatchTestRef hemoglobin = new BatchTestRef("1", "Hemoglobin", "5");
    private final BatchTestRef glucose = new BatchTestRef("2", "Glucose", "6");

    @Before
    public void setUp() {
        service.setClock(Clock.fixed(NOW.toInstant(), ZONE));
    }

    @Test
    public void aTestWithNoControlLotBlocksTheBatchAndIsNamed() {
        when(qcDAO.getControlLots("1", null)).thenReturn(List.of());

        BatchQcStatusResponse qc = service.evaluate(List.of(hemoglobin), null);

        assertEquals(BatchQcStatus.NO_CONTROL_LOT, qc.status());
        assertTrue(qc.blocking());
        assertFalse(qc.overridable());
        assertEquals(List.of("Hemoglobin"), qc.testsWithoutControlLot());
    }

    @Test
    public void everyActiveControlLevelIsCheckedAndTheWorstWins() {
        QCControlLot low = lot("low", "ACTIVE", null);
        QCControlLot high = lot("high", "ACTIVE", null);
        when(qcDAO.getControlLots("1", null)).thenReturn(List.of(low, high));
        QCResult todayLow = run("r-low", "ACCEPTED", NOW.minusHours(1));
        stubRuns("low", todayLow, todayLow, List.of());
        QCResult yesterdayHigh = run("r-high", "ACCEPTED", NOW.minusDays(1));
        stubRuns("high", yesterdayHigh, yesterdayHigh, List.of());

        BatchQcStatusResponse qc = service.evaluate(List.of(hemoglobin), null);

        assertEquals(BatchQcStatus.OVERDUE, qc.status());
        assertTrue(qc.overridable());
        assertEquals(2, qc.lots().size());
        assertEquals(BatchQcStatus.PASS, qc.lots().get(0).status());
        assertEquals(BatchQcStatus.OVERDUE, qc.lots().get(1).status());
    }

    @Test
    public void theWorstTestDecidesTheBatch() {
        QCControlLot hgbLot = lot("hgb", "ACTIVE", null);
        QCControlLot gluLot = lot("glu", "ACTIVE", null);
        when(qcDAO.getControlLots("1", null)).thenReturn(List.of(hgbLot));
        when(qcDAO.getControlLots("2", null)).thenReturn(List.of(gluLot));
        QCResult pass = run("r1", "ACCEPTED", NOW.minusHours(1));
        stubRuns("hgb", pass, pass, List.of());
        QCResult rejected = run("r2", "REJECTED", NOW.minusHours(1));
        QCRuleViolation violation = new QCRuleViolation();
        violation.setId("v1");
        violation.setRuleCode("1-3s");
        violation.setTriggeringResultId("r2");
        stubRuns("glu", rejected, null, List.of(violation));
        when(qcDAO.getViolationsForResults(List.of("r2"))).thenReturn(java.util.Map.of("r2", List.of(violation)));

        BatchQcStatusResponse qc = service.evaluate(List.of(hemoglobin, glucose), null);

        assertEquals(BatchQcStatus.VIOLATION, qc.status());
        ControlLotQcResponse glu = qc.lots().get(1);
        assertEquals(List.of("1-3s"), glu.ruleCodes());
        assertEquals(List.of("v1"), glu.openViolationIds());
        assertTrue("a bench lot in violation takes an inline QC entry", glu.canRecordInline());
        assertFalse("a passing lot does not offer QC entry", qc.lots().get(0).canRecordInline());
    }

    @Test
    public void anAnalyzerLotIsNeverOfferedForInlineEntry() {
        QCControlLot analyzerLot = lot("an", "ACTIVE", "9");
        when(qcDAO.getControlLots("1", "9")).thenReturn(List.of(analyzerLot));
        stubRuns("an", null, null, List.of());

        BatchQcStatusResponse qc = service.evaluate(List.of(hemoglobin), "9");

        assertEquals(BatchQcStatus.NOT_RUN, qc.status());
        assertFalse(qc.lots().get(0).bench());
        assertFalse(qc.lots().get(0).canRecordInline());
    }

    @Test
    public void establishmentLotsAreUsedOnlyWhenNoActiveLotExists() {
        QCControlLot establishing = lot("est", "ESTABLISHMENT", null);
        QCControlLot active = lot("act", "ACTIVE", null);
        when(qcDAO.getControlLots("1", null)).thenReturn(List.of(establishing, active));
        QCResult pass = run("r1", "ACCEPTED", NOW.minusHours(1));
        stubRuns("act", pass, pass, List.of());

        assertEquals(BatchQcStatus.PASS, service.evaluate(List.of(hemoglobin), null).status());

        when(qcDAO.getControlLots("1", null)).thenReturn(List.of(establishing));
        stubRuns("est", null, null, List.of());
        BatchQcStatusResponse qc = service.evaluate(List.of(hemoglobin), null);
        assertEquals(BatchQcStatus.ESTABLISHMENT, qc.status());
        assertFalse(qc.blocking());
        assertFalse("an establishment lot takes no inline entry", qc.lots().get(0).canRecordInline());
    }

    @Test
    public void onlyRetiredLotsBlockTheBatch() {
        QCControlLot expired = lot("old", "EXPIRED", null);
        QCControlLot pastDate = lot("past", "ACTIVE", null);
        pastDate.setExpirationDate(Timestamp.from(NOW.minusDays(1).toInstant()));
        when(qcDAO.getControlLots("1", null)).thenReturn(List.of(expired, pastDate));
        stubRuns("old", null, null, List.of());

        BatchQcStatusResponse qc = service.evaluate(List.of(hemoglobin), null);

        assertEquals(BatchQcStatus.LOT_EXPIRED, qc.status());
        assertTrue(qc.blocking());
        assertEquals("only the newest retired lot is shown", 1, qc.lots().size());
    }

    @Test
    public void aPendingLatestRunIsFlaggedWhileTheLastFinalRunDecides() {
        QCControlLot active = lot("act", "ACTIVE", null);
        when(qcDAO.getControlLots("1", null)).thenReturn(List.of(active));
        QCResult pending = run("p1", "PENDING", NOW.minusMinutes(1));
        QCResult accepted = run("a1", "ACCEPTED", NOW.minusHours(2));
        when(qcDAO.getRecentResults(eq("act"), anyInt())).thenReturn(List.of(pending, accepted));
        when(qcDAO.getLatestInStatuses(eq("act"), eq(List.of("ACCEPTED", "REJECTED")))).thenReturn(accepted);
        when(qcDAO.getLatestInStatuses(eq("act"), eq(List.of("ACCEPTED")))).thenReturn(accepted);
        when(qcDAO.getOpenRejections("act")).thenReturn(List.of());

        BatchQcStatusResponse qc = service.evaluate(List.of(hemoglobin), null);

        assertTrue(qc.evaluationPending());
        assertEquals(BatchQcStatus.PASS, qc.status());
        assertEquals("a1", qc.lots().get(0).lastRun().id());
    }

    private void stubRuns(String lotId, QCResult latestFinal, QCResult latestAccepted, List<QCRuleViolation> open) {
        when(qcDAO.getRecentResults(eq(lotId), anyInt())).thenReturn(latestFinal == null ? List.of() : List.of(latestFinal));
        when(qcDAO.getLatestInStatuses(eq(lotId), eq(List.of("ACCEPTED", "REJECTED")))).thenReturn(latestFinal);
        when(qcDAO.getLatestInStatuses(eq(lotId), eq(List.of("ACCEPTED")))).thenReturn(latestAccepted);
        when(qcDAO.getOpenRejections(lotId)).thenReturn(open);
    }

    private static QCControlLot lot(String id, String status, String instrumentId) {
        QCControlLot lot = new QCControlLot();
        lot.setId(id);
        lot.setLotNumber("LOT-" + id);
        lot.setControlLevel("NORMAL");
        lot.setStatus(status);
        lot.setInstrumentId(instrumentId);
        return lot;
    }

    private static QCResult run(String id, String status, ZonedDateTime at) {
        QCResult result = new QCResult();
        result.setId(id);
        result.setResultStatus(status);
        result.setRunDateTime(Timestamp.from(at.toInstant()));
        return result;
    }
}
