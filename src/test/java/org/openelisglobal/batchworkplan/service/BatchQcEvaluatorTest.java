package org.openelisglobal.batchworkplan.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.batchworkplan.valueholder.BatchQcStatus;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCFrequencyType;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;

public class BatchQcEvaluatorTest {

    private static final ZoneId ZONE = ZoneId.of("Africa/Nairobi");
    private static final ZonedDateTime NOW = ZonedDateTime.of(2026, 10, 28, 14, 0, 0, 0, ZONE);

    private static QCControlLot lot(String status, QCFrequencyType type) {
        QCControlLot lot = new QCControlLot();
        lot.setId("lot-1");
        lot.setStatus(status);
        lot.setQcFrequencyType(type);
        return lot;
    }

    private static QCResult run(String status, ZonedDateTime at) {
        QCResult result = new QCResult();
        result.setId("run-" + at.toEpochSecond());
        result.setResultStatus(status);
        result.setRunDateTime(Timestamp.from(at.toInstant()));
        return result;
    }

    private static BatchQcStatus evaluate(QCControlLot lot, QCResult latestFinal, QCResult latestAccepted,
            List<QCRuleViolation> open, Timestamp lastRelease) {
        return BatchQcEvaluator.evaluate(lot, latestFinal, latestAccepted, open, lastRelease, NOW);
    }

    @Test
    public void acceptedRunTodayPassesADailyLot() {
        QCResult accepted = run("ACCEPTED", NOW.minusHours(5));
        assertEquals(BatchQcStatus.PASS,
                evaluate(lot("ACTIVE", QCFrequencyType.DAILY), accepted, accepted, List.of(), null));
    }

    @Test
    public void acceptedRunYesterdayLeavesADailyLotOverdue() {
        QCResult accepted = run("ACCEPTED", NOW.minusDays(1));
        assertEquals(BatchQcStatus.OVERDUE,
                evaluate(lot("ACTIVE", QCFrequencyType.DAILY), accepted, accepted, List.of(), null));
    }

    @Test
    public void dailyWindowIsTheCalendarDayNotTwentyFourHours() {
        // 15 hours ago but before midnight: yesterday, so overdue
        QCResult lateYesterday = run("ACCEPTED", NOW.withHour(23).minusDays(1));
        assertEquals(BatchQcStatus.OVERDUE,
                evaluate(lot("ACTIVE", QCFrequencyType.DAILY), lateYesterday, lateYesterday, List.of(), null));
        QCResult justAfterMidnight = run("ACCEPTED", NOW.withHour(0).withMinute(5));
        assertEquals(BatchQcStatus.PASS,
                evaluate(lot("ACTIVE", QCFrequencyType.DAILY), justAfterMidnight, justAfterMidnight, List.of(), null));
    }

    @Test
    public void perShiftUsesTheLotsShiftLength() {
        QCControlLot lot = lot("ACTIVE", QCFrequencyType.PER_SHIFT);
        lot.setShiftDefinitionHours(8);
        QCResult sevenHours = run("ACCEPTED", NOW.minusHours(7));
        QCResult nineHours = run("ACCEPTED", NOW.minusHours(9));
        assertEquals(BatchQcStatus.PASS, evaluate(lot, sevenHours, sevenHours, List.of(), null));
        assertEquals(BatchQcStatus.OVERDUE, evaluate(lot, nineHours, nineHours, List.of(), null));

        lot.setShiftDefinitionHours(12);
        assertEquals(BatchQcStatus.PASS, evaluate(lot, nineHours, nineHours, List.of(), null));
    }

    @Test
    public void customHoursUsesTheLotsHours() {
        QCControlLot lot = lot("ACTIVE", QCFrequencyType.CUSTOM_HOURS);
        lot.setQcFrequencyHours(4);
        QCResult threeHours = run("ACCEPTED", NOW.minusHours(3));
        QCResult fiveHours = run("ACCEPTED", NOW.minusHours(5));
        assertEquals(BatchQcStatus.PASS, evaluate(lot, threeHours, threeHours, List.of(), null));
        assertEquals(BatchQcStatus.OVERDUE, evaluate(lot, fiveHours, fiveHours, List.of(), null));
    }

    @Test
    public void perRunNeedsAPassingRunSinceTheLastRelease() {
        QCControlLot lot = lot("ACTIVE", QCFrequencyType.PER_RUN);
        QCResult accepted = run("ACCEPTED", NOW.minusHours(2));
        Timestamp releasedBefore = Timestamp.from(NOW.minusHours(3).toInstant());
        Timestamp releasedAfter = Timestamp.from(NOW.minusHours(1).toInstant());
        assertEquals(BatchQcStatus.PASS, evaluate(lot, accepted, accepted, List.of(), releasedBefore));
        assertEquals(BatchQcStatus.OVERDUE, evaluate(lot, accepted, accepted, List.of(), releasedAfter));
        assertEquals(BatchQcStatus.PASS, evaluate(lot, accepted, accepted, List.of(), null));
    }

