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
public class RejectionReportByTestSection extends RejectionReport implements IReportParameterSetter {
    private String unitName;

    @Override
    public void setRequestParameters(ReportForm form) {
        new ReportSpecificationParameters(ReportSpecificationParameters.Parameter.DATE_RANGE,
                MessageUtil.getMessage("report.rejection.report.base") + " " + MessageUtil.getMessage("report.by.unit"),
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
    protected void buildReportContent(ReportSpecificationList panelSelection) {

        unitName = getNameForId(panelSelection);
        createReportParameters();

        rejections = new ArrayList<>();
        ArrayList<RejectionReportBean> rawResults = new ArrayList<>();

        List<Note> testRejectionNotes = SpringContext.getBean(NoteService.class).getTestNotesInDateRangeByType(
                dateRange.getLowDate(), dateRange.getHighDate(), NoteServiceImpl.NoteType.REJECTION_REASON);

        Collections.sort(testRejectionNotes, new Comparator<Note>() {
            @Override
            public int compare(Note o1, Note o2) {
                return o1.getReferenceId().compareTo(o2.getReferenceId());
            }
        });

        Analysis currentAnalysis = new Analysis();
        String noteText = null;
        for (Note note : testRejectionNotes) {
            Analysis analysis = SpringContext.getBean(AnalysisService.class).get(note.getReferenceId());
            if (analysis != null && analysis.getTestSection() != null && panelSelection.getSelection() != null
                    && panelSelection.getSelection().equals(analysis.getTestSection().getId())) {
                if (analysis.getId().equals(currentAnalysis.getId())) {
                    noteText += (noteText != null ? "<br/>" : "") + note.getText();
                } else {
                    if (noteText != null) {
                        rawResults.add(createRejectionReportBean(noteText, currentAnalysis, true));
                    }
                    noteText = note.getText();
                }
                currentAnalysis = analysis;
            }
        }

        // pick up last rejection note
        if (noteText != null) {
            rawResults.add(createRejectionReportBean(noteText, currentAnalysis, true));
        }

        injectPatientLineAndCopyToFinalList(rawResults);
    }
}
