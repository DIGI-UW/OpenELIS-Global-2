package org.openelisglobal.analyzer.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.BridgeRegistrationService;
import org.openelisglobal.analyzer.service.FileImportService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

public class AnalyzerProfileApplyControllerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private AnalyzerService analyzerService;
    private FileImportService fileImportService;
    private BridgeRegistrationService bridgeRegistrationService;
    private AnalyzerTestMappingService analyzerTestMappingService;
    private AnalyzerRestController controller;

    @Before
    public void setUp() {
        analyzerService = mock(AnalyzerService.class);
        fileImportService = mock(FileImportService.class);
        bridgeRegistrationService = mock(BridgeRegistrationService.class);
        analyzerTestMappingService = mock(AnalyzerTestMappingService.class);
        controller = new AnalyzerRestController();
        ReflectionTestUtils.setField(controller, "analyzerService", analyzerService);
        ReflectionTestUtils.setField(controller, "fileImportService", fileImportService);
        ReflectionTestUtils.setField(controller, "bridgeRegistrationService", bridgeRegistrationService);
        ReflectionTestUtils.setField(controller, "analyzerTestMappingService", analyzerTestMappingService);
    }

    @Test
    public void applyProfileToAnalyzer_reappliesMappingsAndFileDefaultsForExistingAnalyzer() throws Exception {
        Path baseDir = tempFolder.newFolder("profiles").toPath();
        Files.createDirectories(baseDir.resolve("file"));
        Files.writeString(baseDir.resolve("file").resolve("generic.json"), """
                {
                  "profileMeta": { "id": "file/generic", "version": "1", "displayName": "Generic File" },
                  "protocol": { "name": "FILE" },
                  "default_test_mappings": [],
                  "configDefaults": {}
                }
                """);
        ReflectionTestUtils.setField(controller, "analyzerProfilesDir", baseDir.toString());

        Analyzer analyzer = new Analyzer();
        analyzer.setId("42");
        analyzer.setName("Existing Analyzer");
        when(analyzerService.get("42")).thenReturn(analyzer);
        when(analyzerService.getWithType("42")).thenReturn(Optional.of(analyzer));

        ResponseEntity<Map<String, Object>> response = controller.applyProfileToAnalyzer("42", "file", "generic",
                requestWithUser("7"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(true, response.getBody().get("applied"));
        verify(analyzerService).autoCreateTestMappings(eq("42"), anyMap(), eq("7"));
        verify(fileImportService).autoCreateFromProfile(eq("42"), anyMap(), eq("Existing Analyzer"), eq("7"));
    }

    private static MockHttpServletRequest requestWithUser(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData user = new UserSessionData();
        user.setSytemUserId(Integer.parseInt(userId));
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, user);
        return request;
    }
}
