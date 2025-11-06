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
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.jfree.util.Log;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.project.dao.ProjectDAO;
import org.openelisglobal.project.daoimpl.ProjectDAOImpl;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder;
import org.openelisglobal.reports.action.implementation.reportBeans.TrendsColumnBuilder;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;

/**
 * @author Paul A. Hill (pahill@uw.edu)
 * @since Jan 26, 2011
 */
public class ExportTrendsByDate extends CSVSampleExportReport implements IReportParameterSetter, IReportCreator {
    protected final ProjectDAO projectDAO = new ProjectDAOImpl();
    private String projectStr;
    private Project project;
    private String indicStr;
    protected static final SimpleDateFormat postgresDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    // private String indicLabel;

    // @Override
    @Override
    protected String reportFileName() {
        // return indicLabel;
        return "ExportTrendsByDate";
    }

    @Override
    public void setRequestParameters(ReportForm form) {
        form.setReportName(getReportNameForParameterPage());
        form.setUseLowerDateRange(Boolean.TRUE);
        form.setUseUpperDateRange(Boolean.TRUE);
        // PropertyUtils.setProperty(dynaForm, "useProjectCode", Boolean.TRUE);
        form.setUseDashboard(Boolean.TRUE);
        form.setProjectCodeList(getProjectList());
    }

    protected String getReportNameForParameterPage() {
        return MessageUtil.getMessage("reports.label.project.export") + " "
                + MessageUtil.getMessage("sample.export.releasedDate");
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
        if (isBlankOrNull(projectStr) || "0".equals(Integer.getInteger(projectStr))) {
            add1LineErrorMessage("report.error.message.project.missing");
            return false;
        }
        project = SpringContext.getBean(ProjectService.class).getProjectById(projectStr);
        if (project == null) {
            add1LineErrorMessage("report.error.message.project.missing");
            return false;
        }
        return true;
    }

    /** creating the list for generation to the report */
    private void createReportItems() {
        try {
            csvColumnBuilder = getColumnBuilder(projectStr);
            csvColumnBuilder.buildDataSource();
        } catch (Exception e) {
            Log.error("Error in " + this.getClass().getSimpleName() + ".createReportItems: ", e);
            add1LineErrorMessage("report.error.message.general.error");
        }
    }

    @Override
    protected void writeResultsToBuffer(ByteArrayOutputStream buffer)
            throws IOException, UnsupportedEncodingException, SQLException, ParseException {

        String currentAccessionNumber = null;
        String[] splitBase = null;
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

    private boolean writeAble(String result) throws ParseException {

        String workingResult = result.split("\\(")[0].trim();
        String[] splitLine = indicStr.split(":");
        String indic = splitLine[1];
        if (indic.equals("Unsuppressed VL")) {
            try {
                return workingResult.contains("Log7") || !workingResult.contains("L") && !workingResult.contains("X")
                        && !workingResult.contains("<") && workingResult.length() > 0
                        && (workingResult.replaceAll("[^0-9]", "").length() > 0
                                ? Double.parseDouble(workingResult.replaceAll("[^0-9]", "")) >= 1000
                                : false);
            } catch (Exception e) {
                return false;
            }
        } else if (indic.equals("Suppressed VL")) {
            try {
                return workingResult.contains("L") || workingResult.contains("<")
                        || (workingResult.length() > 0 && !workingResult.toUpperCase().contains("X")
                                && !workingResult.toLowerCase().contains("invalid")
                                && Double.parseDouble(workingResult.replaceAll("[^0-9]", "")) < 1000);
            } catch (Exception e) {
                return false;
            }
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

    protected void writeConsolidatedBaseToBuffer(ByteArrayOutputStream buffer, String[] splitBase)
            throws IOException, UnsupportedEncodingException {

        if (splitBase != null) {
            StringBuffer consolidatedLine = new StringBuffer();
            for (String value : splitBase) {
                consolidatedLine.append(value);
                consolidatedLine.append(",");
            }

            consolidatedLine.deleteCharAt(consolidatedLine.lastIndexOf(","));
            buffer.write(consolidatedLine.toString().getBytes("windows-1252"));
        }
    }

    private CSVColumnBuilder getColumnBuilder(String projectId) {
        // String projectTag = CIColumnBuilder.translateProjectId(projectId);
        return new TrendsColumnBuilder(dateRange, projectStr);
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
    public List<Project> getProjectList() {
        List<Project> projects = new ArrayList<>();
        Project project = new Project();
        /*
         * project.setProjectName("Antiretroviral Study");
         * projects.add(projectDAO.getProjectByName(project, false, false));
         * project.setProjectName("Antiretroviral Followup Study");
         * projects.add(projectDAO.getProjectByName(project, false, false));
         * project.setProjectName("Routine HIV Testing");
         * projects.add(projectDAO.getProjectByName(project, false, false));
         * project.setProjectName("Early Infant Diagnosis for HIV Study");
         * projects.add(projectDAO.getProjectByName(project, false, false));
         * project.setProjectName("Viral Load Results");
         * projects.add(projectDAO.getProjectByName(project, false, false));
         * project.setProjectName("Indeterminate Results");
         * projects.add(projectDAO.getProjectByName(project, false, false));
         */

        project.setId("28:Unsuppressed VL");
        project.setProjectName("Unsuppressed VL");
        projects.add(project);
        project = new Project();
        project.setId("28:Suppressed VL");
        project.setProjectName("Suppressed VL");
        projects.add(project);

        return projects;
    }
}
