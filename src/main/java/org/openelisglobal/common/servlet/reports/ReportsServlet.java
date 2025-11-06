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
package org.openelisglobal.common.servlet.reports;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.common.provider.reports.BaseReportsProvider;
import org.openelisglobal.common.provider.reports.ReportsProviderFactory;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.spring.util.SpringContext;

/**
 * @author benzd1
 */
public class ReportsServlet extends HttpServlet {

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.http.HttpServlet#doGet(jakarta.servlet.http.
     * HttpServletRequest, jakarta.servlet.http.HttpServletResponse)
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // check for authentication
        UserModuleService userModuleService = SpringContext.getBean(UserModuleService.class);
        if (userModuleService.isSessionExpired(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("text/html; charset=utf-8");
            response.getWriter().println(MessageUtil.getMessage("message.error.unauthorized"));
            return;
        }
        String reportsProvider = request.getParameter("provider");
        Map paramMap = request.getParameterMap();

        HashMap parameters = new HashMap();

        Set keySet = paramMap.keySet();

        Collection values = paramMap.values();

        Iterator itKey = keySet.iterator();

        List keyList = new ArrayList();

        while (itKey.hasNext()) {
            String key = (String) itKey.next();
            keyList.add(key);
        }

        Iterator itVal = values.iterator();

        List valList = new ArrayList();

        while (itVal.hasNext()) {
            String[] vals = (String[]) itVal.next();
            valList.add(vals[0]);
        }

        // populate HashMap
        for (int i = 0; i < keyList.size(); i++) {
            parameters.put(keyList.get(i), valList.get(i));
        }

        BaseReportsProvider reportProvider = ReportsProviderFactory.getInstance().getReportsProvider(reportsProvider);

        reportProvider.setServlet(this);
        // bugzilla 2274: added error handling
        boolean success = reportProvider.processRequest(parameters, request, response);

        // if unsuccessful route back to reports menu
        if (!success) {
            ServletContext context = getServletContext();
            RequestDispatcher dispatcher = context.getRequestDispatcher("/MainMenu");
            dispatcher.forward(request, response);
        }
    }
}
