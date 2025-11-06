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
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.dataexchange.aggregatereporting.dao.ReportExternalImportDAO;
import org.openelisglobal.dataexchange.aggregatereporting.valueholder.ReportExternalImport;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ReportExternalImportDAOImpl extends BaseDAOImpl<ReportExternalImport, String>
        implements ReportExternalImportDAO {

    public ReportExternalImportDAOImpl() {
        super(ReportExternalImport.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportExternalImport> getReportsInDateRangeSorted(Timestamp lower, Timestamp upper)
            throws LIMSRuntimeException {
        String sql = "from ReportExternalImport rq where rq.eventDate >= :lower and rq.eventDate <= :upper order"
                + " by rq.sendingSite";

        try {
            Query<ReportExternalImport> query = entityManager.unwrap(Session.class).createQuery(sql,
                    ReportExternalImport.class);
            query.setParameter("lower", lower);
            query.setParameter("upper", upper);

            List<ReportExternalImport> reports = query.list();

            return reports;
        } catch (HibernateException e) {
            handleException(e, "getReportsInDateRangeSorted");
        }

        return null;
    }

    public ReportExternalImport readReportExternalImport(String idString) throws LIMSRuntimeException {

        try {
            ReportExternalImport data = entityManager.unwrap(Session.class).get(ReportExternalImport.class, idString);
            return data;
        } catch (HibernateException e) {
            handleException(e, "readReportExternalImport");
        }
        return null;
    }

    @Override
    @Transactional
    public List<String> getUniqueSites() throws LIMSRuntimeException {
        String sql = "select distinct sending_site from clinlims.report_external_import ";
        try {
            @SuppressWarnings("unchecked")
            NativeQuery query = entityManager.unwrap(Session.class).createNativeQuery(sql);
            List<String> sites = query.list();
            return sites;
        } catch (HibernateException e) {
            handleException(e, "getUniqueSites");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportExternalImport> getReportsInDateRangeSortedForSite(Timestamp lower, Timestamp upper, String site)
            throws LIMSRuntimeException {
        String sql = "from ReportExternalImport rq where rq.eventDate >= :lower and rq.eventDate <= :upper and"
                + " rq.sendingSite = :site order by rq.sendingSite";

        try {
            Query<ReportExternalImport> query = entityManager.unwrap(Session.class).createQuery(sql,
                    ReportExternalImport.class);
            query.setParameter("lower", lower);
            query.setParameter("upper", upper);
            query.setParameter("site", site);

            List<ReportExternalImport> reports = query.list();

            return reports;
        } catch (HibernateException e) {
            handleException(e, "getReportsInDateRangeSortedForSite");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportExternalImport getReportByEventDateSiteType(ReportExternalImport importReport)
            throws LIMSRuntimeException {
        String sql = "from ReportExternalImport rei where rei.eventDate = :eventDate and rei.sendingSite = :site"
                + " and rei.reportType = :type";

        try {
            Query<ReportExternalImport> query = entityManager.unwrap(Session.class).createQuery(sql,
                    ReportExternalImport.class);
            query.setParameter("eventDate", importReport.getEventDate());
            query.setParameter("site", importReport.getSendingSite());
            query.setParameter("type", importReport.getReportType());

            List<ReportExternalImport> reports = query.list();

            return reports.isEmpty() ? new ReportExternalImport() : reports.get(0);

        } catch (HibernateException e) {
            handleException(e, "getReportByEventDateSiteType");
        }

        return null;
    }
}
