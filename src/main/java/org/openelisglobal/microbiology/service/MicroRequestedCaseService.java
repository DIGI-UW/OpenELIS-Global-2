package org.openelisglobal.microbiology.service;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCaseRequestedTestDAO;
import org.openelisglobal.microbiology.dao.MicroCaseSpecimenDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCaseAnalysis;
import org.openelisglobal.microbiology.valueholder.MicroCaseRequestedTest;
import org.openelisglobal.microbiology.valueholder.MicroCaseSpecimen;
import org.openelisglobal.microbiology.valueholder.MicroCaseTestRole;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opens cases from requested tests and retains their ownership at collection.
 */
@Service
@Transactional
public class MicroRequestedCaseService {
    private final MicroCaseDAO cases;
    private final MicroCaseRequestedTestDAO requests;
    private final MicroCaseSpecimenDAO specimens;
    private final MicroCaseAnalysisDAO analyses;
    private final MicroCaseActivityDAO activities;
    private final TestService tests;
    private final PanelItemService panels;
    private final AnalysisService analysisService;

    public MicroRequestedCaseService(MicroCaseDAO cases, MicroCaseRequestedTestDAO requests,
            MicroCaseSpecimenDAO specimens, MicroCaseAnalysisDAO analyses, MicroCaseActivityDAO activities,
            TestService tests, PanelItemService panels, AnalysisService analysisService) {
        this.cases = cases;
        this.requests = requests;
        this.specimens = specimens;
        this.analyses = analyses;
        this.activities = activities;
        this.tests = tests;
        this.panels = panels;
        this.analysisService = analysisService;
    }

    public void lockOrder(String orderId) {
        cases.lockOrder(orderId);
    }

    public void routeRequests(List<SampleTypeRequest> orderedRequests, String actor) {
        MicroCaseServiceImpl.requireText(actor, "performedBy");
        for (SampleTypeRequest request : orderedRequests) {
            if (request.getStatus() == SampleTypeRequest.Status.CANCELLED) {
                continue;
            }
            if (request.getId() == null || request.getSample() == null || request.getTypeOfSample() == null) {
                throw new IllegalArgumentException("Persisted specimen requests are required");
            }
            String orderId = request.getSample().getId();
            cases.lockOrder(orderId);
            Map<String, Test> selected = new LinkedHashMap<>();
            for (String id : ids(request.getRequestedTests())) {
                selected.put(id, tests.get(id));
            }
            for (String panelId : ids(request.getRequestedPanels())) {
                for (var item : panels.getPanelItemsForPanel(panelId)) {
                    if (item.getTest() != null) {
                        selected.put(item.getTest().getId(), tests.get(item.getTest().getId()));
                    }
                }
            }
            if (selected.containsValue(null)) {
                throw new IllegalArgumentException("Unknown requested test");
            }
            for (Test test : selected.values().stream().sorted(Comparator.comparing(t -> !t.isCollectedInSets()))
                    .toList()) {
                MicroCaseRequestedTest membership = requests.getByRequestAndTest(request.getId(), test.getId());
                if (membership == null && !test.isOpensMicrobiologyCase()) {
                    continue;
                }
                MicroCase owner;
                if (membership == null) {
                    if (test.getTestSection() == null || test.getTestSection().getId() == null) {
                        throw new IllegalArgumentException("A micro test requires a lab unit");
                    }
                    String role = MicroCaseTestRole.valueOf(test.getMicrobiologyCaseRole()).name();
                    if (test.isCollectedInSets()
                            && (!"CULTURE".equals(role) || request.getCultureSetNumber() == null)) {
                        throw new IllegalArgumentException("A culture bottle requires an explicit set number");
                    }
                    String unit = test.getTestSection().getId();
                    // Membership takes precedence over grouping; never join existing cases.
                    owner = cases.getByOrder(orderId).stream().filter(c -> unit.equals(c.getTestSectionId()))
                            .filter(c -> requests.getByCaseId(c.getId()).stream()
                                    .anyMatch(r -> r.getRequestId().equals(request.getId())))
                            .findFirst().orElse(null);
                    if (owner == null) {
                        var candidates = cases.getRoutingCandidates(orderId, request.getTypeOfSample().getId(), unit,
                                test.isCollectedInSets() ? test.getId() : null,
                                request.getSampleItem() == null ? null : request.getSampleItem().getId());
                        owner = candidates.isEmpty() ? null : candidates.get(0);
                    }
                    if (owner == null) {
                        owner = new MicroCase();
                        owner.setSampleId(orderId);
                        owner.setSampleTypeId(request.getTypeOfSample().getId());
                        owner.setTestSectionId(unit);
                        owner.setCreatedAt(new Timestamp(System.currentTimeMillis()));
                        owner.setCreatedBy(actor);
                        owner.setSysUserId(actor);
                        cases.insert(owner);
                        activity(owner, actor, MicroCaseActivityType.CASE_CREATED, "Case created from requested tests");
                    }
                    requireOpen(owner);
                    membership = new MicroCaseRequestedTest();
                    membership.setCaseId(owner.getId());
                    membership.setRequestId(request.getId());
                    membership.setTestId(test.getId());
                    membership.setCaseRole(role);
                    membership.setCollectedInSets(test.isCollectedInSets());
                    membership.setCreatedAt(new Timestamp(System.currentTimeMillis()));
                    membership.setCreatedBy(actor);
                    membership.setSysUserId(actor);
                    requests.insert(membership);
                } else {
                    owner = cases.get(membership.getCaseId()).orElseThrow();
                    if (!Objects.equals(orderId, owner.getSampleId())) {
                        throw new IllegalStateException("Requested test belongs to a different order");
                    }
                }
                if (request.getStatus() == SampleTypeRequest.Status.COLLECTED) {
                    attachCollected(request, membership, owner, actor);
                }
            }
        }
    }

