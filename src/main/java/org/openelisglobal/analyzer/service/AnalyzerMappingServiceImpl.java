package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.dao.AnalyzerMappingDAO;
import org.openelisglobal.analyzer.dao.AnalyzerMappingResultDAO;
import org.openelisglobal.analyzer.dao.AnalyzerMappingTestDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerMapping;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResultPK;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTestPK;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerMappingServiceImpl implements AnalyzerMappingService {

    private static final String AUDIT_TABLE = "analyzer_site_binding_revision";

    private final AnalyzerSiteBindingDAO bindingDAO;
    private final AnalyzerMappingDAO revisionDAO;
    private final AnalyzerMappingTestDAO testDAO;
    private final AnalyzerMappingResultDAO resultDAO;
    private final AuditTrailService auditTrailService;
    private final TestService testService;
    private final TestResultService testResultService;
    private final AnalyzerMappingDefaults mappingDefaults;

    public AnalyzerMappingServiceImpl(AnalyzerSiteBindingDAO bindingDAO, AnalyzerMappingDAO revisionDAO,
            AnalyzerMappingTestDAO testDAO, AnalyzerMappingResultDAO resultDAO, AuditTrailService auditTrailService,
            TestService testService, TestResultService testResultService, AnalyzerMappingDefaults mappingDefaults) {
        this.bindingDAO = bindingDAO;
        this.revisionDAO = revisionDAO;
        this.testDAO = testDAO;
        this.resultDAO = resultDAO;
        this.auditTrailService = auditTrailService;
        this.testService = testService;
        this.testResultService = testResultService;
        this.mappingDefaults = mappingDefaults;
    }

    @Override
    @Transactional
    public AnalyzerMappingSnapshot resolveInitialRevision(AnalyzerProfileBinding profileBinding,
            JsonNode portableProfile, String actor) {
        String effectiveActor = requireText(actor, "actor");
        BridgeAnalyzerProfile profile = BridgeAnalyzerProfile.from(portableProfile);
        validateProfileIdentity(profileBinding, profile);
        return bindingDAO.findByProfileBindingId(profileBinding.getId()).map(this::loadLatest)
                .orElseGet(() -> createInitial(profileBinding, profile, effectiveActor));
    }

    @Override
    @Transactional
    public AnalyzerMappingSnapshot appendRevision(AnalyzerSiteBinding binding, AnalyzerMappingDraft draft,
            String actor) {
        String effectiveActor = requireText(actor, "actor");
        if (binding == null || binding.getId() == null || binding.getId().isBlank()) {
            throw new IllegalArgumentException("Site binding is required");
        }
        validateDraft(draft);
        AnalyzerMapping current = revisionDAO.findLatestByBindingId(binding.getId()).orElse(null);
        if (current != null && current.getBindingFingerprint().equals(AnalyzerMappingFingerprint.calculate(draft))) {
            return loadRevision(binding, current);
        }
        int revisionNumber = current == null ? 1 : current.getRevisionNumber() + 1;
        return persistRevision(binding, current, revisionNumber, draft, effectiveActor);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerMappingSnapshot> findCurrentByProfileBindingId(String profileBindingId) {
        String bindingId = requireText(profileBindingId, "profile binding ID");
        return bindingDAO.findByProfileBindingId(bindingId).map(this::loadLatest);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerMappingSnapshot> findByRevisionId(String revisionId) {
        String id = requireText(revisionId, "site binding revision ID");
        return revisionDAO.get(id).map(revision -> loadRevision(revision.getSiteBinding(), revision));
    }

    private AnalyzerMappingSnapshot createInitial(AnalyzerProfileBinding profileBinding, BridgeAnalyzerProfile profile,
            String actor) {
        AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
        binding.setProfileBinding(profileBinding);
        binding.setCreatedBy(actor);
        binding.setSysUserId(actor);
        bindingDAO.insert(binding);
        AnalyzerMappingDraft draft = mappingDefaults.resolve(profile);
        validateDraft(draft);
        return persistRevision(binding, null, 1, draft, actor);
    }

    private AnalyzerMappingSnapshot loadLatest(AnalyzerSiteBinding binding) {
        AnalyzerMapping revision = revisionDAO.findLatestByBindingId(binding.getId())
                .orElseThrow(() -> new IllegalStateException("Site binding has no revision: " + binding.getId()));
        return loadRevision(binding, revision);
    }

    private AnalyzerMappingSnapshot loadRevision(AnalyzerSiteBinding binding, AnalyzerMapping revision) {
        return new AnalyzerMappingSnapshot(binding, revision, testDAO.findByRevisionId(revision.getId()),
                resultDAO.findByRevisionId(revision.getId()));
    }

    private AnalyzerMappingSnapshot persistRevision(AnalyzerSiteBinding binding, AnalyzerMapping supersedes,
            int revisionNumber, AnalyzerMappingDraft draft, String actor) {
        AnalyzerMapping revision = new AnalyzerMapping();
        revision.setSiteBinding(binding);
        revision.setRevisionNumber(revisionNumber);
        revision.setBindingFingerprint(AnalyzerMappingFingerprint.calculate(draft));
        revision.setSupersedesRevision(supersedes);
        revision.setCreatedBy(actor);
        revision.setSysUserId(actor);
        revisionDAO.insert(revision);

        List<AnalyzerMappingTest> tests = draft.tests().stream()
                .sorted(Comparator.comparing(AnalyzerMappingTestDraft::sourceRowKey))
                .map(row -> persistTest(revision, row)).toList();
        List<AnalyzerMappingResult> results = draft.results().stream()
                .sorted(Comparator.comparing(AnalyzerMappingResultDraft::sourceRowKey)
                        .thenComparing(AnalyzerMappingResultDraft::rawValue))
                .map(row -> persistResult(revision, row)).toList();
        auditTrailService.saveNewHistory(revision, actor, AUDIT_TABLE);
        return new AnalyzerMappingSnapshot(binding, revision, tests, results);
    }

    private AnalyzerMappingTest persistTest(AnalyzerMapping revision, AnalyzerMappingTestDraft row) {
        AnalyzerMappingTest entity = new AnalyzerMappingTest();
        entity.setId(new AnalyzerMappingTestPK(revision.getId(), row.sourceRowKey()));
        entity.setSiteBindingRevision(revision);
        entity.setMappingState(row.mappingState());
        entity.setTestId(row.testId());
        testDAO.insert(entity);
        return entity;
    }

    private AnalyzerMappingResult persistResult(AnalyzerMapping revision, AnalyzerMappingResultDraft row) {
        AnalyzerMappingResult entity = new AnalyzerMappingResult();
        entity.setId(new AnalyzerMappingResultPK(revision.getId(), row.sourceRowKey(), row.rawValue()));
        entity.setSiteBindingRevision(revision);
        entity.setMappingState(row.mappingState());
        entity.setTestResultId(row.testResultId());
        resultDAO.insert(entity);
        return entity;
    }

    private static void validateProfileIdentity(AnalyzerProfileBinding selected, BridgeAnalyzerProfile profile) {
        if (selected == null || selected.getId() == null || profile == null
                || !selected.getProfileId().equals(profile.profileId())
                || selected.getProfileRevision() != profile.revision()
                || !selected.getProfileFingerprint().equals(profile.revisionFingerprint())) {
            throw new IllegalArgumentException("Portable profile does not match the selected profile reference");
        }
    }

    private void validateDraft(AnalyzerMappingDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException("Site binding draft is required");
        }
        Set<String> testRows = new HashSet<>();
        for (AnalyzerMappingTestDraft row : draft.tests()) {
            String sourceRowKey = requireText(row.sourceRowKey(), "test source row");
            if (!testRows.add(sourceRowKey)) {
                throw new IllegalArgumentException("Duplicate test source row: " + sourceRowKey);
            }
            validateTarget("test row " + sourceRowKey, row.mappingState(), row.testId());
        }

        Set<ResultSourceKey> resultRows = new HashSet<>();
        for (AnalyzerMappingResultDraft row : draft.results()) {
            String sourceRowKey = requireText(row.sourceRowKey(), "result source row");
            String rawValue = requireText(row.rawValue(), "result raw value");
            if (!testRows.contains(sourceRowKey)) {
                throw new IllegalArgumentException("Result row has no matching test row: " + sourceRowKey);
            }
            if (!resultRows.add(new ResultSourceKey(sourceRowKey, rawValue))) {
                throw new IllegalArgumentException("Duplicate result source row: " + sourceRowKey + "/" + rawValue);
            }
            validateTarget("result row " + sourceRowKey + "/" + rawValue, row.mappingState(), row.testResultId());
        }

        Map<String, AnalyzerMappingTestDraft> testsBySourceRow = draft.tests().stream()
                .collect(Collectors.toMap(AnalyzerMappingTestDraft::sourceRowKey, Function.identity()));
        for (AnalyzerMappingTestDraft row : draft.tests()) {
            if (row.mappingState() == AnalyzerMappingState.BOUND) {
                requireActiveTest(row.sourceRowKey(), row.testId());
            }
        }
        for (AnalyzerMappingResultDraft row : draft.results()) {
            if (row.mappingState() == AnalyzerMappingState.BOUND) {
                AnalyzerMappingTestDraft mappedTest = testsBySourceRow.get(row.sourceRowKey());
                requireActiveOwnedResultOption(row.sourceRowKey(), row.rawValue(), mappedTest, row.testResultId());
            }
        }
    }

    private void requireActiveTest(String sourceRowKey, String testId) {
        Test test = testService.get(testId);
        if (test == null || !test.isActive()) {
            throw new IllegalArgumentException("BOUND test row " + sourceRowKey + " must reference an active Test");
        }
    }

    private void requireActiveOwnedResultOption(String sourceRowKey, String rawValue,
            AnalyzerMappingTestDraft mappedTest, String testResultId) {
        String label = "BOUND result row " + sourceRowKey + "/" + rawValue;
        if (mappedTest == null || mappedTest.mappingState() != AnalyzerMappingState.BOUND) {
            throw new IllegalArgumentException(label + " requires a bound Test");
        }
        TestResult option = testResultService.get(testResultId);
        if (option == null || !Boolean.TRUE.equals(option.getIsActive())
                || !TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(option.getTestResultType())) {
            throw new IllegalArgumentException(label + " must reference an active Result Option");
        }
        Test owner = option.getTest();
        if (owner == null || !mappedTest.testId().equals(owner.getId())) {
            throw new IllegalArgumentException(label + " must belong to mapped Test " + mappedTest.testId());
        }
    }

    private static void validateTarget(String label, AnalyzerMappingState state, String targetId) {
        if (state == null) {
            throw new IllegalArgumentException(label + " requires a mapping state");
        }
        boolean hasTarget = targetId != null && !targetId.isBlank();
        if (state == AnalyzerMappingState.BOUND && !hasTarget) {
            throw new IllegalArgumentException("BOUND " + label + " requires a local target");
        }
        if (state != AnalyzerMappingState.BOUND && hasTarget) {
            throw new IllegalArgumentException(state + " " + label + " cannot have a local target");
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    private record ResultSourceKey(String sourceRowKey, String rawValue) {
    }
}
