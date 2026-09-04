package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.openelisglobal.analyzer.form.AnalyzerMigrationPlanRequest;
import org.openelisglobal.analyzer.form.AnalyzerMigrationPlanRequest.Decision;
import org.openelisglobal.analyzer.form.AnalyzerMigrationPlanRequest.Decision.Action;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;

public class AnalyzerMigrationServiceTest {

    private static final String SOURCE_FINGERPRINT = "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String SNAPSHOT_FINGERPRINT = "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final String PROFILE_FINGERPRINT = "sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";

    @Test
    public void planApplyAndVerifyUseOnlyTheExplicitBridgeReference() {
        AnalyzerMigrationSourceSnapshotService snapshotService = mock(AnalyzerMigrationSourceSnapshotService.class);
        AnalyzerService analyzerService = mock(AnalyzerService.class);
        AnalyzerProfileBindingService profileBindingService = mock(AnalyzerProfileBindingService.class);
        BridgeAnalyzerConnectionClient bridgeClient = mock(BridgeAnalyzerConnectionClient.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-25T12:00:00Z"), ZoneOffset.UTC);
        AnalyzerMigrationServiceImpl service = new AnalyzerMigrationServiceImpl(snapshotService, analyzerService,
                profileBindingService, bridgeClient, clock);

        Analyzer analyzer = new Analyzer();
        analyzer.setId("42");
        analyzer.setName("Released analyzer");
        AnalyzerProfileBinding binding = binding();
        AnalyzerSiteBindingRevision revision = revision(binding);
        AnalyzerMigrationSourceSnapshot snapshot = new AnalyzerMigrationSourceSnapshot(SNAPSHOT_FINGERPRINT,
                List.of(new AnalyzerMigrationSourceSnapshot.AnalyzerSource("42", SOURCE_FINGERPRINT)));
        ObjectNode connection = connection();

        AnalyzerMigrationSourceSnapshot changedAfterApply = new AnalyzerMigrationSourceSnapshot(
                "sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd",
                List.of(new AnalyzerMigrationSourceSnapshot.AnalyzerSource("42", SOURCE_FINGERPRINT)));
        when(snapshotService.snapshot()).thenReturn(snapshot, snapshot, changedAfterApply);
        when(analyzerService.getWithType("42")).thenReturn(Optional.of(analyzer));
        when(bridgeClient.getConnection("bridge-42")).thenReturn(connection);
        when(profileBindingService.assignProfile(eq(analyzer), eq("site.fixture"), eq(2), eq("user-7")))
                .thenAnswer(invocation -> {
                    analyzer.setSiteBindingRevision(revision);
                    return binding;
                });
        when(analyzerService.update(analyzer)).thenReturn(analyzer);

        AnalyzerMigrationManifest plan = service.plan(planRequest(), "user-7");
        AnalyzerMigrationManifest apply = service.apply(plan, "user-7");
        AnalyzerMigrationManifest verifyResult = service.verify(apply);

        assertEquals(AnalyzerMigrationManifest.Status.READY, plan.outcomes().get(0).outcome());
        assertEquals(AnalyzerMigrationManifest.Status.MIGRATED, apply.outcomes().get(0).outcome());
        assertEquals(AnalyzerMigrationManifest.Status.MIGRATED, verifyResult.outcomes().get(0).outcome());
        assertEquals("bridge-42", analyzer.getBridgeConnectionId());
        assertSame(binding, analyzer.getPinnedProfileBinding());
        verify(profileBindingService).assignProfile(analyzer, "site.fixture", 2, "user-7");
        verify(analyzerService).update(analyzer);
    }

    @Test
    public void applyAndVerifyExplicitExclusionRemovesAnalyzerFromOperationalUse() {
        AnalyzerMigrationSourceSnapshotService snapshotService = mock(AnalyzerMigrationSourceSnapshotService.class);
        AnalyzerService analyzerService = mock(AnalyzerService.class);
        AnalyzerProfileBindingService profileBindingService = mock(AnalyzerProfileBindingService.class);
        BridgeAnalyzerConnectionClient bridgeClient = mock(BridgeAnalyzerConnectionClient.class);
        AnalyzerMigrationServiceImpl service = new AnalyzerMigrationServiceImpl(snapshotService, analyzerService,
                profileBindingService, bridgeClient,
                Clock.fixed(Instant.parse("2026-08-25T12:00:00Z"), ZoneOffset.UTC));

        Analyzer analyzer = new Analyzer();
        analyzer.setId("42");
        analyzer.setActive(true);
        analyzer.setStatus(Analyzer.AnalyzerStatus.ACTIVE);
        AnalyzerMigrationSourceSnapshot snapshot = new AnalyzerMigrationSourceSnapshot(SNAPSHOT_FINGERPRINT,
                List.of(new AnalyzerMigrationSourceSnapshot.AnalyzerSource("42", SOURCE_FINGERPRINT)));
        when(snapshotService.snapshot()).thenReturn(snapshot);
        when(analyzerService.getWithType("42")).thenReturn(Optional.of(analyzer));
        when(analyzerService.update(analyzer)).thenReturn(analyzer);

        Decision decision = new Decision();
        decision.setSourceAnalyzerId("42");
        decision.setAction(Action.EXCLUDE);
        decision.setReasonCode("NOT_RETAINED");
        AnalyzerMigrationPlanRequest request = new AnalyzerMigrationPlanRequest();
        request.setRunId("migration-run-2");
        request.setDecisions(List.of(decision));

        AnalyzerMigrationManifest plan = service.plan(request, "user-7");
        AnalyzerMigrationManifest apply = service.apply(plan, "user-7");
        AnalyzerMigrationManifest verifyResult = service.verify(apply);

        assertEquals(AnalyzerMigrationManifest.Status.INTENTIONALLY_EXCLUDED, verifyResult.outcomes().get(0).outcome());
        assertEquals(false, analyzer.isActive());
        assertEquals(Analyzer.AnalyzerStatus.INACTIVE, analyzer.getStatus());
        verify(analyzerService).update(analyzer);
    }

    private static AnalyzerMigrationPlanRequest planRequest() {
        Decision decision = new Decision();
        decision.setSourceAnalyzerId("42");
        decision.setAction(Action.MIGRATE);
        decision.setProfileId("site.fixture");
        decision.setProfileRevision(2);
        decision.setProfileFingerprint(PROFILE_FINGERPRINT);
        decision.setBridgeConnectionId("bridge-42");
        AnalyzerMigrationPlanRequest request = new AnalyzerMigrationPlanRequest();
        request.setRunId("migration-run-1");
        request.setDecisions(List.of(decision));
        return request;
    }

    private static AnalyzerProfileBinding binding() {
        AnalyzerProfileBinding binding = new AnalyzerProfileBinding();
        binding.setId("7");
        binding.setProfileId("site.fixture");
        binding.setProfileRevision(2);
        binding.setProfileFingerprint(PROFILE_FINGERPRINT);
        return binding;
    }

    private static AnalyzerSiteBindingRevision revision(AnalyzerProfileBinding profileBinding) {
        AnalyzerSiteBinding siteBinding = new AnalyzerSiteBinding();
        siteBinding.setId("8");
        siteBinding.setProfileBinding(profileBinding);
        AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
        revision.setId("9");
        revision.setSiteBinding(siteBinding);
        return revision;
    }

    private static ObjectNode connection() {
        ObjectNode connection = new ObjectMapper().createObjectNode();
        connection.put("connectionId", "bridge-42");
        connection.put("clientAnalyzerId", "42");
        connection.put("configRevision", 3);
        connection.putObject("profileRef").put("profileId", "site.fixture").put("revision", 2).put("fingerprint",
                PROFILE_FINGERPRINT);
        return connection;
    }
}
