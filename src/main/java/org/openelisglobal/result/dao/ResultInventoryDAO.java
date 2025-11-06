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
package org.openelisglobal.result.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultInventory;

public interface ResultInventoryDAO extends BaseDAO<ResultInventory, String> {

    // public boolean insertData(ResultInventory resultInventory) throws
    // LIMSRuntimeException;

    // public void deleteData(List results) throws LIMSRuntimeException;

    List<ResultInventory> getAllResultInventorys() throws LIMSRuntimeException;

    void getData(ResultInventory resultInventory) throws LIMSRuntimeException;

    // public void updateData(ResultInventory resultInventory) throws
    // LIMSRuntimeException;

    ResultInventory getResultInventoryById(ResultInventory resultInventory) throws LIMSRuntimeException;

    List<ResultInventory> getResultInventorysByResult(Result result) throws LIMSRuntimeException;
}
