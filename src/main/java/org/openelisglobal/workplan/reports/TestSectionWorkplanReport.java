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
import java.util.HashMap;
import java.util.List;
import net.sf.jasperreports.engine.JRParameter;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.workplan.form.WorkplanForm;

public class TestSectionWorkplanReport implements IWorkplanReport {
    private static int PREFIX_LENGTH = AccessionNumberUtil.getMainAccessionNumberGenerator().getInvarientLength();
    private static final String BASE_FILE_NAME = "WorkplanByTestSection";
    private static final String FILE_NAME_WITH_RESULTS = "WorkplanResultsByTestSection";
    private final HashMap<String, Object> parameterMap = new HashMap<>();
    private String testSection = "";
    private String messageKey = "banner.menu.workplan.";
    protected String reportPath = "";

    public TestSectionWorkplanReport(String testSection) {
        // messageKey = messageKey + testSection.replace(' ', '-');
        // this.testSection = MessageUtil.getContextualMessage(messageKey);

        if (GenericValidator.isBlankOrNull(this.testSection)) {
            this.testSection = testSection;
        }
    }

    @Override
    public String getFileName() {
        return ConfigurationProperties.getInstance().isPropertyValueEqual(Property.RESULTS_ON_WORKPLAN, "false")
                ? BASE_FILE_NAME
                : FILE_NAME_WITH_RESULTS;
    }

    @Override
    public HashMap<String, Object> getParameters() {
        parameterMap.put("testSection", testSection);
        parameterMap.put("printSubjectNo",
                ConfigurationProperties.getInstance().isPropertyValueEqual(Property.SUBJECT_ON_WORKPLAN, "true"));
        parameterMap.put("printNextVisit", ConfigurationProperties.getInstance()
                .isPropertyValueEqual(Property.NEXT_VISIT_DATE_ON_WORKPLAN, "true"));
        parameterMap.put("labNumberTitle", MessageUtil.getContextualMessage("quick.entry.accession.number"));
        parameterMap.put("subjectNoTitle", MessageUtil.getContextualMessage("patient.subject.number"));
        parameterMap.put("nameOfTest", getNameOfTest());
        parameterMap.put("nameOfPatient", getNameOfPatient());
        parameterMap.put("labName", ConfigurationProperties.getInstance().getPropertyValue(Property.SiteName));
        parameterMap.put("accessionPrefix", AccessionNumberUtil.getMainAccessionNumberGenerator().getPrefix());
        parameterMap.put("prefixLength", PREFIX_LENGTH);
        parameterMap.put("SUBREPORT_DIR", reportPath);
        parameterMap.put("receptionDate", MessageUtil.getMessage("report.receptionDate"));
        parameterMap.put("workPlan", MessageUtil.getMessage("report.workPlan"));
        parameterMap.put("appointmentDate", MessageUtil.getMessage("report.appointmentDate"));
        parameterMap.put("testName", MessageUtil.getMessage("report.testName"));
        parameterMap.put("date", MessageUtil.getMessage("report.date"));
        parameterMap.put("from", MessageUtil.getMessage("report.from"));
        parameterMap.put("appointment", MessageUtil.getMessage("report.appointment"));
        parameterMap.put("about", MessageUtil.getMessage("report.about"));
        parameterMap.put(JRParameter.REPORT_RESOURCE_BUNDLE, MessageUtil.getMessageSourceAsResourceBundle());

        return parameterMap;
    }

    @Override
    public List<?> prepareRows(WorkplanForm form) {

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
    public void setReportPath(String reportPath) {
        this.reportPath = reportPath;
    }

    protected String getNameOfTest() {
        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.configurationName, "Haiti LNSP")) {
            return MessageUtil.getContextualMessage("sample.entry.project.patientName.testName");
        } else {
            return MessageUtil.getContextualMessage("sample.entry.project.testName");
        }
    }

    protected String getNameOfPatient() {
        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.configurationName, "Haiti LNSP")) {
            return MessageUtil.getContextualMessage("patient.name");
        } else {
            return null;
        }
    }
}
