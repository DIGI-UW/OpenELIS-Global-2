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
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.dataexchange.order.action;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.openelisglobal.common.log.LogEvent;

public class OrderRawServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // LogEvent.logFatal("IndicatorAggregationReportingServlet", "size",
        // String.valueOf(request.getContentLength()));
        String info = "\ncharacterEncoding: " + request.getCharacterEncoding() + "\ncontentLength: "
                + request.getContentLength() + "\ncontentType: " + request.getContentType() + "\n\n";

        String sentIndicators = getDocument(request.getInputStream(), request.getContentLength());

        // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", info);
        // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown",
        // sentIndicators);

        LogEvent.logFatal("OrderRawServletServlet", "raw", info + sentIndicators);

        response.setStatus(HttpServletResponse.SC_OK);
    }

    private String getDocument(ServletInputStream inputStream, int contentLength) {
        int charCount = 0;
        byte[] byteBuffer = new byte[contentLength];

        while (true) {
            try {
                int readLength = inputStream.readLine(byteBuffer, charCount, 1024);

                if (readLength == -1) {
                    return new String(byteBuffer, StandardCharsets.UTF_8);
                } else {
                    charCount += readLength;
                }

            } catch (IOException e) {
                LogEvent.logDebug(e);
                return null;
            }
        }
    }

    private static final long serialVersionUID = 1L;
}
