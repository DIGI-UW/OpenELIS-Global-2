package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

@Service
public class AnalyzerProfileMigrationServiceImpl implements AnalyzerProfileMigrationService {

    private final AnalyzerProfileCatalogClient profileCatalogClient;
    private final AnalyzerProfileMigrationExecutor executor;

    public AnalyzerProfileMigrationServiceImpl(AnalyzerProfileCatalogClient profileCatalogClient,
            AnalyzerProfileMigrationExecutor executor) {
        this.profileCatalogClient = profileCatalogClient;
        this.executor = executor;
    }

    @Override
    public AnalyzerProfileMigrationResult migrate(String analyzerId, String profileId, int profileRevision,
            String actor) {
        String effectiveAnalyzerId = requireText(analyzerId, "analyzerId is required");
        String effectiveProfileId = requireText(profileId, "profileId is required");
        String effectiveActor = requireText(actor, "actor is required");
        if (profileRevision < 1) {
            throw new IllegalArgumentException("profileRevision must be positive");
        }

        BridgeProfileCatalogEntry entry = profileCatalogClient.get(effectiveProfileId, profileRevision);
        JsonNode profile = entry == null ? null : entry.profile();
        if (profile == null || !effectiveProfileId.equals(profile.path("profileId").asText())
                || profileRevision != profile.path("revision").asInt()) {
            throw new IllegalStateException("Analyzer Bridge returned a different profile revision");
        }
        if (!"ACTIVE".equals(profile.path("status").asText())) {
            throw new IllegalStateException("Analyzer profile is not active: " + effectiveProfileId);
        }
        return executor.execute(effectiveAnalyzerId, entry, effectiveActor);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
