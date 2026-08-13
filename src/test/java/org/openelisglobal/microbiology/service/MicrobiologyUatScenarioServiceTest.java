package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
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
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.service.InventoryManagementService;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.microbiology.form.MicroAntibioticAdminForm;
import org.openelisglobal.microbiology.form.MicroAstPanelAdminForm;
import org.openelisglobal.microbiology.form.MicroBreakpointImportPreviewForm;
import org.openelisglobal.microbiology.form.MicroBreakpointStandardAdminForm;
import org.openelisglobal.microbiology.form.MicroOrganismAdminForm;
import org.openelisglobal.microbiology.form.MicroReferenceAdminPageForm;
import org.openelisglobal.microbiology.form.MicroReferenceAdminQueryForm;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioForm;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioRequestForm;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.qaevent.service.NceCategoryService;
import org.openelisglobal.qaevent.service.NceTypeService;
import org.openelisglobal.qaevent.valueholder.NceCategory;
import org.openelisglobal.qaevent.valueholder.NceType;
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
import org.openelisglobal.testmethod.service.TestMethodService;
import org.openelisglobal.testmethod.valueholder.TestMethod;
import org.openelisglobal.testreagentlink.service.TestReagentLinkService;
import org.openelisglobal.testreagentlink.valueholder.TestReagentLink;
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
    private TestMethodService testMethodService;

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
    private InventoryItemService inventoryItemService;

    @Mock
    private InventoryLotService inventoryLotService;

    @Mock
    private InventoryManagementService inventoryManagementService;

    @Mock
    private TestReagentLinkService testReagentLinkService;

    @Mock
    private NceCategoryService nceCategoryService;

    @Mock
    private NceTypeService nceTypeService;

    @Mock
    private MicrobiologyReferenceAdminService referenceAdminService;

    @Mock
    private MicroBreakpointAdminService breakpointAdminService;

    @Mock
    private MicroBreakpointImportService breakpointImportService;

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
        doAnswer(invocation -> {
            InventoryItem item = invocation.getArgument(0);
            item.setId("UAT microbiology blood agar".equals(item.getName()) ? 13L : 14L);
            return null;
        }).when(inventoryItemService).insert(any(InventoryItem.class));

        service = new MicrobiologyUatScenarioService(methodService, sampleService, sampleItemService, patientService,
                personService, sampleHumanService, typeOfSampleService, typeOfSampleTestService, testService,
                testSectionService, localizationService, analyteService, testAnalyteService, analysisService,
                testResultService, testMethodService, statusService, configurationService, caseService,
                orderRoutingService, inventoryItemService, inventoryLotService, inventoryManagementService,
                testReagentLinkService, referenceAdminService, breakpointAdminService, breakpointImportService,
                nceCategoryService, nceTypeService);
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
        ArgumentCaptor<TestMethod> methodLinkCaptor = ArgumentCaptor.forClass(TestMethod.class);
        verify(testMethodService).linkMethod(methodLinkCaptor.capture());
        assertEquals(test.getId(), methodLinkCaptor.getValue().getTestId());
        assertEquals(method.getId(), methodLinkCaptor.getValue().getMethodId());
        assertTrue(methodLinkCaptor.getValue().getIsDefaultMethod());
        ArgumentCaptor<TestResult> testResultCaptor = ArgumentCaptor.forClass(TestResult.class);
        verify(testResultService).insert(testResultCaptor.capture());
        assertEquals(test, testResultCaptor.getValue().getTest());
        assertEquals("R", testResultCaptor.getValue().getTestResultType());
        assertTrue(testResultCaptor.getValue().getIsActive());
    }

    @Test
    public void provisionsReusableLotTraceabilityFixturesThroughServices() {
        Sample sample = sample("sample-1");
        SampleItem sampleItem = sampleItem("sample-item-1");
        Method method = method("method-1");
        org.openelisglobal.test.valueholder.Test test = test("test-1");
        TestAnalyte testAnalyte = testAnalyte("test-analyte-1");
        Analysis analysis = analysis("analysis-1");
        MicroCase microCase = microCase("case-1");
        configureHappyPath(sample, sampleItem, method, test, testAnalyte, analysis, microCase);

        MicrobiologyUatScenarioRequestForm request = new MicrobiologyUatScenarioRequestForm();
        request.scenario = "MVP";
        request.scenarioKey = "playwright-lot-traceability";

        service.provision(request, "1");

        ArgumentCaptor<InventoryItem> itemCaptor = ArgumentCaptor.forClass(InventoryItem.class);
        verify(inventoryItemService, times(2)).insert(itemCaptor.capture());
        assertEquals("UAT microbiology blood agar", itemCaptor.getAllValues().get(0).getName());
        assertEquals("UAT microbiology AST card", itemCaptor.getAllValues().get(1).getName());
        ArgumentCaptor<TestReagentLink> linkCaptor = ArgumentCaptor.forClass(TestReagentLink.class);
        verify(testReagentLinkService, times(2)).insert(linkCaptor.capture());
        assertEquals("PRIMARY", linkCaptor.getAllValues().get(0).getUsageType());
        assertEquals("SECONDARY", linkCaptor.getAllValues().get(1).getUsageType());
        ArgumentCaptor<InventoryLot> lotCaptor = ArgumentCaptor.forClass(InventoryLot.class);
        verify(inventoryManagementService, times(5)).receiveInventory(lotCaptor.capture(), anyString());
        assertEquals("UAT-MICRO-MEDIA-EXPIRED", lotCaptor.getAllValues().get(0).getLotNumber());
        assertTrue(lotCaptor.getAllValues().get(0).isExpired());
        assertEquals("UAT-MICRO-MEDIA-FEFO", lotCaptor.getAllValues().get(1).getLotNumber());
    }

    @Test
    public void provisionsSpecimenLostVocabularyThroughServices() {
        Sample sample = sample("sample-1");
        SampleItem sampleItem = sampleItem("sample-item-1");
        Method method = method("method-1");
        org.openelisglobal.test.valueholder.Test test = test("test-1");
        TestAnalyte testAnalyte = testAnalyte("test-analyte-1");
        Analysis analysis = analysis("analysis-1");
        MicroCase microCase = microCase("case-1");
        configureHappyPath(sample, sampleItem, method, test, testAnalyte, analysis, microCase);
        doAnswer(invocation -> {
            NceCategory category = invocation.getArgument(0);
            Integer generatedId = System.identityHashCode(category);
            category.setId(generatedId);
            return generatedId;
        }).when(nceCategoryService).insert(any(NceCategory.class));

        MicrobiologyUatScenarioRequestForm request = new MicrobiologyUatScenarioRequestForm();
        request.scenario = "MVP";
        request.scenarioKey = "playwright-nce-vocabulary";

        service.provision(request, "1");

        ArgumentCaptor<NceCategory> categoryCaptor = ArgumentCaptor.forClass(NceCategory.class);
        verify(nceCategoryService).insert(categoryCaptor.capture());
        assertEquals("Pre-analytical", categoryCaptor.getValue().getName());
        assertTrue(categoryCaptor.getValue().getActive());
        ArgumentCaptor<NceType> typeCaptor = ArgumentCaptor.forClass(NceType.class);
        verify(nceTypeService).insert(typeCaptor.capture());
        assertEquals("Specimen lost", typeCaptor.getValue().getName());
        assertEquals(categoryCaptor.getValue().getId(), typeCaptor.getValue().getCategoryId());
        assertTrue(typeCaptor.getValue().getActive());
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

    @Test
    public void provisionsM3ReferenceAdministrationDataThroughServices() {
        Sample sample = sample("sample-1");
        SampleItem sampleItem = sampleItem("sample-item-1");
        Method method = method("method-1");
        org.openelisglobal.test.valueholder.Test test = test("test-1");
        TestAnalyte testAnalyte = testAnalyte("test-analyte-1");
        Analysis analysis = analysis("analysis-1");
        MicroCase microCase = microCase("case-1");
        configureHappyPath(sample, sampleItem, method, test, testAnalyte, analysis, microCase);

        MicroAstPanelAdminForm panel = new MicroAstPanelAdminForm();
        panel.id = "panel-1";
        panel.name = "Gram negative AST panel (UAT)";
        panel.current = true;
        when(referenceAdminService.getAstPanels(any(MicroReferenceAdminQueryForm.class))).thenReturn(pageOf(panel));
        when(referenceAdminService.getOrganisms(any(MicroReferenceAdminQueryForm.class)))
                .thenReturn(new MicroReferenceAdminPageForm<>());
        when(referenceAdminService.getAntibiotics(any(MicroReferenceAdminQueryForm.class)))
                .thenReturn(new MicroReferenceAdminPageForm<>());
        when(referenceAdminService.saveOrganism(any(), any(MicroOrganismAdminForm.class), anyString()))
                .thenAnswer(invocation -> {
                    MicroOrganismAdminForm form = invocation.getArgument(1);
                    form.id = "organism-ref";
                    return form;
                });
        when(referenceAdminService.saveAntibiotic(any(), any(MicroAntibioticAdminForm.class), anyString()))
                .thenAnswer(invocation -> {
                    MicroAntibioticAdminForm form = invocation.getArgument(1);
                    form.id = "antibiotic-ref";
                    return form;
                });
        MicroBreakpointImportPreviewForm preview = new MicroBreakpointImportPreviewForm();
        preview.previewToken = "preview-1";
        when(breakpointImportService.preview(anyString())).thenReturn(preview);
        MicroBreakpointStandardAdminForm loaded = new MicroBreakpointStandardAdminForm();
        loaded.id = "standard-loaded";
        loaded.version = "SYNTH-UAT-LOADED";
        when(breakpointAdminService.getStandards(any(MicroReferenceAdminQueryForm.class))).thenReturn(pageOf(loaded));

        MicrobiologyUatScenarioRequestForm request = new MicrobiologyUatScenarioRequestForm();
        request.scenario = "M3";
        request.scenarioKey = "playwright-reference-admin";
        MicrobiologyUatScenarioForm result = service.provision(request, "1");

        assertEquals("organism-ref", result.organismId);
        assertEquals("antibiotic-ref", result.antibioticId);
        assertEquals("panel-1", result.astPanelId);
        assertEquals("standard-1", result.activeBreakpointStandardId);
        assertEquals("standard-loaded", result.loadedBreakpointStandardId);
        verify(breakpointAdminService).activate(anyString(), any(), anyString());
        verify(breakpointImportService).apply("preview-1", "1");
    }

    @Test
    public void provisionsR1ClassificationScenarioThroughServices() {
        Sample sample = sample("sample-1");
        SampleItem sampleItem = sampleItem("sample-item-1");
        Method method = method("method-1");
        org.openelisglobal.test.valueholder.Test test = test("test-1");
        TestAnalyte testAnalyte = testAnalyte("test-analyte-1");
        Analysis analysis = analysis("analysis-1");
        MicroCase routedCase = microCase("case-bacteriology");
        MicroCase unassignedCase = microCase("case-unassigned");
        unassignedCase.setWorkflowType(MicroWorkflowType.UNASSIGNED.name());
        configureHappyPath(sample, sampleItem, method, test, testAnalyte, analysis, routedCase);
        when(testService.getTestByDescription("UAT microbiology TB culture")).thenReturn(null);
        when(testService.getTestByDescription("UAT routine non-culture test")).thenReturn(null);
        doAnswer(invocation -> {
            Method inserted = invocation.getArgument(0);
            inserted.setId("method-alternate");
            return null;
        }).when(methodService).insert(any(Method.class));
        doAnswer(invocation -> {
            org.openelisglobal.test.valueholder.Test inserted = invocation.getArgument(0);
            if ("UAT microbiology TB culture".equals(inserted.getDescription())) {
                inserted.setId("test-tb");
            } else if ("UAT routine non-culture test".equals(inserted.getDescription())) {
                inserted.setId("test-routine");
            }
            return null;
        }).when(testService).insert(any(org.openelisglobal.test.valueholder.Test.class));
        when(caseService.createOrGetCase(sampleItem.getId(), MicroWorkflowType.UNASSIGNED, null, "1"))
                .thenReturn(unassignedCase);
        MicrobiologyUatScenarioRequestForm request = new MicrobiologyUatScenarioRequestForm();
        request.scenario = "R1";
        request.scenarioKey = "playwright-r1-workflow-classification";

        MicrobiologyUatScenarioForm result = service.provision(request, "1");

        assertEquals("case-unassigned", result.caseId);
        assertEquals("case-bacteriology", result.siblingCaseId);
        assertEquals("method-1", result.methodId);
        assertEquals("method-alternate", result.alternateMethodId);
        assertEquals("sample-type-1", result.sampleTypeId);
        assertEquals("test-1", result.cultureTestId);
        assertEquals("test-tb", result.tbCultureTestId);
        assertEquals("test-routine", result.nonCultureTestId);
        ArgumentCaptor<TestMethod> methodLinkCaptor = ArgumentCaptor.forClass(TestMethod.class);
        verify(testMethodService, times(4)).linkMethod(methodLinkCaptor.capture());
        TestMethod alternateLink = methodLinkCaptor.getAllValues().stream()
                .filter(link -> "method-alternate".equals(link.getMethodId())).findFirst().orElseThrow();
        assertEquals("test-1", alternateLink.getTestId());
        assertFalse(alternateLink.getIsDefaultMethod());
        verify(caseService).createOrGetCase(sampleItem.getId(), MicroWorkflowType.UNASSIGNED, null, "1");
        ArgumentCaptor<org.openelisglobal.microbiology.valueholder.MicroCultureSetup> setupCaptor = ArgumentCaptor
                .forClass(org.openelisglobal.microbiology.valueholder.MicroCultureSetup.class);
        verify(configurationService, times(3)).getOrCreateCultureSetup(setupCaptor.capture());
        assertEquals(MicroWorkflowType.BACTERIOLOGY.name(), setupCaptor.getAllValues().get(0).getWorkflowType());
        assertEquals(MicroWorkflowType.MYCOBACTERIOLOGY_TB.name(), setupCaptor.getAllValues().get(1).getWorkflowType());
        assertEquals("method-alternate", setupCaptor.getAllValues().get(2).getMethodId());
        assertEquals(MicroWorkflowType.BACTERIOLOGY.name(), setupCaptor.getAllValues().get(2).getWorkflowType());
    }

    @Test
    public void provisionsM4WhonetMappedAndUnmappedReferencesThroughServices() {
        Sample sample = sample("sample-1");
        SampleItem sampleItem = sampleItem("sample-item-1");
        Method method = method("method-1");
        org.openelisglobal.test.valueholder.Test test = test("test-1");
        TestAnalyte testAnalyte = testAnalyte("test-analyte-1");
        Analysis analysis = analysis("analysis-1");
        MicroCase microCase = microCase("case-1");
        configureHappyPath(sample, sampleItem, method, test, testAnalyte, analysis, microCase);

        MicroAstPanelAdminForm panel = new MicroAstPanelAdminForm();
        panel.id = "panel-1";
        panel.name = "Gram negative AST panel (UAT)";
        panel.current = true;
        when(referenceAdminService.getAstPanels(any(MicroReferenceAdminQueryForm.class))).thenReturn(pageOf(panel));
        when(referenceAdminService.getOrganisms(any(MicroReferenceAdminQueryForm.class)))
                .thenReturn(new MicroReferenceAdminPageForm<>());
        when(referenceAdminService.getAntibiotics(any(MicroReferenceAdminQueryForm.class)))
                .thenReturn(new MicroReferenceAdminPageForm<>());
        when(referenceAdminService.saveOrganism(any(), any(MicroOrganismAdminForm.class), anyString()))
                .thenAnswer(invocation -> {
                    MicroOrganismAdminForm form = invocation.getArgument(1);
                    form.id = "organism-mapped";
                    return form;
                });
        when(referenceAdminService.saveAntibiotic(any(), any(MicroAntibioticAdminForm.class), anyString()))
                .thenAnswer(invocation -> {
                    MicroAntibioticAdminForm form = invocation.getArgument(1);
                    form.id = "antibiotic-ref";
                    return form;
                });
        when(configurationService.createOrganism(any(MicroOrganism.class))).thenAnswer(invocation -> {
            MicroOrganism organism = invocation.getArgument(0);
            organism.setId("organism-unmapped");
            return organism;
        });
        MicroBreakpointImportPreviewForm preview = new MicroBreakpointImportPreviewForm();
        preview.previewToken = "preview-1";
        when(breakpointImportService.preview(anyString())).thenReturn(preview);
        MicroBreakpointStandardAdminForm loaded = new MicroBreakpointStandardAdminForm();
        loaded.id = "standard-loaded";
        loaded.version = "SYNTH-UAT-LOADED";
        when(breakpointAdminService.getStandards(any(MicroReferenceAdminQueryForm.class))).thenReturn(pageOf(loaded));

        MicrobiologyUatScenarioRequestForm request = new MicrobiologyUatScenarioRequestForm();
        request.scenario = "M4";
        request.scenarioKey = "playwright-whonet-export";
        MicrobiologyUatScenarioForm result = service.provision(request, "1");

        assertEquals("organism-mapped", result.organismId);
        assertEquals("organism-unmapped", result.unmappedOrganismId);
        ArgumentCaptor<MicroOrganism> organismCaptor = ArgumentCaptor.forClass(MicroOrganism.class);
        verify(configurationService).createOrganism(organismCaptor.capture());
        assertTrue(organismCaptor.getValue().getDisplayName().startsWith("WHONET mapping pending (UAT "));
        assertNull(organismCaptor.getValue().getWhonetCode());
        assertEquals("1", organismCaptor.getValue().getLastUpdatedBy());
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
        panel.setName("Gram negative AST panel (UAT)");
        MicroBreakpointStandard standard = new MicroBreakpointStandard();
        standard.setId("standard-1");
        when(configurationService.getOrCreateAntibiotic("Ciprofloxacin (UAT)", "CIPUAT", "Fluoroquinolone"))
                .thenReturn(ciprofloxacin);
        when(configurationService.getOrCreateAntibiotic("Gentamicin (UAT)", "GENUAT", "Aminoglycoside"))
                .thenReturn(gentamicin);
        when(configurationService.getOrCreateAstPanel(anyString(), anyString(), anyString())).thenReturn(panel);
        MicroOrganism organism = new MicroOrganism();
        organism.setId("organism-1");
        when(configurationService.getOrCreateOrganism(anyString(), anyString(), anyString())).thenReturn(organism);
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

    private <T> MicroReferenceAdminPageForm<T> pageOf(T row) {
        MicroReferenceAdminPageForm<T> page = new MicroReferenceAdminPageForm<>();
        page.rows = List.of(row);
        page.total = 1;
        return page;
    }
}
