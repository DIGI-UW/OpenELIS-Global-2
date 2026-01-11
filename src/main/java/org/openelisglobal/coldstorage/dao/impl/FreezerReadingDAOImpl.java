package org.openelisglobal.coldstorage.dao.impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.coldstorage.dao.FreezerReadingDAO;
import org.openelisglobal.coldstorage.valueholder.FreezerReading;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class FreezerReadingDAOImpl extends BaseDAOImpl<FreezerReading, Long> implements FreezerReadingDAO {

    public FreezerReadingDAOImpl() {
        super(FreezerReading.class);
    }

    @Override
    public Optional<FreezerReading> findLatestByFreezer(Long freezerId) {
        String hql = "FROM FreezerReading fr WHERE fr.freezer.id = :freezerId ORDER BY fr.recordedAt DESC";
        Query<FreezerReading> query = entityManager.unwrap(Session.class).createQuery(hql, FreezerReading.class);
        query.setParameter("freezerId", freezerId);
        query.setMaxResults(1);
        return query.uniqueResultOptional();
    }

    @Override
    public List<FreezerReading> findRecentByFreezer(Long freezerId, int limit) {
        String hql = "FROM FreezerReading fr WHERE fr.freezer.id = :freezerId ORDER BY fr.recordedAt DESC";
        Query<FreezerReading> query = entityManager.unwrap(Session.class).createQuery(hql, FreezerReading.class);
        query.setParameter("freezerId", freezerId);
        query.setMaxResults(limit);
        return query.list();
    }

    @Override
    public List<FreezerReading> findByFreezerWithin(Long freezerId, OffsetDateTime start, OffsetDateTime end) {
        String hql = "FROM FreezerReading fr " + "WHERE fr.freezer.id = :freezerId "
                + "AND fr.recordedAt BETWEEN :start AND :end " + "ORDER BY fr.recordedAt";
        Query<FreezerReading> query = entityManager.unwrap(Session.class).createQuery(hql, FreezerReading.class);
        query.setParameter("freezerId", freezerId);
        query.setParameter("start", start);
        query.setParameter("end", end);
        return query.list();
    }
}
