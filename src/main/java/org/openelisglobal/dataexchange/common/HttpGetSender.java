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
package org.openelisglobal.dataexchange.common;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.validator.GenericValidator;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HttpGetSender extends HttpSender {

    @Autowired
    private CloseableHttpClient httpClient;

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.dataexchange.IExternalSender#sendMessage()
     */
    @Override
    public boolean sendMessage() {

        errors = new ArrayList<String>();

        if (GenericValidator.isBlankOrNull(url)) {
            LogEvent.logWarn("HttpGetSender", "send message", "The url is null");
            errors.add("send message The url is null");
            return false;
        }

        HttpGet httpGet = new HttpGet(url);
        try {
            CloseableHttpResponse response = httpClient.execute(httpGet);
            returnStatus = response.getStatusLine().getStatusCode();
            return returnStatus == HttpStatus.SC_OK;
        } catch (IOException e1) {
            LogEvent.logError(e1);
        }
        return false;
    }
}
