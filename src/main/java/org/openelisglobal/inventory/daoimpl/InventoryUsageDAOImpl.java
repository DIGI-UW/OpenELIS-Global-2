package org.openelisglobal.inventory.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.dao.InventoryUsageDAO;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class InventoryUsageDAOImpl extends BaseDAOImpl<InventoryUsage, Long> implements InventoryUsageDAO {

    public InventoryUsageDAOImpl() {
        super(InventoryUsage.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryUsage> getByTestResultId(Long testResultId) throws LIMSRuntimeException {
        try {
            String hql = "FROM InventoryUsage u WHERE u.testResultId = :testResultId ORDER BY u.usageDate DESC";
            Query<InventoryUsage> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryUsage.class);
            query.setParameter("testResultId", testResultId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting usage by test result ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryUsage> getByLotId(Long lotId) throws LIMSRuntimeException {
        try {
            String hql = "FROM InventoryUsage u WHERE u.lot.id = :lotId ORDER BY u.usageDate DESC";
            Query<InventoryUsage> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryUsage.class);
            query.setParameter("lotId", lotId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting usage by lot ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryUsage> getByInventoryItemId(Long itemId) throws LIMSRuntimeException {
        try {
            String hql = "FROM InventoryUsage u WHERE u.inventoryItem.id = :itemId ORDER BY u.usageDate DESC";
            Query<InventoryUsage> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryUsage.class);
            query.setParameter("itemId", itemId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting usage by inventory item ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryUsage> getByAnalysisId(Long analysisId) throws LIMSRuntimeException {
        try {
            String hql = "FROM InventoryUsage u WHERE u.analysisId = :analysisId ORDER BY u.usageDate DESC";
            Query<InventoryUsage> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryUsage.class);
            query.setParameter("analysisId", analysisId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting usage by analysis ID", e);
        }
    }
}
