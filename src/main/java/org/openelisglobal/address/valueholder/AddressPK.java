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
package org.openelisglobal.address.valueholder;

import java.io.Serializable;
import java.util.Objects;

public class AddressPK implements Serializable {

    private static final long serialVersionUID = 1L;

    private String targetId;
    private String addressPartId;

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getAddressPartId() {
        return addressPartId;
    }

    public void setAddressPartId(String addressPartId) {
        this.addressPartId = addressPartId;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        AddressPK that = (AddressPK) o;

        return Objects.equals(this.targetId, that.targetId) && Objects.equals(this.addressPartId, that.addressPartId);
    }

    public int hashCode() {
        return Objects.hash(targetId, addressPartId);
    }
}
