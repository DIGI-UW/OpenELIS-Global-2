package org.openelisglobal.module.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openelisglobal.module.api.ModuleState;
import org.openelisglobal.module.api.OpenElisModule;
import org.openelisglobal.module.testkit.ModuleJarBuilder;
import org.springframework.mock.env.MockEnvironment;

public class ModuleDiscoveryTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private Path modules;
    private Path cache;
    private MockEnvironment environment;
    private final ContractVersions versions = new ContractVersions("3.4.0-SNAPSHOT", "1.0.0", "1.0.0");

    @Before
    public void directories() throws IOException {
        modules = temp.newFolder("modules").toPath();
        cache = temp.newFolder("cache").toPath();
        environment = new MockEnvironment();
        environment.setProperty(ModuleSystemProperties.DIRECTORY, modules.toString());
        environment.setProperty(ModuleSystemProperties.CACHE_DIRECTORY, cache.toString());
    }

    private List<ModuleRecord> discover() {
        return discover(getClass().getClassLoader());
    }

    private List<ModuleRecord> discover(ClassLoader webappLoader) {
        return new ModuleDiscovery(new ModuleSystemProperties(environment), versions, webappLoader).discover();
    }

    static String manifest(String id, String requiresOpenelis, String extra) {
        return "{\"schemaVersion\":1,\"id\":\"" + id + "\",\"kind\":\"module\",\"name\":\"" + id
                + "\",\"version\":\"1.0.0\",\"requires\":{\"openelis\":\"" + requiresOpenelis + "\"}" + extra + "}";
    }

    static String activatorSource(String pkg) {
        return "package " + pkg + ";\n"
                + "public class Activator implements org.openelisglobal.module.api.OpenElisModule {\n"
                + "  public static String hello() { return \"hello from " + pkg + "\"; }\n}\n";
    }

    private Path fixture(String id, String manifest) {
        String pkg = "org.openelisglobal.module." + id.replace('-', '_');
        return ModuleJarBuilder.module().manifest(manifest).source(pkg + ".Activator", activatorSource(pkg))
                .build(modules.resolve(id + "-1.0.0.jar"));
    }

    private static String withActivator(String id) {
        String pkg = "org.openelisglobal.module." + id.replace('-', '_');
        return ",\"backend\":{\"basePackages\":[\"" + pkg + "\"],\"activator\":\"" + pkg + ".Activator\"}";
    }

    @Test
    public void noModuleDirectoryMeansNoModules() {
        environment.setProperty(ModuleSystemProperties.DIRECTORY, modules.resolve("missing").toString());
        assertTrue(discover().isEmpty());
    }

    @Test
    public void validModuleIsLoadedThroughItsOwnClassLoader() throws Exception {
        fixture("alpha", manifest("alpha", ">=3.4.0", withActivator("alpha")));

        List<ModuleRecord> records = discover();

        assertEquals(1, records.size());
        ModuleRecord alpha = records.get(0);
        assertEquals(alpha.reason().orElse(""), ModuleState.VALIDATED, alpha.state());
        assertNotNull(alpha.classLoader());
        assertEquals(64, alpha.sha256().length());
        Class<?> activator = Class.forName("org.openelisglobal.module.alpha.Activator", true, alpha.classLoader());
        assertSame(alpha.classLoader(), activator.getClassLoader());
        assertTrue(OpenElisModule.class.isAssignableFrom(activator));
        assertSame(OpenElisModule.class, Class.forName(OpenElisModule.class.getName(), true, alpha.classLoader()));
        assertEquals("hello from org.openelisglobal.module.alpha", activator.getMethod("hello").invoke(null));
        try {
            Class.forName("org.openelisglobal.module.alpha.Activator", true, getClass().getClassLoader());
            fail("the fixture class must not be on the test class path");
        } catch (ClassNotFoundException expected) {
            // the module loader is the only way to it
        }
    }

    @Test
    public void manifestProblemsMakeTheModuleInvalid() {
        fixture("broken", "{\"schemaVersion\":1,\"id\":\"broken\",\"kind\":\"module\",\"name\":\"x\","
                + "\"version\":\"1.0.0\",\"requires\":{}}");
        ModuleJarBuilder.module().build(modules.resolve("no-manifest.jar"));

        List<ModuleRecord> records = discover();

        assertEquals(2, records.size());
        ModuleRecord broken = find(records, "broken");
        assertEquals(ModuleState.INVALID, broken.state());
        assertTrue(broken.reason().get(), broken.reason().get().contains("openelis"));
        ModuleRecord noManifest = find(records, "no-manifest");
        assertEquals(ModuleState.INVALID, noManifest.state());
        assertTrue(noManifest.reason().get().contains("openelis-module.json"));
    }

    @Test
    public void incompatibleRangesAreSkipped() {
        fixture("future", manifest("future", ">=9.0.0", withActivator("future")));
        fixture("spi-major", manifest("spi-major", ">=3.4.0",
                ",\"backend\":{\"basePackages\":[\"org.openelisglobal.module.spi_major\"],\"activator\":\"org.openelisglobal.module.spi_major.Activator\","
                        + "\"extensions\":[{\"spi\":\"report\",\"class\":\"org.openelisglobal.module.spi_major.Activator\"}]}")
                .replace("\"requires\":{\"openelis\":\">=3.4.0\"}",
                        "\"requires\":{\"openelis\":\">=3.4.0\",\"spi\":\"^2\"}"));

        List<ModuleRecord> records = discover();

        ModuleRecord future = find(records, "future");
        assertEquals(ModuleState.INCOMPATIBLE, future.state());
        assertTrue(future.reason().get(), future.reason().get().contains("9.0.0"));
        ModuleRecord spiMajor = find(records, "spi-major");
        assertEquals(ModuleState.INCOMPATIBLE, spiMajor.state());
        assertTrue(spiMajor.reason().get(), spiMajor.reason().get().contains("openelis-spi"));
        assertNull(future.classLoader());
    }

    @Test
    public void markerFileAndPropertiesDisableAModule() throws IOException {
        Path marked = fixture("marked", manifest("marked", ">=3.4.0", withActivator("marked")));
        Files.writeString(marked.resolveSibling(marked.getFileName() + ".disabled"), "");
        fixture("listed", manifest("listed", ">=3.4.0", withActivator("listed")));
        fixture("switched", manifest("switched", ">=3.4.0", withActivator("switched")));
        environment.setProperty(ModuleSystemProperties.DISABLED, "listed");
        environment.setProperty("modules.switched.enabled", "false");

        List<ModuleRecord> records = discover();

        assertEquals(ModuleState.DISABLED, find(records, "marked").state());
        assertEquals(ModuleState.DISABLED, find(records, "listed").state());
        assertEquals(ModuleState.DISABLED, find(records, "switched").state());
    }

    @Test
    public void twoJarsWithOneIdAbortStartUp() {
        fixture("twin", manifest("twin", ">=3.4.0", withActivator("twin")));
        ModuleJarBuilder.module().manifest(manifest("twin", ">=3.4.0", withActivator("twin")))
                .source("org.openelisglobal.module.twin.Activator", activatorSource("org.openelisglobal.module.twin"))
                .build(modules.resolve("twin-copy.jar"));

        try {
            discover();
            fail("expected a start-up error");
        } catch (ModuleStartupException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("twin"));
            assertTrue(e.getMessage(), e.getMessage().contains("twin-copy.jar"));
        }
    }

    @Test
    public void dependenciesOrderConflictsAndCyclesAreResolved() {
        fixture("base", manifest("base", ">=3.4.0", withActivator("base")));
        fixture("dependent", manifest("dependent", ">=3.4.0", withActivator("dependent")).replace(
                "\"requires\":{\"openelis\":\">=3.4.0\"}",
                "\"requires\":{\"openelis\":\">=3.4.0\",\"modules\":[{\"id\":\"base\",\"version\":\"^1.0\"}]}"));
        fixture("orphan", manifest("orphan", ">=3.4.0", withActivator("orphan")).replace(
                "\"requires\":{\"openelis\":\">=3.4.0\"}",
                "\"requires\":{\"openelis\":\">=3.4.0\",\"modules\":[{\"id\":\"nowhere\",\"version\":\"^1\"}]}"));
        fixture("too-new", manifest("too-new", ">=3.4.0", withActivator("too-new")).replace(
                "\"requires\":{\"openelis\":\">=3.4.0\"}",
                "\"requires\":{\"openelis\":\">=3.4.0\",\"modules\":[{\"id\":\"base\",\"version\":\"^2.0\"}]}"));
        fixture("rival", manifest("rival", ">=3.4.0", withActivator("rival") + ",\"conflicts\":[\"base\"]"));
        fixture("cycle-a", manifest("cycle-a", ">=3.4.0", withActivator("cycle-a")).replace(
                "\"requires\":{\"openelis\":\">=3.4.0\"}",
                "\"requires\":{\"openelis\":\">=3.4.0\",\"modules\":[{\"id\":\"cycle-b\",\"version\":\"*\"}]}"));
        fixture("cycle-b", manifest("cycle-b", ">=3.4.0", withActivator("cycle-b")).replace(
                "\"requires\":{\"openelis\":\">=3.4.0\"}",
                "\"requires\":{\"openelis\":\">=3.4.0\",\"modules\":[{\"id\":\"cycle-a\",\"version\":\"*\"}]}"));

        List<ModuleRecord> records = discover();

        assertEquals(ModuleState.VALIDATED, find(records, "base").state());
        assertEquals(ModuleState.VALIDATED, find(records, "dependent").state());
        assertTrue(indexOf(records, "base") < indexOf(records, "dependent"));
        assertEquals(find(records, "base").classLoader(),
                find(records, "dependent").classLoader().getDependencies().get(0));
        assertEquals(ModuleState.INCOMPATIBLE, find(records, "orphan").state());
        assertTrue(find(records, "orphan").reason().get().contains("nowhere"));
        assertEquals(ModuleState.INCOMPATIBLE, find(records, "too-new").state());
        assertEquals(ModuleState.INCOMPATIBLE, find(records, "rival").state());
        assertTrue(find(records, "rival").reason().get().contains("conflicts"));
        assertEquals(ModuleState.INCOMPATIBLE, find(records, "cycle-a").state());
        assertEquals(ModuleState.INCOMPATIBLE, find(records, "cycle-b").state());
        assertTrue(find(records, "cycle-a").reason().get().contains("cycle"));
    }

    @Test
    public void declaredClassesMustLoadFromTheModuleAndMatchTheirRole() {
        String pkg = "org.openelisglobal.module.wrong";
        ModuleJarBuilder.module()
                .manifest(manifest("wrong", ">=3.4.0",
                        ",\"backend\":{\"basePackages\":[\"" + pkg + "\"]," + "\"activator\":\"" + pkg
                                + ".NotAnActivator\",\"entities\":[\"" + pkg + ".Missing\"],"
                                + "\"liquibase\":\"liquibase/wrong/changelog.xml\"}"))
                .source(pkg + ".NotAnActivator", "package " + pkg + "; public class NotAnActivator {}")
                .build(modules.resolve("wrong-1.0.0.jar"));
        ModuleJarBuilder.module().manifest(manifest("core-class", ">=3.4.0",
                ",\"backend\":{\"basePackages\":[\"org.openelisglobal.module.core\"],"
                        + "\"activator\":\"org.openelisglobal.module.runtime.ModuleDiscoveryTest$CoreActivator\"}"))
                .build(modules.resolve("core-class-1.0.0.jar"));

        List<ModuleRecord> records = discover();

        ModuleRecord wrong = find(records, "wrong");
        assertEquals(ModuleState.INVALID, wrong.state());
        String reason = wrong.reason().get();
        assertTrue(reason, reason.contains("does not implement OpenElisModule"));
        assertTrue(reason, reason.contains("Missing"));
        assertTrue(reason, reason.contains("changelog"));
        ModuleRecord coreClass = find(records, "core-class");
        assertEquals(ModuleState.INVALID, coreClass.state());
        assertTrue(coreClass.reason().get(), coreClass.reason().get().contains("belongs to the core"));
    }

    @Test
    public void frontendModuleNeedsItsPrebuiltBundle() {
        String withFrontend = ",\"backend\":{\"basePackages\":[\"org.openelisglobal.module.uix\"]},\"frontend\":{\"routes\":[{\"path\":"
                + "\"/m/uix/home\",\"expose\":\"./Home\",\"role\":\"RESULTS\"}]}";
        String requires = "\"requires\":{\"openelis\":\">=3.4.0\",\"frontendContract\":\"^1.0\"}";
        ModuleJarBuilder
                .module().manifest(manifest("uix", ">=3.4.0", withFrontend)
                        .replace("\"requires\":{\"openelis\":\">=3.4.0\"}", requires))
                .build(modules.resolve("uix-1.0.0.jar"));
        ModuleJarBuilder.module()
                .manifest(manifest("uix-ok", ">=3.4.0",
                        withFrontend.replace("org.openelisglobal.module.uix", "org.openelisglobal.module.uixok")
                                .replace("/m/uix/", "/m/uix-ok/"))
                        .replace("\"requires\":{\"openelis\":\">=3.4.0\"}", requires))
                .resource("META-INF/resources/modules/uix-ok/remoteEntry.js", "export const ok = true;")
                .resource("META-INF/resources/modules/uix-ok/messages/en.json", "{}")
                .build(modules.resolve("uix-ok-1.0.0.jar"));

        List<ModuleRecord> records = discover();

        ModuleRecord ui = find(records, "uix");
        assertEquals(ModuleState.INVALID, ui.state());
        assertTrue(ui.reason().get(), ui.reason().get().contains("remoteEntry.js"));
        assertEquals(ModuleState.VALIDATED, find(records, "uix-ok").state());
    }

    @Test
    public void strictModeRefusesUnlistedAndTamperedJars() throws IOException {
        Path listed = fixture("listed", manifest("listed", ">=3.4.0", withActivator("listed")));
        fixture("unlisted", manifest("unlisted", ">=3.4.0", withActivator("unlisted")));
        fixture("tampered", manifest("tampered", ">=3.4.0", withActivator("tampered")));
        String digest = ModuleDiscovery.sha256(listed);
        Files.writeString(modules.resolve("modules.lock"),
                "# allowed modules\nlisted@1.0.0 " + digest
                        + "\ntampered@1.0.0 0000000000000000000000000000000000000000000000000000000000000000\n",
                StandardCharsets.UTF_8);

        List<ModuleRecord> records = discover();

        assertEquals(ModuleState.VALIDATED, find(records, "listed").state());
        assertEquals(ModuleState.INVALID, find(records, "unlisted").state());
        assertTrue(find(records, "unlisted").reason().get().contains("modules.lock"));
        assertEquals(ModuleState.INVALID, find(records, "tampered").state());
        assertTrue(find(records, "tampered").reason().get().contains("digest"));
    }

    @Test
    public void bundledLibrariesAreExtractedAndShadowedOnesReported() throws Exception {
        Path library = ModuleJarBuilder.library(temp.newFolder("lib").toPath().resolve("extra-lib-2.0.0.jar"),
                Map.of("extra.Helper",
                        "package extra; public class Helper { public static String name() { return \"extra\"; } }"));
        ModuleJarBuilder.module().manifest(manifest("libs", ">=3.4.0", withActivator("libs")))
                .source("org.openelisglobal.module.libs.Activator", activatorSource("org.openelisglobal.module.libs"))
                .library("extra-lib-2.0.0.jar", library).library("commons-csv-1.12.0.jar", library)
                .build(modules.resolve("libs-1.0.0.jar"));
        URL coreJar = temp.newFile("commons-csv-1.10.0.jar").toURI().toURL();
        ClassLoader webappLoader = new URLClassLoader(new URL[] { coreJar }, getClass().getClassLoader());

        List<ModuleRecord> records = discover(webappLoader);

        ModuleRecord libs = find(records, "libs");
        assertEquals(libs.reason().orElse(""), ModuleState.VALIDATED, libs.state());
        assertEquals(List.of("extra-lib-2.0.0.jar", "commons-csv-1.12.0.jar"), libs.bundledLibraries());
        assertEquals(1, libs.shadowedLibraries().size());
        assertEquals("commons-csv-1.12.0.jar", libs.shadowedLibraries().get(0).bundled());
        assertEquals("commons-csv-1.10.0.jar", libs.shadowedLibraries().get(0).coreCopy());
        assertTrue(Files.isRegularFile(cache.resolve("libs").resolve(libs.sha256()).resolve("extra-lib-2.0.0.jar")));
        Class<?> helper = Class.forName("extra.Helper", true, libs.classLoader());
        assertNotSame(getClass().getClassLoader(), helper.getClassLoader());
        assertEquals("extra", helper.getMethod("name").invoke(null));
    }

    private static ModuleRecord find(List<ModuleRecord> records, String id) {
        return records.stream().filter(record -> record.id().equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("no record " + id + " among " + records));
    }

    private static int indexOf(List<ModuleRecord> records, String id) {
        return records.indexOf(find(records, id));
    }

    /** A core class named as an activator; the registry must refuse it. */
    @Test
    public void twoModulesClaimingOnePackageAreBothRefused() {
        String shared = ",\"backend\":{\"basePackages\":[\"org.openelisglobal.module.shared\"]}";
        String nested = ",\"backend\":{\"basePackages\":[\"org.openelisglobal.module.shared.deeper\"]}";
        ModuleJarBuilder.module().manifest(manifest("claim-a", ">=3.4.0", shared))
                .build(modules.resolve("claim-a-1.0.0.jar"));
        ModuleJarBuilder.module().manifest(manifest("claim-b", ">=3.4.0", nested))
                .build(modules.resolve("claim-b-1.0.0.jar"));

        List<ModuleRecord> records = discover();

        ModuleRecord first = find(records, "claim-a");
        ModuleRecord second = find(records, "claim-b");
        assertEquals(ModuleState.INCOMPATIBLE, first.state());
        assertEquals(ModuleState.INCOMPATIBLE, second.state());
        assertTrue(first.reason().get(), first.reason().get().contains("also claimed by module claim-b"));
        assertTrue(second.reason().get(), second.reason().get().contains("also claimed by module claim-a"));
    }

    public static class CoreActivator implements OpenElisModule {
    }
}
