package org.openelisglobal.labelpreset.dao;

import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.labelpreset.valueholder.OrderLabelRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class OrderLabelRequestDAOImpl extends BaseDAOImpl<OrderLabelRequest, Integer> implements OrderLabelRequestDAO {

    public OrderLabelRequestDAOImpl() {
        super(OrderLabelRequest.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderLabelRequest> listByParentSampleId(String parentSampleId) {
        return entityManager.createQuery("FROM OrderLabelRequest o WHERE o.parentSample.id = :parentSampleId",
                OrderLabelRequest.class).setParameter("parentSampleId", parentSampleId).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderLabelRequest> listBySampleItemId(String sampleItemId) {
        return entityManager
                .createQuery("FROM OrderLabelRequest o WHERE o.sampleItem.id = :sampleItemId", OrderLabelRequest.class)
                .setParameter("sampleItemId", sampleItemId).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderLabelRequest> listByPresetId(Integer presetId) {
        return entityManager
                .createQuery("FROM OrderLabelRequest o WHERE o.preset.id = :presetId", OrderLabelRequest.class)
                .setParameter("presetId", presetId).getResultList();
    }
}
