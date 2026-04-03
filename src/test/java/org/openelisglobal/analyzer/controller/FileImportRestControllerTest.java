package org.openelisglobal.analyzer.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

public class FileImportRestControllerTest extends BaseWebContextSensitiveTest {

    @Mock
    private FileImportService fileImportService;

    private MockHttpSession mockSession;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        FileImportRestController controller = webApplicationContext.getBean(FileImportRestController.class);
        ReflectionTestUtils.setField(controller, "fileImportService", fileImportService);
        ReflectionTestUtils.setField(controller, "baseImportDir", "/tmp/openelis-file-import");

        // Set up authenticated session (required by getSysUserIdWithFallback)
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
        mockSession = new MockHttpSession();
        mockSession.setAttribute(IActionConstants.USER_SESSION_DATA, userSessionData);
    }

    @Test
    public void testCreateConfiguration_WithFileFormat_PersistsValue() throws Exception {
        when(fileImportService.getByAnalyzerId(77)).thenReturn(Optional.empty());
        when(fileImportService.insert(any(FileImportConfiguration.class))).thenReturn("cfg-77");

        mockMvc.perform(post("/rest/analyzer/file-import/configurations").session(mockSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"
                        + "\"analyzerId\":77,"
                        + "\"importDirectory\":\"/tmp/openelis-file-import/incoming\","
                        + "\"archiveDirectory\":\"/tmp/openelis-file-import/archive\","
                        + "\"errorDirectory\":\"/tmp/openelis-file-import/error\","
                        + "\"filePattern\":\"*.csv\","
                        + "\"fileFormat\":\"TSV\","
                        + "\"delimiter\":\"\\t\","
                        + "\"hasHeader\":true,"
                        + "\"active\":true"
                        + "}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<FileImportConfiguration> captor = ArgumentCaptor.forClass(FileImportConfiguration.class);
        verify(fileImportService).insert(captor.capture());
        assertEquals("TSV", captor.getValue().getFileFormat());
    }

    @Test
    public void testUpdateConfiguration_WithFileFormat_UpdatesValue() throws Exception {
        FileImportConfiguration existing = new FileImportConfiguration();
        existing.setId("cfg-78");
        existing.setAnalyzerId(78);
        existing.setImportDirectory("/tmp/openelis-file-import/incoming");
        existing.setFilePattern("*.csv");
        existing.setFileFormat("CSV");
        existing.setDelimiter(",");
        existing.setHasHeader(true);
        existing.setActive(true);

        when(fileImportService.get("cfg-78")).thenReturn(existing);

        mockMvc.perform(put("/rest/analyzer/file-import/configurations/cfg-78").session(mockSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + "\"importDirectory\":\"/tmp/openelis-file-import/incoming\","
                        + "\"archiveDirectory\":\"/tmp/openelis-file-import/archive\","
                        + "\"errorDirectory\":\"/tmp/openelis-file-import/error\"," + "\"filePattern\":\"*.xlsx\","
                        + "\"fileFormat\":\"EXCEL\"," + "\"delimiter\":\",\"," + "\"hasHeader\":true,"
                        + "\"active\":true" + "}"))
                .andExpect(status().isOk());

        ArgumentCaptor<FileImportConfiguration> captor = ArgumentCaptor.forClass(FileImportConfiguration.class);
        verify(fileImportService).update(captor.capture());
        assertEquals("EXCEL", captor.getValue().getFileFormat());
        verify(fileImportService).get(eq("cfg-78"));
    }

    /**
     * OGC-526: Verify that PUT propagates updated fields through the service layer.
     * The service's update() override handles Analyzer entity sync + bridge
     * registration, so the controller just needs to pass the right data through.
     */
    @Test
    public void testUpdateConfiguration_PropagatesDirectoryToService() throws Exception {
        FileImportConfiguration existing = new FileImportConfiguration();
        existing.setId("cfg-99");
        existing.setAnalyzerId(99);
        existing.setImportDirectory("/tmp/openelis-file-import/old-dir");
        existing.setFilePattern("*.csv");
        existing.setFileFormat("CSV");
        existing.setDelimiter(",");
        existing.setHasHeader(true);
        existing.setActive(true);

        when(fileImportService.get("cfg-99")).thenReturn(existing);

        mockMvc.perform(put("/rest/analyzer/file-import/configurations/cfg-99").session(mockSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + "\"importDirectory\":\"/tmp/openelis-file-import/new-dir\","
                        + "\"archiveDirectory\":\"/tmp/openelis-file-import/archive\","
                        + "\"errorDirectory\":\"/tmp/openelis-file-import/error\"," + "\"filePattern\":\"*.tsv\","
                        + "\"fileFormat\":\"TSV\"," + "\"delimiter\":\"\\t\"," + "\"hasHeader\":true,"
                        + "\"active\":true" + "}"))
                .andExpect(status().isOk());

        ArgumentCaptor<FileImportConfiguration> captor = ArgumentCaptor.forClass(FileImportConfiguration.class);
        verify(fileImportService).update(captor.capture());
        FileImportConfiguration updated = captor.getValue();
        assertEquals("New directory should propagate", "/tmp/openelis-file-import/new-dir",
                updated.getImportDirectory());
        assertEquals("New pattern should propagate", "*.tsv", updated.getFilePattern());
        assertEquals("New format should propagate", "TSV", updated.getFileFormat());
    }
}
