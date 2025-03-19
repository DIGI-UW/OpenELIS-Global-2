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
package org.openelisglobal.common.servlet.selectdropdown;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @author benzd1
 *         <p>
 *         bugzilla 1844: extending AjaxXmlBuilder for testsection->test select
 *         making extended version of AjaxJspTag.Select sortable (toggle between
 *         sorting by 2 different properties, toggling label value between 2
 *         different values according to sort
 */
class SortableTestItem {

    protected String name;

    protected String value;

    protected String sortFieldA;

    protected String sortFieldB;

    protected String alternateLabel;

    protected boolean asData;

    /** Constructor for Item. */
    public SortableTestItem() {
        super();
    }

    /**
     * Constructor for Item.
     *
     * @param name
     * @param value
     */
    public SortableTestItem(String name, String value, String sortFieldA, String sortFieldB, String alternateLabel,
            boolean asData) {
        super();
        this.name = name;
        this.value = value;
        this.sortFieldA = sortFieldA;
        this.sortFieldB = sortFieldB;
        this.alternateLabel = alternateLabel;
        this.asData = asData;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Returns the value.
     */
    public String getValue() {
        return this.value;
    }

    /**
     * @param value The value to set.
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return Returns the asCData.
     */
    public boolean isAsCData() {
        return this.asData;
    }

    /**
     * @param asData The asData to set.
     */
    public void setAsData(boolean asData) {
        this.asData = asData;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return new ToStringBuilder(this).append("name", name).append("value", value).append("asData", asData)
                .toString();
    }

    public String getSortFieldA() {
        return sortFieldA;
    }

    public void setSortFieldA(String sortFieldA) {
        this.sortFieldA = sortFieldA;
    }

    public String getSortFieldB() {
        return sortFieldB;
    }

    public void setSortFieldB(String sortFieldB) {
        this.sortFieldB = sortFieldB;
    }

    public String getAlternateLabel() {
        return alternateLabel;
    }

    public void setAlternateLabel(String alternateLabel) {
        this.alternateLabel = alternateLabel;
    }
}
