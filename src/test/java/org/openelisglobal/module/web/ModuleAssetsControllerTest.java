package org.openelisglobal.module.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.module.api.ModuleState;
import org.openelisglobal.module.api.manifest.FrontendSection;
import org.openelisglobal.module.api.manifest.ModuleManifest;
import org.openelisglobal.module.api.manifest.Requires;
import org.openelisglobal.module.runtime.ContractVersions;
import org.openelisglobal.module.runtime.ModuleClassLoader;
import org.openelisglobal.module.runtime.ModuleRecord;
import org.openelisglobal.module.runtime.ModuleRegistry;
import org.openelisglobal.module.runtime.ModuleSystemProperties;
import org.openelisglobal.module.testkit.ModuleJarBuilder;
import org.openelisglobal.module.testkit.ModuleRecords;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class ModuleAssetsControllerTest {

    private static final String DIGEST = "0".repeat(16);
    private static final String ENTRY = "export const page = 1;";

    private MockMvc mockMvc;

    private static ModuleManifest manifest() {
        ModuleManifest manifest = new ModuleManifest();
        manifest.setId("alpha");
        manifest.setKind("module");
        manifest.setName("Alpha");
        manifest.setVersion("1.0.0");
        Requires requires = new Requires();
        requires.setOpenelis(">=3.4.0");
        requires.setFrontendContract("^1.0");
        manifest.setRequires(requires);
        FrontendSection frontend = new FrontendSection();
        frontend.setEntry("remoteEntry.js");
        manifest.setFrontend(frontend);
        return manifest;
    }

    @Before
    public void controller() throws Exception {
        Path jar = ModuleJarBuilder.module().resource("META-INF/resources/modules/alpha/remoteEntry.js", ENTRY)
                .resource("META-INF/resources/modules/alpha/assets/Page-ab12cd.js", "export default 1;")
                .build(Files.createTempDirectory("module-assets").resolve("alpha-1.0.0.jar"));
        ModuleRecord record = ModuleRecords.record(jar, manifest());
        record.setClassLoader(new ModuleClassLoader("alpha", new URL[] { jar.toUri().toURL() },
                getClass().getClassLoader(), List.of()));
        record.transition(ModuleState.STARTED);
        ModuleRegistry registry = new ModuleRegistry(new ModuleSystemProperties(new MockEnvironment()),
                new ContractVersions("3.4.0-SNAPSHOT", "1.0.0", "1.0.0"), List.of(record));
        mockMvc = MockMvcBuilders.standaloneSetup(new ModuleAssetsController(registry)).build();
    }

    @Test
    public void servesTheEntryRevalidatedOnEveryLoadWithAWeakETag() throws Exception {
        String etag = ModuleAssetsController.etag(DIGEST, "remoteEntry.js");
        assertTrue(etag, etag.startsWith("W/\""));
        mockMvc.perform(get("/modules/alpha/remoteEntry.js")).andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, etag))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache"))
                .andExpect(content().contentType("text/javascript")).andExpect(content().string(ENTRY));
    }

    @Test
    public void hashedChunksAreImmutableForAYear() throws Exception {
        mockMvc.perform(get("/modules/alpha/assets/Page-ab12cd.js")).andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=31536000, immutable"))
                .andExpect(content().string("export default 1;"));
    }

    @Test
    public void aRevalidationIsAnsweredFromTheETagBeforeTheJarIsRead() throws Exception {
        // The ETag names the running JAR's digest, so a match means the browser
        // holds this build's file: 304 without a lookup, even for a path the JAR
        // does not contain, and whether a proxy kept the ETag weak or not.
        String etag = ModuleAssetsController.etag(DIGEST, "never-built.js");
        mockMvc.perform(get("/modules/alpha/never-built.js").header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified()).andExpect(header().string(HttpHeaders.ETAG, etag));
        mockMvc.perform(get("/modules/alpha/never-built.js").header(HttpHeaders.IF_NONE_MATCH, etag.substring(2)))
                .andExpect(status().isNotModified());
        mockMvc.perform(get("/modules/alpha/never-built.js")).andExpect(status().isNotFound());
    }

    @Test
    public void anOutdatedETagGetsTheFileAgain() throws Exception {
        mockMvc.perform(get("/modules/alpha/remoteEntry.js").header(HttpHeaders.IF_NONE_MATCH, "W/\"ffff-1\""))
                .andExpect(status().isOk()).andExpect(content().string(ENTRY));
        mockMvc.perform(get("/modules/alpha/remoteEntry.js").header(HttpHeaders.IF_NONE_MATCH,
                "W/\"ffff-1\", " + ModuleAssetsController.etag(DIGEST, "remoteEntry.js")))
                .andExpect(status().isNotModified());
    }

    @Test
    public void anUnknownModuleIs404() throws Exception {
        mockMvc.perform(get("/modules/beta/remoteEntry.js")).andExpect(status().isNotFound());
    }

    @Test
    public void underTheRunningDigestEveryFileIsImmutable() throws Exception {
        // The registry publishes assetBase as /modules/<id>/<sha16>/, so a new
        // JAR is a new URL and a warm cache sends no request at all: the entry
        // and the message files are immutable there, not only the hashed chunks.
        mockMvc.perform(get("/modules/alpha/" + DIGEST + "/remoteEntry.js")).andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=31536000, immutable"))
                .andExpect(header().string(HttpHeaders.ETAG, ModuleAssetsController.etag(DIGEST, "remoteEntry.js")))
                .andExpect(content().contentType("text/javascript")).andExpect(content().string(ENTRY));
        mockMvc.perform(get("/modules/alpha/" + DIGEST + "/assets/Page-ab12cd.js")).andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=31536000, immutable"))
                .andExpect(content().string("export default 1;"));
    }

    @Test
    public void anotherDigestIs404SoAnOldUrlNeverCachesNewBytes() throws Exception {
        mockMvc.perform(get("/modules/alpha/ffffffffffffffff/remoteEntry.js")).andExpect(status().isNotFound());
        // Upper-case hex is not a digest segment: it is a directory the JAR does not
        // have
        mockMvc.perform(get("/modules/alpha/FFFFFFFFFFFFFFFF/remoteEntry.js")).andExpect(status().isNotFound());
    }

    @Test
    public void theDigestUrlStillRevalidatesFromTheETag() throws Exception {
        String etag = ModuleAssetsController.etag(DIGEST, "remoteEntry.js");
        mockMvc.perform(get("/modules/alpha/" + DIGEST + "/remoteEntry.js").header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified()).andExpect(header().string(HttpHeaders.ETAG, etag));
    }

    @Test
    public void theDigestSegmentIsParsedApartFromThePath() {
        ModuleAssetsController.AssetRequest digested = ModuleAssetsController
                .parse("/modules/ctx/" + DIGEST + "/assets/Page-ab12cd.js", "ctx");
        assertEquals(DIGEST, digested.digest());
        assertEquals("assets/Page-ab12cd.js", digested.path());
        ModuleAssetsController.AssetRequest plain = ModuleAssetsController.parse("/modules/ctx/remoteEntry.js", "ctx");
        assertNull(plain.digest());
        assertEquals("remoteEntry.js", plain.path());
        // A digest with nothing behind it, and a digest-like directory that is a real
        // path element elsewhere
        assertNull(ModuleAssetsController.parse("/modules/ctx/" + DIGEST, "ctx"));
        assertNull(ModuleAssetsController.parse("/modules/ctx/" + DIGEST + "/", "ctx"));
        assertEquals("assets/" + DIGEST + "/x.js",
                ModuleAssetsController.parse("/modules/ctx/assets/" + DIGEST + "/x.js", "ctx").path());
    }

    @Test
    public void pathBelowTheModuleDirectoryIsAccepted() {
        assertEquals("remoteEntry.js", ModuleAssetsController.parse("/modules/ctx/remoteEntry.js", "ctx").path());
        assertEquals("assets/Page-ab12cd.js",
                ModuleAssetsController.parse("/modules/ctx/assets/Page-ab12cd.js", "ctx").path());
        assertEquals("messages/en.json", ModuleAssetsController.parse("/modules/ctx/messages/en.json", "ctx").path());
    }

    @Test
    public void pathsThatLeaveTheDirectoryAreRefused() {
        assertNull(ModuleAssetsController.parse("/modules/ctx/../other/x.js", "ctx"));
        assertNull(ModuleAssetsController.parse("/modules/ctx//etc/passwd", "ctx"));
        assertNull(ModuleAssetsController.parse("/modules/ctx/", "ctx"));
        assertNull(ModuleAssetsController.parse("/modules/other/remoteEntry.js", "ctx"));
    }

    @Test
    public void mediaTypesAndCachingFollowTheFileName() {
        assertEquals(MediaType.valueOf("text/javascript"), ModuleAssetsController.mediaType("remoteEntry.js"));
        assertEquals(MediaType.APPLICATION_JSON, ModuleAssetsController.mediaType("messages/fr.json"));
        assertEquals(MediaType.valueOf("text/css"), ModuleAssetsController.mediaType("assets/style-1a2b3c.css"));
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, ModuleAssetsController.mediaType("assets/font.bin"));
        assertTrue(ModuleAssetsController.isImmutable("assets/Page-ab12cd.js"));
        assertTrue(ModuleAssetsController.isImmutable("assets/style-BxQ9kL0z.css"));
        assertFalse(ModuleAssetsController.isImmutable("remoteEntry.js"));
        assertFalse(ModuleAssetsController.isImmutable("messages/en.json"));
    }

    @Test
    public void ifNoneMatchAcceptsListsAndTheStrongForm() {
        String etag = ModuleAssetsController.etag("abc", "x.js");
        assertTrue(ModuleAssetsController.matches(etag, etag));
        assertTrue(ModuleAssetsController.matches(etag.substring(2), etag));
        assertTrue(ModuleAssetsController.matches("\"other\", " + etag, etag));
        assertTrue(ModuleAssetsController.matches("*", etag));
        assertFalse(ModuleAssetsController.matches("\"other\"", etag));
        assertFalse(ModuleAssetsController.matches(null, etag));
    }
}
