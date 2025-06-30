package org.openelisglobal.common.rest;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.rest.provider.bean.TestDisplayBean;
import org.openelisglobal.common.rest.provider.form.DisplayListPagingForm;
import org.openelisglobal.common.rest.util.DisplayListPaging;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.LabelValuePair;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.reports.action.implementation.ExportTrendsByDate;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.controller.UnifiedSystemUserController;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/rest/")
public class DisplayListController extends BaseRestController {
    @Value("${org.itech.login.saml:false}")
    private Boolean useSAML;

    @Value("${org.itech.login.oauth:false}")
    private Boolean useOAUTH;

    @Value("${org.itech.login.form:true}")
    private Boolean useFormLogin;

    @Autowired
    private ProviderService providerService;

    @Autowired
    private PersonService personService;

    @Autowired
    private UserService userService;

    @Autowired
    protected TestService testService;

    @Autowired
    private RoleService roleService;

    @Autowired
    TypeOfSampleService typeOfSampleService;

    @Autowired
    TestSectionService testSectionService;

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private SiteInformationService siteInformationService;

    @Autowired
    private TestResultService testResultService;

    @Autowired
    DictionaryService dictionaryService;

    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;
    private static String authorizationRequestBaseUri = "oauth2/authorization";
    Map<String, String> oauth2AuthenticationUrls = new HashMap<>();

    private static boolean HAS_NFS_PANEL = false;

    static {
        HAS_NFS_PANEL = ConfigurationProperties.getInstance().isPropertyValueEqual(Property.CONDENSE_NFS_PANEL, "true");
    }

    protected static List<Integer> statusList;
    protected static List<String> nfsTestIdList;

    private String escapeRegexChars(String regex) {
        // TODO Auto-generated method stub
        return regex;
    }

    // Manually create an instance of ExportTrendsByDate
    private ExportTrendsByDate exportTrendsByDate = new ExportTrendsByDate();

    @Autowired
    private ProjectService projectService; // Inject the ProjectService

