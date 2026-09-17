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
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingConfirmation;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.test.service.TestSectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerActivationServiceImpl implements AnalyzerActivationService {

    private static final String PROFILE_BLOCKER = "analyzer.activation.blocker.profile";
    private static final String NAME_BLOCKER = "analyzer.activation.blocker.name";
    private static final String LAB_UNIT_BLOCKER = "analyzer.activation.blocker.labUnit";
    private static final String MAPPINGS_BLOCKER = "analyzer.activation.blocker.mappings";
    private static final String RECOGNITION_BLOCKER = "analyzer.activation.blocker.recognition";
    private static final String CONNECTION_BLOCKER = "analyzer.activation.blocker.connection";
    private static final String BRIDGE_ACKNOWLEDGEMENT_BLOCKER = "analyzer.activation.blocker.bridgeAcknowledgement";

    private final AnalyzerService analyzerService;
    private final BridgeProfileCatalogService profileCatalogService;
    private final AnalyzerSiteBindingService siteBindingService;
    private final AnalyzerSiteBindingConfirmationService confirmationService;
    private final TestSectionService testSectionService;
    private final BridgeAnalyzerConnectionClient bridgeClient;
    private final AnalyzerActivationRecordService activationRecordService;
    private final Clock clock;
    private final Supplier<String> activationCommandIdSupplier;
    private final Supplier<String> deactivationCommandIdSupplier;

    @Autowired
    public AnalyzerActivationServiceImpl(AnalyzerService analyzerService,
            BridgeProfileCatalogService profileCatalogService, AnalyzerSiteBindingService siteBindingService,
            AnalyzerSiteBindingConfirmationService confirmationService, TestSectionService testSectionService,
            BridgeAnalyzerConnectionClient bridgeClient, AnalyzerActivationRecordService activationRecordService) {
        this(analyzerService, profileCatalogService, siteBindingService, confirmationService, testSectionService,
                bridgeClient, activationRecordService, Clock.systemUTC(), () -> UUID.randomUUID().toString(),
                () -> UUID.randomUUID().toString());
    }

    AnalyzerActivationServiceImpl(AnalyzerService analyzerService, BridgeProfileCatalogService profileCatalogService,
            AnalyzerSiteBindingService siteBindingService, AnalyzerSiteBindingConfirmationService confirmationService,
            TestSectionService testSectionService, BridgeAnalyzerConnectionClient bridgeClient,
            AnalyzerActivationRecordService activationRecordService, Clock clock,
            Supplier<String> activationCommandIdSupplier, Supplier<String> deactivationCommandIdSupplier) {
        this.analyzerService = analyzerService;
        this.profileCatalogService = profileCatalogService;
        this.siteBindingService = siteBindingService;
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
        return activateExact(findAnalyzer(analyzerId), actor);
    }

    @Override
    @Transactional
    public AnalyzerActivationResult reactivate(String analyzerId, String actor) {
        return activateExact(findAnalyzer(analyzerId), actor);
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
            AnalyzerActivationRecord record = activationRecordService.retain(analyzer, context.snapshot().revision(),
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
            compensateIfApplied(context, acknowledgement, "DEACTIVATE", deactivationCommandIdSupplier, exception);
            throw exception;
        }
    }

    @Override
    @Transactional
    public AnalyzerDeactivationResult deactivate(String analyzerId, String actor) {
        Analyzer analyzer = findAnalyzer(analyzerId);
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
            AnalyzerActivationRecord record = activationRecordService.retain(analyzer, context.snapshot().revision(),
                    context.confirmation(), acknowledgement, "INACTIVE", exactActor);
            analyzer.setLatestActivationRecord(record);
            analyzer.setStatus(Analyzer.AnalyzerStatus.INACTIVE);
            analyzer.setActive(false);
            analyzer.setSysUserId(exactActor);
            analyzerService.update(analyzer);
            return AnalyzerDeactivationResult.deactivated(analyzer);
        } catch (RuntimeException exception) {
            previousState.restore(analyzer);
            compensateIfApplied(context, acknowledgement, "ACTIVATE", activationCommandIdSupplier, exception);
            throw exception;
        }
    }

    private ActivationContext validateActivation(Analyzer analyzer) {
        List<AnalyzerActivationBlocker> blockers = new ArrayList<>();
        if (analyzer.getName() == null || analyzer.getName().isBlank()) {
            blockers.add(new AnalyzerActivationBlocker(NAME_BLOCKER));
        }
        if (analyzer.getTestUnitIds() == null || analyzer.getTestUnitIds().stream().noneMatch(this::isActiveLabUnit)) {
            blockers.add(new AnalyzerActivationBlocker(LAB_UNIT_BLOCKER));
        }

        AnalyzerProfileBinding profile = analyzer.getPinnedProfileBinding();
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

        AnalyzerSiteBindingSnapshot snapshot = null;
        AnalyzerSiteBindingConfirmation confirmation = null;
        String bindingRevisionId = analyzer.getSiteBindingRevision() == null ? null
                : analyzer.getSiteBindingRevision().getId();
        String recognitionFingerprint = profileRevision == null || profileRevision.controlRecognitionSummary() == null
                ? null
                : profileRevision.controlRecognitionSummary().recognitionFingerprint();
        if (bindingRevisionId != null && recognitionFingerprint != null) {
            snapshot = siteBindingService.findByRevisionId(bindingRevisionId).orElse(null);
            if (snapshot != null) {
                AnalyzerSiteBindingVerificationAssessment assessment = confirmationService.assessCurrent(snapshot,
                        recognitionFingerprint);
                if (!assessment.mappingsCurrent()) {
                    blockers.add(new AnalyzerActivationBlocker(MAPPINGS_BLOCKER));
                }
                if (!assessment.recognitionCurrent()) {
                    blockers.add(new AnalyzerActivationBlocker(RECOGNITION_BLOCKER));
                }
                confirmation = assessment.currentConfirmation().orElse(null);
            }
        }
        if (snapshot == null) {
            blockers.add(new AnalyzerActivationBlocker(MAPPINGS_BLOCKER));
            blockers.add(new AnalyzerActivationBlocker(RECOGNITION_BLOCKER));
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
        AnalyzerSiteBindingRevision revision = analyzer.getSiteBindingRevision();
        AnalyzerSiteBindingSnapshot snapshot = revision == null ? null
                : siteBindingService.findByRevisionId(revision.getId()).orElse(null);
        AnalyzerSiteBindingConfirmation confirmation = analyzer.getLatestActivationRecord() == null ? null
                : analyzer.getLatestActivationRecord().getVerificationConfirmation();
        if (snapshot == null) {
            throw new IllegalArgumentException("Analyzer site binding is missing");
        }
        return new ActivationContext(snapshot, confirmation, connection, List.of());
    }

    private static List<AnalyzerActivationBlocker> validateAcknowledgement(ActivationContext context,
            ObjectNode acknowledgement, String action, String state) {
        ConnectionReference connection = context.connection();
        AnalyzerProfileBinding profile = context.snapshot().binding().getProfileBinding();
        if (acknowledgement == null || !connection.connectionId().equals(acknowledgement.path("connectionId").asText())
                || connection.configRevision() != acknowledgement.path("configRevision").asInt(0)
                || !Objects.equals(connection.configFingerprint(),
                        acknowledgement.path("configFingerprint").asText(null))
                || !matchesProfile(profile, acknowledgement.path("profileRef"))
                || !action.equals(acknowledgement.path("action").asText())) {
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
        } else {
            blockers.addAll(bridgeBlockers);
        }
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

    private static boolean matchesCatalogProfile(AnalyzerProfileBinding profile, BridgeAnalyzerProfile catalogProfile) {
        return Objects.equals(profile.getProfileId(), catalogProfile.profileId())
                && profile.getProfileRevision() == catalogProfile.revision()
                && Objects.equals(profile.getProfileFingerprint(), catalogProfile.revisionFingerprint());
    }

    private static boolean matchesProfile(AnalyzerProfileBinding profile, JsonNode profileRef) {
        return profile != null && Objects.equals(profile.getProfileId(), profileRef.path("profileId").asText(null))
                && profile.getProfileRevision() == profileRef.path("revision").asInt(0)
                && Objects.equals(profile.getProfileFingerprint(), profileRef.path("fingerprint").asText(null));
    }

    private boolean isActiveLabUnit(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        var testSection = testSectionService.get(id.trim());
        return testSection != null && "Y".equalsIgnoreCase(testSection.getIsActive());
    }

    private Analyzer findAnalyzer(String analyzerId) {
        String exactId = requireText(analyzerId, "analyzer ID");
        return analyzerService.getWithBinding(exactId)
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

    private record ActivationContext(AnalyzerSiteBindingSnapshot snapshot, AnalyzerSiteBindingConfirmation confirmation,
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
