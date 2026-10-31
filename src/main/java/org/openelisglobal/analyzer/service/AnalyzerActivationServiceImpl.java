package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerActivationRecord;
import org.openelisglobal.analyzer.valueholder.AnalyzerMapping;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingConfirmation;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfilePin;
import org.openelisglobal.test.service.TestSectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerActivationServiceImpl implements AnalyzerActivationService {

    private static final String PROFILE_BLOCKER = "analyzer.activation.blocker.profile";
    private static final String NAME_BLOCKER = "analyzer.activation.blocker.name";
    private static final String RETIRED_BLOCKER = "analyzer.activation.blocker.retired";
    private static final String LAB_UNIT_BLOCKER = "analyzer.activation.blocker.labUnit";
    private static final String MAPPINGS_BLOCKER = "analyzer.activation.blocker.mappings";
    private static final String CONNECTION_BLOCKER = "analyzer.activation.blocker.connection";
    private static final String BRIDGE_PROFILE_UNAVAILABLE = "analyzer.connection.readiness.profileUnavailable";
    /**
     * The Bridge could not restore the connection at start; an activate command
     * restores it.
     */
    private static final String BRIDGE_RESTORE_FAILED = "analyzer.connection.readiness.restoreFailed";
    private static final String BRIDGE_ACKNOWLEDGEMENT_BLOCKER = "analyzer.activation.blocker.bridgeAcknowledgement";

    private final AnalyzerService analyzerService;
    private final BridgeProfileCatalogService profileCatalogService;
    private final AnalyzerMappingService mappingService;
    private final AnalyzerMappingConfirmationService confirmationService;
    private final TestSectionService testSectionService;
    private final BridgeAnalyzerConnectionClient bridgeClient;
    private final AnalyzerActivationRecordService activationRecordService;
    private final Clock clock;
    private final Supplier<String> activationCommandIdSupplier;
    private final Supplier<String> deactivationCommandIdSupplier;

    @Autowired
    public AnalyzerActivationServiceImpl(AnalyzerService analyzerService,
            BridgeProfileCatalogService profileCatalogService, AnalyzerMappingService mappingService,
            AnalyzerMappingConfirmationService confirmationService, TestSectionService testSectionService,
            BridgeAnalyzerConnectionClient bridgeClient, AnalyzerActivationRecordService activationRecordService) {
        this(analyzerService, profileCatalogService, mappingService, confirmationService, testSectionService,
                bridgeClient, activationRecordService, Clock.systemUTC(), () -> UUID.randomUUID().toString(),
                () -> UUID.randomUUID().toString());
    }

    AnalyzerActivationServiceImpl(AnalyzerService analyzerService, BridgeProfileCatalogService profileCatalogService,
            AnalyzerMappingService mappingService, AnalyzerMappingConfirmationService confirmationService,
            TestSectionService testSectionService, BridgeAnalyzerConnectionClient bridgeClient,
            AnalyzerActivationRecordService activationRecordService, Clock clock,
            Supplier<String> activationCommandIdSupplier, Supplier<String> deactivationCommandIdSupplier) {
        this.analyzerService = analyzerService;
        this.profileCatalogService = profileCatalogService;
        this.mappingService = mappingService;
        this.confirmationService = confirmationService;
        this.testSectionService = testSectionService;
        this.bridgeClient = bridgeClient;
        this.activationRecordService = activationRecordService;
        this.clock = clock;
        this.activationCommandIdSupplier = activationCommandIdSupplier;
        this.deactivationCommandIdSupplier = deactivationCommandIdSupplier;
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyzerActivationResult readiness(String analyzerId) {
        Analyzer analyzer = findAnalyzer(analyzerId);
        ActivationContext context = validateActivation(analyzer);
        return context.blockers().isEmpty() ? AnalyzerActivationResult.ready(analyzer)
                : AnalyzerActivationResult.blocked(analyzer, context.blockers());
    }

    @Override
    @Transactional
    public AnalyzerActivationResult activate(String analyzerId, String actor) {
        return activateExact(lockAnalyzer(analyzerId), actor);
    }

    @Override
    @Transactional
    public AnalyzerActivationResult reactivate(String analyzerId, String actor) {
        return activateExact(lockAnalyzer(analyzerId), actor);
    }

    @Override
    @Transactional
    public AnalyzerSetupReceptionResult startSetupReception(String analyzerId, String actor) {
        Analyzer analyzer = lockAnalyzer(analyzerId);
        String exactActor = requireText(actor, "actor");
        // Routine analyzers keep their clinical lifecycle; this mode is for a new
        // instrument whose setup samples must never enter clinical acceptance.
        if (analyzer.getLastActivatedDate() != null || (analyzer.getStatus() != Analyzer.AnalyzerStatus.SETUP
                && analyzer.getStatus() != Analyzer.AnalyzerStatus.VALIDATION
                && analyzer.getStatus() != Analyzer.AnalyzerStatus.INACTIVE)) {
            return new AnalyzerSetupReceptionResult(analyzer.getId(), analyzer.getStatus(), false,
                    List.of(new AnalyzerActivationBlocker("analyzer.setup.reception.blocker.used")));
        }
        ActivationContext context = validateActivation(analyzer, false);
        if (!context.blockers().isEmpty()) {
            return new AnalyzerSetupReceptionResult(analyzer.getId(), analyzer.getStatus(), false, context.blockers());
        }
        ObjectNode acknowledgement;
        try {
            acknowledgement = bridgeClient.applyRuntimeCommand(context.connectionId(), context.configRevision(),
                    "START_SETUP", requireText(activationCommandIdSupplier.get(), "setup reception command ID"));
        } catch (BridgeAnalyzerConnectionException exception) {
            return new AnalyzerSetupReceptionResult(analyzer.getId(), analyzer.getStatus(), false,
                    List.of(connectionBlocker(exception)));
        }
        List<AnalyzerActivationBlocker> blockers = validateAcknowledgement(context, acknowledgement, "START_SETUP",
                "ACTIVE");
        if (!blockers.isEmpty()) {
            return new AnalyzerSetupReceptionResult(analyzer.getId(), analyzer.getStatus(), false, blockers);
        }
        PreviousAnalyzerState previousState = PreviousAnalyzerState.capture(analyzer);
        try {
            AnalyzerActivationRecord record = activationRecordService.retain(analyzer, context.snapshot().mapping(),
                    null, acknowledgement, "SETUP", exactActor);
            analyzer.setLatestActivationRecord(record);
            analyzer.setStatus(Analyzer.AnalyzerStatus.VALIDATION);
            analyzer.setActive(false);
            analyzer.setSysUserId(exactActor);
            analyzerService.update(analyzer);
            return new AnalyzerSetupReceptionResult(analyzer.getId(), analyzer.getStatus(), true, List.of());
        } catch (RuntimeException exception) {
            previousState.restore(analyzer);
            if (previousState.status() != Analyzer.AnalyzerStatus.VALIDATION) {
                compensateIfApplied(context, acknowledgement, "DEACTIVATE", deactivationCommandIdSupplier, exception);
            }
            throw exception;
        }
    }

    private AnalyzerActivationResult activateExact(Analyzer analyzer, String actor) {
        String exactActor = requireText(actor, "actor");
        ActivationContext context = validateActivation(analyzer);
        if (!context.blockers().isEmpty()) {
            return AnalyzerActivationResult.blocked(analyzer, context.blockers());
        }

        ObjectNode acknowledgement;
        try {
            acknowledgement = bridgeClient.applyRuntimeCommand(context.connectionId(), context.configRevision(),
                    "ACTIVATE", requireText(activationCommandIdSupplier.get(), "activation command ID"));
        } catch (BridgeAnalyzerConnectionException exception) {
            return AnalyzerActivationResult.blocked(analyzer, List.of(connectionBlocker(exception)));
        }
        List<AnalyzerActivationBlocker> acknowledgementBlockers = validateAcknowledgement(context, acknowledgement,
                "ACTIVATE", "ACTIVE");
        if (!acknowledgementBlockers.isEmpty()) {
            return AnalyzerActivationResult.blocked(analyzer, acknowledgementBlockers);
        }

        PreviousAnalyzerState previousState = PreviousAnalyzerState.capture(analyzer);
        try {
            AnalyzerActivationRecord record = activationRecordService.retain(analyzer, context.snapshot().mapping(),
                    context.confirmation(), acknowledgement, "ACTIVE", exactActor);
            analyzer.setLatestActivationRecord(record);
            analyzer.setStatus(Analyzer.AnalyzerStatus.ACTIVE);
            analyzer.setActive(true);
            analyzer.setLastActivatedDate(Date.from(clock.instant()));
            analyzer.setSysUserId(exactActor);
            analyzerService.update(analyzer);
            return AnalyzerActivationResult.activated(analyzer);
        } catch (RuntimeException exception) {
            previousState.restore(analyzer);
            // Retrying a failed restore fulfills an already-active runtime intent. A
            // local persistence failure must not switch that connection off.
            if (previousState.status() != Analyzer.AnalyzerStatus.ACTIVE && !previousState.active()) {
                compensateIfApplied(context, acknowledgement, "DEACTIVATE", deactivationCommandIdSupplier, exception);
            }
            throw exception;
        }
    }

    @Override
    @Transactional
    public AnalyzerDeactivationResult deactivate(String analyzerId, String actor) {
        Analyzer analyzer = lockAnalyzer(analyzerId);
        String exactActor = requireText(actor, "actor");
        if (analyzer.getStatus() == Analyzer.AnalyzerStatus.INACTIVE) {
            return AnalyzerDeactivationResult.deactivated(analyzer);
        }

        ConnectionReference connection;
        try {
            connection = requireConnectionReference(analyzer);
        } catch (RuntimeException exception) {
            return AnalyzerDeactivationResult.failed(analyzer, exception.getMessage());
        }
        ObjectNode acknowledgement;
        try {
            acknowledgement = bridgeClient.applyRuntimeCommand(connection.connectionId(), connection.configRevision(),
                    "DEACTIVATE", requireText(deactivationCommandIdSupplier.get(), "deactivation command ID"));
        } catch (BridgeAnalyzerConnectionException exception) {
            return AnalyzerDeactivationResult.failed(analyzer, exception.messageKey());
        }
        ActivationContext context = deactivationContext(analyzer, connection);
        List<AnalyzerActivationBlocker> blockers = validateAcknowledgement(context, acknowledgement, "DEACTIVATE",
                "INACTIVE");
        if (!blockers.isEmpty()) {
            return AnalyzerDeactivationResult.failed(analyzer, blockers.get(0).code());
        }

        PreviousAnalyzerState previousState = PreviousAnalyzerState.capture(analyzer);
        try {
            AnalyzerActivationRecord record = activationRecordService.retain(analyzer, context.snapshot().mapping(),
                    context.confirmation(), acknowledgement, "INACTIVE", exactActor);
            analyzer.setLatestActivationRecord(record);
            analyzer.setStatus(Analyzer.AnalyzerStatus.INACTIVE);
            analyzer.setActive(false);
            analyzer.setSysUserId(exactActor);
            analyzerService.update(analyzer);
            return AnalyzerDeactivationResult.deactivated(analyzer);
        } catch (RuntimeException exception) {
            previousState.restore(analyzer);
            compensateIfApplied(context, acknowledgement,
                    previousState.status() == Analyzer.AnalyzerStatus.VALIDATION ? "START_SETUP" : "ACTIVATE",
                    activationCommandIdSupplier, exception);
            throw exception;
        }
    }

    @Override
    @Transactional
    public void resetUnavailableProfile(String analyzerId, String actor) {
        Analyzer analyzer = lockAnalyzer(analyzerId);
        String exactActor = requireText(actor, "actor");
        if (analyzer.getMapping() == null) {
            return;
        }
        // Only the Bridge's own report counts: an unreachable Bridge also fails the
        // profile lookup, and a reset must not follow a passing outage.
        boolean unavailable = validateActivation(analyzer).blockers().stream()
                .anyMatch(blocker -> BRIDGE_PROFILE_UNAVAILABLE.equals(blocker.code()));
        if (!unavailable) {
            throw new AnalyzerRequestException("analyzer.reset.error.profileAvailable",
                    "The analyzer type is still available. Adopt a newer revision to move this analyzer to it.");
        }
        analyzer.setMapping(null);
        analyzer.setStatus(Analyzer.AnalyzerStatus.INACTIVE);
        analyzer.setActive(false);
        analyzer.setSysUserId(exactActor);
        analyzerService.update(analyzer);
    }

    private ActivationContext validateActivation(Analyzer analyzer) {
        return validateActivation(analyzer, true);
    }

    private ActivationContext validateActivation(Analyzer analyzer, boolean requireVerifiedMapping) {
        List<AnalyzerActivationBlocker> blockers = new ArrayList<>();
        // A retired analyzer returns through Restore, never straight to running.
        if (analyzer.getStatus() == Analyzer.AnalyzerStatus.RETIRED) {
            blockers.add(new AnalyzerActivationBlocker(RETIRED_BLOCKER));
        }
        if (analyzer.getName() == null || analyzer.getName().isBlank()) {
            blockers.add(new AnalyzerActivationBlocker(NAME_BLOCKER));
        }
        if (analyzer.getTestUnitIds() == null || analyzer.getTestUnitIds().stream().noneMatch(this::isActiveLabUnit)) {
            blockers.add(new AnalyzerActivationBlocker(LAB_UNIT_BLOCKER));
        }

        AnalyzerProfilePin profile = analyzer.getPinnedProfile();
        BridgeProfileCatalog.ProfileRevision profileRevision = null;
        if (profile == null) {
            blockers.add(new AnalyzerActivationBlocker(PROFILE_BLOCKER));
        } else {
            try {
                profileRevision = profileCatalogService.getProfile(profile.getProfileId(),
                        profile.getProfileRevision());
                BridgeAnalyzerProfile catalogProfile = BridgeAnalyzerProfile.from(profileRevision.profile());
                if (!"ACTIVE".equals(catalogProfile.status()) || !matchesCatalogProfile(profile, catalogProfile)) {
                    blockers.add(new AnalyzerActivationBlocker(PROFILE_BLOCKER));
                }
            } catch (BridgeProfileCatalogException | IllegalArgumentException exception) {
                blockers.add(new AnalyzerActivationBlocker(PROFILE_BLOCKER));
            }
        }

        AnalyzerMappingSnapshot snapshot = null;
        AnalyzerMappingConfirmation confirmation = null;
        String bindingRevisionId = analyzer.getMapping() == null ? null : analyzer.getMapping().getId();
        if (bindingRevisionId != null) {
            snapshot = mappingService.findById(bindingRevisionId).orElse(null);
            if (snapshot != null && requireVerifiedMapping) {
                AnalyzerMappingVerificationAssessment assessment = confirmationService.assessCurrent(snapshot);
                if (!assessment.mappingsCurrent()) {
                    blockers.add(new AnalyzerActivationBlocker(MAPPINGS_BLOCKER));
                }
                confirmation = assessment.currentConfirmation().orElse(null);
            }
        }
        if (snapshot == null) {
            blockers.add(new AnalyzerActivationBlocker(MAPPINGS_BLOCKER));
        }

        ConnectionReference connection = null;
        try {
            connection = requireConnectionReference(analyzer);
            if (profile == null || !matchesProfile(profile, connection.document().path("profileRef"))) {
                blockers.add(new AnalyzerActivationBlocker(CONNECTION_BLOCKER));
            } else {
                addBridgeReadinessBlockers(connection.document(), blockers);
            }
        } catch (BridgeAnalyzerConnectionException | IllegalArgumentException exception) {
            blockers.add(connectionBlocker(exception));
        }
        return new ActivationContext(snapshot, confirmation, connection, List.copyOf(blockers));
    }

    private ConnectionReference requireConnectionReference(Analyzer analyzer) {
        String connectionId = requireText(analyzer.getBridgeConnectionId(), "Bridge connection ID");
        ObjectNode connection = bridgeClient.getConnection(connectionId);
        if (!connectionId.equals(connection.path("connectionId").asText())
                || !analyzer.getId().equals(connection.path("clientAnalyzerId").asText())) {
            throw new IllegalArgumentException("Bridge connection does not belong to this analyzer");
        }
        int configRevision = connection.path("configRevision").asInt(0);
        String configFingerprint = connection.path("configFingerprint").asText(null);
        if (configRevision < 1 || configFingerprint == null || configFingerprint.isBlank()) {
            throw new IllegalArgumentException("Bridge connection revision is invalid");
        }
        return new ConnectionReference(connectionId, configRevision, configFingerprint, connection);
    }

    private ActivationContext deactivationContext(Analyzer analyzer, ConnectionReference connection) {
        AnalyzerMapping revision = analyzer.getMapping();
        AnalyzerMappingSnapshot snapshot = revision == null ? null
                : mappingService.findById(revision.getId()).orElse(null);
        if (snapshot == null) {
            throw new IllegalArgumentException("Analyzer mapping is missing");
        }
        // Applying a mapping to an active analyzer replaces it without a new
        // activation, so the last activation's confirmation can name an older one.
        AnalyzerMappingConfirmation confirmation = confirmationService.findForMapping(revision.getId()).orElse(null);
        return new ActivationContext(snapshot, confirmation, connection, List.of());
    }

    private static List<AnalyzerActivationBlocker> validateAcknowledgement(ActivationContext context,
            ObjectNode acknowledgement, String action, String state) {
        ConnectionReference connection = context.connection();
        AnalyzerProfilePin profile = context.snapshot().mapping().getProfilePin();
        if (acknowledgement == null || !connection.connectionId().equals(acknowledgement.path("connectionId").asText())
                || connection.configRevision() != acknowledgement.path("configRevision").asInt(0)
                || !Objects.equals(connection.configFingerprint(),
                        acknowledgement.path("configFingerprint").asText(null))
                || !matchesProfile(profile, acknowledgement.path("profileRef"))
                || !action.equals(acknowledgement.path("action").asText())
                || ("START_SETUP".equals(action) && !"SETUP".equals(acknowledgement.path("receptionPurpose").asText()))
                || ("ACTIVATE".equals(action) && "SETUP".equals(acknowledgement.path("receptionPurpose").asText()))) {
            return List.of(new AnalyzerActivationBlocker(BRIDGE_ACKNOWLEDGEMENT_BLOCKER));
        }
        String outcome = acknowledgement.path("outcome").asText();
        if (("APPLIED".equals(outcome) || "ALREADY_APPLIED".equals(outcome))
                && state.equals(acknowledgement.path("actualRuntimeState").asText())) {
            return List.of();
        }
        List<AnalyzerActivationBlocker> blockers = bridgeBlockers(acknowledgement.path("blockers"));
        return blockers.isEmpty() ? List.of(new AnalyzerActivationBlocker(BRIDGE_ACKNOWLEDGEMENT_BLOCKER)) : blockers;
    }

    private void compensateIfApplied(ActivationContext context, ObjectNode acknowledgement, String action,
            Supplier<String> commandIdSupplier, RuntimeException original) {
        if (!"APPLIED".equals(acknowledgement.path("outcome").asText())) {
            return;
        }
        try {
            bridgeClient.applyRuntimeCommand(context.connectionId(), context.configRevision(), action,
                    requireText(commandIdSupplier.get(), "compensation command ID"));
        } catch (RuntimeException compensationFailure) {
            original.addSuppressed(compensationFailure);
        }
    }

    private static void addBridgeReadinessBlockers(ObjectNode connection, List<AnalyzerActivationBlocker> blockers) {
        JsonNode readiness = connection.path("readiness");
        if (readiness.path("ready").asBoolean(false)) {
            return;
        }
        List<AnalyzerActivationBlocker> bridgeBlockers = bridgeBlockers(readiness.path("blockers"));
        if (bridgeBlockers.isEmpty()) {
            blockers.add(new AnalyzerActivationBlocker(CONNECTION_BLOCKER));
            return;
        }
        // Activating is what clears a failed restore, so it never blocks activation.
        bridgeBlockers.stream().filter(blocker -> !BRIDGE_RESTORE_FAILED.equals(blocker.code())).forEach(blockers::add);
    }

    private static List<AnalyzerActivationBlocker> bridgeBlockers(JsonNode source) {
        List<AnalyzerActivationBlocker> blockers = new ArrayList<>();
        if (source.isArray()) {
            source.forEach(blocker -> {
                String messageKey = blocker.path("messageKey").asText(null);
                if (messageKey != null && !messageKey.isBlank()) {
                    blockers.add(new AnalyzerActivationBlocker(messageKey));
                }
            });
        }
        return List.copyOf(blockers);
    }

    private static boolean matchesCatalogProfile(AnalyzerProfilePin profile, BridgeAnalyzerProfile catalogProfile) {
        return profile.names(catalogProfile.profileId(), catalogProfile.revision());
    }

    private static boolean matchesProfile(AnalyzerProfilePin profile, JsonNode profileRef) {
        return profile != null
                && profile.names(profileRef.path("profileId").asText(null), profileRef.path("revision").asInt(0));
    }

    private boolean isActiveLabUnit(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        var testSection = testSectionService.get(id.trim());
        return testSection != null && "Y".equalsIgnoreCase(testSection.getIsActive());
    }

    /**
     * Holds the analyzer row for the rest of the transaction, so lifecycle
     * transitions on one analyzer run one after another. A concurrent transition
     * waits here and then validates against the committed state, instead of both
     * commanding the Bridge and the one that loses the version check compensating
     * the other's runtime away.
     */
    private Analyzer lockAnalyzer(String analyzerId) {
        String exactId = requireText(analyzerId, "analyzer ID");
        analyzerService.findByIdForUpdate(exactId)
                .orElseThrow(() -> new IllegalArgumentException("Analyzer not found: " + exactId));
        return findAnalyzer(exactId);
    }

    private Analyzer findAnalyzer(String analyzerId) {
        String exactId = requireText(analyzerId, "analyzer ID");
        return analyzerService.getWithMapping(exactId)
                .orElseThrow(() -> new IllegalArgumentException("Analyzer not found: " + exactId));
    }

    private static AnalyzerActivationBlocker connectionBlocker(RuntimeException exception) {
        return new AnalyzerActivationBlocker(CONNECTION_BLOCKER,
                Map.of("detail", String.valueOf(exception.getMessage())));
    }

    private static String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    private record ConnectionReference(String connectionId, int configRevision, String configFingerprint,
            ObjectNode document) {
    }

    private record ActivationContext(AnalyzerMappingSnapshot snapshot, AnalyzerMappingConfirmation confirmation,
            ConnectionReference connection, List<AnalyzerActivationBlocker> blockers) {

        String connectionId() {
            return connection.connectionId();
        }

        int configRevision() {
            return connection.configRevision();
        }
    }

    private record PreviousAnalyzerState(Analyzer.AnalyzerStatus status, boolean active,
            AnalyzerActivationRecord latestActivationRecord, Date lastActivatedDate, String sysUserId,
            Timestamp lastupdated) {

        static PreviousAnalyzerState capture(Analyzer analyzer) {
            return new PreviousAnalyzerState(analyzer.getStatus(), analyzer.isActive(),
                    analyzer.getLatestActivationRecord(), analyzer.getLastActivatedDate(), analyzer.getSysUserId(),
                    analyzer.getLastupdated());
        }

        void restore(Analyzer analyzer) {
            analyzer.setStatus(status);
            analyzer.setActive(active);
            analyzer.setLatestActivationRecord(latestActivationRecord);
            analyzer.setLastActivatedDate(lastActivatedDate);
            analyzer.setSysUserId(sysUserId);
            analyzer.setLastupdated(lastupdated);
        }
    }
}
