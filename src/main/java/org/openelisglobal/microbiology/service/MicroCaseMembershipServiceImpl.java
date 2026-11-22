package org.openelisglobal.microbiology.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCaseRequestDAO;
import org.openelisglobal.microbiology.dao.MicroCaseSampleDAO;
import org.openelisglobal.microbiology.dao.MicroCaseSplitDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseAnalysis;
import org.openelisglobal.microbiology.valueholder.MicroCaseRequest;
import org.openelisglobal.microbiology.valueholder.MicroCaseRole;
import org.openelisglobal.microbiology.valueholder.MicroCaseSample;
import org.openelisglobal.microbiology.valueholder.MicroCaseSplit;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MicroCaseMembershipServiceImpl implements MicroCaseMembershipService {
    private final MicroCaseDAO caseDAO;
    private final MicroCaseSampleDAO sampleDAO;
    private final MicroCaseRequestDAO requestDAO;
    private final MicroCaseSplitDAO splitDAO;
    private final MicroCaseActivityDAO activityDAO;
    private final MicroCaseAnalysisDAO caseAnalysisDAO;
    private final MicroCaseAnalysisService caseAnalysisService;
    private final AnalysisService analysisService;
    private final ResultService resultService;

    public MicroCaseMembershipServiceImpl(MicroCaseDAO caseDAO, MicroCaseSampleDAO sampleDAO,
            MicroCaseRequestDAO requestDAO, MicroCaseSplitDAO splitDAO, MicroCaseActivityDAO activityDAO,
            MicroCaseAnalysisDAO caseAnalysisDAO, MicroCaseAnalysisService caseAnalysisService,
            AnalysisService analysisService, ResultService resultService) {
        this.caseDAO = caseDAO;
        this.sampleDAO = sampleDAO;
        this.requestDAO = requestDAO;
        this.splitDAO = splitDAO;
        this.activityDAO = activityDAO;
        this.caseAnalysisDAO = caseAnalysisDAO;
        this.caseAnalysisService = caseAnalysisService;
        this.analysisService = analysisService;
        this.resultService = resultService;
    }

    @Override
    public MicroCaseRequest ownRequest(String caseId, Integer requestId, String testId, MicroCaseRole role,
            boolean collectedInSets, String actor) {
        requireActor(actor);
        MicroCaseServiceImpl.requireText(testId, "testId");
        if (role == null || (collectedInSets && role != MicroCaseRole.CULTURE)) {
            throw new IllegalArgumentException("A case role is required; only culture tests are collected in sets");
        }
        SampleTypeRequest request = requireRequest(requestId);
        MicroCase microCase = mutableCase(caseId);
        if (!Objects.equals(microCase.getSampleId(), request.getSample().getId()) || (!collectedInSets
                && !Objects.equals(microCase.getSampleTypeId(), request.getTypeOfSample().getId()))) {
            throw new IllegalArgumentException("Requested sample does not belong to the case order and sample type");
        }
        MicroCaseRequest existing = requestDAO.getActiveByRequestAndTest(requestId, testId);
        if (existing != null) {
            if (!caseId.equals(existing.getCaseId())) {
                throw new IllegalArgumentException("Requested test already belongs to another case");
            }
            return existing;
        }
        MicroCaseRequest ownership = new MicroCaseRequest();
        ownership.setCaseId(caseId);
        ownership.setSampleTypeRequestId(requestId);
        ownership.setSampleTypeId(request.getTypeOfSample().getId());
        ownership.setTestId(testId);
        ownership.setCaseRole(role);
        ownership.setCollectedInSets(collectedInSets);
        ownership.setRequestedAt(now());
        ownership.setRequestedBy(actor);
        requestDAO.insert(ownership);
        record(caseId, "REQUEST_OWNED", actor, ownership.getId());
        return ownership;
    }

    @Override
    public MicroCaseRequest fulfillRequest(String ownershipId, String sampleItemId, String analysisId, String actor) {
        requireActor(actor);
        MicroCaseRequest ownership = requestDAO.get(ownershipId)
                .orElseThrow(() -> new IllegalArgumentException("Requested-test ownership not found"));
        // All tests of a requested sample share this lock, including across cases.
        SampleTypeRequest request = requireRequest(ownership.getSampleTypeRequestId());
        MicroCase microCase = mutableCase(ownership.getCaseId());
        requireActive(ownership);
        SampleItem sample = requireSample(sampleItemId, microCase);
        if (!ownership.getSampleTypeId().equals(sample.getTypeOfSample().getId())
                || (request.getSampleItem() != null && !sampleItemId.equals(request.getSampleItem().getId()))) {
            throw new IllegalArgumentException("Collected sample does not match the requested sample");
        }
        if (ownership.getSampleItemId() != null) {
            if (!sampleItemId.equals(ownership.getSampleItemId())
                    || !Objects.equals(analysisId, ownership.getAnalysisId())) {
                throw new IllegalArgumentException("Requested test is already attached to another sample or analysis");
            }
            return ownership;
        }
        for (MicroCaseRequest other : requestDAO.getBySampleItemId(sampleItemId)) {
            if (analysisId != null && analysisId.equals(other.getAnalysisId()) && other.getCancelledAt() != null) {
                throw new IllegalArgumentException("Reordering a cancelled requested test requires a new analysis");
            }
            if (!ownership.getSampleTypeRequestId().equals(other.getSampleTypeRequestId())) {
                throw new IllegalArgumentException("Collected sample is already attached to another request");
            }
        }
        Analysis analysis = null;
        if (ownership.getCaseRole() == MicroCaseRole.CASE) {
            if (analysisId != null) {
                throw new IllegalArgumentException("Case tests have no result analysis");
            }
        } else {
            MicroCaseServiceImpl.requireText(analysisId, "analysisId");
            analysis = analysisService.get(analysisId);
            if (analysis == null || analysis.getSampleItem() == null || analysis.getTest() == null
                    || !sampleItemId.equals(analysis.getSampleItem().getId())
                    || !ownership.getTestId().equals(analysis.getTest().getId())) {
                throw new IllegalArgumentException("Analysis does not match the requested test and collected sample");
            }
            MicroCaseAnalysis currentOwner = caseAnalysisDAO.getActiveByAnalysisId(analysisId);
            if (currentOwner != null && !microCase.getId().equals(currentOwner.getCaseId())) {
                throw new IllegalArgumentException("Analysis already belongs to another case");
            }
        }
        join(microCase, sample, actor);
        if (analysis != null) {
            MicroCaseAnalysis link = caseAnalysisService.linkAnalysis(microCase, analysis, null);
            if (link.getCaseRole() != null && link.getCaseRole() != ownership.getCaseRole()) {
                throw new IllegalArgumentException("Existing analysis role does not match the requested test");
            }
            link.setCaseRole(ownership.getCaseRole());
            if (link.getCollectedInSets() != null && link.getCollectedInSets() != ownership.isCollectedInSets()) {
                throw new IllegalArgumentException("Existing analysis set snapshot does not match the requested test");
            }
            link.setCollectedInSets(ownership.isCollectedInSets());
            if (link.getPlacement() == null) {
                link.setPlacement("INITIAL_TESTING");
            }
            caseAnalysisDAO.update(link);
        }
        ownership.setSampleItemId(sampleItemId);
        ownership.setAnalysisId(analysisId);
        requestDAO.update(ownership);
        record(microCase.getId(), "REQUEST_FULFILLED", actor, ownership.getId());
        return ownership;
    }

    @Override
    public void cancelRequest(String ownershipId, String reason, String actor) {
        requireActor(actor);
        MicroCaseRequest ownership = requestDAO.get(ownershipId)
                .orElseThrow(() -> new IllegalArgumentException("Requested-test ownership not found"));
        lockRequest(ownership.getSampleTypeRequestId());
        if (ownership.getCancelledAt() != null) {
            return;
        }
        mutableCase(ownership.getCaseId());
        if (ownership.getAnalysisId() != null) {
            Analysis analysis = analysisService.get(ownership.getAnalysisId());
            if (analysis == null) {
                throw new IllegalArgumentException("Requested analysis is missing");
            }
            if (!resultService.getResultsByAnalysis(analysis).isEmpty()) {
                MicroCaseServiceImpl.requireText(reason, "reason");
            }
            MicroCaseAnalysis link = caseAnalysisDAO.getByCaseAndAnalysis(ownership.getCaseId(),
                    ownership.getAnalysisId());
            if (link == null) {
                throw new IllegalArgumentException("Requested analysis ownership is missing");
            }
            link.setCancelledAt(now());
            link.setCancelledBy(actor);
            link.setCancellationReason(reason);
            caseAnalysisDAO.update(link);
        }
        ownership.setCancelledAt(now());
        ownership.setCancelledBy(actor);
        ownership.setCancellationReason(reason);
        requestDAO.update(ownership);
        record(ownership.getCaseId(), "REQUEST_CANCELLED", actor, reason);
    }

    @Override
    public MicroCaseSample addSample(String caseId, String sampleItemId, String actor) {
        requireActor(actor);
        MicroCase microCase = mutableCase(caseId);
        return join(microCase, requireSample(sampleItemId, microCase), actor);
    }

    private MicroCaseSample join(MicroCase microCase, SampleItem sample, String actor) {
        MicroCaseSample existing = sampleDAO.getActiveByCaseAndSample(microCase.getId(), sample.getId());
        if (existing != null) {
            return existing;
        }
        MicroCaseSample member = new MicroCaseSample();
        member.setCaseId(microCase.getId());
        member.setSampleItemId(sample.getId());
        member.setJoinedAt(now());
        member.setJoinedBy(actor);
        sampleDAO.insert(member);
        if (microCase.getSampleItemId() == null) {
            microCase.setSampleItemId(sample.getId());
            caseDAO.update(microCase);
        }
        record(microCase.getId(), "SAMPLE_JOINED", actor, sample.getId());
        return member;
    }

    private MicroCase mutableCase(String caseId) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        MicroCase microCase = caseDAO.getForUpdate(caseId);
        if (microCase == null || microCase.getSampleId() == null || microCase.getSampleTypeId() == null
                || microCase.getLabUnitId() == null) {
            throw new IllegalArgumentException("A case with explicit order, sample type and lab unit is required");
        }
        MicroCaseMutationGuard.requireMutable(microCase);
        return microCase;
    }

    private SampleTypeRequest requireRequest(Integer requestId) {
        SampleTypeRequest request = lockRequest(requestId);
        if (request.getStatus() == SampleTypeRequest.Status.CANCELLED) {
            throw new IllegalArgumentException("An active requested sample is required");
        }
        return request;
    }

    private SampleTypeRequest lockRequest(Integer requestId) {
        if (requestId == null) {
            throw new IllegalArgumentException("requestId is required");
        }
        SampleTypeRequest request = requestDAO.lockSampleTypeRequest(requestId);
        if (request == null || request.getSample() == null || request.getTypeOfSample() == null) {
            throw new IllegalArgumentException("A requested sample with its order and sample type is required");
        }
        return request;
    }

    private SampleItem requireSample(String sampleItemId, MicroCase microCase) {
        MicroCaseServiceImpl.requireText(sampleItemId, "sampleItemId");
        SampleItem sample = sampleDAO.lockSampleItem(sampleItemId);
        if (sample == null || sample.getSample() == null || sample.getTypeOfSample() == null
                || !microCase.getSampleId().equals(sample.getSample().getId())) {
            throw new IllegalArgumentException("Sample does not belong to the case order");
        }
        return sample;
    }

    private void requireActive(MicroCaseRequest ownership) {
        if (ownership.getCancelledAt() != null) {
            throw new IllegalArgumentException("Cancelled requested-test ownership cannot be fulfilled");
        }
    }

    private void requireActor(String actor) {
        MicroCaseServiceImpl.requireText(actor, "actor");
    }

    private Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }

    private void record(String caseId, String type, String actor, String note) {
        MicroCaseActivity event = new MicroCaseActivity();
        event.setCaseId(caseId);
        event.setActivityType(type);
        event.setPerformedBy(actor);
        event.setOccurredAt(now());
        event.setNote(note);
        activityDAO.insert(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseSample> getCaseSamples(String caseId) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        return sampleDAO.getByCaseId(caseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseRequest> getCaseRequests(String caseId) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        return requestDAO.getByCaseId(caseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getRelatedCaseIds(String caseId) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        Set<String> ids = new TreeSet<>(sampleDAO.getRelatedCaseIds(caseId));
        for (MicroCaseSplit split : splitDAO.getByCaseId(caseId)) {
            ids.add(caseId.equals(split.getSourceCaseId()) ? split.getResultCaseId() : split.getSourceCaseId());
        }
        ids.remove(caseId);
        return new ArrayList<>(ids);
    }
}
