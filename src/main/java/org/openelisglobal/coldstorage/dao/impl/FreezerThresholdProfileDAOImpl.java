package org.openelisglobal.coldstorage.dao.impl;

import java.time.OffsetDateTime;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.coldstorage.dao.FreezerThresholdProfileDAO;
import org.openelisglobal.coldstorage.valueholder.FreezerThresholdProfile;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class FreezerThresholdProfileDAOImpl extends BaseDAOImpl<FreezerThresholdProfile, Long>
        implements FreezerThresholdProfileDAO {

    public FreezerThresholdProfileDAOImpl() {
        super(FreezerThresholdProfile.class);
    }

    @Override
    public List<FreezerThresholdProfile> findActiveAssignments(Long freezerId, OffsetDateTime at) {
        String hql = "FROM FreezerThresholdProfile ftp " + "WHERE ftp.freezer.id = :freezerId "
                + "AND ftp.effectiveStart <= :timestamp "
                + "AND (ftp.effectiveEnd IS NULL OR ftp.effectiveEnd > :timestamp) "
                + "ORDER BY ftp.effectiveStart DESC";
        Query<FreezerThresholdProfile> query = entityManager.unwrap(Session.class).createQuery(hql,
                FreezerThresholdProfile.class);
        query.setParameter("freezerId", freezerId);
        query.setParameter("timestamp", at);
        return query.list();
    }
}
