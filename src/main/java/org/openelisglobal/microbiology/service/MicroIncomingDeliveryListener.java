package org.openelisglobal.microbiology.service;

import java.util.*;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MicroIncomingDeliveryListener {
    private final MicroIncomingAnalyzerAdapter adapter;

    public MicroIncomingDeliveryListener(MicroIncomingAnalyzerAdapter adapter) {
        this.adapter = adapter;
    }

    @TransactionalEventListener
    public void receive(MicroIncomingDelivery event) {
        Map<List<String>, List<AnalyzerResults>> groups = new LinkedHashMap<>();
        for (var row : event.rows()) {
            var key = Arrays.asList(row.getSourceConnectionId(), row.getSourceMessageId(),
                    row.getInstrumentSpecimenId(), row.getTestId(), row.getRawTestCode(), row.getRawSubIdentity());
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }
        for (var group : groups.values()) {
            try {
                adapter.route(group, event.actor());
            } catch (RuntimeException failure) {
                // Staging is already committed; a refused result remains on Analyzer Review.
                LogEvent.logError(failure);
                try {
                    adapter.retainFailure(group, event.actor());
                } catch (RuntimeException retainedInAnalyzerReview) {
                    LogEvent.logError(retainedInAnalyzerReview);
                }
            }
        }
    }
}
