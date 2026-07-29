package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioRequestForm;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testanalyte.service.TestAnalyteService;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class MicrobiologyUatScenarioServiceTest {

    @Mock
    private MethodService methodService;

    @Mock
    private SampleService sampleService;

    @Mock
    private SampleItemService sampleItemService;

    @Mock
    private PatientService patientService;

    @Mock
    private PersonService personService;

    @Mock
    private SampleHumanService sampleHumanService;

    @Mock
    private TypeOfSampleService typeOfSampleService;

    @Mock
    private TypeOfSampleTestService typeOfSampleTestService;

    @Mock
    private TestService testService;

    @Mock
    private TestSectionService testSectionService;

    @Mock
    private LocalizationService localizationService;

    @Mock
    private AnalyteService analyteService;

    @Mock
    private TestAnalyteService testAnalyteService;

    @Mock
    private TestResultService testResultService;

    @Mock
    private AnalysisService analysisService;

    @Mock
    private IStatusService statusService;

    @Mock
    private MicrobiologyConfigurationService configurationService;

    @Mock
    private MicroCaseService caseService;

    @Mock
    private MicroOrderRoutingService orderRoutingService;

    @Mock
    private AutowireCapableBeanFactory beanFactory;

    @Mock
    private DefaultConfigurationProperties configurationProperties;

    @Mock
    private MessageSource messageSource;

    private MicrobiologyUatScenarioService service;
    private AutowireCapableBeanFactory previousFactory;
    private Object previousMessageUtilInstance;

    @Before
    public void setUp() {
        previousFactory = (AutowireCapableBeanFactory) ReflectionTestUtils.getField(SpringContext.class, "factory");
        previousMessageUtilInstance = ReflectionTestUtils.getField(MessageUtil.class, "instance");
        ReflectionTestUtils.setField(SpringContext.class, "factory", beanFactory);
        when(beanFactory.getBean(DefaultConfigurationProperties.class)).thenReturn(configurationProperties);
        when(configurationProperties.getPropertyValue(any(ConfigurationProperties.Property.class))).thenReturn("X");
        when(messageSource.getMessage(anyString(), any(), anyString(), any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if ("date.format.formatKey".equals(key)) {
                return "MM/dd/yyyy";
            }
            return invocation.getArgument(2);
        });
        MessageUtil.setMessageSource(messageSource);

        service = new MicrobiologyUatScenarioService(methodService, sampleService, sampleItemService, patientService,
                personService, sampleHumanService, typeOfSampleService, typeOfSampleTestService, testService,
                testSectionService, localizationService, analyteService, testAnalyteService, analysisService,
                testResultService, statusService, configurationService, caseService, orderRoutingService);
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(SpringContext.class, "factory", previousFactory);
        ReflectionTestUtils.setField(MessageUtil.class, "instance", previousMessageUtilInstance);
    }

    @Test
    public void provisionsPatientWithValidationSafeNameAndDeterministicExternalIdentifier() {
        Sample sample = sample("sample-1");
        SampleItem sampleItem = sampleItem("sample-item-1");
        Method method = method("method-1");
        org.openelisglobal.test.valueholder.Test test = test("test-1");
        TestAnalyte testAnalyte = testAnalyte("test-analyte-1");
        Analysis analysis = analysis("analysis-1");
        MicroCase microCase = microCase("case-1");
        configureHappyPath(sample, sampleItem, method, test, testAnalyte, analysis, microCase);

        MicrobiologyUatScenarioRequestForm request = new MicrobiologyUatScenarioRequestForm();
        request.scenario = "WORKLIST";
        request.scenarioKey = "playwright-worklist-7bd4adf1";

        service.provision(request, "1");

        ArgumentCaptor<Person> personCaptor = ArgumentCaptor.forClass(Person.class);
        verify(personService).insert(personCaptor.capture());
        Person person = personCaptor.getValue();
        assertEquals("UAT", person.getFirstName());
        assertEquals("Microbiology", person.getLastName());
        assertTrue(person.getLastName().matches("[A-Za-z]+"));

        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientService).insert(patientCaptor.capture());
        assertTrue(patientCaptor.getValue().getExternalId().startsWith("UATMICRO-"));
        assertEquals(patientCaptor.getValue().getExternalId(), patientCaptor.getValue().getNationalId());
        assertEquals(Timestamp.valueOf("1990-03-13 00:00:00"), patientCaptor.getValue().getBirthDate());

        ArgumentCaptor<TypeOfSampleTest> mappingCaptor = ArgumentCaptor.forClass(TypeOfSampleTest.class);
        verify(typeOfSampleTestService).insert(mappingCaptor.capture());
        assertEquals("sample-type-1", mappingCaptor.getValue().getTypeOfSampleId());
        assertEquals(test.getId(), mappingCaptor.getValue().getTestId());
        verify(typeOfSampleService).clearCache();
        assertNotNull(test.getLocalizedTestName());
        assertEquals("UAT microbiology culture", test.getLocalizedTestName().getEnglish());
        assertNotNull(test.getLocalizedReportingName());
        assertEquals("UAT microbiology culture", test.getLocalizedReportingName().getEnglish());
        ArgumentCaptor<TestResult> testResultCaptor = ArgumentCaptor.forClass(TestResult.class);
        verify(testResultService).insert(testResultCaptor.capture());
        assertEquals(test, testResultCaptor.getValue().getTest());
        assertEquals("R", testResultCaptor.getValue().getTestResultType());
        assertTrue(testResultCaptor.getValue().getIsActive());
    }

    @Test
    public void repairsExistingUatPatientMissingRequiredOrderDemographics() {
        Sample sample = sample("sample-1");
        SampleItem sampleItem = sampleItem("sample-item-1");
        Method method = method("method-1");
        org.openelisglobal.test.valueholder.Test test = test("test-1");
        TestAnalyte testAnalyte = testAnalyte("test-analyte-1");
        Analysis analysis = analysis("analysis-1");
        MicroCase microCase = microCase("case-1");
        configureHappyPath(sample, sampleItem, method, test, testAnalyte, analysis, microCase);
        Patient existingPatient = new Patient();
        existingPatient.setId("patient-1");
        existingPatient.setExternalId("UATMICRO-01C82736AB");
        when(patientService.getByExternalId(anyString())).thenReturn(existingPatient);

        MicrobiologyUatScenarioRequestForm request = new MicrobiologyUatScenarioRequestForm();
        request.scenario = "WORKLIST";
        request.scenarioKey = "review-amr-microbiology-mvp";

        service.provision(request, "1");

        verify(patientService).update(existingPatient);
        assertEquals(Timestamp.valueOf("1990-03-13 00:00:00"), existingPatient.getBirthDate());
        assertEquals("F", existingPatient.getGender());
        assertEquals(existingPatient.getExternalId(), existingPatient.getNationalId());
    }

    @Test
    public void repairsExistingUatMethodMissingLocalization() {
        Sample sample = sample("sample-1");
        SampleItem sampleItem = sampleItem("sample-item-1");
        Method method = method("method-1");
        org.openelisglobal.test.valueholder.Test test = test("test-1");
        TestAnalyte testAnalyte = testAnalyte("test-analyte-1");
        Analysis analysis = analysis("analysis-1");
        MicroCase microCase = microCase("case-1");
        configureHappyPath(sample, sampleItem, method, test, testAnalyte, analysis, microCase);
        when(localizationService.getAllActiveLocales()).thenReturn(List.of(Locale.ENGLISH, Locale.FRENCH));

        MicrobiologyUatScenarioRequestForm request = new MicrobiologyUatScenarioRequestForm();
        request.scenario = "WORKLIST";
        request.scenarioKey = "playwright-worklist-7bd4adf1";

        service.provision(request, "1");

        verify(methodService).update(method);
        assertNotNull(method.getLocalization());
        verify(localizationService).insert(method.getLocalization());
        assertEquals("UAT micro culture", method.getLocalization().getLocalizedValue(Locale.ENGLISH));
        assertEquals("UAT micro culture", method.getLocalization().getLocalizedValue(Locale.FRENCH));
    }

    private void configureHappyPath(Sample sample, SampleItem sampleItem, Method method,
            org.openelisglobal.test.valueholder.Test test,
            TestAnalyte testAnalyte, Analysis analysis, MicroCase microCase) {
        when(sampleService.getSampleByAccessionNumber(anyString())).thenReturn(sample);
        when(sampleItemService.getSampleItemsBySampleId(sample.getId())).thenReturn(List.of(sampleItem));
        when(patientService.getByExternalId(anyString())).thenReturn(null);
        doAnswer(invocation -> {
            ((Patient) invocation.getArgument(0)).setId("patient-1");
            return null;
        }).when(patientService).insert(any(Patient.class));
        when(sampleHumanService.getDataBySample(any(SampleHuman.class))).thenReturn(null);
        doAnswer(invocation -> {
            TypeOfSample sampleType = invocation.getArgument(0);
            sampleType.setId("sample-type-1");
            return null;
        }).when(typeOfSampleService).insert(any(TypeOfSample.class));

        MicroAntibiotic ciprofloxacin = antibiotic("antibiotic-cip");
        MicroAntibiotic gentamicin = antibiotic("antibiotic-gen");
        MicroAstPanel panel = new MicroAstPanel();
        panel.setId("panel-1");
        MicroBreakpointStandard standard = new MicroBreakpointStandard();
        standard.setId("standard-1");
        when(configurationService.getOrCreateAntibiotic("Ciprofloxacin (UAT)", "CIPUAT", "Fluoroquinolone"))
                .thenReturn(ciprofloxacin);
        when(configurationService.getOrCreateAntibiotic("Gentamicin (UAT)", "GENUAT", "Aminoglycoside"))
                .thenReturn(gentamicin);
        when(configurationService.getOrCreateAstPanel(anyString(), anyString(), anyString())).thenReturn(panel);
        when(configurationService.getOrCreateBreakpointStandard(anyString(), anyString(), any())).thenReturn(standard);

        when(methodService.getMethods(anyString())).thenReturn(List.of(method));
        when(testService.getTestByDescription(anyString())).thenReturn(test);
        TestSection testSection = new TestSection();
        testSection.setId("section-1");
        when(testSectionService.getAllActiveTestSections()).thenReturn(List.of(testSection));
        Analyte analyte = new Analyte();
        analyte.setId("analyte-1");
        when(analyteService.getAnalyteByName(any(Analyte.class), any(Boolean.class))).thenReturn(analyte);
        when(testAnalyteService.getAllTestAnalytesPerTest(test)).thenReturn(List.of(testAnalyte));
        when(analysisService.getAnalysisBySampleItemAndTest(sampleItem.getId(), test.getId())).thenReturn(analysis);
        when(orderRoutingService.routeAnalysesForSampleItem(sampleItem, List.of(analysis), "1"))
                .thenReturn(List.of(microCase));
        when(caseService.createOrGetCase(sampleItem.getId(), MicroWorkflowType.MYCOBACTERIOLOGY_TB, method.getId(), "1"))
                .thenReturn(microCase("case-tb"));
    }

    private Sample sample(String id) {
        Sample sample = new Sample();
        sample.setId(id);
        return sample;
    }

    private SampleItem sampleItem(String id) {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId(id);
        return sampleItem;
    }

    private Method method(String id) {
        Method method = new Method();
        method.setId(id);
        method.setMethodName("UAT micro culture");
        method.setIsActive(IActionConstants.YES);
        return method;
    }

    private org.openelisglobal.test.valueholder.Test test(String id) {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId(id);
        test.setDescription("UAT microbiology culture");
        return test;
    }

    private TestAnalyte testAnalyte(String id) {
        Analyte analyte = new Analyte();
        analyte.setId("analyte-1");
        TestAnalyte testAnalyte = new TestAnalyte();
        testAnalyte.setId(id);
        testAnalyte.setAnalyte(analyte);
        return testAnalyte;
    }

    private Analysis analysis(String id) {
        Analysis analysis = new Analysis();
        analysis.setId(id);
        return analysis;
    }

    private MicroAntibiotic antibiotic(String id) {
        MicroAntibiotic antibiotic = new MicroAntibiotic();
        antibiotic.setId(id);
        return antibiotic;
    }

    private MicroCase microCase(String id) {
        MicroCase microCase = new MicroCase();
        microCase.setId(id);
        microCase.setWorkflowType(MicroWorkflowType.BACTERIOLOGY.name());
        return microCase;
    }
}
