package org.openelisglobal.analyzer.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.service.AnalyzerMappingConfirmationRequest;
import org.openelisglobal.analyzer.service.AnalyzerMappingConfirmationView;
import org.openelisglobal.analyzer.service.AnalyzerMappingEditorService;
import org.openelisglobal.analyzer.service.AnalyzerMappingUpdate;
import org.openelisglobal.analyzer.service.AnalyzerMappingView;
import org.openelisglobal.analyzer.service.BridgeProfileCatalog;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerMappingRestControllerTest {

    @Mock
    private AnalyzerMappingEditorService mappingService;

    private AnalyzerMappingRestController controller;

    @Before
    public void setUp() {
        controller = new AnalyzerMappingRestController(mappingService);
    }

    @Test
    public void getMappingReturnsTheAnalyzersOwnEditorDocument() {
        AnalyzerMappingView expected = view("42", "61", 1);
        when(mappingService.getMapping("42")).thenReturn(expected);

        assertSame(expected, controller.getMapping("42").getBody());
        verify(mappingService).getMapping("42");
    }

    @Test
    public void saveMappingUsesTheAuthenticatedUserAsTheAuditActor() {
        AnalyzerMappingUpdate update = new AnalyzerMappingUpdate(null, List.of(), List.of());
        AnalyzerMappingView expected = view("42", "62", 2);
        when(mappingService.saveMapping("42", update, "17")).thenReturn(expected);

        assertSame(expected, controller.saveMapping("42", update, authenticatedRequest(17)).getBody());
        verify(mappingService).saveMapping("42", update, "17");
    }

    @Test
    public void confirmMappingUsesTheAuthenticatedUserAsTheAuditActor() {
        AnalyzerMappingConfirmationRequest request = new AnalyzerMappingConfirmationRequest("sha256:" + "b".repeat(64),
                "sha256:" + "c".repeat(64), List.of(), List.of());
        AnalyzerMappingConfirmationView expected = AnalyzerMappingConfirmationView.unconfirmed();
        when(mappingService.confirmMapping("42", request, "17")).thenReturn(expected);

        assertSame(expected, controller.confirmMapping("42", request, authenticatedRequest(17)).getBody());
        verify(mappingService).confirmMapping("42", request, "17");
    }

    @Test
    public void aRejectedMappingCommandReturnsAVisibleBadRequest() {
        ResponseEntity<java.util.Map<String, Object>> response = controller
                .handleInvalidRequest(new IllegalArgumentException("The analyzer has no mapping"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("The analyzer has no mapping", response.getBody().get("error"));
    }

    private static AnalyzerMappingView view(String analyzerId, String mappingId, int revision) {
        BridgeProfileCatalog.ControlRecognitionSummary recognition = new BridgeProfileCatalog.ControlRecognitionSummary(
                "NONE", "This analyzer interface transports no control results.", true, List.of());
        return new AnalyzerMappingView(analyzerId, "site.mock", 2, "sha256:test", "Mock Analyzer", "FILE", mappingId,
                revision, "sha256:mapping", List.of(), recognition, null);
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
