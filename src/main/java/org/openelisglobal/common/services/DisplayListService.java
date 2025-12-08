/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.common.services;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.LocaleChangeListener;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.gender.service.GenderService;
import org.openelisglobal.gender.valueholder.Gender;
import org.openelisglobal.internationalization.GlobalLocaleResolver;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.util.OrganizationTypeList;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panel.valueholder.PanelSortOrderComparator;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.program.valueholder.cytology.CytologyReport;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.cytology.CytologySpecimenAdequacy;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySampleReport;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.qaevent.service.QaEventService;
import org.openelisglobal.qaevent.valueholder.QaEvent;
import org.openelisglobal.referral.service.ReferralReasonService;
import org.openelisglobal.referral.valueholder.ReferralReason;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.statusofsample.service.StatusOfSampleService;
import org.openelisglobal.statusofsample.valueholder.StatusOfSample;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.typeofsample.dao.TypeOfSampleDAO.SampleDomain;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultService;
import org.openelisglobal.typeoftestresult.valueholder.TypeOfTestResult;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.LocaleResolver;

@Service
public class DisplayListService implements LocaleChangeListener {

    private static DisplayListService instance;

    public enum ListType {
        HOURS, MINS, SAMPLE_TYPE_ACTIVE, SAMPLE_TYPE_INACTIVE, SAMPLE_TYPE, INITIAL_SAMPLE_CONDITION,
        SAMPLE_PATIENT_PAYMENT_OPTIONS, PATIENT_HEALTH_REGIONS, PATIENT_MARITAL_STATUS, PATIENT_NATIONALITY,
        PATIENT_EDUCATION, GENDERS, SAMPLE_PATIENT_REFERRING_CLINIC, SAMPLE_PATIENT_CLINIC_DEPARTMENT, QA_EVENTS,
        TEST_SECTION_ACTIVE, TEST_SECTION_INACTIVE, TEST_SECTION_BY_NAME, HAITI_DEPARTMENTS, PATIENT_SEARCH_CRITERIA,
        PANELS, PANELS_ACTIVE, PANELS_INACTIVE, ORDERABLE_TESTS, ALL_TESTS, REJECTION_REASONS, REFERRAL_REASONS,
        REFERRAL_ORGANIZATIONS, TEST_LOCATION_CODE, DICTIONARY_PROGRAM, RESULT_TYPE_LOCALIZED, RESULT_TYPE_RAW,
        UNIT_OF_MEASURE, UNIT_OF_MEASURE_ACTIVE, UNIT_OF_MEASURE_INACTIVE, DICTIONARY_TEST_RESULTS, LAB_COMPONENT,
        SEVERITY_CONSEQUENCES_LIST, SEVERITY_RECURRENCE_LIST, ACTION_TYPE_LIST, LABORATORY_COMPONENT, SAMPLE_NATURE,
        ELECTRONIC_ORDER_STATUSES, METHODS, METHODS_INACTIVE, METHOD_BY_NAME, PRACTITIONER_PERSONS, ORDER_PRIORITY,
        PROGRAM, IMMUNOHISTOCHEMISTRY_STATUS, PATHOLOGY_STATUS, CYTOLOGY_SPECIMEN_ADEQUACY_SATISFACTION,
        PATHOLOGY_TECHNIQUES, PATHOLOGIST_REQUESTS, PATHOLOGY_REQUEST_STATUS, PATHOLOGIST_CONCLUSIONS,
        IMMUNOHISTOCHEMISTRY_REPORT_TYPES, IMMUNOHISTOCHEMISTRY_MARKERS_TESTS, CYTOLOGY_STATUS, NOTEBOOK_STATUS,
        CYTOLOGY_SATISFACTORY_FOR_EVALUATION, CYTOLOGY_UN_SATISFACTORY_FOR_EVALUATION, CYTOLOGY_REPORT_TYPES,
        CYTOLOGY_DIAGNOSIS_RESULT_EPITHELIAL_CELL_SQUAMOUS, CYTOLOGY_DIAGNOSIS_RESULT_EPITHELIAL_CELL_GLANDULAR,
        CYTOLOGY_DIAGNOSIS_RESULT_NON_NEO_PLASTIC_CELLULAR, CYTOLOGY_DIAGNOSIS_RESULT_REACTIVE_CELLULAR,
        NOTEBOOK_EXPT_TYPE, ANALYZER_LIST, CYTOLOGY_DIAGNOSIS_RESULT_ORGANISMS, CYTOLOGY_DIAGNOSIS_RESULT_OTHER,
        TB_ORDER_REASONS, TB_DIAGNOSTIC_REASONS, TB_FOLLOWUP_REASONS, TB_ANALYSIS_METHODS, TB_SAMPLE_ASPECTS,
        TB_FOLLOWUP_LINE1, TB_FOLLOWUP_LINE2, ARV_ORG_LIST, ACTIVE_ORG_LIST, IHC_BREAST_CANCER_REPORT_INTENSITY,
        IHC_BREAST_CANCER_REPORT_CERBB2_PATTERN, IHC_BREAST_CANCER_REPORT_MOLE_SUBTYPE;
    }

    private static Map<ListType, List<IdValuePair>> typeToListMap;
    private static Map<String, List<IdValuePair>> dictionaryToListMap = new HashMap<>();

    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private GenderService genderService;
    @Autowired
    private ReferralReasonService referralReasonService;
    @Autowired
    private PanelService panelService;
    @Autowired
    private TestService testService;
    @Autowired
    private QaEventService qaEventService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private MethodService methodService;
    @Autowired
    private UnitOfMeasureService unitOfMeasureService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private TypeOfTestResultService typeOfTestResultService;
    @Autowired
    private StatusOfSampleService statusOfSampleService;
    @Autowired
    private ProviderService providerService;
    @Autowired
    private ProgramService programService;
    @Autowired
    private LocaleResolver localeResolver;
    @Autowired
    private AnalyzerService analyzerService;

    @PostConstruct
    private void setupGlobalVariables() {
        instance = this;

        refreshLists();
        if (localeResolver instanceof GlobalLocaleResolver) {
            ((GlobalLocaleResolver) localeResolver).addLocalChangeListener(this);
        }

    }

    public static DisplayListService getInstance() {
        return instance;
    }

    private List<IdValuePair> createDictionaryTestResults() {
        List<IdValuePair> testResults = createFromDictionaryCategoryLocalizedSort("CG");
        testResults.addAll(createFromDictionaryCategoryLocalizedSort("HL"));
        testResults.addAll(createFromDictionaryCategoryLocalizedSort("KL"));
        testResults.addAll(createFromDictionaryCategoryLocalizedSort("Test Result"));
        testResults.addAll(createFromDictionaryCategoryLocalizedSort("HIV1NInd"));
        testResults.addAll(createFromDictionaryCategoryLocalizedSort("PosNegIndInv"));
        testResults.addAll(createFromDictionaryCategoryLocalizedSort("HIVResult"));

        Collections.sort(testResults, new Comparator<IdValuePair>() {
            @Override
            public int compare(IdValuePair o1, IdValuePair o2) {
                return o1.getValue().toLowerCase().compareTo(o2.getValue().toLowerCase());
            }
        });
        return testResults;
    }

