package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.FileImportConfigurationDAO;
import org.openelisglobal.analyzer.valueholder.FileImportConfiguration;
import org.openelisglobal.analyzerresults.dao.AnalyzerResultsDAO;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for FileImportService implementation
 * 
 * 
 * TDD Workflow (MANDATORY for complex logic): - RED: Write failing test first
 * (defines expected behavior) - GREEN: Write minimal code to make test pass -
 * REFACTOR: Improve code quality while keeping tests green
 * 
 * Test Naming: test{MethodName}_{Scenario}_{ExpectedResult}
 */
@RunWith(MockitoJUnitRunner.class)
public class FileImportServiceTest {

    @Mock
    private FileImportConfigurationDAO fileImportConfigurationDAO;

    @Mock
    private AnalyzerResultsDAO analyzerResultsDAO;

    @InjectMocks
    private FileImportServiceImpl fileImportService;

    private FileImportConfiguration testConfig;
    private Path testFile;
    private Path tempDir;

    @Before
    public void setUp() throws IOException {
        // Create temporary directory for testing
        tempDir = Files.createTempDirectory("file-import-test");
        // Inject baseImportDir for defense-in-depth path validation
        ReflectionTestUtils.setField(fileImportService, "baseImportDir", tempDir.toString());
        testFile = tempDir.resolve("test-file.csv");
        Files.createFile(testFile);
        Files.write(testFile, "Sample_ID,Test_Code,Result\n12345-001,HB,12.5".getBytes());

        // Create test configuration
        testConfig = new FileImportConfiguration();
        testConfig.setId("CONFIG-001");
        testConfig.setAnalyzerId(1);
        testConfig.setImportDirectory("/data/import");
        testConfig.setArchiveDirectory(tempDir.resolve("archive").toString());
        testConfig.setErrorDirectory(tempDir.resolve("error").toString());
        testConfig.setFilePattern("*.csv");
        testConfig.setDelimiter(",");
        testConfig.setHasHeader(true);
        testConfig.setActive(true);

        Map<String, String> columnMappings = new HashMap<>();
        columnMappings.put("Sample_ID", "sampleId");
        columnMappings.put("Test_Code", "testCode");
        columnMappings.put("Result", "result");
        testConfig.setColumnMappings(columnMappings);
    }

    @Test
    public void testGetByAnalyzerId_WithValidId_ReturnsConfiguration() {
        when(fileImportConfigurationDAO.findByAnalyzerId(1)).thenReturn(Optional.of(testConfig));

        Optional<FileImportConfiguration> result = fileImportService.getByAnalyzerId(1);

        assertTrue("Should return configuration", result.isPresent());
        assertEquals("CONFIG-001", result.get().getId());
        verify(fileImportConfigurationDAO).findByAnalyzerId(1);
    }

    @Test
    public void testGetByAnalyzerId_WithInvalidId_ReturnsEmpty() {
        when(fileImportConfigurationDAO.findByAnalyzerId(999)).thenReturn(Optional.empty());

        Optional<FileImportConfiguration> result = fileImportService.getByAnalyzerId(999);

        assertFalse("Should return empty for non-existent analyzer", result.isPresent());
    }

    @Test
    public void testGetAllActive_ReturnsOnlyActiveConfigurations() {
        FileImportConfiguration activeConfig = new FileImportConfiguration();
        activeConfig.setId("ACTIVE-001");
        activeConfig.setActive(true);

        FileImportConfiguration inactiveConfig = new FileImportConfiguration();
        inactiveConfig.setId("INACTIVE-001");
        inactiveConfig.setActive(false);

        List<FileImportConfiguration> allConfigs = new ArrayList<>();
        allConfigs.add(activeConfig);
        allConfigs.add(inactiveConfig);

        when(fileImportConfigurationDAO.findAllActive()).thenReturn(List.of(activeConfig));

        List<FileImportConfiguration> result = fileImportService.getAllActive();

        assertNotNull("Result should not be null", result);
        assertEquals("Should return only active configurations", 1, result.size());
        assertEquals("ACTIVE-001", result.get(0).getId());
        verify(fileImportConfigurationDAO).findAllActive();
    }

