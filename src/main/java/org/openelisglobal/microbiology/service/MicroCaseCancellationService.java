package org.openelisglobal.microbiology.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCaseRequestedTestDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseRequestedTest;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.sampletyperequest.dao.SampleTypeRequestDAO;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.openelisglobal.test.service.TestSectionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reconciles removals after the entire order step has been saved in its
 * transaction.
 */
@Service
@Transactional
public class MicroCaseCancellationService {
    private final MicroCaseDAO cases;
    private final MicroCaseRequestedTestDAO ownership;
    private final MicroCaseAnalysisDAO links;
    private final MicroCaseActivityDAO activities;
    private final MicroIsolateDAO isolates;
    private final SampleTypeRequestDAO requests;
    private final PanelItemService panels;
    private final AnalysisService analyses;
    private final ResultService results;
    private final IStatusService statuses;
    private final TestSectionService units;

    public MicroCaseCancellationService(MicroCaseDAO cases, MicroCaseRequestedTestDAO ownership,
            MicroCaseAnalysisDAO links, MicroCaseActivityDAO activities, MicroIsolateDAO isolates,
            SampleTypeRequestDAO requests, PanelItemService panels, AnalysisService analyses, ResultService results,
            IStatusService statuses, TestSectionService units) {
        this.cases = cases;
        this.ownership = ownership;
        this.links = links;
        this.activities = activities;
        this.isolates = isolates;
        this.requests = requests;
        this.panels = panels;
        this.analyses = analyses;
        this.results = results;
        this.statuses = statuses;
        this.units = units;
    }

    public void lockOrder(String orderId) {
        cases.lockOrder(orderId);
    }

    public void reconcile(String orderId, List<String> confirmedCaseIds, String reason, String actor) {
        reconcile(orderId, confirmedCaseIds, reason, actor, List.of());
    }

    public void reconcile(String orderId, List<String> confirmedCaseIds, String reason, String actor,
            List<String> cancelledAnalysisIds) {
        MicroCaseServiceImpl.requireText(actor, "performedBy");
        cases.lockOrder(orderId);
        String note = reason == null || reason.isBlank() ? null : reason.trim();
        if (note != null && note.length() > 4000) {
            throw new IllegalArgumentException("Cancellation reason must not exceed 4000 characters");
        }
        Map<Integer, SampleTypeRequest> current = new HashMap<>();
        Map<Integer, Set<String>> selected = new HashMap<>();
        for (var request : requests.getRequestsBySampleId(orderId)) {
            current.put(request.getId(), request);
            Set<String> ids = ids(request.getRequestedTests());
            for (String panel : ids(request.getRequestedPanels())) {
                panels.getPanelItemsForPanel(panel).stream().filter(item -> item.getTest() != null)
                        .forEach(item -> ids.add(item.getTest().getId()));
            }
            selected.put(request.getId(), ids);
        }
        List<MicroCaseRequestedTest> removals = new ArrayList<>();
        List<MicroCase> closing = new ArrayList<>();
        List<MicroCaseCancellationRequiredException.AffectedCase> affected = new ArrayList<>();
        String cancelledStatus = statuses
                .getStatusID(org.openelisglobal.common.services.StatusService.AnalysisStatus.Canceled);
        for (MicroCase owner : cases.getByOrder(orderId)) {
            var caseAnalyses = links.getByCaseId(owner.getId()).stream().map(link -> analyses.get(link.getAnalysisId()))
                    .toList();
            boolean edited = caseAnalyses.stream().anyMatch(a -> cancelledAnalysisIds.contains(a.getId()));
            List<MicroCaseRequestedTest> active = ownership.getByCaseId(owner.getId()).stream()
                    .filter(link -> link.getCancelledAt() == null).toList();
            List<MicroCaseRequestedTest> removed = active.stream().filter(link -> {
                var request = current.get(link.getRequestId());
                return request == null || request.getStatus() == SampleTypeRequest.Status.CANCELLED
                        || (request.getStatus() == SampleTypeRequest.Status.REQUESTED
                                && !selected.get(request.getId()).contains(link.getTestId()))
                        || (request.getStatus() == SampleTypeRequest.Status.COLLECTED && request.getSampleItem() != null
                                && caseAnalyses.stream().anyMatch(
                                        a -> cancelledAnalysisIds.contains(a.getId()) && matches(a, request, link))
                                && caseAnalyses.stream().noneMatch(
                                        a -> matches(a, request, link) && !cancelledStatus.equals(a.getStatusId())));
            }).toList();
            if (removed.isEmpty() && !edited) {
                continue;
            }
            MicroCaseMutationGuard.requireMutable(owner);
            removals.addAll(removed);
            var analysisIds = caseAnalyses.stream().map(Analysis::getId).toList();
            boolean liveAnalysis = caseAnalyses.stream().anyMatch(a -> !cancelledStatus.equals(a.getStatusId()));
            if (removed.size() == active.size() && !liveAnalysis) {
                boolean hasResults = (!analysisIds.isEmpty()
                        && !results.getResultsForAnalysisIdList(analysisIds).isEmpty())
                        || !isolates.getByCaseId(owner.getId()).isEmpty() || activities.getByCaseId(owner.getId())
                                .stream().anyMatch(event -> event.getResultSourceSampleItemId() != null);
                var unit = units.get(owner.getTestSectionId());
                affected.add(new MicroCaseCancellationRequiredException.AffectedCase(owner.getId(),
                        unit == null ? owner.getTestSectionId() : unit.getTestSectionName(), hasResults));
                closing.add(owner);
            }
        }
        if (affected.stream().anyMatch(c -> confirmedCaseIds == null || !confirmedCaseIds.contains(c.caseId())
                || (c.hasResults() && note == null))) {
            throw new MicroCaseCancellationRequiredException(affected);
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        for (var link : removals) {
            link.setCancelledAt(now);
            link.setCancelledBy(actor);
            link.setCancellationReason(note);
            link.setSysUserId(actor);
            ownership.update(link);
        }
        for (var owner : closing) {
            owner.setStage("CANCELLED");
            owner.setClosedAt(now);
            owner.setClosedBy(actor);
            owner.setSysUserId(actor);
            cases.update(owner);
            var event = new MicroCaseActivity();
            event.setCaseId(owner.getId());
            event.setActivityType("CASE_CANCELLED");
            event.setOccurredAt(now);
            event.setPerformedBy(actor);
            event.setNote(note);
            event.setSysUserId(actor);
            activities.insert(event);
        }
    }

    private static boolean matches(Analysis analysis, SampleTypeRequest request, MicroCaseRequestedTest link) {
        return analysis.getSampleItem() != null && analysis.getTest() != null
                && request.getSampleItem().getId().equals(analysis.getSampleItem().getId())
                && link.getTestId().equals(analysis.getTest().getId());
    }

    private static Set<String> ids(String csv) {
        Set<String> result = new HashSet<>();
        if (csv != null) {
            for (String id : csv.split(",")) {
                if (!id.isBlank())
                    result.add(id.trim());
            }
        }
        return result;
    }
}
