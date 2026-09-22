package org.openelisglobal.configuration.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.configuration.service.CatalogImportService.FilePlan;
import org.openelisglobal.configuration.service.CatalogImportService.ImportPlan;
import org.openelisglobal.configuration.valueholder.ConfigurationImportRun;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * The apply step of the Import Catalog page (OGC-1228): every file is stored
 * before any is loaded, each file is read on its own, and a file the loader
 * never found, skipped or rejected is reported as an error rather than as a row
 * of zero counts.
 */
public class CatalogImportServiceImplTest {

    /** A loader that only remembers the summary the fake reload hands it. */
    private static final class FakeHandler implements DomainConfigurationHandler {
        private final String domain;
        private final int order;
        volatile CsvLoadSummary lastSummary;

        FakeHandler(String domain, int order) {
            this.domain = domain;
            this.order = order;
        }

        @Override
        public String getDomainName() {
            return domain;
        }

        @Override
        public String getFileExtension() {
            return "csv";
        }

        @Override
        public int getLoadOrder() {
            return order;
        }

        @Override
        public boolean supportsDryRun() {
            return true;
        }

        @Override
        public CsvLoadSummary getLastSummary() {
            return lastSummary;
        }

        @Override
        public void processConfiguration(InputStream inputStream, String fileName) {
        }
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private final FakeHandler sections = new FakeHandler("test-sections", 100);
    private final FakeHandler tests = new FakeHandler("tests", 200);

    private ConfigurationInitializationService initializationService;
    private ConfigurationImportRunService importRunService;
    private UnresolvedReferenceService unresolvedReferenceService;
    private CatalogImportServiceImpl service;

    @Before
    public void setUp() {
        initializationService = mock(ConfigurationInitializationService.class);
        importRunService = mock(ConfigurationImportRunService.class);
        unresolvedReferenceService = mock(UnresolvedReferenceService.class);

        service = new CatalogImportServiceImpl();
        ReflectionTestUtils.setField(service, "configurationBaseDir", tempFolder.getRoot().getAbsolutePath());
        ReflectionTestUtils.setField(service, "instanceId", null);
        ReflectionTestUtils.setField(service, "domainHandlers", List.of(tests, sections));
        ReflectionTestUtils.setField(service, "initializationService", initializationService);
        ReflectionTestUtils.setField(service, "importRunService", importRunService);
        ReflectionTestUtils.setField(service, "unresolvedReferenceService", unresolvedReferenceService);

        ConfigurationImportRun run = new ConfigurationImportRun();
        run.setId("run-1");
        when(importRunService.start(anyString(), anyString())).thenReturn(run);
        when(unresolvedReferenceService.getOpen()).thenReturn(List.of());
    }

    @Test
    public void apply_storesEveryFileBeforeLoadingAndReadsEachOnItsOwn() throws Exception {
        doAnswer(invocation -> loadStoredFile(invocation.getArgument(0))).when(initializationService)
                .reload(any(ConfigurationReloadOptions.class));

        ImportPlan plan = service.apply(
                List.of(csv("tests-c.csv", 3), csv("test-sections-a.csv", 2), csv("test-sections-b.csv", 1)), null,
                "7");

        assertEquals(List.of("test-sections-a.csv", "test-sections-b.csv", "tests-c.csv"),
                plan.files().stream().map(FilePlan::fileName).toList());
        assertEquals(List.of(2, 1, 3), plan.files().stream().map(FilePlan::created).toList());
        assertTrue("every file is loaded without an error",
                plan.files().stream().allMatch(file -> file.error() == null));
        assertTrue(Files.exists(stored("test-sections", "test-sections-a.csv")));
        assertTrue(Files.exists(stored("test-sections", "test-sections-b.csv")));
        assertTrue(Files.exists(stored("tests", "tests-c.csv")));

        ArgumentCaptor<ConfigurationReloadOptions> options = ArgumentCaptor.forClass(ConfigurationReloadOptions.class);
        verify(initializationService, times(3)).reload(options.capture());
        assertEquals(Set.of("test-sections"), options.getAllValues().get(0).domains());
        assertEquals(Set.of("test-sections-a.csv"), options.getAllValues().get(0).forcedFiles());
        assertEquals(Set.of("test-sections-b.csv"), options.getAllValues().get(1).forcedFiles());
        assertEquals(Set.of("tests"), options.getAllValues().get(2).domains());
        assertEquals(Set.of("tests-c.csv"), options.getAllValues().get(2).forcedFiles());

        ArgumentCaptor<String> summary = ArgumentCaptor.forClass(String.class);
        verify(importRunService).finish(any(ConfigurationImportRun.class), summary.capture(), eq(false));
        assertTrue(summary.getValue(), summary.getValue().contains("test-sections/test-sections-a.csv: created=2"));
    }

