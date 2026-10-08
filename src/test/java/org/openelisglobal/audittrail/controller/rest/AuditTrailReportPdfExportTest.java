package org.openelisglobal.audittrail.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.audittrail.action.workers.AuditTrailItem;
import org.openelisglobal.audittrail.action.workers.AuditTrailViewWorker;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.testsupport.PdfText;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class AuditTrailReportPdfExportTest {

    @Mock
    private AutowireCapableBeanFactory beanFactory;

    @Mock
    private AuditTrailViewWorker worker;

    @Mock
    private DefaultConfigurationProperties configurationProperties;

    private Object previousFactory;
    private Object previousMessageUtil;

    @Before
    public void setUp() {
        previousFactory = ReflectionTestUtils.getField(SpringContext.class, "factory");
        previousMessageUtil = ReflectionTestUtils.getField(MessageUtil.class, "instance");
        ReflectionTestUtils.setField(SpringContext.class, "factory", beanFactory);
        when(beanFactory.getBean(AuditTrailViewWorker.class)).thenReturn(worker);
        when(beanFactory.getBean(DefaultConfigurationProperties.class)).thenReturn(configurationProperties);

        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("auditTrail.export.title.orderAuditTrail", LocaleContextHolder.getLocale(),
                "Order Audit Trail");
        messages.addMessage("auditTrail.export.header.action", LocaleContextHolder.getLocale(), "Action");
        messages.addMessage("auditTrail.export.header.user", LocaleContextHolder.getLocale(), "User");
        messages.addMessage("auditTrail.export.header.oldValue", LocaleContextHolder.getLocale(), "Old");
        messages.addMessage("auditTrail.export.header.newValue", LocaleContextHolder.getLocale(), "New");
        MessageUtil.setMessageSource(messages);
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(SpringContext.class, "factory", previousFactory);
        ReflectionTestUtils.setField(MessageUtil.class, "instance", previousMessageUtil);
    }

    @Test
    public void exportPdf_printsTheTitleHeaderAndEveryAuditRow() throws Exception {
        when(worker.getAuditTrail()).thenReturn(List.of(item("2026-03-04 09:15:00", "Insert", "jdoe", "", "Pending"),
                item("2026-03-05 10:30:00", "Update", "asmith", "Pending", "Finalized")));
        MockHttpServletResponse response = new MockHttpServletResponse();

        new AuditTrailReportRestController().exportPdf("ACC-77", response);

        assertEquals("application/pdf", response.getContentType());
        String text = PdfText.of(response.getContentAsByteArray());
        assertTrue(text, text.contains("Order Audit Trail - ACC-77"));
        assertTrue(text, text.contains("Action User"));
        assertTrue(text, text.contains("Old New"));
        assertTrue(text, text.contains("2026-03-04 09:15:00 Insert jdoe"));
        assertTrue(text, text.contains("2026-03-05 10:30:00 Update asmith"));
        assertTrue(text, text.contains("Pending Finalized"));
    }

    private AuditTrailItem item(String timestamp, String action, String user, String oldValue, String newValue) {
        AuditTrailItem item = new AuditTrailItem();
        item.setTimeStamp(Timestamp.valueOf(timestamp));
        item.setAction(action);
        item.setUser(user);
        item.setOldValue(oldValue);
        item.setNewValue(newValue);
        return item;
    }
}