    @Override
    public void localeChanged(String locale) {
        // refreshes those lists which are dependent on local
        typeToListMap.put(ListType.SAMPLE_TYPE, createTypeOfSampleList());
        typeToListMap.put(ListType.SAMPLE_TYPE_ACTIVE, createSampleTypeList(false));
        typeToListMap.put(ListType.SAMPLE_TYPE_INACTIVE, createSampleTypeList(true));
        typeToListMap.put(ListType.INITIAL_SAMPLE_CONDITION,
                createFromDictionaryCategoryLocalizedSort("specimen reception condition"));
        typeToListMap.put(ListType.SAMPLE_NATURE, createFromDictionaryCategoryLocalizedSort("specimen nature"));
        typeToListMap.put(ListType.PATIENT_HEALTH_REGIONS, createPatientHealthRegions());
        typeToListMap.put(ListType.PATIENT_MARITAL_STATUS,
                createFromDictionaryCategoryLocalizedSort("Marital Status Demographic Information"));
        typeToListMap.put(ListType.PATIENT_NATIONALITY,
                createFromDictionaryCategoryLocalizedSort("Nationality Demographic Information"));
        typeToListMap.put(ListType.PATIENT_EDUCATION,
                createFromDictionaryCategoryLocalizedSort("Education Level Demographic Information"));
        typeToListMap.put(ListType.GENDERS, createGenderList());
        typeToListMap.put(ListType.QA_EVENTS, createSortedQAEvents());
        typeToListMap.put(ListType.TEST_SECTION_ACTIVE, createTestSectionActiveList());
        typeToListMap.put(ListType.TEST_SECTION_INACTIVE, createInactiveTestSection());
        typeToListMap.put(ListType.TEST_SECTION_BY_NAME, createTestSectionByNameList());
        typeToListMap.put(ListType.METHODS, createMethodList());
        typeToListMap.put(ListType.METHODS_INACTIVE, createInactiveMethod());
        typeToListMap.put(ListType.METHOD_BY_NAME, createMethodByNameList());
        typeToListMap.put(ListType.SAMPLE_PATIENT_PAYMENT_OPTIONS,
                createFromDictionaryCategoryLocalizedSort("patientPayment"));
        typeToListMap.put(ListType.PATIENT_SEARCH_CRITERIA, createPatientSearchCriteria());
        typeToListMap.put(ListType.PANELS, createPanelList());
        typeToListMap.put(ListType.PANELS_ACTIVE, createPanelList(false));
        typeToListMap.put(ListType.PANELS_INACTIVE, createPanelList(true));
        testService.localeChanged(locale);
        dictionaryToListMap = new HashMap<>();
        typeToListMap.put(ListType.REJECTION_REASONS, createDictionaryListForCategory("resultRejectionReasons"));
        typeToListMap.put(ListType.REFERRAL_REASONS, createReferralReasonList());

        typeToListMap.put(ListType.ORDERABLE_TESTS, createOrderableTestList());
        typeToListMap.put(ListType.ALL_TESTS, createTestList());
        typeToListMap.put(ListType.IMMUNOHISTOCHEMISTRY_MARKERS_TESTS, createImmunoHistoChemistryTestList());
        typeToListMap.put(ListType.TEST_LOCATION_CODE, createDictionaryListForCategory("testLocationCode"));
        typeToListMap.put(ListType.PROGRAM, createProgramList());
        typeToListMap.put(ListType.DICTIONARY_PROGRAM, createDictionaryListForCategory("programs"));
        typeToListMap.put(ListType.RESULT_TYPE_LOCALIZED, createLocalizedResultTypeList());
        typeToListMap.put(ListType.UNIT_OF_MEASURE, createUOMList());
        typeToListMap.put(ListType.DICTIONARY_TEST_RESULTS, createDictionaryTestResults());
        typeToListMap.put(ListType.SEVERITY_CONSEQUENCES_LIST, createConsequencesList());
        typeToListMap.put(ListType.SEVERITY_RECURRENCE_LIST, createRecurrenceList());
        typeToListMap.put(ListType.ACTION_TYPE_LIST, createActionTypeList());
        typeToListMap.put(ListType.LABORATORY_COMPONENT, createLaboratoryComponentList());
        typeToListMap.put(ListType.ORDER_PRIORITY, createSamplePriorityList());
        typeToListMap.put(ListType.PATHOLOGY_STATUS, createPathologyStatusList());
        typeToListMap.put(ListType.CYTOLOGY_SPECIMEN_ADEQUACY_SATISFACTION,
                createCytologySpecimenAdequacySatisfactionList());
        typeToListMap.put(ListType.CYTOLOGY_STATUS, createCytologyStatusList());
        typeToListMap.put(ListType.NOTEBOOK_STATUS, createNoteBookStatusList());
        typeToListMap.put(ListType.IMMUNOHISTOCHEMISTRY_STATUS, createImmunohistochemistryStatusList());
        typeToListMap.put(ListType.IMMUNOHISTOCHEMISTRY_REPORT_TYPES, createImmunohistochemistryReportTypeList());
        typeToListMap.put(ListType.CYTOLOGY_REPORT_TYPES, createCytologyReportTypeList());
        typeToListMap.put(ListType.PATHOLOGY_REQUEST_STATUS, createPathologyRequestStatusList());
        typeToListMap.put(ListType.PATHOLOGY_TECHNIQUES, createDictionaryListForCategory("pathology_techniques"));
        typeToListMap.put(ListType.IHC_BREAST_CANCER_REPORT_INTENSITY,
                createDictionaryListForCategory("ihc_breast_cancer_report_intensity"));
        typeToListMap.put(ListType.IHC_BREAST_CANCER_REPORT_CERBB2_PATTERN,
                createDictionaryListForCategory("ihc_breast_cancer_report_cerbb2_pattern"));
        typeToListMap.put(ListType.IHC_BREAST_CANCER_REPORT_MOLE_SUBTYPE,
                createDictionaryListForCategory("ihc_breast_cancer_report_molecular_subtype"));
        typeToListMap.put(ListType.CYTOLOGY_SATISFACTORY_FOR_EVALUATION,
                createDictionaryListForCategory("cytology_adequacy_satisfactory"));
        typeToListMap.put(ListType.CYTOLOGY_UN_SATISFACTORY_FOR_EVALUATION,
                createDictionaryListForCategory("cytology_adequacy_unsatisfactory"));
        typeToListMap.put(ListType.PATHOLOGIST_REQUESTS, createDictionaryListForCategory("pathologist_requests"));
        typeToListMap.put(ListType.PATHOLOGIST_CONCLUSIONS, createDictionaryListForCategory("pathologist_conclusions"));
        typeToListMap.put(ListType.CYTOLOGY_DIAGNOSIS_RESULT_EPITHELIAL_CELL_SQUAMOUS,
                createDictionaryListForCategory("cytology_epithelial_cell_abnomalit_squamous"));
        typeToListMap.put(ListType.CYTOLOGY_DIAGNOSIS_RESULT_EPITHELIAL_CELL_GLANDULAR,
                createDictionaryListForCategory("cytology_epithelial_cell_abnomalit_glandular"));
        typeToListMap.put(ListType.CYTOLOGY_DIAGNOSIS_RESULT_NON_NEO_PLASTIC_CELLULAR,
                createDictionaryListForCategory("cytology_non-neoplastic_cellular_variations"));
        typeToListMap.put(ListType.CYTOLOGY_DIAGNOSIS_RESULT_REACTIVE_CELLULAR,
                createDictionaryListForCategory("cytology_reactive_cellular_changes"));
        typeToListMap.put(ListType.NOTEBOOK_EXPT_TYPE, createDictionaryListForCategory("notebook_experiment_type"));
        typeToListMap.put(ListType.ANALYZER_LIST, createAnalyzerList());
        typeToListMap.put(ListType.CYTOLOGY_DIAGNOSIS_RESULT_ORGANISMS,
                createDictionaryListForCategory("cytology_diagnosis_organisms"));
        typeToListMap.put(ListType.CYTOLOGY_DIAGNOSIS_RESULT_OTHER,
                createDictionaryListForCategory("cytology_diagnosis_other"));
        typeToListMap.put(ListType.TB_ORDER_REASONS, createDictionaryListForCategory("TB Order Reasons"));
        typeToListMap.put(ListType.TB_DIAGNOSTIC_REASONS, createDictionaryListForCategory("TB Diagnostic Reasons"));
        typeToListMap.put(ListType.TB_FOLLOWUP_REASONS, createDictionaryListForCategory("TB Followup Reasons"));
        typeToListMap.put(ListType.TB_ANALYSIS_METHODS, createDictionaryListForCategory("TB Analysis Methods"));
        typeToListMap.put(ListType.TB_SAMPLE_ASPECTS, createDictionaryListForCategory("TB Sample Aspects"));
        typeToListMap.put(ListType.TB_FOLLOWUP_LINE1, createTBFollowupLine1List());
        typeToListMap.put(ListType.TB_FOLLOWUP_LINE2, createTBFollowupLine2List());
        typeToListMap.put(ListType.ARV_ORG_LIST, createArvOrgList());
        typeToListMap.put(ListType.ACTIVE_ORG_LIST, createActiveOrganizationsList());
    }

