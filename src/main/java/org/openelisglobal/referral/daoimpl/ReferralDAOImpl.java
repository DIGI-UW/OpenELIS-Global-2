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
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
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

        if (!GenericValidator.isBlankOrNull(analysisId)) {
            String sql = "From Referral r where r.analysis.id = :analysisId";

            try {
                Query<Referral> query = entityManager.unwrap(Session.class).createQuery(sql, Referral.class);
                query.setParameter("analysisId", Integer.parseInt(analysisId));
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
        if (!GenericValidator.isBlankOrNull(id)) {
            String sql = "FROM Referral r WHERE r.analysis.sampleItem.sample.id = :sampleId";

            try {
                Query<Referral> query = entityManager.unwrap(Session.class).createQuery(sql, Referral.class);
                query.setParameter("sampleId", Integer.parseInt(id));
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
            query.setParameter("organizationId", Integer.parseInt(organizationId));
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
            query.setParameter("statuses", statuses.stream().map(e -> e.name()).collect(Collectors.toList()));
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
            query.setParameterList("analysisIds",
                    analysisIds.stream().map(e -> Integer.parseInt(e)).collect(Collectors.toList()));
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
                query.setParameter("testUnitIds",
                        testUnitIds.stream().map(e -> Integer.parseInt(e)).collect(Collectors.toList()));
            }
            if (testIds != null && testIds.size() > 0) {
                query.setParameter("testIds",
                        testIds.stream().map(e -> Integer.parseInt(e)).collect(Collectors.toList()));
            }
            return query.list();
        } catch (HibernateException e) {
            handleException(e, "getReferralsByAnalysisIds");
        }
        return null;
    }
}
