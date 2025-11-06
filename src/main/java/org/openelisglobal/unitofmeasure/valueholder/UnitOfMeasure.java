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
package org.openelisglobal.unitofmeasure.valueholder;

import org.openelisglobal.common.valueholder.EnumValueItemImpl;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.localization.valueholder.Localization;

public class UnitOfMeasure extends EnumValueItemImpl {

    private String id;

    private String unitOfMeasureName;

    private String description;

    private ValueHolderInterface localization;

    public UnitOfMeasure() {
        super();
    }

    public void setId(String id) {
        this.id = id;
        this.key = id;
    }

    public String getId() {
        return id;
    }

    public void setUnitOfMeasureName(String unitOfMeasureName) {
        this.unitOfMeasureName = unitOfMeasureName;
        this.name = unitOfMeasureName;
    }

    public String getUnitOfMeasureName() {
        return unitOfMeasureName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    protected String getDefaultLocalizedName() {
        return getUnitOfMeasureName();
    }

    public Localization getLocalization() {
        // return (Localization)localization.getValue();
        //
        // UOM has been designed to support localization,
        // this method is the break point, to support localization
        // add columns to database table and Hibernation interface
        // then call localization.getValue above
        //

        Localization _localization = new Localization();
        _localization.setId(this.getId());
        _localization.setDescription(this.getDescription());
        _localization.setEnglish(this.getDefaultLocalizedName());
        _localization.setFrench("French");

        return (Localization) _localization;
    }

    public void setLocalization(Localization localization) {
        this.localization.setValue(localization);
    }
}
