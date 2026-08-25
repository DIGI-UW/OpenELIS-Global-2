package org.openelisglobal.analyzer.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.form.AnalyzerInstanceRequest;
import org.openelisglobal.analyzer.service.AnalyzerInstanceService;
import org.openelisglobal.analyzer.service.AnalyzerInstanceState;
import org.openelisglobal.analyzer.service.AnalyzerInstanceView;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerInstanceRestControllerTest {

    private static final String FINGERPRINT = "sha256:" + "1".repeat(64);

    @Mock
    private AnalyzerInstanceService service;

    private AnalyzerInstanceRestController controller;
    private AnalyzerInstanceRequest input;
    private MockHttpServletRequest request;

    @Before
    public void setUp() {
        controller = new AnalyzerInstanceRestController(service);
        input = new AnalyzerInstanceRequest();
        input.setName("Synthetic bench 1");
        input.setProfileId("fixture.synthetic-connection");
        input.setProfileRevision(3);
        input.setTestUnitIds(List.of("7"));
        request = new MockHttpServletRequest();
        UserSessionData user = new UserSessionData();
        user.setSytemUserId(17);
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, user);
    }

    @Test
    public void returnsTheOpenElisReferenceAndTransientBridgeConnectionView() {
        AnalyzerInstanceView view = connectedView();
        when(service.create(input, "17")).thenReturn(view);

        ResponseEntity<Map<String, Object>> response = controller.create(input, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertEquals("42", body.get("id"));
        assertEquals("Synthetic bench 1", body.get("name"));
        assertEquals(List.of("7"), body.get("testUnitIds"));
        assertEquals("fixture.synthetic-connection", body.get("profileId"));
        assertEquals(3, body.get("profileRevision"));
        assertEquals("bridge-connection-42", body.get("bridgeConnectionId"));
        assertEquals(view.connection(), body.get("connection"));
        assertFalse(body.containsKey("connectionValues"));
        verify(service).create(input, "17");
    }

    @Test
    public void reloadsTheComposedReferenceAndBridgeView() {
        when(service.get("42")).thenReturn(connectedView());

        ResponseEntity<Map<String, Object>> response = controller.get("42");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("bridge-connection-42", response.getBody().get("bridgeConnectionId"));
        verify(service).get("42");
    }

    @Test
    public void updatesThroughTheSameGenericInstanceBoundary() {
        when(service.update("42", input, "17")).thenReturn(connectedView());

        ResponseEntity<Map<String, Object>> response = controller.update("42", input, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("42", response.getBody().get("id"));
        verify(service).update("42", input, "17");
    }

    @Test
    public void listsReferenceOnlyAnalyzerSummaries() {
        when(service.list()).thenReturn(List.of(connectedView().state()));

        ResponseEntity<Map<String, Object>> response = controller.list("synthetic", "SETUP");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> analyzers = (List<Map<String, Object>>) response.getBody().get("analyzers");
        assertEquals(1, analyzers.size());
        assertEquals("42", analyzers.get(0).get("id"));
        assertEquals("fixture.synthetic-connection", analyzers.get(0).get("profileId"));
        assertEquals(true, analyzers.get(0).get("connected"));
        assertFalse(analyzers.get(0).containsKey("connection"));
        verify(service).list();
    }

    private static AnalyzerInstanceView connectedView() {
        AnalyzerInstanceState state = new AnalyzerInstanceState("42", "Synthetic bench 1", List.of("7"),
                "fixture.synthetic-connection", 3, FINGERPRINT, "bridge-connection-42", Analyzer.AnalyzerStatus.SETUP);
        ObjectNode connection = new ObjectMapper().createObjectNode();
        connection.put("connectionId", "bridge-connection-42");
        connection.putArray("fields").addObject().put("key", "futureTextField");
        return new AnalyzerInstanceView(state, connection, null);
    }
}