    private List<IdValuePair> createPathologyStatusList() {
        return Arrays.asList(PathologySample.PathologyStatus.values()).stream()
                .map(e -> new IdValuePair(e.name(), e.getDisplay())).collect(Collectors.toList());
    }

    private List<IdValuePair> createCytologyStatusList() {
        return Arrays.asList(CytologySample.CytologyStatus.values()).stream()
                .map(e -> new IdValuePair(e.name(), e.getDisplay())).collect(Collectors.toList());
    }

    private List<IdValuePair> createNoteBookStatusList() {
        return Arrays.asList(NoteBook.NoteBookStatus.values()).stream()
                .map(e -> new IdValuePair(e.name(), e.getDisplay())).collect(Collectors.toList());
    }

    private List<IdValuePair> createImmunohistochemistryStatusList() {
        return Arrays.asList(ImmunohistochemistrySample.ImmunohistochemistryStatus.values()).stream()
                .map(e -> new IdValuePair(e.name(), e.getDisplay())).collect(Collectors.toList());
    }

    private List<IdValuePair> createCytologySpecimenAdequacySatisfactionList() {
        return Arrays.asList(CytologySpecimenAdequacy.SpecimenAdequancySatisfaction.values()).stream()
                .map(e -> new IdValuePair(e.name(), e.getDisplay())).collect(Collectors.toList());
    }

    private List<IdValuePair> createImmunohistochemistryReportTypeList() {
        return Arrays.asList(ImmunohistochemistrySampleReport.ImmunoHistologyReportType.values()).stream()
                .map(e -> new IdValuePair(e.name(), e.getDisplay())).collect(Collectors.toList());
    }

    private List<IdValuePair> createCytologyReportTypeList() {
        return Arrays.asList(CytologyReport.CytologyReportType.values()).stream()
                .map(e -> new IdValuePair(e.name(), e.getDisplay())).collect(Collectors.toList());
    }

    private List<IdValuePair> createPathologyRequestStatusList() {
        return Arrays.asList(PathologyRequest.RequestStatus.values()).stream()
                .map(e -> new IdValuePair(e.name(), e.getDisplay())).collect(Collectors.toList());
    }

    public List<IdValuePair> getList(ListType listType) {
        return typeToListMap.get(listType);
    }

    public List<IdValuePair> getListWithLeadingBlank(ListType listType) {
        List<IdValuePair> list = new ArrayList<>();
        list.add(new IdValuePair("0", ""));
        list.addAll(getList(listType));
        return list;
    }

    public List<IdValuePair> getNumberedList(ListType listType) {
        return addNumberingToDisplayList(getList(listType));
    }

    public List<IdValuePair> getNumberedListWithLeadingBlank(ListType listType) {
        List<IdValuePair> list = new ArrayList<>();
        list.add(new IdValuePair("0", ""));
        list.addAll(getNumberedList(listType));
        return list;
    }

    public List<IdValuePair> getDictionaryListByCategory(String category) {
        List<IdValuePair> list = dictionaryToListMap.get(category);
        if (list == null) {
            list = createDictionaryListForCategory(category);
            if (!list.isEmpty()) {
                dictionaryToListMap.put(category, list);
            }
        }

        return list;
    }

    private List<IdValuePair> createUOMList() {
        List<IdValuePair> list = new ArrayList<>();
        List<UnitOfMeasure> uomList = unitOfMeasureService.getAll();
        for (UnitOfMeasure uom : uomList) {
            list.add(new IdValuePair(uom.getId(), uom.getLocalizedName()));
        }

        return list;
    }

    private List<IdValuePair> createProgramList() {
        List<IdValuePair> list = new ArrayList<>();
        List<Program> programList = programService.getAll();
        for (Program program : programList) {
            list.add(new IdValuePair(program.getId(), program.getProgramName()));
        }
        return list;
    }

    private List<IdValuePair> createElectronicOrderStatusList() {
        List<IdValuePair> list = new ArrayList<>();
        List<StatusOfSample> statusList = statusOfSampleService.getAllStatusOfSamples();

        for (StatusOfSample status : statusList) {
            if (status.getStatusType().equals("EXTERNAL_ORDER")) {
                list.add(new IdValuePair(status.getId(), status.getDefaultLocalizedName()));
            }
        }
        return list;
    }

    private List<IdValuePair> createLocalizedResultTypeList() {
        List<IdValuePair> typeList = new ArrayList<>();

        List<TypeOfTestResult> typeOfTestResultList = typeOfTestResultService.getAll();
        for (TypeOfTestResult typeOfTestResult : typeOfTestResultList) {
            String description = typeOfTestResult.getDescription();
            if ("Dictionary".equals(description)) {
                typeList.add(new IdValuePair(typeOfTestResult.getId(), MessageUtil.getMessage("result.type.select")));
            } else if ("Numeric".equals(description)) {
                typeList.add(new IdValuePair(typeOfTestResult.getId(), MessageUtil.getMessage("result.type.numeric")));
            } else if ("Remark".equals(description)) {
                typeList.add(new IdValuePair(typeOfTestResult.getId(), MessageUtil.getMessage("result.type.freeText")));
            } else if ("Alpha,no range check".equals(description)) {
                typeList.add(new IdValuePair(typeOfTestResult.getId(), MessageUtil.getMessage("result.type.alpha")));
            } else if ("Multiselect".equals(description)) {
                typeList.add(
                        new IdValuePair(typeOfTestResult.getId(), MessageUtil.getMessage("result.type.multiselect")));
            } else if ("Cascading Multiselect".equals(description)) {
                typeList.add(
                        new IdValuePair(typeOfTestResult.getId(), MessageUtil.getMessage("result.type.cascading")));
            }
        }

        return typeList;
    }

