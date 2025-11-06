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

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;

public abstract class RetroCIReport extends Report implements IReportCreator {

    protected static final String ANTIRETROVIRAL_STUDY = "Antiretroviral Study";
    protected static final String ANTIRETROVIRAL_FOLLOW_UP_STUDY = "Antiretroviral Followup Study";
    protected static final String VL_STUDY = "Viral Load Results";
    protected static final String EID_STUDY = "Early Infant Diagnosis for HIV Study";
    protected static final String INDETERMINATE_STUDY = "Indeterminate Results";
    protected static final String SPECIAL_REQUEST_STUDY = "Special Request";
    protected static String ANTIRETROVIRAL_STUDY_ID;
    protected static String ANTIRETROVIRAL_FOLLOW_UP_STUDY_ID;
    protected static String EID_STUDY_ID;
    protected static String VL_STUDY_ID;
    protected static String SPECIAL_REQUEST_STUDY_ID;
    protected static String INDETERMINATE_STUDY_ID;
    protected static String OBSERVATION_DOCTOR_ID;
    protected static String OBSERVATION_REQUESTOR_ID;
    protected static String OBSERVATION_HOSPITAL_ID;
    protected static String OBSERVATION_SERVICE_ID;
    protected static String OBSERVATION_PROJECT_ID;
    protected static String OBSERVATION_WHICH_PCR_ID;
    protected static String OBSERVATION_UNDER_INVESTIGATION_ID;
    protected static List<Integer> ANTIRETROVIRAL_ID = new ArrayList<>();

    protected static String CONCLUSION_ID;
    protected static String CD4_CNT_CONCLUSION;

    protected static ObservationHistoryService observationHistoryService = SpringContext
            .getBean(ObservationHistoryService.class);

    static {
        ObservationHistoryTypeService observationTypeService = SpringContext
                .getBean(ObservationHistoryTypeService.class);
        ObservationHistoryType observationType = observationTypeService.getByName("nameOfDoctor");
        OBSERVATION_DOCTOR_ID = observationType.getId();
        observationType = observationTypeService.getByName("nameOfRequestor");
        OBSERVATION_REQUESTOR_ID = observationType.getId();
        observationType = observationTypeService.getByName("hospital");
        OBSERVATION_HOSPITAL_ID = observationType.getId();
        observationType = observationTypeService.getByName("service");
        OBSERVATION_SERVICE_ID = observationType.getId();
        observationType = observationTypeService.getByName("projectFormName");
        OBSERVATION_PROJECT_ID = observationType.getId();
        observationType = observationTypeService.getByName("whichPCR");
        OBSERVATION_WHICH_PCR_ID = observationType.getId();
        observationType = observationTypeService.getByName("underInvestigation");
        OBSERVATION_UNDER_INVESTIGATION_ID = observationType.getId();

        AnalyteService analyteService = SpringContext.getBean(AnalyteService.class);
        Analyte analyte = new Analyte();
        analyte.setAnalyteName("Conclusion");
        analyte = analyteService.getAnalyteByName(analyte, false);
        CONCLUSION_ID = analyte.getId();
        analyte.setAnalyteName("generated CD4 Count");
        analyte = analyteService.getAnalyteByName(analyte, false);
        CD4_CNT_CONCLUSION = analyte.getId();

        ProjectService projectService = SpringContext.getBean(ProjectService.class);
        List<Project> projectList = projectService.getAllProjects();

        for (Project project : projectList) {
            if (ANTIRETROVIRAL_STUDY.equals(project.getProjectName())) {
                ANTIRETROVIRAL_STUDY_ID = project.getId();
            } else if (ANTIRETROVIRAL_FOLLOW_UP_STUDY.equals(project.getProjectName())) {
                ANTIRETROVIRAL_FOLLOW_UP_STUDY_ID = project.getId();
            } else if (VL_STUDY.equals(project.getProjectName())) {
                VL_STUDY_ID = project.getId();
            } else if (EID_STUDY.equals(project.getProjectName())) {
                EID_STUDY_ID = project.getId();
            } else if (INDETERMINATE_STUDY.equals(project.getProjectName())) {
                INDETERMINATE_STUDY_ID = project.getId();
            } else if (SPECIAL_REQUEST_STUDY.equals(project.getProjectName())) {
                SPECIAL_REQUEST_STUDY_ID = project.getId();
            }
        }
        ANTIRETROVIRAL_ID.add(Integer.parseInt(ANTIRETROVIRAL_STUDY_ID));
        ANTIRETROVIRAL_ID.add(Integer.parseInt(ANTIRETROVIRAL_FOLLOW_UP_STUDY_ID));
        ANTIRETROVIRAL_ID.add(Integer.parseInt(VL_STUDY_ID));
    }

    /**
     * @see org.openelisglobal.reports.action.implementation.IReportCreator#initializeReport(org.openelisglobal.common.action.BaseActionForm)
     */
    @Override
    public abstract void initializeReport(ReportForm form);
}
