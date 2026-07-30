package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.fhir.providers.SpecimenProvider;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
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