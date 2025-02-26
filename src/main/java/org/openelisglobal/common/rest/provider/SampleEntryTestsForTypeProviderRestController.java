package org.openelisglobal.common.rest.provider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
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

    private static String USER_TEST_SECTION_ID;

    private PanelService panelService = SpringContext.getBean(PanelService.class);

    private TestSectionService testSectionService = SpringContext.getBean(TestSectionService.class);

    private TypeOfSamplePanelService samplePanelService = SpringContext.getBean(TypeOfSamplePanelService.class);

    private PanelItemService panelItemService = SpringContext.getBean(PanelItemService.class);

    private TypeOfSampleService typeOfSampleService = SpringContext.getBean(TypeOfSampleService.class);

    private UserService userService = SpringContext.getBean(UserService.class);

    private RoleService roleService = SpringContext.getBean(RoleService.class);

    ArrayList<PanelTestMap> panelsMapList = new ArrayList<>();

    ArrayList<TestMap> testsMapList = new ArrayList<>();

    SampleEntryTests sampleEntryTests;

    private void initializeGlobalVariables() {
        USER_TEST_SECTION_ID = testSectionService.getTestSectionByName("user").getId();
        sampleEntryTests = new SampleEntryTests();
    }

    public SampleEntryTestsForTypeProviderRestController() {
        initializeGlobalVariables();
    }

    @GetMapping(value = "sample-type-tests", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public SampleEntryTests processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String sampleType = request.getParameter("sampleType");

        String receptionRoleId = roleService.getRoleByName(Constants.ROLE_RECEPTION).getId();
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(IActionConstants.USER_SESSION_DATA);
        List<IdValuePair> testSections = userService.getUserTestSections(String.valueOf(usd.getSystemUserId()),
                receptionRoleId);
        List<String> testUnitIds = new ArrayList<>();
        if (testSections != null) {
            testSections.forEach(test -> testUnitIds.add(test.getId()));
        }

        createSearchResultXML(sampleType, testUnitIds);

        return sampleEntryTests;
    }

    @GetMapping(value = "user-sample-types", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getUserSampleTests(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        return userService.getUserSampleTypes(getSysUserId(request), Constants.ROLE_RECEPTION);
    }

    @GetMapping(value = "user-programs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getUserSPrograms(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        return userService.getUserPrograms(getSysUserId(request), Constants.ROLE_RECEPTION);
    }

    private void createSearchResultXML(String sampleType, List<String> testUnitIds) {

        List<Test> tests = typeOfSampleService.getActiveTestsBySampleTypeIdAndTestUnit(sampleType, true, testUnitIds);

        Collections.sort(tests, new Comparator<Test>() {

            @Override
            public int compare(Test t1, Test t2) {
                if (GenericValidator.isBlankOrNull(t1.getSortOrder())
                        || GenericValidator.isBlankOrNull(t2.getSortOrder())) {
                    return TestServiceImpl.getUserLocalizedTestName(t1)
                            .compareTo(TestServiceImpl.getUserLocalizedTestName(t2));
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
                    return TestServiceImpl.getUserLocalizedTestName(t1)
                            .compareTo(TestServiceImpl.getUserLocalizedTestName(t2));
                }
            }
        });

        sampleEntryTests.setSampleTypeId(StringUtil.snipToMaxIdLength(sampleType));
        addTests(tests);

        List<TypeOfSamplePanel> panelList = getPanelList(sampleType);
        List<PanelTestMap> panelMap = linkTestsToPanels(panelList, tests);

        addPanels(panelMap);
    }

    private void addTests(List<Test> tests) {
        testsMapList.clear();
        for (Test test : tests) {
            testsMapList.add(new TestMap(test.getId(), TestServiceImpl.getUserLocalizedTestName(test),
                    USER_TEST_SECTION_ID.equals(test.getTestSection().getId())));
        }
        sampleEntryTests.setTests(testsMapList);
    }

    private void addPanels(List<PanelTestMap> panelMap) {
        panelMap = sortPanels(panelMap);
        panelsMapList.clear();
        for (PanelTestMap testMap : panelMap) {
            panelsMapList.add(new PanelTestMap(testMap.getId(), testMap.getPanelOrder(), testMap.getName(),
                    testMap.getTestIds()));
        }
        sampleEntryTests.setPanels(panelsMapList);
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

        Map<String, Integer> testNameOrderMap = new HashMap<>();

        for (int i = 0; i < tests.size(); i++) {
            testNameOrderMap.put(TestServiceImpl.getUserLocalizedTestName(tests.get(i)), i);
        }

        for (TypeOfSamplePanel samplePanel : panelList) {
            Panel panel = panelService.getPanelById(samplePanel.getPanelId());
            if ("Y".equals(panel.getIsActive())) {
                String matchTests = getTestIndexesForPanels(samplePanel.getPanelId(), testNameOrderMap,
                        panelItemService);
                if (!GenericValidator.isBlankOrNull(matchTests)) {
                    int panelOrder = panelService.getPanelById(samplePanel.getPanelId()).getSortOrderInt();
                    selected.add(new PanelTestMap(samplePanel.getPanelId(), panelOrder, panel.getLocalizedName(),
                            matchTests));
                }
            }
        }

        return selected;
    }

    @SuppressWarnings("unchecked")
    private String getTestIndexesForPanels(String panelId, Map<String, Integer> testIdOrderMap,
            PanelItemService panelItemService) {
        StringBuilder indexes = new StringBuilder();
        List<PanelItem> items = panelItemService.getPanelItemsForPanel(panelId);

        for (PanelItem item : items) {
            String derivedNameFromPanel = getDerivedNameFromPanel(item);
            if (derivedNameFromPanel != null) {
                String ItemId = item.getTest().getId();

                if (ItemId != null) {
                    indexes.append(ItemId);
                    indexes.append(",");
                }
            }
        }

        String withExtraComma = indexes.toString();
        return withExtraComma.length() > 0 ? withExtraComma.substring(0, withExtraComma.length() - 1) : "";
    }

    private String getDerivedNameFromPanel(PanelItem item) {
        // This cover the transition in the DB between the panel_item being linked by
        // name
        // to being linked by id
        if (item.getTest() != null) {
            return TestServiceImpl.getUserLocalizedTestName(item.getTest());
        } else {
            return item.getTestName();
        }
    }

    public class SampleEntryTests {

        private String sampleTypeId;

        private ArrayList<PanelTestMap> panels;

        private ArrayList<TestMap> tests;

        public SampleEntryTests() {
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

    public class PanelTestMap {

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

    public class TestMap {

        String id;

        String name;

        boolean userBenchChoice;

        public TestMap(String id, String name, boolean userBenchChoice) {
            this.id = id;
            this.name = name;
            this.userBenchChoice = userBenchChoice;
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
    }
}
