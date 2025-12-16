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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.medlab.dao;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.medlab.valueholder.TransportPackaging;

/** DAO interface for TransportPackaging entity operations. */
public interface TransportPackagingDAO extends BaseDAO<TransportPackaging, Integer> {

    /**
     * Get transport packaging by shipment ID.
     *
     * @param shipmentId the shipment identifier
     * @return transport packaging or null if not found
     */
    TransportPackaging getByShipmentId(String shipmentId);

    /**
     * Get all transport packaging records received within a date range.
     *
     * @param startDate start of date range
     * @param endDate   end of date range
     * @return list of transport packaging records
     */
    List<TransportPackaging> getByReceivedDateRange(Timestamp startDate, Timestamp endDate);

    /**
     * Get all non-compliant (IATA PI650) packaging records.
     *
     * @return list of non-compliant records
     */
    List<TransportPackaging> getNonCompliantPackaging();

    /**
     * Get packaging records by compliance status.
     *
     * @param compliant true for compliant, false for non-compliant
     * @return list of transport packaging records
     */
    List<TransportPackaging> getByComplianceStatus(boolean compliant);

    /**
     * Get packaging records by tracking number.
     *
     * @param trackingNumber the courier tracking number
     * @return transport packaging or null if not found
     */
    TransportPackaging getByTrackingNumber(String trackingNumber);
}
