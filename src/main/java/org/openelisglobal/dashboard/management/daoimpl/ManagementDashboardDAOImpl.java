package org.openelisglobal.dashboard.management.daoimpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.openelisglobal.dashboard.management.dao.ManagementDashboardDAO;
import org.openelisglobal.reports.qi.QiReportSupport;
import org.springframework.stereotype.Component;

@Component
public class ManagementDashboardDAOImpl implements ManagementDashboardDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public long countSamplesReceived(LocalDate from, LocalDate to) {
        return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM clinlims.sample WHERE received_date >= :from"
                        + " AND received_date < :to")
                .setParameter("from", QiReportSupport.startOf(from)).setParameter("to", QiReportSupport.endOf(to))
                .getSingleResult()).longValue();
    }

    @Override
    public long countNcesReported(LocalDate from, LocalDate to) {
        return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM clinlims.nc_event WHERE report_date BETWEEN :from AND :to")
                .setParameter("from", Date.valueOf(from)).setParameter("to", Date.valueOf(to)).getSingleResult())
                .longValue();
    }

    @Override
    public Map<String, Boolean> getFeatures() {
        Object[] row = (Object[]) entityManager
                .createNativeQuery("SELECT" + " EXISTS (SELECT 1 FROM clinlims.inventory_item) AS inventory,"
                        + " EXISTS (SELECT 1 FROM clinlims.eqa_lab_program_enrollment) AS eqa,"
                        + " EXISTS (SELECT 1 FROM clinlims.analyzer WHERE is_active) AS analyzers,"
                        + " EXISTS (SELECT 1 FROM clinlims.micro_isolate) AS microbiology,"
                        + " EXISTS (SELECT 1 FROM clinlims.qc_result) AS qc")
                .getSingleResult();
        Map<String, Boolean> features = new LinkedHashMap<>();
        features.put("inventory", (Boolean) row[0]);
        features.put("eqa", (Boolean) row[1]);
        features.put("analyzers", (Boolean) row[2]);
        features.put("microbiology", (Boolean) row[3]);
        features.put("qc", (Boolean) row[4]);
        return features;
    }
}
