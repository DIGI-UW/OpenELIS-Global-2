package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.common.provider.validation.IAccessionNumberValidator;
import org.openelisglobal.common.provider.validation.IAccessionNumberValidator.ValidationResults;
import org.openelisglobal.fhir.providers.ServiceRequestProvider;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

public class ServiceRequestFacadeTest extends BaseWebContextSensitiveTest {

    private static final String ANALYSIS1_FHIRID = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";

    @Autowired
    private MockServletContext servletContext;

    @Autowired
    private ServiceRequestProvider serviceRequestProvider;

    @Autowired
    private PanelService panelService;

    @Autowired
    private LocalizationService localizationSevice;

    @Autowired
    private AnalysisService analysisService;

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {

        AccessionNumberValidatorFactory mockFactory = Mockito.mock(AccessionNumberValidatorFactory.class);
        IAccessionNumberValidator mockValidator = Mockito.mock(IAccessionNumberValidator.class);
        IAccessionNumberGenerator mockGenerator = Mockito.mock(IAccessionNumberGenerator.class);

        Mockito.when(mockValidator.validFormat(Mockito.anyString(), Mockito.anyBoolean()))
                .thenReturn(ValidationResults.SUCCESS);

        Mockito.when(mockValidator.getMaxAccessionLength()).thenReturn(10);
        Mockito.when(mockValidator.getMinAccessionLength()).thenReturn(5);

        Mockito.when(mockGenerator.getChangeableLength()).thenReturn(5);
        Mockito.when(mockGenerator.getInvarientLength()).thenReturn(5);

        Mockito.when(mockFactory.getValidator(Mockito.any())).thenReturn(mockValidator);
        Mockito.when(mockFactory.getGenerator(Mockito.any())).thenReturn(mockGenerator);

        Field field = AccessionNumberUtil.class.getDeclaredField("accessionNumberValidatorFactory");
        field.setAccessible(true);
        field.set(null, mockFactory);

        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(serviceRequestProvider));

        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);

        objectMapper = new ObjectMapper();
        executeDataSetWithStateManagement("testdata/facade-servicerequest.xml");
        ensureReferenceTables("PATIENT", "PERSON", "PATIENT_IDENTITY", "sample_human");
    }

    @Test
    public void read_shouldReturnServiceRequestByFhirUUID() throws ServletException, IOException {
        MockHttpServletRequest request = buildFhirRequest("GET", "/ServiceRequest/" + ANALYSIS1_FHIRID);
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("ServiceRequest", jsonResponse.get("resourceType").asText());

    }

    @Test
    public void readPractitioner_withNonExistentId_shouldReturn404() throws Exception {

        String nonExistentUuid = "00000000-0000-0000-0000-000000000000";
        MockHttpServletRequest request = buildFhirRequest("GET", "/ServiceRequest/" + nonExistentUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", jsonResponse.get("resourceType").asText());
    }

    @Test
    public void readServiceRequest_withInvalidUuid_shouldReturn400() throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/ServiceRequest/not-a-uuid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(400, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", jsonResponse.get("resourceType").asText());
    }

    @Test
    public void searchServiceRequest_endpointExists_shouldNotReturn404() throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/ServiceRequest");
        request.setQueryString("subject=Patient/550e8400-e29b-41d4-a716-446655440001");
        request.addParameter("subject", "Patient/550e8400-e29b-41d4-a716-446655440001");

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertNotNull(response);
        org.junit.Assert.assertTrue(response.getStatus() != 404);
    }

    @Test
    public void createServiceRequest_shouldReturnCreatedServiceRequest() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "analysis", });
        String subjectUUID = "b479ab79-5f53-4d1f-bc9b-10f19ce04635";
        String specimenUUID = "68438220-5cef-44c4-9e6f-9f88e6b93270";
        String practitionerUUID = "550e8400-e29b-41d4-a716-446655441004";
        String locationUUID = "68438220-5cef-44c4-9e6f-9f88e6b93287";
        String loinc = "123456";
        String loincDisplay = "Blood Test";
        MockHttpServletRequest request = buildFhirRequest("POST", "/ServiceRequest");
        String jsonBody = """
                {
                "resourceType": "ServiceRequest",
                "status": "active",
                "intent": "order",
                "priority": "routine",

                "code": {
                    "coding": [
                    {
                        "system": "http://loinc.org",
                        "code": "%s",
                        "display": "%s"
                    }
                    ]
                },

                "subject": {
                    "reference": "Patient/%s"
                },

                "authoredOn": "2023-02-03T00:00:00Z",

                "requester": {
                    "reference": "Practitioner/%s"
                },

                "locationReference": [
                    {
                    "reference": "Location/%s"
                    }
                ],

                "specimen": [
                    {
                    "reference": "Specimen/%s"
                    }
                ],

                "note": [
                    {
                    "text": "Generated for OpenELIS Package 0.1.0"
                    }
                ]
                }
                """.formatted(loinc, loincDisplay, subjectUUID, practitionerUUID, locationUUID, specimenUUID);
        request.setContentType("application/json");
        request.setContent(jsonBody.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(201, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("ServiceRequest", jsonResponse.get("resourceType").asText());
    }

    @Test
    public void createServiceRequest_shouldReturnCreatedServiceRequestGivenTestName() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "analysis", });
        String subjectUUID = "b479ab79-5f53-4d1f-bc9b-10f19ce04635";
        String specimenUUID = "68438220-5cef-44c4-9e6f-9f88e6b93270";
        String practitionerUUID = "550e8400-e29b-41d4-a716-446655441004";
        String locationUUID = "68438220-5cef-44c4-9e6f-9f88e6b93287";
        String loincDisplay = "Complete Blood Count";
        MockHttpServletRequest request = buildFhirRequest("POST", "/ServiceRequest");
        String jsonBody = """
                {
                "resourceType": "ServiceRequest",
                "status": "active",
                "intent": "order",
                "priority": "routine",

                "code": {
                    "coding": [
                    {
                        "system": "http://loinc.org",
                        "display": "%s"
                    }
                    ]
                },

                "subject": {
                    "reference": "Patient/%s"
                },

                "authoredOn": "2023-02-03T00:00:00Z",

                "requester": {
                    "reference": "Practitioner/%s"
                },

                "locationReference": [
                    {
                    "reference": "Location/%s"
                    }
                ],

                "specimen": [
                    {
                    "reference": "Specimen/%s"
                    }
                ],

                "note": [
                    {
                    "text": "Generated for OpenELIS Package 0.1.0"
                    }
                ]
                }
                """.formatted(loincDisplay, subjectUUID, practitionerUUID, locationUUID, specimenUUID);
        request.setContentType("application/json");
        request.setContent(jsonBody.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(201, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("ServiceRequest", jsonResponse.get("resourceType").asText());
    }

    @Test
    public void updateServiceRequest_shouldReturnUpdatedServiceRequest() throws Exception {
        String analysisUUID = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";
        String subjectUUID = "b479ab79-5f53-4d1f-bc9b-10f19ce04635";
        String specimenUUID = "68438220-5cef-44c4-9e6f-9f88e6b93270";
        String practitionerUUID = "550e8400-e29b-41d4-a716-446655441004";
        String loinc = "543216";
        String loincDisplay = "Blood Test";
        MockHttpServletRequest request = buildFhirRequest("PUT", "/ServiceRequest/" + analysisUUID);
        String jsonBody = """
                {
                "resourceType": "ServiceRequest",
                "id": "%s",
                "status": "active",
                "intent": "order",
                "priority": "routine",

                "code": {
                    "coding": [
                    {
                        "system": "http://loinc.org",
                        "code": "%s",
                        "display": "%s"
                    }
                    ]
                },

                "subject": {
                    "reference": "Patient/%s"
                },

                "authoredOn": "2023-02-03T00:00:00Z",

                "requester": {
                    "reference": "Practitioner/%s"
                },

                "specimen": [
                    {
                    "reference": "Specimen/%s"
                    }
                ],

                "note": [
                    {
                    "text": "Generated for OpenELIS Package 0.1.0"
                    }
                ]
                }
                """.formatted(analysisUUID, loinc, loincDisplay, subjectUUID, practitionerUUID, specimenUUID);
        request.setContentType("application/json");
        request.setContent(jsonBody.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("ServiceRequest", jsonResponse.get("resourceType").asText());
    }

    @Test
    public void delete_shouldDeleteServiceRequestByFhirUUID() throws ServletException, IOException {
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

        MockHttpServletRequest request = buildFhirRequest("DELETE", "/ServiceRequest/" + ANALYSIS1_FHIRID);
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(204, response.getStatus());
        Analysis analysis1 = analysisService.get("1");
        assertEquals("8", analysis1.getStatusId());

    }
}
