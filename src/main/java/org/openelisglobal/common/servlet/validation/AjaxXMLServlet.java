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
package org.openelisglobal.common.servlet.validation;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.json.XML;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.provider.validation.BaseValidationProvider;
import org.openelisglobal.common.provider.validation.ValidationProviderFactory;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

public class AjaxXMLServlet extends AjaxServlet {

    private static final long serialVersionUID = 1L;

    @Override
    public void sendData(String field, String message, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        if (!StringUtil.isNullorNill(field)) {
            StringBuilder sb = new StringBuilder().append("<fieldmessage>").append("<formfield>").append(field)
                    .append("</formfield>").append("<message>").append(message).append("</message>")
                    .append("</fieldmessage>");
            if ("true".equals(request.getParameter("asJSON"))) {
                response.setContentType("application/json");
                response.setHeader("Cache-Control", "no-cache");
                response.getWriter().write(XML.toJSONObject(sb.toString()).toString());
            } else {
                response.setContentType("text/xml");
                response.setHeader("Cache-Control", "no-cache");
                response.getWriter().write(sb.toString());
            }
        } else {
            // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", "Returning
            // no
            // content with field " + field + " message " +
            // message);
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException, LIMSRuntimeException {
        boolean unauthorized = false;

        // check for module authentication
        UserModuleService userModuleService = SpringContext.getBean(UserModuleService.class);
        unauthorized |= userModuleService.isSessionExpired(request);

        // check for csrf token to prevent js hijacking since we employ callback
        // functions
        CsrfToken officialToken = new HttpSessionCsrfTokenRepository().loadToken(request);
        String clientSuppliedToken = request.getHeader("X-CSRF-Token");
        // unauthorized |= !officialToken.getToken().equals(clientSuppliedToken);
        unauthorized |= clientSuppliedToken == null;

        if (unauthorized) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("text/html; charset=utf-8");
            response.getWriter().println(MessageUtil.getMessage("message.error.unauthorized"));
            return;
        }

        String valProvider = request.getParameter("provider");
        BaseValidationProvider provider = ValidationProviderFactory.getInstance().getValidationProvider(valProvider);
        provider.setServlet(this);
        provider.processRequest(request, response);
    }

    public void getHeader(String headerName) {
    }
}
