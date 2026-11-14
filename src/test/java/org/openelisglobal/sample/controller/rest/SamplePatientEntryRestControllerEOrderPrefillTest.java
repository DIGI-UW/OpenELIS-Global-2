package org.openelisglobal.sample.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.parser.IParser;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Task;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.service.SamplePatientEntryService;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.systemuser.service.UserService;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class SamplePatientEntryRestControllerEOrderPrefillTest {

    @Mock
    private ElectronicOrderService electronicOrderService;
    @Mock
    private SamplePatientEntryService samplePatientService;
    @Mock
    private FhirUtil fhirUtil;
    @Mock
    private OrganizationService organizationService;
    @Mock
    private ProviderService providerService;
    @Mock
    private UserService userService;
    @Mock
    private DisplayListService displayListService;
    @Mock
    private ProjectService projectService;
    @Mock
    private LocalizationService localizationService;

    @InjectMocks
    private SamplePatientEntryRestController controller;

    private MockHttpServletRequest request;
    private AutowireCapableBeanFactory previousFactory;
    private Object previousMessageUtilInstance;

    @Before
    public void setUp() {
        AutowireCapableBeanFactory beanFactory = mock(AutowireCapableBeanFactory.class);
        DefaultConfigurationProperties configProps = mock(DefaultConfigurationProperties.class);
        when(configProps.getPropertyValue(ConfigurationProperties.Property.DEFAULT_DATE_LOCALE)).thenReturn("en-US");
        when(beanFactory.getBean(any(Class.class))).thenAnswer(inv -> {
            Class<?> clazz = inv.getArgument(0);
            if (DefaultConfigurationProperties.class.isAssignableFrom(clazz)) {
                return configProps;
            }
            return mock(clazz);
        });
        previousFactory = (AutowireCapableBeanFactory) ReflectionTestUtils.getField(org.openelisglobal.spring.util.SpringContext.class, "factory");
        ReflectionTestUtils.setField(org.openelisglobal.spring.util.SpringContext.class, "factory", beanFactory);

        MessageSource messageSource = mock(MessageSource.class);
        when(messageSource.getMessage(anyString(), any(), anyString(), any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if ("date.format".equals(key) || "date.format.formatKey".equals(key)) {
                return "MM/dd/yyyy";
            }
            if ("dateTime.format".equals(key) || "timestamp.format.formatKey".equals(key)) {
                return "MM/dd/yyyy HH:mm";
            }
            if ("time.format".equals(key) || "time.format.formatKey".equals(key)) {
                return "HH:mm";
            }
            return invocation.getArgument(2);
        });
        previousMessageUtilInstance = ReflectionTestUtils.getField(MessageUtil.class, "instance");
        MessageUtil.setMessageSource(messageSource);

        ReflectionTestUtils.setField(DisplayListService.class, "instance", displayListService);
        when(displayListService.getList(any())).thenReturn(Collections.emptyList());
        when(displayListService.getFreshList(any())).thenReturn(Collections.emptyList());

        request = new MockHttpServletRequest();
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        request.setAttribute(IActionConstants.USER_SESSION_DATA, usd);

        ReflectionTestUtils.setField(controller, "projectService", projectService);
        ReflectionTestUtils.setField(controller, "localizationService", localizationService);
        ReflectionTestUtils.setField(controller, "request", request);
        when(projectService.getAll()).thenReturn(Collections.emptyList());
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(DisplayListService.class, "instance", null);
        ReflectionTestUtils.setField(MessageUtil.class, "instance", previousMessageUtilInstance);
        ReflectionTestUtils.setField(org.openelisglobal.spring.util.SpringContext.class, "factory", previousFactory);
    }

    @Test
    public void setupForm_withMultipleElectronicOrders_picksNewestAndCallsPrefill() throws Exception {
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        String externalOrderNumber = "ext-order-1";

        ElectronicOrder olderOrder = new ElectronicOrder();
        olderOrder.setId("eo-1");
        olderOrder.setData("{\"resourceType\":\"Task\",\"id\":\"task-older\"}");
        olderOrder.setPriority(OrderPriority.ROUTINE);

        ElectronicOrder newerOrder = new ElectronicOrder();
        newerOrder.setId("eo-2");
        newerOrder.setData("{\"resourceType\":\"Task\",\"id\":\"task-newer\"}");
        newerOrder.setPriority(OrderPriority.STAT);

        List<ElectronicOrder> eOrders = Arrays.asList(olderOrder, newerOrder);
        when(electronicOrderService.getElectronicOrdersByExternalId(externalOrderNumber)).thenReturn(eOrders);

        Task newerTask = new Task();
        newerTask.setId("task-newer");

        IParser parser = mock(IParser.class);
        when(fhirUtil.getFhirParser()).thenReturn(parser);
        when(parser.parseResource(eq(Task.class), eq(newerOrder.getData()))).thenReturn(newerTask);

        controller.setupForm(form, request, externalOrderNumber);

        assertEquals(OrderPriority.STAT, form.getSampleOrderItems().getPriority());
        verify(parser, never()).parseResource(eq(Task.class), eq(olderOrder.getData()));
        verify(samplePatientService).resolveAndPrefillQuestionnaireResponse(form.getSampleOrderItems(), newerTask,
                newerOrder);
    }

    @Test
    public void setupForm_withBlankExternalOrderNumber_skipsPrefill() throws Exception {
        SamplePatientEntryForm form = new SamplePatientEntryForm();

        controller.setupForm(form, request, "");

        verify(electronicOrderService, never()).getElectronicOrdersByExternalId(anyString());
        verify(samplePatientService, never()).resolveAndPrefillQuestionnaireResponse(any(), any(), any());
    }

    @Test
    public void setupForm_withNoMatchingElectronicOrders_skipsPrefill() throws Exception {
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        String externalOrderNumber = "missing-order";
        when(electronicOrderService.getElectronicOrdersByExternalId(externalOrderNumber))
                .thenReturn(Collections.emptyList());

        controller.setupForm(form, request, externalOrderNumber);

        verify(samplePatientService, never()).resolveAndPrefillQuestionnaireResponse(any(), any(), any());
    }

    @Test
    public void prefillElectronicOrderIfPresent_whenAdditionalQuestionsHasNoMeaningfulId_resolvesNewestEOrderAndPrefills() {
        SampleOrderItem sampleOrder = new SampleOrderItem();
        sampleOrder.setExternalOrderNumber("ext-post-1");
        sampleOrder.setAdditionalQuestions(null);

        ElectronicOrder olderOrder = new ElectronicOrder();
        olderOrder.setId("eo-post-old");
        olderOrder.setData("{\"resourceType\":\"Task\",\"id\":\"task-post-old\"}");

        ElectronicOrder newerOrder = new ElectronicOrder();
        newerOrder.setId("eo-post-new");
        newerOrder.setData("{\"resourceType\":\"Task\",\"id\":\"task-post-new\"}");

        when(electronicOrderService.getElectronicOrdersByExternalId("ext-post-1"))
                .thenReturn(Arrays.asList(olderOrder, newerOrder));

        Task newerTask = new Task();
        newerTask.setId("task-post-new");

        IParser parser = mock(IParser.class);
        when(fhirUtil.getFhirParser()).thenReturn(parser);
        when(parser.parseResource(eq(Task.class), eq(newerOrder.getData()))).thenReturn(newerTask);

        controller.prefillElectronicOrderIfPresent(sampleOrder);

        verify(samplePatientService).resolveAndPrefillQuestionnaireResponse(sampleOrder, newerTask, newerOrder);
        verify(parser, never()).parseResource(eq(Task.class), eq(olderOrder.getData()));
    }

    @Test
    public void prefillElectronicOrderIfPresent_whenProgramIdIsBlank_resolvesNewestEOrderAndPrefills() {
        SampleOrderItem sampleOrder = new SampleOrderItem();
        sampleOrder.setExternalOrderNumber("ext-post-2");
        QuestionnaireResponse existingQr = new QuestionnaireResponse();
        existingQr.setId("existing-qr-id");
        sampleOrder.setAdditionalQuestions(existingQr);
        sampleOrder.setProgramId(""); // Program ID is blank

        ElectronicOrder newerOrder = new ElectronicOrder();
        newerOrder.setId("eo-post-2");
        newerOrder.setData("{\"resourceType\":\"Task\",\"id\":\"task-post-2\"}");

        when(electronicOrderService.getElectronicOrdersByExternalId("ext-post-2"))
                .thenReturn(Collections.singletonList(newerOrder));

        Task task = new Task();
        task.setId("task-post-2");

        IParser parser = mock(IParser.class);
        when(fhirUtil.getFhirParser()).thenReturn(parser);
        when(parser.parseResource(eq(Task.class), eq(newerOrder.getData()))).thenReturn(task);

        controller.prefillElectronicOrderIfPresent(sampleOrder);

        verify(samplePatientService).resolveAndPrefillQuestionnaireResponse(sampleOrder, task, newerOrder);
    }

    @Test
    public void prefillElectronicOrderIfPresent_whenAlreadyHasMeaningfulIdAndProgramId_skipsEOrderResolution() {
        SampleOrderItem sampleOrder = new SampleOrderItem();
        sampleOrder.setExternalOrderNumber("ext-post-3");
        QuestionnaireResponse existingQr = new QuestionnaireResponse();
        existingQr.setId("meaningful-qr-id");
        sampleOrder.setAdditionalQuestions(existingQr);
        sampleOrder.setProgramId("prog-456");

        controller.prefillElectronicOrderIfPresent(sampleOrder);

        verify(electronicOrderService, never()).getElectronicOrdersByExternalId(anyString());
        verify(samplePatientService, never()).resolveAndPrefillQuestionnaireResponse(any(), any(), any());
    }

    @Test
    public void prefillElectronicOrderIfPresent_whenExternalOrderNumberBlank_skipsEOrderResolution() {
        SampleOrderItem sampleOrder = new SampleOrderItem();
        sampleOrder.setExternalOrderNumber("   ");

        controller.prefillElectronicOrderIfPresent(sampleOrder);

        verify(electronicOrderService, never()).getElectronicOrdersByExternalId(anyString());
        verify(samplePatientService, never()).resolveAndPrefillQuestionnaireResponse(any(), any(), any());
    }
}
