package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingResultDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingRevisionDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingTestDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingResultPK;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTestPK;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerSiteBindingServiceImpl implements AnalyzerSiteBindingService {

    private static final String AUDIT_TABLE = "analyzer_site_binding_revision";

    private final AnalyzerSiteBindingDAO bindingDAO;
    private final AnalyzerSiteBindingRevisionDAO revisionDAO;
    private final AnalyzerSiteBindingTestDAO testDAO;
    private final AnalyzerSiteBindingResultDAO resultDAO;
    private final AuditTrailService auditTrailService;
    private final TestService testService;
    private final TestResultService testResultService;

    public AnalyzerSiteBindingServiceImpl(AnalyzerSiteBindingDAO bindingDAO, AnalyzerSiteBindingRevisionDAO revisionDAO,
            AnalyzerSiteBindingTestDAO testDAO, AnalyzerSiteBindingResultDAO resultDAO,
            AuditTrailService auditTrailService, TestService testService, TestResultService testResultService) {
        this.bindingDAO = bindingDAO;
        this.revisionDAO = revisionDAO;
        this.testDAO = testDAO;
        this.resultDAO = resultDAO;
        this.auditTrailService = auditTrailService;
        this.testService = testService;
        this.testResultService = testResultService;
    }

    @Override
    @Transactional
    public AnalyzerSiteBindingSnapshot resolveInitialRevision(AnalyzerProfileBinding profileBinding,
            JsonNode portableProfile, String actor) {
        String effectiveActor = requireText(actor, "actor");
        BridgeAnalyzerProfile profile = BridgeAnalyzerProfile.from(portableProfile);
        validateProfileIdentity(profileBinding, profile);
        return bindingDAO.findByProfileBindingId(profileBinding.getId()).map(this::loadLatest)
                .orElseGet(() -> createInitial(profileBinding, profile, effectiveActor));
    }

    @Override
    @Transactional
    public AnalyzerSiteBindingSnapshot appendRevision(AnalyzerSiteBinding binding, AnalyzerSiteBindingDraft draft,
            String actor) {
        String effectiveActor = requireText(actor, "actor");
        if (binding == null || binding.getId() == null || binding.getId().isBlank()) {
            throw new IllegalArgumentException("Site binding is required");
        }
        validateDraft(draft);
        AnalyzerSiteBindingRevision current = revisionDAO.findLatestByBindingId(binding.getId()).orElse(null);
        if (current != null
                && current.getBindingFingerprint().equals(AnalyzerSiteBindingFingerprint.calculate(draft))) {
            return loadRevision(binding, current);
        }
        int revisionNumber = current == null ? 1 : current.getRevisionNumber() + 1;
        return persistRevision(binding, current, revisionNumber, draft, effectiveActor);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerSiteBindingSnapshot> findCurrentByProfileBindingId(String profileBindingId) {
        String bindingId = requireText(profileBindingId, "profile binding ID");
        return bindingDAO.findByProfileBindingId(bindingId).map(this::loadLatest);
    }

    private AnalyzerSiteBindingSnapshot createInitial(AnalyzerProfileBinding profileBinding,
            BridgeAnalyzerProfile profile, String actor) {
        AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
        binding.setProfileBinding(profileBinding);
        binding.setCreatedBy(actor);
        binding.setSysUserId(actor);
        bindingDAO.insert(binding);
        return persistRevision(binding, null, 1, unresolvedDraft(profile), actor);
    }

    private AnalyzerSiteBindingSnapshot loadLatest(AnalyzerSiteBinding binding) {
        AnalyzerSiteBindingRevision revision = revisionDAO.findLatestByBindingId(binding.getId())
                .orElseThrow(() -> new IllegalStateException("Site binding has no revision: " + binding.getId()));
        return loadRevision(binding, revision);
    }

    private AnalyzerSiteBindingSnapshot loadRevision(AnalyzerSiteBinding binding,
            AnalyzerSiteBindingRevision revision) {
        return new AnalyzerSiteBindingSnapshot(binding, revision, testDAO.findByRevisionId(revision.getId()),
                resultDAO.findByRevisionId(revision.getId()));
    }

    private AnalyzerSiteBindingSnapshot persistRevision(AnalyzerSiteBinding binding,
            AnalyzerSiteBindingRevision supersedes, int revisionNumber, AnalyzerSiteBindingDraft draft, String actor) {
        AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
        revision.setSiteBinding(binding);
        revision.setRevisionNumber(revisionNumber);
        revision.setBindingFingerprint(AnalyzerSiteBindingFingerprint.calculate(draft));
        revision.setSupersedesRevision(supersedes);
        revision.setCreatedBy(actor);
        revision.setSysUserId(actor);
        revisionDAO.insert(revision);

        List<AnalyzerSiteBindingTest> tests = draft.tests().stream()
                .sorted(Comparator.comparing(AnalyzerSiteBindingTestDraft::sourceRowKey))
                .map(row -> persistTest(revision, row)).toList();
        List<AnalyzerSiteBindingResult> results = draft.results().stream()
                .sorted(Comparator.comparing(AnalyzerSiteBindingResultDraft::sourceRowKey)
                        .thenComparing(AnalyzerSiteBindingResultDraft::rawValue))
                .map(row -> persistResult(revision, row)).toList();
        auditTrailService.saveNewHistory(revision, actor, AUDIT_TABLE);
        return new AnalyzerSiteBindingSnapshot(binding, revision, tests, results);
    }

    private AnalyzerSiteBindingTest persistTest(AnalyzerSiteBindingRevision revision,
            AnalyzerSiteBindingTestDraft row) {
        AnalyzerSiteBindingTest entity = new AnalyzerSiteBindingTest();
        entity.setId(new AnalyzerSiteBindingTestPK(revision.getId(), row.sourceRowKey()));
        entity.setSiteBindingRevision(revision);
        entity.setMappingState(row.mappingState());
        entity.setTestId(row.testId());
        testDAO.insert(entity);
        return entity;
    }

    private AnalyzerSiteBindingResult persistResult(AnalyzerSiteBindingRevision revision,
            AnalyzerSiteBindingResultDraft row) {
        AnalyzerSiteBindingResult entity = new AnalyzerSiteBindingResult();
        entity.setId(new AnalyzerSiteBindingResultPK(revision.getId(), row.sourceRowKey(), row.rawValue()));
        entity.setSiteBindingRevision(revision);
        entity.setMappingState(row.mappingState());
        entity.setTestResultId(row.testResultId());
        resultDAO.insert(entity);
        return entity;
    }

    private AnalyzerSiteBindingDraft unresolvedDraft(BridgeAnalyzerProfile profile) {
        List<AnalyzerSiteBindingTestDraft> tests = new ArrayList<>();
        List<AnalyzerSiteBindingResultDraft> results = new ArrayList<>();
        for (BridgeAnalyzerProfile.TestDefinition test : profile.testDefinitions()) {
            String sourceRowKey = requireText(test.analyzerCode(), "analyzer code");
            tests.add(new AnalyzerSiteBindingTestDraft(sourceRowKey, AnalyzerSiteBindingMappingState.UNRESOLVED, null));
            for (String value : test.resultValues()) {
                String rawValue = requireText(value, "result value");
                results.add(new AnalyzerSiteBindingResultDraft(sourceRowKey, rawValue,
                        AnalyzerSiteBindingMappingState.UNRESOLVED, null));
            }
        }
        AnalyzerSiteBindingDraft draft = new AnalyzerSiteBindingDraft(tests, results);
        validateDraft(draft);
        return draft;
    }

    private static void validateProfileIdentity(AnalyzerProfileBinding selected, BridgeAnalyzerProfile profile) {
        if (selected == null || selected.getId() == null || profile == null
                || !selected.getProfileId().equals(profile.profileId())
                || selected.getProfileRevision() != profile.revision()
                || !selected.getProfileFingerprint().equals(profile.revisionFingerprint())) {
            throw new IllegalArgumentException("Portable profile does not match the selected profile reference");
        }
    }

    private void validateDraft(AnalyzerSiteBindingDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException("Site binding draft is required");
        }
        Set<String> testRows = new HashSet<>();
        for (AnalyzerSiteBindingTestDraft row : draft.tests()) {
            String sourceRowKey = requireText(row.sourceRowKey(), "test source row");
            if (!testRows.add(sourceRowKey)) {
                throw new IllegalArgumentException("Duplicate test source row: " + sourceRowKey);
            }
            validateTarget("test row " + sourceRowKey, row.mappingState(), row.testId());
        }

        Set<ResultSourceKey> resultRows = new HashSet<>();
        for (AnalyzerSiteBindingResultDraft row : draft.results()) {
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

        Map<String, AnalyzerSiteBindingTestDraft> testsBySourceRow = draft.tests().stream()
                .collect(Collectors.toMap(AnalyzerSiteBindingTestDraft::sourceRowKey, Function.identity()));
        for (AnalyzerSiteBindingTestDraft row : draft.tests()) {
            if (row.mappingState() == AnalyzerSiteBindingMappingState.BOUND) {
                requireActiveTest(row.sourceRowKey(), row.testId());
            }
        }
        for (AnalyzerSiteBindingResultDraft row : draft.results()) {
            if (row.mappingState() == AnalyzerSiteBindingMappingState.BOUND) {
                AnalyzerSiteBindingTestDraft mappedTest = testsBySourceRow.get(row.sourceRowKey());
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
            AnalyzerSiteBindingTestDraft mappedTest, String testResultId) {
        String label = "BOUND result row " + sourceRowKey + "/" + rawValue;
        if (mappedTest == null || mappedTest.mappingState() != AnalyzerSiteBindingMappingState.BOUND) {
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

    private static void validateTarget(String label, AnalyzerSiteBindingMappingState state, String targetId) {
        if (state == null) {
            throw new IllegalArgumentException(label + " requires a mapping state");
        }
        boolean hasTarget = targetId != null && !targetId.isBlank();
        if (state == AnalyzerSiteBindingMappingState.BOUND && !hasTarget) {
            throw new IllegalArgumentException("BOUND " + label + " requires a local target");
        }
        if (state != AnalyzerSiteBindingMappingState.BOUND && hasTarget) {
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
