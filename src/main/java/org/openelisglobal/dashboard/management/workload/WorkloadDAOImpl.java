package org.openelisglobal.dashboard.management.workload;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.reports.qi.QiReportSupport;
import org.springframework.stereotype.Component;

@Component
public class WorkloadDAOImpl implements WorkloadDAO {

    @PersistenceContext
    private EntityManager entityManager;

    // Section fallback and the canceled/rejected filters match the TAT report.
    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> countSamplesByDayAndSection(LocalDate from, LocalDate to) {
        return entityManager.createNativeQuery("SELECT to_char(s.received_date, 'YYYY-MM-DD') AS day,"
                + " CAST(COALESCE(a.test_sect_id, t.test_section_id) AS varchar) AS section_id,"
                + " count(DISTINCT s.id) FROM clinlims.analysis a"
                + " JOIN clinlims.sample_item si ON si.id = a.sampitem_id JOIN clinlims.sample s ON s.id = si.samp_id"
                + " LEFT JOIN clinlims.test t ON t.id = a.test_id"
                + " WHERE s.received_date >= :from AND s.received_date < :to"
                + " AND COALESCE(si.rejected, false) = false"
                + " AND a.status_id NOT IN (SELECT id FROM clinlims.status_of_sample"
                + " WHERE status_type = 'ANALYSIS' AND name = 'Test Canceled')"
                + " AND COALESCE(a.test_sect_id, t.test_section_id) IS NOT NULL GROUP BY 1, 2")
                .setParameter("from", QiReportSupport.startOf(from)).setParameter("to", QiReportSupport.endOf(to))
                .getResultList();
    }
}
