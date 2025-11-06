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
package org.openelisglobal.referencetables.valueholder;

import org.openelisglobal.common.valueholder.EnumValueItemImpl;

// bugzilla 2571 go through ReferenceTablesService to get reference tables info
public class ReferenceTables extends EnumValueItemImpl {

    private String id;

    private String tableName;

    private String keepHistory;

    private String isHl7Encoded;

    public ReferenceTables() {
        super();
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
        // bugzilla 2571 go through ReferenceTablesService to get reference tables info
        name = tableName;
    }

    public String getKeepHistory() {
        return keepHistory;
    }

    public void setKeepHistory(String keepHistory) {
        this.keepHistory = keepHistory;
    }

    public String getIsHl7Encoded() {
        return isHl7Encoded;
    }

    public void setIsHl7Encoded(String isHl7Encoded) {
        this.isHl7Encoded = isHl7Encoded;
    }
}
