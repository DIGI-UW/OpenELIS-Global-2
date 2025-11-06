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

import static org.apache.commons.validator.GenericValidator.isBlankOrNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.jfree.util.Log;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder;
import org.openelisglobal.reports.action.implementation.reportBeans.ForCIDashboardColumnBuilder;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;

/**
 * @author Paul A. Hill (pahill@uw.edu)
 * @since Jan 26, 2011
 */
public class ForCIDashboard extends CSVSampleExportReport implements IReportParameterSetter, IReportCreator {
    protected final ProjectService projectService = SpringContext.getBean(ProjectService.class);
    private String projectStr;
    private Project project;
    private String indicStr;
    protected final SimpleDateFormat postgresDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    // private String indicLabel;

    @Override
    protected String reportFileName() {
        // return indicLabel;
        return "ForCIDashboard";
    }

    @Override
    public void setRequestParameters(ReportForm form) {
        try {
            form.setReportName(getReportNameForParameterPage());
            form.setUseLowerDateRange(Boolean.TRUE);
            form.setUseUpperDateRange(Boolean.TRUE);
            // form.setUseProjectCode(Boolean.TRUE);
            form.setUseDashboard(Boolean.TRUE);
            form.setProjectCodeList(getProjectList());
        } catch (RuntimeException e) {
            Log.error("Error in CIDashboard.setRequestParemeters: ", e);
        }
    }

    protected String getReportNameForParameterPage() {
        return MessageUtil.getMessage("reports.label.project.export") + " " + "Date d'impression du rapport";
        // MessageUtil.getContextualMessage("sample.collectionDate");
    }

    @Override
    protected void createReportParameters() {
        super.createReportParameters();
        reportParameters.put("studyName", (project == null) ? null : project.getLocalizedName());
    }

    @Override
    public void initializeReport(ReportForm form) {
        super.initializeReport();
        errorFound = false;

        indicStr = form.getVlStudyType();

        lowDateStr = form.getLowerDateRange();
        highDateStr = form.getUpperDateRange();
        projectStr = form.getVlStudyType();
        dateRange = new DateRange(lowDateStr, highDateStr);
        String[] splitline = form.getVlStudyType().split(":");

        projectStr = splitline[0];
        // indicLabel = splitline[1];
        createReportParameters();

        errorFound = !validateSubmitParameters();
        if (errorFound) {
            return;
        }

        createReportItems();
    }

    /** check everything */
    private boolean validateSubmitParameters() {
        return dateRange.validateHighLowDate("report.error.message.date.received.missing") && validateProject();
    }

    /**
     * @return true, if location is not blank or "0" is is found in the DB; false
     *         otherwise
     */
    private boolean validateProject() {
        if (isBlankOrNull(projectStr) || "0".equals(Integer.getInteger(projectStr).toString())) {
            add1LineErrorMessage("report.error.message.project.missing");
            return false;
        }
        project = projectService.getProjectById(projectStr);
        if (project == null) {
            add1LineErrorMessage("report.error.message.project.missing");
            return false;
        }
        return true;
    }

    /** creating the list for generation to the report */
    private void createReportItems() {
        try {
            csvColumnBuilder = getColumnBuilder();
            csvColumnBuilder.buildDataSource();
        } catch (SQLException e) {
            Log.error("Error in " + this.getClass().getSimpleName() + ".createReportItems: ", e);
            add1LineErrorMessage("report.error.message.general.error");
        }
    }

    @Override
    protected void writeResultsToBuffer(ByteArrayOutputStream buffer) throws IOException, SQLException, ParseException {

        String currentAccessionNumber = null;
        String[] splitBase = {};
        while (csvColumnBuilder.next()) {
            String line = csvColumnBuilder.nextLine();
            String[] splitLine = line.split(",");

            if (splitLine[0].equals(currentAccessionNumber)) {
                merge(splitBase, splitLine);
            } else {
                if (currentAccessionNumber != null && writeAble(splitBase[16].trim())) {

                    writeConsolidatedBaseToBuffer(buffer, splitBase);
                }
                splitBase = splitLine;
                currentAccessionNumber = splitBase[0];
            }
        }
        if (writeAble(splitBase[16].trim())) {
            writeConsolidatedBaseToBuffer(buffer, splitBase);
        }
    }

