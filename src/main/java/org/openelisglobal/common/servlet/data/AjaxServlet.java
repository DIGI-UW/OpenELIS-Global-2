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
package org.openelisglobal.common.servlet.data;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.openelisglobal.common.provider.data.BaseDataProvider;

/**
 * @author diane benz bugzilla 2443
 */
public abstract class AjaxServlet extends HttpServlet {

    private BaseDataProvider dataProvider = null;

    public abstract void sendData(String field, String message, HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException;

    public abstract void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException;
}
