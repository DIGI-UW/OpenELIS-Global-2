package org.openelisglobal.dashboard.management.quality;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Date;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class QualityDAOImpl implements QualityDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public long countNcesReported(LocalDate from, LocalDate to) {
        return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM clinlims.nc_event WHERE report_date BETWEEN :from AND :to")
                .setParameter("from", Date.valueOf(from)).setParameter("to", Date.valueOf(to)).getSingleResult())
                .longValue();
    }
}
