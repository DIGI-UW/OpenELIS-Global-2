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
package org.openelisglobal.inventory.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.valueholder.InventoryItem;

public interface InventoryItemDAO extends BaseDAO<InventoryItem, String> {

    // public boolean insertData(InventoryItem InventoryItem) throws
    // LIMSRuntimeException;

    // public void deleteData(List<InventoryItem> results) throws
    // LIMSRuntimeException;

    public List<InventoryItem> getAllInventoryItems() throws LIMSRuntimeException;

    public InventoryItem readInventoryItem(String idString) throws LIMSRuntimeException;

    // public void getData(InventoryItem inventoryItem) throws LIMSRuntimeException;

    // public void updateData(InventoryItem inventoryItem) throws
    // LIMSRuntimeException;

    // public InventoryItem getInventoryItemById(InventoryItem inventoryItem) throws
    // LIMSRuntimeException;

}
