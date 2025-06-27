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
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.dataexchange.order.daoimpl;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.dataexchange.order.dao.ElectronicOrderDAO;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder.SortOrder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ElectronicOrderDAOImpl extends BaseDAOImpl<ElectronicOrder, String> implements ElectronicOrderDAO {

    public ElectronicOrderDAOImpl() {
        super(ElectronicOrder.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ElectronicOrder> getElectronicOrdersByExternalId(String id) throws LIMSRuntimeException {
        if (GenericValidator.isBlankOrNull(id)) {
            return new ArrayList<>();
        }
        String sql = "from ElectronicOrder eo where eo.externalId = :externalid order by id";

        try {
            Query<ElectronicOrder> query = entityManager.unwrap(Session.class).createQuery(sql, ElectronicOrder.class);
            query.setParameter("externalid", id);

            List<ElectronicOrder> eOrders = query.list();
            return eOrders;
        } catch (HibernateException e) {
            handleException(e, "getElectronicOrderByExternalId");
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ElectronicOrder> getAllElectronicOrdersOrderedBy(SortOrder order) {
        List<ElectronicOrder> list = new Vector<>();
        try {
            if (order.equals(ElectronicOrder.SortOrder.LAST_UPDATED_DESC)) {
                String sql = "from ElectronicOrder eo order by lastupdated desc";
                list = entityManager.unwrap(Session.class).createQuery(sql, ElectronicOrder.class).list();
            } else if (order.equals(ElectronicOrder.SortOrder.LAST_UPDATED_ASC)) {
                String sql = "from ElectronicOrder eo order by lastupdated asc";
                list = entityManager.unwrap(Session.class).createQuery(sql, ElectronicOrder.class).list();
            } else if (order.equals(SortOrder.STATUS_ID)) {
                String sql = "from ElectronicOrder eo order by status_id";
                list = entityManager.unwrap(Session.class).createQuery(sql, ElectronicOrder.class).list();
            } else {
                String sql = "from ElectronicOrder eo order by external_id";
                list = entityManager.unwrap(Session.class).createQuery(sql, ElectronicOrder.class).list();
            }
        } catch (RuntimeException e) {
            handleException(e, "getAllElectronicOrdersOrderedBy");
        }

        return list;
    }

    @Override
    public List<ElectronicOrder> getAllElectronicOrdersContainingValueOrderedBy(String searchValue, SortOrder order) {

        String sql = "from ElectronicOrder eo join eo.patient patient join patient.person person  where"
                + " lower(eo.data) like concat('%', lower(:searchValue), '%') or"
                + " lower(person.firstName) like concat('%', lower(:searchValue), '%') or"
                + " lower(person.lastName) like concat('%', lower(:searchValue), '%') or patient.id in"
                + " (SELECT identity.patientId FROM PatientIdentity identity WHERE"
                + " identity.identityData like concat('%', :searchValue, '%')) or patient.nationalId"
                + " like concat('%', :searchValue, '%') or lower(concat(person.firstName, ' ',"
                + " person.lastName)) like concat('%', lower(:searchValue), '%') order by ";

        switch (order.getValue()) {
        case "statusId":
            sql = sql + "eo.statusId asc";
            break;
        case "lastupdatedasc":
            sql = sql + "eo.statusId asc, eo.lastupdated asc";
            break;
        case "lastupdateddesc":
            sql = sql + "eo.statusId asc, eo.lastupdated desc";
            break;
        case "externalId":
            sql = sql + "eo.externalId asc";
            break;
        default:
            //
            break;
        }
        try {

            Query query = entityManager.unwrap(Session.class).createQuery(sql);
            query.setParameter("searchValue", searchValue);
            // query.setParameter("order", order.getValue());
            List records = query.list();
            List<ElectronicOrder> eOrders = new ArrayList<>();
            for (int i = 0; i < records.size(); i++) {
                Object[] oArray = (Object[]) records.get(i);
                ElectronicOrder eo = (ElectronicOrder) oArray[0];
                eOrders.add(eo);
            }
            return eOrders;
        } catch (HibernateException e) {
            handleException(e, "getAllElectronicOrdersContainingValue");
        }
        return null;
    }

    @Override
    public List<ElectronicOrder> getAllElectronicOrdersMatchingAnyValue(List<String> identifierValues,
            String patientValue, SortOrder order) {

        String hql = "from ElectronicOrder eo join eo.patient patient join patient.person person where"
                + " lower(eo.externalId) in (:identifierValues) or lower(person.firstName) ="
                + " lower(:patientValue) or lower(person.lastName) = lower(:patientValue) or patient.id"
                + " in (SELECT identity.patientId FROM PatientIdentity identity WHERE"
                + " lower(identity.identityData) = lower(:patientValue)) or lower(patient.nationalId) ="
                + " lower(:patientValue) or lower(concat(person.firstName, ' ', person.lastName)) ="
                + " lower(:patientValue) order by ";

        switch (order.getValue()) {
        case "statusId":
            hql = hql + "eo.statusId asc";
            break;
        case "lastupdatedasc":
            hql = hql + "eo.statusId asc, eo.lastupdated asc";
            break;
        case "lastupdateddesc":
            hql = hql + "eo.statusId asc, eo.lastupdated desc";
            break;
        case "externalId":
            hql = hql + "eo.externalId asc";
            break;
        default:
            //
            break;
        }
        try {

            Query<?> query = entityManager.unwrap(Session.class).createQuery(hql);
            query.setParameterList("identifierValues", identifierValues);
            query.setParameter("patientValue", patientValue);
            // query.setParameter("order", order.getValue());
            List<?> records = query.list();
            List<ElectronicOrder> eOrders = new ArrayList<>();
            for (int i = 0; i < records.size(); i++) {
                Object[] oArray = (Object[]) records.get(i);
                ElectronicOrder eo = (ElectronicOrder) oArray[0];
                eOrders.add(eo);
            }
            return eOrders;
        } catch (HibernateException e) {
            handleException(e, "getAllElectronicOrdersMatchingAnyValue");
        }
        return null;
    }

    @Override
    public List<ElectronicOrder> getElectronicOrdersContainingValueExludedByOrderedBy(String searchValue,
            List<Integer> excludedStatuses, SortOrder sortOrder) {

        String sql = "from ElectronicOrder eo join eo.patient patient join patient.person person  where"
                + " lower(eo.data) like concat('%', lower(:searchValue), '%') or"
                + " lower(person.firstName) like concat('%', lower(:searchValue), '%') or"
                + " lower(person.lastName) like concat('%', lower(:searchValue), '%') or"
                + " lower(concat(person.firstName, ' ', person.lastName)) like concat('%',"
                + " lower(:searchValue), '%')or patient.id in (SELECT identity.patientId FROM"
                + " PatientIdentity identity WHERE identity.identityData like concat('%', :searchValue,"
                + " '%')) or patient.nationalId like concat('%', :searchValue, '%') and eo.statusId not"
                + " in (:excludedStatuses) order by ";

        switch (sortOrder) {
        case STATUS_ID:
            sql = sql + "eo.statusId asc";
            break;
        case LAST_UPDATED_ASC:
            sql = sql + "eo.statusId asc, eo.lastupdated asc";
            break;
        case LAST_UPDATED_DESC:
            sql = sql + "eo.statusId asc, eo.lastupdated desc";
            break;
        case EXTERNAL_ID:
            sql = sql + "eo.externalId asc";
            break;
        default:
            //
            break;
        }
        try {

            Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);
            query.setParameter("searchValue", searchValue);
            query.setParameter("excludedStatuses", excludedStatuses);
            // query.setParameter("order", order.getValue());
            List<?> records = query.list();
            List<ElectronicOrder> eOrders = new ArrayList<>();
            for (int i = 0; i < records.size(); i++) {
                Object[] oArray = (Object[]) records.get(i);
                ElectronicOrder eo = (ElectronicOrder) oArray[0];
                eOrders.add(eo);
            }
            return eOrders;
        } catch (HibernateException e) {
            handleException(e, "getAllElectronicOrdersContainingValue");
        }
        return null;
    }

    @Override
    public List<ElectronicOrder> getAllElectronicOrdersContainingValuesOrderedBy(String accessionNumber,
            String patientLastName, String patientFirstName, String gender, SortOrder order) {
        String sql = "from ElectronicOrder eo " + "join eo.patient patient " + "join patient.person person  ";
        boolean whereClauseStarted = false;
        if (!GenericValidator.isBlankOrNull(accessionNumber)) {
            sql += getWherePrefix(whereClauseStarted)
                    + " lower(eo.data) like concat('%', lower(:accessionNumber), '%') ";
            whereClauseStarted = true;
        }
        // if (!GenericValidator.isBlankOrNull(patientId)) {
        // sql += getWherePrefix(whereClauseStarted) + "and lower(eo.data) like
        // concat('%',
        // lower(:patientId), '%') ";
        // }
        if (!GenericValidator.isBlankOrNull(patientLastName)) {
            sql += getWherePrefix(whereClauseStarted)
                    + " lower(person.lastName) like concat('%', lower(:patientLastName), '%') ";
            whereClauseStarted = true;
        }
        if (!GenericValidator.isBlankOrNull(patientFirstName)) {
            sql += getWherePrefix(whereClauseStarted)
                    + " lower(person.firstName) like concat('%', lower(:patientFirstName), '%') ";
            whereClauseStarted = true;
        }
        // if (!GenericValidator.isBlankOrNull(dateOfBirth)) {
        // sql += getWherePrefix(whereClauseStarted) + "lower(patient.birthDate) like
        // concat('%', lower(:dateOfBirth), '%') ";
        // }
        if (!GenericValidator.isBlankOrNull(gender)) {
            sql += getWherePrefix(whereClauseStarted) + " lower(patient.gender) = lower(:gender) ";
            whereClauseStarted = true;
        }
        sql += " order by ";

        switch (order.getValue()) {
        case "statusId":
            sql = sql + "eo.statusId asc";
            break;
        case "lastupdatedasc":
            sql = sql + "eo.statusId asc, eo.lastupdated asc";
            break;
        case "lastupdateddesc":
            sql = sql + "eo.statusId asc, eo.lastupdated desc";
            break;
        case "externalId":
            sql = sql + "eo.externalId asc";
            break;
        default:
            //
            break;
        }
        try {

            Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);
            if (!GenericValidator.isBlankOrNull(accessionNumber)) {
                query.setParameter("accessionNumber", accessionNumber);
            }
            if (!GenericValidator.isBlankOrNull(patientLastName)) {
                query.setParameter("patientLastName", patientLastName);
            }
            if (!GenericValidator.isBlankOrNull(patientFirstName)) {
                query.setParameter("patientFirstName", patientFirstName);
            }
            if (!GenericValidator.isBlankOrNull(gender)) {
                query.setParameter("gender", gender);
            }
            // query.setParameter("order", order.getValue());
            List<?> records = query.list();
            List<ElectronicOrder> eOrders = new ArrayList<>();
            for (int i = 0; i < records.size(); i++) {
                Object[] oArray = (Object[]) records.get(i);
                ElectronicOrder eo = (ElectronicOrder) oArray[0];
                eOrders.add(eo);
            }
            return eOrders;
        } catch (HibernateException e) {
            handleException(e, "getAllElectronicOrdersContainingValue");
        }
        return null;
    }

    private String getWherePrefix(boolean whereClauseStarted) {
        if (!whereClauseStarted) {
            return " where ";
        } else {
            return " and ";
        }
    }

    @Override
    public List<ElectronicOrder> getAllElectronicOrdersByDateAndStatus(Date startDate, Date endDate, String statusId,
            SortOrder sortOrder) {
        String hql = "From ElectronicOrder eo WHERE 1 = 1 ";
        if (startDate != null) {
            hql += "AND eo.orderTimestamp BETWEEN :startDate AND :endDate ";
        }
        if (!GenericValidator.isBlankOrNull(statusId)) {
            hql += "AND eo.statusId = :statusId ";
        }

        switch (sortOrder) {
        case STATUS_ID:
            hql += "ORDER BY eo.statusId asc ";
            break;
        case LAST_UPDATED_ASC:
            hql += "ORDER BY eo.lastupdated asc ";
            break;
        case LAST_UPDATED_DESC:
            hql += "ORDER BY eo.lastupdated desc ";
            break;
        case EXTERNAL_ID:
            hql += "ORDER BY eo.externalId asc ";
            break;
        default:
            //
            break;
        }

        try {
            Query<ElectronicOrder> query = entityManager.unwrap(Session.class).createQuery(hql, ElectronicOrder.class);
            if (startDate != null) {
                query.setParameter("startDate", startDate);
                query.setParameter("endDate", endDate);
            }
            if (!GenericValidator.isBlankOrNull(statusId)) {
                query.setParameter("statusId", Integer.parseInt(statusId));
            }
            return query.list();
        } catch (HibernateException e) {
            handleException(e, "getAllElectronicOrdersByDateAndStatus");
        }
        return null;
    }

    @Override
    public List<ElectronicOrder> getAllElectronicOrdersByTimestampAndStatus(java.sql.Timestamp startTimestamp,
            java.sql.Timestamp endTimestamp, String statusId, SortOrder sortOrder) {
        String hql = "From ElectronicOrder eo WHERE 1 = 1 ";
        if (startTimestamp != null) {
            hql += "AND eo.orderTimestamp BETWEEN :startDate AND :endDate ";
        }
        if (!GenericValidator.isBlankOrNull(statusId)) {
            hql += "AND eo.statusId = :statusId ";
        }

        switch (sortOrder) {
        case STATUS_ID:
            hql += "ORDER BY eo.statusId asc ";
            break;
        case LAST_UPDATED_ASC:
            hql += "ORDER BY eo.lastupdated asc ";
            break;
        case LAST_UPDATED_DESC:
            hql += "ORDER BY eo.lastupdated desc ";
            break;
        case EXTERNAL_ID:
            hql += "ORDER BY eo.externalId asc ";
            break;
        default:
            //
            break;
        }

        try {
            Query<ElectronicOrder> query = entityManager.unwrap(Session.class).createQuery(hql, ElectronicOrder.class);
            if (startTimestamp != null) {
                query.setParameter("startDate", startTimestamp);
                query.setParameter("endDate", endTimestamp);
            }
            if (!GenericValidator.isBlankOrNull(statusId)) {
                query.setParameter("statusId", Integer.parseInt(statusId));
            }
            return query.list();
        } catch (HibernateException e) {
            handleException(e, "getAllElectronicOrdersByDateAndStatus");
        }
        return null;
    }

    @Override
    public int getCountOfElectronicOrdersByTimestampAndStatus(java.sql.Timestamp startTimestamp,
            java.sql.Timestamp endTimestamp, String statusId) {
        String hql = "SELECT COUNT(*) From ElectronicOrder eo WHERE 1 = 1 ";
        if (startTimestamp != null) {
            hql += "AND eo.orderTimestamp BETWEEN :startDate AND :endDate ";
        }
        if (!GenericValidator.isBlankOrNull(statusId)) {
            hql += "AND eo.statusId = :statusId ";
        }

        try {
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            if (startTimestamp != null) {
                query.setParameter("startDate", startTimestamp);
                query.setParameter("endDate", endTimestamp);
            }
            if (!GenericValidator.isBlankOrNull(statusId)) {
                query.setParameter("statusId", Integer.parseInt(statusId));
            }
            Long count = query.uniqueResult();
            return count.intValue();
        } catch (HibernateException e) {
            handleException(e, "getAllElectronicOrdersByDateAndStatus");
        }
        return 0;
    }

    @Override
    public int getCountOfAllElectronicOrdersByDateAndStatus(Date startDate, Date endDate, String statusId) {
        String hql = "SELECT COUNT(*) From ElectronicOrder eo WHERE 1 = 1 ";
        if (startDate != null) {
            hql += "AND eo.orderTimestamp BETWEEN :startDate AND :endDate ";
        }
        if (!GenericValidator.isBlankOrNull(statusId)) {
            hql += "AND eo.statusId = :statusId ";
        }

        try {
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            if (startDate != null) {
                query.setParameter("startDate", startDate);
                query.setParameter("endDate", endDate);
            }
            if (!GenericValidator.isBlankOrNull(statusId)) {
                query.setParameter("statusId", Integer.parseInt(statusId));
            }
            Long count = query.uniqueResult();
            return count.intValue();
        } catch (HibernateException e) {
            handleException(e, "getCountOfAllElectronicOrdersByDateAndStatus");
        }
        return 0;
    }

    @Override
    public int getCountOfElectronicOrdersByStatusList(List<Integer> statusIds) {
        String hql = "SELECT COUNT(*) From ElectronicOrder eo WHERE 1 = 1 ";

        if (statusIds != null) {
            hql += "AND eo.statusId IN (:statusIds)";
        }

        try {
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            if (statusIds != null) {
                query.setParameter("statusIds", statusIds);
            }
            Long count = query.uniqueResult();
            return count.intValue();
        } catch (HibernateException e) {
            handleException(e, "getCountOfElectronicOrdersByStatusList");
        }
        return 0;
    }

    @Override
    public List<ElectronicOrder> getAllElectronicOrdersByStatusList(List<Integer> statusIds, SortOrder sortOrder) {
        String hql = "From ElectronicOrder eo WHERE 1 = 1 ";

        if (statusIds != null) {
            hql += "AND eo.statusId IN (:statusIds)";
        }

        switch (sortOrder) {
        case STATUS_ID:
            hql += "ORDER BY eo.statusId asc ";
            break;
        case LAST_UPDATED_ASC:
            hql += "ORDER BY eo.lastupdated asc ";
            break;
        case LAST_UPDATED_DESC:
            hql += "ORDER BY eo.lastupdated desc ";
            break;
        case EXTERNAL_ID:
            hql += "ORDER BY eo.externalId asc ";
            break;
        default:
            //
            break;
        }

        try {
            Query<ElectronicOrder> query = entityManager.unwrap(Session.class).createQuery(hql, ElectronicOrder.class);

            if (statusIds != null) {
                query.setParameter("statusIds", statusIds);
            }
            return query.list();
        } catch (HibernateException e) {
            handleException(e, "getAllElectronicOrdersByStatusList");
        }
        return new ArrayList<>();
    }
}
