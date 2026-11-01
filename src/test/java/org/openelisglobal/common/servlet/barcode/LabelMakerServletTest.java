package org.openelisglobal.common.servlet.barcode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.Errors;

/**
 * Covers validation and the actual servlet dispatch for alternate printing
 * routes.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class LabelMakerServletTest {

    @Mock
    private AutowireCapableBeanFactory beanFactory;

    @Mock
    private DefaultConfigurationProperties configurationProperties;

    @Mock
    private SampleService sampleService;

    private AutowireCapableBeanFactory previousFactory;

    private Object previousMessages;
    private LabelMakerServlet servlet;

    @Before
    public void setUp() {
        previousFactory = (AutowireCapableBeanFactory) ReflectionTestUtils.getField(SpringContext.class, "factory");
        ReflectionTestUtils.setField(SpringContext.class, "factory", beanFactory);

        when(beanFactory.getBean(SampleService.class)).thenReturn(sampleService);
        // ConfigurationProperties.getInstance() looks the bean up via SpringContext.
        when(beanFactory.getBean(DefaultConfigurationProperties.class)).thenReturn(configurationProperties);
        // ACCESSION_NUMBER_VALIDATE != "true" so validate() takes the no-strict-format
        // branch.
        when(configurationProperties.getPropertyValue(any(Property.class))).thenReturn("false");

        Sample sample = new Sample();
        sample.setId("S-1");
        sample.setAccessionNumber("ACC-1");
        when(sampleService.getSampleByAccessionNumber(anyString())).thenReturn(sample);

        previousMessages = ReflectionTestUtils.getField(MessageUtil.class, "instance");
        MessageSource messages = mock(MessageSource.class);
        MessageUtil.setMessageSource(messages);
        when(beanFactory.getBean(UserModuleService.class)).thenReturn(mock(UserModuleService.class));
        when(beanFactory.getBean(TestService.class)).thenReturn(mock(TestService.class));
        servlet = new LabelMakerServlet();
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(SpringContext.class, "factory", previousFactory);
        ReflectionTestUtils.setField(MessageUtil.class, "instance", previousMessages);
    }

    @Test
    public void validate_acceptsPositiveQuantity() {
        Errors errors = servlet.validate("ACC-1", "", "order", "5", "false");

        assertFalse(hasError(errors, "barcode.label.error.quantity.invalid"));
    }

    @Test
    public void validate_rejectsZeroQuantity() {
        // Frontend NumberInput clamps to >= 0, but a tampered URL or scripted
        // caller can still hit the servlet directly. Reject rather than silently
        // emitting an empty PDF that the cap-prompt UI misinterprets.
        Errors errors = servlet.validate("ACC-1", "", "order", "0", "false");

        assertTrue(hasError(errors, "barcode.label.error.quantity.invalid"));
    }

    @Test
    public void validate_rejectsNegativeQuantity() {
        Errors errors = servlet.validate("ACC-1", "", "order", "-3", "false");

        assertTrue(hasError(errors, "barcode.label.error.quantity.invalid"));
    }

    @Test
    public void validate_rejectsNonIntegerQuantity() {
        Errors errors = servlet.validate("ACC-1", "", "order", "abc", "false");

        assertTrue(hasError(errors, "barcode.label.error.quantity.invalid"));
    }

    @Test
    public void validate_rejectsQuantityAboveCap() {
        // Without the upper bound, override=true&quantity=999999999 hangs the
        // PDF render loop in BarcodeLabelMaker. The cap is the only guard.
        // ConfigurationProperties is mocked to return null for this property,
        // so getMaxRequestQuantity() falls back to DEFAULT_MAX_REQUEST_QUANTITY.
        Errors errors = servlet.validate("ACC-1", "", "order",
                Integer.toString(LabelMakerServlet.DEFAULT_MAX_REQUEST_QUANTITY + 1), "true");

        assertTrue(hasError(errors, "barcode.label.error.quantity.invalid"));
    }

    @Test
    public void validate_acceptsQuantityAtCap() {
        Errors errors = servlet.validate("ACC-1", "", "order",
                Integer.toString(LabelMakerServlet.DEFAULT_MAX_REQUEST_QUANTITY), "false");

        assertFalse(hasError(errors, "barcode.label.error.quantity.invalid"));
    }

    @Test
    public void validate_acceptsAllDispatcherBackedTypes() {
        for (String type : new String[] { "default", "order", "specimen", "blank", "blockOrder", "slideOrder",
                "freezerOrder" }) {
            Errors errors = servlet.validate("ACC-1", "", type, "1", "false");

            assertFalse("Expected " + type + " to be accepted", hasError(errors, "barcode.label.error.type.invalid"));
        }
    }

    @Test
    public void validate_rejectsBareBlockSlideFreezer() {
        // BarcodeLabelMaker.generateLabels has no branch for these — accepting
        // them would silently produce empty PDFs misread as "max reached".
        for (String bareType : new String[] { "block", "slide", "freezer" }) {
            Errors errors = servlet.validate("ACC-1", "", bareType, "1", "false");

            assertTrue("Expected " + bareType + " to be rejected",
                    hasError(errors, "barcode.label.error.type.invalid"));
        }
    }

    @Test
    public void validate_rejectsUnknownType() {
        Errors errors = servlet.validate("ACC-1", "", "totallyUnknown", "1", "false");

        assertTrue(hasError(errors, "barcode.label.error.type.invalid"));
    }

    @Test
    public void legacyDispatch_rejectsInvalidQuantitiesEvenWithOverride() throws Exception {
        for (String type : new String[] { "block", "slide" }) {
            for (String quantity : new String[] { "0", "-1", "101", "2147483648", "not-a-number" }) {
                MockHttpServletRequest request = authenticatedRequest();
                request.setParameter("labelType", type);
                request.setParameter("code", "ACC-1");
                request.setParameter("quantity", quantity);
                request.setParameter("override", "TRUE");
                MockHttpServletResponse response = new MockHttpServletResponse();
                servlet.doGet(request, response);
                assertEquals(type + " quantity=" + quantity, 400, response.getStatus());
            }
        }
    }

    @Test
    public void preprintDispatch_rejectsInvalidCountsBeforeAccessionGeneration() throws Exception {
        for (String field : new String[] { "numSetsOfLabels", "numOrderLabelsPerSet", "numSpecimenLabelsPerSet" }) {
            for (String quantity : new String[] { "-1", "101", "2147483648", "not-a-number" }) {
                MockHttpServletRequest request = authenticatedRequest();
                request.setParameter("prePrinting", "true");
                request.setParameter("testIds", "1");
                request.setParameter("numSetsOfLabels", "1");
                request.setParameter("numOrderLabelsPerSet", "1");
                request.setParameter("numSpecimenLabelsPerSet", "1");
                request.setParameter(field, quantity);
                MockHttpServletResponse response = new MockHttpServletResponse();
                servlet.doGet(request, response);
                assertEquals(field + "=" + quantity, 400, response.getStatus());
            }
        }
    }

    @Test
    public void validate_usesConfiguredLimitForBothOverrideValues() {
        when(configurationProperties.getPropertyValue(Property.MAX_REQUEST_LABEL_QUANTITY)).thenReturn("12");
        for (String override : new String[] { "false", "true", "TRUE" }) {
            assertFalse(hasError(servlet.validate("ACC-1", "", "order", "12", override),
                    "barcode.label.error.quantity.invalid"));
            assertTrue(hasError(servlet.validate("ACC-1", "", "order", "13", override),
                    "barcode.label.error.quantity.invalid"));
        }
    }

    private MockHttpServletRequest authenticatedRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, new UserSessionData());
        return request;
    }

    private boolean hasError(Errors errors, String code) {
        return errors.getAllErrors().stream().anyMatch(error -> code.equals(error.getCode()));
    }
}
