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
 * @author benzd1 bugzilla 1765 changed to validate city only (combination of
 *         city/zip is validated elsewhere)
 */
public class CityValidationProvider extends BaseValidationProvider {

    protected CityStateZipService cityStateZipService = SpringContext.getBean(CityStateZipService.class);

    public CityValidationProvider() {
        super();
    }

    public CityValidationProvider(AjaxServlet ajaxServlet) {
        this.ajaxServlet = ajaxServlet;
    }

    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // get id from request
        String city = request.getParameter("id");
        String formField = request.getParameter("field");
        String result = validate(city);
        ajaxServlet.sendData(Encode.forXmlContent(formField), result, request, response);
    }

    public String validate(String city) throws LIMSRuntimeException {

        StringBuffer s = new StringBuffer();

        if (!StringUtil.isNullorNill(city)) {
            // bugzilla 1545
            CityStateZip cityStateZip = new CityStateZip();
            cityStateZip.setCity(city.trim());
            cityStateZip = cityStateZipService.getCity(cityStateZip);

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
