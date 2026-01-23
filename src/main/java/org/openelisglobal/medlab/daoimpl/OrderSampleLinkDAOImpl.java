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
package org.openelisglobal.medlab.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.medlab.dao.OrderSampleLinkDAO;
import org.openelisglobal.medlab.valueholder.OrderSampleLink;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for OrderSampleLink entity operations.
 *
 * <p>
 * Enables order-driven validation: samples without orders are rejected at QC.
 */
@Component
@Transactional
public class OrderSampleLinkDAOImpl extends BaseDAOImpl<OrderSampleLink, Integer> implements OrderSampleLinkDAO {

    public OrderSampleLinkDAOImpl() {
        super(OrderSampleLink.class);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasOrderForSample(Integer sampleId) {
        try {
            String sql = "SELECT COUNT(l) FROM OrderSampleLink l WHERE l.sampleId = :sampleId";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
            query.setParameter("sampleId", sampleId);
            Long count = query.getSingleResult();
            return count != null && count > 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in hasOrderForSample()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSampleLink> getLinksBySampleId(Integer sampleId) {
        try {
            String sql = "FROM OrderSampleLink l WHERE l.sampleId = :sampleId ORDER BY l.createdAt DESC";
            Query<OrderSampleLink> query = entityManager.unwrap(Session.class).createQuery(sql, OrderSampleLink.class);
            query.setParameter("sampleId", sampleId);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getLinksBySampleId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSampleLink> getLinksByOrderId(Integer electronicOrderId) {
        try {
            String sql = "FROM OrderSampleLink l WHERE l.electronicOrderId = :orderId ORDER BY l.createdAt DESC";
            Query<OrderSampleLink> query = entityManager.unwrap(Session.class).createQuery(sql, OrderSampleLink.class);
            query.setParameter("orderId", electronicOrderId);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getLinksByOrderId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSampleLink> getLinksBySampleItemId(Integer sampleItemId) {
        try {
            String sql = "FROM OrderSampleLink l WHERE l.sampleItemId = :sampleItemId ORDER BY l.createdAt DESC";
            Query<OrderSampleLink> query = entityManager.unwrap(Session.class).createQuery(sql, OrderSampleLink.class);
            query.setParameter("sampleItemId", sampleItemId);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getLinksBySampleItemId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSampleLink> getValidatedLinksByOrderId(Integer electronicOrderId) {
        try {
            String sql = "FROM OrderSampleLink l WHERE l.electronicOrderId = :orderId "
                    + "AND l.validated = true ORDER BY l.validatedAt DESC";
            Query<OrderSampleLink> query = entityManager.unwrap(Session.class).createQuery(sql, OrderSampleLink.class);
            query.setParameter("orderId", electronicOrderId);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getValidatedLinksByOrderId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSampleLink> getUnvalidatedLinks() {
        try {
            String sql = "FROM OrderSampleLink l WHERE l.validated = false ORDER BY l.createdAt ASC";
            Query<OrderSampleLink> query = entityManager.unwrap(Session.class).createQuery(sql, OrderSampleLink.class);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getUnvalidatedLinks()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderSampleLink getLinkByOrderSampleTest(Integer electronicOrderId, Integer sampleId, Integer testId) {
        try {
            String sql;
            Query<OrderSampleLink> query;
            if (testId != null) {
                sql = "FROM OrderSampleLink l WHERE l.electronicOrderId = :orderId "
                        + "AND l.sampleId = :sampleId AND l.testId = :testId";
                query = entityManager.unwrap(Session.class).createQuery(sql, OrderSampleLink.class);
                query.setParameter("orderId", electronicOrderId);
                query.setParameter("sampleId", sampleId);
                query.setParameter("testId", testId);
            } else {
                sql = "FROM OrderSampleLink l WHERE l.electronicOrderId = :orderId "
                        + "AND l.sampleId = :sampleId AND l.testId IS NULL";
                query = entityManager.unwrap(Session.class).createQuery(sql, OrderSampleLink.class);
                query.setParameter("orderId", electronicOrderId);
                query.setParameter("sampleId", sampleId);
            }
            List<OrderSampleLink> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getLinkByOrderSampleTest()", e);
        }
    }
}
