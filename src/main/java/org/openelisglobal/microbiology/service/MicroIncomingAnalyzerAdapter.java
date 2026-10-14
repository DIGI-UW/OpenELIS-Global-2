package org.openelisglobal.microbiology.service;

import java.util.*;
import org.openelisglobal.analyzerresults.service.AnalyzerResultPlacement;
import org.openelisglobal.analyzerresults.service.AnalyzerResultPlacementService;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.form.MicroCaseResultRequestForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Routes already mapped observations; protocol parsing and mapping remain in
 * the Bridge pipeline.
 */
@Service
public class MicroIncomingAnalyzerAdapter {
    private final MicroCaseDAO cases;
    private final MicroIncomingResultService incoming;
    private final AnalyzerResultPlacementService placement;
    private final AnalyzerResultsService staging;
    private final org.openelisglobal.microbiology.dao.MicroIncomingItemDAO queue;

    public MicroIncomingAnalyzerAdapter(MicroCaseDAO cases, MicroIncomingResultService incoming,
            AnalyzerResultPlacementService placement, AnalyzerResultsService staging,
            org.openelisglobal.microbiology.dao.MicroIncomingItemDAO queue) {
        this.cases = cases;
        this.incoming = incoming;
        this.placement = placement;
        this.staging = staging;
        this.queue = queue;
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void route(List<AnalyzerResults> rows, String actor) {
        routeRows(rows, actor, false);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void retainFailure(List<AnalyzerResults> rows, String actor) {
        routeRows(rows, actor, true);
    }

    private void routeRows(List<AnalyzerResults> rows, String actor, boolean failed) {
        Map<List<String>, List<AnalyzerResults>> groups = new LinkedHashMap<>();
        for (var row : rows) {
            if (row.getIsControl() || row.isReadOnly() || row.getTestId() == null || row.getSourceMessageId() == null)
                continue;
            var key = Arrays.asList(row.getSourceConnectionId(), row.getSourceMessageId(),
                    row.getInstrumentSpecimenId() == null ? row.getAccessionNumber() : row.getInstrumentSpecimenId(),
                    row.getTestId());
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }
        for (var group : groups.values()) {
            var first = group.get(0);
            String tube = first.getInstrumentSpecimenId() == null ? first.getAccessionNumber()
                    : first.getInstrumentSpecimenId();
            var match = placement.place(tube, first.getTestId(), first.getInstrumentPatientId(),
                    first.getInstrumentPatientName());
            if (match.proposedSampleItemId() == null
                    || match.patient().status() == AnalyzerResultPlacement.PatientStatus.MISMATCH
                    || match.patient().status() == AnalyzerResultPlacement.PatientStatus.NO_ORDER_PATIENT)
                continue;
            var owners = cases.getBySampleItem(match.proposedSampleItemId()).stream()
                    .filter(c -> c.getLabUnitId() != null).toList();
            if (owners.size() != 1)
                continue;
            var payload = new MicroCaseResultRequestForm();
            payload.components = new ArrayList<>();
            for (var row : group) {
                var component = new MicroCaseResultRequestForm.ResultComponent();
                component.componentId = row.getComponentId();
                component.value = row.getResult();
                payload.components.add(component);
            }
            payload.note = group.stream().map(AnalyzerResults::getInstrumentNote).filter(Objects::nonNull)
                    .filter(n -> !n.isBlank()).distinct().collect(java.util.stream.Collectors.joining("\n"));
            String flags = group.stream().map(AnalyzerResults::getInstrumentSampleFlags).filter(Objects::nonNull)
                    .filter(n -> !n.isBlank()).distinct().collect(java.util.stream.Collectors.joining(", "));
            if (!flags.isBlank()) {
                payload.note += "\n" + org.openelisglobal.internationalization.MessageUtil
                        .getMessage("note.analyzer.sampleFlags", new String[] { flags });
            }
            String sourceKey = String.join(":", String.valueOf(first.getSourceConnectionId()),
                    first.getSourceMessageId(), String.valueOf(tube), first.getTestId());
            var received = incoming.receive(owners.get(0).getId(), match.proposedSampleItemId(), first.getTestId(),
                    "ANALYZER", sourceKey,
                    group.stream().map(r -> String.valueOf(r.getRawResultValue()))
                            .collect(java.util.stream.Collectors.joining(" | ")),
                    payload, !failed && match.state() == AnalyzerResultPlacement.State.RESOLVED
                            && match.patient().status() == AnalyzerResultPlacement.PatientStatus.MATCH,
                    actor);
            if (failed && "WAITING".equals(received.state)) {
                var item = queue.withCase(received.id);
                item.setState("FAILED");
                item.setFailureReason("MICROBIOLOGY_INCOMING_REVIEW_REQUIRED");
                item.setSysUserId(actor);
                queue.update(item);
            }
            // Ownership transfers only after the incoming item and any attached result
            // persist.
            for (var row : group)
                staging.delete(row.getId(), actor);
        }
    }
}
