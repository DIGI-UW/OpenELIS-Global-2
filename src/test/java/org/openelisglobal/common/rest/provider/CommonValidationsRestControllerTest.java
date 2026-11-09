package org.openelisglobal.common.rest.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.common.rest.provider.CommonValidationsRestController.ResponseObject;
import org.openelisglobal.common.services.PhoneNumberService;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.search.service.SearchResultsService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

public class CommonValidationsRestControllerTest {

    private CommonValidationsRestController controller;
    private ProjectService projectService;
    private SearchResultsService searchResultsService;
    private PhoneNumberService phoneNumberService;
    private MessageSource messageSource;
    private AutowireCapableBeanFactory beanFactory;
    private DefaultConfigurationProperties configurationProperties;
    private AccessionNumberValidatorFactory validatorFactory;
    private IAccessionNumberGenerator accessionNumberGenerator;

    private Object previousMessageUtilInstance;
    private AutowireCapableBeanFactory previousFactory;

    @Before
    public void setUp() throws Exception {
        controller = new CommonValidationsRestController();
        projectService = mock(ProjectService.class);
        searchResultsService = mock(SearchResultsService.class);
        phoneNumberService = mock(PhoneNumberService.class);
        messageSource = mock(MessageSource.class);
        beanFactory = mock(AutowireCapableBeanFactory.class);
        configurationProperties = mock(DefaultConfigurationProperties.class);
        validatorFactory = mock(AccessionNumberValidatorFactory.class);
        accessionNumberGenerator = mock(IAccessionNumberGenerator.class);

        Field factoryField = SpringContext.class.getDeclaredField("factory");
        factoryField.setAccessible(true);
        previousFactory = (AutowireCapableBeanFactory) factoryField.get(null);
        factoryField.set(null, beanFactory);

        when(beanFactory.getBean(DefaultConfigurationProperties.class)).thenReturn(configurationProperties);
        when(beanFactory.getBean(AccessionNumberValidatorFactory.class)).thenReturn(validatorFactory);
        when(validatorFactory.getGenerator(any())).thenReturn(accessionNumberGenerator);

        when(configurationProperties.getPropertyValue(Property.PHONE_FORMAT)).thenReturn("(xxx) xxx-xxxx");
        when(configurationProperties.isPropertyValueEqual(Property.VALIDATE_PHONE_FORMAT, "false")).thenReturn(false);

        previousMessageUtilInstance = ReflectionTestUtils.getField(MessageUtil.class, "instance");
        when(messageSource.getMessage(any(), any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        MessageUtil.setMessageSource(messageSource);

        controller.projectService = projectService;
        controller.searchResultsService = searchResultsService;
        controller.phoneNumberService = phoneNumberService;
    }

    @After
    public void tearDown() throws Exception {
        ReflectionTestUtils.setField(MessageUtil.class, "instance", previousMessageUtilInstance);
        Field factoryField = SpringContext.class.getDeclaredField("factory");
        factoryField.setAccessible(true);
        factoryField.set(null, previousFactory);
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
        assertEquals("phone.number.format.error", invalidResp.getBody());
    }

    @Test
    public void testAccessionNumberGenerator_NoStateBleedBetweenRequests() {
        when(accessionNumberGenerator.getNextAvailableAccessionNumber("", true)).thenReturn("ACC12345");

        ResponseObject successResponse = controller.accessionNumberGenerator("", false, null);
        assertTrue("Successful generator request must return status=true", successResponse.isStatus());
        assertEquals("ACC12345", successResponse.getBody());

        when(projectService.getAllProjects()).thenReturn(Collections.emptyList());

        ResponseObject failResponse = controller.accessionNumberGenerator("NON_EXISTENT_PROGRAM", false, null);
        assertFalse("Subsequent failed request must return status=false without state bleed from prior success",
                failResponse.isStatus());
        assertNotSame("Each call must return a fresh ResponseObject", successResponse, failResponse);
    }

    @Test
    public void testValidateSubjectNumberAndNationalId_DuplicateAndNonDuplicateIsolation() throws Exception {
        MockHttpServletRequest dupReq = new MockHttpServletRequest();
        dupReq.setParameter("numberType", "subjectNumber");
        dupReq.setParameter("subjectNumber", "SUBJ-001");
        when(searchResultsService.getSearchResultsExact(null, null, null, "SUBJ-001", null, null, null, null, null,
                null)).thenReturn(List.of(new PatientSearchResults()));
        when(configurationProperties.isPropertyValueEqual(Property.ALLOW_DUPLICATE_SUBJECT_NUMBERS, "true"))
                .thenReturn(false);

        ResponseObject dupResp = controller.validateSubjectNumberAndNationalId(dupReq, new MockHttpServletResponse());
        assertFalse("Duplicate subject number must return status=false", dupResp.isStatus());
        assertTrue("Duplicate response body must contain warning key",
                dupResp.getBody().contains("error.duplicate.subjectNumber.warning"));

        MockHttpServletRequest validReq = new MockHttpServletRequest();
        validReq.setParameter("numberType", "subjectNumber");
        validReq.setParameter("subjectNumber", "SUBJ-002");
        when(searchResultsService.getSearchResultsExact(null, null, null, "SUBJ-002", null, null, null, null, null,
                null)).thenReturn(Collections.emptyList());

        ResponseObject validResp = controller.validateSubjectNumberAndNationalId(validReq,
                new MockHttpServletResponse());
        assertTrue("Non-duplicate subject number must return status=true", validResp.isStatus());
        assertEquals("Valid", validResp.getBody());
        assertNotSame("Distinct requests must return independent ResponseObject instances", dupResp, validResp);
    }

    @Test
    public void testValidateSubjectNumberAndNationalId_MissingServiceThrowsException() throws Exception {
        controller.searchResultsService = null;
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("numberType", "subjectNumber");
        req.setParameter("subjectNumber", "SUBJ-001");

        try {
            controller.validateSubjectNumberAndNationalId(req, new MockHttpServletResponse());
            fail("Missing SearchResultsService must throw IllegalStateException");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("SearchResultsService is required"));
        }
    }

    @Test
    public void testConcurrentRequestsDoNotInterfere() throws InterruptedException, ExecutionException {
        when(phoneNumberService.validatePhoneNumber("1234567890")).thenReturn(true);
        when(phoneNumberService.validatePhoneNumber("invalid")).thenReturn(false);

        int threads = 10;
        int iterations = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        try {
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
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
