package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.dao.AnalyzerProfileDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfile;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bootstraps built-in analyzer profiles from JSON files at startup.
 *
 * <p>
 * Scans {@code projects/analyzer-defaults/{astm,hl7}/*.json} (or
 * ANALYZER_DEFAULTS_DIR) and upserts each into analyzer_profile with
 * source=BUILT_IN, is_mutable=false.
 *
 * <p>
 * Built-in profile metadata is sourced from each JSON file's profileMeta block.
 */
@Service
@DependsOn("liquibase")
public class BuiltInProfileBootstrapService {

    private static final String SOURCE_BUILT_IN = "BUILT_IN";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private AnalyzerProfileDAO analyzerProfileDAO;

    /**
     * Resilient bootstrap by design: a bad file is logged and skipped so other
     * valid built-in profiles still load at startup.
     */
    @PostConstruct
    @Transactional
    public void bootstrapBuiltInProfiles() {
        Path baseDir = resolveDefaultsDir();
        if (baseDir == null || !Files.exists(baseDir) || !Files.isDirectory(baseDir)) {
            LogEvent.logInfo(this.getClass().getName(), "bootstrapBuiltInProfiles",
                    "Analyzer defaults directory not found, skipping built-in profile bootstrap");
            return;
        }

        int upserted = 0;
        int skipped = 0;
        List<String> failures = new ArrayList<>();

        for (String protocol : List.of("astm", "hl7")) {
            Path protocolDir = baseDir.resolve(protocol);
            if (!Files.exists(protocolDir) || !Files.isDirectory(protocolDir)) {
                continue;
            }
            try {
                List<Path> jsonFiles = Files.list(protocolDir).filter(p -> p.getFileName().toString().endsWith(".json"))
                        .toList();
                for (Path file : jsonFiles) {
                    try {
                        if (upsertProfileFromFile(file)) {
                            upserted++;
                        } else {
                            skipped++;
                        }
                    } catch (Exception e) {
                        failures.add(file.toString() + ": " + e.getMessage());
                        LogEvent.logError(this.getClass().getName(), "bootstrapBuiltInProfiles",
                                "Error loading " + file.getFileName() + ": " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                failures.add("scan " + protocol + ": " + e.getMessage());
                LogEvent.logError(this.getClass().getName(), "bootstrapBuiltInProfiles",
                        "Error scanning " + protocol + ": " + e.getMessage());
            }
        }

        LogEvent.logInfo(this.getClass().getName(), "bootstrapBuiltInProfiles",
                "Built-in profile bootstrap complete: " + upserted + " upserted, " + skipped + " skipped");
        if (!failures.isEmpty()) {
            LogEvent.logWarn(this.getClass().getName(), "bootstrapBuiltInProfiles", "Built-in profile bootstrap had "
                    + failures.size() + " failure(s): " + String.join(" | ", failures));
        }
    }

    private Path resolveDefaultsDir() {
        String dir = System.getenv("ANALYZER_DEFAULTS_DIR");
        if (dir == null || dir.isEmpty()) {
            dir = System.getenv("ANALYZER_PROFILES_DIR");
        }
        if (dir == null || dir.isEmpty()) {
            dir = System.getProperty("analyzer.defaults.dir");
        }
        if (dir == null || dir.isEmpty()) {
            dir = "/data/analyzer-defaults";
        }
        Path base = Path.of(dir);
        if (!Files.exists(base)) {
            Path relative = Path.of("projects/analyzer-defaults");
            if (Files.exists(relative)) {
                return relative.toAbsolutePath();
            }
        }
        return base;
    }

    private boolean upsertProfileFromFile(Path file) throws Exception {
        String jsonContent = Files.readString(file, StandardCharsets.UTF_8);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = OBJECT_MAPPER.readValue(jsonContent, Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> profileMeta = (Map<String, Object>) payload.get("profileMeta");
        if (profileMeta == null) {
            throw new IllegalArgumentException(
                    "Missing profileMeta in " + file.getFileName() + " (id/version/displayName required)");
        }

        String profileMetaId = requiredProfileMetaValue(profileMeta, "id", file);
        String profileMetaVersion = requiredProfileMetaValue(profileMeta, "version", file);
        String displayName = (String) profileMeta.get("displayName");
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = (String) payload.get("analyzer_name");
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = profileMetaId + " " + profileMetaVersion;
            }
            profileMeta.put("displayName", displayName);
        }
        payload.put("profileMeta", profileMeta);

        String profileJson = OBJECT_MAPPER.writeValueAsString(payload);
        String checksum = computeSha256(profileJson);

        if (analyzerProfileDAO.existsByMetaIdAndVersion(profileMetaId, profileMetaVersion)) {
            AnalyzerProfile existing = analyzerProfileDAO.findByMetaIdAndVersion(profileMetaId, profileMetaVersion);
            if (existing != null && existing.getChecksumSha256().equals(checksum)) {
                return false;
            }
            existing.setProfileJson(profileJson);
            existing.setChecksumSha256(checksum);
            existing.setDisplayName(displayName);
            existing.setLastupdatedFields();
            analyzerProfileDAO.update(existing);
            return true;
        }

        AnalyzerProfile profile = new AnalyzerProfile();
        profile.setProfileMetaId(profileMetaId);
        profile.setProfileMetaVersion(profileMetaVersion);
        profile.setDisplayName(displayName);
        profile.setSource(SOURCE_BUILT_IN);
        profile.setProfileJson(profileJson);
        profile.setChecksumSha256(checksum);
        profile.setIsMutable(false);
        profile.setIsLatest(true);
        profile.setCreatedBy("system");
        profile.setUpdatedBy("system");
        analyzerProfileDAO.insert(profile);
        return true;
    }

    private String requiredProfileMetaValue(Map<String, Object> profileMeta, String key, Path file) {
        Object value = profileMeta.get(key);
        if (value == null || value.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("Missing profileMeta." + key + " in " + file.getFileName());
        }
        return value.toString().trim();
    }

    private static String computeSha256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error computing checksum", e);
        }
    }
}
