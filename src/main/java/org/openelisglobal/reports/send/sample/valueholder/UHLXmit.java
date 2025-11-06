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
package org.openelisglobal.reports.send.sample.valueholder;

import org.openelisglobal.common.valueholder.BaseObject;

public class UHLXmit extends BaseObject<String> {

    private String id;

    private String applicationName;

    private String mdhId;

    private ProviderXmit provider;

    private TestingFacilityXmit facility;

    private String messageTime;

    private String processingId;

    private String transportMethod;

    private String transportDestination;

    private String transportDestinationPort;

    public String getTransportDestination() {
        return transportDestination;
    }

    public void setTransportDestination(String transportDestination) {
        this.transportDestination = transportDestination;
    }

    public String getTransportDestinationPort() {
        return transportDestinationPort;
    }

    public void setTransportDestinationPort(String transportDestinationPort) {
        this.transportDestinationPort = transportDestinationPort;
    }

    public String getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(String messageTime) {
        this.messageTime = messageTime;
    }

    public String getProcessingId() {
        return processingId;
    }

    public void setProcessingId(String processingId) {
        this.processingId = processingId;
    }

    public String getTransportMethod() {
        return transportMethod;
    }

    public void setTransportMethod(String transportMethod) {
        this.transportMethod = transportMethod;
    }

    public TestingFacilityXmit getFacility() {
        return facility;
    }

    public void setFacility(TestingFacilityXmit facility) {
        this.facility = facility;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getMdhId() {
        return mdhId;
    }

    public void setMdhId(String mdhId) {
        this.mdhId = mdhId;
    }

    public ProviderXmit getProvider() {
        return provider;
    }

    public void setProvider(ProviderXmit provider) {
        this.provider = provider;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
