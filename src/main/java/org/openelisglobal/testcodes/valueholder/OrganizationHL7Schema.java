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

import org.openelisglobal.common.valueholder.BaseObject;

public class OrganizationHL7Schema extends BaseObject<OrganizationSchemaPK> {

    private static final long serialVersionUID = -9609118384222987L;

    private OrganizationSchemaPK compoundId = new OrganizationSchemaPK();

    public void setCompoundId(OrganizationSchemaPK compoundId) {
        this.compoundId = compoundId;
    }

    public OrganizationSchemaPK getCompoundId() {
        return compoundId;
    }

    public String getOrganizationId() {
        return compoundId.getOrganizationId();
    }

    public void setOrganizationId(String organizationId) {
        compoundId.setOrganizationId(organizationId);
    }

    public String getEncodingTypeId() {
        return compoundId.getEncodingTypeId();
    }

    public void setEncodingTypeId(String encodingTypeId) {
        compoundId.setEncodingTypeId(encodingTypeId);
    }

    @Override
    public void setId(OrganizationSchemaPK id) {
        setCompoundId(id);
    }

    @Override
    public OrganizationSchemaPK getId() {
        return getCompoundId();
    }
}
