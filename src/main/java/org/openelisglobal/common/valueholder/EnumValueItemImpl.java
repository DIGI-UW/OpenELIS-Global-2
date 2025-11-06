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
package org.openelisglobal.common.valueholder;

import org.openelisglobal.common.action.IActionConstants;

public abstract class EnumValueItemImpl extends BaseObject<String> implements EnumValueItem {

    protected String name;

    protected String enumName;

    protected String sortOrder;

    protected String key;

    protected String isActive = IActionConstants.YES;

    // enum name
    @Override
    public String getEnumName() {
        return enumName;
    }

    @Override
    public void setEnumName(String enumName) {
        this.enumName = enumName;
    }

    // Each enumValueItem can be retrieved using a string key
    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(String key) {
        this.key = key;
    }

    // enumValueItem name
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String pName) {
        name = pName;
    }

    // enumValueItem sortorder
    @Override
    public String getSortOrder() {
        return sortOrder;
    }

    @Override
    public void setSortOrder(String pSortorder) {
        sortOrder = pSortorder;
    }

    // is enum active
    @Override
    public String getIsActive() {
        return isActive;
    }

    @Override
    public void setIsActive(String pActive) {
        isActive = pActive;
    }
}
