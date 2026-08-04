package org.openelisglobal.microbiology.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.service.InventoryManagementService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
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
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;
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
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testanalyte.service.TestAnalyteService;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testreagentlink.service.TestReagentLinkService;
import org.openelisglobal.testreagentlink.valueholder.TestReagentLink;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provisions deterministic review scenarios through normal application
 * services. The HTTP entry point is disabled unless the UAT scenario property
 * is explicitly enabled.
 */
@Service
public class MicrobiologyUatScenarioService {

    private static final String BACTERIOLOGY = MicroWorkflowType.BACTERIOLOGY.name();
    private static final String WORKLIST_SCENARIO = "WORKLIST";
    private static final String REFERENCE_ADMIN_SCENARIO = "M3";
    private static final String UAT_METHOD_NAME = "UAT micro culture";
    private static final String UAT_METHOD_DESCRIPTION = "UAT microbiology culture method";
    private static final String UAT_TEST_DESCRIPTION = "UAT microbiology culture";
    private static final String UAT_TEST_SECTION_NAME = "UAT Microbiology";
    private static final String UAT_ANALYTE_NAME = "UAT microbiology culture result";
    private static final String UAT_SAMPLE_TYPE_DESCRIPTION = "UAT micro specimen";
    private static final String UAT_PATIENT_EXTERNAL_ID_PREFIX = "UATMICRO-";
    private static final String UAT_PATIENT_LAST_NAME = "Microbiology";
    private static final String UAT_PATIENT_BIRTH_DATE = "1990-03-13 00:00:00";
    private static final String UAT_MEDIA_NAME = "UAT microbiology blood agar";
    private static final String UAT_AST_CARD_NAME = "UAT microbiology AST card";

    private final MethodService methodService;
    private final SampleService sampleService;
    private final SampleItemService sampleItemService;
    private final PatientService patientService;
    private final PersonService personService;
    private final SampleHumanService sampleHumanService;
    private final TypeOfSampleService typeOfSampleService;
    private final TypeOfSampleTestService typeOfSampleTestService;
    private final LocalizationService localizationService;
    private final TestService testService;
    private final TestSectionService testSectionService;
    private final AnalyteService analyteService;
    private final TestAnalyteService testAnalyteService;
    private final AnalysisService analysisService;
    private final TestResultService testResultService;
    private final IStatusService statusService;
    private final MicrobiologyConfigurationService configurationService;
    private final MicroCaseService caseService;
    private final MicroOrderRoutingService orderRoutingService;
    private final InventoryItemService inventoryItemService;
    private final InventoryLotService inventoryLotService;
    private final InventoryManagementService inventoryManagementService;
    private final TestReagentLinkService testReagentLinkService;
    private final MicrobiologyReferenceAdminService referenceAdminService;
    private final MicroBreakpointAdminService breakpointAdminService;
    private final MicroBreakpointImportService breakpointImportService;

    public MicrobiologyUatScenarioService(MethodService methodService, SampleService sampleService,
            SampleItemService sampleItemService, PatientService patientService, PersonService personService,
            SampleHumanService sampleHumanService, TypeOfSampleService typeOfSampleService,
            TypeOfSampleTestService typeOfSampleTestService, TestService testService,
            TestSectionService testSectionService, LocalizationService localizationService,
            AnalyteService analyteService, TestAnalyteService testAnalyteService, AnalysisService analysisService,
            TestResultService testResultService, IStatusService statusService,
            MicrobiologyConfigurationService configurationService, MicroCaseService caseService,
            MicroOrderRoutingService orderRoutingService, InventoryItemService inventoryItemService,
            InventoryLotService inventoryLotService, InventoryManagementService inventoryManagementService,
            TestReagentLinkService testReagentLinkService, MicrobiologyReferenceAdminService referenceAdminService,
            MicroBreakpointAdminService breakpointAdminService, MicroBreakpointImportService breakpointImportService) {
        this.methodService = methodService;
        this.sampleService = sampleService;
        this.sampleItemService = sampleItemService;
        this.patientService = patientService;
        this.personService = personService;
        this.sampleHumanService = sampleHumanService;
        this.typeOfSampleService = typeOfSampleService;
        this.typeOfSampleTestService = typeOfSampleTestService;
        this.localizationService = localizationService;
        this.testService = testService;
        this.testSectionService = testSectionService;
        this.analyteService = analyteService;
        this.testAnalyteService = testAnalyteService;
        this.analysisService = analysisService;
        this.testResultService = testResultService;
        this.statusService = statusService;
        this.configurationService = configurationService;
        this.caseService = caseService;
        this.orderRoutingService = orderRoutingService;
        this.inventoryItemService = inventoryItemService;
        this.inventoryLotService = inventoryLotService;
        this.inventoryManagementService = inventoryManagementService;
        this.testReagentLinkService = testReagentLinkService;
        this.referenceAdminService = referenceAdminService;
        this.breakpointAdminService = breakpointAdminService;
        this.breakpointImportService = breakpointImportService;
    }

