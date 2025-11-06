package org.openelisglobal.common.provider.query;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.servlet.validation.AjaxServlet;

public abstract class BaseQueryProvider implements IActionConstants {

    protected AjaxServlet ajaxServlet = null;

    public abstract void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException;

    public void setServlet(AjaxServlet as) {
        this.ajaxServlet = as;
    }

    public AjaxServlet getServlet() {
        return this.ajaxServlet;
    }
}
