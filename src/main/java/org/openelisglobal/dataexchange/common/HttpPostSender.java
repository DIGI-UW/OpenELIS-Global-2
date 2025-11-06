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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HttpPostSender extends HttpSender {

    @Autowired
    private CloseableHttpClient httpClient;

    @Override
    public boolean sendMessage() {

        errors = new ArrayList<>();

        if (GenericValidator.isBlankOrNull(message) || GenericValidator.isBlankOrNull(url)) {
            LogEvent.logWarn("HttpPutSender", "send message",
                    "The " + message == null ? " message " : "url" + " is null");
            errors.add("send message The " + (message == null ? " message " : "url") + " is null");
            errors.add("Application not configured correctly for sending results");
            return false;
        }

        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(message, ContentType.TEXT_PLAIN));
        try {
            CloseableHttpResponse response = httpClient.execute(httpPost);
            returnStatus = response.getStatusLine().getStatusCode();
            return returnStatus == HttpStatus.SC_OK;
        } catch (IOException e1) {
            LogEvent.logError(e1);
        }
        return false;
    }
}
