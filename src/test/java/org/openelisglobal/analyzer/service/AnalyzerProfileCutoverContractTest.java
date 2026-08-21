package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.controller.AnalyzerRestController;
import org.openelisglobal.analyzer.dao.AnalyzerProfileBindingDAO;
import org.openelisglobal.analyzer.form.AnalyzerForm;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.analyzerimport.action.AnalyzerFhirImportController;
import org.springframework.web.bind.annotation.GetMapping;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerProfileCutoverContractTest {

    private static final String PROFILE_ID = "site.mock-hematology";
    private static final int PROFILE_REVISION = 3;
    private static final String FINGERPRINT = "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Mock
    private AnalyzerProfileBindingDAO bindingDAO;

    @Mock
    private BridgeProfileCatalogService catalogService;

    @Mock
    private AnalyzerSiteBindingService siteBindingService;

    private AnalyzerProfileBindingServiceImpl service;

    @Before
    public void setUp() throws Exception {
        service = new AnalyzerProfileBindingServiceImpl(bindingDAO, catalogService, siteBindingService);
        when(catalogService.getCatalog()).thenReturn(new BridgeProfileCatalog("1.0", FINGERPRINT,
                java.util.List.of(new BridgeProfileCatalog.ProfileRevision(
                        new ObjectMapper().readTree(
                                """
                                        {
                                          "profileMeta":{"id":"site.mock-hematology","version":"1.0.0","displayName":"Mock Hematology","confidence":"VALIDATED"},
                                          "protocol":{"name":"ASTM","version":"LIS2-A2"},
                                          "communication":{"mode":"ANALYZER_INITIATED","supports_lis_initiated":false},
                                          "default_test_mappings":[],
                                          "configDefaults":{"connectionRole":"SERVER","defaultTransport":"TCP/IP","aggregationMode":"PER_MESSAGE"},
                                          "catalog":{
                                            "revision":3,
                                            "revisionFingerprint":"sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                                            "source":"SITE",
                                            "status":"ACTIVE"
                                          }
                                        }
                                        """),
                        new ObjectMapper().createObjectNode()))));
    }

    @Test
    public void analyzerFormUsesExactProfileRevisionAndHasNoLegacyBootstrapHint() {
        assertTrue(hasField(AnalyzerForm.class, "profileId"));
        assertTrue(hasField(AnalyzerForm.class, "profileRevision"));
        assertFalse(hasField(AnalyzerForm.class, "defaultConfigId"));
        assertFalse("Analyzer must pin profiles only through its site-binding revision",
                hasField(Analyzer.class, "profileBinding"));
    }

    @Test
    public void assignmentPinsTheResolvedRevisionWithoutCopyingProfileContent() throws Exception {
        AnalyzerProfileBinding binding = binding();
        AnalyzerSiteBinding siteBinding = new AnalyzerSiteBinding();
        siteBinding.setProfileBinding(binding);
        AnalyzerSiteBindingRevision siteBindingRevision = new AnalyzerSiteBindingRevision();
        siteBindingRevision.setSiteBinding(siteBinding);
        when(bindingDAO.findByProfileIdAndRevision(PROFILE_ID, PROFILE_REVISION)).thenReturn(Optional.of(binding));
        when(siteBindingService.resolveInitialRevision(eq(binding), any(), eq("17")))
                .thenReturn(new AnalyzerSiteBindingSnapshot(siteBinding, siteBindingRevision, java.util.List.of(),
                        java.util.List.of()));
        Analyzer analyzer = new Analyzer();

        Method assign = AnalyzerProfileBindingServiceImpl.class.getMethod("assignProfile", Analyzer.class, String.class,
                int.class, String.class);
        Object result = invoke(assign, service, analyzer, PROFILE_ID, PROFILE_REVISION, "17");

        assertSame(binding, result);
        assertSame(siteBindingRevision, analyzer.getSiteBindingRevision());
        assertSame(binding, analyzer.getPinnedProfileBinding());
        assertFalse(Arrays.stream(Analyzer.class.getDeclaredFields())
                .anyMatch(field -> field.getName().toLowerCase().contains("profilesnapshot")));
    }

    @Test
    public void analyzerControllerHasNoFilesystemProfileCatalogHandlers() {
        for (Method method : AnalyzerRestController.class.getDeclaredMethods()) {
            GetMapping mapping = method.getAnnotation(GetMapping.class);
            if (mapping == null) {
                continue;
            }
            for (String path : mapping.value()) {
                assertFalse("Legacy filesystem profile route remains: " + path,
                        path.startsWith("/profiles") || path.startsWith("/defaults"));
            }
        }
    }

    @Test
    public void fhirImportDoesNotInferOrApplyProfilesFromFilesystem() {
        for (Method method : AnalyzerFhirImportController.class.getDeclaredMethods()) {
            String name = method.getName();
            assertFalse("FHIR import still owns profile selection: " + name, name.equals("applyMatchedProfile")
                    || name.equals("matchProfileFromDevice") || name.equals("resolveProfilesBaseDir"));
        }
    }

    @Test
    public void harnessAndPlaywrightPreconditionsUseBridgeProfilePinsWithoutFilesystemProfiles() throws Exception {
        String seed = Files.readString(Path.of("projects/analyzer-harness/seed-analyzers.sh"));
        String devCompose = Files.readString(Path.of("projects/analyzer-harness/docker-compose.dev.yml"));
        String analyzerCompose = Files
                .readString(Path.of("projects/analyzer-harness/docker-compose.analyzer-test.yml"));
        String ciCompose = Files.readString(Path.of(".github/ci/ci.analyzer-harness.yml"));
        String ciParity = Files.readString(Path.of("projects/analyzer-harness/ci-parity-test.sh"));
        String ensureAnalyzer = Files.readString(Path.of("frontend/playwright/helpers/ensure-analyzer.ts"));

        assertFalse("Harness still submits defaultConfigId", seed.contains("defaultConfigId"));
        assertFalse("Playwright fixture still submits defaultConfigId", ensureAnalyzer.contains("defaultConfigId"));
        assertTrue("Harness seed must submit an exact profile ID", seed.contains("profileId"));
        assertTrue("Harness seed must submit an exact profile revision", seed.contains("profileRevision"));
        assertFalse("Harness still reads the OpenELIS profile filesystem", seed.contains("projects/analyzer-profiles"));
        assertFalse("Harness still treats analyzer_test_map as profile authority", seed.contains("analyzer_test_map"));
        assertFalse("CI parity still treats analyzer_test_map as profile authority",
                ciParity.contains("analyzer_test_map"));
        assertFalse("Local harness still mounts OpenELIS profile files",
                devCompose.contains("projects/analyzer-profiles"));
        assertFalse("CI harness still mounts OpenELIS profile files", ciCompose.contains("projects/analyzer-profiles"));
        assertFalse("Analyzer harness still mounts OpenELIS profile files",
                analyzerCompose.contains("projects/analyzer-profiles"));
        assertTrue("Local mock must mount the versioned Bridge profile catalog",
                analyzerCompose.contains("contracts/analyzer/v1/fixtures:/data/bridge-profiles:ro"));
        assertTrue("Local mock must resolve exact Bridge profile references",
                analyzerCompose.contains("ANALYZER_BRIDGE_PROFILES_DIR=/data/bridge-profiles"));
        assertTrue("Local mock must use the configured Bridge username",
                analyzerCompose.contains("BRIDGE_USER=${BRIDGE_SECURITY_USERNAME:-admin}"));
        assertTrue("Local mock must use the configured Bridge password",
                analyzerCompose.contains("BRIDGE_PASS=${BRIDGE_SECURITY_PASSWORD:-adminADMIN!}"));
        assertTrue("CI mock must mount the versioned Bridge profile catalog",
                ciCompose.contains("contracts/analyzer/v1/fixtures:/data/bridge-profiles:ro"));
        assertTrue("CI mock must resolve exact Bridge profile references",
                ciCompose.contains("ANALYZER_BRIDGE_PROFILES_DIR=/data/bridge-profiles"));
        assertFalse("Local mock still exposes an unversioned profile root",
                analyzerCompose.contains("ANALYZER_PROFILES_DIR"));
        assertFalse("CI mock still exposes an unversioned profile root", ciCompose.contains("ANALYZER_PROFILES_DIR"));
        for (String profileId : List.of("genexpert-astm", "fluorocycler-xt", "quantstudio")) {
            assertTrue("Harness must seed priority profile " + profileId, seed.contains(profileId));
        }
        for (String obsoleteSeed : List.of("mindray-bc5380", "mindray-bs200", "wondfo-csv", "tecan-f50",
                "multiskan-fc")) {
            assertFalse("Harness still seeds non-priority profile " + obsoleteSeed, seed.contains(obsoleteSeed));
        }
    }

    @Test
    public void priorityMockTemplatesPinExactPublishedBridgeProfiles() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> expectedProfiles = Map.of("genexpert_astm", "genexpert-astm", "hain_fluorocycler",
                "fluorocycler-xt", "quantstudio5", "quantstudio", "quantstudio7", "quantstudio");

        for (Map.Entry<String, String> expected : expectedProfiles.entrySet()) {
            com.fasterxml.jackson.databind.JsonNode template = mapper
                    .readTree(Path.of("tools/analyzer-mock-server/templates", expected.getKey() + ".json").toFile());
            assertEquals(expected.getValue(), template.path("profileRef").path("profileId").asText());
            assertEquals(1, template.path("profileRef").path("revision").asInt());
            assertFalse("Priority mock still has an unversioned profile path", template.has("profile"));
            assertFalse("Priority mock duplicates profile-owned assay fields", template.has("fields"));
        }
    }

    private static Object invoke(Method method, Object target, Object... args) throws Exception {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw exception;
        }
    }

    private static boolean hasField(Class<?> type, String fieldName) {
        return Arrays.stream(type.getDeclaredFields()).anyMatch(field -> fieldName.equals(field.getName()));
    }

    private static AnalyzerProfileBinding binding() {
        AnalyzerProfileBinding binding = new AnalyzerProfileBinding();
        binding.setId("41");
        binding.setProfileId(PROFILE_ID);
        binding.setProfileRevision(PROFILE_REVISION);
        binding.setProfileFingerprint(FINGERPRINT);
        return binding;
    }
}