    @Test
    public void testArchiveFile_WithValidPath_Succeeds() throws IOException {
        Path archiveDir = Paths.get(testConfig.getArchiveDirectory());
        Files.createDirectories(archiveDir);
        Path testFile = tempDir.resolve("test-archive.csv");
        Files.createFile(testFile);
        Files.write(testFile, "test content".getBytes());

        boolean result = fileImportService.archiveFile(testFile, testConfig);

        assertTrue("Archive should succeed", result);
        assertTrue("File should exist in archive directory", Files.exists(archiveDir.resolve("test-archive.csv")));
        assertFalse("Original file should not exist", Files.exists(testFile));
    }

    @Test
    public void testArchiveFile_WithMissingDirectory_CreatesDirectory() throws IOException {
        Path archiveDir = Paths.get(testConfig.getArchiveDirectory());
        // Ensure directory doesn't exist
        if (Files.exists(archiveDir)) {
            Files.delete(archiveDir);
        }
        Path testFile = tempDir.resolve("test-create-dir.csv");
        Files.createFile(testFile);

        boolean result = fileImportService.archiveFile(testFile, testConfig);

        assertTrue("Archive should succeed and create directory", result);
        assertTrue("Archive directory should be created", Files.exists(archiveDir));
        assertTrue("File should exist in archive directory", Files.exists(archiveDir.resolve("test-create-dir.csv")));
    }

    @Test
    public void testArchiveFile_WithNoArchiveDirectory_ReturnsFalse() {
        testConfig.setArchiveDirectory(null);
        Path testFile = tempDir.resolve("test-no-archive.csv");

        boolean result = fileImportService.archiveFile(testFile, testConfig);

        assertFalse("Should return false when archive directory not configured", result);
    }

    @Test
    public void testArchiveFile_WithEmptyArchiveDirectory_ReturnsFalse() {
        testConfig.setArchiveDirectory("");
        Path testFile = tempDir.resolve("test-empty-archive.csv");

        boolean result = fileImportService.archiveFile(testFile, testConfig);

        assertFalse("Should return false when archive directory is empty", result);
    }

    @Test
    public void testMoveToErrorDirectory_WithValidPath_Succeeds() throws IOException {
        Path errorDir = Paths.get(testConfig.getErrorDirectory());
        Files.createDirectories(errorDir);
        Path testFile = tempDir.resolve("test-error.csv");
        Files.createFile(testFile);
        Files.write(testFile, "test content".getBytes());

        boolean result = fileImportService.moveToErrorDirectory(testFile, testConfig, "Test error message");

        assertTrue("Move to error directory should succeed", result);
        assertTrue("File should exist in error directory", Files.exists(errorDir.resolve("test-error.csv")));
        assertFalse("Original file should not exist", Files.exists(testFile));
    }

    @Test
    public void testMoveToErrorDirectory_WithMissingDirectory_CreatesDirectory() throws IOException {
        Path errorDir = Paths.get(testConfig.getErrorDirectory());
        // Ensure directory doesn't exist
        if (Files.exists(errorDir)) {
            Files.delete(errorDir);
        }
        Path testFile = tempDir.resolve("test-create-error-dir.csv");
        Files.createFile(testFile);

        boolean result = fileImportService.moveToErrorDirectory(testFile, testConfig, "Test error");

        assertTrue("Move should succeed and create directory", result);
        assertTrue("Error directory should be created", Files.exists(errorDir));
        assertTrue("File should exist in error directory", Files.exists(errorDir.resolve("test-create-error-dir.csv")));
    }

    @Test
    public void testMoveToErrorDirectory_WithNoErrorDirectory_ReturnsFalse() {
        testConfig.setErrorDirectory(null);
        Path testFile = tempDir.resolve("test-no-error-dir.csv");

        boolean result = fileImportService.moveToErrorDirectory(testFile, testConfig, "Test error");

        assertFalse("Should return false when error directory not configured", result);
    }

    @Test
    public void testProcessFile_WithValidFile_ProcessesFile() throws IOException {
        // Note: This test verifies the method structure, but full testing requires
        // SpringContext for FileAnalyzerReader plugin matching (see
        // FileAnalyzerReaderIntegrationTest)
        Path testFile = tempDir.resolve("test-process.csv");
        Files.write(testFile, "Sample_ID,Test_Code,Result\n12345-001,HB,12.5".getBytes());

        // This will fail at FileAnalyzerReader.readStream() due to missing
        // SpringContext,
        // but verifies the method structure and error handling
        boolean result = fileImportService.processFile(testFile, testConfig, "testUser");

        // Method should return false when FileAnalyzerReader fails (no SpringContext in
        // unit test)
        // Full integration test in FileAnalyzerReaderIntegrationTest
        assertFalse("Should return false when SpringContext not available in unit test", result);
    }

