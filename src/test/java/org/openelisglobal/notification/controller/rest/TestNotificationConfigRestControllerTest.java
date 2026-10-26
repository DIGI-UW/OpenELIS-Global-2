package org.openelisglobal.notification.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notification.service.NotificationPayloadTemplateServiceImpl.DisallowedFieldException;
import org.openelisglobal.notification.service.TestNotificationConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * Test Notification Configuration refuses a per-test template that names the
 * patient with the fields, the way the shared templates are refused, instead of
 * a server error the page cannot explain.
 */
public class TestNotificationConfigRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestNotificationConfigService testNotificationConfigService;

    @After
    public void resetMock() {
        Mockito.reset(testNotificationConfigService);
    }

    @Test
    public void aPerTestTemplateNamingThePatientIsRefusedWithTheFields() throws Exception {
        Mockito.doThrow(new DisallowedFieldException(List.of("patientFirstName", "patientLastNameInitial")))
                .when(testNotificationConfigService).saveStatusAndMessages(any(), anyString());
        UserSessionData admin = new UserSessionData();
        admin.setSytemUserId(1);

        // No testId: the form's setter loads that Test row, and a sibling fixture
        // may have replaced the catalogue; the service is mocked anyway.
        mockMvc.perform(post("/rest/TestNotificationConfig").contentType(MediaType.APPLICATION_JSON)
                .content("{\"config\":{\"patientEmail\":{\"notificationNature\":\"RESULT_VALIDATION\","
                        + "\"notificationPersonType\":\"PATIENT\",\"notificationMethod\":\"EMAIL\",\"active\":true,"
                        + "\"payloadTemplate\":{\"subjectTemplate\":\"Ready\",\"messageTemplate\":"
                        + "\"Dear [patientFirstName] [patientLastNameInitial]\"}}},"
                        + "\"editSystemDefaultPayloadTemplate\":false}")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, admin)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.disallowedFields[0]").value("patientFirstName"))
                .andExpect(jsonPath("$.disallowedFields[1]").value("patientLastNameInitial"));
    }
}
