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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.dataexchange.connectionTest;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.openelisglobal.dataexchange.common.ITransmissionResponseHandler;
import org.openelisglobal.dataexchange.common.ReportTransmission;
import org.openelisglobal.dataexchange.common.ReportTransmission.HTTP_TYPE;
import org.openelisglobal.internationalization.MessageUtil;

public class ConnectionTest {

    public String testURL(String url) {

        boolean sendAsychronously = false;
        TestFailHandler responseHandler = new TestFailHandler();

        new ReportTransmission().sendRawReport(null, url, sendAsychronously, responseHandler, HTTP_TYPE.GET);

        return responseHandler.getResponse();
    }

    public class TestFailHandler implements ITransmissionResponseHandler {
        private String response;

        @Override
        public void handleResponse(int httpReturnStatus, List<String> errors, String msg) {

            if (httpReturnStatus == HttpServletResponse.SC_OK) {
                response = MessageUtil.getMessage("http.success");
            } else {
                if (!errors.isEmpty()) {
                    response = errors.get(0);
                }
            }
        }

        public String getResponse() {
            return response;
        }
    }
}
