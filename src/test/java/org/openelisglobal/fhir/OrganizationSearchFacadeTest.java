package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.fhir.providers.OrganizationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

/**
 * Organization search answered from the OpenELIS database
 * (facade-organization.xml: Global Health Org, Health Services Inc, Community
 * Health Center).
 */
public class OrganizationSearchFacadeTest extends BaseWebContextSensitiveTest {

    private static final String GLOBAL_HEALTH_UUID = "f2cdeff8-8d5b-4023-bd7c-932b4b98b6d3";
    private static final String HEALTH_SERVICES_UUID = "a3bdeff8-8d5b-4023-bd7c-932b4b98b6d4";
    private static final String COMMUNITY_HEALTH_UUID = "c4dfeff8-8d5b-4023-bd7c-932b4b98b6d5";

    @Autowired
    private OrganizationProvider organizationProvider;

    @Autowired
    private javax.sql.DataSource dataSource;

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/facade-organization.xml");

        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(organizationProvider));
        MockServletConfig servletConfig = new MockServletConfig(new MockServletContext());
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);

        objectMapper = new ObjectMapper();
    }

    @Test
    public void search_withoutParameters_returnsEveryOrganization() throws Exception {
        JsonNode bundle = search();

        assertEquals(3, bundle.get("total").asInt());
        assertTrue(idsOf(bundle).containsAll(List.of(GLOBAL_HEALTH_UUID, HEALTH_SERVICES_UUID, COMMUNITY_HEALTH_UUID)));
    }

    @Test
    public void search_byId_returnsOnlyThatOrganization() throws Exception {
        assertEquals(List.of(HEALTH_SERVICES_UUID), idsOf(search("_id", HEALTH_SERVICES_UUID)));
        assertEquals(0, search("_id", "00000000-0000-0000-0000-000000000000").get("total").asInt());
    }

    @Test
    public void search_byIdentifier_matchesCodeShortNameCliaAndUuid() throws Exception {
        assertEquals(List.of(GLOBAL_HEALTH_UUID), idsOf(search("identifier", "GHG001")));
        assertEquals(List.of(GLOBAL_HEALTH_UUID), idsOf(search("identifier", "GHG")));
        assertEquals(List.of(HEALTH_SERVICES_UUID), idsOf(search("identifier", "CLIA67890")));
        assertEquals(List.of(COMMUNITY_HEALTH_UUID),
                idsOf(search("identifier", "http://openelis-global.org/org_code|CHC003")));
        assertEquals(0, search("identifier", "http://openelis-global.org/org_code|GHG").get("total").asInt());
        assertEquals(List.of(COMMUNITY_HEALTH_UUID),
                idsOf(search("identifier", "http://openelis-global.org/org_uuid|" + COMMUNITY_HEALTH_UUID)));
    }

    @Test
    public void search_byName_isPrefixAndCaseInsensitive() throws Exception {
        assertEquals(List.of(HEALTH_SERVICES_UUID), idsOf(search("name", "health services")));
        assertEquals(0, search("name", "Services").get("total").asInt());
        assertEquals(List.of(HEALTH_SERVICES_UUID), idsOf(search("name:contains", "Services")));
    }

    @Test
    public void search_byActive_mapsBooleanToYesNoColumn() throws Exception {
        assertEquals(2, search("active", "true").get("total").asInt());
        assertEquals(List.of(COMMUNITY_HEALTH_UUID), idsOf(search("active", "false")));
    }

    @Test
    public void search_byType_matchesOrganizationTypeName() throws Exception {
        assertEquals(2, search("type", "Healthcare").get("total").asInt());
        assertEquals(List.of(COMMUNITY_HEALTH_UUID), idsOf(search("type", "referingClinic")));
        assertEquals(0, search("type", "Pharmacy").get("total").asInt());
    }

    @Test
    public void search_byAddressCity_andActive_areAndCombined() throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/Organization");
        request.setQueryString("address-city=Chicago&active=true");
        request.addParameter("address-city", "Chicago");
        request.addParameter("active", "true");

        assertEquals(0, serve(request).get("total").asInt());
        assertEquals(List.of(COMMUNITY_HEALTH_UUID), idsOf(search("address-city", "Chicago")));
    }

    @Test
    public void search_byPartOf_andIncludes_followTheHierarchy() throws Exception {
        new JdbcTemplate(dataSource).update("UPDATE clinlims.organization SET org_id = 3 WHERE id IN (4, 5)");

        assertEquals(2, search("partof", "Organization/" + GLOBAL_HEALTH_UUID).get("total").asInt());
        assertEquals(2, search("partof", "3").get("total").asInt());

        MockHttpServletRequest request = buildFhirRequest("GET", "/Organization");
        request.setQueryString("_id=" + HEALTH_SERVICES_UUID + "&_include=Organization:partof");
        request.addParameter("_id", HEALTH_SERVICES_UUID);
        request.addParameter("_include", "Organization:partof");
        JsonNode bundle = serve(request);
        assertEquals(1, bundle.get("total").asInt());
        assertEquals(List.of(HEALTH_SERVICES_UUID, GLOBAL_HEALTH_UUID), idsOf(bundle));
        assertEquals("include", bundle.get("entry").get(1).get("search").get("mode").asText());

        request = buildFhirRequest("GET", "/Organization");
        request.setQueryString("_id=" + GLOBAL_HEALTH_UUID + "&_revinclude=Organization:partof");
        request.addParameter("_id", GLOBAL_HEALTH_UUID);
        request.addParameter("_revinclude", "Organization:partof");
        bundle = serve(request);
        assertEquals(1, bundle.get("total").asInt());
        assertTrue(idsOf(bundle).containsAll(List.of(GLOBAL_HEALTH_UUID, HEALTH_SERVICES_UUID, COMMUNITY_HEALTH_UUID)));
    }

    @Test
    public void search_returnsTransformedOrganizations() throws Exception {
        JsonNode resource = search("_id", GLOBAL_HEALTH_UUID).get("entry").get(0).get("resource");

        assertEquals("Organization", resource.get("resourceType").asText());
        assertEquals("Global Health Org", resource.get("name").asText());
        assertTrue(resource.get("active").asBoolean());
    }

    private JsonNode search() throws Exception {
        return serve(buildFhirRequest("GET", "/Organization"));
    }

    private JsonNode search(String param, String value) throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/Organization");
        request.setQueryString(param + "=" + value);
        request.addParameter(param, value);
        return serve(request);
    }

    private JsonNode serve(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);
        assertEquals(response.getContentAsString(), 200, response.getStatus());
        return objectMapper.readTree(response.getContentAsString());
    }

    private static List<String> idsOf(JsonNode bundle) {
        List<String> ids = new ArrayList<>();
        JsonNode entries = bundle.get("entry");
        if (entries == null) {
            return ids;
        }
        for (JsonNode entry : entries) {
            ids.add(entry.get("resource").get("id").asText());
        }
        return ids;
    }

    @Test
    public void search_byLastUpdated_andExposesMetaLastUpdated() throws Exception {
        assertEquals(List.of(COMMUNITY_HEALTH_UUID), idsOf(search("_lastUpdated", "ge2024-06-05")));
        assertEquals(List.of(GLOBAL_HEALTH_UUID), idsOf(search("_lastUpdated", "le2024-06-03")));
        assertEquals(3, search("_lastUpdated", "ge2024-06-01").get("total").asInt());
        assertEquals(0, search("_lastUpdated", "lt2024-01-01").get("total").asInt());
        JsonNode resource = search("_id", HEALTH_SERVICES_UUID).get("entry").get(0).get("resource");
        assertTrue(resource.get("meta").get("lastUpdated").asText().startsWith("2024-06-04"));
    }
}
