package org.openelisglobal.lesotho.clinicalinfo.service;

import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.lesotho.clinicalinfo.valueholder.OrderClinicalInfo;

public interface OrderClinicalInfoService extends BaseObjectService<OrderClinicalInfo, Integer> {

    /** Largest accepted clinical information document, in characters. */
    int MAX_LENGTH = 65536;

    /** The newest version for the order with this lab number, if any. */
    Optional<OrderClinicalInfo> getLatest(String accessionNumber);

    /**
     * Saves the clinical information of the order with this lab number as a new
     * version, or does nothing when it equals the latest saved version.
     *
     * @return the saved version, or the unchanged latest one
     * @throws IllegalArgumentException when the data is not a JSON object, is too
     *                                  long, or no order has this lab number
     */
    SaveResult save(String accessionNumber, String clinicalDataJson, String sysUserId);

    record SaveResult(OrderClinicalInfo info, boolean changed) {
    }
}
