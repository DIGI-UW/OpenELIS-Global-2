package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.fhir.providers.TaskProvider;
import org.openelisglobal.sample.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.annotation.Rollback;

@Rollback
public class TaskFacadeTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TaskProvider taskProvider;

    @Autowired
    private SampleService sampleService;

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/result.xml");
        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(taskProvider));
        MockServletConfig config = new MockServletConfig(new MockServletContext());
        fhirServlet.init(config);
        objectMapper = new ObjectMapper();
    }

    @Test
    public void readTask_shouldReturn200() throws Exception {

        String sampleFhirUuid = "bbbbbbbb-0000-0000-0000-000000000001";

        MockHttpServletRequest request = buildFhirRequest("GET", "/Task/" + sampleFhirUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("Task", json.get("resourceType").asText());
        assertEquals(sampleFhirUuid, json.get("id").asText());
    }

    @Test
    public void readTask_nonExistent_shouldReturn404() throws Exception {
        String nonExistentUuid = "00000000-0000-0000-0000-000000000000";
        MockHttpServletRequest request = buildFhirRequest("GET", "/Task/" + nonExistentUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", json.get("resourceType").asText());
    }

    @Test
    public void deleteTask_shouldReturn204() throws Exception {
        // fhir_uuid from result.xml sample id="1"
        String sampleFhirUuid = "bbbbbbbb-0000-0000-0000-000000000001";

        MockHttpServletRequest request = buildFhirRequest("DELETE", "/Task/" + sampleFhirUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(204, response.getStatus());
    }

    @Test
    public void deleteTask_nonExistent_shouldReturn404() throws Exception {
        String nonExistentUuid = "00000000-0000-0000-0000-000000000000";
        MockHttpServletRequest request = buildFhirRequest("DELETE", "/Task/" + nonExistentUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());
    }

    @Test
    public void updateTask_invalidTransition_shouldReturn400() throws Exception {

        String sampleFhirUuid = "bbbbbbbb-0000-0000-0000-000000000001";

        MockHttpServletRequest request = buildFhirRequest("PUT", "/Task/" + sampleFhirUuid);
        String updateJson = """
                {
                  "resourceType": "Task",
                  "id": "%s",
                  "status": "completed",
                  "intent": "order"
                }
                """.formatted(sampleFhirUuid);
        request.setContent(updateJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(400, response.getStatus());
    }

    @Test
    public void updateTask_validTransition_shouldReturn200() throws Exception {

        String sampleFhirUuid = "bbbbbbbb-0000-0000-0000-000000000001";

        MockHttpServletRequest request = buildFhirRequest("PUT", "/Task/" + sampleFhirUuid);
        org.openelisglobal.login.valueholder.UserSessionData usd = new org.openelisglobal.login.valueholder.UserSessionData();
        usd.setSytemUserId(1);
        request.getSession().setAttribute(org.openelisglobal.common.action.IActionConstants.USER_SESSION_DATA, usd);
        String updateJson = """
                {
                  "resourceType": "Task",
                  "id": "%s",
                  "status": "in-progress",
                  "intent": "order"
                }
                """.formatted(sampleFhirUuid);
        request.setContent(updateJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("Task", json.get("resourceType").asText());
    }

    @Test
    public void searchTasks_shouldReturnBundle() throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/Task");
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("Bundle", json.get("resourceType").asText());
    }
}
