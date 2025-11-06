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
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.workplan.form.WorkplanForm;

public class TestWorkplanReport implements IWorkplanReport {

    private static int PREFIX_LENGTH = AccessionNumberUtil.getMainAccessionNumberGenerator().getInvarientLength();
    private static final String BASE_FILE_NAME = "WorkplanByTest";
    private static final String RESULT_FILE_NAME = "WorkplanResultsByTest";
    private final HashMap<String, Object> parameterMap = new HashMap<>();
    private String testName = "";
    protected String reportPath = "";

    public TestWorkplanReport(String testType) {
        testName = testType;
    }

    @Override
    public String getFileName() {
        return ConfigurationProperties.getInstance().isPropertyValueEqual(Property.RESULTS_ON_WORKPLAN, "false")
                ? BASE_FILE_NAME
                : RESULT_FILE_NAME;
    }

    @Override
    public HashMap<String, Object> getParameters() {
        parameterMap.put("testName", testName);
        parameterMap.put("printSubjectNo",
                ConfigurationProperties.getInstance().isPropertyValueEqual(Property.SUBJECT_ON_WORKPLAN, "true"));
        parameterMap.put("printNextVisit", ConfigurationProperties.getInstance()
                .isPropertyValueEqual(Property.NEXT_VISIT_DATE_ON_WORKPLAN, "true"));
        parameterMap.put("labNumberTitle", MessageUtil.getContextualMessage("quick.entry.accession.number"));
        parameterMap.put("subjectNoTitle", MessageUtil.getContextualMessage("patient.subject.number"));
        parameterMap.put("nameOfPatient", getNameOfPatient());
        parameterMap.put("labName", ConfigurationProperties.getInstance().getPropertyValue(Property.SiteName));
        parameterMap.put("accessionPrefix", AccessionNumberUtil.getMainAccessionNumberGenerator().getPrefix());
        parameterMap.put("prefixLength", PREFIX_LENGTH);
        parameterMap.put("SUBREPORT_DIR", reportPath);
        parameterMap.put("receptionDate", MessageUtil.getMessage("report.receptionDate"));
        parameterMap.put("workPlan", MessageUtil.getMessage("report.workPlan"));
        parameterMap.put("appointmentDate", MessageUtil.getMessage("report.appointmentDate"));
        parameterMap.put(JRParameter.REPORT_RESOURCE_BUNDLE, MessageUtil.getMessageSourceAsResourceBundle());

        return parameterMap;
    }

    protected String getNameOfPatient() {
        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.configurationName, "Haiti LNSP")) {
            return MessageUtil.getContextualMessage("sample.entry.project.patientName.code");
        } else {
            return null;
        }
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
}