    @Transactional
    public MicrobiologyUatScenarioForm provision(MicrobiologyUatScenarioRequestForm request, String performedBy) {
        String scenario = normalizeScenario(request == null ? null : request.scenario);
        String scenarioKey = normalizeScenarioKey(request == null ? null : request.scenarioKey);
        String suffix = deterministicSuffix(scenarioKey);
        String accessionNumber = "UATMICRO" + suffix;

        Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);
        SampleItem sampleItem;
        if (sample == null) {
            sample = createSample(accessionNumber, performedBy);
            sampleItem = createSampleItem(sample, performedBy);
        } else {
            List<SampleItem> items = sampleItemService.getSampleItemsBySampleId(sample.getId());
            if (items.isEmpty()) {
                sampleItem = createSampleItem(sample, performedBy);
            } else {
                sampleItem = items.get(0);
            }
        }
        Patient patient = getOrCreateUatPatient(suffix, performedBy);
        ensurePatientLink(sample, patient, performedBy);
        AstReferenceData astReferenceData = createAstReferenceData();
        ReferenceAdminData referenceAdminData = REFERENCE_ADMIN_SCENARIO.equals(scenario)
                ? createReferenceAdminData(astReferenceData, performedBy)
                : null;

        ensureSampleType(sampleItem, performedBy);
        Method method = getOrCreateUatMethod(performedBy);
        Test test = getOrCreateUatTest(method, performedBy);
        ensureInventoryTraceability(test, performedBy);
        ensureOrderableSampleTypeMapping(sampleItem.getTypeOfSample(), test, performedBy);
        ensureRemarkTestResult(test, performedBy);
        TestAnalyte reportableTestAnalyte = getOrCreateReportableTestAnalyte(test, performedBy);
        configureCultureSetup(method, reportableTestAnalyte);
        Analysis analysis = getOrCreateAnalysis(test, sampleItem, performedBy);
        MicroCase microCase = routeCultureAnalysis(sampleItem, analysis, performedBy);
        MicroCase sibling = null;
        if (WORKLIST_SCENARIO.equals(scenario)) {
            sibling = caseService.createOrGetCase(sampleItem.getId(), MicroWorkflowType.MYCOBACTERIOLOGY_TB,
                    method.getId(), performedBy);
        }

