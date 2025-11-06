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
import org.openelisglobal.citystatezip.service.CityStateZipService;
import org.openelisglobal.citystatezip.valueholder.CityStateZip;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.servlet.validation.AjaxServlet;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.spring.util.SpringContext;
import org.owasp.encoder.Encode;

/**
 * @author benzd1 bugzilla 1765 changed to validate zipcode only (combination of
 *         city/zip is validated elsewhere)
 */
public class ZipValidationProvider extends BaseValidationProvider {

    protected CityStateZipService cityStateZipService = SpringContext.getBean(CityStateZipService.class);

    public ZipValidationProvider() {
        super();
    }

    public ZipValidationProvider(AjaxServlet ajaxServlet) {
        this.ajaxServlet = ajaxServlet;
    }

    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // get id from request
        String zip = request.getParameter("id");
        String formField = request.getParameter("field");
        String result = validate(zip);
        ajaxServlet.sendData(Encode.forXmlContent(formField), result, request, response);
    }

    // bugzilla 1367 efficiency fix (and bug fix)
    public String validate(String zip) throws LIMSRuntimeException {

        StringBuffer s = new StringBuffer();

        if (!StringUtil.isNullorNill(zip)) {
            // bugzilla 1545
            CityStateZip cityStateZip = new CityStateZip();
            cityStateZip.setZipCode(zip.trim());
            cityStateZip = cityStateZipService.getZipCode(cityStateZip);

            if (cityStateZip == null) {
                s.append(INVALID);
            } else {
                s.append(VALID);
            }
        } else {
            s.append(VALID);
        }

        return s.toString();
    }
}
