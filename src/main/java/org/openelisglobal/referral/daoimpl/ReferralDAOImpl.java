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
package org.openelisglobal.referral.daoimpl;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.referral.dao.ReferralDAO;
import org.openelisglobal.referral.form.ReferredOutTestsForm.ReferDateType;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
 */
@Component
@Transactional
public class ReferralDAOImpl extends BaseDAOImpl<Referral, String> implements ReferralDAO {

    public ReferralDAOImpl() {
        super(Referral.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Referral getReferralById(String referralId) throws LIMSRuntimeException {
        try {
            Referral referral = entityManager.unwrap(Session.class).get(Referral.class, referralId);
            return referral;
        } catch (HibernateException e) {
            handleException(e, "getReferralById");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Referral getReferralByAnalysisId(String analysisId) throws LIMSRuntimeException {

        if (ObjectUtils.isNotEmpty(analysisId)) {
            String sql = "From Referral r where r.analysis.id = :analysisId";

            try {
                Query<Referral> query = entityManager.unwrap(Session.class).createQuery(sql, Referral.class);
                query.setParameter("analysisId", analysisId);
                List<Referral> referralList = query.list();
                return referralList.isEmpty() ? null : referralList.get(referralList.size() - 1);
            } catch (HibernateException e) {
                handleException(e, "getReferralByAnalysisId");
            }
        }

        return null;
    }

    @SuppressWarnings("unused")
    private Referral readResult(String referralId) {
        try {
            Referral referral = entityManager.unwrap(Session.class).get(Referral.class, referralId);
            return referral;
        } catch (HibernateException e) {
            handleException(e, "readResult");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Referral> getAllReferralsBySampleId(String id) throws LIMSRuntimeException {
        if (ObjectUtils.isNotEmpty(id)) {
            String sql = "FROM Referral r WHERE r.analysis.sampleItem.sample.id = :sampleId";

            try {
                Query<Referral> query = entityManager.unwrap(Session.class).createQuery(sql, Referral.class);
                query.setParameter("sampleId", id);
                List<Referral> referralList = query.list();
                return referralList;

            } catch (HibernateException e) {
                handleException(e, "getAllReferralsBySampleId");
            }
        }

        return new ArrayList<>();
    }

    /**
     * @see org.openelisglobal.referral.dao.ReferralDAO#getAllReferralsByOrganization(java.lang.String,
     *      java.sql.Date, java.sql.Date)
     */
    @Override
    @Transactional(readOnly = true)
    public List<Referral> getAllReferralsByOrganization(String organizationId, Date lowDate, Date highDate) {
        String sql = "FROM Referral r WHERE r.organization.id = :organizationId AND r.requestDate >= :lowDate"
                + " AND r.requestDate <= :highDate";

        try {
            Query<Referral> query = entityManager.unwrap(Session.class).createQuery(sql, Referral.class);
            query.setParameter("organizationId", organizationId);
            query.setParameter("lowDate", lowDate);
            query.setParameter("highDate", highDate);
            List<Referral> referralList = query.list();
            // closeSession(); // CSL remove old
            return referralList;
        } catch (HibernateException e) {
            handleException(e, "getAllReferralsByOrganization");
        }
        return new ArrayList<>();
    }

    @Override
    public List<Referral> getReferralsByStatus(List<ReferralStatus> statuses) {
        String sql = "From Referral r where r.status in (:statuses) order by r.id";

        try {
            Query<Referral> query = entityManager.unwrap(Session.class).createQuery(sql, Referral.class);
            query.setParameterList("statuses", statuses);
            List<Referral> referrals = query.list();
            return referrals;
        } catch (HibernateException e) {
            handleException(e, "getAllReferralsByStatus");
        }
        return null;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Referral> getReferralsByAnalysisIds(List<String> analysisIds) {
        if (analysisIds == null || analysisIds.size() == 0) {
            return new ArrayList<>();
        }
        String sql = "From Referral r where r.analysis.id in (:analysisIds)";
        try {
            Query<Referral> query = entityManager.unwrap(Session.class).createQuery(sql, Referral.class);
            query.setParameterList("analysisIds", analysisIds);
            return query.list();
        } catch (HibernateException e) {
            handleException(e, "getReferralsByAnalysisIds");
        }
        return null;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Referral> getReferralsByTestAndDate(ReferDateType dateType, Timestamp startDate, Timestamp endDate,
            List<String> testUnitIds, List<String> testIds) {
        String hql = "From Referral r WHERE 1 = 1 ";
        String subHQL = "SELECT a.id FROM Analysis a WHERE 1 = 1 ";
        if (ReferDateType.RESULT.equals(dateType) && startDate != null) {
            subHQL += "AND a.completedDate BETWEEN :startDate AND :endDate ";
        }
        if (testUnitIds != null && testUnitIds.size() > 0) {
            subHQL += "AND a.testSection.id in (:testUnitIds) ";
        }
        if (testIds != null && testIds.size() > 0) {
            subHQL += "AND a.test.id in (:testIds) ";
        }

        if (!subHQL.endsWith("1 = 1 ")) {
            hql += "AND r.analysis.id in (" + subHQL + ") ";
        }
        if (ReferDateType.SENT.equals(dateType) && startDate != null) {
            hql += "AND r.sentDate BETWEEN :startDate AND :endDate ";
        }

        try {
            Query<Referral> query = entityManager.unwrap(Session.class).createQuery(hql, Referral.class);
            if (startDate != null) {
                query.setParameter("startDate", startDate);
                query.setParameter("endDate", endDate);
            }
            if (testUnitIds != null && testUnitIds.size() > 0) {
                query.setParameter("testUnitIds", testUnitIds);
            }
            if (testIds != null && testIds.size() > 0) {
                query.setParameter("testIds", testIds);
            }
            return query.list();
        } catch (HibernateException e) {
            handleException(e, "getReferralsByAnalysisIds");
        }
        return null;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Referral> getUnassignedReferrals() {
        // Filter in SQL for performance: exclude lost, canceled, and already assigned
        // referrals
        String sql = "FROM Referral r WHERE r.assignedBox IS NULL "
                + "AND (r.lostStatus IS NULL OR r.lostStatus = false) " + "AND r.status != 'CANCELED'";

        try {
            Query<Referral> query = entityManager.unwrap(Session.class).createQuery(sql, Referral.class);

            return query.list();
        } catch (HibernateException e) {
            handleException(e, "getUnassignedReferrals");
        }
        return new ArrayList<>();
    }

    @Transactional(readOnly = true)
    @Override
    public List<Referral> getReferralsByBoxId(Integer boxId) {
        String sql = "FROM Referral r WHERE r.assignedBox.id = :boxId";

        try {
            Query<Referral> query = entityManager.unwrap(Session.class).createQuery(sql, Referral.class);
            query.setParameter("boxId", boxId);
            return query.list();
        } catch (HibernateException e) {
            handleException(e, "getReferralsByBoxId");
        }
        return new ArrayList<>();
    }

    @Transactional(readOnly = true)
    @Override
    public List<Referral> getReferralsBySampleItemId(Integer sampleItemId) {
        String hql = "SELECT DISTINCT r" + " FROM Referral r" + " JOIN FETCH r.analysis a"
                + " JOIN FETCH a.sampleItem si" + " LEFT JOIN FETCH a.test t" + " LEFT JOIN FETCH r.organization org"
                + " WHERE si.id = :sampleItemId" + "   AND r.status != 'CANCELED'"
                + "   AND (r.lostStatus IS NULL OR r.lostStatus = false)" + " ORDER BY r.requestDate";

        try {
            Query<Referral> query = entityManager.unwrap(Session.class).createQuery(hql, Referral.class);
            query.setParameter("sampleItemId", sampleItemId);

            return query.list();
        } catch (HibernateException e) {
            handleException(e, "getReferralsBySampleItemId");
        }
        return new ArrayList<>();
    }

    @Transactional(readOnly = true)
    @Override
    public List<Object[]> getUnassignedReferralsGroupedBySampleItem(List<String> excludedSampleItemIds) {
        String hql = "SELECT DISTINCT si.id, s.accessionNumber, COALESCE(tos.description, ''), tos.id,"
                + " s.collectionDate, si.sortOrder" + " FROM Referral r" + " JOIN r.analysis a"
                + " JOIN a.sampleItem si" + " JOIN si.sample s" + " LEFT JOIN si.typeOfSample tos"
                + " WHERE r.assignedBox IS NULL" + "   AND (r.lostStatus IS NULL OR r.lostStatus = false)"
                + "   AND r.status != 'CANCELED'" + "   AND (si.rejected IS NULL OR si.rejected = false)"
                + "   AND (si.voided IS NULL OR si.voided = false)";

        if (excludedSampleItemIds != null && !excludedSampleItemIds.isEmpty()) {
            hql += " AND si.id NOT IN (:excludedIds)";
        }

        hql += " ORDER BY s.accessionNumber, si.sortOrder";

        try {
            Query<Object[]> query = entityManager.unwrap(Session.class).createQuery(hql, Object[].class);

            if (excludedSampleItemIds != null && !excludedSampleItemIds.isEmpty()) {
                query.setParameterList("excludedIds", excludedSampleItemIds);
            }

            return query.list();
        } catch (HibernateException e) {
            handleException(e, "getUnassignedReferralsGroupedBySampleItem");
        }
        return new ArrayList<>();
    }

    @Transactional(readOnly = true)
    @Override
    public List<Object[]> searchUnassignedByAccessionNumber(String accessionNumber,
            List<String> excludedSampleItemIds) {
        String hql = "SELECT DISTINCT si.id, s.accessionNumber, COALESCE(tos.description, ''), tos.id,"
                + " s.collectionDate, si.sortOrder" + " FROM Referral r" + " JOIN r.analysis a"
                + " JOIN a.sampleItem si" + " JOIN si.sample s" + " LEFT JOIN si.typeOfSample tos"
                + " WHERE r.assignedBox IS NULL" + "   AND (r.lostStatus IS NULL OR r.lostStatus = false)"
                + "   AND r.status != 'CANCELED'" + "   AND (si.rejected IS NULL OR si.rejected = false)"
                + "   AND (si.voided IS NULL OR si.voided = false)" + "   AND s.accessionNumber LIKE :accessionNumber";

        if (excludedSampleItemIds != null && !excludedSampleItemIds.isEmpty()) {
            hql += " AND si.id NOT IN (:excludedIds)";
        }

        hql += " ORDER BY s.accessionNumber, si.sortOrder";

        try {
            Query<Object[]> query = entityManager.unwrap(Session.class).createQuery(hql, Object[].class);

            query.setParameter("accessionNumber", "%" + accessionNumber + "%");

            if (excludedSampleItemIds != null && !excludedSampleItemIds.isEmpty()) {
                query.setParameterList("excludedIds", excludedSampleItemIds);
            }

            return query.list();
        } catch (HibernateException e) {
            handleException(e, "searchUnassignedByAccessionNumber");
        }
        return new ArrayList<>();
    }
}
