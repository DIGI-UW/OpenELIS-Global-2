package org.openelisglobal.lesotho.clinicalinfo.dao;

import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.lesotho.clinicalinfo.valueholder.OrderClinicalInfo;

public interface OrderClinicalInfoDAO extends BaseDAO<OrderClinicalInfo, Integer> {

    /** The newest saved version for the order's sample, if any. */
    Optional<OrderClinicalInfo> getLatestBySampleId(String sampleId);
}
