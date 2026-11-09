package org.openelisglobal.common.rest.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.rest.provider.CommonValidationsRestController.ResponseObject;
import org.openelisglobal.common.services.PhoneNumberService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.search.service.SearchResultsService;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class CommonValidationsRestControllerTest {

    private CommonValidationsRestController controller;
    private ProjectService projectService;
    private SearchResultsService searchResultsService;
    private PhoneNumberService phoneNumberService;
    private MessageSource messageSource;

    @Before
    public void setUp() {
        controller = new CommonValidationsRestController();
        projectService = mock(ProjectService.class);
        searchResultsService = mock(SearchResultsService.class);
        phoneNumberService = mock(PhoneNumberService.class);
        messageSource = mock(MessageSource.class);

        when(messageSource.getMessage(any(), any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        MessageUtil.setMessageSource(messageSource);

        controller.projectService = projectService;
        controller.searchResultsService = searchResultsService;
        controller.phoneNumberService = phoneNumberService;
    }

    @Test
    public void testPhoneNumberValidation_ReturnsDistinctInstances() {
        when(phoneNumberService.validatePhoneNumber("1234567890")).thenReturn(true);
        when(phoneNumberService.validatePhoneNumber("invalid-phone")).thenReturn(false);

        MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.setParameter("fieldId", "phone1");
        request1.setParameter("value", "1234567890");

        MockHttpServletRequest request2 = new MockHttpServletRequest();
        request2.setParameter("fieldId", "phone2");
        request2.setParameter("value", "invalid-phone");

        ResponseObject response1 = controller.getPhoneNumberValidation(request1);
        ResponseObject response2 = controller.getPhoneNumberValidation(request2);

        assertNotSame("Controller must return a new ResponseObject for each request to ensure thread safety",
                response1, response2);
        assertTrue(response1.isStatus());
        assertFalse(response2.isStatus());
    }

    @Test
    public void testPhoneNumberValidation_ValidAndInvalidPhone() {
        when(phoneNumberService.validatePhoneNumber("1234567890")).thenReturn(true);
        when(phoneNumberService.validatePhoneNumber("abc")).thenReturn(false);

        MockHttpServletRequest validReq = new MockHttpServletRequest();
        validReq.setParameter("fieldId", "phone");
        validReq.setParameter("value", "1234567890");

        ResponseObject validResp = controller.getPhoneNumberValidation(validReq);
        assertTrue(validResp.isStatus());
        assertEquals("Valid phone number", validResp.getBody());

        MockHttpServletRequest invalidReq = new MockHttpServletRequest();
        invalidReq.setParameter("fieldId", "phone");
        invalidReq.setParameter("value", "abc");

        ResponseObject invalidResp = controller.getPhoneNumberValidation(invalidReq);
        assertFalse(invalidResp.isStatus());
    }

    @Test
    public void testAccessionNumberGenerator_NoStateBleedBetweenRequests() {
        when(projectService.getAllProjects()).thenReturn(Collections.emptyList());

        ResponseObject failResponse = controller.accessionNumberGenerator("NON_EXISTENT_PROGRAM", false, null);
        assertFalse("Failed generator request must return status=false", failResponse.isStatus());

        Project project = new Project();
        project.setProgramCode("TESTPROG");
        when(projectService.getAllProjects()).thenReturn(List.of(project));

        ResponseObject nextFailResponse = controller.accessionNumberGenerator("INVALID_AGAIN", false, null);
        assertFalse("Subsequent failed request must return status=false without state bleed",
                nextFailResponse.isStatus());
        assertNotSame("Each call must return a fresh ResponseObject", failResponse, nextFailResponse);
    }

    @Test
    public void testValidateSubjectNumberAndNationalId_ReturnsFreshResponseObject() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setParameter("numberType", "subjectNumber");
        request.setParameter("subjectNumber", "");

        ResponseObject resp1 = controller.validateSubjectNumberAndNationalId(request, response);
        ResponseObject resp2 = controller.validateSubjectNumberAndNationalId(request, response);

        assertNotSame("Each call to validateSubjectNumberAndNationalId must return an independent ResponseObject",
                resp1, resp2);
    }

    @Test
    public void testConcurrentRequestsDoNotInterfere() throws InterruptedException, ExecutionException {
        when(phoneNumberService.validatePhoneNumber("1234567890")).thenReturn(true);
        when(phoneNumberService.validatePhoneNumber("invalid")).thenReturn(false);

        int threads = 10;
        int iterations = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            final boolean expectValid = (i % 2 == 0);
            tasks.add(() -> {
                MockHttpServletRequest req = new MockHttpServletRequest();
                req.setParameter("fieldId", "phone");
                req.setParameter("value", expectValid ? "1234567890" : "invalid");

                ResponseObject res = controller.getPhoneNumberValidation(req);
                return res.isStatus() == expectValid;
            });
        }

        List<Future<Boolean>> futures = executor.invokeAll(tasks);
        for (Future<Boolean> future : futures) {
            assertTrue("Concurrent request validation was corrupted by shared state", future.get());
        }
        executor.shutdown();
    }
}
