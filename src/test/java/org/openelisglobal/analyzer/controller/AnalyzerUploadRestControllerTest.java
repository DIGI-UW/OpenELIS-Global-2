package org.openelisglobal.analyzer.controller;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.FileImportService;
import org.openelisglobal.analyzer.valueholder.FileImportConfiguration;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Controller tests for bridge direct import ({@code POST
 * /rest/analyzers/{id}/import}).
 */
public class AnalyzerUploadRestControllerTest extends BaseWebContextSensitiveTest {

    @Mock
    private FileImportService fileImportService;

    private MockHttpSession mockSession;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        AnalyzerUploadRestController controller = webApplicationContext.getBean(AnalyzerUploadRestController.class);
        ReflectionTestUtils.setField(controller, "fileImportService", fileImportService);
        String stagingRoot = System.getProperty("java.io.tmpdir") + "/oe-bridge-import-test";
        ReflectionTestUtils.setField(controller, "fileImportBaseDirectory", stagingRoot);

        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
        mockSession = new MockHttpSession();
        mockSession.setAttribute(IActionConstants.USER_SESSION_DATA, userSessionData);
    }

    @Test
    public void directImport_EmptyFile_Returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

        mockMvc.perform(multipart("/rest/analyzers/1/import").file(file)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    public void directImport_NoFileImportConfig_Returns404() throws Exception {
        when(fileImportService.getByAnalyzerId(99)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "data.csv", "text/csv", "a,b\n".getBytes());

        mockMvc.perform(multipart("/rest/analyzers/99/import").file(file).session(mockSession))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").exists());
    }

    @Test
    public void directImport_ValidFile_Returns200() throws Exception {
        FileImportConfiguration cfg = new FileImportConfiguration();
        cfg.setId(UUID.randomUUID().toString());
        cfg.setAnalyzerId(42);
        cfg.setImportDirectory("/tmp/in");
        cfg.setFilePattern("*.csv");
        cfg.setFileFormat("CSV");
        cfg.setDelimiter(",");
        cfg.setHasHeader(true);
        cfg.setActive(true);
        cfg.setSysUserId("1");
        cfg.setFhirUuid(UUID.randomUUID());

        when(fileImportService.getByAnalyzerId(42)).thenReturn(Optional.of(cfg));
        when(fileImportService.processFile(any(), eq(cfg), eq("1"))).thenReturn(true);

        MockMultipartFile file = new MockMultipartFile("file", "results.csv", "text/csv",
                "Sample,Result\nE2E001,1\n".getBytes());

        mockMvc.perform(multipart("/rest/analyzers/42/import").file(file).session(mockSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.analyzerId").value(42));

        verify(fileImportService).processFile(any(), eq(cfg), eq("1"));
    }

    @Test
    public void directImport_ProcessFileFalse_Returns422() throws Exception {
        FileImportConfiguration cfg = new FileImportConfiguration();
        cfg.setId(UUID.randomUUID().toString());
        cfg.setAnalyzerId(7);
        cfg.setImportDirectory("/tmp/in");
        cfg.setFilePattern("*.csv");
        cfg.setFileFormat("CSV");
        cfg.setDelimiter(",");
        cfg.setHasHeader(true);
        cfg.setActive(true);
        cfg.setSysUserId("1");
        cfg.setFhirUuid(UUID.randomUUID());

        when(fileImportService.getByAnalyzerId(7)).thenReturn(Optional.of(cfg));
        when(fileImportService.processFile(any(), eq(cfg), eq("1"))).thenReturn(false);

        MockMultipartFile file = new MockMultipartFile("file", "results.csv", "text/csv", "a\n".getBytes());

        mockMvc.perform(multipart("/rest/analyzers/7/import").file(file).session(mockSession))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.success").value(false));
    }

    @Test
    public void directImport_Success_DeletesStagingTempFile() throws Exception {
        FileImportConfiguration cfg = new FileImportConfiguration();
        cfg.setId(UUID.randomUUID().toString());
        cfg.setAnalyzerId(42);
        cfg.setImportDirectory("/tmp/in");
        cfg.setFilePattern("*.csv");
        cfg.setFileFormat("CSV");
        cfg.setDelimiter(",");
        cfg.setHasHeader(true);
        cfg.setActive(true);
        cfg.setSysUserId("1");
        cfg.setFhirUuid(UUID.randomUUID());

        when(fileImportService.getByAnalyzerId(42)).thenReturn(Optional.of(cfg));
        ArgumentCaptor<Path> pathCaptor = ArgumentCaptor.forClass(Path.class);
        when(fileImportService.processFile(pathCaptor.capture(), eq(cfg), eq("1"))).thenReturn(true);

        MockMultipartFile file = new MockMultipartFile("file", "results.csv", "text/csv",
                "Sample,Result\nE2E001,1\n".getBytes());

        mockMvc.perform(multipart("/rest/analyzers/42/import").file(file).session(mockSession))
                .andExpect(status().isOk());

        Path staged = pathCaptor.getValue();
        assertFalse("Temp staging file should be deleted after success", Files.exists(staged));
    }

    @Test
    public void directImport_ProcessThrows_DeletesStagingTempFile() throws Exception {
        FileImportConfiguration cfg = new FileImportConfiguration();
        cfg.setId(UUID.randomUUID().toString());
        cfg.setAnalyzerId(42);
        cfg.setImportDirectory("/tmp/in");
        cfg.setFilePattern("*.csv");
        cfg.setFileFormat("CSV");
        cfg.setDelimiter(",");
        cfg.setHasHeader(true);
        cfg.setActive(true);
        cfg.setSysUserId("1");
        cfg.setFhirUuid(UUID.randomUUID());

        when(fileImportService.getByAnalyzerId(42)).thenReturn(Optional.of(cfg));
        ArgumentCaptor<Path> pathCaptor = ArgumentCaptor.forClass(Path.class);
        when(fileImportService.processFile(pathCaptor.capture(), eq(cfg), eq("1")))
                .thenThrow(new RuntimeException("unit test boom"));

        MockMultipartFile file = new MockMultipartFile("file", "results.csv", "text/csv", "x\n".getBytes());

        mockMvc.perform(multipart("/rest/analyzers/42/import").file(file).session(mockSession))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.referenceId").exists());

        Path staged = pathCaptor.getValue();
        assertFalse("Temp staging file should be deleted after failure", Files.exists(staged));
    }
}
