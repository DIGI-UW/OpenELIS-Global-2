package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerProfileMigrationServiceImplTest {

    @Mock
    private AnalyzerProfileCatalogClient profileCatalogClient;

    @Mock
    private AnalyzerProfileMigrationExecutor executor;

    private AnalyzerProfileMigrationServiceImpl service;

    @Before
    public void setUp() {
        service = new AnalyzerProfileMigrationServiceImpl(profileCatalogClient, executor);
    }

    @Test
    public void migrateFetchesTheExplicitProfileRevisionBeforeStartingTheDatabaseWork() throws Exception {
        BridgeProfileCatalogEntry profile = profile("site.mock", 3);
        AnalyzerProfileMigrationResult expected = AnalyzerProfileMigrationResult.blocked("71", "site.mock", 3,
                java.util.List.of());
        when(profileCatalogClient.get("site.mock", 3)).thenReturn(profile);
        when(executor.execute("71", profile, "42")).thenReturn(expected);

        AnalyzerProfileMigrationResult actual = service.migrate("71", "site.mock", 3, "42");

        assertSame(expected, actual);
        verify(profileCatalogClient).get("site.mock", 3);
        verify(executor).execute("71", profile, "42");
    }

    @Test(expected = IllegalStateException.class)
    public void migrateRejectsABridgeResponseForAnotherRevision() throws Exception {
        BridgeProfileCatalogEntry wrongRevision = profile("site.mock", 2);
        when(profileCatalogClient.get("site.mock", 3)).thenReturn(wrongRevision);

        try {
            service.migrate("71", "site.mock", 3, "42");
        } finally {
            verify(executor, never()).execute("71", wrongRevision, "42");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void migrateRequiresAnExplicitPositiveRevision() {
        service.migrate("71", "site.mock", 0, "42");
    }

    private static BridgeProfileCatalogEntry profile(String profileId, int revision) throws Exception {
        return new BridgeProfileCatalogEntry(new ObjectMapper().readTree("""
                {
                  "profileId": "%s",
                  "revision": %d,
                  "status": "ACTIVE",
                  "tests": []
                }
                """.formatted(profileId, revision)),
                new BridgeProfileAudit("CREATED", "bridge", Instant.parse("2026-08-14T08:00:00Z")),
                "sha256:" + "a".repeat(64));
    }
}
