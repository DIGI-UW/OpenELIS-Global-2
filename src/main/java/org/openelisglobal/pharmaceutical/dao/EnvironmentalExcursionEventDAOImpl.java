package org.openelisglobal.pharmaceutical.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.pharmaceutical.valueholder.EnvironmentalExcursionEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EnvironmentalExcursionEventDAOImpl extends BaseDAOImpl<EnvironmentalExcursionEvent, Integer>
        implements EnvironmentalExcursionEventDAO {

    public EnvironmentalExcursionEventDAOImpl() {
        super(EnvironmentalExcursionEvent.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnvironmentalExcursionEvent> findByDeviceId(Integer deviceId) {
        try {
            String hql = "FROM EnvironmentalExcursionEvent WHERE deviceId = :deviceId ORDER BY detectedAt DESC";
            Query<EnvironmentalExcursionEvent> query = entityManager.unwrap(Session.class)
                    .createQuery(hql, EnvironmentalExcursionEvent.class);
            query.setParameter("deviceId", deviceId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding EnvironmentalExcursionEvents by deviceId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnvironmentalExcursionEvent> findByStatus(EnvironmentalExcursionEvent.ExcursionStatus status) {
        try {
            String hql = "FROM EnvironmentalExcursionEvent WHERE status = :status ORDER BY detectedAt DESC";
            Query<EnvironmentalExcursionEvent> query = entityManager.unwrap(Session.class)
                    .createQuery(hql, EnvironmentalExcursionEvent.class);
            query.setParameter("status", status);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding EnvironmentalExcursionEvents by status", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnvironmentalExcursionEvent> findActiveExcursions() {
        try {
            String hql = "FROM EnvironmentalExcursionEvent WHERE status = 'ACTIVE' ORDER BY detectedAt DESC";
            Query<EnvironmentalExcursionEvent> query = entityManager.unwrap(Session.class)
                    .createQuery(hql, EnvironmentalExcursionEvent.class);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding active EnvironmentalExcursionEvents", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnvironmentalExcursionEvent> findByAlertType(EnvironmentalExcursionEvent.AlertType alertType) {
        try {
            String hql = "FROM EnvironmentalExcursionEvent WHERE alertType = :alertType ORDER BY detectedAt DESC";
            Query<EnvironmentalExcursionEvent> query = entityManager.unwrap(Session.class)
                    .createQuery(hql, EnvironmentalExcursionEvent.class);
            query.setParameter("alertType", alertType);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding EnvironmentalExcursionEvents by alertType", e);
        }
    }
}
