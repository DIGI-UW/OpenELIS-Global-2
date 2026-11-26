package org.openelisglobal.testmethod.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.testmethod.controller.rest.TestMethodRestController;
import org.openelisglobal.testmethod.controller.rest.TestMethodRestController.InlineCreateRequest;
import org.openelisglobal.testmethod.controller.rest.TestMethodRestController.LinkMethodRequest;
import org.openelisglobal.testmethod.controller.rest.TestMethodRestController.UpdateLinkRequest;
import org.openelisglobal.testmethod.service.TestMethodService;
import org.openelisglobal.testmethod.service.TestMethodService.InlineCreateData;
import org.openelisglobal.testmethod.valueholder.TestMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

/** An impossible effectiveDate is rejected, not saved as a later day. */
public class TestMethodRestControllerDateTest {

    private static final String TEST_ID = "101";

    private TestMethodRestController controller;
    private TestMethodService service;
    private MockHttpServletRequest request;

    @Before
    public void setUp() {
        controller = new TestMethodRestController();
        service = mock(TestMethodService.class);
        ReflectionTestUtils.setField(controller, "testMethodService", service);
        request = new MockHttpServletRequest();
        UserSessionData user = new UserSessionData();
        user.setSytemUserId(1);
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, user);
    }

    @Test
    public void linkRejectsImpossibleDate() {
        ResponseEntity<?> response = controller.linkMethod(TEST_ID, linkRequest("2026-02-30"), request);

        assertEquals(422, response.getStatusCode().value());
        verify(service, never()).linkMethodDto(any(TestMethod.class));
    }

    @Test
    public void linkKeepsLeapDay() {
        ResponseEntity<?> response = controller.linkMethod(TEST_ID, linkRequest("2028-02-29"), request);

        assertEquals(201, response.getStatusCode().value());
        ArgumentCaptor<TestMethod> saved = ArgumentCaptor.forClass(TestMethod.class);
        verify(service).linkMethodDto(saved.capture());
        assertEquals(Date.valueOf("2028-02-29"), saved.getValue().getEffectiveDate());
    }

    @Test
    public void inlineCreateRejectsImpossibleDate() {
        InlineCreateRequest req = new InlineCreateRequest();
        req.nameEnglish = "Method";
        req.nameFrench = "Methode";
        req.code = "MTH1";
        req.effectiveDate = "2027-02-29";

        ResponseEntity<?> response = controller.inlineCreateAndLink(TEST_ID, req, request);

        assertEquals(422, response.getStatusCode().value());
        verify(service, never()).createAndLinkMethod(anyString(), any(InlineCreateData.class));
    }

    @Test
    public void updateRejectsImpossibleDate() {
        TestMethod existing = new TestMethod();
        existing.setId("7");
        existing.setTestId(TEST_ID);
        existing.setIsActive(IActionConstants.YES);
        existing.setEffectiveDate(Date.valueOf("2026-01-01"));
        when(service.findLinkById("7")).thenReturn(existing);
        UpdateLinkRequest req = new UpdateLinkRequest();
        req.effectiveDate = "2026-04-31";

        ResponseEntity<?> response = controller.updateLink(TEST_ID, "7", req, request);

        assertEquals(422, response.getStatusCode().value());
    }

    private static LinkMethodRequest linkRequest(String effectiveDate) {
        LinkMethodRequest req = new LinkMethodRequest();
        req.methodId = "8001";
        req.effectiveDate = effectiveDate;
        return req;
    }
}
