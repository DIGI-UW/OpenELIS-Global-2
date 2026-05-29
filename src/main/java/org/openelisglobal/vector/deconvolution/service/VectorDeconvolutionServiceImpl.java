package org.openelisglobal.vector.deconvolution.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.service.AnalysisServiceImpl;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testreflex.action.bean.ReflexRule;
import org.openelisglobal.testreflex.action.bean.ReflexRuleAction;
import org.openelisglobal.testreflex.action.bean.ReflexRuleCondition;
import org.openelisglobal.testreflex.action.bean.ReflexRuleOptions.NumericRelationOptions;
import org.openelisglobal.testreflex.action.bean.ReflexRuleOptions.OverallOptions;
import org.openelisglobal.testreflex.service.TestReflexService;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionInitiateRequest;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionOutcome;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionPreview;
import org.openelisglobal.vector.deconvolution.dto.DeconvolutionDTOs.DeconvolutionResult;
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
    private TestReflexService testReflexService;

    @Autowired
    private TestService testService;

    @Autowired
    private NoteService noteService;

    @Autowired
    private VectorPoolService vectorPoolService;

    @Autowired
    private org.openelisglobal.vector.service.VectorPoolLabelService poolLabelService;

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

        // Eagerly queue reflex action tests on the original pool BEFORE
        // snapshotting parentAnalyses so sub-pools inherit them via the copy below.
        evaluateReflexesEagerly(originalPool, sysUserId, analysisNotStartedStatusId);

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

            for (Analysis a : parentAnalyses) {
                Analysis copy = copyAnalysisForPool(a, persistedSubPool.getId(), analysisNotStartedStatusId, sysUserId);
                analysisService.insert(copy);
                testOrdersCreated++;
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
    @Transactional(readOnly = true)
    public DeconvolutionPreview previewReflexes(Long vectorPoolId) {
        if (vectorPoolId == null) {
            return new DeconvolutionPreview(java.util.List.<DeconvolutionPreview.CopiedEntry>of(), java.util.List.of());
        }
        VectorPool originalPool = vectorPoolService.findById(toInt(vectorPoolId)).orElse(null);
        if (originalPool == null) {
            return new DeconvolutionPreview(java.util.List.<DeconvolutionPreview.CopiedEntry>of(), java.util.List.of());
        }

        List<Analysis> parentAnalyses = analysisService.getAnalysesByVectorPoolId(String.valueOf(originalPool.getId()));
        List<SampleItem> poolMembers = vectorPoolService.getMembersByPoolId(originalPool.getId());
        // Use LinkedHashMap to preserve insertion order while tracking ruleLabel per
        // test.
        java.util.LinkedHashMap<String, String> copiedTestToRuleLabel = new java.util.LinkedHashMap<>();
        Set<String> existingTestIds = new HashSet<>();
        Map<String, List<String>> resultsByTestName = new HashMap<>();
        for (Analysis a : parentAnalyses) {
            if (a.getTest() != null && a.getTest().getName() != null) {
                copiedTestToRuleLabel.put(a.getTest().getName(), extractReflexRuleLabel(a));
            }
            if (a.getTest() != null && a.getTest().getId() != null) {
                existingTestIds.add(a.getTest().getId());
            }
            if (a.getTest() == null) {
                continue;
            }
            // Only include results that are confirmed for ALL pool members — the same
            // gate used by evaluateReflexesEagerly so the preview matches reality.
            if (!poolMembers.isEmpty()) {
                final String testId = a.getTest().getId();
                boolean confirmedForAll = poolMembers.stream().allMatch(m -> {
                    try {
                        return analysisService.getAnalysisBySampleItemAndTest(m.getId(), testId) != null;
                    } catch (RuntimeException ex) {
                        return false;
                    }
                });
                if (!confirmedForAll) {
                    continue;
                }
            }
            List<Result> results = resultService.getResultsByAnalysis(a);
            if (results == null) {
                continue;
            }
            for (Result r : results) {
                if (r.getValue() == null || r.getValue().isBlank()) {
                    continue;
                }
                resultsByTestName.computeIfAbsent(a.getTest().getName(), k -> new ArrayList<>())
                        .add(resolveResultText(r));
            }
        }

        int poolMemberCount = vectorPoolService.countMembersByPoolId(originalPool.getId());
        List<DeconvolutionPreview.ReflexEntry> reflexEntries = new ArrayList<>();
        List<String> individualOnlyRuleLabels = new ArrayList<>();
        List<ReflexRule> rules;
        try {
            rules = testReflexService.getAllReflexRules();
        } catch (RuntimeException e) {
            rules = java.util.List.of();
        }
        for (ReflexRule rule : rules) {
            if (!Boolean.TRUE.equals(rule.getActive())) {
                continue;
            }
            if (rule.getConditions() == null || rule.getConditions().isEmpty() || rule.getActions() == null
                    || rule.getActions().isEmpty()) {
                continue;
            }
            boolean any = rule.getOverall() == null || rule.getOverall() == OverallOptions.ANY;
            int satisfied = 0;
            for (ReflexRuleCondition c : rule.getConditions()) {
                List<String> vals = resultsByTestName.get(c.getTestName());
                if (vals != null && conditionMatches(c, vals)) {
                    satisfied++;
                }
            }
            boolean met = any ? satisfied > 0 : satisfied == rule.getConditions().size();
            if (!met) {
                continue;
            }
            String ruleLabel = rule.getRuleName() != null ? rule.getRuleName() : ("rule#" + rule.getId());
            // ruleHasNewActions tracks whether any action test doesn't already exist.
            // If all actions are already present (already fired), the rule should not
            // appear in individualOnlyRuleLabels — it has nothing new to offer.
            boolean ruleHasNewActions = false;
            boolean ruleIsIndividualOnly = true;
            for (ReflexRuleAction action : rule.getActions()) {
                String reflexTestId = action.getReflexTestId();
                if (reflexTestId == null || existingTestIds.contains(reflexTestId)) {
                    continue;
                }
                Test reflexTest = testService.getTestById(reflexTestId);
                if (reflexTest == null) {
                    continue;
                }
                ruleHasNewActions = true;
                if (isIndividualOnlyTest(reflexTest) && poolMemberCount > 1) {
                    continue;
                }
                ruleIsIndividualOnly = false;
                reflexEntries.add(new DeconvolutionPreview.ReflexEntry(reflexTest.getName(), ruleLabel));
            }
            if (ruleHasNewActions && ruleIsIndividualOnly && poolMemberCount > 1) {
                individualOnlyRuleLabels.add(ruleLabel);
            }
        }

        List<DeconvolutionPreview.CopiedEntry> copiedEntries = new ArrayList<>();
        copiedTestToRuleLabel
                .forEach((name, label) -> copiedEntries.add(new DeconvolutionPreview.CopiedEntry(name, label)));
        return new DeconvolutionPreview(copiedEntries, reflexEntries, individualOnlyRuleLabels);
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

        // After all members are confirmed for this analysis, evaluate reflex rules at
        // the pool level. evaluateReflexesEagerly gates on "confirmed for all members"
        // so only this now-confirmed test (and any other previously confirmed tests)
        // contribute to the trigger set — unconfirmed results are excluded.
        // This produces pool-level reflex analyses (same pattern as the primary tests)
        // so the tech enters one pool-level result for the reflex test and confirms it
        // to all members via the same confirmAnalysisForAllMembers flow.
        evaluateReflexesEagerly(pool, sysUserId, notStartedStatusId);

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

    // Sporozoite microscopy dissects the salivary gland of a single mosquito;
    // on a pool it's biologically meaningless. The eager reflex evaluator skips
    // entries here when the source pool has >1 member.
    private static final Set<String> INDIVIDUAL_ONLY_TEST_PREFIXES = Set.of("sporozoite microscopy");

    private boolean isIndividualOnlyTest(Test test) {
        if (test == null || test.getName() == null) {
            return false;
        }
        String name = test.getName().toLowerCase();
        for (String prefix : INDIVIDUAL_ONLY_TEST_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void evaluateReflexesEagerly(VectorPool originalPool, String sysUserId, String notStartedStatusId) {
        List<Analysis> existingAnalyses = analysisService
                .getAnalysesByVectorPoolId(String.valueOf(originalPool.getId()));
        if (existingAnalyses == null || existingAnalyses.isEmpty()) {
            return;
        }

        // Only reflex-trigger on results that are already confirmed for ALL pool
        // members. Unconfirmed results will trigger their reflexes later through
        // evaluateReflexesForMember when each member is individually confirmed.
        List<SampleItem> poolMembers = vectorPoolService.getMembersByPoolId(originalPool.getId());

        Map<String, List<String>> resultsByTestName = new HashMap<>();
        Set<String> existingTestIds = new HashSet<>();
        for (Analysis a : existingAnalyses) {
            if (a.getTest() != null && a.getTest().getId() != null) {
                existingTestIds.add(a.getTest().getId());
            }
            if (a.getTest() == null) {
                continue;
            }
            // Skip analyses whose result is not yet confirmed for every pool member.
            if (!poolMembers.isEmpty()) {
                boolean confirmedForAll = poolMembers.stream().allMatch(m -> {
                    try {
                        return analysisService.getAnalysisBySampleItemAndTest(m.getId(), a.getTest().getId()) != null;
                    } catch (RuntimeException ex) {
                        return false;
                    }
                });
                if (!confirmedForAll) {
                    continue;
                }
            }
            List<Result> results = resultService.getResultsByAnalysis(a);
            if (results == null) {
                continue;
            }
            for (Result r : results) {
                if (r.getValue() == null || r.getValue().isBlank()) {
                    continue;
                }
                resultsByTestName.computeIfAbsent(a.getTest().getName(), k -> new ArrayList<>())
                        .add(resolveResultText(r));
            }
        }
        if (resultsByTestName.isEmpty()) {
            return;
        }

        List<ReflexRule> rules;
        try {
            rules = testReflexService.getAllReflexRules();
        } catch (RuntimeException e) {
            LogEvent.logError(this.getClass().getName(), "evaluateReflexesEagerly", e.getMessage());
            return;
        }
        if (rules == null || rules.isEmpty()) {
            return;
        }

        Optional<SampleItem> representativeOpt = vectorPoolService
                .getFirstNonVoidedMemberByPoolId(originalPool.getId());
        SampleItem representative = representativeOpt.orElse(null);

        int queued = 0;
        for (ReflexRule rule : rules) {
            if (!Boolean.TRUE.equals(rule.getActive())) {
                continue;
            }
            if (rule.getConditions() == null || rule.getConditions().isEmpty() || rule.getActions() == null
                    || rule.getActions().isEmpty()) {
                continue;
            }

            boolean any = rule.getOverall() == null || rule.getOverall() == OverallOptions.ANY;
            int satisfied = 0;
            for (ReflexRuleCondition c : rule.getConditions()) {
                List<String> vals = resultsByTestName.get(c.getTestName());
                if (vals != null && conditionMatches(c, vals)) {
                    satisfied++;
                }
            }
            boolean met = any ? satisfied > 0 : satisfied == rule.getConditions().size();
            if (!met) {
                continue;
            }

            for (ReflexRuleAction action : rule.getActions()) {
                String reflexTestId = action.getReflexTestId();
                if (reflexTestId == null || existingTestIds.contains(reflexTestId)) {
                    continue;
                }
                Test reflexTest = testService.getTestById(reflexTestId);
                if (reflexTest == null) {
                    continue;
                }
                // Reflex engine has no per-specimen quantity gate; enforce it here.
                if (isIndividualOnlyTest(reflexTest)) {
                    int memberCount = vectorPoolService.countMembersByPoolId(originalPool.getId());
                    if (memberCount > 1) {
                        LogEvent.logInfo(this.getClass().getName(), "evaluateReflexesEagerly",
                                "Skipping individual-only reflex test '" + reflexTest.getName() + "' for pool "
                                        + originalPool.getId() + " (size=" + memberCount + "). "
                                        + "Will be queued automatically when the pool is decon'd to qty=1 sub-pools.");
                        continue;
                    }
                }
                Analysis reflexAnalysis = new Analysis();
                reflexAnalysis.setSampleItem(null);
                reflexAnalysis.setVectorPoolId(String.valueOf(originalPool.getId()));
                reflexAnalysis.setTest(reflexTest);
                reflexAnalysis.setTestSection(reflexTest.getTestSection());
                reflexAnalysis.setStatusId(notStartedStatusId);
                reflexAnalysis.setRevision("0");
                reflexAnalysis.setSysUserId(sysUserId);
                reflexAnalysis.setAnalysisType("MANUAL");
                if (representative != null && representative.getTypeOfSample() != null) {
                    reflexAnalysis.setSampleTypeName(representative.getTypeOfSample().getDescription());
                }
                String newAnalysisId = analysisService.insert(reflexAnalysis);
                attachReflexProvenance(newAnalysisId, rule, sysUserId);
                existingTestIds.add(reflexTestId);
                queued++;
            }
        }
        if (queued > 0) {
            LogEvent.logInfo(this.getClass().getName(), "evaluateReflexesEagerly",
                    "Vector eager reflex: queued " + queued + " action analyses on pool " + originalPool.getId());
        }
    }

    /**
     * Returns the reflex rule label if this analysis was created by a reflex rule
     * (has an internal note starting with "reflex:"), or null for original tests.
     */
    private String extractReflexRuleLabel(Analysis analysis) {
        if (analysis == null || analysis.getId() == null) {
            return null;
        }
        try {
            String notes = noteService.getNotesAsString(analysis, false, false, "|",
                    new org.openelisglobal.note.service.NoteServiceImpl.NoteType[] {
                            org.openelisglobal.note.service.NoteServiceImpl.NoteType.INTERNAL },
                    false);
            if (notes != null) {
                for (String note : notes.split("\\|")) {
                    String trimmed = note.trim();
                    if (trimmed.startsWith("reflex:")) {
                        return trimmed.substring("reflex:".length()).trim();
                    }
                }
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    /**
     * Stamp the reflex source onto the new Analysis as an internal Note so the
     * audit trail can show "added by &lt;rule&gt;" instead of generic "reflex". The
     * convention "reflex:&lt;ruleLabel&gt;" is parseable by reporting code.
     */
    private void attachReflexProvenance(String analysisId, ReflexRule rule, String sysUserId) {
        if (analysisId == null || rule == null) {
            return;
        }
        try {
            Note note = new Note();
            note.setReferenceId(analysisId);
            note.setReferenceTableId(AnalysisServiceImpl.getTableReferenceId());
            note.setNoteType(NoteType.INTERNAL.getDBCode());
            note.setSubject("V-03 reflex provenance");
            String ruleLabel = rule.getRuleName() != null ? rule.getRuleName() : ("rule#" + rule.getId());
            note.setText("reflex:" + ruleLabel);
            note.setSysUserId(sysUserId);
            noteService.insert(note);
        } catch (RuntimeException e) {
            LogEvent.logError(this.getClass().getName(), "attachReflexProvenance", e.getMessage());
        }
    }

    private boolean conditionMatches(ReflexRuleCondition condition, List<String> values) {
        NumericRelationOptions relation = condition.getRelation();
        String target = condition.getValue();
        if (relation == null) {
            return true;
        }
        for (String v : values) {
            switch (relation) {
            case OUTSIDE_NORMAL_RANGE:
                if (v != null && !v.isBlank()) {
                    return true;
                }
                break;
            case EQUALS:
                if (target != null && target.equalsIgnoreCase(v)) {
                    return true;
                }
                break;
            case NOT_EQUALS:
                if (target != null && !target.equalsIgnoreCase(v)) {
                    return true;
                }
                break;
            default:
                break;
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

    private static String resolveResultText(Result result) {
        if (result == null) {
            return null;
        }
        String text = result.getValue();
        if (org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType
                .isDictionaryVariant(result.getResultType())) {
            try {
                org.openelisglobal.dictionary.valueholder.Dictionary d = SpringContext
                        .getBean(org.openelisglobal.dictionary.service.DictionaryService.class)
                        .getDictionaryById(result.getValue());
                if (d != null) {
                    text = d.getLocalizedName();
                }
            } catch (RuntimeException ignored) {
            }
        }
        return text;
    }

}