    private List<IdValuePair> createRawResultTypeList() {
        List<IdValuePair> typeList = new ArrayList<>();

        List<TypeOfTestResult> typeOfTestResultList = typeOfTestResultService.getAll();
        for (TypeOfTestResult typeOfTestResult : typeOfTestResultList) {
            typeList.add(new IdValuePair(typeOfTestResult.getId(), typeOfTestResult.getDescription()));
        }

        return typeList;
    }

    private List<IdValuePair> createDictionaryListForCategory(String category) {
        List<IdValuePair> list = new ArrayList<>();
        List<Dictionary> dictionaryList = dictionaryService.getDictionaryEntrysByCategoryAbbreviation("categoryName",
                category, false);
        for (Dictionary dictionary : dictionaryList) {
            list.add(new IdValuePair(dictionary.getId(), dictionary.getLocalizedName()));
        }

        return list;
    }

    private List<IdValuePair> createFromDictionaryCategoryLocalizedSort(String category) {
        List<IdValuePair> dictionaryList = new ArrayList<>();

        List<Dictionary> dictionaries = dictionaryService.getDictionaryEntrysByCategoryNameLocalizedSort(category);
        for (Dictionary dictionary : dictionaries) {
            dictionaryList.add(new IdValuePair(dictionary.getId(), dictionary.getLocalizedName()));
        }

        return dictionaryList;
    }

    public List<IdValuePair> getFreshList(ListType listType) {
        refreshList(listType);
        return typeToListMap.get(listType);
    }

    public synchronized void refreshLists() {
        typeToListMap = new HashMap<>();
        typeToListMap.put(ListType.NOTEBOOK_STATUS, createNoteBookStatusList());
        typeToListMap.put(ListType.CYTOLOGY_STATUS, createCytologyStatusList());
        typeToListMap.put(ListType.PATHOLOGY_STATUS, createPathologyStatusList());
        typeToListMap.put(ListType.CYTOLOGY_SPECIMEN_ADEQUACY_SATISFACTION,
                createCytologySpecimenAdequacySatisfactionList());
        typeToListMap.put(ListType.IMMUNOHISTOCHEMISTRY_STATUS, createImmunohistochemistryStatusList());
        typeToListMap.put(ListType.IMMUNOHISTOCHEMISTRY_REPORT_TYPES, createImmunohistochemistryReportTypeList());
        typeToListMap.put(ListType.CYTOLOGY_REPORT_TYPES, createCytologyReportTypeList());
        typeToListMap.put(ListType.PATHOLOGY_REQUEST_STATUS, createPathologyRequestStatusList());
        typeToListMap.put(ListType.HOURS, createHourList());
        typeToListMap.put(ListType.MINS, createMinList());
        typeToListMap.put(ListType.SAMPLE_TYPE, createTypeOfSampleList());
        typeToListMap.put(ListType.SAMPLE_TYPE_ACTIVE, createSampleTypeList(false));
        typeToListMap.put(ListType.SAMPLE_TYPE_INACTIVE, createSampleTypeList(true));
        typeToListMap.put(ListType.INITIAL_SAMPLE_CONDITION,
                createFromDictionaryCategoryLocalizedSort("specimen reception condition"));
        typeToListMap.put(ListType.SAMPLE_NATURE, createFromDictionaryCategoryLocalizedSort("specimen nature"));
        typeToListMap.put(ListType.PATIENT_HEALTH_REGIONS, createPatientHealthRegions());
        typeToListMap.put(ListType.PATIENT_MARITAL_STATUS,
                createFromDictionaryCategoryLocalizedSort("Marital Status Demographic Information"));
        typeToListMap.put(ListType.PATIENT_NATIONALITY,
                createFromDictionaryCategoryLocalizedSort("Nationality Demographic Information"));
        typeToListMap.put(ListType.PATIENT_EDUCATION,
                createFromDictionaryCategoryLocalizedSort("Education Level Demographic Information"));
        typeToListMap.put(ListType.GENDERS, createGenderList());
        typeToListMap.put(ListType.SAMPLE_PATIENT_REFERRING_CLINIC, createReferringClinicList());
        typeToListMap.put(ListType.QA_EVENTS, createSortedQAEvents());
        typeToListMap.put(ListType.TEST_SECTION_ACTIVE, createTestSectionActiveList());
        typeToListMap.put(ListType.METHODS, createMethodList());
        typeToListMap.put(ListType.METHODS_INACTIVE, createInactiveMethod());
        typeToListMap.put(ListType.METHOD_BY_NAME, createMethodByNameList());
        typeToListMap.put(ListType.TEST_SECTION_INACTIVE, createInactiveTestSection());
        typeToListMap.put(ListType.TEST_SECTION_BY_NAME, createTestSectionByNameList());
        typeToListMap.put(ListType.HAITI_DEPARTMENTS, createAddressDepartmentList());
        typeToListMap.put(ListType.SAMPLE_PATIENT_PAYMENT_OPTIONS,
                createFromDictionaryCategoryLocalizedSort("patientPayment"));
        typeToListMap.put(ListType.PATIENT_SEARCH_CRITERIA, createPatientSearchCriteria());
        typeToListMap.put(ListType.PANELS, createPanelList());
        typeToListMap.put(ListType.PANELS_ACTIVE, createPanelList(false));
        typeToListMap.put(ListType.PANELS_INACTIVE, createPanelList(true));
        typeToListMap.put(ListType.ORDERABLE_TESTS, createOrderableTestList());
        typeToListMap.put(ListType.ALL_TESTS, createTestList());
        typeToListMap.put(ListType.IMMUNOHISTOCHEMISTRY_MARKERS_TESTS, createImmunoHistoChemistryTestList());
        typeToListMap.put(ListType.REJECTION_REASONS, createDictionaryListForCategory("resultRejectionReasons"));
        typeToListMap.put(ListType.REFERRAL_REASONS, createReferralReasonList());
        typeToListMap.put(ListType.REFERRAL_ORGANIZATIONS, createReferralOrganizationList());
        typeToListMap.put(ListType.TEST_LOCATION_CODE, createDictionaryListForCategory("testLocationCode"));
        typeToListMap.put(ListType.PROGRAM, createProgramList());
        typeToListMap.put(ListType.DICTIONARY_PROGRAM, createDictionaryListForCategory("programs"));
        typeToListMap.put(ListType.RESULT_TYPE_LOCALIZED, createLocalizedResultTypeList());
        typeToListMap.put(ListType.RESULT_TYPE_RAW, createRawResultTypeList());
        typeToListMap.put(ListType.UNIT_OF_MEASURE, createUOMList());
        typeToListMap.put(ListType.UNIT_OF_MEASURE_ACTIVE, createUOMList());
        typeToListMap.put(ListType.UNIT_OF_MEASURE_INACTIVE, createUOMList());
        typeToListMap.put(ListType.DICTIONARY_TEST_RESULTS, createDictionaryTestResults());
        typeToListMap.put(ListType.SEVERITY_CONSEQUENCES_LIST, createConsequencesList());
        typeToListMap.put(ListType.SEVERITY_RECURRENCE_LIST, createRecurrenceList());
        typeToListMap.put(ListType.ACTION_TYPE_LIST, createActionTypeList());
        typeToListMap.put(ListType.LABORATORY_COMPONENT, createLaboratoryComponentList());
        typeToListMap.put(ListType.ELECTRONIC_ORDER_STATUSES, createElectronicOrderStatusList());
        typeToListMap.put(ListType.PRACTITIONER_PERSONS, createActivePractitionerPersonsList());
        typeToListMap.put(ListType.ORDER_PRIORITY, createSamplePriorityList());
        typeToListMap.put(ListType.IHC_BREAST_CANCER_REPORT_INTENSITY,
                createDictionaryListForCategory("ihc_breast_cancer_report_intensity"));
        typeToListMap.put(ListType.IHC_BREAST_CANCER_REPORT_CERBB2_PATTERN,
                createDictionaryListForCategory("ihc_breast_cancer_report_cerbb2_pattern"));
        typeToListMap.put(ListType.IHC_BREAST_CANCER_REPORT_MOLE_SUBTYPE,
                createDictionaryListForCategory("ihc_breast_cancer_report_molecular_subtype"));
        typeToListMap.put(ListType.PATHOLOGY_TECHNIQUES, createDictionaryListForCategory("pathology_techniques"));
        typeToListMap.put(ListType.PATHOLOGIST_REQUESTS, createDictionaryListForCategory("pathologist_requests"));
        typeToListMap.put(ListType.PATHOLOGIST_CONCLUSIONS, createDictionaryListForCategory("pathologist_conclusions"));
        typeToListMap.put(ListType.CYTOLOGY_SATISFACTORY_FOR_EVALUATION,
                createDictionaryListForCategory("cytology_adequacy_satisfactory"));
        typeToListMap.put(ListType.CYTOLOGY_UN_SATISFACTORY_FOR_EVALUATION,
                createDictionaryListForCategory("cytology_adequacy_unsatisfactory"));
        typeToListMap.put(ListType.CYTOLOGY_DIAGNOSIS_RESULT_EPITHELIAL_CELL_SQUAMOUS,
                createDictionaryListForCategory("cytology_epithelial_cell_abnomalit_squamous"));
        typeToListMap.put(ListType.CYTOLOGY_DIAGNOSIS_RESULT_EPITHELIAL_CELL_GLANDULAR,
                createDictionaryListForCategory("cytology_epithelial_cell_abnomalit_glandular"));
        typeToListMap.put(ListType.CYTOLOGY_DIAGNOSIS_RESULT_NON_NEO_PLASTIC_CELLULAR,
                createDictionaryListForCategory("cytology_non-neoplastic_cellular_variations"));
        typeToListMap.put(ListType.CYTOLOGY_DIAGNOSIS_RESULT_REACTIVE_CELLULAR,
                createDictionaryListForCategory("cytology_reactive_cellular_changes"));
        typeToListMap.put(ListType.NOTEBOOK_EXPT_TYPE, createDictionaryListForCategory("notebook_experiment_type"));
        typeToListMap.put(ListType.ANALYZER_LIST, createAnalyzerList());
        typeToListMap.put(ListType.CYTOLOGY_DIAGNOSIS_RESULT_ORGANISMS,
                createDictionaryListForCategory("cytology_diagnosis_organisms"));
        typeToListMap.put(ListType.CYTOLOGY_DIAGNOSIS_RESULT_OTHER,
                createDictionaryListForCategory("cytology_diagnosis_other"));
        typeToListMap.put(ListType.TB_ORDER_REASONS, createDictionaryListForCategory("TB Order Reasons"));
        typeToListMap.put(ListType.TB_DIAGNOSTIC_REASONS, createDictionaryListForCategory("TB Diagnostic Reasons"));
        typeToListMap.put(ListType.TB_FOLLOWUP_REASONS, createDictionaryListForCategory("TB Followup Reasons"));
        typeToListMap.put(ListType.TB_ANALYSIS_METHODS, createDictionaryListForCategory("TB Analysis Methods"));
        typeToListMap.put(ListType.TB_SAMPLE_ASPECTS, createDictionaryListForCategory("TB Sample Aspects"));
        typeToListMap.put(ListType.TB_FOLLOWUP_LINE1, createTBFollowupLine1List());
        typeToListMap.put(ListType.TB_FOLLOWUP_LINE2, createTBFollowupLine2List());
        typeToListMap.put(ListType.ARV_ORG_LIST, createArvOrgList());
        typeToListMap.put(ListType.ACTIVE_ORG_LIST, createActiveOrganizationsList());
    }

