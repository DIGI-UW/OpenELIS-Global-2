package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerDeliveryAction;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerDeliveryActionDAOImpl extends BaseDAOImpl<AnalyzerDeliveryAction, String>
        implements AnalyzerDeliveryActionDAO {

    public AnalyzerDeliveryActionDAOImpl() {
        super(AnalyzerDeliveryAction.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerDeliveryAction> findByOutboxEntryId(String outboxEntryId) {
        return entityManager
                .createQuery("FROM AnalyzerDeliveryAction action WHERE action.outboxEntryId = :outboxEntryId "
                        + "ORDER BY action.actedAt, action.id", AnalyzerDeliveryAction.class)
                .setParameter("outboxEntryId", outboxEntryId).getResultList();
    }
}