    @Test
    public void testIsDuplicate_WithNoDuplicates_ReturnsFalse() {
        // Mock: No duplicates found
        when(analyzerResultsDAO.getDuplicateResultByAccessionAndTest(any(AnalyzerResults.class)))
                .thenReturn(null);

        boolean result = fileImportService.isDuplicate(1, "12345-001", "HB", "2026-01-23", "10:30:00");

        assertFalse("Should return false when no duplicates found", result);
        verify(analyzerResultsDAO).getDuplicateResultByAccessionAndTest(any(AnalyzerResults.class));
    }

    @Test
    public void testIsDuplicate_WithExactDuplicate_ReturnsTrue() {
        // Create a duplicate result with matching date
        AnalyzerResults duplicate = new AnalyzerResults();
        duplicate.setId("RESULT-001");
        duplicate.setAnalyzerId("1");
        duplicate.setAccessionNumber("12345-001");
        duplicate.setTestName("HB");
        // Set completeDate to match the test date (2026-01-23 10:30:00)
        try {
            java.sql.Timestamp testDate = java.sql.Timestamp.valueOf("2026-01-23 10:30:00");
            duplicate.setCompleteDate(testDate);
        } catch (Exception e) {
            // If date parsing fails, test will still work
        }

        List<AnalyzerResults> duplicates = new ArrayList<>();
        duplicates.add(duplicate);

        // Mock: Duplicate found with matching date
        when(analyzerResultsDAO.getDuplicateResultByAccessionAndTest(any(AnalyzerResults.class)))
                .thenReturn(duplicates);

        boolean result = fileImportService.isDuplicate(1, "12345-001", "HB", "2026-01-23", "10:30:00");

        assertTrue("Should return true when exact duplicate found", result);
        verify(analyzerResultsDAO).getDuplicateResultByAccessionAndTest(any(AnalyzerResults.class));
    }

    @Test
    public void testIsDuplicate_WithDuplicateButDifferentDate_ReturnsFalse() {
        // Create a duplicate result with different date
        AnalyzerResults duplicate = new AnalyzerResults();
        duplicate.setId("RESULT-001");
        duplicate.setAnalyzerId("1");
        duplicate.setAccessionNumber("12345-001");
        duplicate.setTestName("HB");
        // Set completeDate to different date
        try {
            java.sql.Timestamp differentDate = java.sql.Timestamp.valueOf("2026-01-22 10:30:00");
            duplicate.setCompleteDate(differentDate);
        } catch (Exception e) {
            // If date parsing fails, test will still work
        }

        List<AnalyzerResults> duplicates = new ArrayList<>();
        duplicates.add(duplicate);

        // Mock: Duplicate found but with different date
        when(analyzerResultsDAO.getDuplicateResultByAccessionAndTest(any(AnalyzerResults.class)))
                .thenReturn(duplicates);

        boolean result = fileImportService.isDuplicate(1, "12345-001", "HB", "2026-01-23", "10:30:00");

        assertFalse("Should return false when duplicate has different date", result);
        verify(analyzerResultsDAO).getDuplicateResultByAccessionAndTest(any(AnalyzerResults.class));
    }

    @Test
    public void testIsDuplicate_WithDuplicateButNoDate_ReturnsTrue() {
        // Create a duplicate result without date
        AnalyzerResults duplicate = new AnalyzerResults();
        duplicate.setId("RESULT-001");
        duplicate.setAnalyzerId("1");
        duplicate.setAccessionNumber("12345-001");
        duplicate.setTestName("HB");
        duplicate.setCompleteDate(null);

        List<AnalyzerResults> duplicates = new ArrayList<>();
        duplicates.add(duplicate);

        // Mock: Duplicate found but no date provided in test
        when(analyzerResultsDAO.getDuplicateResultByAccessionAndTest(any(AnalyzerResults.class)))
                .thenReturn(duplicates);

        boolean result = fileImportService.isDuplicate(1, "12345-001", "HB", null, null);

        assertTrue("Should return true when duplicate found and no date provided", result);
        verify(analyzerResultsDAO).getDuplicateResultByAccessionAndTest(any(AnalyzerResults.class));
    }
}