    public void refreshList(ListType listType) {

        switch (listType) {
        case ORDER_PRIORITY: {
            typeToListMap.put(ListType.ORDER_PRIORITY, createSamplePriorityList());
            break;
        }
        case PRACTITIONER_PERSONS: {
            typeToListMap.put(ListType.PRACTITIONER_PERSONS, createActivePractitionerPersonsList());
            break;
        }
        case SAMPLE_PATIENT_REFERRING_CLINIC: {
            typeToListMap.put(ListType.SAMPLE_PATIENT_REFERRING_CLINIC, createReferringClinicList());
            break;
        }
        case ALL_TESTS: {
            testService.refreshTestNames();
            typeToListMap.put(ListType.ALL_TESTS, createTestList());
            break;
        }
        case IMMUNOHISTOCHEMISTRY_MARKERS_TESTS: {
            testService.refreshTestNames();
            typeToListMap.put(ListType.IMMUNOHISTOCHEMISTRY_MARKERS_TESTS, createImmunoHistoChemistryTestList());
            break;
        }
        case ORDERABLE_TESTS: {
            testService.refreshTestNames();
            typeToListMap.put(ListType.ORDERABLE_TESTS, createOrderableTestList());
            break;
        }
        case SAMPLE_TYPE: {
            typeToListMap.put(ListType.SAMPLE_TYPE, createTypeOfSampleList());
            break;
        }
        case SAMPLE_TYPE_ACTIVE: {
            typeToListMap.put(ListType.SAMPLE_TYPE_ACTIVE, createSampleTypeList(false));
            break;
        }
        case SAMPLE_TYPE_INACTIVE: {
            typeToListMap.put(ListType.SAMPLE_TYPE_INACTIVE, createSampleTypeList(true));
            break;
        }
        case TEST_SECTION_ACTIVE: {
            testSectionService.refreshNames();
            typeToListMap.put(ListType.TEST_SECTION_ACTIVE, createTestSectionActiveList());
            break;
        }
        case METHODS: {
            methodService.refreshNames();
            typeToListMap.put(ListType.METHODS, createMethodList());
            break;
        }
        case METHODS_INACTIVE: {
            methodService.refreshNames();
            typeToListMap.put(ListType.METHODS_INACTIVE, createInactiveMethod());
            break;
        }
        case TEST_SECTION_INACTIVE: {
            testSectionService.refreshNames();
            typeToListMap.put(ListType.TEST_SECTION_INACTIVE, createInactiveTestSection());
            break;
        }
        case REFERRAL_ORGANIZATIONS: {
            typeToListMap.put(ListType.REFERRAL_ORGANIZATIONS, createReferralOrganizationList());
            break;
        }
        case PANELS: {
            typeToListMap.put(ListType.PANELS, createPanelList());
            break;
        }
        case PANELS_ACTIVE: {
            typeToListMap.put(ListType.PANELS_ACTIVE, createPanelList(false));
            break;
        }
        case PANELS_INACTIVE: {
            typeToListMap.put(ListType.PANELS_INACTIVE, createPanelList(true));
            break;
        }
        case UNIT_OF_MEASURE: {
            unitOfMeasureService.refreshNames();
            typeToListMap.put(ListType.UNIT_OF_MEASURE, createUnitOfMeasureList());
            break;
        }
        case PATIENT_HEALTH_REGIONS: {
            typeToListMap.put(ListType.PATIENT_HEALTH_REGIONS, createPatientHealthRegions());
            break;
        }
        case PROGRAM: {
            typeToListMap.put(ListType.PROGRAM, createProgramList());
        }
        case DICTIONARY_TEST_RESULTS: {
            typeToListMap.put(ListType.DICTIONARY_TEST_RESULTS, createDictionaryTestResults());
        }
        case ARV_ORG_LIST: {
            typeToListMap.put(ListType.ARV_ORG_LIST, createArvOrgList());
        }
        case ACTIVE_ORG_LIST: {
            typeToListMap.put(ListType.ACTIVE_ORG_LIST, createActiveOrganizationsList());
        }
        }
    }

