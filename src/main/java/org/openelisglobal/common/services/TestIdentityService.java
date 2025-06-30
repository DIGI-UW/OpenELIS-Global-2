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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TestIdentityService implements ITestIdentityService {

    private static ITestIdentityService instance;

    @Autowired
    private TestService testService;
    @Autowired
    private PanelService panelService;
    @Autowired
    private TestResultService testResultService;

    private String VIRAL_LOAD_TEST_ID = null;
    private Boolean VIRAL_LOAD_IS_NUMERIC = Boolean.TRUE;

    @PostConstruct
    private void registerInstance() {
        instance = this;
    }

    @PostConstruct
    public void initialize() {
        Test test = testService.getTestByDescription("VIH-1 Charge Virale(Plasma)");

        if (test != null) {
            VIRAL_LOAD_TEST_ID = test.getId();
        } else {
            test = testService.getTestByDescription("Charge Virale(Plasma)");
            if (test != null) {
                VIRAL_LOAD_TEST_ID = test.getId();
            } else {
                test = testService.getTestByLocalizedName("Viral Load", Locale.ENGLISH);
                if (test != null) {
                    VIRAL_LOAD_TEST_ID = test.getId();
                }
            }
        }

        if (!GenericValidator.isBlankOrNull(VIRAL_LOAD_TEST_ID)) {
            List<TestResult> testResultList = testResultService.getActiveTestResultsByTest(VIRAL_LOAD_TEST_ID);
            VIRAL_LOAD_IS_NUMERIC = !testResultList.isEmpty() && ("N".equals(testResultList.get(0).getTestResultType())
                    || "A".equals(testResultList.get(0).getTestResultType()));
        } else {
            VIRAL_LOAD_IS_NUMERIC = Boolean.FALSE;
        }
    }

    public static ITestIdentityService getInstance() {
        return instance;
    }

    @Override
    public boolean isTestNumericViralLoad(Test test) {
        return isTestNumericViralLoad(test.getId());
    }

    @Override
    public boolean isTestNumericViralLoad(String testID) {
        return VIRAL_LOAD_IS_NUMERIC && (VIRAL_LOAD_TEST_ID != null && VIRAL_LOAD_TEST_ID.equals(testID));
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.common.services.ITestIdentityService#doesTestExist(
     * java.lang.String)
     */
    @Override
    public boolean doesTestExist(String name) {
        List<Test> tests = testService.getTestsByName(name);
        return (tests != null && !tests.isEmpty());
    }

    @Override
    public boolean doesActiveTestExist(String name) {
        List<Test> tests = testService.getActiveTestsByName(name);
        return (tests != null && !tests.isEmpty());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.common.services.ITestIdentityService#doesPanelExist(
     * java.lang.String)
     */
    @Override
    public boolean doesPanelExist(String name) {
        return panelService.getIdForPanelName(name) != null;
    }

    @Override
    public boolean doesTestExistForLoinc(String loincCode) {
        return testService.getTestsByLoincCode(loincCode) != null
                && testService.getTestsByLoincCode(loincCode).size() > 0;
    }

    @Override
    public boolean doesActiveTestExistForLoinc(String loincCode) {
        return testService.getActiveTestsByLoinc(loincCode) != null
                && testService.getActiveTestsByLoinc(loincCode).size() > 0;
    }

    @Override
    public boolean doesActivePanelExistForLoinc(String loincCode) {
        return panelService.getAllActivePanels().stream().anyMatch(e -> Objects.equals(e.getLoinc(), loincCode));
    }
}
