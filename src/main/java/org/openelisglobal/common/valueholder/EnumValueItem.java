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

public interface EnumValueItem {
    // enum name
    public String getEnumName();

    public void setEnumName(String enumName);

    // Each enumValueItem can be retrieved using a string key
    public String getKey();

    public void setKey(String key);

    // is enum active
    public String getIsActive();

    public void setIsActive(String pActive);

    // enumValueItem name
    public String getName();

    public void setName(String pName);

    // enumValueItem sortorder
    public String getSortOrder();

    public void setSortOrder(String pSortorder);
}
