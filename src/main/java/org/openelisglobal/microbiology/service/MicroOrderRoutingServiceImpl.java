package org.openelisglobal.microbiology.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCaseRequestDAO;
import org.openelisglobal.microbiology.valueholder.*;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.program.service.ProgramSampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampletyperequest.service.SampleTypeRequestService;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MicroOrderRoutingServiceImpl implements MicroOrderRoutingService {
    private final MicroCaseDAO cases;
    private final MicroCaseRequestDAO requests;
    private final MicroCaseAnalysisDAO links;
    private final MicroCaseMembershipService membership;
    private final MicroCaseAnalysisService caseAnalyses;
    private final SampleTypeRequestService sampleRequests;
    private final SampleItemService samples;
    private final AnalysisService analyses;
    private final TestService tests;
    private final PanelItemService panels;
    private final ProgramSampleService programSamples;
    private final MicroOrderSiteService sites;
    private final org.openelisglobal.typeofsample.service.TypeOfSampleService sampleTypes;

    @org.springframework.beans.factory.annotation.Autowired
    private org.openelisglobal.common.services.IStatusService statuses;

    public MicroOrderRoutingServiceImpl(MicroCaseDAO cases, MicroCaseRequestDAO requests, MicroCaseAnalysisDAO links,
            MicroCaseMembershipService membership, MicroCaseAnalysisService caseAnalyses,
            SampleTypeRequestService sampleRequests, SampleItemService samples, AnalysisService analyses,
            TestService tests, PanelItemService panels, ProgramSampleService programSamples,
            MicroOrderSiteService sites, org.openelisglobal.typeofsample.service.TypeOfSampleService sampleTypes) {
        this.cases = cases;
        this.requests = requests;
        this.links = links;
        this.membership = membership;
        this.caseAnalyses = caseAnalyses;
        this.sampleRequests = sampleRequests;
        this.samples = samples;
        this.analyses = analyses;
        this.tests = tests;
        this.panels = panels;
        this.programSamples = programSamples;
        this.sites = sites;
        this.sampleTypes = sampleTypes;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroOrderDraftGrouping.Group> previewNewOrder(List<MicroOrderDraftGrouping.Selection> selections) {
        return MicroOrderDraftGrouping.group(selections);
    }

    @Override
    public void routeOrder(Sample order, String actor) {
        routeOrder(order, actor, null);
    }

    @Override
    public void routeOrder(Sample order, String actor, String cancelReason) {
        requireOrder(order, actor);
        cases.lockOrder(order.getId());
        // Establish set-culture ownership before other work on the same bottles.
        // Cancel requests that were removed from the order
        for (MicroCase c : cases.getByOrder(order.getId())) {
            for (org.openelisglobal.microbiology.valueholder.MicroCaseRequest req : membership
                    .getCaseRequests(c.getId())) {
                if (req.getCancelledAt() == null) {
                    org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest str = sampleRequests
                            .get(req.getSampleTypeRequestId());
                    if (str == null || str
                            .getStatus() == org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest.Status.CANCELLED
                            || !ids(str.getRequestedTests()).contains(req.getTestId())) {
                        membership.cancelRequest(req.getId(), cancelReason, actor);
                    }
                }
            }
        }

        List<RequestedTest> work = new ArrayList<>();
        for (org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest request : sampleRequests
                .getRequestsBySampleId(order.getId())) {
            if (request
                    .getStatus() == org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest.Status.CANCELLED)
                continue;
            Set<String> ids = ids(request.getRequestedTests());
            for (String panelId : ids(request.getRequestedPanels())) {
                for (var item : panels.getPanelItemsForPanel(panelId))
                    ids.add(item.getTest().getId());
            }
            for (String testId : ids) {
                Test test = tests.get(testId);
                if (test == null)
                    throw new IllegalArgumentException("Unknown requested test");
                if (test.isOpensMicrobiologyCase())
                    work.add(new RequestedTest(request, test));
            }
        }
        work.sort(Comparator.comparing(w -> !w.test.isCollectedInSets()));
        for (RequestedTest selected : work)
            routeRequest(selected.request, selected.test, actor);
        List<Analysis> collected = new ArrayList<>();
        for (SampleItem sample : samples.getSampleItemsBySampleId(order.getId())) {
            if (!sample.isRejected())
                collected.addAll(analyses.getAnalysesBySampleItem(sample));
        }
        collected.sort(Comparator.comparing(a -> !a.getTest().isCollectedInSets()));
        for (Analysis analysis : collected)
            routeAnalysis(analysis, actor);
    }

    private void routeRequest(SampleTypeRequest request, Test test, String actor) {
        MicroCaseRequest ownership = requests.getActiveByRequestAndTest(request.getId(), test.getId());
        if (ownership == null) {
            MicroCaseRoutingRule.requireCatalog(test);
            if (test.isCollectedInSets() && request.getCultureSetNumber() == null) {
                throw new IllegalArgumentException("A set number is required for a collected-in-sets test");
            }
            String key = request.getSampleItem() == null ? "request:" + request.getId()
                    : "sample:" + request.getSampleItem().getId();
            String siteId = sites.resolve(request.getSample(),
                    request.getSampleItem() == null ? request.getCollectionLocationId()
                            : request.getSampleItem().getCollectionLocationId(),
                    test);
            if (siteId != null && request.getCollectionLocationId() == null) {
                request.setCollectionLocationId(siteId);
                request.setSysUserId(actor);
                sampleRequests.update(request);
            }
            MicroCase owner = owner(request.getSample(), request.getTypeOfSample().getId(), key, test, actor, siteId);
            ownership = membership.ownRequest(owner.getId(), request.getId(), test.getId(), role(test),
                    test.isCollectedInSets(), actor);
        }
        if (request.getSampleItem() != null && ownership.getSampleItemId() == null) {
            Analysis analysis = ownership.getCaseRole() == MicroCaseRole.CASE ? null
                    : activeAnalysis(request.getSampleItem(), test.getId());
            if (ownership.getCaseRole() != MicroCaseRole.CASE && analysis == null)
                throw new IllegalStateException("Collected requested test has no analysis");
            membership.fulfillRequest(ownership.getId(), request.getSampleItem().getId(),
                    analysis == null ? null : analysis.getId(), actor);
        }
    }

    private Analysis activeAnalysis(SampleItem sample, String testId) {
        String cancelled = statuses
                .getStatusID(org.openelisglobal.common.services.StatusService.AnalysisStatus.Canceled);
        return analyses.getAnalysesBySampleItem(sample).stream().filter(
                a -> testId.equals(a.getTest().getId()) && (cancelled == null || !cancelled.equals(a.getStatusId())))
                .findFirst().orElse(null);
    }

    @Override
    public void routeAnalysis(Analysis analysis, String actor) {
        if (analysis == null || analysis.getTest() == null || analysis.getSampleItem() == null)
            return;
        String cancelled = statuses
                .getStatusID(org.openelisglobal.common.services.StatusService.AnalysisStatus.Canceled);
        if (cancelled != null && !cancelled.isBlank() && cancelled.equals(analysis.getStatusId()))
            return;
        Test test = tests.get(analysis.getTest().getId());
        if (links.getActiveByAnalysisId(analysis.getId()) != null)
            return;
        if (test == null)
            return;
        if (!test.isOpensMicrobiologyCase()) {
            if (analysis.getParentAnalysis() == null)
                return;
            var parent = links.getActiveByAnalysisId(analysis.getParentAnalysis().getId());
            if (parent == null)
                return;
            MicroCase source = cases.getForUpdate(parent.getCaseId());
            membership.addSample(source.getId(), analysis.getSampleItem().getId(), actor);
            var link = caseAnalyses.linkAnalysis(source, analysis, null);
            link.setCaseRole(MicroCaseRole.DIRECT);
            link.setCollectedInSets(false);
            link.setPlacement("INITIAL");
            links.update(link);
            return;
        }
        MicroCaseRoutingRule.requireCatalog(test);
        if (role(test) == MicroCaseRole.CASE)
            throw new IllegalArgumentException("Case-role tests have no result analysis");
        SampleItem sample = analysis.getSampleItem();
        requireOrder(sample.getSample(), actor);
        cases.lockOrder(sample.getSample().getId());
        MicroCase owner = owner(sample.getSample(), sample.getTypeOfSampleId(), "sample:" + sample.getId(), test, actor,
                sites.resolve(sample.getSample(), sample.getCollectionLocationId(), test));
        membership.addSample(owner.getId(), sample.getId(), actor);
        MicroCaseAnalysis link = caseAnalyses.linkAnalysis(owner, analysis, null);
        link.setCaseRole(role(test));
        link.setCollectedInSets(test.isCollectedInSets());
        link.setPlacement("INITIAL");
        links.update(link);
    }

    @Override
    public void routeCaseTest(SampleItem sample, Test test, String actor) {
        routeCaseTest(sample.getSample(), sample.getTypeOfSampleId(), sample, test, actor);
    }

    @Override
    public void routeCaseTest(Sample order, String sampleTypeId, SampleItem sample, Test test, String actor) {
        requireOrder(order, actor);
        if (sample != null && (!order.getId().equals(sample.getSample().getId())
                || !sampleTypeId.equals(sample.getTypeOfSampleId())))
            throw new IllegalArgumentException("Case work must use a sample of its requested type and order");
        var type = sampleTypes.get(sampleTypeId);
        if (type == null)
            throw new IllegalArgumentException("Unknown requested sample type");
        MicroCaseRoutingRule.requireCatalog(test);
        if (role(test) != MicroCaseRole.CASE)
            throw new IllegalArgumentException("A case-role test is required");
        cases.lockOrder(order.getId());
        SampleTypeRequest requested = sampleRequests.getRequestsBySampleId(order.getId()).stream()
                .filter(r -> r.getStatus() != SampleTypeRequest.Status.CANCELLED
                        && sampleTypeId.equals(r.getTypeOfSample().getId())
                        && (sample == null ? r.getSampleItem() == null
                                : r.getSampleItem() != null && sample.getId().equals(r.getSampleItem().getId())))
                .findFirst().orElse(null);
        if (requested == null) {
            requested = new SampleTypeRequest();
            requested.setSample(order);
            requested.setTypeOfSample(type);
            requested.setSampleItem(sample);
            requested.setStatus(
                    sample == null ? SampleTypeRequest.Status.REQUESTED : SampleTypeRequest.Status.COLLECTED);
            requested.setCreatedDate(new java.sql.Timestamp(System.currentTimeMillis()));
            requested.setSysUserId(actor);
            requested.setRequestedTests(test.getId());
            sampleRequests.insert(requested);
        } else {
            Set<String> selected = ids(requested.getRequestedTests());
            selected.add(test.getId());
            requested.setRequestedTests(String.join(",", selected));
            requested.setSysUserId(actor);
            sampleRequests.update(requested);
        }
        routeRequest(requested, test, actor);
    }

    private MicroCase owner(Sample order, String typeId, String sampleKey, Test test, String actor, String siteId) {
        var candidates = candidates(order.getId());
        var chosen = MicroCaseRoutingRule.choose(candidates, test.getTestSection().getId(), typeId,
                test.isCollectedInSets() ? test.getId() : null, sampleKey, siteId);
        if (chosen != null)
            return cases.get(chosen.caseId).orElseThrow();
        MicroCase created = new MicroCase();
        created.setSampleId(order.getId());
        created.setSampleTypeId(typeId);
        created.setLabUnitId(test.getTestSection().getId());
        created.setCreatedBy(actor);
        created.setSiteId(siteId);
        var orderProgram = programSamples.getProgrammeSampleBySample(Integer.valueOf(order.getId()), null);
        if (orderProgram != null && orderProgram.getProgram().isShowOnMicroCase()) {
            created.setProgramId(orderProgram.getProgram().getId());
        }
        cases.insert(created);
        return created;
    }

    public List<MicroCaseRoutingRule.Candidate> candidates(String orderId) {
        List<MicroCaseRoutingRule.Candidate> result = new ArrayList<>();
        for (MicroCase c : cases.getByOrder(orderId)) {
            if (c.getStatus() != MicroCaseStatus.ACTIVE)
                continue;
            var candidate = new MicroCaseRoutingRule.Candidate(c.getId(), c.getLabUnitId(), c.getSampleTypeId(),
                    c.getSiteId());
            for (var member : membership.getCaseSamples(c.getId())) {
                if (member.getSplitOutAt() == null)
                    candidate.samples.add("sample:" + member.getSampleItemId());
            }
            for (var request : membership.getCaseRequests(c.getId())) {
                if (request.getCancelledAt() != null)
                    continue;
                candidate.samples.add("request:" + request.getSampleTypeRequestId());
                if (request.isCollectedInSets())
                    candidate.setsTests.add(request.getTestId());
            }
            for (var link : links.getByCaseId(c.getId())) {
                if (link.getCancelledAt() == null && Boolean.TRUE.equals(link.getCollectedInSets())) {
                    Analysis analysis = analyses.get(link.getAnalysisId());
                    candidate.setsTests.add(analysis.getTest().getId());
                }
            }
            result.add(candidate);
        }
        return result;
    }

    private static Set<String> ids(String csv) {
        Set<String> result = new LinkedHashSet<>();
        if (csv != null)
            Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(result::add);
        return result;
    }

    private static MicroCaseRole role(Test test) {
        return MicroCaseRole.valueOf(test.getMicrobiologyCaseRole());
    }

    private static void requireOrder(Sample order, String actor) {
        if (order == null || order.getId() == null)
            throw new IllegalArgumentException("A persisted order is required");
        MicroCaseServiceImpl.requireText(actor, "actor");
    }

    private record RequestedTest(SampleTypeRequest request, Test test) {
    }
}
