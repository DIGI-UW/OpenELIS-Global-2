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
package org.openelisglobal.requester.valueholder;

import org.openelisglobal.common.valueholder.BaseObject;

public class SampleRequester extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String id;
    private long sampleId;
    private long requesterId;
    private long requesterTypeId;

    public long getSampleId() {
        return sampleId;
    }

    public void setSampleId(long sampleId) {
        this.sampleId = sampleId;
    }

    /*
     * public void setSampleId(String sampleId) { this.sampleId =
     * Long.parseLong(sampleId); }
     */
    public long getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(long requesterId) {
        this.requesterId = requesterId;
    }

    public void setRequesterId(String requesterId) {
        this.requesterId = Long.parseLong(requesterId);
    }

    public long getRequesterTypeId() {
        return requesterTypeId;
    }

    public void setRequesterTypeId(long requesterTypeId) {
        this.requesterTypeId = requesterTypeId;
    }

    public void setRequesterTypeId(String requesterTypeId) {
        this.requesterTypeId = Long.parseLong(requesterTypeId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof SampleRequester)) {
            return false;
        }
        SampleRequester sr = (SampleRequester) obj;
        return sr.requesterId == requesterId && sr.requesterTypeId == requesterTypeId && sr.sampleId == sampleId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash = 31 * hash + (int) requesterId;
        hash = 31 * hash + (int) requesterTypeId;
        hash = 31 * hash + (int) sampleId;
        return hash;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }
}
