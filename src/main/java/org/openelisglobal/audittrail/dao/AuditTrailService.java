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
package org.openelisglobal.audittrail.dao;

import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * @author Hung Nguyen
 * @date created 09/12/2006
 */
public interface AuditTrailService {

    void saveHistory(BaseObject newObject, BaseObject existingObject, String sysUserId, String event, String tableName)
            throws LIMSRuntimeException;

    void saveNewHistory(BaseObject newObject, String sysUserId, String tableName) throws LIMSRuntimeException;

    String getXML(String table, String id) throws LIMSRuntimeException;

    String getXMLData(String table, String id) throws LIMSRuntimeException;

    // public List getHistoryByRefIdAndRefTableId(History history) throws
    // LIMSRuntimeException;

    // public List getHistoryByRefIdAndRefTableId(String refId, String tableId)
    // throws LIMSRuntimeException;

    // public List getHistoryBySystemUserAndDateAndRefTableId(History history)
    // throws LIMSRuntimeException;

    // public String retrieveBlobData(String id) throws LIMSRuntimeException;

    // public List<History> getHistoryByRefTableIdAndDateRange(String
    // referenceTableId, Date start, Date end)
    // throws LIMSRuntimeException;
}
