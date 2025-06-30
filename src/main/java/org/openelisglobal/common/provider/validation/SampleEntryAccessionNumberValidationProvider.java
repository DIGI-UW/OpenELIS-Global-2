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
package org.openelisglobal.common.provider.validation;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.openelisglobal.common.provider.validation.IAccessionNumberValidator.ValidationResults;
import org.openelisglobal.common.servlet.validation.AjaxServlet;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.sample.util.CI.ProjectForm;
import org.owasp.encoder.Encode;

/**
 * The QuickEntryAccessionNumberValidationProvider class is used to validate,
 * via AJAX.
 */
public class SampleEntryAccessionNumberValidationProvider extends BaseValidationProvider {

    public SampleEntryAccessionNumberValidationProvider() {
        super();
    }

    public SampleEntryAccessionNumberValidationProvider(AjaxServlet ajaxServlet) {
        this.ajaxServlet = ajaxServlet;
    }

    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String accessionNumber = request.getParameter("accessionNumber");
        String field = request.getParameter("field");
        String recordType = request.getParameter("recordType");
        String isRequired = request.getParameter("isRequired");
        String projectFormName = request.getParameter("projectFormName");
        boolean altAccession = "true".equalsIgnoreCase(request.getParameter("altAccession"));
        boolean parseForProjectFormName = "true".equalsIgnoreCase(request.getParameter("parseForProjectFormName"));
        boolean ignoreYear = "true".equals(request.getParameter("ignoreYear"));
        boolean ignoreUsage = "true".equals(request.getParameter("ignoreUsage"));

        ValidationResults result;

        if (parseForProjectFormName) {
            projectFormName = ProgramAccessionValidator.findStudyFormName(accessionNumber);
        }
        boolean projectFormNameUsed = ProjectForm.findProjectFormByFormId(projectFormName) != null;

        if (ignoreYear || ignoreUsage) {
            result = projectFormNameUsed ? new ProgramAccessionValidator().validFormat(accessionNumber, !ignoreYear)
                    : AccessionNumberUtil.getGeneralAccessionNumberValidator().validFormat(accessionNumber,
                            !ignoreYear);
            if (result == ValidationResults.SUCCESS && !ignoreUsage) {
                result = AccessionNumberUtil.isUsed(accessionNumber) ? ValidationResults.SAMPLE_FOUND
                        : ValidationResults.SAMPLE_NOT_FOUND;
            }
        } else {
            // year matters and number must not be used
            result = projectFormNameUsed
                    ? new ProgramAccessionValidator().checkAccessionNumberValidity(accessionNumber, recordType,
                            isRequired, projectFormName)
                    : AccessionNumberUtil.getGeneralAccessionNumberValidator()
                            .checkAccessionNumberValidity(accessionNumber, recordType, isRequired, projectFormName);
        }

        String returnData;

        // if( !Boolean.valueOf(ConfigurationProperties.getInstance()
        // .getPropertyValue(Property.ACCESSION_NUMBER_VALIDATE))) {
        // result = ValidationResults.SUCCESS;
        // }

        switch (result) {
        case SUCCESS:
            returnData = VALID;
            break;
        case SAMPLE_FOUND:
        case SAMPLE_NOT_FOUND:
            returnData = result.name();
            break;
        default:
            if (projectFormNameUsed) {
                returnData = !ignoreUsage ? new ProgramAccessionValidator().getInvalidMessage(result)
                        : new ProgramAccessionValidator().getInvalidFormatMessage(result);
            } else {
                returnData = !ignoreUsage ? altAccession //
                        ? AccessionNumberUtil.getAltAccessionNumberValidator().getInvalidMessage(result)
                        : AccessionNumberUtil.getMainAccessionNumberValidator().getInvalidMessage(result)
                        : altAccession
                                ? AccessionNumberUtil.getAltAccessionNumberValidator().getInvalidFormatMessage(result)
                                : AccessionNumberUtil.getMainAccessionNumberValidator().getInvalidFormatMessage(result);
            }
        }

        response.setCharacterEncoding("UTF-8");
        ajaxServlet.sendData(Encode.forXmlContent(field), returnData, request, response);
    }
}
