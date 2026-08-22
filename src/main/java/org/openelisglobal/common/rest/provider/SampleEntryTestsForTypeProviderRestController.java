package org.openelisglobal.common.rest.provider;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.microbiology.service.MicrobiologyReferenceService;
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testmethod.service.TestMethodService;
import org.openelisglobal.testmethod.service.TestMethodService.TestMethodDto;
import org.openelisglobal.typeofsample.service.TypeOfSamplePanelService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/rest/")
public class SampleEntryTestsForTypeProviderRestController extends BaseRestController {

    private final PanelService panelService;
    private final TestSectionService testSectionService;
    private final TypeOfSamplePanelService samplePanelService;
    private final PanelItemService panelItemService;
    private final TypeOfSampleService typeOfSampleService;
    private final UserService userService;
    private final RoleService roleService;
    private final ProgramService programService;
    private final TestMethodService testMethodService;
    private final MicrobiologyReferenceService microbiologyReferenceService;

    public SampleEntryTestsForTypeProviderRestController(PanelService panelService,
            TestSectionService testSectionService, TypeOfSamplePanelService samplePanelService,
            PanelItemService panelItemService, TypeOfSampleService typeOfSampleService, UserService userService,
            RoleService roleService, ProgramService programService, TestMethodService testMethodService,
            MicrobiologyReferenceService microbiologyReferenceService) {
        this.panelService = panelService;
        this.testSectionService = testSectionService;
        this.samplePanelService = samplePanelService;
        this.panelItemService = panelItemService;
        this.typeOfSampleService = typeOfSampleService;
        this.userService = userService;
        this.roleService = roleService;
        this.programService = programService;
        this.testMethodService = testMethodService;
        this.microbiologyReferenceService = microbiologyReferenceService;
    }

    @GetMapping(value = "sample-type-tests", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public SampleEntryTests processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String sampleType = request.getParameter("sampleType");

        String receptionRoleId = roleService.getRoleByName(Constants.ROLE_RECEPTION).getId();
        List<IdValuePair> testSections = userService.getUserTestSections(getSysUserId(request), receptionRoleId);
        List<String> testUnitIds = new ArrayList<>();
        if (testSections != null) {
            testSections.forEach(test -> testUnitIds.add(test.getId()));
        }

        return createSearchResult(sampleType, testUnitIds);
    }

