package org.openelisglobal.sampletyperequest.service;

import java.sql.Timestamp;
import java.time.LocalTime;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;

/**
 * Carries recorded request details forward only where collection omitted them.
 */
public final class RequestedSpecimenDetails {
    private RequestedSpecimenDetails() {
    }

    public static boolean apply(SampleTypeRequest request, SampleItem item) {
        boolean changed = false;
        if (item.getCultureSetNumber() == null && request.getCultureSetNumber() != null) {
            item.setCultureSetNumber(request.getCultureSetNumber());
            changed = true;
        }
        if (item.getContainer() == null && request.getContainer() != null) {
            item.setContainer(request.getContainer());
            changed = true;
        }
        if (item.getSourceOther() == null && request.getBodySite() != null) {
            item.setSourceOther(request.getBodySite());
            changed = true;
        }
        if (item.getCollectionDate() == null && request.getCollectionDate() != null) {
            LocalTime time = request.getCollectionTime() == null ? LocalTime.MIDNIGHT
                    : LocalTime.parse(request.getCollectionTime());
            item.setCollectionDate(Timestamp.valueOf(request.getCollectionDate().toLocalDate().atTime(time)));
            changed = true;
        }
        return changed;
    }
}
