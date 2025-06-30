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
 * @author benzd1 bugzilla 1765 validates combinations of city/state/zip
 */
public class CityStateZipComboValidationProvider extends BaseValidationProvider {

    protected CityStateZipService cityStateZipService = SpringContext.getBean(CityStateZipService.class);

    public CityStateZipComboValidationProvider() {
        super();
    }

    public CityStateZipComboValidationProvider(AjaxServlet ajaxServlet) {
        this.ajaxServlet = ajaxServlet;
    }

    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // get id from request
        String formField = request.getParameter("field");
        String city = request.getParameter("city");
        String zip = request.getParameter("zipCode");
        String state = request.getParameter("state");

        if (StringUtil.isNullorNill(city)) {
            city = "";
        }
        if (StringUtil.isNullorNill(state)) {
            state = "";
        }
        if (StringUtil.isNullorNill(zip)) {
            zip = "";
        }
        String result = validate(city, state, zip);
        ajaxServlet.sendData(Encode.forXmlContent(formField), result, request, response);
    }

    public String validate(String city, String state, String zipCode) throws LIMSRuntimeException {

        StringBuffer s = new StringBuffer();

        // bugzilla 1545
        CityStateZip cityStateZip = new CityStateZip();
        cityStateZip.setCity(city.trim());
        cityStateZip.setZipCode(zipCode);
        cityStateZip.setState(state);
        if (cityStateZipService.isCityStateZipComboValid(cityStateZip)) {
            s.append(VALID);
        } else {
            s.append(INVALID);
        }

        return s.toString();
    }
}