    @GetMapping(value = "user-sample-types", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getUserSampleTests(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        return userService.getUserSampleTypes(getSysUserId(request), Constants.ROLE_RECEPTION);
    }

    @GetMapping(value = "user-programs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<ProgramOption> getUserSPrograms(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        return userService.getUserPrograms(getSysUserId(request), Constants.ROLE_RECEPTION).stream().map(option -> {
            Program program = programService.get(option.getId());
            return program == null ? null : new ProgramOption(option.getId(), option.getValue(), program.getCode());
        }).filter(java.util.Objects::nonNull).toList();
    }

    private SampleEntryTests createSearchResult(String sampleType, List<String> testUnitIds) {

        List<Test> tests = new ArrayList<>(
                typeOfSampleService.getActiveTestsBySampleTypeIdAndTestUnit(sampleType, true, testUnitIds));

        Collections.sort(tests, new Comparator<Test>() {

            @Override
            public int compare(Test t1, Test t2) {
                if (GenericValidator.isBlankOrNull(t1.getSortOrder())
                        || GenericValidator.isBlankOrNull(t2.getSortOrder())) {
                    return localizedTestName(t1).compareTo(localizedTestName(t2));
                }

                try {
                    int t1Sort = Integer.parseInt(t1.getSortOrder());
                    int t2Sort = Integer.parseInt(t2.getSortOrder());

                    if (t1Sort > t2Sort) {
                        return 1;
                    } else if (t1Sort < t2Sort) {
                        return -1;
                    } else {
                        return 0;
                    }

                } catch (NumberFormatException e) {
                    return localizedTestName(t1).compareTo(localizedTestName(t2));
                }
            }
        });

        List<TypeOfSamplePanel> panelList = getPanelList(sampleType);
        List<PanelTestMap> panelMap = linkTestsToPanels(panelList, tests);
        return new SampleEntryTests(StringUtil.snipToMaxIdLength(sampleType), addPanels(panelMap), addTests(tests));
    }

    private ArrayList<TestMap> addTests(List<Test> tests) {
        String userTestSectionId = testSectionService.getTestSectionByName("user").getId();
        ArrayList<TestMap> testsMapList = new ArrayList<>();
        for (Test test : tests) {
            List<OrderEntryMethod> methods = testMethodService.getLinkedMethodDtos(test.getId()).stream()
                    .map(method -> toOrderEntryMethod(method, test.getCultureWorkflowType())).toList();
            testsMapList.add(new TestMap(test.getId(), localizedTestName(test),
                    userTestSectionId.equals(test.getTestSection().getId()), test.getCultureWorkflowType(), methods));
        }
        return testsMapList;
    }

    private OrderEntryMethod toOrderEntryMethod(TestMethodDto method, String workflowType) {
        MicroCultureSetup setup = workflowType == null || workflowType.isBlank() ? null
                : microbiologyReferenceService.getActiveCultureSetupForMethod(method.methodId,
                        MicroWorkflowType.valueOf(workflowType));
        return new OrderEntryMethod(method, setup);
    }

    private ArrayList<PanelTestMap> addPanels(List<PanelTestMap> panelMap) {
        panelMap = sortPanels(panelMap);
        ArrayList<PanelTestMap> panelsMapList = new ArrayList<>();
        for (PanelTestMap testMap : panelMap) {
            panelsMapList.add(new PanelTestMap(testMap.getId(), testMap.getPanelOrder(), testMap.getName(),
                    testMap.getTestIds()));
        }
        return panelsMapList;
    }

    private List<PanelTestMap> sortPanels(List<PanelTestMap> panelMap) {

        Collections.sort(panelMap, new Comparator<PanelTestMap>() {

            @Override
            public int compare(PanelTestMap o1, PanelTestMap o2) {
                return o1.getPanelOrder() - o2.getPanelOrder();
            }
        });

        return panelMap;
    }

    private List<TypeOfSamplePanel> getPanelList(String sampleType) {
        return samplePanelService.getTypeOfSamplePanelsForSampleType(sampleType);
    }

    private List<PanelTestMap> linkTestsToPanels(List<TypeOfSamplePanel> panelList, List<Test> tests) {
        List<PanelTestMap> selected = new ArrayList<>();

        Map<String, String> testIdsByName = new HashMap<>();

        for (Test test : tests) {
            testIdsByName.put(localizedTestName(test), test.getId());
        }

        for (TypeOfSamplePanel samplePanel : panelList) {
            Panel panel = panelService.getPanelById(samplePanel.getPanelId());
            if ("Y".equals(panel.getIsActive())) {
                String matchTests = getTestIdsForPanel(samplePanel.getPanelId(), testIdsByName, panelItemService);
                if (!GenericValidator.isBlankOrNull(matchTests)) {
                    int panelOrder = panelService.getPanelById(samplePanel.getPanelId()).getSortOrderInt();
                    selected.add(new PanelTestMap(samplePanel.getPanelId(), panelOrder, panel.getLocalizedName(),
                            matchTests));
                }
            }
        }

        return selected;
    }

    private String getTestIdsForPanel(String panelId, Map<String, String> testIdsByName,
            PanelItemService panelItemService) {
        StringBuilder testIds = new StringBuilder();
        List<PanelItem> items = panelItemService.getPanelItemsForPanel(panelId);

        for (PanelItem item : items) {
            String testId = item.getTest() == null ? testIdsByName.get(item.getTestName()) : item.getTest().getId();
            if (testId != null && testIdsByName.containsValue(testId)) {
                testIds.append(testId).append(",");
            }
        }

        String withExtraComma = testIds.toString();
        return withExtraComma.length() > 0 ? withExtraComma.substring(0, withExtraComma.length() - 1) : "";
    }

    private String localizedTestName(Test test) {
        if (test == null) {
            return "";
        }
        try {
            return test.getLocalizedTestName().getLocalizedValue();
        } catch (RuntimeException e) {
            return test.getDescription() == null ? "" : test.getDescription();
        }
    }

    public static class SampleEntryTests {

        private String sampleTypeId;

        private ArrayList<PanelTestMap> panels;

        private ArrayList<TestMap> tests;

        public SampleEntryTests(String sampleTypeId, ArrayList<PanelTestMap> panels, ArrayList<TestMap> tests) {
            this.sampleTypeId = sampleTypeId;
            this.panels = panels;
            this.tests = tests;
        }

        public String getSampleTypeId() {
            return sampleTypeId;
        }

        public void setSampleTypeId(String sampleTypeId) {
            this.sampleTypeId = sampleTypeId;
        }

        public ArrayList<PanelTestMap> getPanels() {
            return panels;
        }

        public void setPanels(ArrayList<PanelTestMap> panels) {
            this.panels = panels;
        }

        public ArrayList<TestMap> getTests() {
            return tests;
        }

        public void setTests(ArrayList<TestMap> tests) {
            this.tests = tests;
        }
    }

    public static class PanelTestMap {

        private String name;

        private String testIds;

        // panel id
        private String id;

        private int panelOrder;

        public PanelTestMap(String id, int panelOrder, String panelName, String testIds) {
            name = panelName;
            this.testIds = testIds;
            this.id = id;
            this.panelOrder = panelOrder;
        }

        public String getName() {
            return name;
        }

        public String getTestIds() {
            return testIds;
        }

        public String getId() {
            return id;
        }

        public int getPanelOrder() {
            return panelOrder;
        }
    }

    public static class TestMap {

        String id;

        String name;

        boolean userBenchChoice;

        String cultureWorkflowType;

        List<OrderEntryMethod> methods;

        public TestMap(String id, String name, boolean userBenchChoice, String cultureWorkflowType,
                List<OrderEntryMethod> methods) {
            this.id = id;
            this.name = name;
            this.userBenchChoice = userBenchChoice;
            this.cultureWorkflowType = cultureWorkflowType;
            this.methods = methods;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isUserBenchChoice() {
            return userBenchChoice;
        }

        public void setUserBenchChoice(boolean userBenchChoice) {
            this.userBenchChoice = userBenchChoice;
        }

        public String getCultureWorkflowType() {
            return cultureWorkflowType;
        }

        public void setCultureWorkflowType(String cultureWorkflowType) {
            this.cultureWorkflowType = cultureWorkflowType;
        }

        public List<OrderEntryMethod> getMethods() {
            return methods;
        }
    }

    public static class OrderEntryMethod {
        public String id;
        public String methodId;
        public String methodName;
        public String methodCode;
        public boolean isDefault;
        public String effectiveDate;
        public String mediaDefaults;
        public String incubationDefaults;
        public String atmosphereDefaults;

        OrderEntryMethod(TestMethodDto method, MicroCultureSetup setup) {
            id = method.id;
            methodId = method.methodId;
            methodName = method.methodName;
            methodCode = method.methodCode;
            isDefault = method.isDefault;
            effectiveDate = method.effectiveDate;
            if (setup != null) {
                mediaDefaults = setup.getMediaDefaults();
                incubationDefaults = setup.getIncubationDefaults();
                atmosphereDefaults = setup.getAtmosphereDefaults();
            }
        }
    }

    public static class ProgramOption {
        private final String id;
        private final String value;
        private final String code;

        public ProgramOption(String id, String value, String code) {
            this.id = id;
            this.value = value;
            this.code = code;
        }

        public String getId() {
            return id;
        }

        public String getValue() {
            return value;
        }

        public String getCode() {
            return code;
        }
    }
}
