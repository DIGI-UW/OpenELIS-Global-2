package org.openelisglobal.dashboard.management.equipment;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.reports.qi.QiReportSupport;
import org.springframework.stereotype.Component;

@Component
public class EquipmentDAOImpl implements EquipmentDAO {

    @PersistenceContext
    private EntityManager entityManager;

    // Accepted analyzer results carry only a date in completed_date, so last
    // activity also reads the delivery, event and staging timestamps.
    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> getActivity(LocalDate from, LocalDate to) {
        return entityManager.createNativeQuery("WITH scoped AS (SELECT a.analyzer_id,"
                + " count(DISTINCT s.id) AS samples,"
                + " mode() WITHIN GROUP (ORDER BY COALESCE(a.test_sect_id, t.test_section_id)) AS section_id"
                + " FROM clinlims.analysis a JOIN clinlims.sample_item si ON si.id = a.sampitem_id"
                + " JOIN clinlims.sample s ON s.id = si.samp_id LEFT JOIN clinlims.test t ON t.id = a.test_id"
                + " WHERE a.analyzer_id IS NOT NULL AND s.received_date >= :from AND s.received_date < :to"
                + " GROUP BY a.analyzer_id)"
                + " SELECT CAST(an.id AS varchar), an.name, COALESCE(sc.samples, 0), GREATEST("
                + " (SELECT max(completed_date) FROM clinlims.analysis WHERE analyzer_id = an.id),"
                + " (SELECT max(accepted_at) FROM clinlims.analyzer_delivery_receipt WHERE analyzer_id = an.id),"
                + " (SELECT max(received_at) FROM clinlims.analyzer_event WHERE analyzer_id = CAST(an.id AS varchar)),"
                + " (SELECT max(last_updated) FROM clinlims.analyzer_results WHERE analyzer_id = an.id)),"
                + " CAST(sc.section_id AS varchar) FROM clinlims.analyzer an"
                + " LEFT JOIN scoped sc ON sc.analyzer_id = an.id WHERE an.is_active ORDER BY an.name")
                .setParameter("from", QiReportSupport.startOf(from)).setParameter("to", QiReportSupport.endOf(to))
                .getResultList();
    }
}
