package org.openelisglobal.fhir;

import static org.junit.Assert.*;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.fhir.providers.PatientProvider;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.*;
import org.springframework.test.annotation.Rollback;

@Rollback
public class PatientFacadeTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PatientService patientService;
    @Autowired
    private PatientProvider patientProvider;
    @Autowired
    private MockServletContext servletContext;

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        // Reset singleton caches to avoid stale identity type IDs from previous tests
        org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap.reset();

        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(patientProvider));

        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);

        objectMapper = new ObjectMapper();
        executeDataSetWithStateManagement("testdata/facade-patient.xml");
        ensureReferenceTables("PATIENT", "PERSON", "PATIENT_IDENTITY");
        executeDataSetWithStateManagement("testdata/system-user.xml");
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

        UserSessionData data = new UserSessionData();
        data.setSytemUserId(1);
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, data);

        return request;
    }

    @Test
    public void readPatient_shouldReturnPatientResource() throws Exception {
        Patient existingPatient = patientService.get("1");
        assertNotNull(existingPatient);

        String patientUuid = existingPatient.getFhirUuidAsString();
        MockHttpServletRequest request = buildRequest("GET", "/Patient/" + patientUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("Patient", json.get("resourceType").asText());
        assertEquals(patientUuid, json.get("id").asText());
        assertEquals("male", json.get("gender").asText());
    }

    @Test
    public void readPatient_sendsOnlyMAsMaleAndFAsFemale() throws Exception {
        String patientUuid = patientService.get("2").getFhirUuidAsString();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String[][] cases = { { "F", "female" }, { "M", "male" }, { "O", "other" }, { "X", "unknown" },
                { "", "unknown" } };
        for (String[] sexAndGender : cases) {
            jdbc.update("UPDATE clinlims.patient SET gender = ? WHERE id = 2", sexAndGender[0]);

            assertEquals("stored sex '" + sexAndGender[0] + "'", sexAndGender[1],
                    read(patientUuid).get("gender").asText());
        }
    }

    private JsonNode read(String patientUuid) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(buildRequest("GET", "/Patient/" + patientUuid), response);
        assertEquals(200, response.getStatus());
        return objectMapper.readTree(response.getContentAsString());
    }

}
