package org.openelisglobal.analyzer.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingRevisionDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingTestDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTestPK;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerSiteBindingServiceImpl implements AnalyzerSiteBindingService {

    private static final String AUDIT_TABLE = "analyzer_site_binding_revision";

    private final AnalyzerSiteBindingDAO bindingDAO;
    private final AnalyzerSiteBindingRevisionDAO revisionDAO;
    private final AnalyzerSiteBindingTestDAO testDAO;
    private final AuditTrailService auditTrailService;

    public AnalyzerSiteBindingServiceImpl(AnalyzerSiteBindingDAO bindingDAO, AnalyzerSiteBindingRevisionDAO revisionDAO,
            AnalyzerSiteBindingTestDAO testDAO, AuditTrailService auditTrailService) {
        this.bindingDAO = bindingDAO;
        this.revisionDAO = revisionDAO;
        this.testDAO = testDAO;
        this.auditTrailService = auditTrailService;
    }

    @Override
    @Transactional
    public AnalyzerSiteBindingSnapshot create(AnalyzerSiteBindingDraft draft, String actor) {
        String effectiveActor = requireActor(actor);
        validate(draft);

        AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
        binding.setCreatedBy(effectiveActor);
        binding.setSysUserId(effectiveActor);
        bindingDAO.insert(binding);
        return appendRevision(binding, null, 1, draft, effectiveActor);
    }

    @Override
    @Transactional
    public AnalyzerSiteBindingSnapshot revise(String bindingId, AnalyzerSiteBindingDraft draft, String actor) {
        String effectiveActor = requireActor(actor);
        validate(draft);
        String effectiveBindingId = requireText(bindingId, "bindingId is required");

        AnalyzerSiteBinding binding = bindingDAO.findByIdForUpdate(effectiveBindingId).orElseThrow(
                () -> new IllegalArgumentException("Analyzer site binding not found: " + effectiveBindingId));
        AnalyzerSiteBindingRevision current = revisionDAO.findLatestByBindingId(effectiveBindingId).orElseThrow(
                () -> new IllegalStateException("Analyzer site binding has no revision: " + effectiveBindingId));
        return appendRevision(binding, current, current.getRevisionNumber() + 1, draft, effectiveActor);
    }

    private AnalyzerSiteBindingSnapshot appendRevision(AnalyzerSiteBinding binding,
            AnalyzerSiteBindingRevision supersedes, int revisionNumber, AnalyzerSiteBindingDraft draft, String actor) {
        AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
        revision.setSiteBinding(binding);
        revision.setRevisionNumber(revisionNumber);
        revision.setBridgeProfileId(draft.bridgeProfileId());
        revision.setBridgeProfileRevision(draft.bridgeProfileRevision());
        revision.setFingerprint(AnalyzerSiteBindingFingerprint.calculate(draft));
        revision.setSupersedesRevision(supersedes);
        revision.setCreatedBy(actor);
        revision.setSysUserId(actor);
        revisionDAO.insert(revision);

        List<AnalyzerSiteBindingTest> rows = new ArrayList<>();
        draft.tests().stream().sorted(Comparator.comparing(AnalyzerSiteBindingTestDraft::sourceRowKey))
                .forEach(test -> {
                    AnalyzerSiteBindingTest row = toEntity(revision, test);
                    testDAO.insert(row);
                    rows.add(row);
                });
        auditTrailService.saveNewHistory(revision, actor, AUDIT_TABLE);
        return new AnalyzerSiteBindingSnapshot(binding, revision, rows);
    }

    private static AnalyzerSiteBindingTest toEntity(AnalyzerSiteBindingRevision revision,
            AnalyzerSiteBindingTestDraft draft) {
        AnalyzerSiteBindingTest row = new AnalyzerSiteBindingTest();
        row.setId(new AnalyzerSiteBindingTestPK(revision.getId(), draft.sourceRowKey()));
        row.setSiteBindingRevision(revision);
        row.setRawAnalyzerCode(draft.rawAnalyzerCode());
        row.setAliases(draft.aliases());
        row.setDisplayName(draft.displayName());
        row.setResultType(draft.resultType());
        row.setNormalizedSystem(draft.normalizedSystem());
        row.setNormalizedCode(draft.normalizedCode());
        row.setMappingState(draft.mappingState());
        row.setTestId(draft.testId());
        row.setComponentId(draft.componentId());
        return row;
    }

    private static void validate(AnalyzerSiteBindingDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException("site binding draft is required");
        }
        requireText(draft.bridgeProfileId(), "bridgeProfileId is required");
        if (draft.bridgeProfileRevision() < 1) {
            throw new IllegalArgumentException("bridgeProfileRevision must be positive");
        }

        Set<String> sourceRowKeys = new HashSet<>();
        for (AnalyzerSiteBindingTestDraft row : draft.tests()) {
            if (row == null) {
                throw new IllegalArgumentException("site binding test row is required");
            }
            String sourceRowKey = requireText(row.sourceRowKey(), "sourceRowKey is required");
            if (!sourceRowKeys.add(sourceRowKey)) {
                throw new IllegalArgumentException("Duplicate sourceRowKey: " + sourceRowKey);
            }
            requireText(row.rawAnalyzerCode(), "rawAnalyzerCode is required for row " + sourceRowKey);
            if (row.aliases().stream().anyMatch(alias -> alias == null || alias.isBlank())) {
                throw new IllegalArgumentException("aliases cannot be blank for row " + sourceRowKey);
            }
            if ((row.normalizedSystem() == null) != (row.normalizedCode() == null)) {
                throw new IllegalArgumentException(
                        "normalized system and code must be provided together for row " + sourceRowKey);
            }
            if (row.mappingState() == null) {
                throw new IllegalArgumentException("mappingState is required for row " + sourceRowKey);
            }
            if (row.mappingState() == AnalyzerSiteBindingTest.MappingState.BOUND && row.testId() == null) {
                throw new IllegalArgumentException("BOUND row " + sourceRowKey + " requires a local Test target");
            }
            if (row.mappingState() != AnalyzerSiteBindingTest.MappingState.BOUND
                    && (row.testId() != null || row.componentId() != null)) {
                throw new IllegalArgumentException(
                        row.mappingState() + " row " + sourceRowKey + " cannot have a local target");
            }
        }
    }

    private static String requireActor(String actor) {
        return requireText(actor, "actor is required");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
