package org.openelisglobal.vector.deconvolution.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionInitiateRequest;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionNode;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionOutcome;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionPreview;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionResult;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.PanelTestGroup;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.PoolResultSummary;
import org.openelisglobal.vector.service.VectorPoolService;
import org.openelisglobal.vector.valueholder.VectorPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VectorDeconvolutionServiceImpl implements VectorDeconvolutionService {

    public static final String STATUS_NOT_APPLICABLE = "NOT_APPLICABLE";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETE = "COMPLETE";

    private static final String VECTOR_DOMAIN = "V";

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private TestService testService;

    @Autowired
    private NoteService noteService;

    @Autowired
    private VectorPoolService vectorPoolService;

    @Autowired
    private org.openelisglobal.vector.service.VectorPoolLabelService poolLabelService;

    @Autowired
    private PanelService panelService;

    @Autowired
    private PanelItemService panelItemService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private DictionaryService dictionaryService;

    @Override
    @Transactional
    public DeconvolutionResult initiate(DeconvolutionInitiateRequest request, String sysUserId) {
        validate(request);

        VectorPool originalPool;
        try {
            originalPool = vectorPoolService.get(toInt(request.getVectorPoolId()));
        } catch (RuntimeException lookupFailure) {
            // Hibernate may throw ObjectNotFoundException (or wrappers) for
            // missing ids depending on session / proxy state; normalise to the
            // not-found IllegalStateException shape callers already handle.
            throw new IllegalStateException("VectorPool not found: " + request.getVectorPoolId(), lookupFailure);
        }
        if (originalPool == null) {
            throw new IllegalStateException("VectorPool not found: " + request.getVectorPoolId());
        }
        Sample parentSample = sampleService.get(originalPool.getSampleId());
        if (parentSample == null) {
            throw new IllegalStateException("VectorPool " + originalPool.getId() + " has no parent Sample");
        }

        gateBusinessRules(parentSample, originalPool);

        // Decon redistributes existing organisms into smaller pools — it does NOT
        // create new SampleItem rows. vector_pool_member (M:N) lets a SampleItem
        // belong to multiple pools across decon rounds (intake + first sub-pool +
        // sub-sub-pool, etc.).
        List<SampleItem> originalMembers = vectorPoolService.getMembersByPoolId(originalPool.getId());
        int requested;
        if (request.getMemberAssignments() != null && !request.getMemberAssignments().isEmpty()) {
            requested = request.getMemberAssignments().size();
        } else {
            requested = request.getPoolCount() * request.getOrganismsPerPool();
        }
        if (requested > originalMembers.size()) {
            throw new IllegalStateException("requested " + requested + " members but pool " + originalPool.getId()
                    + " has only " + originalMembers.size());
        }

        String analysisNotStartedStatusId = SpringContext.getBean(IStatusService.class)
                .getStatusID(AnalysisStatus.NotStarted);

        List<Analysis> parentAnalyses = analysisService.getAnalysesByVectorPoolId(String.valueOf(originalPool.getId()));

        List<Long> createdChildIds = new ArrayList<>(requested);
        List<String> createdExternalIds = new ArrayList<>(requested);
        List<Long> createdSubPoolIds = new ArrayList<>(request.getPoolCount());
        int testOrdersCreated = 0;

        java.util.Map<Integer, Long> locationOverrides = request.getSubPoolLocationIds() == null ? java.util.Map.of()
                : request.getSubPoolLocationIds();
        java.util.Map<Integer, String> notesOverrides = request.getSubPoolNotes() == null ? java.util.Map.of()
                : request.getSubPoolNotes();

        // When the frontend provides memberAssignments, honour them exactly so
        // the saved sub-pools match the Preview Grouping the tech saw. Otherwise
        // fall back to sequential sortOrder slicing — the intake fan-out lays
        // out members 1..N so consecutive chunks are deterministic.
        java.util.Map<Long, Integer> rawAssignments = request.getMemberAssignments();
        boolean useAssignments = rawAssignments != null && !rawAssignments.isEmpty();
        java.util.Map<Integer, List<SampleItem>> membersByAssignedPool = new java.util.HashMap<>();
        if (useAssignments) {
            java.util.Map<String, SampleItem> byId = new java.util.HashMap<>();
            for (SampleItem m : originalMembers) {
                byId.put(m.getId(), m);
            }
            for (java.util.Map.Entry<Long, Integer> e : rawAssignments.entrySet()) {
                SampleItem m = byId.get(String.valueOf(e.getKey()));
                Integer subPoolIndex = e.getValue();
                if (m == null || subPoolIndex == null || subPoolIndex < 1 || subPoolIndex > request.getPoolCount()) {
                    continue;
                }
                membersByAssignedPool.computeIfAbsent(subPoolIndex, k -> new ArrayList<>()).add(m);
            }
        }
        // Pool homogeneity gate: a sub-pool may only contain organisms of a
        // single sample type (mosquito / fly / flea / rodent / …). Cross-type
        // pooling makes no biological sense — a flea sitting with mosquitoes
        // confounds every downstream identification and pathogen test. Check
        // both the explicit-assignment path and the sortOrder-slicing fallback.
        for (int s = 1; s <= request.getPoolCount(); s++) {
            List<SampleItem> candidates;
            if (useAssignments) {
                candidates = membersByAssignedPool.getOrDefault(s, new ArrayList<>());
            } else {
                int start = (s - 1) * request.getOrganismsPerPool();
                int end = Math.min(start + request.getOrganismsPerPool(), originalMembers.size());
                candidates = start < end ? originalMembers.subList(start, end) : new ArrayList<>();
            }
            java.util.Set<String> distinctTypeNames = new java.util.LinkedHashSet<>();
            for (SampleItem m : candidates) {
                if (m.getTypeOfSample() != null && m.getTypeOfSample().getDescription() != null) {
                    distinctTypeNames.add(m.getTypeOfSample().getDescription());
                }
            }
            if (distinctTypeNames.size() > 1) {
                throw new IllegalStateException("sub-pool " + s + " mixes sample types ("
                        + String.join(", ", distinctTypeNames)
                        + ") — a pool must be sample-type-homogeneous (no flea + mosquito + rodent in one pool)");
            }
        }

        for (int s = 1; s <= request.getPoolCount(); s++) {
            List<SampleItem> subPoolMembers;
            if (useAssignments) {
                subPoolMembers = membersByAssignedPool.getOrDefault(s, new ArrayList<>());
            } else {
                int start = (s - 1) * request.getOrganismsPerPool();
                int end = start + request.getOrganismsPerPool();
                subPoolMembers = new ArrayList<>(originalMembers.subList(start, end));
            }

            VectorPool subPool = new VectorPool();
            subPool.setSampleId(parentSample.getId());
            subPool.setParentPool(originalPool);
            subPool.setActive(Boolean.TRUE);
            subPool.setSysUserId(sysUserId);
            String parentBase;
            if (originalPool.getParentPool() == null) {
                parentBase = poolLabelService.intakeLotBase(originalPool, parentSample);
            } else {
                parentBase = originalPool.getExternalId() != null && !originalPool.getExternalId().isBlank()
                        ? originalPool.getExternalId()
                        : poolLabelService.intakeLotBase(originalPool, parentSample);
            }
            subPool.setExternalId(poolLabelService.subPoolLabel(parentBase, s));
            VectorPool persistedSubPool = vectorPoolService.createPoolWithMembers(subPool, subPoolMembers, sysUserId);
            createdSubPoolIds.add(persistedSubPool.getId().longValue());

            Long locOverride = locationOverrides.get(s);
            String notesOverride = notesOverrides.get(s);
            for (SampleItem member : subPoolMembers) {
                if (locOverride != null) {
                    member.setCollectionLocationId(String.valueOf(locOverride));
                }
                if (notesOverride != null && !notesOverride.isBlank()) {
                    member.setCollectionNotes(notesOverride);
                }
                if (locOverride != null || (notesOverride != null && !notesOverride.isBlank())) {
                    member.setSysUserId(sysUserId);
                    sampleItemService.update(member);
                }
                createdChildIds.add(parseLong(member.getId()));
                createdExternalIds.add(member.getExternalId());
            }

            // Determine which analyses to copy. If the request specifies selected test IDs,
            // honour that selection; otherwise copy every pool analysis.
            List<String> selectedIds = request.getSelectedTestIds();
            Set<String> selectedSet = (selectedIds != null && !selectedIds.isEmpty()) ? new HashSet<>(selectedIds)
                    : null;
            for (Analysis a : parentAnalyses) {
                if (selectedSet != null && (a.getTest() == null || !selectedSet.contains(a.getTest().getId()))) {
                    continue; // tech excluded this test from sub-pools
                }
                Analysis copy = copyAnalysisForPool(a, persistedSubPool.getId(), analysisNotStartedStatusId, sysUserId);
                analysisService.insert(copy);
                testOrdersCreated++;
                if (selectedSet != null) {
                    selectedSet.remove(a.getTest() != null ? a.getTest().getId() : null);
                }
            }
            // Any IDs still in selectedSet are tests the tech added that don't exist
            // on the parent pool — create fresh pool analyses on the sub-pool.
            if (selectedSet != null) {
                for (String extraTestId : selectedSet) {
                    if (extraTestId == null)
                        continue;
                    org.openelisglobal.test.valueholder.Test extraTest = testService.getTestById(extraTestId);
                    if (extraTest == null)
                        continue;
                    Analysis fresh = new Analysis();
                    fresh.setSampleItem(null);
                    fresh.setVectorPoolId(String.valueOf(persistedSubPool.getId()));
                    fresh.setTest(extraTest);
                    fresh.setTestSection(extraTest.getTestSection());
                    fresh.setStatusId(analysisNotStartedStatusId);
                    fresh.setRevision("0");
                    fresh.setSysUserId(sysUserId);
                    fresh.setAnalysisType("MANUAL");
                    analysisService.insert(fresh);
                    testOrdersCreated++;
                }
            }
        }

        originalPool.setDeconvolutionStatus(STATUS_IN_PROGRESS);
        originalPool.setSysUserId(sysUserId);
        vectorPoolService.update(originalPool);

        String strategyLabel = request.getAssignmentStrategy() != null ? request.getAssignmentStrategy()
                : (useAssignments ? "explicit" : "sequential");
        LogEvent.logInfo(this.getClass().getName(), "initiate",
                "Vector deconvolution: created " + request.getPoolCount() + " sub-pools redistributing "
                        + createdChildIds.size() + " of " + originalMembers.size() + " members from pool "
                        + originalPool.getId() + " (sample " + parentSample.getId() + ", accession "
                        + parentSample.getAccessionNumber() + ") via " + strategyLabel + " strategy, "
                        + testOrdersCreated + " test orders queued");

        DeconvolutionResult result = new DeconvolutionResult(parseLong(parentSample.getId()), request.getVectorPoolId(),
                createdChildIds, createdExternalIds, testOrdersCreated, STATUS_IN_PROGRESS);
        result.setChildPoolIds(createdSubPoolIds);
        return result;
    }

    @Override
    @Transactional
    public DeconvolutionPreview previewReflexes(Long vectorPoolId) {
        if (vectorPoolId == null) {
            return new DeconvolutionPreview(java.util.List.of());
        }
        VectorPool originalPool = vectorPoolService.findById(toInt(vectorPoolId)).orElse(null);
        if (originalPool == null) {
            return new DeconvolutionPreview(java.util.List.of());
        }

        List<Analysis> parentAnalyses = analysisService.getAnalysesByVectorPoolId(String.valueOf(originalPool.getId()));
        List<SampleItem> poolMembers = vectorPoolService.getMembersByPoolId(originalPool.getId());

        List<DeconvolutionPreview.PoolTestEntry> entries = new ArrayList<>();
        for (Analysis a : parentAnalyses) {
            if (a.getTest() == null || a.getTest().getId() == null) {
                continue;
            }
            final String testId = a.getTest().getId();
            boolean confirmedForAll = !poolMembers.isEmpty() && poolMembers.stream().allMatch(m -> {
                try {
                    return analysisService.getAnalysisBySampleItemAndTest(m.getId(), testId) != null;
                } catch (RuntimeException ex) {
                    return false;
                }
            });
            entries.add(new DeconvolutionPreview.PoolTestEntry(testId, a.getTest().getName(), confirmedForAll));
        }
        return new DeconvolutionPreview(entries);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PanelTestGroup> getAvailablePanelTests(Long poolId) {
        if (poolId == null) {
            return java.util.List.of();
        }
        VectorPool pool = vectorPoolService.findById(poolId.intValue()).orElse(null);
        if (pool == null) {
            return java.util.List.of();
        }

        // Sample type from the pool's members.
        List<SampleItem> members = vectorPoolService.getMembersByPoolId(pool.getId());
        String sampleTypeId = members.isEmpty() ? null : members.get(0).getTypeOfSampleId();

        // Tests already on the pool — exclude these.
        List<Analysis> poolAnalyses = analysisService.getAnalysesByVectorPoolId(String.valueOf(poolId));
        Set<String> onPoolTestIds = new HashSet<>();
        for (Analysis a : poolAnalyses) {
            if (a.getTest() != null && a.getTest().getId() != null) {
                onPoolTestIds.add(a.getTest().getId());
            }
        }

        // All test IDs valid for this sample type.
        Set<String> sampleTypeTestIds = new HashSet<>();
        if (sampleTypeId != null) {
            List<org.openelisglobal.test.valueholder.Test> sts = typeOfSampleService
                    .getAllTestsBySampleTypeId(sampleTypeId);
            if (sts != null) {
                for (org.openelisglobal.test.valueholder.Test t : sts) {
                    if (t.getId() != null)
                        sampleTypeTestIds.add(t.getId());
                }
            }
        }

        // Walk every panel; keep tests matching the sample type not already on pool.
        List<PanelTestGroup> result = new ArrayList<>();
        for (org.openelisglobal.panel.valueholder.Panel panel : panelService.getAllPanels()) {
            List<org.openelisglobal.panelitem.valueholder.PanelItem> items = panelItemService
                    .getPanelItemsForPanel(panel.getId());
            if (items == null)
                continue;

            List<PanelTestGroup.PanelTestEntry> available = new ArrayList<>();
            for (org.openelisglobal.panelitem.valueholder.PanelItem pi : items) {
                if (pi.getTest() == null || pi.getTest().getId() == null)
                    continue;
                String testId = pi.getTest().getId();
                if (!sampleTypeTestIds.isEmpty() && !sampleTypeTestIds.contains(testId))
                    continue;
                if (onPoolTestIds.contains(testId))
                    continue;
                available.add(new PanelTestGroup.PanelTestEntry(testId,
                        pi.getTest().getName() != null ? pi.getTest().getName() : ""));
            }
            if (!available.isEmpty()) {
                result.add(new PanelTestGroup(panel.getId(),
                        panel.getPanelName() != null ? panel.getPanelName() : "Panel " + panel.getId(), available));
            }
        }
        return result;
    }

    @Override
    @Transactional
    public String evaluateResultEntered(Long vectorPoolId, String sysUserId) {
        if (vectorPoolId == null) {
            return null;
        }
        VectorPool pool;
        try {
            pool = vectorPoolService.get(vectorPoolId.intValue());
        } catch (org.hibernate.ObjectNotFoundException e) {
            return null;
        }
        Sample sample;
        try {
            sample = sampleService.get(pool.getSampleId());
        } catch (org.hibernate.ObjectNotFoundException e) {
            return null;
        }
        if (!VECTOR_DOMAIN.equals(sample.getDomain())) {
            return null;
        }
        if (!STATUS_NOT_APPLICABLE.equals(pool.getDeconvolutionStatus()) && pool.getDeconvolutionStatus() != null) {
            return null;
        }
        if (vectorPoolService.countMembersByPoolId(pool.getId()) < 2) {
            // A 1-member sub-pool cannot be split further — auto-complete it so
            // evaluateChildResultsForCompletion can close the intake pool once all
            // leaves are done. Intake pools with a single member (edge case) are
            // left for the tech to handle via the normal PENDING/confirm path.
            if (pool.getParentPool() != null) {
                pool.setDeconvolutionStatus(STATUS_COMPLETE);
                pool.setSysUserId(sysUserId);
                vectorPoolService.update(pool);
                LogEvent.logInfo(this.getClass().getName(), "evaluateResultEntered",
                        "Vector result watcher: 1-member sub-pool " + pool.getId() + " (sample " + sample.getId()
                                + ", accession " + sample.getAccessionNumber() + ") auto-completed on result entry");
                evaluateChildResultsForCompletion(pool.getId().longValue(), sysUserId);
                return STATUS_COMPLETE;
            }
            return null;
        }
        pool.setDeconvolutionStatus(STATUS_PENDING);
        pool.setSysUserId(sysUserId);
        vectorPoolService.update(pool);
        LogEvent.logInfo(this.getClass().getName(), "evaluateResultEntered",
                "Vector result watcher: pool " + pool.getId() + " (sample " + sample.getId() + ", accession "
                        + sample.getAccessionNumber() + ") pool.deconvolutionStatus → PENDING (result entered)");
        return STATUS_PENDING;
    }

    @Override
    @Transactional
    public void confirmResultForAllMembers(Long vectorPoolId, String sysUserId) {
        if (vectorPoolId == null) {
            throw new IllegalArgumentException("vectorPoolId is required");
        }
        VectorPool pool = vectorPoolService.findById(vectorPoolId.intValue()).orElse(null);
        if (pool == null) {
            throw new IllegalArgumentException("VectorPool not found: " + vectorPoolId);
        }
        Sample sample;
        try {
            sample = sampleService.get(pool.getSampleId());
        } catch (org.hibernate.ObjectNotFoundException e) {
            throw new IllegalArgumentException("Sample not found for pool: " + vectorPoolId);
        }
        if (!VECTOR_DOMAIN.equals(sample.getDomain())) {
            throw new IllegalArgumentException("confirmResultForAllMembers only available on VECTOR-domain pools");
        }

        String finalizedStatusId = SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized);
        List<Analysis> poolAnalyses = analysisService.getAnalysesByVectorPoolId(String.valueOf(pool.getId()));
        List<SampleItem> members = vectorPoolService.getMembersByPoolId(pool.getId());

        for (SampleItem member : members) {
            for (Analysis poolAnalysis : poolAnalyses) {
                Analysis copy = new Analysis();
                copy.setSampleItem(member);
                copy.setVectorPoolId(null);
                copy.setTest(poolAnalysis.getTest());
                copy.setTestSection(poolAnalysis.getTestSection());
                copy.setStatusId(finalizedStatusId);
                copy.setRevision("0");
                copy.setSysUserId(sysUserId);
                copy.setAnalysisType(
                        poolAnalysis.getAnalysisType() != null ? poolAnalysis.getAnalysisType() : "MANUAL");
                copy.setSampleTypeName(poolAnalysis.getSampleTypeName());
                String newAnalysisId = analysisService.insert(copy);

                List<Result> poolResults = resultService.getResultsByAnalysis(poolAnalysis);
                for (Result poolResult : poolResults) {
                    Result resultCopy = new Result();
                    resultCopy.setAnalysis(analysisService.get(newAnalysisId));
                    resultCopy.setValue(poolResult.getValue());
                    resultCopy.setResultType(poolResult.getResultType());
                    resultCopy.setSysUserId(sysUserId);
                    resultService.insert(resultCopy);
                }
            }
        }

        pool.setDeconvolutionStatus(STATUS_COMPLETE);
        // When there is no prior split the pool IS the only leaf — 100% confirmed.
        if (pool.getParentPool() == null) {
            pool.setDeconvolutionOutcomePct(100.0);
        }
        pool.setSysUserId(sysUserId);
        vectorPoolService.update(pool);

        LogEvent.logInfo(this.getClass().getName(), "confirmResultForAllMembers",
                "Vector pool " + pool.getId() + " (sample " + sample.getId() + ", accession "
                        + sample.getAccessionNumber() + "): result confirmed for " + members.size()
                        + " members — analyses copied, pool closed");

        // Roll up to intake only when this is a sub-pool leaf; intake pools
        // confirmed directly are already set to COMPLETE above.
        if (pool.getParentPool() != null) {
            evaluateChildResultsForCompletion(vectorPoolId, sysUserId);
        }
    }

    @Override
    @Transactional
    public void confirmAnalysisForAllMembers(Long vectorPoolId, String analysisId, String sysUserId) {
        if (vectorPoolId == null || analysisId == null) {
            throw new IllegalArgumentException("vectorPoolId and analysisId are required");
        }
        VectorPool pool = vectorPoolService.findById(vectorPoolId.intValue()).orElse(null);
        if (pool == null) {
            throw new IllegalArgumentException("VectorPool not found: " + vectorPoolId);
        }
        Sample sample;
        try {
            sample = sampleService.get(pool.getSampleId());
        } catch (org.hibernate.ObjectNotFoundException e) {
            throw new IllegalArgumentException("Sample not found for pool: " + vectorPoolId);
        }
        if (!VECTOR_DOMAIN.equals(sample.getDomain())) {
            throw new IllegalArgumentException("confirmAnalysisForAllMembers only available on VECTOR-domain pools");
        }
        Analysis poolAnalysis = analysisService.get(analysisId);
        if (poolAnalysis == null) {
            throw new IllegalArgumentException("Analysis not found: " + analysisId);
        }
        String poolId = String.valueOf(vectorPoolId);
        if (!poolId.equals(String.valueOf(poolAnalysis.getVectorPoolId()))) {
            throw new IllegalArgumentException("Analysis " + analysisId + " does not belong to pool " + vectorPoolId);
        }

        // Member-level analyses are created with TechnicalAcceptance so they surface
        // in the validation screen for individual sign-off, matching the pool-level
        // validation workflow. Finalized status would hide them from validation.
        String memberStatusId = SpringContext.getBean(IStatusService.class)
                .getStatusID(AnalysisStatus.TechnicalAcceptance);
        String notStartedStatusId = SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted);
        List<SampleItem> members = vectorPoolService.getMembersByPoolId(pool.getId());
        List<Result> poolResults = resultService.getResultsByAnalysis(poolAnalysis);

        for (SampleItem member : members) {
            // Idempotent: skip members that already have this test confirmed.
            if (analysisService.getAnalysisBySampleItemAndTest(member.getId(),
                    poolAnalysis.getTest().getId()) != null) {
                continue;
            }
            Analysis copy = new Analysis();
            copy.setSampleItem(member);
            copy.setVectorPoolId(null);
            copy.setTest(poolAnalysis.getTest());
            copy.setTestSection(poolAnalysis.getTestSection());
            copy.setStatusId(memberStatusId);
            copy.setRevision("0");
            copy.setSysUserId(sysUserId);
            copy.setAnalysisType(poolAnalysis.getAnalysisType() != null ? poolAnalysis.getAnalysisType() : "MANUAL");
            copy.setSampleTypeName(poolAnalysis.getSampleTypeName());
            String newAnalysisId = analysisService.insert(copy);

            for (Result poolResult : poolResults) {
                Result resultCopy = new Result();
                resultCopy.setAnalysis(analysisService.get(newAnalysisId));
                resultCopy.setValue(poolResult.getValue());
                resultCopy.setResultType(poolResult.getResultType());
                resultCopy.setSysUserId(sysUserId);
                resultService.insert(resultCopy);
            }
        }

        // Advance pool to COMPLETE if every pool analysis is now confirmed for all
        // members.
        List<Analysis> allPoolAnalyses = analysisService.getAnalysesByVectorPoolId(poolId);
        boolean allConfirmed = true;
        for (Analysis pa : allPoolAnalyses) {
            if (pa.getTest() == null) {
                continue;
            }
            for (SampleItem member : members) {
                if (analysisService.getAnalysisBySampleItemAndTest(member.getId(), pa.getTest().getId()) == null) {
                    allConfirmed = false;
                    break;
                }
            }
            if (!allConfirmed) {
                break;
            }
        }
        if (allConfirmed) {
            pool.setDeconvolutionStatus(STATUS_COMPLETE);
            if (pool.getParentPool() == null) {
                pool.setDeconvolutionOutcomePct(100.0);
            }
            pool.setSysUserId(sysUserId);
            vectorPoolService.update(pool);
            if (pool.getParentPool() != null) {
                evaluateChildResultsForCompletion(vectorPoolId, sysUserId);
            }
        }
    }

    @Override
    @Transactional
    public DeconvolutionOutcome evaluateChildResultsForCompletion(Long anyPoolId, String sysUserId) {
        if (anyPoolId == null) {
            return null;
        }

        // Walk up to the intake pool to find the parent Sample.
        VectorPool startPool = vectorPoolService.findById(anyPoolId.intValue()).orElse(null);
        if (startPool == null) {
            return null;
        }
        VectorPool intake = startPool;
        while (intake.getParentPool() != null) {
            intake = intake.getParentPool();
        }

        Sample sample;
        try {
            sample = sampleService.get(intake.getSampleId());
        } catch (org.hibernate.ObjectNotFoundException e) {
            return null;
        }
        if (!VECTOR_DOMAIN.equals(sample.getDomain())) {
            return null;
        }
        // Gate: the intake pool must be IN_PROGRESS to advance to COMPLETE.
        if (!STATUS_IN_PROGRESS.equals(intake.getDeconvolutionStatus())) {
            return null;
        }
        String accession = sample.getAccessionNumber();

        // Leaves = sub-pools (anywhere in the tree) reachable from this intake,
        // with no further children of their own.
        List<VectorPool> leaves = new ArrayList<>();
        for (VectorPool p : vectorPoolService.getBySampleId(intake.getSampleId())) {
            if (!isDescendantOf(p, intake.getId())) {
                continue;
            }
            if (vectorPoolService.getByParentPoolId(p.getId()).isEmpty()) {
                leaves.add(p);
            }
        }
        if (leaves.isEmpty()) {
            return null;
        }

        // All leaf pools must be COMPLETE (tech-confirmed). Any that are still
        // PENDING (awaiting decision) or IN_PROGRESS (mid-decon) block completion.
        int confirmedCount = 0;
        for (VectorPool leaf : leaves) {
            if (!STATUS_COMPLETE.equals(leaf.getDeconvolutionStatus())) {
                return null;
            }
            confirmedCount++;
        }

        int totalLeafCount = leaves.size();
        double pct = totalLeafCount > 0 ? ((double) confirmedCount / totalLeafCount) * 100.0 : 0.0;
        intake.setDeconvolutionStatus(STATUS_COMPLETE);
        intake.setDeconvolutionOutcomePct(pct);
        intake.setSysUserId(sysUserId);
        vectorPoolService.update(intake);

        LogEvent.logInfo(this.getClass().getName(), "evaluateChildResultsForCompletion",
                "Vector deconvolution complete: pool " + intake.getId() + " (sample " + sample.getId() + ", accession "
                        + accession + ") " + confirmedCount + "/" + totalLeafCount + " sub-pools confirmed");

        return new DeconvolutionOutcome(Long.valueOf(sample.getId()), confirmedCount, totalLeafCount);
    }

    /**
     * True if {@code candidate} is a descendant of (or equal to) the pool with id
     * {@code intakePoolId}.
     */
    private boolean isDescendantOf(VectorPool candidate, Integer intakePoolId) {
        VectorPool p = candidate;
        while (p != null) {
            if (intakePoolId.equals(p.getId())) {
                return true;
            }
            p = p.getParentPool();
        }
        return false;
    }

    @Override
    @Transactional
    public void forceComplete(Long vectorPoolId, String sysUserId) {
        if (vectorPoolId == null) {
            throw new IllegalArgumentException("vectorPoolId is required");
        }
        VectorPool pool = vectorPoolService.findById(vectorPoolId.intValue()).orElse(null);
        if (pool == null) {
            throw new IllegalArgumentException("VectorPool not found: " + vectorPoolId);
        }
        Sample sample;
        try {
            sample = sampleService.get(pool.getSampleId());
        } catch (org.hibernate.ObjectNotFoundException e) {
            throw new IllegalArgumentException("Sample not found for pool: " + vectorPoolId);
        }
        if (!VECTOR_DOMAIN.equals(sample.getDomain())) {
            throw new IllegalArgumentException(
                    "forceComplete only available on VECTOR-domain pools; got domain: " + sample.getDomain());
        }
        String current = pool.getDeconvolutionStatus();
        if (current == null || STATUS_NOT_APPLICABLE.equals(current)) {
            throw new IllegalArgumentException(
                    "Pool " + vectorPoolId + " has no deconvolution to complete (status=" + current + ")");
        }
        pool.setDeconvolutionStatus(STATUS_COMPLETE);
        pool.setSysUserId(sysUserId);
        vectorPoolService.update(pool);

        LogEvent.logInfo(this.getClass().getName(), "forceComplete",
                "Supervisor override: pool " + vectorPoolId + " (sample " + sample.getId() + ", accession "
                        + sample.getAccessionNumber() + ") deconvolutionStatus → COMPLETE");
    }

    @Override
    @Transactional(readOnly = true)
    public DeconvolutionResult getDeconvolution(Long poolId) {
        VectorPool intakePool = vectorPoolService.findById(poolId.intValue()).orElse(null);
        if (intakePool == null) {
            throw new IllegalArgumentException("VectorPool not found: " + poolId);
        }
        String status = intakePool.getDeconvolutionStatus();
        if (status == null || STATUS_NOT_APPLICABLE.equals(status)) {
            throw new IllegalArgumentException(
                    "Pool " + poolId + " has no deconvolution in progress (status=" + status + ")");
        }
        Sample sample;
        try {
            sample = sampleService.get(intakePool.getSampleId());
        } catch (org.hibernate.ObjectNotFoundException e) {
            throw new IllegalArgumentException("Sample not found for pool: " + poolId);
        }
        Long sampleId = Long.parseLong(sample.getId());

        List<VectorPool> allPools = vectorPoolService.getBySampleId(String.valueOf(sampleId));

        List<VectorPool> subPools = new ArrayList<>();
        for (VectorPool p : allPools) {
            if (p.getParentPool() != null) {
                subPools.add(p);
            }
        }

        List<Long> childIds = new ArrayList<>();
        List<String> childExternalIds = new ArrayList<>();
        List<DeconvolutionNode> tree = new ArrayList<>();
        Set<Integer> nonLeafPoolIds = new HashSet<>();
        for (VectorPool sub : subPools) {
            if (sub.getParentPool() != null) {
                nonLeafPoolIds.add(sub.getParentPool().getId());
            }
        }

        int intakeMemberCount = vectorPoolService.countMembersByPoolId(intakePool.getId());
        String intakeLabel = poolLabel(intakePool);
        DeconvolutionNode intakeNode = new DeconvolutionNode(intakePool.getId().longValue(), intakeLabel, null,
                intakeMemberCount);
        intakeNode.setResults(getResultSummariesForPool(intakePool));
        tree.add(intakeNode);

        for (VectorPool sub : subPools) {
            int memberCount = vectorPoolService.countMembersByPoolId(sub.getId());
            String label = poolLabel(sub);
            Long parentPoolId = sub.getParentPool() == null ? null : sub.getParentPool().getId().longValue();
            DeconvolutionNode node = new DeconvolutionNode(sub.getId().longValue(), label, parentPoolId, memberCount);
            node.setResults(getResultSummariesForPool(sub));
            tree.add(node);

            for (SampleItem member : vectorPoolService.getMembersByPoolId(sub.getId())) {
                childIds.add(parseLong(member.getId()));
                childExternalIds.add(member.getExternalId());
            }
        }

        DeconvolutionResult result = new DeconvolutionResult(sampleId, poolId, childIds, childExternalIds, 0, status);
        result.setDeconvolutionOutcomePct(intakePool.getDeconvolutionOutcomePct());
        result.setTree(tree);

        int leafTotal;
        int leafPositive;
        if (subPools.isEmpty()) {
            leafTotal = vectorPoolService.countMembersByPoolId(intakePool.getId());
            leafPositive = STATUS_COMPLETE.equals(status) ? leafTotal : 0;
        } else {
            leafTotal = 0;
            leafPositive = 0;
            for (VectorPool sub : subPools) {
                if (nonLeafPoolIds.contains(sub.getId())) {
                    continue;
                }
                leafTotal++;
                if (STATUS_COMPLETE.equals(sub.getDeconvolutionStatus())) {
                    leafPositive++;
                }
            }
        }
        result.setLeafTotalCount(leafTotal);
        result.setLeafPositiveCount(leafPositive);
        return result;
    }

    private List<PoolResultSummary> getResultSummariesForPool(VectorPool pool) {
        List<PoolResultSummary> summaries = new ArrayList<>();
        try {
            List<Analysis> analyses = analysisService.getAnalysesByVectorPoolId(String.valueOf(pool.getId()));
            if (analyses == null) {
                return summaries;
            }
            List<SampleItem> members = vectorPoolService.getMembersByPoolId(pool.getId());
            for (Analysis a : analyses) {
                if (a == null || a.getTest() == null) {
                    continue;
                }
                List<Result> results = resultService.getResultsByAnalysis(a);
                if (results == null) {
                    continue;
                }
                for (Result r : results) {
                    String raw = r.getValue();
                    if (raw == null || raw.isBlank()) {
                        continue;
                    }
                    String display = raw;
                    if ("D".equals(r.getResultType())) {
                        try {
                            Dictionary dict = dictionaryService.getDataForId(raw);
                            if (dict != null && dict.getDictEntry() != null) {
                                display = dict.getDictEntry();
                            }
                        } catch (RuntimeException ignored) {
                        }
                    }
                    boolean confirmed = !members.isEmpty() && members.stream().allMatch(member -> {
                        try {
                            return analysisService.getAnalysisBySampleItemAndTest(member.getId(),
                                    a.getTest().getId()) != null;
                        } catch (RuntimeException ex) {
                            return false;
                        }
                    });
                    summaries.add(new PoolResultSummary(a.getTest().getName(), display, a.getId(), confirmed));
                }
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
        }
        return summaries;
    }

    private String poolLabel(VectorPool pool) {
        if (pool.getExternalId() != null && !pool.getExternalId().isBlank()) {
            return pool.getExternalId();
        }
        return vectorPoolService.getFirstNonVoidedMemberByPoolId(pool.getId()).map(SampleItem::getExternalId)
                .map(VectorDeconvolutionServiceImpl::stripLastSegment).orElse("Pool #" + pool.getId());
    }

    private static String stripLastSegment(String externalId) {
        if (externalId == null) {
            return null;
        }
        int idx = externalId.lastIndexOf('-');
        return idx > 0 ? externalId.substring(0, idx) : externalId;
    }

    private void validate(DeconvolutionInitiateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("deconvolution request must not be null");
        }
        if (request.getVectorPoolId() == null) {
            throw new IllegalArgumentException("vectorPoolId is required");
        }
        if (request.getPoolCount() < 2) {
            throw new IllegalArgumentException("poolCount must be at least 2; got: " + request.getPoolCount());
        }
        if (request.getOrganismsPerPool() < 1) {
            throw new IllegalArgumentException(
                    "organismsPerPool must be at least 1; got: " + request.getOrganismsPerPool());
        }
        if (request.getNotes() != null && request.getNotes().length() > 500) {
            throw new IllegalArgumentException("notes must be 500 characters or less");
        }
    }

    private void gateBusinessRules(Sample parentSample, VectorPool originalPool) {
        if (!VECTOR_DOMAIN.equals(parentSample.getDomain())) {
            throw new IllegalStateException(
                    "deconvolution only available on VECTOR-domain samples; got domain: " + parentSample.getDomain());
        }

        int memberCount = vectorPoolService.countMembersByPoolId(originalPool.getId());
        if (memberCount < 2) {
            throw new IllegalStateException(
                    "deconvolution requires a pool with more than one member; got: " + memberCount);
        }

        int depth = poolDepth(originalPool);
        if (depth >= org.openelisglobal.vector.service.VectorPoolLabelService.MAX_DECON_DEPTH) {
            throw new IllegalStateException(
                    "pool " + originalPool.getId() + " is already at maximum deconvolution depth ("
                            + org.openelisglobal.vector.service.VectorPoolLabelService.MAX_DECON_DEPTH
                            + "); no further splitting is permitted");
        }

        // Block re-splitting only when an existing sub-pool has advanced beyond
        // DRAFT (any test result entered, or any analysis past NotStarted). An
        // empty-draft sub-pool set may be replaced — the user hasn't done lab
        // work on it yet. The check is per-VectorPool so deeper recursion works.
        if (hasNonDraftSubPools(originalPool)) {
            throw new IllegalStateException("pool " + originalPool.getId()
                    + " already has sub-pools with results — physical groupings are immutable");
        }
    }

    private boolean hasNonDraftSubPools(VectorPool originalPool) {
        String notStartedStatusId = SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted);
        for (VectorPool subPool : vectorPoolService.getByParentPoolId(originalPool.getId())) {
            List<Analysis> analyses = analysisService.getAnalysesByVectorPoolId(String.valueOf(subPool.getId()));
            if (analyses == null) {
                continue;
            }
            for (Analysis a : analyses) {
                if (!notStartedStatusId.equals(a.getStatusId())) {
                    return true;
                }
                List<Result> results = resultService.getResultsByAnalysis(a);
                if (results != null && !results.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private Analysis copyAnalysisForPool(Analysis source, Integer destPoolId, String notStartedStatusId,
            String sysUserId) {
        Analysis copy = new Analysis();
        // Pool-anchored: leave sampleItem null, set vector_pool_id. The XOR
        // constraint ck_analysis_pool_or_item demands exactly one of the two.
        copy.setSampleItem(null);
        copy.setVectorPoolId(String.valueOf(destPoolId));
        copy.setTest(source.getTest());
        copy.setTestSection(source.getTestSection());
        copy.setStatusId(notStartedStatusId);
        copy.setRevision("0");
        copy.setSysUserId(sysUserId);
        // analysis_type is NOT NULL on the column. Inherit from source when
        // present, default to MANUAL otherwise (covers legacy rows that may
        // have been seeded without it).
        copy.setAnalysisType(source.getAnalysisType() != null ? source.getAnalysisType() : "MANUAL");
        copy.setSampleTypeName(source.getSampleTypeName());
        // No result-copy: parent positives do NOT propagate to children. Each
        // sub-pool runs its own assay.
        return copy;
    }

    private static long parseLong(String s) {
        try {
            return s == null ? 0L : Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static Integer toInt(Long id) {
        return id == null ? null : id.intValue();
    }

    private static int poolDepth(VectorPool pool) {
        int depth = 0;
        VectorPool p = pool;
        while (p != null) {
            depth++;
            p = p.getParentPool();
        }
        return depth;
    }

}
