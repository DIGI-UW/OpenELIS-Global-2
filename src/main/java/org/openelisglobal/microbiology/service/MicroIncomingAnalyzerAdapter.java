package org.openelisglobal.microbiology.service;

import java.util.*;
import org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem;
import org.openelisglobal.analyzerresults.service.AnalyzerResultPlacement;
import org.openelisglobal.analyzerresults.service.AnalyzerResultPlacementService;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.form.MicroCaseResultRequestForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transfers explicitly accepted review rows within the acceptance transaction.
 */
@Service
public class MicroIncomingAnalyzerAdapter {
    private final MicroCaseDAO cases;
    private final MicroCaseAnalysisDAO links;
    private final MicroIncomingResultService incoming;
    private final AnalyzerResultPlacementService placement;
    private final AnalyzerResultsService staging;

    public MicroIncomingAnalyzerAdapter(MicroCaseDAO cases, MicroIncomingResultService incoming,
            AnalyzerResultPlacementService placement, AnalyzerResultsService staging, MicroCaseAnalysisDAO links) {
        this.cases = cases;
        this.links = links;
        this.incoming = incoming;
        this.placement = placement;
        this.staging = staging;
    }

    private record Reviewed(AnalyzerResults source, AnalyzerResultItem decision) {
    }

    @Transactional
    public Set<String> acceptReviewed(List<AnalyzerResultItem> decisions, String actor) {
        Map<List<String>, List<Reviewed>> groups = new LinkedHashMap<>();
        for (var decision : decisions) {
            if (!decision.getIsAccepted() || decision.getIsDeleted() || decision.getIsRejected())
                continue;
            var row = staging.get(decision.getId());
            if (row == null || row.getIsControl() || row.isReadOnly() || row.getTestId() == null
                    || row.getSourceMessageId() == null)
                continue;
            var key = Arrays.asList(row.getSourceConnectionId(), row.getSourceMessageId(),
                    decision.getInstrumentSpecimenId() == null ? decision.getAccessionNumber()
                            : decision.getInstrumentSpecimenId(),
                    row.getTestId());
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(new Reviewed(row, decision));
        }
        Set<String> consumed = new HashSet<>();
        for (var group : groups.values()) {
            var first = group.get(0).source();
            var decision = group.get(0).decision();
            String tube = decision.getInstrumentSpecimenId() == null ? decision.getAccessionNumber()
                    : decision.getInstrumentSpecimenId();
            var match = placement.place(tube, first.getTestId(), first.getInstrumentPatientId(),
                    first.getInstrumentPatientName());
            if (match.patient().status() == AnalyzerResultPlacement.PatientStatus.MISMATCH
                    && (decision.getNote() == null || decision.getNote().isBlank()))
                continue;
            var destinations = group.stream().map(r -> r.decision().getChosenAnalysisId())
                    .map(id -> id == null || id.isBlank() ? match.proposedAnalysisId() : id)
                    .collect(java.util.stream.Collectors.toSet());
            if (destinations.size() > 1)
                throw new IllegalArgumentException("MICROBIOLOGY_ANALYZER_COMPONENT_PLACEMENT_CONFLICT");
            String sampleId = match.proposedSampleItemId();
            String analysisId = match.proposedAnalysisId();
            var chosen = match.analyses().stream().filter(a -> a.analysisId().equals(decision.getChosenAnalysisId()))
                    .findFirst();
            if (chosen.isPresent()) {
                sampleId = chosen.get().sampleItemId();
                analysisId = chosen.get().analysisId();
            }
            if (sampleId == null)
                continue;
            var owners = cases.getBySampleItem(sampleId).stream().filter(c -> c.getLabUnitId() != null).toList();
            if (analysisId != null) {
                var owner = links.getActiveByAnalysisId(analysisId);
                if (owner == null)
                    continue;
                owners = owners.stream().filter(c -> c.getId().equals(owner.getCaseId())).toList();
            }
            if (owners.size() != 1)
                continue;
            var payload = new MicroCaseResultRequestForm();
            payload.components = new ArrayList<>();
            for (var reviewed : group) {
                var component = new MicroCaseResultRequestForm.ResultComponent();
                component.componentId = reviewed.source().getComponentId();
                component.value = reviewed.decision().getResult();
                component.criticalAcknowledged = reviewed.decision().isCriticalAcknowledged();
                component.invalidResultConfirmed = reviewed.decision().isInvalidResultConfirmed();
                payload.components.add(component);
            }
            payload.note = group.stream()
                    .flatMap(r -> java.util.stream.Stream.of(r.source().getInstrumentNote(), r.decision().getNote()))
                    .filter(Objects::nonNull).filter(n -> !n.isBlank()).distinct()
                    .collect(java.util.stream.Collectors.joining("\n"));
            String flags = group.stream().map(r -> r.source().getInstrumentSampleFlags()).filter(Objects::nonNull)
                    .filter(n -> !n.isBlank()).distinct().collect(java.util.stream.Collectors.joining(", "));
            if (!flags.isBlank())
                payload.note += "\n" + org.openelisglobal.internationalization.MessageUtil
                        .getMessage("note.analyzer.sampleFlags", new String[] { flags });
            String sourceKey = String.join(":", String.valueOf(first.getSourceConnectionId()),
                    first.getSourceMessageId(), String.valueOf(tube), first.getTestId());
            incoming.receiveReviewed(owners.get(0).getId(), sampleId, first.getTestId(), "ANALYZER", sourceKey,
                    group.stream().map(r -> String.valueOf(r.source().getRawResultValue()))
                            .collect(java.util.stream.Collectors.joining(" | ")),
                    payload, analysisId, actor, first.getAnalyzerId());
            for (var reviewed : group) {
                staging.delete(reviewed.source().getId(), actor);
                consumed.add(reviewed.source().getId());
            }
        }
        return consumed;
    }
}
