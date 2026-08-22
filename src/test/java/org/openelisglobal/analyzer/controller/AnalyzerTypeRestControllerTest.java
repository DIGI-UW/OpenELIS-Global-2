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
import org.openelisglobal.analyzer.service.AnalyzerControlRecognitionUpdate;
import org.openelisglobal.analyzer.service.AnalyzerMappingCatalogService;
import org.openelisglobal.analyzer.service.AnalyzerSiteBindingConfirmationRequest;
import org.openelisglobal.analyzer.service.AnalyzerSiteBindingConfirmationView;
import org.openelisglobal.analyzer.service.AnalyzerTypeCatalogService;
import org.openelisglobal.analyzer.service.AnalyzerTypeCatalogView;
import org.openelisglobal.analyzer.service.AnalyzerTypeMappingService;
import org.openelisglobal.analyzer.service.AnalyzerTypeMappingUpdate;
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
    public void saveMappingUsesTheAuthenticatedUserAsTheAuditActor() {
        BridgeProfileCatalog.ControlRecognitionSummary recognition = new BridgeProfileCatalog.ControlRecognitionSummary(
                "NONE", "This analyzer interface transports no control results.", true, List.of());
        AnalyzerTypeMappingUpdate update = new AnalyzerTypeMappingUpdate(null, List.of(), List.of());
        AnalyzerTypeMappingView expected = new AnalyzerTypeMappingView("site.mock", 2, "sha256:test", "Mock Analyzer",
                "FILE", "51", 1, "sha256:binding", List.of(), recognition);
        when(mappingService.saveMapping("site.mock", 2, update, "17")).thenReturn(expected);

        assertSame(expected, controller.saveMapping("site.mock", 2, update, authenticatedRequest(17)).getBody());
        verify(mappingService).saveMapping("site.mock", 2, update, "17");
    }

    @Test
    public void confirmMappingUsesTheAuthenticatedUserAsTheAuditActor() {
        AnalyzerSiteBindingConfirmationRequest request = new AnalyzerSiteBindingConfirmationRequest(
                "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                "sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc", List.of(), List.of());
        AnalyzerSiteBindingConfirmationView expected = AnalyzerSiteBindingConfirmationView.unconfirmed();
        when(mappingService.confirmMapping("site.mock", 2, request, "17")).thenReturn(expected);

        assertSame(expected, controller.confirmMapping("site.mock", 2, request, authenticatedRequest(17)).getBody());
        verify(mappingService).confirmMapping("site.mock", 2, request, "17");
    }

    @Test
    public void invalidMappingCommandsReturnAVisibleBadRequest() {
        ResponseEntity<AnalyzerTypeRestController.ErrorResponse> response = controller
                .handleInvalidMapping(new IllegalArgumentException("Every source row must be resolved"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Every source row must be resolved", response.getBody().error());
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
    public void controlRecognitionDraftCommandsUseTheAuthenticatedOpenElisActor() throws Exception {
        JsonNode current = objectMapper.readTree("{\"draftId\":\"draft-1\",\"recognition\":{\"mode\":\"RULES\"}}");
        JsonNode changed = objectMapper.readTree("{\"draftId\":\"draft-1\",\"recognition\":{\"mode\":\"NONE\"}}");
        AnalyzerControlRecognitionUpdate update = new AnalyzerControlRecognitionUpdate("NONE", true, List.of());
        when(managementService.getControlRecognition("draft-1")).thenReturn(current);
        when(managementService.updateControlRecognition("draft-1", update, "17")).thenReturn(changed);

        assertSame(current, controller.getControlRecognition("draft-1").getBody());
        assertSame(changed, controller.updateControlRecognition("draft-1", update, authenticatedRequest(17)).getBody());

        verify(managementService).getControlRecognition("draft-1");
        verify(managementService).updateControlRecognition("draft-1", update, "17");
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
