package org.openelisglobal.addon.packaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.addon.AddonTestPackages;

public class AddonManifestTest {

    private static List<String> problems(String manifest) {
        Map<String, byte[]> files = AddonTestPackages.files(manifest);
        return AddonManifest.parse(manifest).problems(files);
    }

    private static String valid() {
        return AddonTestPackages.manifest("hello", "example", "[\"orders.read\",\"results.read\"]",
                "[\"result.validated\"]");
    }

    @Test
    public void aValidManifestHasNoProblems() {
        assertEquals(List.of(), problems(valid()));
        AddonManifest m = AddonManifest.fromPackage(AddonTestPackages.files(valid()));
        assertEquals("hello", m.id());
        assertEquals(List.of("result.validated"), m.events());
        assertEquals("result-lookup-side-panel-slot", m.extensions().get(0).slot());
        assertEquals(200, m.extensions().get(0).height());
    }

    @Test
    public void anEventNeedsItsScope() {
        String m = AddonTestPackages.manifest("hello", "example", "[\"orders.read\"]", "[\"result.validated\"]");
        assertEquals(List.of("event result.validated needs scope results.read"), problems(m));
    }

    @Test
    public void unknownScopesEventsAndSlotsAreRefused() {
        String m = valid().replace("\"orders.read\"", "\"patients.write\"").replace("result-lookup-side-panel-slot",
                "header-slot");
        List<String> p = problems(m);
        assertTrue(p.toString(), p.contains("unknown scope patients.write"));
        assertTrue(p.toString(), p.contains("extension hello-panel: unknown slot header-slot"));
        assertTrue(problems(valid().replace("\"result.validated\"", "\"sample.deleted\""))
                .contains("unknown event sample.deleted"));
    }

    @Test
    public void theImageMustBePinnedByDigestAndComeFromARegistry() {
        assertTrue(problems(valid().replace(AddonTestPackages.IMAGE, "registry.example.org/hello:latest"))
                .contains("image.ref must be an image pinned by digest (registry/name@sha256:<64 hex digits>)"));
        String archive = valid().replace("\"image\":{\"ref\":\"" + AddonTestPackages.IMAGE + "\"}",
                "\"image\":{\"archive\":\"image.tar\"}");
        assertTrue(problems(archive)
                .contains("image.archive is not supported: publish the image to a registry and give image.ref"));
    }

    @Test
    public void anExtensionPageMustBeInThePackage() {
        assertTrue(problems(valid().replace("panel.html", "missing.html"))
                .contains("extension hello-panel: page ui/missing.html is not in the package"));
    }

    @Test
    public void uiFilesMustHaveAnAllowedType() {
        Map<String, byte[]> files = AddonTestPackages.files(valid());
        files.put("ui/run.sh", "#!/bin/sh".getBytes(StandardCharsets.UTF_8));
        assertTrue(AddonManifest.parse(valid()).problems(files)
                .contains("ui/run.sh: file type not allowed in the UI folder"));
    }

    @Test
    public void badIdentifiersAreRefused() {
        assertTrue(problems(valid().replace("\"id\":\"hello\"", "\"id\":\"Hello World\"")).get(0).startsWith("id:"));
        assertTrue(problems(valid().replace("\"version\":\"1.0.0\"", "\"version\":\"v1\""))
                .contains("version must be a semantic version (1.2.3)"));
        assertTrue(problems(valid().replace("\"schemaVersion\":1", "\"schemaVersion\":2"))
                .contains("schemaVersion must be 1"));
    }

    @Test
    public void fromPackageRefusesWithEveryProblemListed() {
        String bad = AddonTestPackages.manifest("hello", "example", "[]", "[\"order.created\"]").replace("1.0.0", "x");
        AddonPackageException e = assertThrows(AddonPackageException.class,
                () -> AddonManifest.fromPackage(AddonTestPackages.files(bad)));
        assertEquals("manifest.invalid", e.getCode());
        assertEquals(2, e.getProblems().size());
    }

    @Test
    public void configValuesAreCheckedAgainstTheSchema() {
        AddonManifest m = AddonManifest.parse(valid());
        assertEquals(Map.of("greeting", "Hi", "limit", 5.0), m.validateConfig(Map.of()));
        assertEquals(Map.of("greeting", "Hello", "limit", 7.0),
                m.validateConfig(Map.of("greeting", "Hello", "limit", "7")));
        assertEquals("limit",
                assertThrows(IllegalArgumentException.class, () -> m.validateConfig(Map.of("limit", 51))).getMessage());
        assertEquals("other",
                assertThrows(IllegalArgumentException.class, () -> m.validateConfig(Map.of("other", 1))).getMessage());
    }
}
