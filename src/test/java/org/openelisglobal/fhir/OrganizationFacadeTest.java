package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void readOrganization_shouldReturnOrganizationResource() throws Exception {
        Organization existingOrg = organizationService.get("3");
        assertNotNull("Test organization with id=3 must exist", existingOrg);
        String orgUuid = existingOrg.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Organization/" + orgUuid);
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
        MockHttpServletRequest request = buildFhirRequest("GET", "/Organization/" + nonExistentUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", jsonResponse.get("resourceType").asText());
    }

    @Test
    public void createOrganization_shouldReturnSuccess() throws Exception {
        cleanupDatabase();

        MockHttpServletRequest request = buildFhirRequest("POST", "/Organization");

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

        MockHttpServletRequest request = buildFhirRequest("PUT", "/Organization/" + orgUuid);

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
        MockHttpServletRequest request = buildFhirRequest("DELETE", "/Organization/" + orgUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(204, response.getStatus());

        Organization deletedOrg = organizationService.getOrganizationByFhirId(orgUuid);
        assertNotNull(deletedOrg);
        assertEquals("N", deletedOrg.getIsActive());
        assertFalse(deletedOrg.getIsActive().equals("Y"));
    }

    @Test
    public void createOrganization_withPartOf_linksParentAndAnswersPartofSearch() throws Exception {
        cleanupDatabase();

        JsonNode parent = post("/Organization", """
                {"resourceType": "Organization", "active": true, "name": "Parent Laboratory"}
                """, 201);
        String parentId = parent.get("id").asText();

        JsonNode child = post("/Organization", """
                {"resourceType": "Organization", "active": true, "name": "Child Ward",
                 "partOf": {"reference": "Organization/%s"}}
                """.formatted(parentId), 201);
        assertEquals("Organization/" + parentId, child.get("partOf").get("reference").asText());

        MockHttpServletRequest request = buildFhirRequest("GET", "/Organization");
        request.setQueryString("partof=Organization/" + parentId);
        request.addParameter("partof", "Organization/" + parentId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);
        assertEquals(200, response.getStatus());
        JsonNode bundle = objectMapper.readTree(response.getContentAsString());
        assertEquals(1, bundle.get("total").asInt());
        assertEquals(child.get("id").asText(), bundle.get("entry").get(0).get("resource").get("id").asText());
    }

    @Test
    public void createOrganization_withUnknownPartOf_returns422() throws Exception {
        JsonNode outcome = post("/Organization", """
                {"resourceType": "Organization", "active": true, "name": "Orphan Ward",
                 "partOf": {"reference": "Organization/00000000-0000-0000-0000-000000000000"}}
                """, 422);
        assertEquals("OperationOutcome", outcome.get("resourceType").asText());
    }

    /**
     * STATE is a two character US state code. The rejection has to name the field
     * and its limit: it used to surface as the entire generated INSERT with every
     * bound value inlined, which told a client nothing it could act on.
     */
    @Test
    public void createOrganization_withOverlongState_returns422NotServerError() throws Exception {
        JsonNode outcome = post("/Organization", """
                {"resourceType": "Organization", "active": true, "name": "Wide State Clinic",
                 "address": [{"city": "Kampala", "state": "Central Region"}]}
                """, 422);
        String diagnostics = outcome.get("issue").get(0).get("diagnostics").asText();
        assertTrue("the error should name the field, but was: " + diagnostics,
                diagnostics.contains("Organization.address.state"));
        assertTrue("the error should state the limit, but was: " + diagnostics, diagnostics.contains("2 characters"));
        assertFalse("the error should not carry the generated SQL, but was: " + diagnostics,
                diagnostics.contains("insert into"));
    }

    private JsonNode post(String path, String json, int expectedStatus) throws Exception {
        MockHttpServletRequest request = buildFhirRequest("POST", path);
        request.setContent(json.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);
        assertEquals(response.getContentAsString(), expectedStatus, response.getStatus());
        return objectMapper.readTree(response.getContentAsString());
    }
}
