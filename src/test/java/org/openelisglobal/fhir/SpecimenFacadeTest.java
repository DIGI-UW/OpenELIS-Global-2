package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
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
    private IStatusService statusService;

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
    public void createSpecimen_shouldReturnCreatedSpecimenResource() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "sample_item" });

        String accession = "DEV01260000000000002";
        String typeCode = "BT";
        String typeDisplay = "Complete Blood Count";

        String jsonBody = """
                {
                  "resourceType": "Specimen",
                  "status": "available",

                  "accessionIdentifier": {
                    "use": "usual",
                    "system": "http://openelis-global.org/sampleItem_labNo",
                    "value": "%s"
                  },

                  "type": {
                    "coding": [
                      {
                        "system": "http://openelis-global.org/sampleType",
                        "code": "%s",
                        "display": "%s"
                      }
                    ]
                  },

                  "subject": {
                    "reference": "Patient/123"
                  },

                  "receivedTime": "2026-05-04T10:30:00Z",

                  "collection": {
                    "collectedDateTime": "2026-05-04T09:45:00Z",

                    "collector": {
                      "display": "Edie Maphie"
                    },

                    "bodySite": {
                      "coding": [
                        {
                          "system": "http://snomed.info/sct",
                          "code": "368208006",
                          "display": "Blood"
                        }
                      ]
                    },

                    "method": {
                      "coding": [
                        {
                          "system": "http://snomed.info/sct",
                          "code": "258580003",
                          "display": "Venipuncture"
                        }
                      ]
                    }
                  },

                  "container": [
                    {
                      "type": {
                        "coding": [
                          {
                            "system": "http://snomed.info/sct",
                            "code": "434711009",
                            "display": "Specimen container"
                          }
                        ]
                      },
                      "specimenQuantity": {
                        "value": 5,
                        "unit": "g/L",
                        "system": "http://unitsofmeasure.org",
                        "code": "g/L"
                      }
                    }
                  ],

                  "note": [
                    { "text": "Sample slightly hemolyzed" },
                    { "text": "Patient was fasting" }
                  ]
                }
                """.formatted(accession, typeCode, typeDisplay);

        MockHttpServletRequest request = buildFhirRequest("POST", "/Specimen");
        request.setContentType("application/json");
        request.setContent(jsonBody.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(201, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("Specimen", jsonResponse.get("resourceType").asText());

        // Optional (but useful)
        assertEquals("available", jsonResponse.get("status").asText());

        assertEquals(typeDisplay, jsonResponse.get("type").get("coding").get(0).get("display").asText());
    }

    @Test
    public void updateSpecimen_shouldUpdateOnlyCollector() throws Exception {

        String accession = "DEV01260000000000001";
        String typeCode = "BT";
        String typeDisplay = "Complete Blood Count";
        String specimenUUID = "68438220-5cef-44c4-9e6f-9f88e6b93270";

        String jsonBody = """
                {
                  "resourceType": "Specimen",
                  "id": "%s",
                  "status": "available",

                  "accessionIdentifier": {
                    "use": "usual",
                    "system": "http://openelis-global.org/sampleItem_labNo",
                    "value": "%s"
                  },

                  "type": {
                    "coding": [
                      {
                        "system": "http://openelis-global.org/sampleType",
                        "code": "%s",
                        "display": "%s"
                      }
                    ]
                  },

                  "subject": {
                    "reference": "Patient/123"
                  },

                  "receivedTime": "2026-05-04T10:30:00Z",

                  "collection": {
                    "collectedDateTime": "2026-05-04T09:45:00Z",

                    "collector": {
                      "display": "Emily Maphie"
                    },

                    "bodySite": {
                      "coding": [
                        {
                          "system": "http://snomed.info/sct",
                          "code": "368208006",
                          "display": "Blood"
                        }
                      ]
                    },

                    "method": {
                      "coding": [
                        {
                          "system": "http://snomed.info/sct",
                          "code": "258580003",
                          "display": "Venipuncture"
                        }
                      ]
                    }
                  },

                  "container": [
                    {
                      "type": {
                        "coding": [
                          {
                            "system": "http://snomed.info/sct",
                            "code": "434711009",
                            "display": "Specimen container"
                          }
                        ]
                      },
                      "specimenQuantity": {
                        "value": 5,
                        "unit": "g/L",
                        "system": "http://unitsofmeasure.org",
                        "code": "g/L"
                      }
                    }
                  ],

                  "note": [
                    { "text": "Sample slightly hemolyzed" },
                    { "text": "Patient was fasting" }
                  ]
                }
                """.formatted(specimenUUID, accession, typeCode, typeDisplay);

        MockHttpServletRequest request = buildFhirRequest("PUT", "/Specimen" + "/" + specimenUUID);
        request.setContentType("application/json");
        request.setContent(jsonBody.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("Specimen", jsonResponse.get("resourceType").asText());

    }

    @Test
    public void updateSpecimen_withTheResourceAsRead_keepsTheItemAsItWas() throws Exception {
        String specimenUuid = "68438220-5cef-44c4-9e6f-9f88e6b93271";
        JsonNode asRead = readSpecimen(specimenUuid);
        assertEquals("DEV01260000000000002-12", asRead.get("accessionIdentifier").get("value").asText());

        MockHttpServletResponse response = putSpecimen(specimenUuid, asRead.toString());

        assertEquals(response.getContentAsString(), 200, response.getStatus());
        SampleItem stored = sampleItemService.get("2");
        assertEquals("12", stored.getSortOrder());
        assertEquals("2", stored.getSample().getId());
        assertEquals(asRead.get("accessionIdentifier"), readSpecimen(specimenUuid).get("accessionIdentifier"));
    }

    @Test
    public void updateSpecimen_withTheResourceAsRead_keepsARejectedItemRejected() throws Exception {
        String rejected = statusService.getStatusID(SampleStatus.SampleRejected);
        SampleItem item = sampleItemService.get("1");
        item.setStatusId(rejected);
        item.setSysUserId("1");
        sampleItemService.update(item);
        String specimenUuid = item.getFhirUuidAsString();

        MockHttpServletResponse response = putSpecimen(specimenUuid, readSpecimen(specimenUuid).toString());

        assertEquals(response.getContentAsString(), 200, response.getStatus());
        assertEquals(rejected, sampleItemService.get("1").getStatusId());
    }

    @Test
    public void updateSpecimen_withTheResourceAsRead_doesNotRepeatTheCollectionConditions() throws Exception {
        SampleItem item = sampleItemService.get("1");
        item.setCollectionConditions("Fasting 12h");
        item.setSysUserId("1");
        sampleItemService.update(item);
        String specimenUuid = item.getFhirUuidAsString();

        MockHttpServletResponse response = putSpecimen(specimenUuid, readSpecimen(specimenUuid).toString());

        assertEquals(response.getContentAsString(), 200, response.getStatus());
        assertEquals("Fasting 12h", sampleItemService.get("1").getCollectionConditions());
    }

    @Test
    public void updateSpecimen_namingAnotherOrder_isRejectedAs400() throws Exception {
        String specimenUuid = "68438220-5cef-44c4-9e6f-9f88e6b93270";
        ObjectNode moved = (ObjectNode) readSpecimen(specimenUuid);
        ((ObjectNode) moved.get("accessionIdentifier")).put("value", "DEV01260000000000002-1");

        MockHttpServletResponse response = putSpecimen(specimenUuid, moved.toString());

        assertEquals(response.getContentAsString(), 400, response.getStatus());
        assertEquals("1", sampleItemService.get("1").getSample().getId());
    }

    @Test
    public void createSpecimen_isNumberedAfterTheOrdersExistingItems() throws Exception {
        String jsonBody = """
                {
                  "resourceType": "Specimen",
                  "status": "available",
                  "accessionIdentifier": { "value": "DEV01260000000000002" },
                  "type": { "coding": [{ "code": "BT", "display": "Complete Blood Count" }] },
                  "collection": { "collectedDateTime": "2026-05-04T09:45:00Z" }
                }
                """;
        MockHttpServletRequest request = buildFhirRequest("POST", "/Specimen");
        request.setContentType("application/json");
        request.setContent(jsonBody.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(response.getContentAsString(), 201, response.getStatus());
        assertEquals("DEV01260000000000002-13",
                objectMapper.readTree(response.getContentAsString()).get("accessionIdentifier").get("value").asText());
    }

    @Test
    public void createSpecimen_withoutASampleType_isRefusedAs422() throws Exception {
        MockHttpServletRequest request = buildFhirRequest("POST", "/Specimen");
        request.setContentType("application/json");
        request.setContent("""
                {
                  "resourceType": "Specimen",
                  "accessionIdentifier": { "value": "DEV01260000000000002" }
                }
                """.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(response.getContentAsString(), 422, response.getStatus());
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

    @Test
    public void updateSpecimen_withTheResourceAsRead_keepsAUnitWithAUcumCode() throws Exception {
        String specimenUuid = specimenWithQuantity("10^3/µl", "10*3/uL");

        MockHttpServletResponse response = putSpecimen(specimenUuid, readSpecimen(specimenUuid).toString());

        assertEquals(response.getContentAsString(), 200, response.getStatus());
        assertEquals("1400", sampleItemService.get("2").getUnitOfMeasure().getId());
    }

    @Test
    public void updateSpecimen_withOnlyAUcumCode_findsTheUnitThatCarriesIt() throws Exception {
        String specimenUuid = specimenWithQuantity("10^3/µl", "10*3/uL");
        jdbcSetUnitOfMeasure(null);
        ObjectNode asRead = (ObjectNode) readSpecimen(specimenUuid);
        ObjectNode quantity = (ObjectNode) asRead.get("container").get(0).get("specimenQuantity");
        quantity.remove("unit");
        quantity.put("system", "http://unitsofmeasure.org");
        quantity.put("code", "10*3/uL");

        MockHttpServletResponse response = putSpecimen(specimenUuid, asRead.toString());

        assertEquals(response.getContentAsString(), 200, response.getStatus());
        assertEquals("1400", sampleItemService.get("2").getUnitOfMeasure().getId());
    }

    @Test
    public void updateSpecimen_withTheResourceAsRead_keepsALabTypedUnit() throws Exception {
        String specimenUuid = specimenWithQuantity("10^3/µl", null);
        String unitId = sampleItemService.get("2").getUnitOfMeasure().getId();

        MockHttpServletResponse response = putSpecimen(specimenUuid, readSpecimen(specimenUuid).toString());

        assertEquals(response.getContentAsString(), 200, response.getStatus());
        assertEquals("1400", sampleItemService.get("2").getUnitOfMeasure().getId());
    }

    private void jdbcSetUnitOfMeasure(String uomId) {
        new JdbcTemplate(dataSource).update("UPDATE clinlims.sample_item SET uom_id = CAST(? AS numeric) WHERE id = 2",
                uomId);
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

    private MockHttpServletResponse putSpecimen(String specimenUuid, String body) throws Exception {
        MockHttpServletRequest request = buildFhirRequest("PUT", "/Specimen/" + specimenUuid);
        request.setContentType("application/json");
        request.setContent(body.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);
        return response;
    }

    @Test
    public void deleteSpecimen_shouldReturn204() throws Exception {
        SampleItem existingSampleItem = sampleItemService.get("1");
        assertNotNull("Test specimen with id=1 must exist", existingSampleItem);
        String specimenUuid = existingSampleItem.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("DELETE", "/Specimen/" + specimenUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(204, response.getStatus());
        SampleItem sampleItem = sampleItemService.get("1");
        assertTrue(sampleItem.isRejected());

    }

}