    @Test
    public void aRunDatedInTheFutureCoversNothing() {
        QCResult future = run("ACCEPTED", NOW.plusHours(1));
        assertEquals(BatchQcStatus.OVERDUE,
                evaluate(lot("ACTIVE", QCFrequencyType.DAILY), future, future, List.of(), null));
    }

    @Test
    public void noRunAtAllIsNotRun() {
        assertEquals(BatchQcStatus.NOT_RUN,
                evaluate(lot("ACTIVE", QCFrequencyType.DAILY), null, null, List.of(), null));
    }

    @Test
    public void aRejectedLatestRunFailsEvenWithAnEarlierPassToday() {
        QCResult accepted = run("ACCEPTED", NOW.minusHours(3));
        QCResult rejected = run("REJECTED", NOW.minusHours(1));
        assertEquals(BatchQcStatus.FAILED,
                evaluate(lot("ACTIVE", QCFrequencyType.DAILY), rejected, accepted, List.of(), null));
    }

    @Test
    public void anOpenRejectionOutranksAnAcceptedLatestRun() {
        QCResult accepted = run("ACCEPTED", NOW.minusHours(1));
        assertEquals(BatchQcStatus.VIOLATION, evaluate(lot("ACTIVE", QCFrequencyType.DAILY), accepted, accepted,
                List.of(new QCRuleViolation()), null));
    }

    @Test
    public void establishmentAndRetirementOutrankEverything() {
        QCResult rejected = run("REJECTED", NOW.minusHours(1));
        List<QCRuleViolation> open = List.of(new QCRuleViolation());
        assertEquals(BatchQcStatus.ESTABLISHMENT,
                evaluate(lot("ESTABLISHMENT", QCFrequencyType.DAILY), rejected, null, open, null));
        assertEquals(BatchQcStatus.LOT_EXPIRED,
                evaluate(lot("EXPIRED", QCFrequencyType.DAILY), rejected, null, open, null));
    }

    @Test
    public void anActiveLotPastItsExpirationDateIsExpired() {
        QCControlLot lot = lot("ACTIVE", QCFrequencyType.DAILY);
        QCResult accepted = run("ACCEPTED", NOW.minusHours(1));
        lot.setExpirationDate(Timestamp.from(NOW.minusMinutes(1).toInstant()));
        assertEquals(BatchQcStatus.LOT_EXPIRED, evaluate(lot, accepted, accepted, List.of(), null));
        lot.setExpirationDate(Timestamp.from(NOW.plusDays(1).toInstant()));
        assertEquals(BatchQcStatus.PASS, evaluate(lot, accepted, accepted, List.of(), null));
    }

    @Test
    public void aLotWithQcNotRequiredPassesWithoutRuns() {
        QCControlLot lot = lot("ACTIVE", QCFrequencyType.DAILY);
        lot.setQcRequired(false);
        assertEquals(BatchQcStatus.PASS, evaluate(lot, null, null, List.of(), null));
        lot.setQcRequired(true);
        assertEquals(BatchQcStatus.NOT_RUN, evaluate(lot, null, null, List.of(), null));
    }

    @Test
    public void nextDueFollowsTheFrequency() {
        Timestamp ranAt = Timestamp.from(NOW.minusHours(2).toInstant());
        assertEquals(NOW.toLocalDate().plusDays(1).atStartOfDay(ZONE),
                BatchQcEvaluator.nextDue(lot("ACTIVE", QCFrequencyType.DAILY), ranAt, NOW));
        QCControlLot custom = lot("ACTIVE", QCFrequencyType.CUSTOM_HOURS);
        custom.setQcFrequencyHours(6);
        assertEquals(NOW.plusHours(4), BatchQcEvaluator.nextDue(custom, ranAt, NOW));
        assertNull(BatchQcEvaluator.nextDue(lot("ACTIVE", QCFrequencyType.PER_RUN), ranAt, NOW));
    }

    @Test
    public void worstPicksTheMostSevereStatus() {
        assertEquals(BatchQcStatus.VIOLATION, BatchQcStatus.worst(BatchQcStatus.OVERDUE, BatchQcStatus.VIOLATION));
        assertEquals(BatchQcStatus.LOT_EXPIRED, BatchQcStatus.worst(BatchQcStatus.LOT_EXPIRED, BatchQcStatus.PASS));
        assertTrue(BatchQcStatus.NO_CONTROL_LOT.isBlocking());
        assertFalse(BatchQcStatus.ESTABLISHMENT.isBlocking());
    }
}
