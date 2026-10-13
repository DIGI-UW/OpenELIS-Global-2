package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.openelisglobal.analyzer.dao.AnalyzerMappingConfirmationDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingConfirmation;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfilePin;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerMappingConfirmationServiceImpl implements AnalyzerMappingConfirmationService {

    private static final String AUDIT_TABLE = "analyzer_mapping_confirmation";
    private static final String FINGERPRINT_PATTERN = "sha256:[0-9a-f]{64}";
    private static final TypeReference<List<AnalyzerMappingSourceRow>> ROW_LIST = new TypeReference<>() {
    };
    private static final Comparator<AnalyzerMappingSourceRow> ROW_ORDER = Comparator
            .comparing(AnalyzerMappingSourceRow::sourceRowKey).thenComparing(AnalyzerMappingSourceRow::subIdentity)
            .thenComparing(AnalyzerMappingSourceRow::rawValue, Comparator.nullsFirst(String::compareTo));

    private final AnalyzerMappingConfirmationDAO confirmationDAO;
    private final AuditTrailService auditTrailService;
    private final SystemUserService systemUserService;
    private final AnalyzerMappingCatalogService mappingCatalogService;
    private final ObjectMapper objectMapper;

    public AnalyzerMappingConfirmationServiceImpl(AnalyzerMappingConfirmationDAO confirmationDAO,
            AuditTrailService auditTrailService, SystemUserService systemUserService,
            AnalyzerMappingCatalogService mappingCatalogService) {
        this.confirmationDAO = confirmationDAO;
        this.auditTrailService = auditTrailService;
        this.systemUserService = systemUserService;
        this.mappingCatalogService = mappingCatalogService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional
    public AnalyzerMappingConfirmationView confirm(AnalyzerMappingSnapshot candidate, String recognitionFingerprint,
            AnalyzerMappingConfirmationRequest request, String actor) {
        CandidateContext context = requireCandidate(candidate, recognitionFingerprint);
        String effectiveActor = requireText(actor, "actor");
        if (request == null) {
            throw new IllegalArgumentException("Confirmation request is required");
        }
        requireMatchingFingerprint(request.baseMappingFingerprint(), context.mappingFingerprint,
                "analyzer.mapping.error.changedSinceLoaded", "Analyzer mapping changed after Verify was loaded");
        requireMatchingFingerprint(request.recognitionFingerprint(), context.recognitionFingerprint,
                "analyzer.mapping.error.recognitionChanged", "Control recognition changed after Verify was loaded");
        if (!hasCurrentCatalogBindings(candidate)) {
            throw new AnalyzerRequestException("analyzer.mapping.error.catalogNotCurrent",
                    "Analyzer mapping references inactive or unrelated catalog values");
        }

        RowDisposition expected = expectedRows(candidate);
        List<AnalyzerMappingSourceRow> confirmedRows = normalizeRows(request.confirmedRows(), "confirmed");
        List<AnalyzerMappingSourceRow> excludedRows = normalizeRows(request.excludedRows(), "excluded");
        if (!confirmedRows.equals(expected.confirmed) || !excludedRows.equals(expected.excluded)) {
            throw new AnalyzerRequestException("analyzer.mapping.error.changedSinceLoaded",
                    "Confirmation rows must exactly match the current mapping decisions");
        }

        Optional<AnalyzerMappingConfirmation> existing = confirmationDAO.findByMappingId(candidate.mapping().getId());
        if (existing.isPresent()) {
            return toView(existing.get(), AnalyzerMappingConfirmationView.State.CURRENT);
        }

        AnalyzerMappingConfirmation confirmation = new AnalyzerMappingConfirmation();
        confirmation.setMapping(candidate.mapping());
        confirmation.setProfileId(context.profile.getProfileId());
        confirmation.setProfileRevision(context.profile.getProfileRevision());
        confirmation.setProfileRevisionFingerprint(context.profileRevisionFingerprint);
        confirmation.setMappingFingerprint(context.mappingFingerprint);
        confirmation.setRecognitionFingerprint(context.recognitionFingerprint);
        confirmation.setConfirmedRowsJson(writeRows(confirmedRows));
        confirmation.setExcludedRowsJson(writeRows(excludedRows));
        confirmation.setConfirmedBy(effectiveActor);
        confirmation.setConfirmedAt(Timestamp.from(Instant.now()));
        confirmation.setSysUserId(effectiveActor);
        confirmationDAO.insert(confirmation);
        String auditEventId = auditTrailService.saveNewHistory(confirmation, effectiveActor, AUDIT_TABLE);
        if (hasText(auditEventId)) {
            confirmation.setAuditEventId(auditEventId);
            confirmationDAO.update(confirmation);
            return toView(confirmation, AnalyzerMappingConfirmationView.State.CURRENT);
        }
        return toView(confirmation, AnalyzerMappingConfirmationView.State.STALE);
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyzerMappingConfirmationView getStatus(AnalyzerMappingSnapshot candidate, String recognitionFingerprint) {
        CandidateContext context = requireCandidate(candidate, recognitionFingerprint);
        return confirmationDAO.findLatestByAnalyzerId(candidate.mapping().getAnalyzer().getId())
                .map(confirmation -> toView(confirmation,
                        isCurrent(candidate, context, confirmation) && hasCurrentCatalogBindings(candidate)
                                && hasExactSavedRows(candidate, confirmation)
                                        ? AnalyzerMappingConfirmationView.State.CURRENT
                                        : AnalyzerMappingConfirmationView.State.STALE))
                .orElseGet(AnalyzerMappingConfirmationView::unconfirmed);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasMatchingConfirmation(AnalyzerMappingSnapshot candidate, String recognitionFingerprint) {
        CandidateContext context = requireCandidate(candidate, recognitionFingerprint);
        return confirmationDAO.findByMappingId(candidate.mapping().getId())
                .filter(confirmation -> isCurrent(candidate, context, confirmation)
                        && hasExactSavedRows(candidate, confirmation))
                .isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerMappingConfirmation> findForMapping(String mappingId) {
        return confirmationDAO.findByMappingId(mappingId);
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyzerMappingVerificationAssessment assessCurrent(AnalyzerMappingSnapshot candidate,
            String recognitionFingerprint) {
        CandidateContext context = requireCandidate(candidate, recognitionFingerprint);
        Optional<AnalyzerMappingConfirmation> stored = confirmationDAO.findByMappingId(candidate.mapping().getId());
        if (stored.isEmpty()) {
            return AnalyzerMappingVerificationAssessment.unconfirmed();
        }
        AnalyzerMappingConfirmation confirmation = stored.get();
        boolean durableCandidateMatch = isDurableCandidateMatch(candidate, context, confirmation);
        boolean mappingsCurrent = durableCandidateMatch && hasCurrentCatalogBindings(candidate)
                && hasExactSavedRows(candidate, confirmation);
        boolean recognitionCurrent = durableCandidateMatch
                && context.recognitionFingerprint.equals(confirmation.getRecognitionFingerprint());
        return new AnalyzerMappingVerificationAssessment(mappingsCurrent, recognitionCurrent, confirmation);
    }

    private static CandidateContext requireCandidate(AnalyzerMappingSnapshot candidate, String recognitionFingerprint) {
        if (candidate == null || candidate.mapping() == null || candidate.mapping().getId() == null
                || candidate.mapping().getAnalyzer() == null) {
            throw new IllegalArgumentException("Complete analyzer mapping candidate is required");
        }
        String mappingFingerprint = requireFingerprint(candidate.mapping().getMappingFingerprint(),
                "mapping fingerprint");
        String profileRevisionFingerprint = requireFingerprint(candidate.mapping().getProfileFingerprint(),
                "profile revision fingerprint");
        String effectiveRecognitionFingerprint = requireFingerprint(recognitionFingerprint, "recognition fingerprint");
        AnalyzerProfilePin profile = new AnalyzerProfilePin(candidate.mapping().getProfileId(),
                candidate.mapping().getProfileRevision(), candidate.mapping().getProfileFingerprint());
        return new CandidateContext(profile, profileRevisionFingerprint, mappingFingerprint,
                effectiveRecognitionFingerprint);
    }

    private static RowDisposition expectedRows(AnalyzerMappingSnapshot candidate) {
        List<AnalyzerMappingSourceRow> confirmed = new ArrayList<>();
        List<AnalyzerMappingSourceRow> excluded = new ArrayList<>();
        candidate.tests()
                .forEach(row -> addRow(row.getMappingState(),
                        new AnalyzerMappingSourceRow(row.getId().getSourceRowKey(), null, row.getId().getSubIdentity()),
                        confirmed, excluded));
        candidate.results().forEach(
                row -> addRow(row.getMappingState(), new AnalyzerMappingSourceRow(row.getId().getSourceRowKey(),
                        row.getId().getRawValue(), row.getId().getSubIdentity()), confirmed, excluded));
        confirmed.sort(ROW_ORDER);
        excluded.sort(ROW_ORDER);
        return new RowDisposition(List.copyOf(confirmed), List.copyOf(excluded));
    }

    private static void addRow(AnalyzerMappingState state, AnalyzerMappingSourceRow row,
            List<AnalyzerMappingSourceRow> confirmed, List<AnalyzerMappingSourceRow> excluded) {
        if (state == AnalyzerMappingState.BOUND) {
            confirmed.add(row);
        } else if (state == AnalyzerMappingState.EXCLUDED) {
            excluded.add(row);
        } else if (state != AnalyzerMappingState.UNRESOLVED) {
            throw new IllegalArgumentException("Every source row must have an explicit mapping state");
        }
    }

    private static List<AnalyzerMappingSourceRow> normalizeRows(List<AnalyzerMappingSourceRow> rows, String label) {
        if (rows == null) {
            throw new IllegalArgumentException(label + " rows are required");
        }
        List<AnalyzerMappingSourceRow> normalized = rows.stream().sorted(ROW_ORDER).toList();
        Set<AnalyzerMappingSourceRow> unique = new HashSet<>(normalized);
        if (unique.size() != normalized.size()) {
            throw new IllegalArgumentException("Duplicate " + label + " source row");
        }
        return normalized;
    }

    private static boolean isCurrent(AnalyzerMappingSnapshot candidate, CandidateContext context,
            AnalyzerMappingConfirmation confirmation) {
        return isDurableCandidateMatch(candidate, context, confirmation)
                && context.recognitionFingerprint.equals(confirmation.getRecognitionFingerprint());
    }

    private static boolean isDurableCandidateMatch(AnalyzerMappingSnapshot candidate, CandidateContext context,
            AnalyzerMappingConfirmation confirmation) {
        return confirmation.getMapping() != null
                && Objects.equals(candidate.mapping().getId(), confirmation.getMapping().getId())
                && Objects.equals(context.profile.getProfileId(), confirmation.getProfileId())
                && context.profile.getProfileRevision() == confirmation.getProfileRevision()
                && context.profileRevisionFingerprint.equals(confirmation.getProfileRevisionFingerprint())
                && context.mappingFingerprint.equals(confirmation.getMappingFingerprint())
                && hasText(confirmation.getConfirmedBy()) && confirmation.getConfirmedAt() != null
                && hasText(confirmation.getAuditEventId());
    }

    private boolean hasCurrentCatalogBindings(AnalyzerMappingSnapshot candidate) {
        AnalyzerMappingCatalogState.Validation catalog = AnalyzerMappingCatalogState.load(mappingCatalogService)
                .validate(candidate);
        return candidate.tests().stream()
                .allMatch(row -> row.getMappingState() == AnalyzerMappingState.UNRESOLVED
                        || catalog.isCurrentTest(AnalyzerMappingRowKey.of(row)))
                && candidate.results().stream().allMatch(row -> row.getMappingState() == AnalyzerMappingState.UNRESOLVED
                        || catalog.isCurrentResult(AnalyzerMappingRowKey.of(row), row.getId().getRawValue()));
    }

    private boolean hasExactSavedRows(AnalyzerMappingSnapshot candidate, AnalyzerMappingConfirmation confirmation) {
        try {
            RowDisposition expected = expectedRows(candidate);
            return expected.confirmed.equals(readRows(confirmation.getConfirmedRowsJson()))
                    && expected.excluded.equals(readRows(confirmation.getExcludedRowsJson()));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return false;
        }
    }

    private AnalyzerMappingConfirmationView toView(AnalyzerMappingConfirmation confirmation,
            AnalyzerMappingConfirmationView.State state) {
        return new AnalyzerMappingConfirmationView(state, confirmation.getProfileId(),
                confirmation.getProfileRevision(), confirmation.getMappingFingerprint(),
                confirmation.getRecognitionFingerprint(), confirmation.getConfirmedBy(),
                resolveActorDisplayName(confirmation.getConfirmedBy()),
                confirmation.getConfirmedAt() == null ? null : confirmation.getConfirmedAt().toInstant(),
                readRowsForView(confirmation.getConfirmedRowsJson()),
                readRowsForView(confirmation.getExcludedRowsJson()));
    }

    private String resolveActorDisplayName(String actorId) {
        if (!hasText(actorId)) {
            return null;
        }
        SystemUser actor = systemUserService.getUserById(actorId);
        if (actor == null) {
            return actorId;
        }
        String fullName = (textOrEmpty(actor.getFirstName()) + " " + textOrEmpty(actor.getLastName())).trim();
        if (!fullName.isEmpty()) {
            return fullName;
        }
        String loginName = textOrEmpty(actor.getLoginName()).trim();
        return loginName.isEmpty() ? actorId : loginName;
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String writeRows(List<AnalyzerMappingSourceRow> rows) {
        try {
            return objectMapper.writeValueAsString(rows);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot record analyzer mapping confirmation rows", e);
        }
    }

    private List<AnalyzerMappingSourceRow> readRows(String json) {
        try {
            return normalizeRows(objectMapper.readValue(json, ROW_LIST), "stored");
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new IllegalStateException("Stored analyzer mapping confirmation rows are invalid", e);
        }
    }

    private List<AnalyzerMappingSourceRow> readRowsForView(String json) {
        try {
            return readRows(json);
        } catch (IllegalStateException exception) {
            return List.of();
        }
    }

    private static void requireMatchingFingerprint(String submitted, String expected, String messageKey,
            String message) {
        if (!expected.equals(submitted)) {
            throw new AnalyzerRequestException(messageKey, message);
        }
    }

    private static String requireFingerprint(String value, String label) {
        String fingerprint = requireText(value, label);
        if (!fingerprint.matches(FINGERPRINT_PATTERN)) {
            throw new IllegalArgumentException(label + " is invalid");
        }
        return fingerprint;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record CandidateContext(AnalyzerProfilePin profile, String profileRevisionFingerprint,
            String mappingFingerprint, String recognitionFingerprint) {
    }

    private record RowDisposition(List<AnalyzerMappingSourceRow> confirmed, List<AnalyzerMappingSourceRow> excluded) {
    }
}
