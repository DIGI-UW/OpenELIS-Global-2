package org.openelisglobal.patient.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for PatientManagementRestController. Uses real services and
 * test database to simulate runtime environment. Follows OpenELIS integration
 * test pattern - no service mocking.
 */
@Rollback
public class PatientManagementRestControllerTest extends BaseWebContextSensitiveTest {

    private static final String PATIENT_WITH_PHOTO_ID = "1";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/patient.xml");
        executeDataSetWithStateManagement("testdata/patient_management_rest.xml");
    }

    @Test
    public void getPhoto_shouldReturnPhotoDataGivenPatientId() throws Exception {
        MvcResult result = mockMvc
                .perform(get("/rest/patient-photos/" + PATIENT_WITH_PHOTO_ID + "/true")
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        assertEquals("Response: " + result.getResponse().getContentAsString(),
                200, result.getResponse().getStatus());

        String json = result.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> response = mapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });

        assertTrue("Response should contain data key", response.containsKey("data"));
        Object data = response.get("data");
        assertNotNull("Photo data should not be null", data);
        assertTrue("Photo data should not be empty", ((String) data).length() > 0);
    }

    @Test
    public void getPhoto_shouldReturnPhotoDataForFullSize() throws Exception {
        MvcResult result = mockMvc
                .perform(get("/rest/patient-photos/" + PATIENT_WITH_PHOTO_ID + "/false")
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        assertEquals("Response: " + result.getResponse().getContentAsString(),
                200, result.getResponse().getStatus());

        String json = result.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> response = mapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });

        Object data = response.get("data");
        assertNotNull("Photo data should not be null", data);
        assertTrue("Photo should be data URI", ((String) data).startsWith("data:image"));
    }

}
