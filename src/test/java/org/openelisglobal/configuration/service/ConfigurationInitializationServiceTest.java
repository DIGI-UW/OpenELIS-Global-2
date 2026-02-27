package org.openelisglobal.configuration.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationInitializationServiceTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private PathMatchingResourcePatternResolver resolver;

    /** Real resolver used to handle file: patterns against the temp folder. */
    private final PathMatchingResourcePatternResolver realResolver = new PathMatchingResourcePatternResolver();

    @InjectMocks
    private ConfigurationInitializationService service;

    private DomainConfigurationHandler mockHandler;
    private ContextRefreshedEvent mockEvent;

    @Before
    public void setUp() throws Exception {
        mockHandler = mock(DomainConfigurationHandler.class);
        when(mockHandler.getDomainName()).thenReturn("tests");
        when(mockHandler.getLoadOrder()).thenReturn(100);
        when(mockHandler.getFileMatcher()).thenReturn("*.csv");

        mockEvent = mock(ContextRefreshedEvent.class);

        ReflectionTestUtils.setField(service, "configurationBaseDir", tempFolder.getRoot().getAbsolutePath());
        ReflectionTestUtils.setField(service, "autocreateOn", true);
        ReflectionTestUtils.setField(service, "instanceId", null);
        ReflectionTestUtils.setField(service, "initialized", false);
        service.setDomainHandlers(Arrays.asList(mockHandler));

        // Delegate file: patterns to the real resolver (for filesystem tests);
        // return empty for everything else (classpath patterns can be overridden
        // per-test).
        when(resolver.getResources(anyString())).thenAnswer(invocation -> {
            String pattern = invocation.getArgument(0);
            if (pattern.startsWith("file:")) {
                return realResolver.getResources(pattern);
            }
            return new Resource[] {};
        });
    }

    // -- Skipping / early-exit tests --

    @Test
    public void onApplicationEvent_shouldSkipLoadingWhenAutocreateDisabled() throws Exception {
        ReflectionTestUtils.setField(service, "autocreateOn", false);

        service.onApplicationEvent(mockEvent);

        verify(resolver, never()).getResources(anyString());
        verify(mockHandler, never()).processConfiguration(any(), anyString());
    }

    @Test
    public void onApplicationEvent_shouldSkipLoadingWhenHandlersNull() throws Exception {
        service.setDomainHandlers(null);

        service.onApplicationEvent(mockEvent);

        verify(resolver, never()).getResources(anyString());
    }

    @Test
    public void onApplicationEvent_shouldSkipLoadingWhenHandlersEmpty() throws Exception {
        service.setDomainHandlers(Collections.emptyList());

        service.onApplicationEvent(mockEvent);

        verify(resolver, never()).getResources(anyString());
    }

    // -- Initialized guard --

    @Test
    public void onApplicationEvent_shouldOnlyProcessOnceWhenFiredMultipleTimes() throws Exception {
        Resource resource = createMockResource("data.csv", "content");
        when(resolver.getResources("classpath*:configuration/tests/*.csv")).thenReturn(new Resource[] { resource });

        service.onApplicationEvent(mockEvent);
        service.onApplicationEvent(mockEvent);

        // Handler is called only once — the second event is short-circuited by the
        // initialized guard before any handler processing occurs
        verify(mockHandler, times(1)).processConfiguration(any(InputStream.class), eq("data.csv"));
    }

    // -- Handler ordering --

    @Test
    public void onApplicationEvent_shouldProcessHandlersInLoadOrder() throws Exception {
        DomainConfigurationHandler laterHandler = mock(DomainConfigurationHandler.class);
        when(laterHandler.getDomainName()).thenReturn("roles");
        when(laterHandler.getLoadOrder()).thenReturn(300);
        when(laterHandler.getFileMatcher()).thenReturn("*.csv");

        DomainConfigurationHandler earlyHandler = mock(DomainConfigurationHandler.class);
        when(earlyHandler.getDomainName()).thenReturn("sample-types");
        when(earlyHandler.getLoadOrder()).thenReturn(50);
        when(earlyHandler.getFileMatcher()).thenReturn("*.csv");

        // Provide resources so processConfiguration gets called
        Resource earlyResource = createMockResource("samples.csv", "sample data");
        Resource laterResource = createMockResource("roles.csv", "role data");
        when(resolver.getResources("classpath*:configuration/sample-types/*.csv"))
                .thenReturn(new Resource[] { earlyResource });
        when(resolver.getResources("classpath*:configuration/roles/*.csv"))
                .thenReturn(new Resource[] { laterResource });

        // Deliberately add in reverse order to verify sorting
        service.setDomainHandlers(Arrays.asList(laterHandler, earlyHandler));

        service.onApplicationEvent(mockEvent);

        InOrder order = inOrder(earlyHandler, laterHandler);
        order.verify(earlyHandler).processConfiguration(any(), eq("samples.csv"));
        order.verify(laterHandler).processConfiguration(any(), eq("roles.csv"));
    }

    @Test
    public void setDomainHandlers_shouldSortByLoadOrder() throws Exception {
        DomainConfigurationHandler handler200 = mock(DomainConfigurationHandler.class);
        when(handler200.getLoadOrder()).thenReturn(200);

        DomainConfigurationHandler handler50 = mock(DomainConfigurationHandler.class);
        when(handler50.getLoadOrder()).thenReturn(50);

        // Set handlers in reverse order — setDomainHandlers sorts them
        service.setDomainHandlers(Arrays.asList(handler200, handler50));

        // Verify domainHandlers is sorted after setter injection
        @SuppressWarnings("unchecked")
        java.util.List<DomainConfigurationHandler> handlers = (java.util.List<DomainConfigurationHandler>) ReflectionTestUtils
                .getField(service, "domainHandlers");
        assertEquals("First handler should have lower load order", 50, handlers.get(0).getLoadOrder());
        assertEquals("Second handler should have higher load order", 200, handlers.get(1).getLoadOrder());
    }

    // -- Basic classpath loading --

    @Test
    public void loadDomainConfiguration_shouldProcessClasspathResource() throws Exception {
        Resource resource = createMockResource("test-data.csv", "header\nrow1\n");
        when(resolver.getResources("classpath*:configuration/tests/*.csv")).thenReturn(new Resource[] { resource });

        service.onApplicationEvent(mockEvent);

        verify(mockHandler).processConfiguration(any(InputStream.class), eq("test-data.csv"));
    }

    @Test
    public void loadDomainConfiguration_shouldProcessAllClasspathResources() throws Exception {
        Resource resource1 = createMockResource("file1.csv", "data1");
        Resource resource2 = createMockResource("file2.csv", "data2");
        when(resolver.getResources("classpath*:configuration/tests/*.csv"))
                .thenReturn(new Resource[] { resource1, resource2 });

        service.onApplicationEvent(mockEvent);

        verify(mockHandler).processConfiguration(any(InputStream.class), eq("file1.csv"));
        verify(mockHandler).processConfiguration(any(InputStream.class), eq("file2.csv"));
    }

    @Test
    public void loadDomainConfiguration_shouldSkipResourceWhenFilenameNull() throws Exception {
        Resource nullNameResource = mock(Resource.class);
        when(nullNameResource.getFilename()).thenReturn(null);

        Resource validResource = createMockResource("valid.csv", "data");
        when(resolver.getResources("classpath*:configuration/tests/*.csv"))
                .thenReturn(new Resource[] { nullNameResource, validResource });

        service.onApplicationEvent(mockEvent);

        verify(mockHandler).processConfiguration(any(InputStream.class), eq("valid.csv"));
        verify(mockHandler, times(1)).processConfiguration(any(), anyString());
    }

    // -- Basic filesystem loading --

    @Test
    public void loadDomainConfiguration_shouldProcessFilesystemFile() throws Exception {
        createTestFile("tests/lab-tests.csv", "header\nrow1\n");

        service.onApplicationEvent(mockEvent);

        verify(mockHandler).processConfiguration(any(InputStream.class), eq("lab-tests.csv"));
    }

    @Test
    public void loadDomainConfiguration_shouldIgnoreFilesWithWrongExtension() throws Exception {
        createTestFile("tests/data.csv", "csv data");
        createTestFile("tests/notes.txt", "text data");

        service.onApplicationEvent(mockEvent);

        verify(mockHandler).processConfiguration(any(InputStream.class), eq("data.csv"));
        verify(mockHandler, times(1)).processConfiguration(any(), anyString());
    }

    @Test
    public void loadDomainConfiguration_shouldNotErrorWhenFilesystemDirMissing() throws Exception {
        // No filesystem directory created; resolver returns nothing
        service.onApplicationEvent(mockEvent);

        verify(mockHandler, never()).processConfiguration(any(), anyString());
    }

    // -- Filesystem takes precedence over classpath --

    @Test
    public void loadDomainConfiguration_shouldUseOnlyFilesystemWhenFilesystemFilesExist() throws Exception {
        createTestFile("tests/fs-tests.csv", "fs data");

        service.onApplicationEvent(mockEvent);

        // Only filesystem file should be processed — classpath is not even consulted
        // when filesystem files exist
        verify(mockHandler).processConfiguration(any(InputStream.class), eq("fs-tests.csv"));
        verify(resolver, never()).getResources(eq("classpath*:configuration/tests/*.csv"));
    }

    @Test
    public void loadDomainConfiguration_shouldFallBackToClasspathWhenNoFilesystemFiles() throws Exception {
        Resource cpResource = createMockResource("classpath-tests.csv", "cp data");
        when(resolver.getResources("classpath*:configuration/tests/*.csv")).thenReturn(new Resource[] { cpResource });
        // No filesystem directory created

        service.onApplicationEvent(mockEvent);

        verify(mockHandler).processConfiguration(any(InputStream.class), eq("classpath-tests.csv"));
    }

    // -- Checksum behavior --

    @Test
    public void loadDomainConfiguration_shouldSkipUnchangedFileOnSecondRun() throws Exception {
        createTestFile("tests/stable.csv", "unchanging content");

        service.onApplicationEvent(mockEvent);

        // Reset guard to allow a second run (simulating a restart with persisted
        // checksums)
        ReflectionTestUtils.setField(service, "initialized", false);
        service.onApplicationEvent(mockEvent);

        // Processed on first run, skipped on second (checksum match)
        verify(mockHandler, times(1)).processConfiguration(any(InputStream.class), eq("stable.csv"));
    }

    @Test
    public void loadDomainConfiguration_shouldReprocessFileWhenContentChanges() throws Exception {
        File testFile = createTestFile("tests/mutable.csv", "original content");

        service.onApplicationEvent(mockEvent);

        // Change the file content to produce a different checksum
        Files.write(testFile.toPath(), "modified content".getBytes());

        ReflectionTestUtils.setField(service, "initialized", false);
        service.onApplicationEvent(mockEvent);

        verify(mockHandler, times(2)).processConfiguration(any(InputStream.class), eq("mutable.csv"));
    }

    @Test
    public void loadDomainConfiguration_shouldPersistChecksumFileAfterProcessing() throws Exception {
        createTestFile("tests/persisted.csv", "some content");

        service.onApplicationEvent(mockEvent);

        File checksumFile = new File(tempFolder.getRoot(), "tests-checksums.properties");
        assertTrue("Checksum file should be created after processing", checksumFile.exists());

        Properties checksums = new Properties();
        try (FileInputStream fis = new FileInputStream(checksumFile)) {
            checksums.load(fis);
        }
        assertFalse("Checksum properties should contain an entry for the processed file",
                checksums.getProperty("persisted.csv").isEmpty());
    }

    // -- Error handling --

    @Test
    public void onApplicationEvent_shouldContinueWithNextHandlerWhenOneThrows() throws Exception {
        DomainConfigurationHandler failingHandler = mock(DomainConfigurationHandler.class);
        when(failingHandler.getDomainName()).thenReturn("bad-domain");
        when(failingHandler.getLoadOrder()).thenReturn(50);
        when(failingHandler.getFileMatcher()).thenReturn("*.csv");

        DomainConfigurationHandler goodHandler = mock(DomainConfigurationHandler.class);
        when(goodHandler.getDomainName()).thenReturn("good-domain");
        when(goodHandler.getLoadOrder()).thenReturn(100);
        when(goodHandler.getFileMatcher()).thenReturn("*.csv");

        // Make the first handler's resolver call throw
        when(resolver.getResources("classpath*:configuration/bad-domain/*.csv"))
                .thenThrow(new IOException("simulated failure"));

        Resource goodResource = createMockResource("good.csv", "data");
        when(resolver.getResources("classpath*:configuration/good-domain/*.csv"))
                .thenReturn(new Resource[] { goodResource });

        service.setDomainHandlers(Arrays.asList(failingHandler, goodHandler));

        service.onApplicationEvent(mockEvent);

        // The good handler should still be processed despite the first one failing
        verify(goodHandler).processConfiguration(any(InputStream.class), eq("good.csv"));
    }

    @Test
    public void loadDomainConfiguration_shouldContinueWithNextFileWhenProcessingFails() throws Exception {
        Resource failResource = createMockResource("fail.csv", "fail data");
        Resource okResource = createMockResource("ok.csv", "ok data");
        when(resolver.getResources("classpath*:configuration/tests/*.csv"))
                .thenReturn(new Resource[] { failResource, okResource });

        doThrow(new RuntimeException("processing error")).when(mockHandler).processConfiguration(any(InputStream.class),
                eq("fail.csv"));

        service.onApplicationEvent(mockEvent);

        // The second file should still be processed
        verify(mockHandler).processConfiguration(any(InputStream.class), eq("ok.csv"));
    }

    // -- Instance-specific loading: classpath --

    @Test
    public void loadDomainConfiguration_shouldSkipBaseFilesWhenInstanceFilesFoundOnClasspath() throws Exception {
        ReflectionTestUtils.setField(service, "instanceId", "myinstance");

        Resource instanceResource = createMockResource("instance-tests.csv", "instance data");
        when(resolver.getResources("classpath*:configuration/tests/myinstance/*.csv"))
                .thenReturn(new Resource[] { instanceResource });

        service.onApplicationEvent(mockEvent);

        verify(mockHandler).processConfiguration(any(InputStream.class), eq("instance-tests.csv"));
        // The base classpath pattern should never be resolved since instance files were
        // found
        verify(resolver, never()).getResources(eq("classpath*:configuration/tests/*.csv"));
    }

    // -- Instance-specific loading: filesystem --

    @Test
    public void loadDomainConfiguration_shouldSkipBaseFilesWhenInstanceFilesFoundOnFilesystem() throws Exception {
        ReflectionTestUtils.setField(service, "instanceId", "myinstance");

        // Create instance-specific filesystem files
        createTestFile("tests/myinstance/instance-data.csv", "instance fs data");
        // Create base filesystem files (should NOT be loaded)
        createTestFile("tests/base-data.csv", "base fs data");

        service.onApplicationEvent(mockEvent);

        verify(mockHandler).processConfiguration(any(InputStream.class), eq("instance-data.csv"));
        verify(mockHandler, never()).processConfiguration(any(InputStream.class), eq("base-data.csv"));
    }

    // -- Instance-specific loading: fallback --

    @Test
    public void loadDomainConfiguration_shouldFallBackToBaseWhenNoInstanceFilesExist() throws Exception {
        ReflectionTestUtils.setField(service, "instanceId", "myinstance");

        // No instance-specific files anywhere
        // Base classpath has a resource
        Resource baseResource = createMockResource("base-tests.csv", "base data");
        when(resolver.getResources("classpath*:configuration/tests/*.csv")).thenReturn(new Resource[] { baseResource });

        service.onApplicationEvent(mockEvent);

        // Should fall back to base
        verify(mockHandler).processConfiguration(any(InputStream.class), eq("base-tests.csv"));
        // Instance pattern was checked (but returned nothing)
        verify(resolver).getResources(eq("classpath*:configuration/tests/myinstance/*.csv"));
    }

    @Test
    public void loadDomainConfiguration_shouldFallBackToBaseFilesystemWhenNoInstanceFilesExist() throws Exception {
        ReflectionTestUtils.setField(service, "instanceId", "myinstance");

        // No instance-specific files; base filesystem has a file
        createTestFile("tests/fallback.csv", "fallback data");

        service.onApplicationEvent(mockEvent);

        verify(mockHandler).processConfiguration(any(InputStream.class), eq("fallback.csv"));
    }

    // -- No instance ID --

    @Test
    public void loadDomainConfiguration_shouldUseBaseFilesWhenInstanceIdNull() throws Exception {
        ReflectionTestUtils.setField(service, "instanceId", null);

        Resource baseResource = createMockResource("base.csv", "base data");
        when(resolver.getResources("classpath*:configuration/tests/*.csv")).thenReturn(new Resource[] { baseResource });

        service.onApplicationEvent(mockEvent);

        verify(mockHandler).processConfiguration(any(InputStream.class), eq("base.csv"));
        // Two resolver calls (file: then classpath:) — no instance pattern checked
        verify(resolver, times(2)).getResources(anyString());
    }

    @Test
    public void loadDomainConfiguration_shouldUseBaseFilesWhenInstanceIdBlank() throws Exception {
        ReflectionTestUtils.setField(service, "instanceId", "   ");

        Resource baseResource = createMockResource("base.csv", "base data");
        when(resolver.getResources("classpath*:configuration/tests/*.csv")).thenReturn(new Resource[] { baseResource });

        service.onApplicationEvent(mockEvent);

        verify(mockHandler).processConfiguration(any(InputStream.class), eq("base.csv"));
        // Two resolver calls (file: then classpath:) — no instance pattern checked
        verify(resolver, times(2)).getResources(anyString());
    }

    // -- Instance + checksum interaction --

    @Test
    public void loadDomainConfiguration_shouldRespectChecksumsForInstanceFiles() throws Exception {
        ReflectionTestUtils.setField(service, "instanceId", "myinstance");

        createTestFile("tests/myinstance/instance.csv", "instance content");

        // First run: processes the file
        service.onApplicationEvent(mockEvent);
        verify(mockHandler, times(1)).processConfiguration(any(InputStream.class), eq("instance.csv"));

        // Second run: same file, should be skipped (checksum match)
        ReflectionTestUtils.setField(service, "initialized", false);
        service.onApplicationEvent(mockEvent);
        verify(mockHandler, times(1)).processConfiguration(any(InputStream.class), eq("instance.csv"));
    }

    // -- Claimed-files tracking (shared domain) --

    @Test
    public void loadDomainConfiguration_shouldLetSpecificHandlerClaimFilesBeforeBroadHandler() throws Exception {
        // Specific handler: matches only "*-levels.csv" in domain "shared"
        DomainConfigurationHandler specificHandler = mock(DomainConfigurationHandler.class);
        when(specificHandler.getDomainName()).thenReturn("shared");
        when(specificHandler.getLoadOrder()).thenReturn(50);
        when(specificHandler.getFileMatcher()).thenReturn("*-levels.csv");

        // Broad handler: matches all "*.csv" in domain "shared"
        DomainConfigurationHandler broadHandler = mock(DomainConfigurationHandler.class);
        when(broadHandler.getDomainName()).thenReturn("shared");
        when(broadHandler.getLoadOrder()).thenReturn(100);
        when(broadHandler.getFileMatcher()).thenReturn("*.csv");

        // Create filesystem files: one matching the specific pattern, one not
        createTestFile("shared/address-levels.csv", "levels data");
        createTestFile("shared/values.csv", "values data");

        service.setDomainHandlers(Arrays.asList(specificHandler, broadHandler));
        service.onApplicationEvent(mockEvent);

        // Specific handler processes only its matching file
        verify(specificHandler).processConfiguration(any(InputStream.class), eq("address-levels.csv"));
        verify(specificHandler, never()).processConfiguration(any(InputStream.class), eq("values.csv"));

        // Broad handler processes the unclaimed file, skips the claimed one
        verify(broadHandler).processConfiguration(any(InputStream.class), eq("values.csv"));
        verify(broadHandler, never()).processConfiguration(any(InputStream.class), eq("address-levels.csv"));
    }

    // -- Helpers --

    /**
     * Creates a mock classpath Resource that returns a fresh InputStream on every
     * call to getInputStream().
     */
    private Resource createMockResource(String fileName, String content) throws IOException {
        Resource resource = mock(Resource.class);
        when(resource.getFilename()).thenReturn(fileName);
        when(resource.getInputStream()).thenAnswer(invocation -> new ByteArrayInputStream(content.getBytes()));
        return resource;
    }

    /**
     * Creates a real file under the temp folder at the given relative path.
     */
    private File createTestFile(String relativePath, String content) throws IOException {
        File file = new File(tempFolder.getRoot(), relativePath);
        file.getParentFile().mkdirs();
        Files.write(file.toPath(), content.getBytes());
        return file;
    }
}
