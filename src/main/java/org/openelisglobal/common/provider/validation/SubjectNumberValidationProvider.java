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
package org.openelisglobal.common.provider.validation;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.common.servlet.validation.AjaxServlet;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.search.service.SearchResultsService;
import org.openelisglobal.spring.util.SpringContext;
import org.owasp.encoder.Encode;

/**
 * The QuickEntryAccessionNumberValidationProvider class is used to validate,
 * via AJAX.
 */
public class SubjectNumberValidationProvider extends BaseValidationProvider {

    protected SearchResultsService searchResultsService = SpringContext.getBean(SearchResultsService.class);

    public SubjectNumberValidationProvider() {
        super();
    }

    public SubjectNumberValidationProvider(AjaxServlet ajaxServlet) {
        this.ajaxServlet = ajaxServlet;
    }

    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String queryResponse = "valid";
        String fieldId = request.getParameter("fieldId");
        String number = request.getParameter("subjectNumber");
        String numberType = request.getParameter("numberType");
        String STNumber = numberType.equals("STnumber") ? number : null;
        String subjectNumber = numberType.equals("subjectNumber") ? number : null;
        String nationalId = numberType.equals("nationalId") ? number : null;

        // We just care about duplicates but blank values do not count as duplicates
        if (!(GenericValidator.isBlankOrNull(STNumber) && GenericValidator.isBlankOrNull(subjectNumber)
                && GenericValidator.isBlankOrNull(nationalId))) {
            List<PatientSearchResults> results = searchResultsService.getSearchResultsExact(null, null, STNumber,
                    subjectNumber, nationalId, null, null, null, null, null);

            boolean allowDuplicateSubjectNumber = ConfigurationProperties.getInstance()
                    .isPropertyValueEqual(ConfigurationProperties.Property.ALLOW_DUPLICATE_SUBJECT_NUMBERS, "true");
            boolean allowDuplicateNationalId = ConfigurationProperties.getInstance()
                    .isPropertyValueEqual(ConfigurationProperties.Property.ALLOW_DUPLICATE_NATIONAL_IDS, "true");
            if (!results.isEmpty() && !GenericValidator.isBlankOrNull(subjectNumber)) {
                queryResponse = (allowDuplicateSubjectNumber ? "warning#" + MessageUtil.getMessage("alert.warning")
                        : "fail#" + MessageUtil.getMessage("alert.error")) + ": "
                        + MessageUtil.getMessage("error.duplicate.subjectNumber.warning");
            } else if (!results.isEmpty() && !GenericValidator.isBlankOrNull(nationalId)) {
                queryResponse = (allowDuplicateNationalId ? "warning#" + MessageUtil.getMessage("alert.warning")
                        : "fail#" + MessageUtil.getMessage("alert.error")) + ": "
                        + MessageUtil.getMessage("error.duplicate.subjectNumber.warning");
            } else if (!results.isEmpty()) {
                queryResponse = "fail#" + MessageUtil.getMessage("alert.error") + ": "
                        + MessageUtil.getMessage("error.duplicate.subjectNumber.warning");
            }
        }
        response.setCharacterEncoding("UTF-8");
        ajaxServlet.sendData(Encode.forXmlContent(fieldId), queryResponse, request, response);
    }
}
