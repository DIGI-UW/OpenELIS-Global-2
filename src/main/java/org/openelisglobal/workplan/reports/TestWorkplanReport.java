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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.workplan.reports;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.workplan.form.WorkplanForm;

public class TestWorkplanReport implements IWorkplanReport {

    private String testName = "";

    public TestWorkplanReport(String testType) {
        testName = testType;
    }

    protected String getNameOfPatient() {
        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.configurationName, "Haiti LNSP")) {
            return MessageUtil.getContextualMessage("sample.entry.project.patientName.code");
        } else {
            return null;
        }
    }

    @Override
    public List<TestResultItem> prepareRows(WorkplanForm form) {

        List<TestResultItem> workplanTests = form.getWorkplanTests();

        // remove unwanted tests from workplan
        List<TestResultItem> includedTests = new ArrayList<>();
        for (TestResultItem test : workplanTests) {
            if (!test.isNotIncludedInWorkplan()) {
                includedTests.add(test);
            } else {
                // handles the case that the checkbox is unchecked
                test.setNotIncludedInWorkplan(false);
            }
        }
        return includedTests;
    }

    @Override
    public byte[] renderPdf(List<TestResultItem> rows) {
        return WorkplanPdf.render(MessageUtil.getMessage("report.workPlan") + ": " + testName, rows, false,
                getNameOfPatient());
    }

}
