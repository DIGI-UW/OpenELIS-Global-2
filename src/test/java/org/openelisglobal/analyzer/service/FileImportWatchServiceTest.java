package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.valueholder.FileImportConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class FileImportWatchServiceTest {

    @Mock
    private FileImportService fileImportService;

    @InjectMocks
    private FileImportWatchService fileImportWatchService;

    private Path tempBaseDir;

    @Before
    public void setUp() throws Exception {
        tempBaseDir = Files.createTempDirectory("file-import-watch-test");
        ReflectionTestUtils.setField(fileImportWatchService, "baseImportDir", tempBaseDir.toString());
    }

    @Test
    public void testPollImportDirectories_WithCsvFormat_SkipsNonCsvFiles() throws Exception {
        Path importDir = Files.createDirectories(tempBaseDir.resolve("incoming-csv"));
        Path csvFile = Files.writeString(importDir.resolve("results.csv"), "Sample_ID,Result\nA1,12");
        Files.writeString(importDir.resolve("results.xlsx"), "not-real-xlsx");

        FileImportConfiguration config = new FileImportConfiguration();
        config.setAnalyzerId(101);
        config.setImportDirectory(importDir.toString());
        config.setArchiveDirectory(tempBaseDir.resolve("archive").toString());
        config.setErrorDirectory(tempBaseDir.resolve("error").toString());
        config.setFilePattern("*.*");
        config.setFileFormat("CSV");
        config.setActive(true);

        when(fileImportService.getAllActive()).thenReturn(List.of(config));
        when(fileImportService.processFile(any(Path.class), eq(config), any(String.class))).thenReturn(true);
        when(fileImportService.archiveFile(any(Path.class), eq(config))).thenReturn(true);

        fileImportWatchService.pollImportDirectories();

        ArgumentCaptor<Path> filePathCaptor = ArgumentCaptor.forClass(Path.class);
        verify(fileImportService).processFile(filePathCaptor.capture(), eq(config), any(String.class));
        assertEquals("CSV file should be the only processed file", csvFile.getFileName().toString(),
                filePathCaptor.getValue().getFileName().toString());
        verify(fileImportService, never()).moveToErrorDirectory(any(Path.class), eq(config), any(String.class));
    }

    @Test
    public void testPollImportDirectories_DuplicateConfigs_FileProcessedOnlyOnce() throws Exception {
        // Two configs watching the SAME directory with the same format (legacy overlap)
        Path importDir = Files.createDirectories(tempBaseDir.resolve("shared-dir"));
        Files.writeString(importDir.resolve("results.xlsx"), "fake-excel");

        FileImportConfiguration config1 = new FileImportConfiguration();
        config1.setAnalyzerId(201);
        config1.setImportDirectory(importDir.toString());
        config1.setArchiveDirectory(tempBaseDir.resolve("archive1").toString());
        config1.setErrorDirectory(tempBaseDir.resolve("error1").toString());
        config1.setFilePattern("*.xlsx");
        config1.setFileFormat("EXCEL");
        config1.setActive(true);

        FileImportConfiguration config2 = new FileImportConfiguration();
        config2.setAnalyzerId(202);
        config2.setImportDirectory(importDir.toString());
        config2.setArchiveDirectory(tempBaseDir.resolve("archive2").toString());
        config2.setErrorDirectory(tempBaseDir.resolve("error2").toString());
        config2.setFilePattern("*.xlsx");
        config2.setFileFormat("EXCEL");
        config2.setActive(true);

        when(fileImportService.getAllActive()).thenReturn(List.of(config1, config2));
        when(fileImportService.processFile(any(Path.class), eq(config1), any(String.class))).thenReturn(true);
        when(fileImportService.archiveFile(any(Path.class), eq(config1))).thenReturn(true);

        fileImportWatchService.pollImportDirectories();

        // File should be processed exactly once (by the first config)
        verify(fileImportService, times(1)).processFile(any(Path.class), eq(config1), any(String.class));
        // Second config should NOT process the same file
        verify(fileImportService, never()).processFile(any(Path.class), eq(config2), any(String.class));
    }

    @Test
    public void testPollImportDirectories_SharedDir_DifferentFormats_BothProcess() throws Exception {
        // Two configs watching the same directory but DIFFERENT formats (valid use
        // case)
        Path importDir = Files.createDirectories(tempBaseDir.resolve("multi-format"));
        Files.writeString(importDir.resolve("results.csv"), "Sample_ID,Result\nA1,12");
        Files.writeString(importDir.resolve("results.xlsx"), "fake-excel");

        FileImportConfiguration csvConfig = new FileImportConfiguration();
        csvConfig.setAnalyzerId(301);
        csvConfig.setImportDirectory(importDir.toString());
        csvConfig.setArchiveDirectory(tempBaseDir.resolve("archive-csv").toString());
        csvConfig.setErrorDirectory(tempBaseDir.resolve("error-csv").toString());
        csvConfig.setFilePattern("*.csv");
        csvConfig.setFileFormat("CSV");
        csvConfig.setActive(true);

        FileImportConfiguration excelConfig = new FileImportConfiguration();
        excelConfig.setAnalyzerId(302);
        excelConfig.setImportDirectory(importDir.toString());
        excelConfig.setArchiveDirectory(tempBaseDir.resolve("archive-excel").toString());
        excelConfig.setErrorDirectory(tempBaseDir.resolve("error-excel").toString());
        excelConfig.setFilePattern("*.xlsx");
        excelConfig.setFileFormat("EXCEL");
        excelConfig.setActive(true);

        when(fileImportService.getAllActive()).thenReturn(List.of(csvConfig, excelConfig));
        when(fileImportService.processFile(any(Path.class), any(), any(String.class))).thenReturn(true);
        when(fileImportService.archiveFile(any(Path.class), any())).thenReturn(true);

        fileImportWatchService.pollImportDirectories();

        // Each config should process its own file (different files, no overlap)
        verify(fileImportService, times(1)).processFile(any(Path.class), eq(csvConfig), any(String.class));
        verify(fileImportService, times(1)).processFile(any(Path.class), eq(excelConfig), any(String.class));
    }
}
