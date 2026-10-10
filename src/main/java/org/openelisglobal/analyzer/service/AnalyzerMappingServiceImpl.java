package org.openelisglobal.analyzer.service;

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
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerMapping;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResultPK;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTestPK;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerMappingServiceImpl implements AnalyzerMappingService {

    private static final String AUDIT_TABLE = "analyzer_mapping";

    private final AnalyzerMappingDAO mappingDAO;
    private final AnalyzerMappingTestDAO testDAO;
    private final AnalyzerMappingResultDAO resultDAO;
    private final AuditTrailService auditTrailService;
    private final TestService testService;
    private final TestResultService testResultService;
    private final TestResultComponentService componentService;
    private final AnalyzerMappingDefaults mappingDefaults;
    private final BridgeProfileCatalogService profileCatalogService;

    public AnalyzerMappingServiceImpl(AnalyzerMappingDAO mappingDAO, AnalyzerMappingTestDAO testDAO,
            AnalyzerMappingResultDAO resultDAO, AuditTrailService auditTrailService, TestService testService,
            TestResultService testResultService, TestResultComponentService componentService,
            AnalyzerMappingDefaults mappingDefaults, BridgeProfileCatalogService profileCatalogService) {
        this.mappingDAO = mappingDAO;
        this.testDAO = testDAO;
        this.resultDAO = resultDAO;
        this.auditTrailService = auditTrailService;
        this.testService = testService;
        this.testResultService = testResultService;
        this.componentService = componentService;
        this.mappingDefaults = mappingDefaults;
        this.profileCatalogService = profileCatalogService;
    }

    @Override
    @Transactional
    public AnalyzerMappingSnapshot assignProfile(Analyzer analyzer, String profileId, int profileRevision,
            String actor) {
        String effectiveActor = requireText(actor, "actor");
        if (analyzer == null || analyzer.getId() == null) {
            throw new IllegalArgumentException("A saved analyzer is required");
        }
        String normalizedProfileId = requireText(profileId, "Profile ID");
        if (profileRevision < 1) {
            throw new IllegalArgumentException("Profile revision must be at least 1");
        }
        BridgeAnalyzerProfile profile = findProfile(normalizedProfileId, profileRevision);
        if (!"ACTIVE".equals(profile.status())) {
            throw new IllegalArgumentException(profileLabel(normalizedProfileId, profileRevision) + " is not active");
        }
        AnalyzerMappingDraft draft = mappingDefaults.resolve(profile);
        validateDraft(draft);
        Optional<AnalyzerMapping> latest = mappingDAO.findLatestByAnalyzerId(analyzer.getId());
        return persistRevision(analyzer, latest.orElse(null), latest.map(m -> m.getRevisionNumber() + 1).orElse(1),
                profile.profileId(), profile.revision(), profile.revisionFingerprint(), draft, effectiveActor);
    }

    @Override
    @Transactional
    public AnalyzerMappingSnapshot appendRevision(Analyzer analyzer, AnalyzerMappingDraft draft, String actor) {
        String effectiveActor = requireText(actor, "actor");
        if (analyzer == null || analyzer.getId() == null) {
            throw new IllegalArgumentException("A saved analyzer is required");
        }
        validateDraft(draft);
        AnalyzerMapping current = mappingDAO.findLatestByAnalyzerId(analyzer.getId())
                .orElseThrow(() -> new IllegalStateException("Analyzer has no mapping: " + analyzer.getId()));
        if (current.getMappingFingerprint().equals(AnalyzerMappingFingerprint.calculate(draft))) {
            return load(current);
        }
        return persistRevision(analyzer, current, current.getRevisionNumber() + 1, current.getProfileId(),
                current.getProfileRevision(), current.getProfileFingerprint(), draft, effectiveActor);
    }

    @Override
    @Transactional
    public AnalyzerMappingSnapshot adoptRevision(Analyzer analyzer, int profileRevision, AnalyzerMappingDraft draft,
            String actor) {
        String effectiveActor = requireText(actor, "actor");
        if (analyzer == null || analyzer.getId() == null) {
            throw new IllegalArgumentException("A saved analyzer is required");
        }
        validateDraft(draft);
        AnalyzerMapping current = mappingDAO.findLatestByAnalyzerId(analyzer.getId())
                .orElseThrow(() -> new IllegalStateException("Analyzer has no mapping: " + analyzer.getId()));
        if (profileRevision <= current.getProfileRevision()) {
            throw new IllegalArgumentException("Adoption moves to a newer revision of " + current.getProfileId());
        }
        BridgeAnalyzerProfile profile = findProfile(current.getProfileId(), profileRevision);
        if (!"ACTIVE".equals(profile.status())) {
            throw new IllegalArgumentException(
                    profileLabel(current.getProfileId(), profileRevision) + " is not active");
        }
        return persistRevision(analyzer, current, current.getRevisionNumber() + 1, profile.profileId(),
                profile.revision(), profile.revisionFingerprint(), draft, effectiveActor);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerMappingSnapshot> findLatestByAnalyzerId(String analyzerId) {
        return mappingDAO.findLatestByAnalyzerId(requireText(analyzerId, "analyzer ID")).map(this::load);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerMappingSnapshot> findById(String mappingId) {
        return mappingDAO.get(requireText(mappingId, "mapping ID")).map(this::load);
    }

    private BridgeAnalyzerProfile findProfile(String profileId, int profileRevision) {
        return profileCatalogService.getCatalog().profiles().stream()
                .map(revision -> BridgeAnalyzerProfile.from(revision.profile()))
                .filter(profile -> profileId.equals(profile.profileId()) && profileRevision == profile.revision())
                .findFirst().orElseThrow(() -> new IllegalArgumentException(
                        profileLabel(profileId, profileRevision) + " was not found"));
    }

    private static String profileLabel(String profileId, int profileRevision) {
        return "Bridge profile " + profileId + " revision " + profileRevision;
    }

    private AnalyzerMappingSnapshot load(AnalyzerMapping mapping) {
        return new AnalyzerMappingSnapshot(mapping, testDAO.findByMappingId(mapping.getId()),
                resultDAO.findByMappingId(mapping.getId()));
    }

    private AnalyzerMappingSnapshot persistRevision(Analyzer analyzer, AnalyzerMapping supersedes, int revisionNumber,
            String profileId, int profileRevision, String profileFingerprint, AnalyzerMappingDraft draft,
            String actor) {
        AnalyzerMapping mapping = new AnalyzerMapping();
        mapping.setAnalyzer(analyzer);
        mapping.setRevisionNumber(revisionNumber);
        mapping.setProfileId(profileId);
        mapping.setProfileRevision(profileRevision);
        mapping.setProfileFingerprint(profileFingerprint);
        mapping.setMappingFingerprint(AnalyzerMappingFingerprint.calculate(draft));
        mapping.setSupersedes(supersedes);
        mapping.setCreatedBy(actor);
        mapping.setSysUserId(actor);
        mappingDAO.insert(mapping);

        List<AnalyzerMappingTest> tests = draft.tests().stream()
                .sorted(Comparator.comparing(AnalyzerMappingTestDraft::sourceRowKey)
                        .thenComparing(AnalyzerMappingTestDraft::subIdentity))
                .map(row -> persistTest(mapping, row)).toList();
        List<AnalyzerMappingResult> results = draft.results().stream()
                .sorted(Comparator.comparing(AnalyzerMappingResultDraft::sourceRowKey)
                        .thenComparing(AnalyzerMappingResultDraft::subIdentity)
                        .thenComparing(AnalyzerMappingResultDraft::rawValue))
                .map(row -> persistResult(mapping, row)).toList();
        auditTrailService.saveNewHistory(mapping, actor, AUDIT_TABLE);
        return new AnalyzerMappingSnapshot(mapping, tests, results);
    }

    private AnalyzerMappingTest persistTest(AnalyzerMapping mapping, AnalyzerMappingTestDraft row) {
        AnalyzerMappingTest entity = new AnalyzerMappingTest();
        entity.setId(new AnalyzerMappingTestPK(mapping.getId(), row.sourceRowKey(), row.subIdentity()));
        entity.setMapping(mapping);
        entity.setMappingState(row.mappingState());
        entity.setOrigin(row.origin());
        entity.setTestId(row.testId());
        entity.setComponentId(row.componentId());
        entity.setCallComponentId(row.callComponentId());
        entity.setUnresolvedReason(row.unresolvedReason());
        testDAO.insert(entity);
        return entity;
    }

    private AnalyzerMappingResult persistResult(AnalyzerMapping mapping, AnalyzerMappingResultDraft row) {
        AnalyzerMappingResult entity = new AnalyzerMappingResult();
        entity.setId(
                new AnalyzerMappingResultPK(mapping.getId(), row.sourceRowKey(), row.subIdentity(), row.rawValue()));
        entity.setMapping(mapping);
        entity.setMappingState(row.mappingState());
        entity.setOrigin(row.origin());
        entity.setTestResultId(row.testResultId());
        entity.setUnresolvedReason(row.unresolvedReason());
        resultDAO.insert(entity);
        return entity;
    }

    private void validateDraft(AnalyzerMappingDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException("Mapping draft is required");
        }
        Set<AnalyzerMappingRowKey> testRows = new HashSet<>();
        for (AnalyzerMappingTestDraft row : draft.tests()) {
            String sourceRowKey = label(requireText(row.sourceRowKey(), "test source row"), row.subIdentity());
            if (!testRows.add(row.rowKey())) {
                throw new IllegalArgumentException("Duplicate test source row: " + sourceRowKey);
            }
            validateTarget("test row " + sourceRowKey, row.mappingState(), row.testId());
            if ((hasText(row.componentId()) || hasText(row.callComponentId()))
                    && row.mappingState() != AnalyzerMappingState.BOUND) {
                throw new IllegalArgumentException(
                        row.mappingState() + " test row " + sourceRowKey + " cannot have a component");
            }
        }

        Set<ResultSourceKey> resultRows = new HashSet<>();
        for (AnalyzerMappingResultDraft row : draft.results()) {
            String sourceRowKey = label(requireText(row.sourceRowKey(), "result source row"), row.subIdentity());
            String rawValue = requireText(row.rawValue(), "result raw value");
            if (!testRows.contains(row.rowKey())) {
                throw new IllegalArgumentException("Result row has no matching test row: " + sourceRowKey);
            }
            if (!resultRows.add(new ResultSourceKey(row.rowKey(), rawValue))) {
                throw new IllegalArgumentException("Duplicate result source row: " + sourceRowKey + "/" + rawValue);
            }
            validateTarget("result row " + sourceRowKey + "/" + rawValue, row.mappingState(), row.testResultId());
        }

        Map<AnalyzerMappingRowKey, AnalyzerMappingTestDraft> testsBySourceRow = draft.tests().stream()
                .collect(Collectors.toMap(AnalyzerMappingTestDraft::rowKey, Function.identity()));
        for (AnalyzerMappingTestDraft row : draft.tests()) {
            if (row.mappingState() == AnalyzerMappingState.BOUND) {
                String sourceRowKey = label(row.sourceRowKey(), row.subIdentity());
                requireActiveTest(sourceRowKey, row.testId());
                if (hasText(row.componentId())) {
                    requireComponentOfTest(sourceRowKey, row.testId(), row.componentId());
                }
                if (hasText(row.callComponentId())) {
                    requireComponentOfTest(sourceRowKey, row.testId(), row.callComponentId());
                }
            }
        }
        for (AnalyzerMappingResultDraft row : draft.results()) {
            if (row.mappingState() == AnalyzerMappingState.BOUND) {
                AnalyzerMappingTestDraft mappedTest = testsBySourceRow.get(row.rowKey());
                requireActiveOwnedResultOption(label(row.sourceRowKey(), row.subIdentity()), row.rawValue(), mappedTest,
                        row.testResultId());
            }
        }
    }

    private void requireActiveTest(String sourceRowKey, String testId) {
        Test test = testService.get(testId);
        if (test == null || !test.isActive()) {
            throw new IllegalArgumentException("BOUND test row " + sourceRowKey + " must reference an active Test");
        }
    }

    private void requireComponentOfTest(String sourceRowKey, String testId, String componentId) {
        boolean belongs = componentService.getComponentsByTestId(testId).stream()
                .anyMatch(component -> componentId.equals(component.getId()));
        if (!belongs) {
            throw new IllegalArgumentException("Test row " + sourceRowKey + " must name a component of Test " + testId);
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

    private record ResultSourceKey(AnalyzerMappingRowKey record, String rawValue) {
    }

    private static String label(String sourceRowKey, String subIdentity) {
        return new AnalyzerMappingRowKey(sourceRowKey, subIdentity).label();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
