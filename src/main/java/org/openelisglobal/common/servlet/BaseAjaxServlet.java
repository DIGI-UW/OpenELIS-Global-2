package org.openelisglobal.common.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * An abstract class from which each example servlet extends. This class wraps
 * the XML creation (delegated to the child servlet class) and submission back
 * through the HTTP response.
 *
 * @author Darren L. Spurgeon Modified by Caleb Steele-Lane for jakarta
 *         migration
 */
public abstract class BaseAjaxServlet extends HttpServlet {

    /**
     * @see jakarta.servlet.http.HttpServlet#doGet(jakarta.servlet.http.HttpServletRequest,
     *      jakarta.servlet.http.HttpServletResponse)
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String xml = null;

        try {
            xml = getXmlContent(request, response);
        } catch (Exception ex) {
            // Send back a 500 error code.
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Can not create response");
            return;
        }

        // Set content to xml
        response.setContentType("text/xml; charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        PrintWriter pw = response.getWriter();
        pw.write(xml);
        pw.close();
    }

    /**
     * @see jakarta.servlet.http.HttpServlet#doPost(jakarta.servlet.http.HttpServletRequest,
     *      jakarta.servlet.http.HttpServletResponse)
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    /**
     * Each child class should override this method to generate the specific XML
     * content necessary for each AJAX action.
     *
     * @param request  the {@jakarta.servlet.http.HttpServletRequest} object
     * @param response the {@jakarta.servlet.http.HttpServletResponse} object
     * @return a {@java.lang.String} representation of the XML response/content
     */
    public abstract String getXmlContent(HttpServletRequest request, HttpServletResponse response) throws Exception;
}
