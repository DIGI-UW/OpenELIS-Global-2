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
package org.openelisglobal.common.provider.autocomplete;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.openelisglobal.common.servlet.BaseAjaxServlet;

public abstract class BaseAutocompleteProvider {

    protected BaseAjaxServlet ajaxServlet = null;

    public abstract List processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException;

    public void setServlet(BaseAjaxServlet as) {
        this.ajaxServlet = as;
    }

    public BaseAjaxServlet getServlet() {
        return this.ajaxServlet;
    }
}