    private boolean writeAble(String result) {

        String workingResult = result.split("\\(")[0].trim();
        // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", "result="
        // +
        // result + " / workingResult= " +
        // workingResult);
        String[] splitLine = indicStr.split(":");
        String indic = splitLine[1];
        if (indic.equals("Unsuppressed VL")) {
            return workingResult.contains("Log7")
                    || !workingResult.contains("L") && !workingResult.contains("X") && !workingResult.contains("<")
                            && workingResult.length() > 0 && Double.parseDouble(workingResult) >= 1000; // workingResult.length()>=4
            // &&
        } else if (indic.equals("Suppressed VL")) {
            return workingResult.contains("L") || workingResult.contains("<") || (workingResult.length() > 0
                    && !workingResult.contains("X") && Double.parseDouble(workingResult) < 1000);
        }

        return false;
    }

    private void merge(String[] base, String[] line) {
        for (int i = 0; i < base.length; ++i) {
            if (GenericValidator.isBlankOrNull(base[i])) {
                base[i] = line[i];
            }
        }
    }

    protected void writeConsolidatedBaseToBuffer(ByteArrayOutputStream buffer, String[] splitBase) throws IOException {

        if (splitBase != null) {
            int splitBaseNumChars = StringUtil.countChars(splitBase);
            StringBuilder consolidatedLine = new StringBuilder(splitBaseNumChars + splitBase.length);
            for (String value : splitBase) {
                consolidatedLine.append(value);
                consolidatedLine.append(",");
            }

            consolidatedLine.deleteCharAt(consolidatedLine.lastIndexOf(","));
            buffer.write(consolidatedLine.toString().getBytes("windows-1252"));
        }
    }

    private CSVColumnBuilder getColumnBuilder() {
        // String projectTag = CIColumnBuilder.translateProjectId(projectId);
        return new ForCIDashboardColumnBuilder(dateRange, projectStr);
    }

    /*
     * if (projectTag.equals("ARVB")) { return new
     * ARVInitialColumnBuilder(dateRange, projectStr); } else if
     * (projectTag.equals("ARVS")) { return new ARVFollowupColumnBuilder(dateRange,
     * projectStr); } else if (projectTag.equalsIgnoreCase("DBS")) { return new
     * EIDColumnBuilder(dateRange, projectStr); } else if
     * (projectTag.equalsIgnoreCase("VLS")) { return new VLColumnBuilder(dateRange,
     * projectStr); } else if (projectTag.equalsIgnoreCase("RTN")) { return new
     * RTNColumnBuilder(dateRange, projectStr); } else if
     * (projectTag.equalsIgnoreCase("IND")) { return new RTNColumnBuilder(dateRange,
     * projectStr); } throw new IllegalArgumentException(); }
     *
     *
     *
     *
     *
     *
     * /**
     *
     * @return a list of the correct projects for display
     */
    protected List<Project> getProjectList() {
        List<Project> projects = new ArrayList<>();
        Project curProject = new Project();

        // project.setProjectName("Antiretroviral Study");
        // projects.add(projectService.getProjectByName(project, false, false));
        // project.setProjectName("Antiretroviral Followup Study");
        // projects.add(projectService.getProjectByName(project, false, false));
        // project.setProjectName("Routine HIV Testing");
        // projects.add(projectService.getProjectByName(project, false, false));
        // project.setProjectName("Early Infant Diagnosis for HIV Study");
        // projects.add(projectService.getProjectByName(project, false, false));
        // project.setProjectName("Viral Load Results");
        // projects.add(projectService.getProjectByName(project, false, false));
        // project.setProjectName("Indeterminate Results");
        // projects.add(projectService.getProjectByName(project, false, false));

        curProject.setId("28:Unsuppressed VL");
        curProject.setProjectName("Unsuppressed VL");
        projects.add(project);
        curProject = new Project();
        curProject.setId("28:Suppressed VL");
        curProject.setProjectName("Suppressed VL");
        projects.add(project);

        return projects;
    }
}