    private void attachCollected(SampleTypeRequest request, MicroCaseRequestedTest membership, MicroCase owner,
            String actor) {
        var item = request.getSampleItem();
        if (item == null || !Objects.equals(owner.getSampleId(), item.getSample().getId())
                || !Objects.equals(request.getTypeOfSample().getId(), item.getTypeOfSampleId())) {
            throw new IllegalArgumentException("Collected specimen differs from its request");
        }
        if (membership.isCollectedInSets() && item.getCultureSetNumber() == null) {
            throw new IllegalArgumentException("A collected culture bottle requires a set number");
        }
        if (specimens.getByCaseAndSampleItem(owner.getId(), item.getId()) == null) {
            requireOpen(owner);
            var member = new MicroCaseSpecimen();
            member.setCaseId(owner.getId());
            member.setSampleItemId(item.getId());
            member.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            member.setCreatedBy(actor);
            member.setSysUserId(actor);
            specimens.insert(member);
            activity(owner, actor, MicroCaseActivityType.SPECIMEN_ADDED, "Requested sample collected");
        }
        for (var analysis : analysisService.getAnalysesBySampleItem(item)) {
            if (analysis.getTest() == null || !membership.getTestId().equals(analysis.getTest().getId())) {
                continue;
            }
            var existing = analyses.getByAnalysis(analysis.getId());
            if (existing != null) {
                if (!owner.getId().equals(existing.getCaseId())) {
                    throw new IllegalStateException("Collection cannot move an analysis to another case");
                }
                continue;
            }
            requireOpen(owner);
            var link = new MicroCaseAnalysis();
            link.setCaseId(owner.getId());
            link.setAnalysisId(analysis.getId());
            link.setCaseRole(membership.getCaseRole());
            link.setCollectedInSets(membership.isCollectedInSets());
            link.setSysUserId(actor);
            analyses.insert(link);
        }
    }

    private void activity(MicroCase owner, String actor, MicroCaseActivityType type, String note) {
        var event = new MicroCaseActivity();
        event.setCaseId(owner.getId());
        event.setActivityType(type.name());
        event.setPerformedBy(actor);
        event.setNote(note);
        event.setSysUserId(actor);
        activities.insert(event);
    }

    private static void requireOpen(MicroCase owner) {
        if (owner.getClosedAt() != null || "FINAL_RELEASED".equals(owner.getFinalReleaseState())) {
            throw new IllegalStateException("A finalized case requires an amendment before adding tests");
        }
    }

    private static List<String> ids(String csv) {
        return csv == null ? List.of()
                : java.util.Arrays.stream(csv.split(",")).map(String::trim).filter(id -> !id.isEmpty()).distinct()
                        .toList();
    }
}
