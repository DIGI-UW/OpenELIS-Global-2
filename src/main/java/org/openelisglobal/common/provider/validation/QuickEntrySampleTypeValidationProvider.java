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
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.owasp.encoder.Encode;

/**
 * The QuickEntrySampleTypeValidationProvider class is used to validate, via
 * AJAX, the Sample Type entered on the Quick Entry view.
 *
 * @author Ken Rosha 08/30/2006
 */
public class QuickEntrySampleTypeValidationProvider extends BaseValidationProvider {

    protected TypeOfSampleService typeOfSampleService = SpringContext.getBean(TypeOfSampleService.class);

    public QuickEntrySampleTypeValidationProvider() {
        super();
    }
    // ==============================================================

    public QuickEntrySampleTypeValidationProvider(AjaxServlet ajaxServlet) {
        this.ajaxServlet = ajaxServlet;
    }
    // ==============================================================

    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String targetId = request.getParameter("id");
        String formField = request.getParameter("field");
        String result = validate(targetId);
        ajaxServlet.sendData(Encode.forXmlContent(formField), Encode.forXmlContent(result), request, response);
    }
    // ==============================================================

    // modified for efficiency bugzilla 1367
    public String validate(String targetId) throws LIMSRuntimeException {
        StringBuffer s = new StringBuffer();

        if (!StringUtil.isNullorNill(targetId)) {
            TypeOfSample typeOfSample = new TypeOfSample();
            typeOfSample.setDescription(targetId);
            // passing in a nill or null domain retrieves records for ALL domains
            typeOfSample = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(typeOfSample, true);
            if (typeOfSample != null) {
                // bugzilla 1465
                s.append(VALID + typeOfSample.getId());
            } else {
                s.append(INVALID);
            }
        } else {
            s.append(VALID);
        }
        return s.toString();
    }

    // ==============================================================
}
