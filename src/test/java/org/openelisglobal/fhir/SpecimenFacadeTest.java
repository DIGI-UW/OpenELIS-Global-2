package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.fhir.providers.SpecimenProvider;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

public class SpecimenFacadeTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private SpecimenProvider specimenProvider;

    @Autowired
    private MockServletContext servletContext;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private FhirContext fhirContext;

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        fhirContext = FhirContext.forR4();

        fhirServlet = new RestfulServer(fhirContext);
        fhirServlet.setResourceProviders(Arrays.asList(specimenProvider));

        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);

        objectMapper = new ObjectMapper();

        executeDataSetWithStateManagement("testdata/facade-specimen.xml");
    }

    @Test
    public void readSpecimen_shouldReturnSpecimenResource() throws Exception {
        SampleItem existingSampleItem = sampleItemService.get("1");
        assertNotNull("Test specimen with id=1 must exist", existingSampleItem);
        String specimenUuid = existingSampleItem.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Specimen/" + specimenUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("available", jsonResponse.get("status").asText());

        JsonNode codingArray = jsonResponse.get("type").get("coding");

        assertNotNull("Coding array should not be null", codingArray);
        assertEquals("Complete Blood Count", codingArray.get(0).get("display").asText());
    }

    @Test
    public void readSpecimen_shouldReturn404GivenNonExistingId() throws Exception {
        String specimenUuid = "00000000-0000-0000-0000-000000000000";

        MockHttpServletRequest request = buildFhirRequest("GET", "/Specimen/" + specimenUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());

    }

    @Test
    public void readSpecimen_aUcumUnit_isSentWithTheUcumSystem() throws Exception {
        String specimenUuid = specimenWithQuantity("g/L", null);

        JsonNode quantity = readSpecimen(specimenUuid).get("container").get(0).get("specimenQuantity");

        assertEquals("g/L", quantity.get("unit").asText());
        assertEquals("http://unitsofmeasure.org", quantity.get("system").asText());
        assertEquals("g/L", quantity.get("code").asText());
    }

    @Test
    public void readSpecimen_aLabTypedUnit_isSentAsTextOnly() throws Exception {
        String specimenUuid = specimenWithQuantity("10^3/µl", null);

        JsonNode quantity = readSpecimen(specimenUuid).get("container").get(0).get("specimenQuantity");

        assertEquals("10^3/µl", quantity.get("unit").asText());
        assertFalse("not a UCUM code, so no UCUM system", quantity.has("system"));
        assertFalse(quantity.has("code"));
    }

    @Test
    public void readSpecimen_aLabTypedUnitWithAUcumCode_sendsThatCode() throws Exception {
        String specimenUuid = specimenWithQuantity("10^3/µl", "10*3/uL");

        JsonNode quantity = readSpecimen(specimenUuid).get("container").get(0).get("specimenQuantity");

        assertEquals("10^3/µl", quantity.get("unit").asText());
        assertEquals("http://unitsofmeasure.org", quantity.get("system").asText());
        assertEquals("10*3/uL", quantity.get("code").asText());
    }

    private String specimenWithQuantity(String unitName, String ucumCode) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO clinlims.unit_of_measure (id, name, description, ucum_code, is_active, lastupdated)"
                + " VALUES (1400, ?, ?, ?, 'Y', now()) ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name,"
                + " ucum_code = EXCLUDED.ucum_code", unitName, unitName, ucumCode);
        jdbc.update("UPDATE clinlims.sample_item SET quantity = 3, uom_id = 1400 WHERE id = 2");
        return "68438220-5cef-44c4-9e6f-9f88e6b93271";
    }

    private JsonNode readSpecimen(String specimenUuid) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(buildFhirRequest("GET", "/Specimen/" + specimenUuid), response);
        assertEquals(200, response.getStatus());
        return objectMapper.readTree(response.getContentAsString());
    }

}