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
package org.openelisglobal.analyte.valueholder;

import org.openelisglobal.common.valueholder.EnumValueItemImpl;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;

public class Analyte extends EnumValueItemImpl {

    private String id;

    // defined in EnumValueItemImpl
    // private String isActive;

    private String externalId;

    private ValueHolderInterface analyte;

    private String analyteName;

    private String selectedAnalyteId;

    // bugzilla 2432
    private String localAbbreviation;

    public Analyte() {
        super();
        this.analyte = new ValueHolder();
    }

    public String getId() {
        return this.id;
    }

    public String getIsActive() {
        return this.isActive;
    }

    public Analyte getAnalyte() {
        return (Analyte) this.analyte.getValue();
    }

    protected ValueHolderInterface getAnalyteHolder() {
        return this.analyte;
    }

    public String getAnalyteName() {
        return this.analyteName;
    }

    public void setId(String id) {
        this.id = id;
        // bugzilla 1625
        this.key = id;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    public void setAnalyte(Analyte analyte) {
        this.analyte.setValue(analyte);
    }

    protected void setAnalyteHolder(ValueHolderInterface analyte) {
        this.analyte = analyte;
    }

    public void setAnalyteName(String analyteName) {
        this.analyteName = analyteName;
        // bugzilla 1625
        this.name = analyteName;
    }

    public void setSelectedAnalyteId(String selectedAnalyteId) {
        this.selectedAnalyteId = selectedAnalyteId;
    }

    public String getSelectedAnalyteId() {
        return this.selectedAnalyteId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getLocalAbbreviation() {
        return localAbbreviation;
    }

    public void setLocalAbbreviation(String localAbbreviation) {
        this.localAbbreviation = localAbbreviation;
    }
}