    @GetMapping(value = "projects", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getProjects() {
        List<Project> projectList = projectService.getAllProjects(); // Assuming a method in ProjectService to fetch all
                                                                     // projects
        List<IdValuePair> projects = new ArrayList<>();

        // Convert Project objects to IdValuePair and add to the list
        for (Project project : projectList) {
            projects.add(new IdValuePair(project.getId(), project.getProjectName()));
        }

        return projects;
    }

    @GetMapping(value = "trendsprojects", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getTempProjects() {
        // Use the manually created instance of ExportTrendsByDate
        List<Project> projects = exportTrendsByDate.getProjectList();
        List<IdValuePair> projectList = new ArrayList<>();
        projects.forEach(project -> {
            projectList.add(new IdValuePair(project.getId(), project.getProjectName()));
        });
        return projectList;
    }

    @PostConstruct
    private void initialize() {
        if (statusList == null) {
            statusList = new ArrayList<>();
            statusList.add(Integer
                    .parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted)));
            statusList.add(Integer.parseInt(
                    SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.BiologistRejected)));
            statusList.add(Integer.parseInt(
                    SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalRejected)));
            statusList.add(Integer.parseInt(
                    SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NonConforming_depricated)));
        }

        if (nfsTestIdList == null) {
            nfsTestIdList = new ArrayList<>();
            nfsTestIdList.add(getTestId("GB"));
            nfsTestIdList.add(getTestId("Neut %"));
            nfsTestIdList.add(getTestId("Lymph %"));
            nfsTestIdList.add(getTestId("Mono %"));
            nfsTestIdList.add(getTestId("Eo %"));
            nfsTestIdList.add(getTestId("Baso %"));
            nfsTestIdList.add(getTestId("GR"));
            nfsTestIdList.add(getTestId("Hb"));
            nfsTestIdList.add(getTestId("HCT"));
            nfsTestIdList.add(getTestId("VGM"));
            nfsTestIdList.add(getTestId("TCMH"));
            nfsTestIdList.add(getTestId("CCMH"));
            nfsTestIdList.add(getTestId("PLQ"));
        }
    }

    protected String getTestId(String testName) {
        Test test = testService.getTestByLocalizedName(testName);
        if (test == null) {
            test = new Test();
        }
        return test.getId();
    }

    protected List<IdValuePair> adjustNFSTests(List<IdValuePair> allTestsList) {
        List<IdValuePair> adjustedList = new ArrayList<>(allTestsList.size());
        for (IdValuePair idValuePair : allTestsList) {
            if (!nfsTestIdList.contains(idValuePair.getId())) {
                adjustedList.add(idValuePair);
            }
        }
        // add NFS to the list
        adjustedList.add(new IdValuePair("NFS", "NFS"));
        return adjustedList;
    }

    protected boolean allNFSTestsRequested(List<String> testIdList) {
        return (testIdList.containsAll(nfsTestIdList));
    }

    @GetMapping(value = "displayList/{listType}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getDisplayList(@PathVariable DisplayListService.ListType listType) {
        return DisplayListService.getInstance().getFreshList(listType);
    }

    @GetMapping(value = "paginatedDisplayList/{listType}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public DisplayListPagingForm getPaginatedDisplayList(HttpServletRequest request,
            @PathVariable DisplayListService.ListType listType)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        DisplayListPagingForm displayListform = new DisplayListPagingForm();
        DisplayListPaging paging = new DisplayListPaging();
        List<IdValuePair> displayItems = new ArrayList<>();
        String requestedPage = request.getParameter("page");
        if (GenericValidator.isBlankOrNull(requestedPage)) {
            displayItems = DisplayListService.getInstance().getFreshList(listType);

            paging.setDatabaseResults(request, displayListform, displayItems);
        } else {
            int requestedPageNumber = Integer.parseInt(requestedPage);
            paging.page(request, displayListform, requestedPageNumber);
        }
        return displayListform;
    }

    @GetMapping(value = "tests", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getTests() {
        return DisplayListService.getInstance().getFreshList(ListType.ALL_TESTS);
    }

    @GetMapping(value = "tests-by-sample", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getTestsBySample(@RequestParam String sampleType) {
        List<IdValuePair> tests = new ArrayList<>();
        List<Test> testList = new ArrayList<>();
        if (StringUtils.isNotBlank(sampleType)) {
            testList = typeOfSampleService.getActiveTestsBySampleTypeId(sampleType, false);
        } else {
            return tests;
        }

        testList.forEach(test -> {
            tests.add(new IdValuePair(test.getId(), TestServiceImpl.getLocalizedTestNameWithType(test)));
        });
        return tests;
    }

    @GetMapping(value = "samples", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getSamples() {
        return DisplayListService.getInstance().getList(ListType.SAMPLE_TYPE_ACTIVE);
    }

    @GetMapping(value = "health-regions", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getHealthRegions() {
        return DisplayListService.getInstance().getList(ListType.PATIENT_HEALTH_REGIONS);
    }

    @GetMapping(value = "education-list", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getEducationList() {
        return DisplayListService.getInstance().getList(ListType.PATIENT_EDUCATION);
    }

    @GetMapping(value = "marital-statuses", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getMaritialList() {
        return DisplayListService.getInstance().getList(ListType.PATIENT_MARITAL_STATUS);
    }

    @GetMapping(value = "nationalities", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getNationalityList() {
        return DisplayListService.getInstance().getList(ListType.PATIENT_NATIONALITY);
    }

    @GetMapping(value = "programs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getPrograms() {
        return DisplayListService.getInstance().getList(ListType.PROGRAM);
    }

    @GetMapping(value = "dictionaryPrograms", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getDictionaryPrograms() {
        return DisplayListService.getInstance().getList(ListType.DICTIONARY_PROGRAM);
    }

    @GetMapping(value = "patientPaymentsOptions", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getSamplePatientPaymentOptions() {
        return DisplayListService.getInstance().getList(ListType.SAMPLE_PATIENT_PAYMENT_OPTIONS);
    }

    @GetMapping(value = "testLocationCodes", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getTestLocationCodes() {
        return DisplayListService.getInstance().getList(ListType.TEST_LOCATION_CODE);
    }

    @GetMapping(value = "test-rejection-reasons", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getTestRejectionReasons() {
        return DisplayListService.getInstance().getList(ListType.REJECTION_REASONS);
    }

    @GetMapping(value = "referral-reasons", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    private List<IdValuePair> createReferralReasonList() {
        return DisplayListService.getInstance().getList(ListType.REFERRAL_REASONS);
    }

    @GetMapping(value = "referral-organizations", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    private List<IdValuePair> createReferralOrganizationsList() {
        return DisplayListService.getInstance().getList(ListType.REFERRAL_ORGANIZATIONS);
    }

    @GetMapping(value = "site-names", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    private List<IdValuePair> getSiteNameList() {
        return DisplayListService.getInstance().getList(ListType.SAMPLE_PATIENT_REFERRING_CLINIC);
    }

    @GetMapping(value = "configuration-properties", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    private Map<String, Object> getConfigurationProperties() {
        SiteInformation DEFAULT_SITE_INFORATION = new SiteInformation();
        String DEFAULT_REGEX = "0-9a-z .'_@-";
        DEFAULT_SITE_INFORATION.setValue(DEFAULT_REGEX);
        String FIRST_NAME_REGEX = "^[" + escapeRegexChars(
                siteInformationService.getMatch("name", "firstNameCharset").orElse(DEFAULT_SITE_INFORATION).getValue())
                + "]*$";
        String LAST_NAME_REGEX = "^[" + escapeRegexChars(
                siteInformationService.getMatch("name", "lastNameCharset").orElse(DEFAULT_SITE_INFORATION).getValue())
                + "]*$";
        Map<String, Object> configs = getOpenConfigurationProperties();

        configs.put(Property.allowResultRejection.toString(),
                ConfigurationProperties.getInstance().getPropertyValue(Property.allowResultRejection));

        configs.put(Property.AccessionFormat.toString(),
                ConfigurationProperties.getInstance().getPropertyValue(Property.AccessionFormat));
        configs.put(Property.USE_ALPHANUM_ACCESSION_PREFIX.toString(),
                ConfigurationProperties.getInstance().getPropertyValue(Property.USE_ALPHANUM_ACCESSION_PREFIX));
        configs.put(Property.ALERT_FOR_INVALID_RESULTS.toString(),
                ConfigurationProperties.getInstance().getPropertyValue(Property.ALERT_FOR_INVALID_RESULTS));
        configs.put(Property.DEFAULT_DATE_LOCALE.toString(),
                ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_DATE_LOCALE));
        configs.put(Property.UseExternalPatientInfo.toString(),
                ConfigurationProperties.getInstance().getPropertyValue(Property.UseExternalPatientInfo));
        configs.put("DEFAULT_PAGE_SIZE",
                ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        configs.put("FIRST_NAME_REGEX", FIRST_NAME_REGEX);
        configs.put("LAST_NAME_REGEX", LAST_NAME_REGEX);
        return configs;
    }

    // these are fetched before login
    @GetMapping(value = "open-configuration-properties", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    private Map<String, Object> getOpenConfigurationProperties() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(Property.restrictFreeTextProviderEntry.toString(),
                ConfigurationProperties.getInstance().getPropertyValue(Property.restrictFreeTextProviderEntry));
        configs.put(Property.restrictFreeTextRefSiteEntry.toString(),
                ConfigurationProperties.getInstance().getPropertyValue(Property.restrictFreeTextRefSiteEntry));
        configs.put(Property.PHONE_FORMAT.toString(),
                ConfigurationProperties.getInstance().getPropertyValue(Property.PHONE_FORMAT));
        configs.put(Property.releaseNumber.toString(),
                ConfigurationProperties.getInstance().getPropertyValue(Property.releaseNumber));
        configs.put(Property.ACCESSION_NUMBER_VALIDATE.toString(),
                ConfigurationProperties.getInstance().getPropertyValue(Property.ACCESSION_NUMBER_VALIDATE));
        configs.put(Property.AUTOFILL_COLLECTION_DATE.toString(),
                ConfigurationProperties.getInstance().getPropertyValue(Property.AUTOFILL_COLLECTION_DATE));
        configs.put(Property.ACCEPT_EXTERNAL_ORDERS.toString(),
                ConfigurationProperties.getInstance().getPropertyValue(Property.ACCEPT_EXTERNAL_ORDERS));
        configs.put("currentDateAsText", DateUtil.getCurrentDateAsText());
        configs.put("currentTimeAsText", DateUtil.getCurrentTimeAsText());
        configs.put(Property.BANNER_TEXT.toString(), localizationService
                .getLocalizedValueById(ConfigurationProperties.getInstance().getPropertyValue(Property.BANNER_TEXT)));
        SiteInformation studyManagementTab = siteInformationService.getSiteInformationByName("Study Management tab");
        configs.put("studyManagementTab", studyManagementTab != null ? studyManagementTab.getValue() : "false");
        configs.put("useSaml", useSAML ? "true" : "false");
        configs.put("useOauth", useOAUTH ? "true" : "false");
        if (useOAUTH) {
            ResolvableType type = ResolvableType.forInstance(clientRegistrationRepository).as(Iterable.class);
            if (type != ResolvableType.NONE && ClientRegistration.class.isAssignableFrom(type.resolveGenerics()[0])) {
                @SuppressWarnings("unchecked")
                Iterable<ClientRegistration> clientRegistrations = (Iterable<ClientRegistration>) clientRegistrationRepository;
                clientRegistrations.forEach(registration -> oauth2AuthenticationUrls.put(registration.getClientName(),
                        authorizationRequestBaseUri + "/" + registration.getRegistrationId()));
            }
            configs.put("oauthUrls",
                    oauth2AuthenticationUrls.entrySet().stream().map(e -> new KeyValuePair(e.getKey(), e.getValue())));
        }
        configs.put("useFormLogin", useFormLogin ? "true" : "false");
        configs.put(Property.SUBJECT_ON_WORKPLAN.toString(),
                ConfigurationProperties.getInstance().getPropertyValue(Property.SUBJECT_ON_WORKPLAN));
        configs.put(Property.NEXT_VISIT_DATE_ON_WORKPLAN.toString(),
                ConfigurationProperties.getInstance().getPropertyValue(Property.NEXT_VISIT_DATE_ON_WORKPLAN));
        configs.put(Property.configurationName.toString(),
                ConfigurationProperties.getInstance().getPropertyValue(Property.configurationName));
        configs.put(Property.REQUIRE_LAB_UNIT_AT_LOGIN.toString(),
                ConfigurationProperties.getInstance().getPropertyValue(Property.REQUIRE_LAB_UNIT_AT_LOGIN));
        configs.put(Property.ENABLE_CLIENT_REGISTRY.toString(),
                ConfigurationProperties.getInstance().getPropertyValue(Property.ENABLE_CLIENT_REGISTRY));
        return configs;
    }

    @GetMapping(value = "practitioner", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    private Provider getProviderInformation(@RequestParam String providerId) {
        if (providerId != null) {
            Person person = personService.getPersonById(providerId);
            Provider provider = providerService.getProviderByPerson(person);
            return provider;
        }
        return null;
    }

    @GetMapping(value = "test-list", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    private List<IdValuePair> getTestDropdownList(HttpServletRequest request) {
        List<IdValuePair> testList = userService.getAllDisplayUserTestsByLabUnit(getSysUserId(request),
                Constants.ROLE_RESULTS);

        if (HAS_NFS_PANEL) {
            testList = adjustNFSTests(testList);
        }
        Collections.sort(testList, new ValueComparator());
        return testList;
    }

    @GetMapping(value = "priorities", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    private List<IdValuePair> createPriorityList() {
        return DisplayListService.getInstance().getList(ListType.ORDER_PRIORITY);
    }

    @GetMapping(value = "panels", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    private List<IdValuePair> createPanelList() {
        return DisplayListService.getInstance().getList(ListType.PANELS);
    }

    @GetMapping(value = "test-sections", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    private List<IdValuePair> createTestSectionsList() {
        return DisplayListService.getInstance().getList(ListType.TEST_SECTION_ACTIVE);
    }

    @GetMapping(value = "user-test-sections/{roleName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    private List<IdValuePair> createUserTestSectionsList(HttpServletRequest request, @PathVariable String roleName) {
        if (roleName.equals("ALL")) {
            return userService.getUserTestSections(getSysUserId(request), null);
        } else {
            Role role = roleService.getRoleByName(roleName);
            if (role == null) {
                return new ArrayList<>();
            }
            String resultsRoleId = role.getId();
            return userService.getUserTestSections(getSysUserId(request), resultsRoleId);
        }
    }

    @GetMapping(value = "analysis-status-types", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getAnalysisStatusTypes() {

        List<IdValuePair> list = new ArrayList<>();
        list.add(new IdValuePair("0", ""));

        list.add(new IdValuePair(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted),
                SpringContext.getBean(IStatusService.class).getStatusName(AnalysisStatus.NotStarted)));
        list.add(new IdValuePair(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled),
                SpringContext.getBean(IStatusService.class).getStatusName(AnalysisStatus.Canceled)));
        list.add(new IdValuePair(
                SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance),
                SpringContext.getBean(IStatusService.class).getStatusName(AnalysisStatus.TechnicalAcceptance)));
        list.add(new IdValuePair(
                SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalRejected),
                SpringContext.getBean(IStatusService.class).getStatusName(AnalysisStatus.TechnicalRejected)));
        list.add(new IdValuePair(
                SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.BiologistRejected),
                SpringContext.getBean(IStatusService.class).getStatusName(AnalysisStatus.BiologistRejected)));

        return list;
    }

    @GetMapping(value = "sample-status-types", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getSampleStatusTypes() {

        List<IdValuePair> list = new ArrayList<>();
        list.add(new IdValuePair("0", ""));

        list.add(new IdValuePair(
                SpringContext.getBean(IStatusService.class).getStatusID(StatusService.OrderStatus.Entered),
                SpringContext.getBean(IStatusService.class).getStatusName(StatusService.OrderStatus.Entered)));
        list.add(new IdValuePair(
                SpringContext.getBean(IStatusService.class).getStatusID(StatusService.OrderStatus.Started),
                SpringContext.getBean(IStatusService.class).getStatusName(StatusService.OrderStatus.Started)));

        return list;
    }

    class ValueComparator implements Comparator<IdValuePair> {

        @Override
        public int compare(IdValuePair p1, IdValuePair p2) {
            return p1.getValue().toUpperCase().compareTo(p2.getValue().toUpperCase());
        }
    }

    @GetMapping(value = "departments-for-site", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getDepartmentsForReferingSite(@RequestParam String refferingSiteId) {

        List<IdValuePair> list = new ArrayList<>();
        List<Organization> departments = organizationService.getOrganizationsByParentId(refferingSiteId).stream()
                .filter(org -> org.getIsActive().equals(IActionConstants.YES)).collect(Collectors.toList());
        departments.forEach(d -> {
            list.add(new IdValuePair(d.getId(), d.getOrganizationName()));
        });

        return list;
    }

    @GetMapping(value = "test-display-beans", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<TestDisplayBean> getTestBeansBySample(@RequestParam(required = false) String sampleType) {
        return getTestBeansForSample(sampleType);
    }

    @GetMapping(value = "test-display-beans-map", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, List<TestDisplayBean>> getTestBeansMap(@RequestParam(required = false) String samplesTypes) {

        List<String> samplesList = new ArrayList<>();
        if (StringUtils.isNotBlank(samplesTypes)) {
            samplesList = Arrays.asList(samplesTypes.split(","));
        }
        Set<String> sampleSet = new HashSet<>(samplesList);
        Map<String, List<TestDisplayBean>> testBeanMap = new HashMap<>();

        sampleSet.forEach(sampleType -> {
            testBeanMap.put(sampleType, getTestBeansForSample(sampleType));
        });
        return testBeanMap;
    }

    private List<TestDisplayBean> getTestBeansForSample(String sampleType) {

        List<TestDisplayBean> testItems = new ArrayList<>();
        List<Test> testList = new ArrayList<>();
        if (StringUtils.isNotBlank(sampleType)) {
            testList = typeOfSampleService.getActiveTestsBySampleTypeId(sampleType, true);
        } else {
            return testItems;
        }

        for (Test test : testList) {
            TestDisplayBean testDisplayBean = new TestDisplayBean(test.getId(),
                    TestServiceImpl.getLocalizedTestNameWithType(test), testService.getResultType(test));
            List<IdValuePair> resultList = new ArrayList<>();
            List<TestResult> results = testResultService.getActiveTestResultsByTest(test.getId());
            results.forEach(result -> {
                if (result.getValue() != null) {
                    Dictionary dict = dictionaryService.getDictionaryById(result.getValue());
                    resultList.add(new IdValuePair(dict.getId(), dict.getLocalizedName()));
                }
            });
            testDisplayBean.setResultList(resultList);
            testItems.add(testDisplayBean);

            Collections.sort(testItems, new Comparator<TestDisplayBean>() {

                @Override
                public int compare(TestDisplayBean o1, TestDisplayBean o2) {
                    return o1.getValue().compareTo(o2.getValue());
                }
            });
        }
        return testItems;

    }

    @GetMapping(value = "systemroles", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<LabelValuePair> getRoles(@RequestParam(required = false) String sampleType) {
        return roleService.getAllActiveRoles().stream().filter(r -> !r.getGroupingRole())
                .map(r -> new LabelValuePair(r.getDescription(), r.getName())).collect(Collectors.toList());
    }

    @GetMapping(value = "systemroles-testsections", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<LabelValuePair> getRolesWithTestSections() {
        List<LabelValuePair> rolesWithTestSections = new ArrayList<>();
        String globalParentRoleId = roleService.getRoleByName(Constants.GLOBAL_ROLES_GROUP).getId();
        String labUnitRoleId = roleService.getRoleByName(Constants.LAB_ROLES_GROUP).getId();
        List<TestSection> testSections = testSectionService.getAllActiveTestSections();

        List<Role> roles = roleService.getAllActiveRoles();
        List<Role> globalRoles = roles.stream().filter(role -> globalParentRoleId.equals(role.getGroupingParent()))
                .collect(Collectors.toList());
        List<Role> labUnitRoles = roles.stream().filter(role -> labUnitRoleId.equals(role.getGroupingParent()))
                .collect(Collectors.toList());
        rolesWithTestSections.addAll(
                globalRoles.stream().map(r -> new LabelValuePair(r.getDescription(), "oeg-" + r.getName().trim()))
                        .collect(Collectors.toList()));

        rolesWithTestSections.addAll(labUnitRoles.stream()
                .map(r -> new LabelValuePair(r.getDescription(),
                        "oeg-" + r.getName().trim() + "-" + UnifiedSystemUserController.ALL_LAB_UNITS))
                .collect(Collectors.toList()));
        testSections.forEach(e -> rolesWithTestSections.addAll(labUnitRoles.stream()
                .map(r -> new LabelValuePair(r.getDescription(),
                        "oeg-" + r.getName().trim() + "-" + e.getTestSectionName().trim()))
                .collect(Collectors.toList())));
        return rolesWithTestSections;
    }
}
