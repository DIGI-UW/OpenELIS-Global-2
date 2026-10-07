package org.openelisglobal.sample.service;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.junit.Test;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * OGC-1424: a step save of a sample that already exists carries every
 * collection and handling detail it sends. Before, the collection method, GPS
 * position and legacy temperature and origin of a saved sample were silently
 * dropped.
 */
public class SampleItemHandlingCopyTest {

    private static final Timestamp EARLIER = Timestamp.valueOf("2026-10-06 08:00:00");
    private static final Timestamp NOW = Timestamp.valueOf("2026-10-06 09:30:00");

    private static SampleItem saved() {
        SampleItem saved = new SampleItem();
        saved.setCollectionMethod("VENIPUNCTURE");
        saved.setSampleTemperature("4 C");
        saved.setSpecimenOrigin("REFERRED");
        saved.setGpsLatitude("-9.44");
        saved.setGpsLongitude("147.18");
        saved.setReceivedById("3");
        saved.setArrivalCondition("REFRIGERATED");
        saved.setArrivalTemperature(new BigDecimal("4.0"));
        saved.setArrivalRecordedById("3");
        saved.setArrivalRecordedAt(EARLIER);
        return saved;
    }

    @Test
    public void editedCollectionDetailsReachTheSavedSample() {
        SampleItem incoming = new SampleItem();
        incoming.setCollectionMethod("CAPILLARY");
        incoming.setSampleTemperature("4 C");
        incoming.setSpecimenOrigin("REFERRED");
        incoming.setGpsLatitude("-6.1");
        incoming.setGpsLongitude("145.4");
        incoming.setArrivalCondition("REFRIGERATED");
        incoming.setArrivalTemperature(new BigDecimal("4"));
        SampleItem saved = saved();

        SamplePatientEntryServiceImpl.copyHandlingDetails(incoming, saved);

        assertEquals("CAPILLARY", saved.getCollectionMethod());
        assertEquals("-6.1", saved.getGpsLatitude());
        assertEquals("145.4", saved.getGpsLongitude());
        assertEquals("4 C", saved.getSampleTemperature());
        assertEquals("REFERRED", saved.getSpecimenOrigin());
    }

    @Test
    public void anUnchangedArrivalKeepsWhoRecordedItAndWhen() {
        SampleItem incoming = new SampleItem();
        incoming.setArrivalCondition("REFRIGERATED");
        incoming.setArrivalTemperature(new BigDecimal("4"));
        incoming.setArrivalRecordedById("9");
        incoming.setArrivalRecordedAt(NOW);
        SampleItem saved = saved();

        SamplePatientEntryServiceImpl.copyHandlingDetails(incoming, saved);

        assertEquals("3", saved.getArrivalRecordedById());
        assertEquals(EARLIER, saved.getArrivalRecordedAt());
        assertEquals("a save without a receiver keeps the stored one", "3", saved.getReceivedById());
    }

    @Test
    public void aDefaultedReceiverIsNeverWrittenOnASampleReceivedEarlier() {
        SampleItem incoming = new SampleItem();
        incoming.setReceivedDate(NOW);
        incoming.setReceivedById("9");
        incoming.setReceivedByDefaulted(true);
        SampleItem saved = saved();
        saved.setReceivedById(null);
        saved.setReceivedDate(EARLIER);

        SamplePatientEntryServiceImpl.copyHandlingDetails(incoming, saved);

        assertEquals(null, saved.getReceivedById());
    }

    @Test
    public void aDefaultedReceiverIsStoredWhenTheReceiptIsRecordedInThisSave() {
        SampleItem incoming = new SampleItem();
        incoming.setReceivedDate(NOW);
        incoming.setReceivedById("9");
        incoming.setReceivedByDefaulted(true);
        SampleItem saved = saved();
        saved.setReceivedById(null);

        SamplePatientEntryServiceImpl.copyHandlingDetails(incoming, saved);

        assertEquals("9", saved.getReceivedById());
    }

    @Test
    public void aRejectedTemperatureNeverClearsTheStoredOne() {
        SampleItem incoming = new SampleItem();
        incoming.setArrivalCondition("REFRIGERATED");
        incoming.setArrivalTemperatureRejected(true);
        SampleItem saved = saved();

        SamplePatientEntryServiceImpl.copyHandlingDetails(incoming, saved);

        assertEquals(new BigDecimal("4.0"), saved.getArrivalTemperature());
        assertEquals(EARLIER, saved.getArrivalRecordedAt());
    }

    @Test
    public void aChangedArrivalIsRecordedAgainByTheSavingUser() {
        SampleItem incoming = new SampleItem();
        incoming.setReceivedById("9");
        incoming.setArrivalCondition("ROOM_TEMPERATURE");
        incoming.setArrivalRecordedById("9");
        incoming.setArrivalRecordedAt(NOW);
        SampleItem saved = saved();

        SamplePatientEntryServiceImpl.copyHandlingDetails(incoming, saved);

        assertEquals("ROOM_TEMPERATURE", saved.getArrivalCondition());
        assertEquals(null, saved.getArrivalTemperature());
        assertEquals("9", saved.getArrivalRecordedById());
        assertEquals(NOW, saved.getArrivalRecordedAt());
        assertEquals("9", saved.getReceivedById());
    }
}
