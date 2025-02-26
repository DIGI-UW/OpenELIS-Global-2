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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.common.provider.query;

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
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.util.XMLUtil;
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
import org.openelisglobal.testdictionary.service.TestDictionaryService;
import org.openelisglobal.testdictionary.valueholder.TestDictionary;
import org.openelisglobal.typeofsample.service.TypeOfSamplePanelService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel;

public class SampleEntryTestsForTypeProvider extends BaseQueryProvider {
    private static String USER_TEST_SECTION_ID;
    private static String VARIABLE_SAMPLE_TYPE_ID;

    private PanelService panelService = SpringContext.getBean(PanelService.class);
    private TestSectionService testSectionService = SpringContext.getBean(TestSectionService.class);
    private TestDictionaryService testDictionaryService = SpringContext.getBean(TestDictionaryService.class);
    private TypeOfSamplePanelService samplePanelService = SpringContext.getBean(TypeOfSamplePanelService.class);
    private PanelItemService panelItemService = SpringContext.getBean(PanelItemService.class);
    private TypeOfSampleService typeOfSampleService = SpringContext.getBean(TypeOfSampleService.class);
    private UserService userService = SpringContext.getBean(UserService.class);
    private RoleService roleService = SpringContext.getBean(RoleService.class);

    private boolean isVariableTypeOfSample;

    private final void initializeGlobalVariables() {
        USER_TEST_SECTION_ID = testSectionService.getTestSectionByName("user").getId();
        VARIABLE_SAMPLE_TYPE_ID = typeOfSampleService.getTypeOfSampleIdForLocalAbbreviation("Variable");
    }

    public SampleEntryTestsForTypeProvider() {
        initializeGlobalVariables();
    }

    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String sampleType = request.getParameter("sampleType");
        isVariableTypeOfSample = VARIABLE_SAMPLE_TYPE_ID.equals(sampleType);
        StringBuilder xml = new StringBuilder();

        String receptionRoleId = roleService.getRoleByName(Constants.ROLE_RECEPTION).getId();
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(IActionConstants.USER_SESSION_DATA);
        List<IdValuePair> testSections = userService.getUserTestSections(String.valueOf(usd.getSystemUserId()),
                receptionRoleId);
        List<String> testUnitIds = new ArrayList<>();
        if (testSections != null) {
            testSections.forEach(test -> testUnitIds.add(test.getId()));
        }

        String result = createSearchResultXML(sampleType, xml, testUnitIds);

        ajaxServlet.sendData(xml.toString(), result, request, response);
    }

    private String createSearchResultXML(String sampleType, StringBuilder xml, List<String> testUnitIds) {

        String success = VALID;

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

        if (isVariableTypeOfSample) {
            xml.append("<variableSampleType/>");
        }
        XMLUtil.appendKeyValue("sampleTypeId", StringUtil.snipToMaxIdLength(sampleType), xml);
        addTests(tests, xml);

        List<TypeOfSamplePanel> panelList = getPanelList(sampleType);
        List<PanelTestMap> panelMap = linkTestsToPanels(panelList, tests);

        addPanels(panelMap, xml);

        return success;
    }

    private void addTests(List<Test> tests, StringBuilder xml) {
        xml.append("<tests>");
        for (Test test : tests) {
            addTest(test, xml);
        }

        xml.append("</tests>");
    }

    private void addTest(Test test, StringBuilder xml) {
        xml.append("<test>");
        XMLUtil.appendKeyValue("name", TestServiceImpl.getUserLocalizedTestName(test), xml);
        XMLUtil.appendKeyValue("id", test.getId(), xml);
        XMLUtil.appendKeyValue("userBenchChoice",
                String.valueOf(USER_TEST_SECTION_ID.equals(test.getTestSection().getId())), xml);
        if (isVariableTypeOfSample) {
            addVariableSampleTypes(test, xml);
        }
        xml.append("</test>");
    }

    private void addVariableSampleTypes(Test test, StringBuilder xml) {
        TestDictionary testDictionary = testDictionaryService.getTestDictionaryForTestId(test.getId());
        if (testDictionary == null) {
            return;
        }
        List<IdValuePair> pairs = DisplayListService.getInstance()
                .getDictionaryListByCategory(testDictionary.getDictionaryCategory().getCategoryName());
        xml.append("<variableSampleTypes ");
        if (!GenericValidator.isBlankOrNull(testDictionary.getQualifiableDictionaryId())) {
            XMLUtil.appendAttributeKeyValue("qualifiableId", testDictionary.getQualifiableDictionaryId(), xml);
        }
        xml.append(" >");
        for (IdValuePair pair : pairs) {
            xml.append("<type ");
            XMLUtil.appendAttributeKeyValue("id", pair.getId(), xml);
            XMLUtil.appendAttributeKeyValue("name", pair.getValue(), xml);
            xml.append(" />");
        }
        xml.append("</variableSampleTypes>");
    }

    private void addPanels(List<PanelTestMap> panelMap, StringBuilder xml) {
        panelMap = sortPanels(panelMap);

        xml.append("<panels>");
        for (PanelTestMap testMap : panelMap) {
            addPanel(testMap, xml);
        }
        xml.append("</panels>");
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

    private void addPanel(PanelTestMap testMap, StringBuilder xml) {
        xml.append("<panel>");
        XMLUtil.appendKeyValue("name", testMap.getName(), xml);
        XMLUtil.appendKeyValue("id", testMap.getPanelId(), xml);
        XMLUtil.appendKeyValue("testIds", testMap.getTestIds(), xml);
        xml.append("</panel>");
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
                Integer index = testIdOrderMap.get(derivedNameFromPanel);

                if (index != null) {
                    indexes.append(index.toString());
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

    public class PanelTestMap {
        private String name;
        private String testIds;
        private String panelId;
        private int panelOrder;

        public PanelTestMap(String panelId, int panelOrder, String panelName, String testIds) {
            name = panelName;
            this.testIds = testIds;
            this.panelId = panelId;
            this.panelOrder = panelOrder;
        }

        public String getName() {
            return name;
        }

        public String getTestIds() {
            return testIds;
        }

        public String getPanelId() {
            return panelId;
        }

        public int getPanelOrder() {
            return panelOrder;
        }
    }
}
