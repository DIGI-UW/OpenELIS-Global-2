package org.openelisglobal.alert.controller.rest;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.alert.service.AlertNotificationConfigService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class AlertNotificationConfigRestControllerTest {

    @InjectMocks
    private AlertNotificationConfigRestController controller;

    @Mock
    private AlertNotificationConfigService alertNotificationConfigService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @Test
    public void saveAlertNotificationConfig_returnsBadRequestForInvalidEscalationDelayMinutes() {
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(IActionConstants.USER_SESSION_DATA)).thenReturn(userSessionData);

        Map<String, Object> config = new HashMap<>();
        config.put("escalationDelayMinutes", "abc");

        doThrow(new IllegalArgumentException("some internal validation detail")).when(alertNotificationConfigService)
                .saveAlertNotificationConfig(config, "1");

        ResponseEntity<Map<String, String>> response = controller.saveAlertNotificationConfig(config, request);

        Assert.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals("Invalid escalationDelayMinutes: must be an integer", response.getBody().get("error"));
    }
}
