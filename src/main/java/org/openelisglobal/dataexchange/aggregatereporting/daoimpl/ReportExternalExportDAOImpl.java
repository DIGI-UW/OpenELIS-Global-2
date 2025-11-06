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
package org.openelisglobal.dataexchange.aggregatereporting.daoimpl;

import java.sql.Timestamp;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.dataexchange.aggregatereporting.dao.ReportExternalExportDAO;
import org.openelisglobal.dataexchange.aggregatereporting.valueholder.ReportExternalExport;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ReportExternalExportDAOImpl extends BaseDAOImpl<ReportExternalExport, String>
        implements ReportExternalExportDAO {
    public ReportExternalExportDAOImpl() {
        super(ReportExternalExport.class);
    }

    private static final long DAY_IN_MILLSEC = 1000L * 60L * 60L * 24L;

    @Override
    @Transactional(readOnly = true)
    public List<ReportExternalExport> getRecalculateReportExports(String reportQueueTypeId)
            throws LIMSRuntimeException {
        String sql = "from ReportExternalExport rq where rq.recalculate = true and rq.typeId = :typeId";
        try {
            Query<ReportExternalExport> query = entityManager.unwrap(Session.class).createQuery(sql,
                    ReportExternalExport.class);
            query.setParameter("typeId", Integer.parseInt(reportQueueTypeId));
            List<ReportExternalExport> reports = query.list();

            return reports;
        } catch (HibernateException e) {
            handleException(e, "getRecalculateReportExports");
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportExternalExport> getUnsentReportExports(String reportQueueTypeId) throws LIMSRuntimeException {
        String sql = "from ReportExternalExport rq where rq.send = true and rq.typeId = :typeId";
        try {
            Query<ReportExternalExport> query = entityManager.unwrap(Session.class).createQuery(sql,
                    ReportExternalExport.class);
            query.setParameter("typeId", Integer.parseInt(reportQueueTypeId));
            List<ReportExternalExport> reports = query.list();

            return reports;
        } catch (HibernateException e) {
            handleException(e, "getRecalculateReportExternalExports");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportExternalExport getLatestSentReportExport(String reportQueueTypeId) throws LIMSRuntimeException {
        String sql = "from ReportExternalExport rq where rq.send = false and rq.typeId = :typeId order by"
                + " rq.sentDate desc";

        try {
            Query<ReportExternalExport> query = entityManager.unwrap(Session.class).createQuery(sql,
                    ReportExternalExport.class);
            query.setParameter("typeId", Integer.parseInt(reportQueueTypeId));
            ReportExternalExport report = query.setMaxResults(1).uniqueResult();

            return report;
        } catch (HibernateException e) {
            handleException(e, "getLatestSentReportExternalExport");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportExternalExport getLatestEventReportExport(String reportQueueTypeId) throws LIMSRuntimeException {
        String sql = "from ReportExternalExport rq where rq.typeId = :typeId order by rq.eventDate desc";

        try {
            Query<ReportExternalExport> query = entityManager.unwrap(Session.class).createQuery(sql,
                    ReportExternalExport.class);
            query.setParameter("typeId", Integer.parseInt(reportQueueTypeId));
            ReportExternalExport report = query.setMaxResults(1).uniqueResult();

            return report;
        } catch (HibernateException e) {
            handleException(e, "getLatestSentReportExternalExport");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportExternalExport> getReportsInDateRange(Timestamp lower, Timestamp upper, String reportQueueTypeId)
            throws LIMSRuntimeException {
        String sql = "from ReportExternalExport rq where rq.sentDate >= :lower and rq.sentDate <= :upper";

        try {
            Query<ReportExternalExport> query = entityManager.unwrap(Session.class).createQuery(sql,
                    ReportExternalExport.class);
            query.setParameter("lower", lower);
            query.setParameter("upper", upper);
            List<ReportExternalExport> reports = query.list();

            return reports;
        } catch (HibernateException e) {
            handleException(e, "getReportsInDateRange");
        }

        return null;
    }

    @Override
    public ReportExternalExport readReportExternalExport(String idString) throws LIMSRuntimeException {

        try {
            ReportExternalExport data = entityManager.unwrap(Session.class).get(ReportExternalExport.class, idString);
            return data;
        } catch (HibernateException e) {
            handleException(e, "readReportExternalExport");
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Timestamp getLastSentTimestamp() throws LIMSRuntimeException {
        String sql = "From ReportExternalExport ree where ree.sentDate IS NOT NULL order by ree.sentDate DESC";

        try {
            Query<ReportExternalExport> query = entityManager.unwrap(Session.class).createQuery(sql,
                    ReportExternalExport.class);
            ReportExternalExport report = query.setMaxResults(1).uniqueResult();
            if (report != null) {
                return report.getSentDate();
            }
        } catch (HibernateException e) {
            handleException(e, "getLastSentTimestamp");
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Timestamp getLastCollectedTimestamp() throws LIMSRuntimeException {
        String sql = "From ReportExternalExport ree order by ree.collectionDate DESC";

        try {
            Query<ReportExternalExport> query = entityManager.unwrap(Session.class).createQuery(sql,
                    ReportExternalExport.class);
            ReportExternalExport report = query.setMaxResults(1).uniqueResult();
            if (report != null) {
                return report.getCollectionDate();
            }
        } catch (HibernateException e) {
            handleException(e, "getLastCollectedTimestamp");
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportExternalExport getReportByEventDateAndType(ReportExternalExport report) throws LIMSRuntimeException {
        String sql = "From ReportExternalExport ree where ree.eventDate >= :eventDate and ree.eventDate <"
                + " :nextDay and ree.typeId = :typeId";

        try {
            Query<ReportExternalExport> query = entityManager.unwrap(Session.class).createQuery(sql,
                    ReportExternalExport.class);
            query.setParameter("eventDate", report.getEventDate());
            query.setParameter("nextDay", new Timestamp(report.getEventDate().getTime() + DAY_IN_MILLSEC));
            query.setParameter("typeId", Integer.parseInt(report.getTypeId()));
            ReportExternalExport foundReport = query.setMaxResults(1).uniqueResult();
            return foundReport == null ? report : foundReport;

        } catch (HibernateException e) {
            handleException(e, "getReportByEventDateAndType");
        }
        return report;
    }

    @Override
    public ReportExternalExport loadReport(ReportExternalExport report) throws LIMSRuntimeException {
        return readReportExternalExport(report.getId());
    }
}
