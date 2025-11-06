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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.action.implementation.reportBeans.ActivityReportBean;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.result.service.ResultServiceImpl;
import org.openelisglobal.result.valueholder.Result;

/** */
public class ActivityReportByTestSection extends ActivityReport implements IReportCreator, IReportParameterSetter {
    private String unitName;

    @Override
    public void setRequestParameters(ReportForm form) {
        new ReportSpecificationParameters(ReportSpecificationParameters.Parameter.DATE_RANGE,
                MessageUtil.getMessage("report.activity.report.base") + " " + MessageUtil.getMessage("report.by.unit"),
                MessageUtil.getMessage("report.instruction.all.fields")).setRequestParameters(form);
        new ReportSpecificationList(
                DisplayListService.getInstance().getList(DisplayListService.ListType.TEST_SECTION_ACTIVE),
                MessageUtil.getMessage("workplan.unit.types")).setRequestParameters(form);
    }

    @Override
    protected String getActivityLabel() {
        return MessageUtil.getMessage("report.unit") + ": " + unitName;
    }

    @Override
    protected void buildReportContent(ReportSpecificationList unitSelection) {
        String selection = unitSelection.getSelection();
        if (unitSelection.getList().isEmpty()) {
            unitSelection = new ReportSpecificationList(
                    DisplayListService.getInstance().getList(DisplayListService.ListType.TEST_SECTION_ACTIVE),
                    MessageUtil.getMessage("workplan.unit.types"));
            unitSelection.setSelection(selection);
            unitName = unitSelection.getSelectionAsName();
        } else {
            unitName = unitSelection.getSelectionAsName();
        }
        createReportParameters();

        List<Result> resultList = ResultServiceImpl.getResultsInTimePeriodInTestSection(dateRange.getLowDate(),
                dateRange.getHighDate(), unitSelection.getSelection());
        ArrayList<ActivityReportBean> rawResults = new ArrayList<>(resultList.size());
        testsResults = new ArrayList<>();

        String currentAnalysisId = "-1";
        for (Result result : resultList) {
            if (result.getAnalysis() != null && result.getAnalysis().getId() != null) {
                if (!currentAnalysisId.equals(result.getAnalysis().getId())) {
                    rawResults.add(createActivityReportBean(result, true));
                    currentAnalysisId = result.getAnalysis().getId();
                }
            }
        }

        Collections.sort(rawResults, new Comparator<ActivityReportBean>() {
            @Override
            public int compare(ActivityReportBean o1, ActivityReportBean o2) {
                return o1.getAccessionNumber().compareTo(o2.getAccessionNumber());
            }
        });

        String currentAccessionNumber = "";
        for (ActivityReportBean item : rawResults) {
            if (!currentAccessionNumber.equals(item.getAccessionNumber())) {
                testsResults.add(createIdentityActivityBean(item, true));
                currentAccessionNumber = item.getAccessionNumber();
            }
            testsResults.add(item);
        }
    }
}
