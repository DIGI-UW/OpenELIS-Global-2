package org.openelisglobal.labelpreset.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.labelpreset.valueholder.OrderLabelRequest;

public interface OrderLabelRequestDAO extends BaseDAO<OrderLabelRequest, Integer> {

    /** All label requests rooted at a parent sample (the "order"). */
    List<OrderLabelRequest> listByParentSampleId(String parentSampleId);

    /** All label requests for a specific sample item. */
    List<OrderLabelRequest> listBySampleItemId(String sampleItemId);

    /** All label requests referencing a preset. */
    List<OrderLabelRequest> listByPresetId(Integer presetId);
}
