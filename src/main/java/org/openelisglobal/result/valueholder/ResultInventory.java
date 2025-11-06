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
package org.openelisglobal.result.valueholder;

import org.openelisglobal.common.valueholder.BaseObject;

public class ResultInventory extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String id;
    private String resultId;
    private String inventoryLocationId;
    private String description;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getInventoryLocationId() {
        return inventoryLocationId;
    }

    public void setInventoryLocationId(String inventoryLocationId) {
        this.inventoryLocationId = inventoryLocationId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
