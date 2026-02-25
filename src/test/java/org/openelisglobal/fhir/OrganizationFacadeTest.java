package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.fhir.providers.OrganizationProvider;
import org.openelisglobal.organization.service.OrganizationContactService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.service.OrganizationTypeService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.organization.valueholder.OrganizationContact;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

public class OrganizationFacadeTest extends BaseWebContextSensitiveTest {

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private OrganizationTypeService organizationTypeService;

    @Autowired
    private OrganizationContactService organizationContactService;

    @Autowired
    private OrganizationProvider organizationProvider;

    @Autowired
    private MockServletContext servletContext;

    @Autowired
    private FhirContext fhirContext;

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        fhirContext = FhirContext.forR4();

        fhirServlet = new RestfulServer(fhirContext);
        fhirServlet.setResourceProviders(Arrays.asList(organizationProvider));

        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);

        objectMapper = new ObjectMapper();

        executeDataSetWithStateManagement("testdata/facade-organization.xml");
    }

    private void cleanupDatabase() {
        List<OrganizationContact> contacts = organizationContactService.getAll();
        if (contacts != null && !contacts.isEmpty()) {
            organizationContactService.deleteAll(contacts);
        }

        List<OrganizationType> types = organizationTypeService.getAll();
        if (types != null && !types.isEmpty()) {
            organizationTypeService.deleteAll(types);
        }

        List<Organization> orgs = organizationService.getAll();
        if (orgs != null && !orgs.isEmpty()) {
            organizationService.deleteAll(orgs);
        }
    }

    private MockHttpServletRequest buildRequest(String method, String pathInfo) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setContextPath("");
        request.setServletPath("/fhir");
        request.setPathInfo(pathInfo);
        request.setRequestURI("/fhir" + pathInfo);
        request.setContentType("application/fhir+json");
        request.addHeader("Accept", "application/fhir+json");
        return request;
    }

    @Test
    public void readOrganization_shouldReturnOrganizationResource() throws Exception {
        Organization existingOrg = organizationService.get("3");
        assertNotNull("Test organization with id=3 must exist", existingOrg);
        String orgUuid = existingOrg.getFhirUuidAsString();

        MockHttpServletRequest request = buildRequest("GET", "/Organization/" + orgUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("Organization", jsonResponse.get("resourceType").asText());
        assertEquals(orgUuid, jsonResponse.get("id").asText());

        // Verify organization name from test data
        assertEquals("Global Health Org", jsonResponse.get("name").asText());
    }

    @Test
    public void readOrganization_withNonExistentId_shouldReturn404() throws Exception {
        String nonExistentUuid = "00000000-0000-0000-0000-000000000000";
        MockHttpServletRequest request = buildRequest("GET", "/Organization/" + nonExistentUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", jsonResponse.get("resourceType").asText());
    }

    @Test
    public void createOrganization_shouldReturnSuccess() throws Exception {
        cleanupDatabase();

        MockHttpServletRequest request = buildRequest("POST", "/Organization");

        String organizationJson = """
                {
                  "resourceType": "Organization",
                  "active": true,
                  "type": [
                    {
                      "coding": [
                        {
                          "system": "http://openelis-global.org/orgType",
                          "code": "CLINIC"
                        }
                      ],
                      "text": "Regional Laboratory"
                    }
                  ],
                  "name": "Green Laboratory",
                  "address": [
                    {
                      "line": ["Plot 88 Entebbe Road"],
                      "city": "Kampala",
                      "postalCode": "256"
                    }
                  ]
                }
                """;

        request.setContent(organizationJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(201, response.getStatus());
        assertEquals("application/fhir+json;charset=UTF-8", response.getContentType());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("Organization", jsonResponse.get("resourceType").asText());
        assertNotNull(jsonResponse.get("id"));

        List<Organization> savedOrgs = organizationService.getAll();
        assertFalse(savedOrgs.isEmpty());

        Organization savedOrg = savedOrgs.get(0);
        assertEquals("Green Laboratory", savedOrg.getOrganizationName());
        assertNotNull(savedOrg.getFhirUuid());
    }

    @Test
    public void updateOrganization_shouldReturnSuccess() throws Exception {
        Organization existingOrg = organizationService.get("3");
        String orgUuid = existingOrg.getFhirUuidAsString();

        MockHttpServletRequest request = buildRequest("PUT", "/Organization/" + orgUuid);

        String updateJson = """
                {
                  "resourceType": "Organization",
                  "id": "%s",
                  "active": true,
                  "type": [
                    {
                      "coding": [
                        {
                          "system": "http://openelis-global.org/orgType",
                          "code": "LABORATORY"
                        }
                      ],
                      "text": "Updated Laboratory"
                    }
                  ],
                  "name": "Updated Health Organization",
                  "address": [
                    {
                      "line": ["Updated Address Line"],
                      "city": "Updated City",
                      "postalCode": "99999"
                    }
                  ]
                }
                """.formatted(orgUuid);

        request.setContent(updateJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals(orgUuid, jsonResponse.get("id").asText());
        assertEquals("Updated Health Organization", jsonResponse.get("name").asText());

        Organization updatedOrg = organizationService.getOrganizationByFhirId(orgUuid);
        assertNotNull(updatedOrg);
        assertEquals("Updated Health Organization", updatedOrg.getOrganizationName());

    }

    @Test
    public void deleteOrganization_shouldSetOrganizationInactive() throws ServletException, IOException {
        Organization existingOrg = organizationService.get("3");
        String orgUuid = existingOrg.getFhirUuidAsString();
        MockHttpServletRequest request = buildRequest("DELETE", "/Organization/" + orgUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(204, response.getStatus());

        Organization deletedOrg = organizationService.getOrganizationByFhirId(orgUuid);
        assertNotNull(deletedOrg);
        assertEquals("N", deletedOrg.getIsActive());
        assertFalse(deletedOrg.getIsActive().equals("Y"));
    }
}