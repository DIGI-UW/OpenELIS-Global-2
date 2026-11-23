package org.openelisglobal.common.rest.provider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.rest.provider.CommonValidationsRestController.ResponseObject;
import org.openelisglobal.project.service.ProjectService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The controller is a singleton: each request must get its own answer, or one
 * request's status leaks into another's (a valid phone number reported as
 * invalid while a national ID check ran, a failed accession number reported as
 * a success).
 */
public class CommonValidationsRestControllerResponseTest extends BaseWebContextSensitiveTest {

    private CommonValidationsRestController controller;

    @Before
    public void createController() {
        controller = new CommonValidationsRestController();
        ProjectService projects = mock(ProjectService.class);
        when(projects.getAllProjects()).thenReturn(List.of());
        ReflectionTestUtils.setField(controller, "projectService", projects);
    }

    @Test
    public void eachRequestGetsItsOwnAnswer() {
        ResponseObject first = controller.getPhoneNumberValidation(phone(""));
        ResponseObject second = controller.getPhoneNumberValidation(phone(""));

        assertNotSame(first, second);
    }

    @Test
    public void aFailedAccessionNumberIsNotReportedWithAnEarlierRequestsSuccess() {
        assertTrue(controller.getPhoneNumberValidation(phone("")).isStatus());

        ResponseObject generated = controller.accessionNumberGenerator("NO-SUCH-PROGRAM", true, null);

        assertFalse(generated.getBody(), generated.isStatus());
    }

    private static MockHttpServletRequest phone(String number) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("fieldId", "phone");
        request.setParameter("value", number);
        return request;
    }
}
