package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.fhir.providers.ObservationProvider;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

public class ObservationFacadeTest extends BaseWebContextSensitiveTest {

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Autowired
    private ResultService resultService;

    @Autowired
    private ObservationProvider observationProvider;

    @Autowired
    private PanelService panelService;

    @Autowired
    private LocalizationService localizationSevice;

    @Autowired
    private AnalysisService analysisService;

    private MockServletContext servletContext;

    @Before
    public void setUp() throws Exception {

        executeDataSetWithStateManagement("testdata/result-facade.xml");

        servletContext = new MockServletContext();

        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(observationProvider));

        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter("name", "FhirServlet");

        fhirServlet.init(servletConfig);

        objectMapper = new ObjectMapper();

    }

    @Test
    public void readObservation_shouldReturnSuccess() throws Exception {

        String fhirUuid = "550e8400-e29b-41d4-a716-446655440004";

        Result result = resultService.getResultByFhirUuid(fhirUuid);
        assertNotNull("Result not found in test data", result);

        MockHttpServletRequest request = buildFhirRequest("GET", "/Observation/" + fhirUuid);

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("Observation", jsonResponse.get("resourceType").asText());

        assertEquals("final", jsonResponse.get("status").asText());
    }

    @Test
    public void createObservation_withoutLoincCode_shouldReturn422() throws Exception {

        String patientFhirUuid = "550e8400-e29b-41d4-a716-446655440001";
        String specimenFhirUuid = "68438220-5cef-44c4-9e6f-9f88e6b93270";
        String analysisFhirUuid = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";

        Analysis analysis = analysisService.getAnalysisById("1");

        Localization localizationOld = new Localization();
        localizationOld.setDescription("Test Panel");
        localizationOld.setLastupdated(new Timestamp(System.currentTimeMillis()));

        Localization savedLocalization = localizationSevice.save(localizationOld);

        Panel newPanel = new Panel();
        newPanel.setPanelName("New Panel Name");
        newPanel.setDescription("A test panel from dataset.");
        newPanel.setLocalization(savedLocalization);

        Panel panel = panelService.save(newPanel);

        analysis.setPanel(panel);
        analysisService.save(analysis);

        assertNotNull("Analysis reference required for creating result", analysis);

        MockHttpServletRequest request = buildFhirRequest("POST", "/Observation");

        String createJson = """
                {
                  "resourceType": "Observation",
                  "status": "final",
                  "code": {
                    "coding": [{
                      "system": "http://loinc.org"
                    }]
                  },
                  "subject": {
                    "reference": "Patient/%s"
                  },
                  "specimen": {
                    "reference": "Specimen/%s"
                  },
                  "basedOn": [{
                    "reference": "ServiceRequest/%s"
                  }],
                  "effectiveDateTime": "2026-03-09T10:00:00+03:00",
                  "valueQuantity": {
                    "value": 85.5,
                    "unit": "g/L"
                  }
                }
                """.formatted(patientFhirUuid, specimenFhirUuid, analysisFhirUuid);

        request.setContent(createJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(422, response.getStatus());
    }

    @Test
    public void createObservation_withInvalidLoincCode_shouldReturn422() throws Exception {

        String patientFhirUuid = "550e8400-e29b-41d4-a716-446655440001";
        String specimenFhirUuid = "68438220-5cef-44c4-9e6f-9f88e6b93270";
        String analysisFhirUuid = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";

        Analysis analysis = analysisService.getAnalysisById("1");

        Localization localizationOld = new Localization();
        localizationOld.setDescription("Test Panel");
        localizationOld.setLastupdated(new Timestamp(System.currentTimeMillis()));

        Localization savedLocalization = localizationSevice.save(localizationOld);

        Panel newPanel = new Panel();
        newPanel.setPanelName("New Panel Name");
        newPanel.setDescription("A test panel from dataset.");
        newPanel.setLocalization(savedLocalization);

        Panel panel = panelService.save(newPanel);

        analysis.setPanel(panel);
        analysisService.save(analysis);

        assertNotNull("Analysis reference required for creating result", analysis);

        MockHttpServletRequest request = buildFhirRequest("POST", "/Observation");

        String createJson = """
                {
                  "resourceType": "Observation",
                  "status": "final",
                  "code": {
                    "coding": [{
                      "system": "http://loinc.org",
                      "code": "999999",
                      "display": "Invalid LOINC"
                    }]
                  },
                  "subject": {
                    "reference": "Patient/%s"
                  },
                  "specimen": {
                    "reference": "Specimen/%s"
                  },
                  "basedOn": [{
                    "reference": "ServiceRequest/%s"
                  }],
                  "effectiveDateTime": "2026-03-09T10:00:00+03:00",
                  "valueQuantity": {
                    "value": 85.5,
                    "unit": "g/L"
                  }
                }
                """.formatted(patientFhirUuid, specimenFhirUuid, analysisFhirUuid);

        request.setContent(createJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(422, response.getStatus());
    }

    @Test
    public void createObservation_withoutValueQuantity_shouldReturn422() throws Exception {

        String patientFhirUuid = "550e8400-e29b-41d4-a716-446655440001";
        String specimenFhirUuid = "68438220-5cef-44c4-9e6f-9f88e6b93270";
        String analysisFhirUuid = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";

        Analysis analysis = analysisService.getAnalysisById("1");

        Localization localizationOld = new Localization();
        localizationOld.setDescription("Test Panel");
        localizationOld.setLastupdated(new Timestamp(System.currentTimeMillis()));

        Localization savedLocalization = localizationSevice.save(localizationOld);

        Panel newPanel = new Panel();
        newPanel.setPanelName("New Panel Name");
        newPanel.setDescription("A test panel from dataset.");
        newPanel.setLocalization(savedLocalization);

        Panel panel = panelService.save(newPanel);

        analysis.setPanel(panel);
        analysisService.save(analysis);

        assertNotNull("Analysis reference required for creating result", analysis);

        MockHttpServletRequest request = buildFhirRequest("POST", "/Observation");

        String createJson = """
                {
                  "resourceType": "Observation",
                  "status": "final",
                  "code": {
                    "coding": [{
                      "system": "http://loinc.org",
                      "code": "123456",
                      "display": "Complete Blood Count"
                    }]
                  },
                  "subject": {
                    "reference": "Patient/%s"
                  },
                  "specimen": {
                    "reference": "Specimen/%s"
                  },
                  "basedOn": [{
                    "reference": "ServiceRequest/%s"
                  }],
                  "effectiveDateTime": "2026-03-09T10:00:00+03:00"
                }
                """.formatted(patientFhirUuid, specimenFhirUuid, analysisFhirUuid);

        request.setContent(createJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(422, response.getStatus());
    }

    @Test
    public void createObservation_withoutStatus_shouldReturn422() throws Exception {

        String patientFhirUuid = "550e8400-e29b-41d4-a716-446655440001";
        String specimenFhirUuid = "68438220-5cef-44c4-9e6f-9f88e6b93270";
        String analysisFhirUuid = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";

        Analysis analysis = analysisService.getAnalysisById("1");

        Localization localizationOld = new Localization();
        localizationOld.setDescription("Test Panel");
        localizationOld.setLastupdated(new Timestamp(System.currentTimeMillis()));

        Localization savedLocalization = localizationSevice.save(localizationOld);

        Panel newPanel = new Panel();
        newPanel.setPanelName("New Panel Name");
        newPanel.setDescription("A test panel from dataset.");
        newPanel.setLocalization(savedLocalization);

        Panel panel = panelService.save(newPanel);

        analysis.setPanel(panel);
        analysisService.save(analysis);

        assertNotNull("Analysis reference required for creating result", analysis);

        MockHttpServletRequest request = buildFhirRequest("POST", "/Observation");

        String createJson = """
                {
                  "resourceType": "Observation",
                  "code": {
                    "coding": [{
                      "system": "http://loinc.org",
                      "code": "123456",
                      "display": "Complete Blood Count"
                    }]
                  },
                  "subject": {
                    "reference": "Patient/%s"
                  },
                  "specimen": {
                    "reference": "Specimen/%s"
                  },
                  "basedOn": [{
                    "reference": "ServiceRequest/%s"
                  }],
                  "effectiveDateTime": "2026-03-09T10:00:00+03:00",
                  "valueQuantity": {
                    "value": 85.5,
                    "unit": "g/L"
                  }
                }
                """.formatted(patientFhirUuid, specimenFhirUuid, analysisFhirUuid);

        request.setContent(createJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        int statusCode = response.getStatus();
        assertTrue(statusCode == 400 || statusCode == 422);
    }

    @Test
    public void readObservation_withNonExistentId_shouldReturn404() throws Exception {

        String nonExistentUuid = "00000000-0000-0000-0000-000000000000";

        MockHttpServletRequest request = buildFhirRequest("GET", "/Observation/" + nonExistentUuid);

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("OperationOutcome", jsonResponse.get("resourceType").asText());
    }

    @Test
    public void updateObservation_shouldUpdateValue() throws Exception {

        String observationFhirUuid = "550e8400-e29b-41d4-a716-446655440003";
        String patientFhirUuid = "550e8400-e29b-41d4-a716-446655440001";
        String analysisFhirUuid = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";
        String specimenFhirUuid = "68438220-5cef-44c4-9e6f-9f88e6b93270";

        Result result = resultService.getResultByFhirUuid(observationFhirUuid);
        assertNotNull("Result not found in test data", result);
        Analysis analysis = analysisService.getAnalysisById("1");

        Localization localizationOld = new Localization();
        localizationOld.setDescription("Test Panel");
        localizationOld.setLastupdated(new Timestamp(System.currentTimeMillis()));
        Localization savedLocalization = localizationSevice.save(localizationOld);
        Panel newPanel = new Panel();
        newPanel.setPanelName("New Panel Name");
        newPanel.setDescription("A test panel from dataset.");
        newPanel.setLocalization(savedLocalization);
        Panel panel = panelService.save(newPanel);
        analysis.setPanel(panel);
        analysisService.save(analysis);

        MockHttpServletRequest request = buildFhirRequest("PUT", "/Observation/" + observationFhirUuid);

        String updateJson = """
                {
                  "resourceType": "Observation",
                  "id": "%s",
                  "status": "final",
                  "code": {
                    "coding": [{
                      "system": "http://loinc.org",
                      "code": "123456",
                      "display": "Complete Blood Count"
                    }]
                  },
                  "subject": {
                    "reference": "Patient/%s"
                  },
                  "specimen": {
                    "reference": "Specimen/%s"
                  },
                  "basedOn": [{
                    "reference": "ServiceRequest/%s"
                  }],
                  "effectiveDateTime": "2026-03-05T00:00:00+03:00",
                  "valueQuantity": {
                    "value": 99.0,
                    "unit": "g/L"
                  }
                }
                """.formatted(observationFhirUuid, patientFhirUuid, specimenFhirUuid, analysisFhirUuid);

        request.setContent(updateJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        Result updatedResult = resultService.getResultByFhirUuid(observationFhirUuid);

        assertEquals("99.0", updatedResult.getValue());
    }

    @Test
    public void deleteObservation_shouldReturn204() throws Exception {

        String fhirUuid = "550e8400-e29b-41d4-a716-446655440003";

        Result result = resultService.getResultByFhirUuid(fhirUuid);
        assertNotNull("Result not found in test data", result);

        assertNotNull("Result not found in test data", result);
        Analysis analysis = analysisService.getAnalysisById("1");

        Localization localizationOld = new Localization();
        localizationOld.setDescription("Test Panel");
        localizationOld.setLastupdated(new Timestamp(System.currentTimeMillis()));
        Localization savedLocalization = localizationSevice.save(localizationOld);
        Panel newPanel = new Panel();
        newPanel.setPanelName("New Panel Name");
        newPanel.setDescription("A test panel from dataset.");
        newPanel.setLocalization(savedLocalization);
        Panel panel = panelService.save(newPanel);
        analysis.setPanel(panel);
        analysisService.save(analysis);

        MockHttpServletRequest request = buildFhirRequest("DELETE", "/Observation/" + fhirUuid);

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(204, response.getStatus());

        Result deletedResult = resultService.getResultByFhirUuid(fhirUuid);

        assertNotNull(deletedResult);
    }

    @Test
    public void searchObservation_endpointExists_shouldNotReturn404() throws Exception {

        String patientUuid = "550e8400-e29b-41d4-a716-446655440001";

        MockHttpServletRequest request = buildFhirRequest("GET", "/Observation");

        request.setQueryString("patient=" + patientUuid);
        request.addParameter("patient", patientUuid);

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertNotNull(response);
        assertTrue(response.getStatus() != 404);
    }

    @Test
    public void createObservation_shouldCreateNewResult() throws Exception {
        String patientFhirUuid = "550e8400-e29b-41d4-a716-446655440001";
        String analysisFhirUuid = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";
        String specimenFhirUuid = "68438220-5cef-44c4-9e6f-9f88e6b93270";

        Analysis analysis = analysisService.getAnalysisById("1");

        Localization localizationOld = new Localization();
        localizationOld.setDescription("Test Panel");
        localizationOld.setLastupdated(new Timestamp(System.currentTimeMillis()));
        Localization savedLocalization = localizationSevice.save(localizationOld);
        Panel newPanel = new Panel();
        newPanel.setPanelName("New Panel Name");
        newPanel.setDescription("A test panel from dataset.");
        newPanel.setLocalization(savedLocalization);

        Panel panel = panelService.save(newPanel);
        analysis.setPanel(panel);
        analysisService.save(analysis);
        assertNotNull("Analysis reference required for creating result", analysis);

        MockHttpServletRequest request = buildFhirRequest("POST", "/Observation");

        String createJson = """
                {
                  "resourceType": "Observation",
                  "status": "final",
                  "code": {
                    "coding": [{
                      "system": "http://loinc.org",
                      "code": "123456",
                      "display": "Complete Blood Count"
                    }]
                  },
                  "subject": {
                    "reference": "Patient/%s"
                  },
                  "specimen": {
                    "reference": "Specimen/%s"
                  },
                  "basedOn": [{
                    "reference": "ServiceRequest/%s"
                  }],
                  "effectiveDateTime": "2026-03-09T10:00:00+03:00",
                  "valueQuantity": {
                    "value": 85.5,
                    "unit": "g/L"
                  }
                }
                """.formatted(patientFhirUuid, specimenFhirUuid, analysisFhirUuid);

        request.setContent(createJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(201, response.getStatus());

        String location = response.getHeader("Location");
        assertNotNull("Location header should contain the new Resource URL", location);
        String newUuid = location.substring(location.lastIndexOf("/") + 1);

        Result createdResult = resultService.getResultByFhirUuid(newUuid);
        assertNotNull("Result should be persisted in the database", createdResult);
        assertEquals("85.5", createdResult.getValue());
    }

    @Test
    public void createObservation_withoutServiceRequestShouldThrow422() throws Exception {
        String patientFhirUuid = "550e8400-e29b-41d4-a716-446655440001";
        String specimenFhirUuid = "68438220-5cef-44c4-9e6f-9f88e6b93270";

        Analysis analysis = analysisService.getAnalysisById("1");

        Localization localizationOld = new Localization();
        localizationOld.setDescription("Test Panel");
        localizationOld.setLastupdated(new Timestamp(System.currentTimeMillis()));
        Localization savedLocalization = localizationSevice.save(localizationOld);
        Panel newPanel = new Panel();
        newPanel.setPanelName("New Panel Name");
        newPanel.setDescription("A test panel from dataset.");
        newPanel.setLocalization(savedLocalization);

        Panel panel = panelService.save(newPanel);
        analysis.setPanel(panel);
        analysisService.save(analysis);
        assertNotNull("Analysis reference required for creating result", analysis);

        MockHttpServletRequest request = buildFhirRequest("POST", "/Observation");

        String createJson = """
                {
                  "resourceType": "Observation",
                  "status": "final",
                  "code": {
                    "coding": [{
                      "system": "http://loinc.org",
                      "code": "123456",
                      "display": "Complete Blood Count"
                    }]
                  },
                  "subject": {
                    "reference": "Patient/%s"
                  },
                  "specimen": {
                    "reference": "Specimen/%s"
                  },
                  "effectiveDateTime": "2026-03-09T10:00:00+03:00",
                  "valueQuantity": {
                    "value": 85.5,
                    "unit": "g/L"
                  }
                }
                """.formatted(patientFhirUuid, specimenFhirUuid);

        request.setContent(createJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(422, response.getStatus());

    }

    @Test
    public void createObservation_withoutSpecimenShouldThrow422() throws Exception {
        String patientFhirUuid = "550e8400-e29b-41d4-a716-446655440001";
        String analysisFhirUuid = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";

        Analysis analysis = analysisService.getAnalysisById("1");

        Localization localizationOld = new Localization();
        localizationOld.setDescription("Test Panel");
        localizationOld.setLastupdated(new Timestamp(System.currentTimeMillis()));
        Localization savedLocalization = localizationSevice.save(localizationOld);
        Panel newPanel = new Panel();
        newPanel.setPanelName("New Panel Name");
        newPanel.setDescription("A test panel from dataset.");
        newPanel.setLocalization(savedLocalization);

        Panel panel = panelService.save(newPanel);
        analysis.setPanel(panel);
        analysisService.save(analysis);
        assertNotNull("Analysis reference required for creating result", analysis);

        MockHttpServletRequest request = buildFhirRequest("POST", "/Observation");

        String createJson = """
                {
                  "resourceType": "Observation",
                  "status": "final",
                  "code": {
                    "coding": [{
                      "system": "http://loinc.org",
                      "code": "123456",
                      "display": "Complete Blood Count"
                    }]
                  },
                  "subject": {
                    "reference": "Patient/%s"
                  },
                  "basedOn": [{
                    "reference": "ServiceRequest/%s"
                  }],
                  "effectiveDateTime": "2026-03-09T10:00:00+03:00",
                  "valueQuantity": {
                    "value": 85.5,
                    "unit": "g/L"
                  }
                }
                """.formatted(patientFhirUuid, analysisFhirUuid);

        request.setContent(createJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(422, response.getStatus());

    }

    @Test
    public void createObservation_withoutBasedOnShouldThrow422() throws Exception {
        String analysisFhirUuid = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";
        String specimenFhirUuid = "68438220-5cef-44c4-9e6f-9f88e6b93270";

        Analysis analysis = analysisService.getAnalysisById("1");

        Localization localizationOld = new Localization();
        localizationOld.setDescription("Test Panel");
        localizationOld.setLastupdated(new Timestamp(System.currentTimeMillis()));
        Localization savedLocalization = localizationSevice.save(localizationOld);
        Panel newPanel = new Panel();
        newPanel.setPanelName("New Panel Name");
        newPanel.setDescription("A test panel from dataset.");
        newPanel.setLocalization(savedLocalization);

        Panel panel = panelService.save(newPanel);
        analysis.setPanel(panel);
        analysisService.save(analysis);
        assertNotNull("Analysis reference required for creating result", analysis);

        MockHttpServletRequest request = buildFhirRequest("POST", "/Observation");

        String createJson = """
                {
                  "resourceType": "Observation",
                  "status": "final",
                  "code": {
                    "coding": [{
                      "system": "http://loinc.org",
                      "code": "123456",
                      "display": "Complete Blood Count"
                    }]
                  },
                  "specimen": {
                    "reference": "Specimen/%s"
                  },
                  "basedOn": [{
                    "reference": "ServiceRequest/%s"
                  }],
                  "effectiveDateTime": "2026-03-09T10:00:00+03:00",
                  "valueQuantity": {
                    "value": 85.5,
                    "unit": "g/L"
                  }
                }
                """.formatted(specimenFhirUuid, analysisFhirUuid);

        request.setContent(createJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(422, response.getStatus());

    }

    @Test
    public void updateObservation_withoutServiceRequest_shouldReturn422() throws Exception {
        String observationFhirUuid = "550e8400-e29b-41d4-a716-446655440003";
        String patientFhirUuid = "550e8400-e29b-41d4-a716-446655440001";
        String specimenFhirUuid = "68438220-5cef-44c4-9e6f-9f88e6b93270";

        Result result = resultService.getResultByFhirUuid(observationFhirUuid);
        assertNotNull("Result not found in test data", result);

        MockHttpServletRequest request = buildFhirRequest("PUT", "/Observation/" + observationFhirUuid);

        String updateJson = """
                {
                  "resourceType": "Observation",
                  "id": "%s",
                  "status": "final",
                  "code": {
                    "coding": [{
                      "system": "http://loinc.org",
                      "code": "123456",
                      "display": "Complete Blood Count"
                    }]
                  },
                  "subject": {
                    "reference": "Patient/%s"
                  },
                  "specimen": {
                    "reference": "Specimen/%s"
                  },
                  "effectiveDateTime": "2026-03-05T00:00:00+03:00",
                  "valueQuantity": {
                    "value": 99.0,
                    "unit": "g/L"
                  }
                }
                """.formatted(observationFhirUuid, patientFhirUuid, specimenFhirUuid);

        request.setContent(updateJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(422, response.getStatus());
    }

    @Test
    public void updateObservation_withoutSubject_shouldReturn422() throws Exception {
        String observationFhirUuid = "550e8400-e29b-41d4-a716-446655440003";
        String specimenFhirUuid = "68438220-5cef-44c4-9e6f-9f88e6b93270";
        String analysisFhirUuid = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";

        Analysis analysis = analysisService.getAnalysisById("1");

        Localization localizationOld = new Localization();
        localizationOld.setDescription("Test Panel");
        localizationOld.setLastupdated(new Timestamp(System.currentTimeMillis()));
        Localization savedLocalization = localizationSevice.save(localizationOld);
        Panel newPanel = new Panel();
        newPanel.setPanelName("New Panel Name");
        newPanel.setDescription("A test panel from dataset.");
        newPanel.setLocalization(savedLocalization);

        Panel panel = panelService.save(newPanel);
        analysis.setPanel(panel);
        analysisService.save(analysis);
        assertNotNull("Analysis reference required for creating result", analysis);

        Result result = resultService.getResultByFhirUuid(observationFhirUuid);
        assertNotNull("Result not found in test data", result);

        MockHttpServletRequest request = buildFhirRequest("PUT", "/Observation/" + observationFhirUuid);

        String updateJson = """
                {
                  "resourceType": "Observation",
                  "id": "%s",
                  "status": "final",
                  "code": {
                    "coding": [{
                      "system": "http://loinc.org",
                      "code": "123456",
                      "display": "Complete Blood Count"
                    }]
                  },
                  "specimen": {
                    "reference": "Specimen/%s"
                  },
                  "basedOn": [{
                    "reference": "ServiceRequest/%s"
                  }],
                  "effectiveDateTime": "2026-03-05T00:00:00+03:00",
                  "valueQuantity": {
                    "value": 99.0,
                    "unit": "g/L"
                  }
                }
                """.formatted(observationFhirUuid, specimenFhirUuid, analysisFhirUuid);

        request.setContent(updateJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(422, response.getStatus());
    }

    @Test
    public void updateObservation_withoutSpecimen_shouldReturn422() throws Exception {
        String observationFhirUuid = "550e8400-e29b-41d4-a716-446655440003";
        String patientFhirUuid = "550e8400-e29b-41d4-a716-446655440001";
        String analysisFhirUuid = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";

        Result result = resultService.getResultByFhirUuid(observationFhirUuid);
        assertNotNull("Result not found in test data", result);

        MockHttpServletRequest request = buildFhirRequest("PUT", "/Observation/" + observationFhirUuid);

        String updateJson = """
                {
                  "resourceType": "Observation",
                  "id": "%s",
                  "status": "final",
                  "code": {
                    "coding": [{
                      "system": "http://loinc.org",
                      "code": "123456",
                      "display": "Complete Blood Count"
                    }]
                  },
                  "subject": {
                    "reference": "Patient/%s"
                  },
                  "basedOn": [{
                    "reference": "ServiceRequest/%s"
                  }],
                  "effectiveDateTime": "2026-03-05T00:00:00+03:00",
                  "valueQuantity": {
                    "value": 99.0,
                    "unit": "g/L"
                  }
                }
                """.formatted(observationFhirUuid, patientFhirUuid, analysisFhirUuid);

        request.setContent(updateJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(422, response.getStatus());
    }

    @Test
    public void updateObservation_withoutLoincCode_shouldReturn422() throws Exception {
        String observationFhirUuid = "550e8400-e29b-41d4-a716-446655440003";
        String patientFhirUuid = "550e8400-e29b-41d4-a716-446655440001";
        String specimenFhirUuid = "68438220-5cef-44c4-9e6f-9f88e6b93270";
        String analysisFhirUuid = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";

        Result result = resultService.getResultByFhirUuid(observationFhirUuid);
        assertNotNull("Result not found in test data", result);

        MockHttpServletRequest request = buildFhirRequest("PUT", "/Observation/" + observationFhirUuid);

        String updateJson = """
                {
                  "resourceType": "Observation",
                  "id": "%s",
                  "status": "final",
                  "code": {
                    "coding": [{
                      "system": "http://loinc.org"
                    }]
                  },
                  "subject": {
                    "reference": "Patient/%s"
                  },
                  "specimen": {
                    "reference": "Specimen/%s"
                  },
                  "basedOn": [{
                    "reference": "ServiceRequest/%s"
                  }],
                  "effectiveDateTime": "2026-03-05T00:00:00+03:00",
                  "valueQuantity": {
                    "value": 99.0,
                    "unit": "g/L"
                  }
                }
                """.formatted(observationFhirUuid, patientFhirUuid, specimenFhirUuid, analysisFhirUuid);

        request.setContent(updateJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(422, response.getStatus());
    }

    @Test
    public void updateObservation_withoutValue_shouldReturn422() throws Exception {
        String observationFhirUuid = "550e8400-e29b-41d4-a716-446655440003";
        String patientFhirUuid = "550e8400-e29b-41d4-a716-446655440001";
        String specimenFhirUuid = "68438220-5cef-44c4-9e6f-9f88e6b93270";
        String analysisFhirUuid = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";

        Result result = resultService.getResultByFhirUuid(observationFhirUuid);
        assertNotNull("Result not found in test data", result);

        MockHttpServletRequest request = buildFhirRequest("PUT", "/Observation/" + observationFhirUuid);

        String updateJson = """
                {
                  "resourceType": "Observation",
                  "id": "%s",
                  "status": "final",
                  "code": {
                    "coding": [{
                      "system": "http://loinc.org",
                      "code": "123456",
                      "display": "Complete Blood Count"
                    }]
                  },
                  "subject": {
                    "reference": "Patient/%s"
                  },
                  "specimen": {
                    "reference": "Specimen/%s"
                  },
                  "basedOn": [{
                    "reference": "ServiceRequest/%s"
                  }],
                  "effectiveDateTime": "2026-03-05T00:00:00+03:00"
                }
                """.formatted(observationFhirUuid, patientFhirUuid, specimenFhirUuid, analysisFhirUuid);

        request.setContent(updateJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(422, response.getStatus());
    }

}