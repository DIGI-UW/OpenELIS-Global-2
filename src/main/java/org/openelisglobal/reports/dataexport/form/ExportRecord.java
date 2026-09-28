package org.openelisglobal.reports.dataexport.form;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One independent source record compiled inside the data service transaction.
 * eventId is optional: a source must prove an actual grouping relationship
 * before supplying it. Matching timestamps or row positions do not establish
 * one.
 */
public record ExportRecord(String id, String groupId, String eventId, Map<String, String> attributes,
        Map<String, String> measurements) {
    public ExportRecord {
        if (id == null || id.isBlank() || groupId == null || groupId.isBlank()) {
            throw new IllegalArgumentException("reporting.record.identityRequired");
        }
        attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
        measurements = Collections.unmodifiableMap(new LinkedHashMap<>(measurements));
        eventId = eventId == null || eventId.isBlank() ? null : eventId;
    }
}
