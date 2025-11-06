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
package org.openelisglobal.panel.valueholder;

import java.util.Objects;
import org.openelisglobal.common.valueholder.EnumValueItemImpl;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.spring.util.SpringContext;

public class Panel extends EnumValueItemImpl {

    private static final long serialVersionUID = 1L;

    private String id;
    private String panelName;
    private String description;
    private String loinc;

    private int sortOrderInt;
    private ValueHolder localization = new ValueHolder();

    public Panel() {
        super();
    }

    @Override
    public String getId() {
        return id;
    }

    public String getPanelName() {
        return panelName;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public void setPanelName(String panelName) {
        this.panelName = panelName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLoinc() {
        return loinc;
    }

    public void setLoinc(String loinc) {
        this.loinc = loinc;
    }

    @Override
    protected String getDefaultLocalizedName() {
        return SpringContext.getBean(LocalizationService.class).getLocalizedValueById(getLocalization().getId());
    }

    public int getSortOrderInt() {
        return sortOrderInt;
    }

    public void setSortOrderInt(int sortOrderInt) {
        this.sortOrderInt = sortOrderInt;
    }

    public Localization getLocalization() {
        return (Localization) localization.getValue();
    }

    public void setLocalization(Localization localization) {
        this.localization.setValue(localization);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Panel that = (Panel) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
