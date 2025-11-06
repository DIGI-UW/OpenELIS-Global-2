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
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.i
 */
package org.openelisglobal.statusofsample.valueholder;

import org.openelisglobal.common.valueholder.EnumValueItemImpl;

/**
 * @author bill mcgough bugzilla 1625
 */
public class StatusOfSample extends EnumValueItemImpl {

    private static final long serialVersionUID = 1L;
    private String id;
    private String statusOfSampleName;
    private String description;
    private String code;
    private String statusType;
    private String isActive;

    public StatusOfSample() {
        super();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        // bugzilla 1625
        this.name = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        // bugzilla 1625
        this.key = id;
    }

    public String getStatusType() {
        return statusType;
    }

    public void setStatusType(String statusType) {
        this.statusType = statusType;
    }

    public String getStatusOfSampleName() {
        return statusOfSampleName;
    }

    public void setStatusOfSampleName(String statusOfSampleName) {
        this.statusOfSampleName = statusOfSampleName;
    }

    public String getDefaultLocalizedName() {
        return getStatusOfSampleName();
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }
}
