package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import java.sql.Date;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.controller.rest.MicroBreakpointAdminRestController;
import org.openelisglobal.microbiology.controller.rest.MicrobiologyReferenceAdminRestController;
import org.openelisglobal.microbiology.form.MicroOrganismAdminForm;
import org.openelisglobal.microbiology.service.MicroBreakpointAdminService;
import org.openelisglobal.microbiology.service.MicroBreakpointImportService;
import org.openelisglobal.microbiology.service.MicrobiologyReferenceAdminService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;

public class MicrobiologyReferenceAdminRestControllerTest {

    @Test
    public void referenceWriteUsesAuthenticatedActor() {
        MicrobiologyReferenceAdminService service = org.mockito.Mockito.mock(MicrobiologyReferenceAdminService.class);
        MicrobiologyReferenceAdminRestController controller = new MicrobiologyReferenceAdminRestController(service);
        MicroOrganismAdminForm request = new MicroOrganismAdminForm();
        request.displayName = "Synthetic organism";
        request.whonetCode = "syn";

        controller.createOrganism(requestFor("42"), request);

        verify(service).saveOrganism(null, request, "42");
    }

    @Test
    public void breakpointActivationUsesAuthenticatedActor() {
        MicroBreakpointAdminService service = org.mockito.Mockito.mock(MicroBreakpointAdminService.class);
        MicroBreakpointImportService importService = org.mockito.Mockito.mock(MicroBreakpointImportService.class);
        MicroBreakpointAdminRestController controller = new MicroBreakpointAdminRestController(service, importService);

        controller.activate("standard-1", Date.valueOf("2026-08-04"), requestFor("77"));

        verify(service).activate("standard-1", Date.valueOf("2026-08-04"), "77");
    }

    @Test
    public void adminControllersRequireAdminRole() {
        PreAuthorize reference = MicrobiologyReferenceAdminRestController.class.getAnnotation(PreAuthorize.class);
        PreAuthorize breakpoint = MicroBreakpointAdminRestController.class.getAnnotation(PreAuthorize.class);

        assertEquals("hasRole('ADMIN')", reference.value());
        assertEquals("hasRole('ADMIN')", breakpoint.value());
    }

    private MockHttpServletRequest requestFor(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(Integer.parseInt(userId));
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return request;
    }
}
