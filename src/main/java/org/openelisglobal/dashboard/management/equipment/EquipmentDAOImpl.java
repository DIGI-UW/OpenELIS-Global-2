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
                + " GROUP BY a.analyzer_id)," + " seen AS (SELECT analyzer_id, max(at) AS at FROM ("
                + " SELECT CAST(analyzer_id AS varchar) AS analyzer_id, completed_date AS at FROM clinlims.analysis"
                + " WHERE analyzer_id IS NOT NULL"
                + " UNION ALL SELECT CAST(analyzer_id AS varchar), accepted_at FROM clinlims.analyzer_delivery_receipt"
                + " UNION ALL SELECT analyzer_id, received_at FROM clinlims.analyzer_event"
                + " UNION ALL SELECT CAST(analyzer_id AS varchar), last_updated FROM clinlims.analyzer_results"
                + ") activity GROUP BY analyzer_id)"
                + " SELECT CAST(an.id AS varchar), an.name, COALESCE(sc.samples, 0), seen.at,"
                + " CAST(sc.section_id AS varchar) FROM clinlims.analyzer an"
                + " LEFT JOIN scoped sc ON sc.analyzer_id = an.id"
                + " LEFT JOIN seen ON seen.analyzer_id = CAST(an.id AS varchar) WHERE an.is_active ORDER BY an.name")
                .setParameter("from", QiReportSupport.startOf(from)).setParameter("to", QiReportSupport.endOf(to))
                .getResultList();
    }
}
