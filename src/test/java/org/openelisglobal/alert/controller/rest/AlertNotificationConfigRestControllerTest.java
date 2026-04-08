package org.openelisglobal.alert.controller.rest;

import static org.mockito.Mockito.doThrow;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.alert.service.AlertNotificationConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class AlertNotificationConfigRestControllerTest {

    @InjectMocks
    private AlertNotificationConfigRestController controller;

    @Mock
    private AlertNotificationConfigService alertNotificationConfigService;

    @Test
    public void saveAlertNotificationConfig_returnsBadRequestForInvalidEscalationDelayMinutes() {
        Map<String, Object> config = new HashMap<>();
        config.put("escalationDelayMinutes", "abc");

        doThrow(new IllegalArgumentException("Invalid escalationDelayMinutes: must be an integer"))
                .when(alertNotificationConfigService).saveAlertNotificationConfig(config);

        ResponseEntity<Map<String, String>> response = controller.saveAlertNotificationConfig(config);

        Assert.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assert.assertNotNull(response.getBody());
        Assert.assertEquals("Invalid escalationDelayMinutes: must be an integer", response.getBody().get("error"));
    }
}
