package org.openelisglobal.notification.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate.NotificationPayloadType;
import org.springframework.beans.factory.annotation.Autowired;

public class NotificationPayloadTemplateServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private NotificationPayloadTemplateService notificationPayloadTemplateService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/user-role.xml");
        executeDataSetWithStateManagement("testdata/notification-payload-template-test-data.xml");
    }

    @Test
    public void getAll_validCall_returnsNonNullList() {
        List<NotificationPayloadTemplate> templates = notificationPayloadTemplateService.getAll();
        assertNotNull(templates);
    }

    @Test
    public void getAll_validCall_returnsAllTemplates() {
        List<NotificationPayloadTemplate> templates = notificationPayloadTemplateService.getAll();
        assertNotNull(templates);
        assertTrue(templates.size() >= 1);
    }

    @Test
    public void get_validId_returnsTemplate() {
        NotificationPayloadTemplate template = notificationPayloadTemplateService.get(1);
        assertNotNull(template);
        assertEquals(Integer.valueOf(1), template.getId());
        assertNotNull(template.getMessageTemplate());
    }

    @Test
    public void getSystemDefaultPayloadTemplateForType_validType_returnsTemplate() {
        NotificationPayloadTemplate template = notificationPayloadTemplateService
                .getSystemDefaultPayloadTemplateForType(NotificationPayloadType.TEST_RESULT);
        assertNotNull(template);
        assertEquals(NotificationPayloadType.TEST_RESULT, template.getType());
    }
}
