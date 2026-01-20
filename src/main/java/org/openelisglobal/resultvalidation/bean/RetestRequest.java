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
package org.openelisglobal.resultvalidation.bean;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Request bean for sending results back for retest
 */
public class RetestRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Result IDs are required")
    @NotEmpty(message = "At least one result ID is required")
    private List<String> resultIds = new ArrayList<>();

    @NotNull(message = "Retest reason is required")
    @NotEmpty(message = "Retest reason cannot be empty")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String reason;

    private String requestedBy;
    private String requestedAt;

    public RetestRequest() {
    }

    public RetestRequest(List<String> resultIds, String reason) {
        this.resultIds = resultIds;
        this.reason = reason;
    }

    public List<String> getResultIds() {
        return resultIds;
    }

    public void setResultIds(List<String> resultIds) {
        this.resultIds = resultIds;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(String requestedAt) {
        this.requestedAt = requestedAt;
    }
}
