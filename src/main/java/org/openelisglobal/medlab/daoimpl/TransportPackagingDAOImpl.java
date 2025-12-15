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

import java.sql.Timestamp;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.medlab.dao.TransportPackagingDAO;
import org.openelisglobal.medlab.valueholder.TransportPackaging;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for TransportPackaging entity operations.
 */
@Component
@Transactional
public class TransportPackagingDAOImpl extends BaseDAOImpl<TransportPackaging, Integer>
        implements TransportPackagingDAO {

    public TransportPackagingDAOImpl() {
        super(TransportPackaging.class);
    }

    @Override
    @Transactional(readOnly = true)
    public TransportPackaging getByShipmentId(String shipmentId) {
        try {
            String sql = "FROM TransportPackaging tp WHERE tp.shipmentId = :shipmentId";
            Query<TransportPackaging> query = entityManager.unwrap(Session.class).createQuery(sql,
                    TransportPackaging.class);
            query.setParameter("shipmentId", shipmentId);
            List<TransportPackaging> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getByShipmentId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransportPackaging> getByReceivedDateRange(Timestamp startDate, Timestamp endDate) {
        try {
            String sql = "FROM TransportPackaging tp WHERE tp.receivedDate BETWEEN :startDate AND :endDate "
                    + "ORDER BY tp.receivedDate DESC";
            Query<TransportPackaging> query = entityManager.unwrap(Session.class).createQuery(sql,
                    TransportPackaging.class);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getByReceivedDateRange()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransportPackaging> getNonCompliantPackaging() {
        try {
            String sql = "FROM TransportPackaging tp WHERE tp.iataPi650Compliant = false "
                    + "ORDER BY tp.receivedDate DESC";
            Query<TransportPackaging> query = entityManager.unwrap(Session.class).createQuery(sql,
                    TransportPackaging.class);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getNonCompliantPackaging()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransportPackaging> getByComplianceStatus(boolean compliant) {
        try {
            String sql = "FROM TransportPackaging tp WHERE tp.iataPi650Compliant = :compliant "
                    + "ORDER BY tp.receivedDate DESC";
            Query<TransportPackaging> query = entityManager.unwrap(Session.class).createQuery(sql,
                    TransportPackaging.class);
            query.setParameter("compliant", compliant);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getByComplianceStatus()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TransportPackaging getByTrackingNumber(String trackingNumber) {
        try {
            String sql = "FROM TransportPackaging tp WHERE tp.tertiaryTrackingNumber = :trackingNumber";
            Query<TransportPackaging> query = entityManager.unwrap(Session.class).createQuery(sql,
                    TransportPackaging.class);
            query.setParameter("trackingNumber", trackingNumber);
            List<TransportPackaging> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getByTrackingNumber()", e);
        }
    }
}
