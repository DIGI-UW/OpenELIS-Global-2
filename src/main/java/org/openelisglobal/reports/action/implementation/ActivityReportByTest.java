/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package org.openelisglobal.reports.action.implementation;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.result.service.ResultServiceImpl;
import org.openelisglobal.result.valueholder.Result;

/** */
public class ActivityReportByTest extends ActivityReport implements IReportCreator, IReportParameterSetter {
    private String testName = "";

    @Override
    protected boolean isReportByTest() {
        return true;
    }

    @Override
    public void setRequestParameters(ReportForm form) {
        new ReportSpecificationParameters(ReportSpecificationParameters.Parameter.DATE_RANGE,
                MessageUtil.getMessage("report.activity.report.base") + " " + MessageUtil.getMessage("report.by.test"),
                MessageUtil.getMessage("report.instruction.all.fields")).setRequestParameters(form);
        new ReportSpecificationList(DisplayListService.getInstance().getList(DisplayListService.ListType.ALL_TESTS),
                MessageUtil.getMessage("workplan.test.types")).setRequestParameters(form);
    }

    @Override
    protected void buildReportContent(ReportSpecificationList testSelection) {
        String selection = testSelection.getSelection();
        if (testSelection.getList().isEmpty()) {
            testSelection = new ReportSpecificationList(
                    DisplayListService.getInstance().getList(DisplayListService.ListType.ALL_TESTS),
                    MessageUtil.getMessage("workplan.test.types"));
            testSelection.setSelection(selection);
            testName = testSelection.getSelectionAsName();
        } else {
            testName = testSelection.getSelectionAsName();
        }
        createReportParameters();

        // do not print the separator bar between name/Id and tests
        reportParameters.put("underlineResults", false);

        List<Result> resultList = ResultServiceImpl.getResultsInTimePeriodWithTest(dateRange.getLowDate(),
                dateRange.getHighDate(), testSelection.getSelection());
        testsResults = new ArrayList<>(resultList.size());

        String currentAnalysisId = "-1";
        for (Result result : resultList) {
            if (result.getAnalysis() != null && result.getAnalysis().getId() != null) {
                if (!currentAnalysisId.equals(result.getAnalysis().getId())) {
                    testsResults.add(createActivityReportBean(result, false));
                    // System.out.println("ActivityReport:" + "in buildReportContent " +
                    // result.getStringId());
                    currentAnalysisId = result.getAnalysis().getId();
                }
            }
        }
    }

    @Override
    protected String getActivityLabel() {
        return "Test: " + testName;
    }
}
