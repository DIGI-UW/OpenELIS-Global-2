package org.openelisglobal.observationhistory;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Every observation the application writes needs its
 * {@code observation_history_type} row: {@code createObservation} drops the
 * value silently when the type is unknown. The optional observation-types CSV
 * used to be the only source of several of them, and a deployment whose copy of
 * that file is empty lost, among others, the sampling site of every
 * environmental order (release-test run 2026-10-19, OGC-1517). Changeset 022
 * seeds them all, so each enum value must resolve here.
 */
public class EveryObservationTypeSeededTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ObservationHistoryService observationHistoryService;

    @Test
    public void everyObservationTypeTheCodeWritesHasARow() {
        List<String> missing = new ArrayList<>();
        for (ObservationType type : ObservationType.values()) {
            if (observationHistoryService.getObservationTypeIdForType(type) == null) {
                missing.add(type.getDatabaseName());
            }
        }
        assertEquals("observation types without a row", List.of(), missing);
    }
}
