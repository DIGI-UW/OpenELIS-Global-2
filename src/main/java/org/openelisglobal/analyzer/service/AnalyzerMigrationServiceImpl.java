package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.form.AnalyzerMigrationPlanRequest;
import org.openelisglobal.analyzer.form.AnalyzerMigrationPlanRequest.Decision;
import org.openelisglobal.analyzer.service.AnalyzerMigrationManifest.Mode;
import org.openelisglobal.analyzer.service.AnalyzerMigrationManifest.Outcome;
import org.openelisglobal.analyzer.service.AnalyzerMigrationManifest.ProfileReference;
import org.openelisglobal.analyzer.service.AnalyzerMigrationManifest.ProfileSelection;
import org.openelisglobal.analyzer.service.AnalyzerMigrationManifest.Status;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.Analyzer.AnalyzerStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerMigrationServiceImpl implements AnalyzerMigrationService {

    private final AnalyzerMigrationSourceSnapshotService snapshotService;
    private final AnalyzerService analyzerService;
    private final AnalyzerProfileBindingService profileBindingService;
    private final BridgeAnalyzerConnectionClient bridgeClient;
    private final Clock clock;

    @Autowired
    public AnalyzerMigrationServiceImpl(AnalyzerMigrationSourceSnapshotService snapshotService,
            AnalyzerService analyzerService, AnalyzerProfileBindingService profileBindingService,
            BridgeAnalyzerConnectionClient bridgeClient) {
        this(snapshotService, analyzerService, profileBindingService, bridgeClient, Clock.systemUTC());
    }

    AnalyzerMigrationServiceImpl(AnalyzerMigrationSourceSnapshotService snapshotService,
            AnalyzerService analyzerService, AnalyzerProfileBindingService profileBindingService,
            BridgeAnalyzerConnectionClient bridgeClient, Clock clock) {
        this.snapshotService = snapshotService;
        this.analyzerService = analyzerService;
        this.profileBindingService = profileBindingService;
        this.bridgeClient = bridgeClient;
        this.clock = clock;
    }

    @Override
    public AnalyzerMigrationManifest plan(AnalyzerMigrationPlanRequest request, String actor) {
        requireText(request == null ? null : request.getRunId(), "Migration run ID");
        String selectedBy = requireText(actor, "Migration actor");
        AnalyzerMigrationSourceSnapshot snapshot = snapshotService.snapshot();
        Map<String, Decision> decisions = decisionsByAnalyzer(request.getDecisions());
        Set<String> sourceIds = snapshot.analyzers().stream()
                .map(AnalyzerMigrationSourceSnapshot.AnalyzerSource::analyzerId).collect(Collectors.toSet());
        if (!sourceIds.containsAll(decisions.keySet())) {
            throw new IllegalArgumentException("Migration decision references an unknown analyzer");
        }

        Instant startedAt = clock.instant();
        List<Outcome> outcomes = snapshot.analyzers().stream()
                .map(source -> plan(source, decisions.get(source.analyzerId()), selectedBy, startedAt)).toList();
        return manifest(request.getRunId(), Mode.PLAN, snapshot.fingerprint(), startedAt, outcomes);
    }

    @Override
    @Transactional
    public AnalyzerMigrationManifest apply(AnalyzerMigrationManifest plan, String actor) {
        requireMode(plan, Mode.PLAN);
        String migrationActor = requireText(actor, "Migration actor");
        AnalyzerMigrationSourceSnapshot snapshot = requireSameSnapshot(plan);
        Map<String, AnalyzerMigrationSourceSnapshot.AnalyzerSource> sources = sourcesById(snapshot);
        Instant startedAt = clock.instant();
        List<Outcome> outcomes = plan.outcomes().stream().map(outcome -> apply(outcome, sources, migrationActor))
                .toList();
        return manifest(plan.runId(), Mode.APPLY, snapshot.fingerprint(), startedAt, outcomes);
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyzerMigrationManifest verify(AnalyzerMigrationManifest apply) {
        requireMode(apply, Mode.APPLY);
        Instant startedAt = clock.instant();
        List<Outcome> outcomes = apply.outcomes().stream().map(this::verify).toList();
        return manifest(apply.runId(), Mode.VERIFY, apply.sourceSnapshotFingerprint(), startedAt, outcomes);
    }

    private Outcome plan(AnalyzerMigrationSourceSnapshot.AnalyzerSource source, Decision decision, String actor,
            Instant selectedAt) {
        if (decision == null) {
            return correction(source, "EXPLICIT_MIGRATION_DECISION_REQUIRED");
        }
        if (decision.getAction() == Decision.Action.EXCLUDE) {
            if (isBlank(decision.getReasonCode())) {
                return correction(source, "EXCLUSION_REASON_REQUIRED");
            }
            return new Outcome(migrationKey(source), source.analyzerId(), source.fingerprint(),
                    Status.INTENTIONALLY_EXCLUDED, null, null, null, decision.getReasonCode().trim());
        }
        if (decision.getAction() != Decision.Action.MIGRATE || isBlank(decision.getProfileId())
                || decision.getProfileRevision() < 1 || isBlank(decision.getProfileFingerprint())
                || isBlank(decision.getBridgeConnectionId())) {
            return correction(source, "EXPLICIT_PROFILE_AND_BRIDGE_CONNECTION_REQUIRED");
        }

        ProfileReference profileRef = new ProfileReference(decision.getProfileId().trim(),
                decision.getProfileRevision(), decision.getProfileFingerprint().trim());
        ProfileSelection selection = new ProfileSelection("EXPLICIT", profileRef, actor, selectedAt.toString());
        try {
            ObjectNode connection = requireExactConnection(source.analyzerId(), decision.getBridgeConnectionId(),
                    profileRef);
            return new Outcome(migrationKey(source), source.analyzerId(), source.fingerprint(), Status.READY, selection,
                    connection.path("connectionId").asText(), connection.path("configRevision").asInt(), null);
        } catch (BridgeAnalyzerConnectionException | IllegalArgumentException exception) {
            return new Outcome(migrationKey(source), source.analyzerId(), source.fingerprint(), Status.NEEDS_CORRECTION,
                    selection, decision.getBridgeConnectionId(), null, "BRIDGE_CONNECTION_VALIDATION_FAILED");
        }
    }

    private Outcome apply(Outcome outcome, Map<String, AnalyzerMigrationSourceSnapshot.AnalyzerSource> sources,
            String actor) {
        if (outcome.outcome() == Status.INTENTIONALLY_EXCLUDED) {
            AnalyzerMigrationSourceSnapshot.AnalyzerSource source = matchingSource(outcome, sources);
            Analyzer analyzer = analyzerService.getWithType(source.analyzerId())
                    .orElseThrow(() -> new IllegalArgumentException("Analyzer no longer exists"));
            analyzer.setActive(false);
            analyzer.setStatus(AnalyzerStatus.INACTIVE);
            analyzer.setBridgeConnectionId(null);
            analyzer.setSiteBindingRevision(null);
            analyzer.setSysUserId(actor);
            analyzerService.update(analyzer);
            return outcome;
        }
        if (outcome.outcome() != Status.READY) {
            return outcome;
        }
        AnalyzerMigrationSourceSnapshot.AnalyzerSource source = matchingSource(outcome, sources);
        ProfileReference profileRef = requireSelection(outcome).profileRef();
        ObjectNode connection;
        try {
            connection = requireExactConnection(source.analyzerId(), outcome.bridgeConnectionId(), profileRef);
        } catch (BridgeAnalyzerConnectionException | IllegalArgumentException exception) {
            return correction(outcome, "MIGRATION_APPLY_FAILED");
        }
        if (!Objects.equals(outcome.bridgeConfigRevision(), connection.path("configRevision").asInt())) {
            return correction(outcome, "BRIDGE_CONNECTION_CHANGED_AFTER_PLAN");
        }
        Analyzer analyzer = analyzerService.getWithType(source.analyzerId())
                .orElseThrow(() -> new IllegalArgumentException("Analyzer no longer exists"));
        profileBindingService.assignProfile(analyzer, profileRef.profileId(), profileRef.revision(), actor);
        analyzer.setBridgeConnectionId(connection.path("connectionId").asText());
        analyzer.setSysUserId(actor);
        analyzerService.update(analyzer);
        return new Outcome(outcome.migrationKey(), outcome.sourceAnalyzerId(), outcome.sourceConfigFingerprint(),
                Status.MIGRATED, outcome.profileSelection(), outcome.bridgeConnectionId(),
                connection.path("configRevision").asInt(), null);
    }

    private Outcome verify(Outcome outcome) {
        if (outcome.outcome() == Status.INTENTIONALLY_EXCLUDED) {
            return analyzerService.getWithType(outcome.sourceAnalyzerId())
                    .filter(analyzer -> !analyzer.isActive() && analyzer.getStatus() == AnalyzerStatus.INACTIVE
                            && analyzer.getBridgeConnectionId() == null && analyzer.getSiteBindingRevision() == null)
                    .map(analyzer -> outcome).orElseGet(() -> correction(outcome, "EXCLUSION_VERIFICATION_FAILED"));
        }
        if (outcome.outcome() != Status.MIGRATED) {
            return outcome;
        }
        try {
            ProfileReference profileRef = requireSelection(outcome).profileRef();
            Analyzer analyzer = analyzerService.getWithType(outcome.sourceAnalyzerId())
                    .orElseThrow(() -> new IllegalArgumentException("Analyzer no longer exists"));
            if (!Objects.equals(outcome.bridgeConnectionId(), analyzer.getBridgeConnectionId())
                    || analyzer.getPinnedProfileBinding() == null
                    || !Objects.equals(profileRef.profileId(), analyzer.getPinnedProfileBinding().getProfileId())
                    || profileRef.revision() != analyzer.getPinnedProfileBinding().getProfileRevision()
                    || !Objects.equals(profileRef.fingerprint(),
                            analyzer.getPinnedProfileBinding().getProfileFingerprint())) {
                return correction(outcome, "OPENELIS_REFERENCE_VERIFICATION_FAILED");
            }
            ObjectNode connection = requireExactConnection(outcome.sourceAnalyzerId(), outcome.bridgeConnectionId(),
                    profileRef);
            if (!Objects.equals(outcome.bridgeConfigRevision(), connection.path("configRevision").asInt())) {
                return correction(outcome, "BRIDGE_CONNECTION_CHANGED_AFTER_APPLY");
            }
            return outcome;
        } catch (BridgeAnalyzerConnectionException | IllegalArgumentException exception) {
            return correction(outcome, "MIGRATION_VERIFY_FAILED");
        }
    }

    private AnalyzerMigrationSourceSnapshot requireSameSnapshot(AnalyzerMigrationManifest manifest) {
        AnalyzerMigrationSourceSnapshot snapshot = snapshotService.snapshot();
        if (!Objects.equals(manifest.sourceSnapshotFingerprint(), snapshot.fingerprint())) {
            throw new IllegalArgumentException("Analyzer source data changed after migration planning");
        }
        return snapshot;
    }

    private ObjectNode requireExactConnection(String analyzerId, String connectionId, ProfileReference profileRef) {
        String exactConnectionId = requireText(connectionId, "Bridge connection ID");
        ObjectNode connection = bridgeClient.getConnection(exactConnectionId);
        JsonNode actualProfile = connection.path("profileRef");
        if (!Objects.equals(exactConnectionId, connection.path("connectionId").asText(null))
                || !Objects.equals(analyzerId, connection.path("clientAnalyzerId").asText(null))
                || !Objects.equals(profileRef.profileId(), actualProfile.path("profileId").asText(null))
                || profileRef.revision() != actualProfile.path("revision").asInt(0)
                || !Objects.equals(profileRef.fingerprint(), actualProfile.path("fingerprint").asText(null))
                || connection.path("configRevision").asInt(0) < 1) {
            throw new IllegalArgumentException("Bridge connection does not match the explicit migration selection");
        }
        return connection;
    }

    private AnalyzerMigrationManifest manifest(String runId, Mode mode, String snapshotFingerprint, Instant startedAt,
            List<Outcome> outcomes) {
        return new AnalyzerMigrationManifest("1.0", runId, mode, snapshotFingerprint, startedAt.toString(),
                clock.instant().toString(), outcomes);
    }

    private static Map<String, Decision> decisionsByAnalyzer(List<Decision> decisions) {
        if (decisions == null) {
            return Map.of();
        }
        Map<String, Decision> byAnalyzer = new HashMap<>();
        for (Decision decision : decisions) {
            String analyzerId = requireText(decision == null ? null : decision.getSourceAnalyzerId(),
                    "Source analyzer ID");
            if (byAnalyzer.put(analyzerId, decision) != null) {
                throw new IllegalArgumentException("Duplicate migration decision for analyzer " + analyzerId);
            }
        }
        return byAnalyzer;
    }

    private static Map<String, AnalyzerMigrationSourceSnapshot.AnalyzerSource> sourcesById(
            AnalyzerMigrationSourceSnapshot snapshot) {
        return snapshot.analyzers().stream().collect(
                Collectors.toMap(AnalyzerMigrationSourceSnapshot.AnalyzerSource::analyzerId, source -> source));
    }

    private static AnalyzerMigrationSourceSnapshot.AnalyzerSource matchingSource(Outcome outcome,
            Map<String, AnalyzerMigrationSourceSnapshot.AnalyzerSource> sources) {
        AnalyzerMigrationSourceSnapshot.AnalyzerSource source = Optional
                .ofNullable(sources.get(outcome.sourceAnalyzerId()))
                .orElseThrow(() -> new IllegalArgumentException("Analyzer no longer exists"));
        if (!Objects.equals(outcome.sourceConfigFingerprint(), source.fingerprint())) {
            throw new IllegalArgumentException("Analyzer source data changed after migration planning");
        }
        return source;
    }

    private static ProfileSelection requireSelection(Outcome outcome) {
        if (outcome.profileSelection() == null || outcome.profileSelection().profileRef() == null
                || !"EXPLICIT".equals(outcome.profileSelection().method())) {
            throw new IllegalArgumentException("Explicit profile selection is required");
        }
        return outcome.profileSelection();
    }

    private static Outcome correction(AnalyzerMigrationSourceSnapshot.AnalyzerSource source, String reasonCode) {
        return new Outcome(migrationKey(source), source.analyzerId(), source.fingerprint(), Status.NEEDS_CORRECTION,
                null, null, null, reasonCode);
    }

    private static Outcome correction(Outcome outcome, String reasonCode) {
        return new Outcome(outcome.migrationKey(), outcome.sourceAnalyzerId(), outcome.sourceConfigFingerprint(),
                Status.NEEDS_CORRECTION, outcome.profileSelection(), outcome.bridgeConnectionId(),
                outcome.bridgeConfigRevision(), reasonCode);
    }

    private static String migrationKey(AnalyzerMigrationSourceSnapshot.AnalyzerSource source) {
        String fingerprint = requireText(source.fingerprint(), "Source configuration fingerprint");
        String digest = fingerprint.startsWith("sha256:") ? fingerprint.substring(7) : fingerprint;
        return source.analyzerId() + ":" + digest.substring(0, Math.min(8, digest.length()));
    }

    private static void requireMode(AnalyzerMigrationManifest manifest, Mode expected) {
        if (manifest == null || manifest.mode() != expected) {
            throw new IllegalArgumentException("Expected migration manifest mode " + expected);
        }
    }

    private static String requireText(String value, String label) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
