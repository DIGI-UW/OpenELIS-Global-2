package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.dao.AnalyzerMappingConfirmationDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingConfirmation;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfilePin;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.common.log.LogEvent;
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
    private final BridgeProfileCatalogService profileCatalogService;
    private final ObjectMapper objectMapper;
    private final BridgeAnalyzerConnectionClient connectionClient;

    public AnalyzerMappingConfirmationServiceImpl(AnalyzerMappingConfirmationDAO confirmationDAO,
            AuditTrailService auditTrailService, SystemUserService systemUserService,
            AnalyzerMappingCatalogService mappingCatalogService, BridgeProfileCatalogService profileCatalogService,
            BridgeAnalyzerConnectionClient connectionClient) {
        this.confirmationDAO = confirmationDAO;
        this.auditTrailService = auditTrailService;
        this.systemUserService = systemUserService;
        this.mappingCatalogService = mappingCatalogService;
        this.profileCatalogService = profileCatalogService;
        this.connectionClient = connectionClient;
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

        Optional<ServedReading> served = servedIfReadable(context.profile, candidate.mapping().getAnalyzer());
        Optional<AnalyzerMappingConfirmation> existing = confirmationDAO.findByMappingId(candidate.mapping().getId());
        if (existing.isPresent()) {
            ServedReading reading = served.orElseThrow(() -> new AnalyzerRequestException(
                    "analyzer.mapping.error.profileUnavailable",
                    "The analyzer profile could not be read. Retry confirmation when the Bridge is available."));
            return confirmAgain(existing.get(), context, reading, effectiveActor);
        }

        AnalyzerMappingConfirmation confirmation = new AnalyzerMappingConfirmation();
        // Without a served profile, start tracking readings on the first readable
        // delivery.
        served.ifPresent(reading -> keep(confirmation, reading));
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
        return confirmationDAO.findLatestByAnalyzerId(candidate.mapping().getAnalyzer().getId()).map(confirmation -> {
            AnalyzerConfirmationReading reading = currentReading(context.profile, confirmation,
                    candidate.mapping().getAnalyzer());
            boolean current = isCurrent(candidate, context, confirmation) && hasCurrentCatalogBindings(candidate)
                    && hasExactSavedRows(candidate, confirmation) && reading.isUnchanged();
            return withReading(toView(confirmation, current ? AnalyzerMappingConfirmationView.State.CURRENT
                    : AnalyzerMappingConfirmationView.State.STALE), reading);
        }).orElseGet(AnalyzerMappingConfirmationView::unconfirmed);
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
        AnalyzerConfirmationReading reading = currentReading(context.profile, confirmation,
                candidate.mapping().getAnalyzer());
        boolean mappingsCurrent = durableCandidateMatch && hasCurrentCatalogBindings(candidate)
                && hasExactSavedRows(candidate, confirmation) && reading.changedRows().isEmpty();
        boolean recognitionCurrent = durableCandidateMatch && !reading.typeChanged()
                && context.recognitionFingerprint.equals(confirmation.getRecognitionFingerprint());
        return new AnalyzerMappingVerificationAssessment(mappingsCurrent, recognitionCurrent, confirmation);
    }

    @Override
    @Transactional
    public AnalyzerConfirmationReading readingOnDelivery(AnalyzerMappingSnapshot binding, String deliveredProfileId,
            int deliveredRevision, String deliveredProfileFingerprint) {
        Optional<AnalyzerMappingConfirmation> found = confirmationDAO.findByMappingId(binding.mapping().getId());
        if (found.isEmpty()) {
            return AnalyzerConfirmationReading.unchanged();
        }
        AnalyzerMappingConfirmation confirmation = found.get();
        String readFrom = confirmation.getReadingsFingerprint();
        JsonNode overrides;
        try {
            overrides = connectionOverrides(binding.mapping().getAnalyzer());
        } catch (BridgeAnalyzerConnectionException exception) {
            return AnalyzerConfirmationReading.typeReadingChanged();
        }
        if (readFrom != null && readFrom.equals(deliveredProfileFingerprint)) {
            if (!savedOverrides(readTree(confirmation.getTypeReadingJson())).equals(overrides)) {
                return AnalyzerConfirmationReading.typeReadingChanged();
            }
            return AnalyzerConfirmationReading.unchanged().arrivingAs(new AnalyzerArrivalReading(
                    readTree(confirmation.getTypeReadingJson()), readRowReadings(confirmation.getRowReadingsJson())));
        }
        // A delivery that does not say what it was parsed with is compared with what
        // the
        // Bridge serves, and how it arrived stays unknown.
        ServedReading served;
        try {
            served = withConnectionReading(
                    served(new AnalyzerProfilePin(deliveredProfileId, deliveredRevision, deliveredProfileFingerprint)),
                    overrides);
        } catch (BridgeProfileCatalogException | IllegalArgumentException exception) {
            LogEvent.logWarn(getClass().getSimpleName(), "readingOnDelivery",
                    "The served profile could not be read: " + exception.getMessage());
            return AnalyzerConfirmationReading.typeReadingChanged();
        }
        // Compare the profile that actually parsed this delivery, never a newer edit
        // served under the same revision number.
        if (deliveredProfileFingerprint != null && !served.fingerprint().equals(deliveredProfileFingerprint)) {
            return AnalyzerConfirmationReading.typeReadingChanged();
        }
        AnalyzerArrivalReading arrival = served.fingerprint().equals(deliveredProfileFingerprint)
                ? new AnalyzerArrivalReading(served.type(), served.rows())
                : null;
        if (readFrom == null) {
            // No historical reading exists to compare. Start tracking the served reading
            // from this delivery; a whole-file hash cannot establish a semantic change.
            keep(confirmation, served);
            confirmationDAO.update(confirmation);
            return AnalyzerConfirmationReading.unchanged().arrivingAs(arrival);
        }
        AnalyzerConfirmationReading reading = compare(confirmation, served);
        if (reading.isUnchanged()) {
            confirmation.setReadingsFingerprint(served.fingerprint());
            confirmationDAO.update(confirmation);
        }
        return reading.arrivingAs(arrival);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean readsAsConfirmed(String mappingId, String code, String subIdentity, String arrivalReading) {
        Optional<AnalyzerArrivalReading.RecordReading> arrived = AnalyzerArrivalReading.parseRecord(arrivalReading);
        Optional<AnalyzerMappingConfirmation> found = confirmationDAO.findByMappingId(mappingId);
        if (arrived.isEmpty() || found.isEmpty() || found.get().getReadingsFingerprint() == null) {
            return false;
        }
        AnalyzerMappingConfirmation confirmation = found.get();
        if (!AnalyzerRecordReading.parsing(arrived.get().type())
                .equals(AnalyzerRecordReading.parsing(readTree(confirmation.getTypeReadingJson())))) {
            return false;
        }
        String sub = subIdentity == null ? "" : subIdentity;
        Map<AnalyzerMappingSourceRow, JsonNode> confirmed = readRowReadings(confirmation.getRowReadingsJson());
        JsonNode record = confirmed.get(new AnalyzerMappingSourceRow(code, null, sub));
        if (record == null) {
            // A code undeclared at arrival and confirmation has no profile reading on
            // either side. It may recover only after an explicit confirmed decision,
            // including exclusion so the held row can leave review.
            AnalyzerMappingSourceRow row = new AnalyzerMappingSourceRow(code, null, sub);
            return arrived.get().byValue().isEmpty()
                    && (readRowsForView(confirmation.getConfirmedRowsJson()).contains(row)
                            || readRowsForView(confirmation.getExcludedRowsJson()).contains(row));
        }
        if (!record.equals(arrived.get().byValue().get(null))) {
            return false;
        }
        return arrived.get().byValue().entrySet().stream().filter(entry -> entry.getKey() != null).allMatch(entry -> {
            JsonNode now = confirmed.get(new AnalyzerMappingSourceRow(code, entry.getKey(), sub));
            return entry.getValue().equals(now);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean standsAsServed(String mappingId, BridgeProfileCatalog.ProfileRevision served) {
        if (served == null) {
            return false;
        }
        ServedReading reading = servedFrom(served);
        try {
            return confirmationDAO
                    .findByMappingId(
                            mappingId)
                    .filter(confirmation -> hasText(confirmation.getAuditEventId())
                            && readingAgainst(withConnectionReading(reading,
                                    connectionOverrides(confirmation.getMapping().getAnalyzer())), confirmation)
                                    .isUnchanged())
                    .isPresent();
        } catch (BridgeAnalyzerConnectionException exception) {
            return false;
        }
    }

    /**
     * Confirms an analyzer's mapping again: the readings it stands on become the
     * served ones.
     */
    private AnalyzerMappingConfirmationView confirmAgain(AnalyzerMappingConfirmation confirmation,
            CandidateContext context, ServedReading served, String actor) {
        boolean sameRecognition = context.recognitionFingerprint.equals(confirmation.getRecognitionFingerprint());
        if (confirmation.getReadingsFingerprint() != null && sameRecognition
                && compare(confirmation, served).isUnchanged()) {
            if (!served.fingerprint().equals(confirmation.getReadingsFingerprint())) {
                confirmation.setReadingsFingerprint(served.fingerprint());
                confirmationDAO.update(confirmation);
            }
            return toView(confirmation, AnalyzerMappingConfirmationView.State.CURRENT);
        }
        AnalyzerMappingConfirmation before = copyOf(confirmation);
        keep(confirmation, served);
        confirmation.setRecognitionFingerprint(context.recognitionFingerprint);
        confirmation.setConfirmedBy(actor);
        confirmation.setConfirmedAt(Timestamp.from(Instant.now()));
        confirmation.setSysUserId(actor);
        confirmationDAO.update(confirmation);
        auditTrailService.saveHistory(confirmation, before, actor, "U", AUDIT_TABLE);
        return toView(confirmation, AnalyzerMappingConfirmationView.State.CURRENT);
    }

    /**
     * The served profile against a confirmation, without writing; an unreadable
     * profile changes nothing, so a Bridge outage never undoes a verification.
     */
    private AnalyzerConfirmationReading currentReading(AnalyzerProfilePin pin, AnalyzerMappingConfirmation confirmation,
            Analyzer analyzer) {
        try {
            return readingAgainst(withConnectionReading(served(pin), connectionOverrides(analyzer)), confirmation);
        } catch (BridgeProfileCatalogException | BridgeAnalyzerConnectionException
                | IllegalArgumentException exception) {
            return AnalyzerConfirmationReading.unchanged();
        }
    }

    private AnalyzerConfirmationReading readingAgainst(ServedReading served, AnalyzerMappingConfirmation confirmation) {
        if (confirmation.getReadingsFingerprint() == null) {
            return AnalyzerConfirmationReading.unchanged();
        }
        if (served.fingerprint().equals(confirmation.getReadingsFingerprint())
                && savedOverrides(served.type()).equals(savedOverrides(readTree(confirmation.getTypeReadingJson())))) {
            return AnalyzerConfirmationReading.unchanged();
        }
        return compare(confirmation, served);
    }

    private AnalyzerConfirmationReading compare(AnalyzerMappingConfirmation confirmation, ServedReading served) {
        boolean typeChanged = !readTree(confirmation.getTypeReadingJson()).equals(served.type());
        Map<AnalyzerMappingSourceRow, JsonNode> stored = readRowReadings(confirmation.getRowReadingsJson());
        Set<AnalyzerMappingSourceRow> changed = readRowsForView(confirmation.getConfirmedRowsJson()).stream()
                .map(AnalyzerMappingConfirmationServiceImpl::key)
                .filter(row -> !Objects.equals(stored.get(row), served.rows().get(row))).collect(Collectors.toSet());
        return new AnalyzerConfirmationReading(typeChanged, changed);
    }

    private Optional<ServedReading> servedIfReadable(AnalyzerProfilePin pin, Analyzer analyzer) {
        try {
            return Optional.of(withConnectionReading(served(pin), connectionOverrides(analyzer)));
        } catch (BridgeProfileCatalogException | BridgeAnalyzerConnectionException
                | IllegalArgumentException exception) {
            LogEvent.logWarn(getClass().getSimpleName(), "servedIfReadable",
                    "The served profile could not be read: " + exception.getMessage());
            return Optional.empty();
        }
    }

    private JsonNode connectionOverrides(Analyzer analyzer) {
        ObjectNode overrides = objectMapper.createObjectNode();
        if (analyzer == null || !hasText(analyzer.getBridgeConnectionId())) {
            return overrides;
        }
        ObjectNode connection = connectionClient.getConnection(analyzer.getBridgeConnectionId());
        if (connection == null) {
            throw new BridgeAnalyzerConnectionException("analyzer.bridge.connection.unavailable");
        }
        ObjectNode codes = objectMapper.createObjectNode();
        connection.path("values").path("codeOverrides").fields()
                .forEachRemaining(entry -> codes.put(entry.getKey(), entry.getValue().asText().trim()));
        if (!codes.isEmpty()) {
            overrides.set("connection.codeOverrides", codes);
        }
        JsonNode numberFormat = connection.path("values").path("numberFormat");
        if (numberFormat.isTextual()) {
            overrides.set("connection.numberFormat", numberFormat);
        }
        return overrides;
    }

    private JsonNode savedOverrides(JsonNode type) {
        ObjectNode overrides = objectMapper.createObjectNode();
        for (String key : List.of("connection.codeOverrides", "connection.numberFormat")) {
            if (type.has(key)) {
                overrides.set(key, type.get(key));
            }
        }
        return overrides;
    }

    private ServedReading withConnectionReading(ServedReading served, JsonNode overrides) {
        ObjectNode type = (ObjectNode) served.type().deepCopy();
        overrides.fields().forEachRemaining(entry -> type.set(entry.getKey(), entry.getValue()));
        return new ServedReading(served.fingerprint(), served.rows(), type);
    }

    private ServedReading served(AnalyzerProfilePin pin) {
        BridgeProfileCatalog.ProfileRevision revision = profileCatalogService.getProfile(pin.getProfileId(),
                pin.getProfileRevision());
        if (revision == null) {
            throw new IllegalArgumentException(
                    "The Bridge does not serve " + pin.getProfileId() + " revision " + pin.getProfileRevision());
        }
        return servedFrom(revision);
    }

    private ServedReading servedFrom(BridgeProfileCatalog.ProfileRevision revision) {
        BridgeAnalyzerProfile profile = BridgeAnalyzerProfile.from(revision.profile());
        Map<AnalyzerMappingSourceRow, JsonNode> rows = new LinkedHashMap<>();
        AnalyzerRecordReading.rowReadings(profile)
                .forEach((row, reading) -> rows.put(key(row), objectMapper.valueToTree(reading)));
        return new ServedReading(profile.revisionFingerprint(), rows,
                objectMapper.valueToTree(AnalyzerRecordReading.typeReading(profile)));
    }

    private void keep(AnalyzerMappingConfirmation confirmation, ServedReading served) {
        ArrayNode rows = objectMapper.createArrayNode();
        served.rows().forEach((row, reading) -> {
            ObjectNode entry = rows.addObject().put("code", row.sourceRowKey()).put("subIdentity", row.subIdentity());
            if (row.rawValue() != null) {
                entry.put("value", row.rawValue());
            }
            entry.set("reading", reading);
        });
        confirmation.setReadingsFingerprint(served.fingerprint());
        confirmation.setRowReadingsJson(rows.toString());
        confirmation.setTypeReadingJson(served.type().toString());
    }

    private Map<AnalyzerMappingSourceRow, JsonNode> readRowReadings(String json) {
        Map<AnalyzerMappingSourceRow, JsonNode> rows = new LinkedHashMap<>();
        readTree(json).forEach(entry -> rows.put(new AnalyzerMappingSourceRow(entry.path("code").asText(),
                entry.hasNonNull("value") ? entry.path("value").asText() : null, entry.path("subIdentity").asText("")),
                entry.path("reading")));
        return rows;
    }

    private JsonNode readTree(String json) {
        try {
            return json == null ? objectMapper.missingNode() : objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            return objectMapper.missingNode();
        }
    }

    private static AnalyzerMappingSourceRow key(AnalyzerMappingSourceRow row) {
        return new AnalyzerMappingSourceRow(row.sourceRowKey(), row.rawValue(),
                row.subIdentity() == null ? "" : row.subIdentity());
    }

    private static AnalyzerMappingConfirmation copyOf(AnalyzerMappingConfirmation confirmation) {
        AnalyzerMappingConfirmation copy = new AnalyzerMappingConfirmation();
        copy.setId(confirmation.getId());
        copy.setMapping(confirmation.getMapping());
        copy.setProfileId(confirmation.getProfileId());
        copy.setProfileRevision(confirmation.getProfileRevision());
        copy.setProfileRevisionFingerprint(confirmation.getProfileRevisionFingerprint());
        copy.setMappingFingerprint(confirmation.getMappingFingerprint());
        copy.setRecognitionFingerprint(confirmation.getRecognitionFingerprint());
        copy.setConfirmedRowsJson(confirmation.getConfirmedRowsJson());
        copy.setExcludedRowsJson(confirmation.getExcludedRowsJson());
        copy.setConfirmedBy(confirmation.getConfirmedBy());
        copy.setConfirmedAt(confirmation.getConfirmedAt());
        copy.setAuditEventId(confirmation.getAuditEventId());
        copy.setReadingsFingerprint(confirmation.getReadingsFingerprint());
        copy.setRowReadingsJson(confirmation.getRowReadingsJson());
        copy.setTypeReadingJson(confirmation.getTypeReadingJson());
        return copy;
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

    /**
     * The view with what the served profile now reads differently, rows in their
     * saved order.
     */
    private static AnalyzerMappingConfirmationView withReading(AnalyzerMappingConfirmationView view,
            AnalyzerConfirmationReading reading) {
        List<AnalyzerMappingSourceRow> changed = reading.changedRows().stream().sorted(ROW_ORDER).toList();
        return new AnalyzerMappingConfirmationView(view.state(), view.profileId(), view.profileRevision(),
                view.mappingFingerprint(), view.recognitionFingerprint(), view.confirmedBy(),
                view.confirmedByDisplayName(), view.confirmedAt(), view.confirmedRows(), view.excludedRows(),
                reading.typeChanged(), changed);
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

    private record ServedReading(String fingerprint, Map<AnalyzerMappingSourceRow, JsonNode> rows, JsonNode type) {
    }
}