    private List<IdValuePair> createActivePractitionerPersonsList() {
        List<IdValuePair> providerDisplayList = new ArrayList<>();

        List<Provider> providerList = providerService.getAllActiveProviders();
        providerList.sort((e, f) -> {
            String lastNameE = e.getPerson().getLastName();
            String lastNameF = f.getPerson().getLastName();

            if (lastNameE == null && lastNameF == null)
                return 0;
            if (lastNameE == null)
                return 1; // Put null values at the end
            if (lastNameF == null)
                return -1; // Put null values at the end

            return lastNameE.compareTo(lastNameF);
        });

        for (Provider provider : providerList) {
            providerDisplayList.add(new IdValuePair(provider.getPerson().getId(),
                    provider.getPerson().getLastName() + ", " + provider.getPerson().getFirstName()));
        }

        return providerDisplayList;
    }

    private List<IdValuePair> createReferringClinicList() {
        List<IdValuePair> requesterList = new ArrayList<>();

        List<Organization> orgList = organizationService.getOrganizationsByTypeName("shortName",
                RequesterService.REFERRAL_ORG_TYPE);
        orgList.sort((e, f) -> {
            return e.getOrganizationName().compareTo(f.getOrganizationName());
        });

        for (Organization organization : orgList) {
            if (GenericValidator.isBlankOrNull(organization.getShortName())) {
                requesterList.add(new IdValuePair(organization.getId(), organization.getOrganizationName()));
            } else {
                requesterList.add(new IdValuePair(organization.getId(),
                        organization.getShortName() + " - " + organization.getOrganizationName()));
            }
        }

        return requesterList;
    }

    private List<IdValuePair> createActiveOrganizationsList() {
        List<IdValuePair> activeOrgList = new ArrayList<>();

        List<Organization> orgList = organizationService.getActiveOrganizations();

        for (Organization organization : orgList) {

            activeOrgList.add(new IdValuePair(organization.getId(), organization.getOrganizationName()));
        }
        return activeOrgList;
    }

    private List<IdValuePair> createArvOrgList() {
        List<IdValuePair> requesterList = new ArrayList<>();

        List<Organization> orgList = OrganizationTypeList.ARV_ORGS.getList();
        orgList.sort((e, f) -> {
            return e.getOrganizationName().compareTo(f.getOrganizationName());
        });

        for (Organization organization : orgList) {
            if (GenericValidator.isBlankOrNull(organization.getShortName())) {
                requesterList.add(new IdValuePair(organization.getId(), organization.getOrganizationName()));
            } else {
                requesterList.add(new IdValuePair(organization.getId(),
                        organization.getShortName() + " - " + organization.getOrganizationName()));
            }
        }

        return requesterList;
    }

    private List<IdValuePair> createGenderList() {
        List<IdValuePair> genders = new ArrayList<>();

        List<Gender> genderList = genderService.getAll();

        for (Gender gender : genderList) {
            genders.add(new IdValuePair(gender.getGenderType(), MessageUtil.getContextualMessage(gender.getNameKey())));
        }
        return genders;
    }

    private List<IdValuePair> createReferralReasonList() {
        List<IdValuePair> referralReasons = new ArrayList<>();
        List<ReferralReason> reasonList = referralReasonService.getAllReferralReasons();

        for (ReferralReason reason : reasonList) {
            referralReasons.add(new IdValuePair(reason.getId(), reason.getLocalizedName()));
        }

        return referralReasons;
    }

    private List<IdValuePair> createReferralOrganizationList() {
        List<IdValuePair> pairs = new ArrayList<>();

        List<Organization> orgs = organizationService.getOrganizationsByTypeName("organizationName", "referralLab");
        orgs.sort((e, f) -> {
            return e.getOrganizationName().compareTo(f.getOrganizationName());
        });
        for (Organization org : orgs) {
            pairs.add(new IdValuePair(org.getId(), org.getOrganizationName()));
        }

        return pairs;
    }

    private List<IdValuePair> createPanelList() {
        ArrayList<IdValuePair> panels = new ArrayList<>();

        List<Panel> panelList = panelService.getAllPanels();

        Collections.sort(panelList, PanelSortOrderComparator.SORT_ORDER_COMPARATOR);
        for (Panel panel : panelList) {
            panels.add(new IdValuePair(panel.getId(), panel.getLocalizedName()));
        }

        return panels;
    }

