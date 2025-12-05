package org.openelisglobal.inventory.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class InventoryUsageDAOImpl extends BaseDAOImpl<InventoryUsage, String> implements InventoryUsageDAO {

    public InventoryUsageDAOImpl() {
        super(InventoryUsage.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryUsage> findByLotId(String lotId) {
        try {
            String hql = "FROM InventoryUsage u WHERE u.lot.id = :lotId ORDER BY u.usageDate DESC";
            Query<InventoryUsage> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryUsage.class);
            query.setParameter("lotId", lotId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding usage by lot ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryUsage> findByTestResultId(String testResultId) {
        try {
            String hql = "FROM InventoryUsage u WHERE u.testResultId = :testResultId ORDER BY u.usageDate DESC";
            Query<InventoryUsage> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryUsage.class);
            query.setParameter("testResultId", testResultId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding usage by test result ID", e);
        }
    }
}
