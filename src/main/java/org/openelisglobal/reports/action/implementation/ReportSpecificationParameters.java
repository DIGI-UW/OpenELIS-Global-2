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
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.patient.action.bean.PatientSearch;
import org.openelisglobal.reports.form.ReportForm;

public class ReportSpecificationParameters implements IReportParameterSetter {
    public enum Parameter {
        NO_SPECIFICATION, DATE_RANGE, ACCESSION_RANGE, USE_SITE_SEARCH, USE_PATIENT_SEARCH
    }

    private String reportTitle;
    private String instructions;
    private ArrayList<Parameter> parameters = new ArrayList<>();

    /**
     * Constructor for a single parameter.
     *
     * @param parameter    The parameter which will appear on the parameter page
     * @param title        The title for the page, it will appear above the
     *                     parameters
     * @param instructions The instructions for the user on how to fill in the
     *                     parameters
     */
    public ReportSpecificationParameters(Parameter parameter, String title, String instructions) {
        parameters.add(parameter);
        reportTitle = title;
        this.instructions = instructions;
    }

    public ReportSpecificationParameters(Parameter[] parameters, String title, String instructions) {
        reportTitle = title;
        this.instructions = instructions;

        for (Parameter newParameter : parameters) {
            this.parameters.add(newParameter);
        }
    }

    @Override
    public void setRequestParameters(ReportForm form) {
        try {
            form.setReportName(reportTitle);
            if (!GenericValidator.isBlankOrNull(instructions)) {
                form.setInstructions(instructions);
            }
            form.setReportName(reportTitle);
            for (Parameter parameter : parameters) {
                switch (parameter) {
                case USE_PATIENT_SEARCH: {
                    form.setUsePatientSearch(true);
                    form.setPatientSearch(new PatientSearch());
                    break;
                }
                case USE_SITE_SEARCH: {
                    form.setUseSiteSearch(true);
                    if (form.getReport().equals("patientVL1")) {
                        form.setReferringSiteList(
                                DisplayListService.getInstance().getList(DisplayListService.ListType.ARV_ORG_LIST));
                    } else {
                        form.setReferringSiteList(DisplayListService.getInstance()
                                .getList(DisplayListService.ListType.SAMPLE_PATIENT_REFERRING_CLINIC));
                    }
                    break;
                }
                case DATE_RANGE: {
                    form.setUseLowerDateRange(true);
                    form.setUseUpperDateRange(true);
                    break;
                }
                case ACCESSION_RANGE: {
                    form.setUseAccessionDirect(true);
                    form.setUseHighAccessionDirect(true);
                    break;
                }
                case NO_SPECIFICATION: {
                    form.setNoRequestSpecifications(true);
                    break;
                }
                }
            }
        } catch (RuntimeException e) {
            LogEvent.logDebug(e);
        }
    }
}
