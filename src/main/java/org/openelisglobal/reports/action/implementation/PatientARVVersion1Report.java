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
package org.openelisglobal.reports.action.implementation;

import java.util.List;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;

public class PatientARVVersion1Report extends PatientARVReport implements IReportCreator {

    @Override
    protected void createReportParameters() {
        super.createReportParameters();
        reportParameters.put("showSerologie", Boolean.FALSE);
        reportParameters.put("showVirologie", Boolean.TRUE);
    }

    @Override
    protected String reportFileName() {
        return "Patient_ARV_Version1";
    }

    @Override
    protected String getReportNameForReport() {
        // assume that we'll not get this
        return "Bilan-Suivi";
    }

    @Override
    protected boolean allowSample() {
        List<ObservationHistory> historyList = observationHistoryService.getAll(reportPatient, reportSample,
                OBSERVATION_PROJECT_ID);

        for (ObservationHistory history : historyList) {
            if ("FollowUpARV_Id".equals(history.getValue())) {
                reportParameters.put("studyName", MessageUtil.getMessage("reports.label.patient.ARV.followup"));
                return true;
            } else if ("InitialARV_Id".equals(history.getValue())) {
                reportParameters.put("studyName", MessageUtil.getMessage("reports.label.patient.ARV.initial"));
                return true;
            } else if ("VL_Id".equals(history.getValue())) {
                reportParameters.put("studyName", MessageUtil.getMessage("reports.label.patient.VL"));
                return true;
            }
        }

        return false;
    }

    @Override
    protected String getProjectId() {
        return ANTIRETROVIRAL_STUDY_ID + ":" + ANTIRETROVIRAL_FOLLOW_UP_STUDY_ID + ":" + VL_STUDY_ID;
    }
}