    private List<IdValuePair> createOrderableTestList() {
        ArrayList<IdValuePair> tests = new ArrayList<>();

        List<Test> testList = testService.getAllActiveOrderableTests();
        for (Test test : testList) {
            tests.add(new IdValuePair(test.getId(), TestServiceImpl.getLocalizedTestNameWithType(test)));
        }

        Collections.sort(tests, new Comparator<IdValuePair>() {
            @Override
            public int compare(IdValuePair o1, IdValuePair o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        return tests;
    }

    private List<IdValuePair> createTestList() {
        ArrayList<IdValuePair> tests = new ArrayList<>();

        List<Test> testList = testService.getAllActiveTests(false);
        for (Test test : testList) {
            tests.add(new IdValuePair(test.getId(), TestServiceImpl.getLocalizedTestNameWithType(test)));

            Collections.sort(tests, new Comparator<IdValuePair>() {
                @Override
                public int compare(IdValuePair o1, IdValuePair o2) {
                    return o1.getValue().compareTo(o2.getValue());
                }
            });
        }

        return tests;
    }

    private List<IdValuePair> createImmunoHistoChemistryTestList() {
        ArrayList<IdValuePair> tests = new ArrayList<>();
        String id = testSectionService.getTestSectionByName("Immunohistochemistry").getId();
        if (id == null) {
            return tests;
        }
        List<Test> testList = testService.getTestsByTestSectionId(id);
        for (Test test : testList) {
            tests.add(new IdValuePair(test.getId(), TestServiceImpl.getLocalizedTestNameWithType(test)));

            Collections.sort(tests, new Comparator<IdValuePair>() {

                @Override
                public int compare(IdValuePair o1, IdValuePair o2) {
                    return o1.getValue().compareTo(o2.getValue());
                }
            });
        }
        return tests;
    }

    private List<IdValuePair> createPatientHealthRegions() {
        List<IdValuePair> regionList = new ArrayList<>();
        List<Organization> orgList = organizationService.getOrganizationsByTypeName("id", "Health Region");
        orgList.sort((e, f) -> {
            return e.getOrganizationName().compareTo(f.getOrganizationName());
        });
        for (Organization org : orgList) {
            regionList.add(new IdValuePair(org.getId(), org.getOrganizationName()));
        }
        return regionList;
    }

    public List<IdValuePair> addNumberingToDisplayList(List<IdValuePair> displayList) {
        List<IdValuePair> numberedList = new ArrayList<>(displayList.size());
        int cnt = 1;
        for (IdValuePair pair : displayList) {
            numberedList.add(new IdValuePair(pair.getId(), cnt++ + ". " + pair.getValue()));
        }

        return numberedList;
    }

    private List<IdValuePair> createSampleTypeList(boolean inactiveTypes) {
        List<TypeOfSample> list = typeOfSampleService.getTypesForDomainBySortOrder(SampleDomain.HUMAN);

        List<IdValuePair> filteredList = new ArrayList<>();

        for (TypeOfSample type : list) {
            if ((!inactiveTypes && type.isActive()) || (inactiveTypes && !type.isActive())) {
                filteredList.add(new IdValuePair(type.getId(), type.getLocalizedName()));
            }
        }

        return filteredList;
    }

    private List<IdValuePair> createPanelList(boolean inactiveTypes) {
        List<Panel> list = panelService.getAllPanels();
        Collections.sort(list, PanelSortOrderComparator.SORT_ORDER_COMPARATOR);
        List<IdValuePair> filteredList = new ArrayList<>();

        for (Panel panel : list) {
            if ((!inactiveTypes && ("Y").equals(panel.getIsActive()))
                    || (inactiveTypes && !("Y").equals(panel.getIsActive()))) {
                filteredList.add(new IdValuePair(panel.getId(), panel.getLocalizedName()));
            }
        }

        return filteredList;
    }

    private List<IdValuePair> createHourList() {
        List<IdValuePair> hours = new ArrayList<>();

        for (int i = 0; i < 24; i++) {
            hours.add(new IdValuePair(String.valueOf(i), String.valueOf(i)));
        }

        return hours;
    }

    private List<IdValuePair> createMinList() {
        List<IdValuePair> minutes = new ArrayList<>();
        minutes.add(new IdValuePair("0", "00"));
        for (int i = 10; i < 60; i = i + 10) {
            minutes.add(new IdValuePair(String.valueOf(i), String.valueOf(i)));
        }
        return minutes;
    }

    private List<IdValuePair> createSortedQAEvents() {
        List<IdValuePair> qaEvents = new ArrayList<>();
        List<QaEvent> qaEventList = qaEventService.getAllQaEvents();

        boolean sortList = ConfigurationProperties.getInstance().isPropertyValueEqual(Property.QA_SORT_EVENT_LIST,
                "true");
        if (sortList) {
            Collections.sort(qaEventList, new Comparator<QaEvent>() {
                @Override
                public int compare(QaEvent o1, QaEvent o2) {
                    return o1.getLocalizedName().compareTo(o2.getLocalizedName());
                }
            });
        }

        QaEvent otherQaEvent = null;
        // Put the "Other" type of event at the bottom of the list.
        for (QaEvent event : qaEventList) {
            if (sortList && "Other".equals(event.getQaEventName())) {
                otherQaEvent = event;
            } else {
                qaEvents.add(new IdValuePair(event.getId(), event.getLocalizedName()));
            }
        }

        if (otherQaEvent != null) {
            qaEvents.add(new IdValuePair(otherQaEvent.getId(), otherQaEvent.getLocalizedName()));
        }

        return qaEvents;
    }

    private List<IdValuePair> createTestSectionActiveList() {
        List<IdValuePair> testSectionsPairs = new ArrayList<>();
        List<TestSection> testSections = testSectionService.getAllActiveTestSections();

        for (TestSection section : testSections) {
            testSectionsPairs.add(new IdValuePair(section.getId(), section.getLocalizedName()));
        }

        return testSectionsPairs;
    }

    private List<IdValuePair> createMethodList() {
        List<IdValuePair> methodPairs = new ArrayList<>();
        List<Method> methods = methodService.getAll();

        for (Method method : methods) {
            methodPairs.add(new IdValuePair(method.getId(), method.getLocalization().getLocalizedValue()));
        }

        return methodPairs;
    }

    private List<IdValuePair> createUnitOfMeasureList() {
        List<IdValuePair> unitOfMeasuresPairs = new ArrayList<>();
        // List<UnitOfMeasure> unitOfMeasures =
        // unitOfMeasureService.getAllActiveUnitOfMeasures();
        List<UnitOfMeasure> unitOfMeasures = unitOfMeasureService.getAll();

        for (UnitOfMeasure unitOfMeasure : unitOfMeasures) {
            unitOfMeasuresPairs.add(new IdValuePair(unitOfMeasure.getId(), unitOfMeasure.getLocalizedName()));
        }

        return unitOfMeasuresPairs;
    }

    private List<IdValuePair> createTypeOfSampleList() {
        List<IdValuePair> typeOfSamplePairs = new ArrayList<>();
        List<TypeOfSample> typeOfSamples = typeOfSampleService.getAllTypeOfSamplesSortOrdered();

        for (TypeOfSample typeOfSample : typeOfSamples) {
            typeOfSamplePairs.add(new IdValuePair(typeOfSample.getId(), typeOfSample.getLocalizedName()));
        }

        return typeOfSamplePairs;
    }

    private List<IdValuePair> createInactiveTestSection() {
        List<IdValuePair> testSectionsPairs = new ArrayList<>();
        List<TestSection> testSections = testSectionService.getAllInActiveTestSections();

        for (TestSection section : testSections) {
            testSectionsPairs.add(new IdValuePair(section.getId(), section.getLocalizedName()));
        }

        return testSectionsPairs;
    }

    private List<IdValuePair> createInactiveMethod() {
        List<IdValuePair> methodPairs = new ArrayList<>();
        List<Method> methods = methodService.getAllInActiveMethods();

        for (Method method : methods) {
            methodPairs.add(new IdValuePair(method.getId(), method.getLocalization().getLocalizedValue()));
        }

        return methodPairs;
    }

    private List<IdValuePair> createTestSectionByNameList() {
        List<IdValuePair> testSectionsPairs = new ArrayList<>();
        List<TestSection> testSections = testSectionService.getAllActiveTestSections();

        for (TestSection section : testSections) {
            testSectionsPairs.add(new IdValuePair(section.getId(), section.getTestSectionName()));
        }

        return testSectionsPairs;
    }

    private List<IdValuePair> createAddressDepartmentList() {
        List<IdValuePair> departmentPairs = new ArrayList<>();
        List<Dictionary> departments = dictionaryService.getDictionaryEntrysByCategoryAbbreviation("description",
                "haitiDepartment", true);

        for (Dictionary dictionary : departments) {
            departmentPairs.add(new IdValuePair(dictionary.getId(), dictionary.getDictEntry()));
        }

        return departmentPairs;
    }

    private List<IdValuePair> createPatientSearchCriteria() {
        List<IdValuePair> searchCriteria = new ArrayList<>();

        // N.B. If the order is to be changed just change the order but keep the
        // id:value pairing the same
        searchCriteria.add(new IdValuePair("0", MessageUtil.getMessage("label.select.search.by")));
        searchCriteria.add(new IdValuePair("2", "1. " + MessageUtil.getMessage("label.select.last.name")));
        searchCriteria.add(new IdValuePair("1", "2. " + MessageUtil.getMessage("label.select.first.name")));
        searchCriteria.add(new IdValuePair("3", "3. " + MessageUtil.getMessage("label.select.last.first.name")));
        searchCriteria.add(new IdValuePair("4", "4. " + MessageUtil.getMessage("label.select.patient.ID")));
        searchCriteria
                .add(new IdValuePair("5", "5. " + MessageUtil.getContextualMessage("quick.entry.accession.number")));

        return searchCriteria;
    }

    private List<IdValuePair> createConsequencesList() {
        List<IdValuePair> consequencesList = new ArrayList<>();

        // N.B. If the order is to be changed just change the order but keep the
        // id:value pairing the same
        consequencesList.add(new IdValuePair("0", MessageUtil.getMessage("label.select.one")));
        consequencesList.add(new IdValuePair("1", MessageUtil.getMessage("label.select.consequences.none")));
        consequencesList.add(new IdValuePair("2", MessageUtil.getMessage("label.select.consequences.moderate")));
        consequencesList.add(new IdValuePair("3", MessageUtil.getMessage("label.select.consequences.high")));
        return consequencesList;
    }

    private List<IdValuePair> createRecurrenceList() {
        List<IdValuePair> recurrenceList = new ArrayList<>();

        // N.B. If the order is to be changed just change the order but keep the
        // id:value pairing the same
        recurrenceList.add(new IdValuePair("0", MessageUtil.getMessage("label.select.one")));
        recurrenceList.add(new IdValuePair("1", MessageUtil.getMessage("label.select.recurrence.not")));
        recurrenceList.add(new IdValuePair("2", MessageUtil.getMessage("label.select.recurrence.somewhat")));
        recurrenceList.add(new IdValuePair("3", MessageUtil.getMessage("label.select.recurrence.highly")));
        return recurrenceList;
    }

    private List<IdValuePair> createTBFollowupLine1List() {
        List<IdValuePair> tbFollowupLine1List = new ArrayList<>();
        tbFollowupLine1List.add(new IdValuePair("M2", MessageUtil.getMessage("dictionary.tb.order.followup") + " M2"));
        tbFollowupLine1List.add(new IdValuePair("M5", MessageUtil.getMessage("dictionary.tb.order.followup") + " M5"));
        tbFollowupLine1List.add(new IdValuePair("M6", MessageUtil.getMessage("dictionary.tb.order.followup") + " M6"));
        return tbFollowupLine1List;
    }

    private List<IdValuePair> createTBFollowupLine2List() {
        List<IdValuePair> tbFollowupLine2List = new ArrayList<>();
        for (int i = 0; i <= 24; i++) {
            tbFollowupLine2List
                    .add(new IdValuePair("M" + i, MessageUtil.getMessage("dictionary.tb.order.followup") + " M" + i));
        }
        return tbFollowupLine2List;
    }

    private List<IdValuePair> createActionTypeList() {
        List<IdValuePair> recurrenceList = new ArrayList<>();

        // N.B. If the order is to be changed just change the order but keep the
        // id:value pairing the same
        recurrenceList.add(new IdValuePair("1", MessageUtil.getMessage("label.input.actiontype.corrective")));
        recurrenceList.add(new IdValuePair("2", MessageUtil.getMessage("label.input.actiontype.preventive")));
        recurrenceList.add(new IdValuePair("3", MessageUtil.getMessage("label.input.actiontype.concurrent")));
        return recurrenceList;
    }

    private List<IdValuePair> createLaboratoryComponentList() {
        List<IdValuePair> recurrenceList = new ArrayList<>();

        // N.B. If the order is to be changed just change the order but keep the
        // id:value pairing the same
        recurrenceList.add(
                new IdValuePair("1", MessageUtil.getMessage("label.select.laboratoryComponent.facilitiesAndSafety")));
        recurrenceList
                .add(new IdValuePair("2", MessageUtil.getMessage("label.select.laboratoryComponent.organization")));
        recurrenceList.add(new IdValuePair("3", MessageUtil.getMessage("label.select.laboratoryComponent.personnel")));
        recurrenceList.add(new IdValuePair("4", MessageUtil.getMessage("label.select.laboratoryComponent.equipment")));
        recurrenceList.add(new IdValuePair("5", MessageUtil.getMessage("label.select.laboratoryComponent.purchasing")));
        recurrenceList.add(new IdValuePair("6", MessageUtil.getMessage("label.select.laboratoryComponent.process")));
        recurrenceList
                .add(new IdValuePair("7", MessageUtil.getMessage("label.select.laboratoryComponent.information")));
        recurrenceList.add(new IdValuePair("8", MessageUtil.getMessage("label.select.laboratoryComponent.documents")));
        recurrenceList.add(new IdValuePair("9", MessageUtil.getMessage("label.select.laboratoryComponent.assessment")));
        recurrenceList
                .add(new IdValuePair("10", MessageUtil.getMessage("label.select.laboratoryComponent.nceManagement")));
        recurrenceList.add(
                new IdValuePair("11", MessageUtil.getMessage("label.select.laboratoryComponent.continualImprovement")));
        return recurrenceList;
    }

    private List<IdValuePair> createMethodByNameList() {
        List<IdValuePair> methodsPairs = new ArrayList<>();
        List<Method> methods = methodService.getAllActiveMethods();
        for (Method method : methods) {
            methodsPairs.add(new IdValuePair(method.getId(), method.getMethodName()));
        }
        return methodsPairs;
    }

    private List<IdValuePair> createSamplePriorityList() {
        List<IdValuePair> priorities = new ArrayList<>();
        priorities.add(new IdValuePair(OrderPriority.ROUTINE.name(), MessageUtil.getMessage("label.priority.routine")));
        priorities.add(new IdValuePair(OrderPriority.ASAP.name(), MessageUtil.getMessage("label.priority.asap")));
        priorities.add(new IdValuePair(OrderPriority.STAT.name(), MessageUtil.getMessage("label.priority.stat")));
        priorities.add(new IdValuePair(OrderPriority.TIMED.name(), MessageUtil.getMessage("label.priority.timed")));
        priorities.add(
                new IdValuePair(OrderPriority.FUTURE_STAT.name(), MessageUtil.getMessage("label.priority.futureStat")));
        return priorities;
    }

    private List<IdValuePair> createAnalyzerList() {
        List<Analyzer> analyzerList = analyzerService.getAll();
        return analyzerList.stream().map(analyzer -> new IdValuePair(analyzer.getId(), analyzer.getName()))
                .collect(Collectors.toList());
    }
}
