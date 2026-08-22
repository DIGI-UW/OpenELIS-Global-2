package org.openelisglobal.analyzer.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.service.AnalyzerMappingCatalogService;
import org.openelisglobal.analyzer.service.AnalyzerTypeCatalogService;
import org.openelisglobal.analyzer.service.AnalyzerTypeCatalogView;
import org.openelisglobal.analyzer.service.AnalyzerTypeMappingService;
import org.openelisglobal.analyzer.service.AnalyzerTypeMappingView;
import org.openelisglobal.analyzer.service.BridgeProfileCatalog;
import org.openelisglobal.analyzer.service.BridgeProfileManagementException;
import org.openelisglobal.analyzer.service.BridgeProfileManagementService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerTypeRestControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AnalyzerTypeCatalogService catalogService;

    @Mock
    private BridgeProfileManagementService managementService;

    @Mock
    private AnalyzerMappingCatalogService mappingCatalogService;

    @Mock
    private AnalyzerTypeMappingService mappingService;

    private AnalyzerTypeRestController controller;

    @Before
    public void setUp() {
        controller = new AnalyzerTypeRestController(catalogService, managementService, mappingCatalogService,
                mappingService);
    }

    @Test
    public void getAnalyzerTypesReturnsComposedLabFacingCatalog() {
        AnalyzerTypeCatalogView expected = new AnalyzerTypeCatalogView("1.0", "sha256:test",
                new AnalyzerTypeCatalogView.CatalogSummary(0, 0, 0, 0), List.of());
        when(catalogService.getCatalog()).thenReturn(expected);

        assertSame(expected, controller.getAnalyzerTypes().getBody());
    }

    @Test
    public void getAnalyzerTypeReturnsTheExactComposedRevision() {
        AnalyzerTypeCatalogView.TypeSummary expected = new AnalyzerTypeCatalogView.TypeSummary("site.mock", 2,
                "sha256:test", "Mock revision 2", "OpenELIS", "Mock", "SITE", "ACTIVE", "ASTM",
                new AnalyzerTypeCatalogView.InstanceDefaults("ASTM_LIS2_A2", "BOTH", 9200), null, null, "51",
                new AnalyzerTypeCatalogView.MappingSummary(0, 1, "NOT_STARTED"),
                new AnalyzerTypeCatalogView.MappingSummary(0, 0, "NOT_APPLICABLE"), 1, "NEEDS_LOCAL_MAPPING",
                "PUBLISHED", "17", "2026-08-18T12:00:00Z");
        when(catalogService.getType("site.mock", 2)).thenReturn(expected);

        assertSame(expected, controller.getAnalyzerType("site.mock", 2).getBody());
    }

    @Test
    public void searchMappingTestsReturnsCompleteActiveCatalogMatches() {
        List<AnalyzerMappingCatalogService.TestOption> expected = List
                .of(new AnalyzerMappingCatalogService.TestOption("1", "HIV Viral Load", "HIVVL", List.of("25836-8")));
        when(mappingCatalogService.searchActiveTests("viral")).thenReturn(expected);

        assertSame(expected, controller.searchMappingTests("viral").getBody());
        verify(mappingCatalogService).searchActiveTests("viral");
    }

    @Test
    public void getMappingResultOptionsScopesChoicesToTheMappedTest() {
        List<AnalyzerMappingCatalogService.ResultOption> expected = List
                .of(new AnalyzerMappingCatalogService.ResultOption("11", "501", "Detected"));
        when(mappingCatalogService.getActiveResultOptions("1")).thenReturn(expected);

        assertSame(expected, controller.getMappingResultOptions("1").getBody());
        verify(mappingCatalogService).getActiveResultOptions("1");
    }

    @Test
    public void getMappingReturnsTheSoleSharedEditorDocumentForTheExactRevision() {
        BridgeProfileCatalog.ControlRecognitionSummary recognition = new BridgeProfileCatalog.ControlRecognitionSummary(
                "NONE", "This analyzer interface transports no control results.", true, List.of());
        AnalyzerTypeMappingView expected = new AnalyzerTypeMappingView("site.mock", 2, "sha256:test", "Mock Analyzer",
                "FILE", null, 0, null, List.of(), recognition);
        when(mappingService.getMapping("site.mock", 2)).thenReturn(expected);

        assertSame(expected, controller.getMapping("site.mock", 2).getBody());
        verify(mappingService).getMapping("site.mock", 2);
    }

    @Test
    public void duplicateUsesAuthenticatedUserAsBridgeActor() throws Exception {
        JsonNode response = objectMapper.readTree("{\"draftId\":\"draft-1\",\"kind\":\"DUPLICATE\"}");
        when(managementService.duplicate("site.mock", 3, "Mock Analyzer -1", "17")).thenReturn(response);
        AnalyzerTypeRestController.DuplicateProfileRequest request = new AnalyzerTypeRestController.DuplicateProfileRequest(
                3, "Mock Analyzer -1");

        ResponseEntity<JsonNode> result = controller.duplicate("site.mock", request, authenticatedRequest(17));

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertSame(response, result.getBody());
        verify(managementService).duplicate("site.mock", 3, "Mock Analyzer -1", "17");
    }

    @Test
    public void createDraftUsesAuthenticatedUserAndBridgeGeneratedIdentity() throws Exception {
        JsonNode response = objectMapper.readTree("{\"draftId\":\"draft-1\",\"kind\":\"CREATE\"}");
        when(managementService.createDraft("Site Mock Analyzer", "17")).thenReturn(response);

        ResponseEntity<JsonNode> result = controller.createDraft(
                new AnalyzerTypeRestController.CreateDraftRequest("Site Mock Analyzer"), authenticatedRequest(17));

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertSame(response, result.getBody());
        verify(managementService).createDraft("Site Mock Analyzer", "17");
    }

    @Test
    public void updateSharedStartsFromAnExplicitSourceRevision() throws Exception {
        JsonNode response = objectMapper.readTree("{\"draftId\":\"draft-2\",\"kind\":\"UPDATE\"}");
        when(managementService.updateShared("site.mock", 3, "17")).thenReturn(response);

        ResponseEntity<JsonNode> result = controller.updateShared("site.mock",
                new AnalyzerTypeRestController.SourceRevisionRequest(3), authenticatedRequest(17));

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertSame(response, result.getBody());
        verify(managementService).updateShared("site.mock", 3, "17");
    }

    @Test
    public void getDraftReturnsTheExactBridgeOwnedDraft() throws Exception {
        JsonNode response = objectMapper.readTree("{\"draftId\":\"draft-1\",\"kind\":\"DUPLICATE\"}");
        when(managementService.getDraft("draft-1")).thenReturn(response);

        ResponseEntity<JsonNode> result = controller.getDraft("draft-1");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertSame(response, result.getBody());
        verify(managementService).getDraft("draft-1");
    }

    @Test
    public void controllerExposesNoHardDeleteOrLegacyInstanceCrud() {
        for (Method method : AnalyzerTypeRestController.class.getDeclaredMethods()) {
            assertFalse(method.isAnnotationPresent(DeleteMapping.class));
            assertFalse(method.getName().toLowerCase().contains("instance"));
        }
    }

    @Test
    public void managementFailuresPreserveDeterministicStatusAndMessage() {
        ResponseEntity<AnalyzerTypeRestController.ErrorResponse> result = controller
                .handleProfileManagementError(new BridgeProfileManagementException(502, "Bridge unavailable"));

        assertEquals(HttpStatus.BAD_GATEWAY, result.getStatusCode());
        assertEquals("Bridge unavailable", result.getBody().error());
    }

    private static MockHttpServletRequest authenticatedRequest(int systemUserId) {
        UserSessionData user = new UserSessionData();
        user.setSytemUserId(systemUserId);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, user);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        return request;
    }
}
