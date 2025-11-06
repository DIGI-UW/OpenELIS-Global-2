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
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.dataexchange.order.dao;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder.SortOrder;

public interface ElectronicOrderDAO extends BaseDAO<ElectronicOrder, String> {

    public List<ElectronicOrder> getElectronicOrdersByExternalId(String id) throws LIMSRuntimeException;

    // public List<ElectronicOrder> getElectronicOrdersByPatientId(String id) throws
    // LIMSRuntimeException;

    // public void updateData(ElectronicOrder eOrder) throws LIMSRuntimeException;

    // public List<ElectronicOrder> getAllElectronicOrders();

    List<ElectronicOrder> getAllElectronicOrdersOrderedBy(ElectronicOrder.SortOrder order);

    public List<ElectronicOrder> getAllElectronicOrdersContainingValueOrderedBy(String searchValue, SortOrder order);

    List<ElectronicOrder> getAllElectronicOrdersContainingValuesOrderedBy(String accessionNumber,
            String patientLastName, String patientFirstName, String gender, SortOrder order);

    public List<ElectronicOrder> getElectronicOrdersContainingValueExludedByOrderedBy(String searchValue,
            List<Integer> exludedStatusIds, SortOrder sortOrder);

    List<ElectronicOrder> getAllElectronicOrdersByDateAndStatus(Date startDate, Date endDate, String statusId,
            SortOrder sortOrder);

    List<ElectronicOrder> getAllElectronicOrdersByTimestampAndStatus(Timestamp startTimestamp, Timestamp endTimestamp,
            String statusId, SortOrder sortOrder);

    int getCountOfElectronicOrdersByTimestampAndStatus(Timestamp startTimestamp, Timestamp endTimestamp,
            String statusId);

    public List<ElectronicOrder> getAllElectronicOrdersMatchingAnyValue(List<String> identifierValues,
            String patientValue, SortOrder order);

    int getCountOfAllElectronicOrdersByDateAndStatus(Date startDate, Date endDate, String statusId);

    int getCountOfElectronicOrdersByStatusList(List<Integer> statusIds);

    List<ElectronicOrder> getAllElectronicOrdersByStatusList(List<Integer> statusIds, SortOrder sortOrder);
}
