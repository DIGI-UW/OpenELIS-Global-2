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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.common.provider.validation;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.servlet.validation.AjaxServlet;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.samplepdf.service.SamplePdfService;
import org.openelisglobal.spring.util.SpringContext;
import org.owasp.encoder.Encode;

public class FileValidationProvider extends BaseValidationProvider {

    protected SamplePdfService samplePdfService = SpringContext.getBean(SamplePdfService.class);

    int indentLevel = -1;

    public FileValidationProvider() {
        super();
    }

    public FileValidationProvider(AjaxServlet ajaxServlet) {
        this.ajaxServlet = ajaxServlet;
    }

    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // get id from request
        String targetId = request.getParameter("id");
        String formField = request.getParameter("field");
        String result = validate(targetId);
        ajaxServlet.sendData(Encode.forXmlContent(formField), result, request, response);
    }

    public String validate(String targetId) throws LIMSRuntimeException {
        String msg = INVALID;
        boolean isFound = false;
        if (!StringUtil.isNullorNill(targetId)) {
            int x = Integer.parseInt(targetId);
            isFound = samplePdfService.isAccessionNumberFound(x);
            if (isFound) {
                msg = VALID;
            }
        } else {
            msg = VALID;
        }
        return msg;
    }
}
