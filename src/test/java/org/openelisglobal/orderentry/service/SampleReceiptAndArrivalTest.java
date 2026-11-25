package org.openelisglobal.orderentry.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.junit.Test;
import org.openelisglobal.orderentry.valueholder.ArrivalCondition;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * OGC-1424 (FR-B23, FR-C9a): who received a sample and the condition it arrived
 * in, as a step save records them.
 */
public class SampleReceiptAndArrivalTest {

    private static final Timestamp NOW = Timestamp.valueOf("2026-10-06 09:30:00");

    @Test
    public void theReceiverDefaultsToTheSavingUserForAReceivedSample() {
        SampleItem item = new SampleItem();
        item.setReceivedDate(NOW);

        SampleReceiptAndArrival.apply(item, "", null, null, "7", NOW);

        assertEquals("7", item.getReceivedById());
    }

    @Test
    public void aChosenReceiverIsKeptAndANotYetReceivedSampleGetsNone() {
        SampleItem chosen = new SampleItem();
        chosen.setReceivedDate(NOW);
        SampleReceiptAndArrival.apply(chosen, "12", null, null, "7", NOW);
        assertEquals("12", chosen.getReceivedById());

        SampleItem notReceived = new SampleItem();
        SampleReceiptAndArrival.apply(notReceived, "not-a-user", null, null, "7", NOW);
        assertNull(notReceived.getReceivedById());
    }

    @Test
    public void aKnownArrivalIsStoredByNameAndStampedWithTheSavingUser() {
        SampleItem item = new SampleItem();

        SampleReceiptAndArrival.apply(item, null, " refrigerated ", "4,46", "7", NOW);

        assertEquals("REFRIGERATED", item.getArrivalCondition());
        assertEquals(new BigDecimal("4.5"), item.getArrivalTemperature());
        assertEquals("7", item.getArrivalRecordedById());
        assertEquals(NOW, item.getArrivalRecordedAt());
    }

    @Test
    public void anUnknownConditionOrAnImplausibleTemperatureIsNotStoredOrStamped() {
        SampleItem item = new SampleItem();

        SampleReceiptAndArrival.apply(item, null, "SUNBATHED", "450", "7", NOW);

        assertNull(item.getArrivalCondition());
        assertNull(item.getArrivalTemperature());
        assertEquals("a rejected temperature is flagged so a later save keeps the stored one", true,
                item.isArrivalTemperatureRejected());
        assertNull(item.getArrivalRecordedById());
        assertNull(item.getArrivalRecordedAt());
    }

    @Test
    public void temperaturesAreReadWithinMinusOneHundredToSixtyDegrees() {
        assertEquals(new BigDecimal("-78.0"), SampleReceiptAndArrival.plausibleTemperature("-78"));
        assertEquals(new BigDecimal("60.0"), SampleReceiptAndArrival.plausibleTemperature("60"));
        assertNull(SampleReceiptAndArrival.plausibleTemperature("-100.1"));
        assertNull(SampleReceiptAndArrival.plausibleTemperature("warm"));
        assertNull(SampleReceiptAndArrival.plausibleTemperature(" "));
    }

    @Test
    public void conditionNamesAreTheShortCodedList() {
        assertEquals(ArrivalCondition.DRY_ICE, ArrivalCondition.fromValue("dry_ice"));
        assertNull(ArrivalCondition.fromValue("Dry ice"));
        assertEquals(5, ArrivalCondition.values().length);
    }
}