        MicrobiologyUatScenarioForm form = new MicrobiologyUatScenarioForm();
        form.scenario = scenario;
        form.scenarioKey = scenarioKey;
        form.accessionNumber = accessionNumber;
        form.sampleId = sample.getId();
        form.sampleItemId = sampleItem.getId();
        form.patientId = patient.getId();
        form.caseId = microCase.getId();
        form.siblingCaseId = sibling == null ? null : sibling.getId();
        form.analysisId = analysis.getId();
        form.reportableTestAnalyteId = reportableTestAnalyte.getId();
        form.methodId = method.getId();
        if (referenceAdminData != null) {
            form.organismId = referenceAdminData.organismId();
            form.antibioticId = referenceAdminData.antibioticId();
            form.astPanelId = referenceAdminData.astPanelId();
            form.activeBreakpointStandardId = referenceAdminData.activeStandardId();
            form.loadedBreakpointStandardId = referenceAdminData.loadedStandardId();
        }
        return form;
    }

    private void ensureInventoryTraceability(Test test, String performedBy) {
        InventoryItem media = getOrCreateInventoryItem(UAT_MEDIA_NAME, ItemType.REAGENT, "plate", performedBy);
        InventoryItem astCard = getOrCreateInventoryItem(UAT_AST_CARD_NAME, ItemType.CARTRIDGE, "card", performedBy);
        getOrCreateReagentLink(test, media, "PRIMARY", "plate", performedBy);
        getOrCreateReagentLink(test, astCard, "SECONDARY", "card", performedBy);

        ensureLot(media, "UAT-MICRO-MEDIA-EXPIRED", -1, 10.0, performedBy);
        ensureLot(media, "UAT-MICRO-MEDIA-FEFO", 30, 20.0, performedBy);
        ensureLot(media, "UAT-MICRO-MEDIA-LATER", 90, 20.0, performedBy);
        ensureLot(astCard, "UAT-MICRO-CARD-FEFO", 45, 10.0, performedBy);
        ensureLot(astCard, "UAT-MICRO-CARD-LATER", 120, 10.0, performedBy);
    }

    private InventoryItem getOrCreateInventoryItem(String name, ItemType itemType, String units, String performedBy) {
        InventoryItem item = inventoryItemService.searchByName(name).stream()
                .filter(candidate -> name.equals(candidate.getName())).findFirst().orElse(null);
        if (item == null) {
            item = new InventoryItem();
            item.setFhirUuid(UUID.randomUUID());
            item.setName(name);
            item.setDescription("Property-gated microbiology UAT traceability fixture");
            item.setItemType(itemType);
            item.setCategory("Microbiology UAT");
            item.setUnits(units);
            item.setQuantityPerUnit(1);
            item.setLowStockThreshold(1);
            item.setExpirationAlertDays(30);
            item.setIsActive(IActionConstants.YES);
            item.setSysUserId(performedBy);
            inventoryItemService.insert(item);
        }
        return item;
    }

    private void getOrCreateReagentLink(Test test, InventoryItem item, String role, String unit, String performedBy) {
        TestReagentLink link = testReagentLinkService.getByTestIdAndReagentId(test.getId(), item.getId());
        boolean created = link == null;
        if (created) {
            link = new TestReagentLink();
            link.setTestId(test.getId());
            link.setReagentId(item.getId());
        }
        link.setUsageType(role);
        link.setQuantityPerTest(BigDecimal.ONE);
        link.setQuantityUnit(unit);
        link.setSysUserId(performedBy);
        if (created) {
            testReagentLinkService.insert(link);
        } else {
            testReagentLinkService.update(link);
        }
    }

    private void ensureLot(InventoryItem item, String lotNumber, int expiresInDays, double quantity,
            String performedBy) {
        InventoryLot lot = inventoryLotService.getByLotNumber(lotNumber);
        Timestamp expiration = Timestamp.from(Instant.now().plusSeconds(expiresInDays * 86_400L));
        if (lot == null) {
            lot = new InventoryLot();
            lot.setInventoryItem(item);
            lot.setLotNumber(lotNumber);
            lot.setExpirationDate(expiration);
            lot.setInitialQuantity(quantity);
            lot.setCurrentQuantity(quantity);
            lot.setQcStatus(QCStatus.PASSED);
            lot.setStatus(LotStatus.ACTIVE);
            inventoryManagementService.receiveInventory(lot, performedBy);
            return;
        }
        lot.setExpirationDate(expiration);
        lot.setStatus(LotStatus.ACTIVE);
        lot.setQcStatus(QCStatus.PASSED);
        if (lot.getCurrentQuantity() == null || lot.getCurrentQuantity() < quantity) {
            lot.setCurrentQuantity(quantity);
        }
        lot.setSysUserId(performedBy);
        inventoryLotService.update(lot);
    }

    private Sample createSample(String accessionNumber, String performedBy) {
        Date today = new Date(System.currentTimeMillis());
        Sample sample = new Sample();
        sample.setAccessionNumber(accessionNumber);
        sample.setEnteredDate(today);
        sample.setReceivedTimestamp(Timestamp.from(Instant.now()));
        sample.setStatusId(requireSampleEnteredStatus());
        sample.setSysUserId(performedBy);
        sampleService.insert(sample);
        return sample;
    }

    private SampleItem createSampleItem(Sample sample, String performedBy) {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sample);
        sampleItem.setTypeOfSample(getOrCreateUatSampleType(performedBy));
        sampleItem.setSortOrder("1");
        sampleItem.setStatusId(requireSampleEnteredStatus());
        sampleItem.setSysUserId(performedBy);
        sampleItemService.insert(sampleItem);
        return sampleItem;
    }

    private void ensureSampleType(SampleItem sampleItem, String performedBy) {
        if (sampleItem.getTypeOfSample() != null) {
            return;
        }
        sampleItem.setTypeOfSample(getOrCreateUatSampleType(performedBy));
        sampleItem.setSysUserId(performedBy);
        sampleItemService.update(sampleItem);
    }

    private String requireSampleEnteredStatus() {
        statusService.refreshCache();
        String statusId = statusService.getStatusID(SampleStatus.Entered);
        if ("-1".equals(statusId)) {
            throw new IllegalStateException("SampleStatus.Entered is required for microbiology UAT scenarios");
        }
        return statusId;
    }

    private Patient getOrCreateUatPatient(String suffix, String performedBy) {
        String externalId = UAT_PATIENT_EXTERNAL_ID_PREFIX + suffix;
        Patient patient = patientService.getByExternalId(externalId);
        if (patient != null) {
            boolean changed = false;
            if (patient.getBirthDate() == null) {
                patient.setBirthDate(Timestamp.valueOf(UAT_PATIENT_BIRTH_DATE));
                changed = true;
            }
            if (patient.getGender() == null || patient.getGender().trim().isEmpty()) {
                patient.setGender("F");
                changed = true;
            }
            if (patient.getNationalId() == null || patient.getNationalId().trim().isEmpty()) {
                patient.setNationalId(externalId);
                changed = true;
            }
            if (changed) {
                patient.setSysUserId(performedBy);
                patientService.update(patient);
            }
            return patient;
        }

        Person person = new Person();
        person.setFirstName("UAT");
        person.setLastName(UAT_PATIENT_LAST_NAME);
        person.setSysUserId(performedBy);
        personService.insert(person);

        patient = new Patient();
        patient.setPerson(person);
        patient.setExternalId(externalId);
        patient.setNationalId(externalId);
        patient.setGender("F");
        patient.setBirthDate(Timestamp.valueOf(UAT_PATIENT_BIRTH_DATE));
        patient.setSysUserId(performedBy);
        patientService.insert(patient);
        return patient;
    }

    private void ensurePatientLink(Sample sample, Patient patient, String performedBy) {
        SampleHuman lookup = new SampleHuman();
        lookup.setSampleId(sample.getId());
        SampleHuman sampleHuman = sampleHumanService.getDataBySample(lookup);
        if (sampleHuman == null) {
            sampleHuman = new SampleHuman();
            sampleHuman.setSampleId(sample.getId());
            sampleHuman.setPatientId(patient.getId());
            sampleHuman.setSysUserId(performedBy);
            sampleHumanService.insert(sampleHuman);
            return;
        }
        if (!patient.getId().equals(sampleHuman.getPatientId())) {
            sampleHuman.setPatientId(patient.getId());
            sampleHuman.setSysUserId(performedBy);
            sampleHumanService.update(sampleHuman);
        }
    }

    private AstReferenceData createAstReferenceData() {
        MicroAntibiotic ciprofloxacin = configurationService.getOrCreateAntibiotic("Ciprofloxacin (UAT)", "CIPUAT",
                "Fluoroquinolone");
        MicroAntibiotic gentamicin = configurationService.getOrCreateAntibiotic("Gentamicin (UAT)", "GENUAT",
                "Aminoglycoside");
        MicroAstPanel panel = configurationService.getOrCreateAstPanel("Gram negative AST panel (UAT)", BACTERIOLOGY,
                "GRAM_NEGATIVE");
        configurationService.getOrCreatePanelAntibiotic(panel.getId(), ciprofloxacin.getId(), 1);
        configurationService.getOrCreatePanelAntibiotic(panel.getId(), gentamicin.getId(), 2);

        MicroBreakpointStandard standard = configurationService.getOrCreateBreakpointStandard("CLSI", "2026",
                new Date(System.currentTimeMillis()));
        configurationService.getOrCreateBreakpointRule(micBreakpointRule(standard.getId(), ciprofloxacin.getId()));
        configurationService.getOrCreateBreakpointRule(micBreakpointRule(standard.getId(), gentamicin.getId()));
        return new AstReferenceData(panel, standard);
    }

    private ReferenceAdminData createReferenceAdminData(AstReferenceData astReferenceData, String performedBy) {
        MicroAstPanelAdminForm currentPanel = currentPanel(astReferenceData.panel());
        MicroOrganismAdminForm organism = getOrCreateReferenceOrganism(currentPanel.id, performedBy);
        MicroAntibioticAdminForm antibiotic = getOrCreateReferenceAntibiotic(performedBy);

        breakpointAdminService.activate(astReferenceData.standard().getId(), new Date(System.currentTimeMillis()),
                performedBy);
        String csv = "publisher,version,organism_or_group,antibiotic_whonet_code,method,specimen_type_id,"
                + "breakpoint_type,susceptible_value,intermediate_lower_value,intermediate_upper_value,"
                + "resistant_value,units\n"
                + "CLSI,SYNTH-UAT-LOADED,group:UAT_SYNTHETIC,REFUAT,MIC,,MIC,1,2,2,4,synthetic-mg/L\n";
        MicroBreakpointImportPreviewForm preview = breakpointImportService.preview(csv);
        breakpointImportService.apply(preview.previewToken, performedBy);

        MicroReferenceAdminQueryForm standardQuery = new MicroReferenceAdminQueryForm();
        standardQuery.q = "SYNTH-UAT-LOADED";
        MicroBreakpointStandardAdminForm loaded = breakpointAdminService.getStandards(standardQuery).rows.stream()
                .filter(candidate -> "SYNTH-UAT-LOADED".equals(candidate.version)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Synthetic loaded breakpoint standard was not created"));
        return new ReferenceAdminData(organism.id, antibiotic.id, currentPanel.id, astReferenceData.standard().getId(),
                loaded.id);
    }

    private MicroAstPanelAdminForm currentPanel(MicroAstPanel fallback) {
        MicroReferenceAdminQueryForm query = new MicroReferenceAdminQueryForm();
        query.q = fallback.getName();
        query.pageSize = 100;
        return referenceAdminService.getAstPanels(query).rows.stream()
                .filter(panel -> panel.current && fallback.getName().equals(panel.name)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Current UAT AST panel was not found"));
    }

    private MicroOrganismAdminForm getOrCreateReferenceOrganism(String panelId, String performedBy) {
        MicroReferenceAdminQueryForm query = new MicroReferenceAdminQueryForm();
        query.q = "Reference organism (UAT)";
        MicroReferenceAdminPageForm<MicroOrganismAdminForm> page = referenceAdminService.getOrganisms(query);
        MicroOrganismAdminForm organism = page.rows.stream()
                .filter(candidate -> "REFUAT".equalsIgnoreCase(candidate.whonetCode)).findFirst()
                .orElseGet(MicroOrganismAdminForm::new);
        organism.displayName = "Reference organism (UAT)";
        organism.shortName = "UAT reference";
        organism.whonetCode = "refuat";
        organism.organismGroup = "UAT_SYNTHETIC";
        organism.gramStain = "GRAM_NEGATIVE";
        organism.initialSignificance = "POSSIBLE";
        organism.defaultAstPanelId = panelId;
        organism.notes = "Synthetic UAT reference only; not clinical guidance";
        organism.active = true;
        return referenceAdminService.saveOrganism(organism.id, organism, performedBy);
    }

    private MicroAntibioticAdminForm getOrCreateReferenceAntibiotic(String performedBy) {
        MicroReferenceAdminQueryForm query = new MicroReferenceAdminQueryForm();
        query.q = "REFUAT";
        MicroReferenceAdminPageForm<MicroAntibioticAdminForm> page = referenceAdminService.getAntibiotics(query);
        MicroAntibioticAdminForm antibiotic = page.rows.stream()
                .filter(candidate -> "REFUAT".equalsIgnoreCase(candidate.whonetCode)).findFirst()
                .orElseGet(MicroAntibioticAdminForm::new);
        antibiotic.displayName = "Reference antibiotic (UAT)";
        antibiotic.whonetCode = "REFUAT";
        antibiotic.antibioticClass = "Synthetic UAT";
        antibiotic.route = "BOTH";
        antibiotic.notes = "Synthetic UAT reference only; not clinical guidance";
        antibiotic.active = true;
        return referenceAdminService.saveAntibiotic(antibiotic.id, antibiotic, performedBy);
    }

    private MicroBreakpointRule micBreakpointRule(String standardId, String antibioticId) {
        MicroBreakpointRule rule = new MicroBreakpointRule();
        rule.setStandardId(standardId);
        rule.setAntibioticId(antibioticId);
        rule.setMethod("MIC");
        rule.setBreakpointType("MIC");
        rule.setSusceptibleValue(new BigDecimal("8"));
        rule.setIntermediateLowerValue(new BigDecimal("16"));
        rule.setIntermediateUpperValue(new BigDecimal("16"));
        rule.setResistantValue(new BigDecimal("32"));
        return rule;
    }

    private Method getOrCreateUatMethod(String performedBy) {
        Method method = methodService.getMethods(UAT_METHOD_NAME).stream()
                .filter(candidate -> UAT_METHOD_NAME.equals(candidate.getMethodName())).findFirst().orElse(null);
        if (method == null) {
            method = new Method();
            method.setMethodName(UAT_METHOD_NAME);
            method.setDescription(UAT_METHOD_DESCRIPTION);
            method.setReportingDescription(UAT_METHOD_DESCRIPTION);
            method.setCode("UATMICRO");
            method.setNameKey("method.UAT_micro_culture");
            method.setIsActive(IActionConstants.YES);
            method.setSysUserId(performedBy);
            method.setLocalization(createUatMethodLocalization(performedBy));
            methodService.insert(method);
            return method;
        }
        boolean changed = false;
        if (method.getLocalization() == null) {
            method.setLocalization(createUatMethodLocalization(performedBy));
            changed = true;
        }
        if (!IActionConstants.YES.equals(method.getIsActive())) {
            method.setIsActive(IActionConstants.YES);
            changed = true;
        }
        if (changed) {
            method.setSysUserId(performedBy);
            methodService.update(method);
        }
        return method;
    }

    private Localization createUatMethodLocalization(String performedBy) {
        Localization localization = new Localization();
        localization.setDescription(UAT_METHOD_DESCRIPTION);
        localization.setSysUserId(performedBy);
        List<Locale> activeLocales = localizationService.getAllActiveLocales();
        if (activeLocales.isEmpty()) {
            localization.setEnglish(UAT_METHOD_NAME);
        } else {
            for (Locale locale : activeLocales) {
                localization.setLocalizedValue(locale.getLanguage(), UAT_METHOD_NAME);
            }
        }
        localizationService.insert(localization);
        return localization;
    }

    private Test getOrCreateUatTest(Method method, String performedBy) {
        Test test = testService.getTestByDescription(UAT_TEST_DESCRIPTION);
        if (test == null) {
            test = new Test();
            test.setName(UAT_TEST_DESCRIPTION);
            test.setDescription(UAT_TEST_DESCRIPTION);
            test.setGuid(UUID.nameUUIDFromBytes(UAT_TEST_DESCRIPTION.getBytes(StandardCharsets.UTF_8)).toString());
            test.setDomain("CLINICAL");
            test.setOrderable(true);
            test.setAntimicrobialResistance(true);
        }
        if (test.getLocalizedTestName() == null) {
            test.setLocalizedTestName(
                    createUatTestLocalization("UAT microbiology test name", UAT_TEST_DESCRIPTION, performedBy));
        }
        if (test.getLocalizedReportingName() == null) {
            test.setLocalizedReportingName(createUatTestLocalization("UAT microbiology reporting test name",
                    UAT_TEST_DESCRIPTION, performedBy));
        }
        test.setMethod(method);
        test.setTestSection(getOrCreateUatReportTestSection(performedBy));
        test.setCultureWorkflowType(BACTERIOLOGY);
        test.setIsActive(IActionConstants.YES);
        test.setIsReportable(IActionConstants.YES);
        test.setSysUserId(performedBy);
        if (test.getId() == null) {
            testService.insert(test);
        } else {
            testService.update(test);
        }
        return test;
    }

    private Localization createUatTestLocalization(String description, String displayName, String performedBy) {
        Localization localization = new Localization();
        localization.setDescription(description);
        localization.setSysUserId(performedBy);
        List<Locale> activeLocales = localizationService.getAllActiveLocales();
        if (activeLocales.isEmpty()) {
            localization.setEnglish(displayName);
        } else {
            for (Locale locale : activeLocales) {
                localization.setLocalizedValue(locale.getLanguage(), displayName);
            }
        }
        localizationService.insert(localization);
        return localization;
    }

    TestSection getOrCreateUatReportTestSection(String performedBy) {
        List<TestSection> activeSections = testSectionService.getAllActiveTestSections();
        if (activeSections != null && !activeSections.isEmpty()) {
            return activeSections.get(0);
        }

        TestSection section = testSectionService.getTestSectionByName(UAT_TEST_SECTION_NAME);
        if (section == null) {
            section = new TestSection();
            section.setTestSectionName(UAT_TEST_SECTION_NAME);
            section.setDescription("Service-created microbiology UAT test section");
            section.setLocalization(
                    createUatTestLocalization("UAT microbiology test section", UAT_TEST_SECTION_NAME, performedBy));
            section.setIsExternal(IActionConstants.NO);
            section.setDomain("CLINICAL");
            List<TestSection> allSections = testSectionService.getAllTestSections();
            int nextSortOrder = allSections == null ? 1
                    : allSections.stream().mapToInt(TestSection::getSortOrderInt).max().orElse(0) + 1;
            section.setSortOrderInt(nextSortOrder);
        }
        section.setIsActive(IActionConstants.YES);
        section.setSysUserId(performedBy);
        if (section.getId() == null) {
            String generatedId = testSectionService.insert(section);
            section.setId(generatedId);
        } else {
            testSectionService.update(section);
        }
        return section;
    }

    private TestAnalyte getOrCreateReportableTestAnalyte(Test test, String performedBy) {
        Analyte lookup = new Analyte();
        lookup.setAnalyteName(UAT_ANALYTE_NAME);
        Analyte analyte = analyteService.getAnalyteByName(lookup, true);
        if (analyte == null) {
            analyte = lookup;
            analyte.setIsActive(IActionConstants.YES);
            analyte.setLocalAbbreviation("UATMC");
            analyte.setSysUserId(performedBy);
            analyteService.insert(analyte);
        }
        Analyte reportableAnalyte = analyte;

        TestAnalyte testAnalyte = testAnalyteService.getAllTestAnalytesPerTest(test).stream()
                .filter(candidate -> candidate.getAnalyte() != null
                        && reportableAnalyte.getId().equals(candidate.getAnalyte().getId()))
                .findFirst().orElse(null);
        if (testAnalyte == null) {
            testAnalyte = new TestAnalyte();
            testAnalyte.setTest(test);
            testAnalyte.setAnalyte(reportableAnalyte);
            testAnalyte.setSortOrder("0");
        }
        testAnalyte.setIsReportable(IActionConstants.YES);
        testAnalyte.setSysUserId(performedBy);
        if (testAnalyte.getId() == null) {
            testAnalyteService.insert(testAnalyte);
        } else {
            testAnalyteService.update(testAnalyte);
        }
        return testAnalyte;
    }

    private void ensureRemarkTestResult(Test test, String performedBy) {
        boolean configured = testResultService.getAllActiveTestResultsPerTest(test).stream()
                .anyMatch(testResult -> ResultType.REMARK.matches(testResult.getTestResultType()));
        if (configured) {
            return;
        }
        TestResult testResult = new TestResult();
        testResult.setTest(test);
        testResult.setTestResultType(ResultType.REMARK.getCharacterValue());
        testResult.setIsActive(true);
        testResult.setSortOrder("0");
        testResult.setSysUserId(performedBy);
        testResultService.insert(testResult);
    }

    private void ensureOrderableSampleTypeMapping(TypeOfSample sampleType, Test test, String performedBy) {
        boolean mapped = typeOfSampleTestService.getTypeOfSampleTestsForTest(test.getId()).stream()
                .anyMatch(candidate -> sampleType.getId().equals(candidate.getTypeOfSampleId()));
        if (!mapped) {
            TypeOfSampleTest mapping = new TypeOfSampleTest();
            mapping.setTypeOfSampleId(sampleType.getId());
            mapping.setTestId(test.getId());
            mapping.setSysUserId(performedBy);
            typeOfSampleTestService.insert(mapping);
        }
        typeOfSampleService.clearCache();
    }

    private void configureCultureSetup(Method method, TestAnalyte reportableTestAnalyte) {
        MicroCultureSetup setup = new MicroCultureSetup();
        setup.setMethodId(method.getId());
        setup.setName("UAT bacteriology culture");
        setup.setWorkflowType(BACTERIOLOGY);
        setup.setMediaDefaults("Blood agar");
        setup.setIncubationDefaults("18-24h");
        setup.setAtmosphereDefaults("Ambient");
        setup.setReportableTestAnalyteId(reportableTestAnalyte.getId());
        configurationService.getOrCreateCultureSetup(setup);
    }

    private Analysis getOrCreateAnalysis(Test test, SampleItem sampleItem, String performedBy) {
        Analysis analysis = analysisService.getAnalysisBySampleItemAndTest(sampleItem.getId(), test.getId());
        if (analysis != null) {
            return analysis;
        }
        analysis = analysisService.buildAnalysis(test, sampleItem);
        analysis.setSampleTypeName(UAT_SAMPLE_TYPE_DESCRIPTION);
        analysis.setSysUserId(performedBy);
        analysisService.insert(analysis);
        return analysis;
    }

    private MicroCase routeCultureAnalysis(SampleItem sampleItem, Analysis analysis, String performedBy) {
        return orderRoutingService.routeAnalysesForSampleItem(sampleItem, List.of(analysis), performedBy).stream()
                .filter(candidate -> MicroWorkflowType.BACTERIOLOGY.name().equals(candidate.getWorkflowType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("UAT culture analysis did not route to bacteriology"));
    }

    private TypeOfSample getOrCreateUatSampleType(String performedBy) {
        TypeOfSample sampleType = typeOfSampleService.getAllTypeOfSamples().stream()
                .filter(candidate -> UAT_SAMPLE_TYPE_DESCRIPTION.equals(candidate.getDescription())).findFirst()
                .orElse(null);
        if (sampleType == null) {
            Localization localization = new Localization();
            localization.setDescription("UAT microbiology sample type");
            localization.setEnglish(UAT_SAMPLE_TYPE_DESCRIPTION);
            localization.setSysUserId(performedBy);
            localizationService.insert(localization);

            sampleType = new TypeOfSample();
            sampleType.setDescription(UAT_SAMPLE_TYPE_DESCRIPTION);
            sampleType.setDomain("H");
            sampleType.setLocalAbbreviation("UATMS");
            sampleType.setActive(true);
            sampleType.setSortOrder(999);
            sampleType.setLocalization(localization);
            sampleType.setSysUserId(performedBy);
            typeOfSampleService.insert(sampleType);
            return sampleType;
        }
        if (!sampleType.getIsActive()) {
            sampleType.setActive(true);
            sampleType.setSysUserId(performedBy);
            typeOfSampleService.update(sampleType);
        }
        return sampleType;
    }

    private String normalizeScenario(String scenario) {
        String normalized = scenario == null ? "MVP" : scenario.trim().toUpperCase(Locale.ROOT);
        if (!"CASE".equals(normalized) && !"MVP".equals(normalized) && !WORKLIST_SCENARIO.equals(normalized)
                && !REFERENCE_ADMIN_SCENARIO.equals(normalized)) {
            throw new IllegalArgumentException("scenario must be CASE, MVP, WORKLIST, or M3");
        }
        return normalized;
    }

    private String normalizeScenarioKey(String scenarioKey) {
        if (scenarioKey == null || scenarioKey.trim().isEmpty()) {
            return UUID.randomUUID().toString();
        }
        return scenarioKey.trim();
    }

    private String deterministicSuffix(String scenarioKey) {
        UUID uuid = UUID.nameUUIDFromBytes(scenarioKey.getBytes(StandardCharsets.UTF_8));
        return uuid.toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
    }

    private record AstReferenceData(MicroAstPanel panel, MicroBreakpointStandard standard) {
    }

    private record ReferenceAdminData(String organismId, String antibioticId, String astPanelId,
            String activeStandardId, String loadedStandardId) {
    }
}
