package org.openelisglobal.sampletyperequest;

import static org.junit.Assert.*;

import java.sql.Date;
import java.sql.Timestamp;
import org.junit.Test;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampletyperequest.service.RequestedSpecimenDetails;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;

public class RequestedSpecimenDetailsTest {
    @Test
    public void explicitCorrectionAndClearingWinOverPreviouslyRequestedDetails() {
        var request = new SampleTypeRequest();
        request.setContainer("Aerobic");
        request.setBodySite("Left arm");
        request.setCollectionDate(Date.valueOf("2026-10-07"));
        request.setCollectionTime("07:25");
        var specimen = new SampleItem();
        specimen.setContainer("");
        specimen.setSourceOther("Right arm");
        specimen.setCollectionDate(Timestamp.valueOf("2026-10-07 08:00:00"));
        assertFalse(RequestedSpecimenDetails.apply(request, specimen));
        assertEquals("", specimen.getContainer());
        assertEquals("Right arm", specimen.getSourceOther());
        assertEquals(Timestamp.valueOf("2026-10-07 08:00:00"), specimen.getCollectionDate());
    }
}
