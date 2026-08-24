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
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingConfirmationDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingConfirmation;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerSiteBindingConfirmationServiceImpl implements AnalyzerSiteBindingConfirmationService {

    private static final String AUDIT_TABLE = "analyzer_site_binding_confirmation";
    private static final String FINGERPRINT_PATTERN = "sha256:[0-9a-f]{64}";
    private static final TypeReference<List<AnalyzerSiteBindingSourceRow>> ROW_LIST = new TypeReference<>() {
    };
    private static final Comparator<AnalyzerSiteBindingSourceRow> ROW_ORDER = Comparator
            .comparing(AnalyzerSiteBindingSourceRow::sourceRowKey)
            .thenComparing(AnalyzerSiteBindingSourceRow::rawValue, Comparator.nullsFirst(String::compareTo));

    private final AnalyzerSiteBindingConfirmationDAO confirmationDAO;
    private final AuditTrailService auditTrailService;
    private final SystemUserService systemUserService;
    private final AnalyzerMappingCatalogService mappingCatalogService;
    private final ObjectMapper objectMapper;

    public AnalyzerSiteBindingConfirmationServiceImpl(AnalyzerSiteBindingConfirmationDAO confirmationDAO,
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
    public AnalyzerSiteBindingConfirmationView confirm(AnalyzerSiteBindingSnapshot candidate,
            String recognitionFingerprint, AnalyzerSiteBindingConfirmationRequest request, String actor) {
        CandidateContext context = requireCandidate(candidate, recognitionFingerprint);
        String effectiveActor = requireText(actor, "actor");
        if (request == null) {
            throw new IllegalArgumentException("Confirmation request is required");
        }
        requireMatchingFingerprint(request.baseBindingFingerprint(), context.bindingFingerprint,
                "Analyzer Type mappings changed after Verify was loaded");
        requireMatchingFingerprint(request.recognitionFingerprint(), context.recognitionFingerprint,
                "Control recognition changed after Verify was loaded");
        if (!hasCurrentCatalogBindings(candidate)) {
            throw new IllegalArgumentException("Analyzer Type mappings reference inactive or unrelated catalog values");
        }

        RowDisposition expected = expectedRows(candidate);
        List<AnalyzerSiteBindingSourceRow> confirmedRows = normalizeRows(request.confirmedRows(), "confirmed");
        List<AnalyzerSiteBindingSourceRow> excludedRows = normalizeRows(request.excludedRows(), "excluded");
        if (!confirmedRows.equals(expected.confirmed) || !excludedRows.equals(expected.excluded)) {
            throw new IllegalArgumentException("Confirmation rows must exactly match the current mapping decisions");
        }

        Optional<AnalyzerSiteBindingConfirmation> existing = confirmationDAO
                .findByRevisionId(candidate.revision().getId());
        if (existing.isPresent()) {
            return toView(existing.get(), AnalyzerSiteBindingConfirmationView.State.CURRENT);
        }

        AnalyzerSiteBindingConfirmation confirmation = new AnalyzerSiteBindingConfirmation();
        confirmation.setSiteBindingRevision(candidate.revision());
        confirmation.setProfileId(context.profile.getProfileId());
        confirmation.setProfileRevision(context.profile.getProfileRevision());
        confirmation.setProfileRevisionFingerprint(context.profileRevisionFingerprint);
        confirmation.setBindingFingerprint(context.bindingFingerprint);
        confirmation.setRecognitionFingerprint(context.recognitionFingerprint);
        confirmation.setConfirmedRowsJson(writeRows(confirmedRows));
        confirmation.setExcludedRowsJson(writeRows(excludedRows));
        confirmation.setConfirmedBy(effectiveActor);
        confirmation.setConfirmedAt(Timestamp.from(Instant.now()));
        confirmation.setSysUserId(effectiveActor);
        confirmationDAO.insert(confirmation);
        confirmation.setAuditEventId(requireText(
                auditTrailService.saveNewHistory(confirmation, effectiveActor, AUDIT_TABLE), "audit event ID"));
        confirmationDAO.update(confirmation);
        return toView(confirmation, AnalyzerSiteBindingConfirmationView.State.CURRENT);
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyzerSiteBindingConfirmationView getStatus(AnalyzerSiteBindingSnapshot candidate,
            String recognitionFingerprint) {
        CandidateContext context = requireCandidate(candidate, recognitionFingerprint);
        return confirmationDAO.findLatestByBindingId(candidate.binding().getId())
                .map(confirmation -> toView(confirmation,
                        isCurrent(candidate, context, confirmation) && hasCurrentCatalogBindings(candidate)
                                ? AnalyzerSiteBindingConfirmationView.State.CURRENT
                                : AnalyzerSiteBindingConfirmationView.State.STALE))
                .orElseGet(AnalyzerSiteBindingConfirmationView::unconfirmed);
    }

    private static CandidateContext requireCandidate(AnalyzerSiteBindingSnapshot candidate,
            String recognitionFingerprint) {
        if (candidate == null || candidate.binding() == null || candidate.binding().getId() == null
                || candidate.revision() == null || candidate.revision().getId() == null
                || candidate.binding().getProfileBinding() == null) {
            throw new IllegalArgumentException("Complete site-binding candidate is required");
        }
        String bindingFingerprint = requireFingerprint(candidate.revision().getBindingFingerprint(),
                "binding fingerprint");
        String profileRevisionFingerprint = requireFingerprint(
                candidate.binding().getProfileBinding().getProfileFingerprint(), "profile revision fingerprint");
        String effectiveRecognitionFingerprint = requireFingerprint(recognitionFingerprint, "recognition fingerprint");
        return new CandidateContext(candidate.binding().getProfileBinding(), profileRevisionFingerprint,
                bindingFingerprint, effectiveRecognitionFingerprint);
    }

    private static RowDisposition expectedRows(AnalyzerSiteBindingSnapshot candidate) {
        List<AnalyzerSiteBindingSourceRow> confirmed = new ArrayList<>();
        List<AnalyzerSiteBindingSourceRow> excluded = new ArrayList<>();
        candidate.tests().forEach(row -> addRow(row.getMappingState(),
                new AnalyzerSiteBindingSourceRow(row.getId().getSourceRowKey(), null), confirmed, excluded));
        candidate.results()
                .forEach(row -> addRow(row.getMappingState(),
                        new AnalyzerSiteBindingSourceRow(row.getId().getSourceRowKey(), row.getId().getRawValue()),
                        confirmed, excluded));
        confirmed.sort(ROW_ORDER);
        excluded.sort(ROW_ORDER);
        return new RowDisposition(List.copyOf(confirmed), List.copyOf(excluded));
    }

    private static void addRow(AnalyzerSiteBindingMappingState state, AnalyzerSiteBindingSourceRow row,
            List<AnalyzerSiteBindingSourceRow> confirmed, List<AnalyzerSiteBindingSourceRow> excluded) {
        if (state == AnalyzerSiteBindingMappingState.BOUND) {
            confirmed.add(row);
        } else if (state == AnalyzerSiteBindingMappingState.EXCLUDED) {
            excluded.add(row);
        } else {
            throw new IllegalArgumentException("Every source row must be bound or excluded before confirmation");
        }
    }

    private static List<AnalyzerSiteBindingSourceRow> normalizeRows(List<AnalyzerSiteBindingSourceRow> rows,
            String label) {
        if (rows == null) {
            throw new IllegalArgumentException(label + " rows are required");
        }
        List<AnalyzerSiteBindingSourceRow> normalized = rows.stream().sorted(ROW_ORDER).toList();
        Set<AnalyzerSiteBindingSourceRow> unique = new HashSet<>(normalized);
        if (unique.size() != normalized.size()) {
            throw new IllegalArgumentException("Duplicate " + label + " source row");
        }
        return normalized;
    }

    private static boolean isCurrent(AnalyzerSiteBindingSnapshot candidate, CandidateContext context,
            AnalyzerSiteBindingConfirmation confirmation) {
        return Objects.equals(candidate.revision().getId(), confirmation.getSiteBindingRevision().getId())
                && context.profileRevisionFingerprint.equals(confirmation.getProfileRevisionFingerprint())
                && context.bindingFingerprint.equals(confirmation.getBindingFingerprint())
                && context.recognitionFingerprint.equals(confirmation.getRecognitionFingerprint())
                && hasText(confirmation.getAuditEventId());
    }

    private boolean hasCurrentCatalogBindings(AnalyzerSiteBindingSnapshot candidate) {
        return AnalyzerSiteBindingCatalogState.load(mappingCatalogService).validate(candidate).allRowsCurrent();
    }

    private AnalyzerSiteBindingConfirmationView toView(AnalyzerSiteBindingConfirmation confirmation,
            AnalyzerSiteBindingConfirmationView.State state) {
        return new AnalyzerSiteBindingConfirmationView(state, confirmation.getProfileId(),
                confirmation.getProfileRevision(), confirmation.getBindingFingerprint(),
                confirmation.getRecognitionFingerprint(), confirmation.getConfirmedBy(),
                resolveActorDisplayName(confirmation.getConfirmedBy()), confirmation.getConfirmedAt().toInstant(),
                readRows(confirmation.getConfirmedRowsJson()), readRows(confirmation.getExcludedRowsJson()));
    }

    private String resolveActorDisplayName(String actorId) {
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

    private String writeRows(List<AnalyzerSiteBindingSourceRow> rows) {
        try {
            return objectMapper.writeValueAsString(rows);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot record analyzer mapping confirmation rows", e);
        }
    }

    private List<AnalyzerSiteBindingSourceRow> readRows(String json) {
        try {
            return normalizeRows(objectMapper.readValue(json, ROW_LIST), "stored");
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new IllegalStateException("Stored analyzer mapping confirmation rows are invalid", e);
        }
    }

    private static void requireMatchingFingerprint(String submitted, String expected, String message) {
        if (!expected.equals(submitted)) {
            throw new IllegalArgumentException(message);
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

    private record CandidateContext(AnalyzerProfileBinding profile, String profileRevisionFingerprint,
            String bindingFingerprint, String recognitionFingerprint) {
    }

    private record RowDisposition(List<AnalyzerSiteBindingSourceRow> confirmed,
            List<AnalyzerSiteBindingSourceRow> excluded) {
    }
}
