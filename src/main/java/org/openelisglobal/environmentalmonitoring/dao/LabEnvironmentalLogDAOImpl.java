package org.openelisglobal.environmentalmonitoring.dao;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.environmentalmonitoring.valueholder.LabEnvironmentalLog;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of LabEnvironmentalLogDAO using Hibernate.
 *
 * All queries use HQL for database portability and optimal caching. Results are
 * ordered by checked date/time descending for most recent first.
 */
@Component
@Transactional
@SuppressWarnings("unused")
public class LabEnvironmentalLogDAOImpl extends BaseDAOImpl<LabEnvironmentalLog, Long>
        implements LabEnvironmentalLogDAO {

    public LabEnvironmentalLogDAOImpl() {
        super(LabEnvironmentalLog.class);
    }

    @Override
    public List<LabEnvironmentalLog> findByStorageUnitType(LabEnvironmentalLog.StorageUnitType storageUnitType) {
        String sql = "SELECT * FROM clinlims.lab_environmental_log " + "WHERE storage_unit_type = :storageUnitType "
                + "ORDER BY checked_date_time DESC";
        List<LabEnvironmentalLog> results = entityManager.createNativeQuery(sql, LabEnvironmentalLog.class)
                .setParameter("storageUnitType", storageUnitType.name()).getResultList();
        return results;
    }

    @Override
    public List<LabEnvironmentalLog> findByStorageUnitId(String storageUnitId) {
        String hql = "FROM LabEnvironmentalLog l WHERE l.storageUnitId = :storageUnitId "
                + "ORDER BY l.checkedDateTime DESC";
        Session session = entityManager.unwrap(Session.class);
        Query<LabEnvironmentalLog> query = session.createQuery(hql, LabEnvironmentalLog.class);
        query.setParameter("storageUnitId", storageUnitId);
        return query.list();
    }

    @Override
    public List<LabEnvironmentalLog> findByStorageUnitTypeAndDateRange(
            LabEnvironmentalLog.StorageUnitType storageUnitType, Timestamp startDate, Timestamp endDate) {
        String sql = "SELECT * FROM clinlims.lab_environmental_log " + "WHERE storage_unit_type = :storageUnitType "
                + "AND checked_date_time >= :startDate AND checked_date_time <= :endDate "
                + "ORDER BY checked_date_time DESC";
        List<LabEnvironmentalLog> results = entityManager.createNativeQuery(sql, LabEnvironmentalLog.class)
                .setParameter("storageUnitType", storageUnitType.name()).setParameter("startDate", startDate)
                .setParameter("endDate", endDate).getResultList();
        return results;
    }

    @Override
    public List<LabEnvironmentalLog> findByStorageUnitIdAndDateRange(String storageUnitId, Timestamp startDate,
            Timestamp endDate) {
        String hql = "FROM LabEnvironmentalLog l WHERE l.storageUnitId = :storageUnitId "
                + "AND l.checkedDateTime >= :startDate AND l.checkedDateTime <= :endDate "
                + "ORDER BY l.checkedDateTime DESC";
        Session session = entityManager.unwrap(Session.class);
        Query<LabEnvironmentalLog> query = session.createQuery(hql, LabEnvironmentalLog.class);
        query.setParameter("storageUnitId", storageUnitId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        return query.list();
    }

    @Override
    public long countByStorageUnitType(LabEnvironmentalLog.StorageUnitType storageUnitType) {
        String sql = "SELECT COUNT(*) FROM clinlims.lab_environmental_log "
                + "WHERE storage_unit_type = :storageUnitType";
        Number result = (Number) entityManager.createNativeQuery(sql)
                .setParameter("storageUnitType", storageUnitType.name()).getSingleResult();
        return result.longValue();
    }

    @Override
    public long countTodayByStorageUnitType(LabEnvironmentalLog.StorageUnitType storageUnitType) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        Timestamp startTimestamp = Timestamp.valueOf(startOfDay);
        Timestamp endTimestamp = Timestamp.valueOf(endOfDay);

        String sql = "SELECT COUNT(*) FROM clinlims.lab_environmental_log "
                + "WHERE storage_unit_type = :storageUnitType "
                + "AND checked_date_time >= :startDate AND checked_date_time <= :endDate";
        Number result = (Number) entityManager.createNativeQuery(sql)
                .setParameter("storageUnitType", storageUnitType.name()).setParameter("startDate", startTimestamp)
                .setParameter("endDate", endTimestamp).getSingleResult();
        return result.longValue();
    }

    @Override
    public List<LabEnvironmentalLog> findOutOfRangeTemperature(LabEnvironmentalLog.StorageUnitType storageUnitType,
            Double minTemp, Double maxTemp) {
        String sql = "SELECT * FROM clinlims.lab_environmental_log " + "WHERE storage_unit_type = :storageUnitType "
                + "AND (temperature_value < :minTemp OR temperature_value > :maxTemp) "
                + "ORDER BY checked_date_time DESC";
        List<LabEnvironmentalLog> results = entityManager.createNativeQuery(sql, LabEnvironmentalLog.class)
                .setParameter("storageUnitType", storageUnitType.name()).setParameter("minTemp", minTemp)
                .setParameter("maxTemp", maxTemp).getResultList();
        return results;
    }

    @Override
    public long countOutOfRangeTemperature(LabEnvironmentalLog.StorageUnitType storageUnitType, Double minTemp,
            Double maxTemp) {
        String sql = "SELECT COUNT(*) FROM clinlims.lab_environmental_log "
                + "WHERE storage_unit_type = :storageUnitType "
                + "AND (temperature_value < :minTemp OR temperature_value > :maxTemp)";
        Number result = (Number) entityManager.createNativeQuery(sql)
                .setParameter("storageUnitType", storageUnitType.name()).setParameter("minTemp", minTemp)
                .setParameter("maxTemp", maxTemp).getSingleResult();
        return result.longValue();
    }

    @Override
    public List<LabEnvironmentalLog> findAllOrderedByDate() {
        String hql = "FROM LabEnvironmentalLog l ORDER BY l.checkedDateTime DESC";
        Session session = entityManager.unwrap(Session.class);
        Query<LabEnvironmentalLog> query = session.createQuery(hql, LabEnvironmentalLog.class);
        return query.list();
    }

    @Override
    public List<LabEnvironmentalLog> findTodaysLogs() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        Timestamp startTimestamp = Timestamp.valueOf(startOfDay);
        Timestamp endTimestamp = Timestamp.valueOf(endOfDay);

        String hql = "FROM LabEnvironmentalLog l WHERE l.checkedDateTime >= :startDate "
                + "AND l.checkedDateTime <= :endDate ORDER BY l.checkedDateTime DESC";
        Session session = entityManager.unwrap(Session.class);
        Query<LabEnvironmentalLog> query = session.createQuery(hql, LabEnvironmentalLog.class);
        query.setParameter("startDate", startTimestamp);
        query.setParameter("endDate", endTimestamp);
        return query.list();
    }
}