    @Test
    public void apply_reportsAFileTheLoaderNeverSawAsAnError() {
        when(initializationService.reload(any(ConfigurationReloadOptions.class)))
                .thenReturn(new ConfigurationReloadResult(List.of()));

        ImportPlan plan = service.apply(List.of(csv("test-sections-a.csv", 1)), null, "7");

        FilePlan file = plan.files().get(0);
        assertNotNull("a file nobody loaded is an error, not a row of zeroes", file.error());
        assertTrue(file.error(), file.error().contains("did not find test-sections-a.csv"));
        assertTrue(file.error(), file.error().contains(stored("test-sections", "").getParent().toString()));
        verify(importRunService).finish(any(ConfigurationImportRun.class), anyString(), eq(true));
    }

    @Test
    public void apply_passesTheLoadersOwnComplaintThrough() {
        when(initializationService.reload(any(ConfigurationReloadOptions.class)))
                .thenReturn(new ConfigurationReloadResult(List.of(ConfigurationReloadFileResult.error("test-sections",
                        "test-sections-a.csv", "must have a 'testSectionName' column"))));

        ImportPlan plan = service.apply(List.of(csv("test-sections-a.csv", 1)), null, "7");

        assertEquals("must have a 'testSectionName' column", plan.files().get(0).error());
    }

    @Test
    public void apply_reportsADomainWideFailureAgainstTheFile() {
        when(initializationService.reload(any(ConfigurationReloadOptions.class))).thenReturn(
                new ConfigurationReloadResult(List.of(ConfigurationReloadFileResult.error("tests", null, "boom"))));

        ImportPlan plan = service.apply(List.of(csv("tests-a.csv", 1)), null, "7");

        assertEquals("boom", plan.files().get(0).error());
    }

    @Test
    public void apply_reportsALoadedFileWithoutCountsAsAnError() {
        when(initializationService.reload(any(ConfigurationReloadOptions.class)))
                .thenReturn(new ConfigurationReloadResult(
                        List.of(ConfigurationReloadFileResult.processed("tests", "tests-a.csv"))));

        ImportPlan plan = service.apply(List.of(csv("tests-a.csv", 1)), null, "7");

        assertTrue(plan.files().get(0).error(), plan.files().get(0).error().contains("reported no row counts"));
    }

    @Test
    public void apply_refusesTheWholeBatchWhenAFileCannotBeStored() throws Exception {
        File notADirectory = tempFolder.newFile("configuration");
        ReflectionTestUtils.setField(service, "configurationBaseDir", notADirectory.getAbsolutePath());

        try {
            service.apply(List.of(csv("test-sections-a.csv", 1)), null, "7");
            fail("a file that cannot be kept fails the apply");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("Could not save test-sections-a.csv to "));
        }

        verify(initializationService, never()).reload(any(ConfigurationReloadOptions.class));
        verify(importRunService).finish(any(ConfigurationImportRun.class), eq(""), eq(true));
    }

    @Test
    public void apply_removesWhatItAddedWhenALaterFileCannotBeStored() throws Exception {
        Path kept = stored("test-sections", "test-sections-kept.csv");
        Files.createDirectories(kept.getParent());
        Files.writeString(kept, "testSectionName\nOld\n");
        tempFolder.newFile("tests");

        try {
            service.apply(
                    List.of(csv("test-sections-new.csv", 1), csv("test-sections-kept.csv", 2), csv("tests-x.csv", 1)),
                    null, "7");
            fail("the tests directory is a file, so the third copy fails");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("tests-x.csv"));
        }

        assertFalse("the file this batch added is removed again",
                Files.exists(stored("test-sections", "test-sections-new.csv")));
        assertTrue("a file that replaced an earlier version stays", Files.exists(kept));
        assertNull(sections.lastSummary);
    }

    /**
     * Stands in for the configuration reload: reads the file the options name from
     * the tree the service wrote to, counts its data rows as created, and hands the
     * summary to that domain's handler, as the real loaders do.
     */
    private ConfigurationReloadResult loadStoredFile(ConfigurationReloadOptions options) throws Exception {
        String domain = options.domains().iterator().next();
        String fileName = options.forcedFiles().iterator().next();
        Path file = stored(domain, fileName);
        if (!Files.exists(file)) {
            return new ConfigurationReloadResult(List.of());
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        CsvLoadSummary summary = new CsvLoadSummary(domain, fileName);
        for (int line = 1; line < lines.size(); line++) {
            summary.record(LoadedRow.created(lines.get(line)), "test", line + 1);
        }
        (domain.equals("tests") ? tests : sections).lastSummary = summary;
        return new ConfigurationReloadResult(List.of(ConfigurationReloadFileResult.processed(domain, fileName)));
    }

    private Path stored(String domain, String fileName) {
        return tempFolder.getRoot().toPath().resolve(domain).resolve(fileName);
    }

    private static MultipartFile csv(String fileName, int dataRows) {
        List<String> lines = new ArrayList<>();
        lines.add("name");
        for (int row = 1; row <= dataRows; row++) {
            lines.add("row " + row);
        }
        return new MockMultipartFile("files", fileName, "text/csv",
                (String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8));
    }
}
