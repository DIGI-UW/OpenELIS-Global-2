package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test verifying built-in profile bootstrap loads GeneXpert ASTM
 * profile correctly.
 *
 * <p>
 * Requires projects/analyzer-defaults/astm/genexpert-astm.json to exist.
 */
public class BuiltInProfileBootstrapIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private BuiltInProfileBootstrapService builtInProfileBootstrapService;

    @Autowired
    private AnalyzerProfileService analyzerProfileService;

    @Test
    public void testBootstrap_LoadsGeneXpertAstmProfile() {
        builtInProfileBootstrapService.bootstrapBuiltInProfiles();

        var profiles = analyzerProfileService.listBySource("BUILT_IN");
        assertNotNull("Built-in profiles list should not be null", profiles);
        assertTrue("Built-in profiles should not be empty", !profiles.isEmpty());

        String metaId = readBuiltInProfileMetaId("projects/analyzer-defaults/astm/genexpert-astm.json");
        var genexpert = analyzerProfileService.getLatestByMetaId(metaId);
        assertNotNull("GeneXpert ASTM profile should be loaded", genexpert);
        assertEquals(metaId, genexpert.getProfileMetaId());
        assertEquals("1.0.0", genexpert.getProfileMetaVersion());
        assertEquals("BUILT_IN", genexpert.getSource());
        assertTrue("Built-in profile should be immutable", !genexpert.getIsMutable());
        assertNotNull("Profile JSON should be set", genexpert.getProfileJson());
        assertTrue("Profile JSON should contain profileMeta block", genexpert.getProfileJson().contains("profileMeta"));
        assertTrue("Profile JSON should contain analyzer_name",
                genexpert.getProfileJson().contains("Cepheid") || genexpert.getProfileJson().contains("GeneXpert"));
    }

    private String readBuiltInProfileMetaId(String filePath) {
        try {
            String jsonContent = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = new ObjectMapper().readValue(jsonContent, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> profileMeta = (Map<String, Object>) payload.get("profileMeta");
            return (String) profileMeta.get("id");
        } catch (Exception e) {
            throw new RuntimeException("Unable to read built-in profile metadata for test: " + filePath, e);
        }
    }
}
