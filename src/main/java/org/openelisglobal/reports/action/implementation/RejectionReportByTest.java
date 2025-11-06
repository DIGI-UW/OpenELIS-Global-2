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
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.reports.action.implementation.reportBeans.RejectionReportBean;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;

/** */
public class RejectionReportByTest extends RejectionReport implements IReportParameterSetter {
    private String testName = "";

    @Override
    public void setRequestParameters(ReportForm form) {
        new ReportSpecificationParameters(ReportSpecificationParameters.Parameter.DATE_RANGE,
                MessageUtil.getMessage("report.rejection.report.base") + " " + MessageUtil.getMessage("report.by.test"),
                MessageUtil.getMessage("report.instruction.all.fields")).setRequestParameters(form);
        new ReportSpecificationList(DisplayListService.getInstance().getList(DisplayListService.ListType.ALL_TESTS),
                MessageUtil.getMessage("workplan.test.types")).setRequestParameters(form);
    }

    @Override
    protected void buildReportContent(ReportSpecificationList testSelection) {

        testName = getNameForId(testSelection);
        createReportParameters();

        rejections = new ArrayList<>();
        List<Note> testRejectionNotes = SpringContext.getBean(NoteService.class).getTestNotesInDateRangeByType(
                dateRange.getLowDate(), dateRange.getHighDate(), NoteServiceImpl.NoteType.REJECTION_REASON);

        Analysis currentAnalysis = new Analysis();
        String noteText = null;
        for (Note note : testRejectionNotes) {
            Analysis analysis = SpringContext.getBean(AnalysisService.class).get(note.getReferenceId());
            if (analysis != null && testSelection.getSelection().equals(analysis.getTest().getId())) {
                if (analysis.getId().equals(currentAnalysis.getId())) {
                    noteText += (noteText != null ? "<br/>" : "") + note.getText();
                } else {
                    if (noteText != null) {
                        rejections.add(createRejectionReportBean(noteText, currentAnalysis, false));
                    }
                    noteText = note.getText();
                }
                currentAnalysis = analysis;
            }
        }

        // pick up last rejection note
        if (noteText != null) {
            rejections.add(createRejectionReportBean(noteText, currentAnalysis, false));
        }

        Collections.sort(rejections, new Comparator<RejectionReportBean>() {
            @Override
            public int compare(RejectionReportBean o1, RejectionReportBean o2) {
                return o1.getAccessionNumber().compareTo(o2.getAccessionNumber());
            }
        });
    }

    @Override
    protected boolean isReportByTest() {
        return Boolean.TRUE;
    }

    @Override
    protected String getActivityLabel() {
        return "Test: " + testName;
    }
}
