package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
}
