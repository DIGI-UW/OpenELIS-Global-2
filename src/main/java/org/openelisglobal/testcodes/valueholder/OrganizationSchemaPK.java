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
package org.openelisglobal.testcodes.valueholder;

import java.io.Serializable;
import java.util.Objects;

public class OrganizationSchemaPK implements Serializable {

    private static final long serialVersionUID = 3067673553215167728L;

    private String organizationId;
    private String encodingTypeId;

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getEncodingTypeId() {
        return encodingTypeId;
    }

    public void setEncodingTypeId(String encodingTypeId) {
        this.encodingTypeId = encodingTypeId;
    }

    public String toString() {
        return organizationId + encodingTypeId;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        OrganizationSchemaPK that = (OrganizationSchemaPK) o;

        return Objects.equals(this.organizationId, that.organizationId)
                && Objects.equals(this.encodingTypeId, that.encodingTypeId);
    }

    public int hashCode() {
        return Objects.hash(organizationId, encodingTypeId);
    }
}
