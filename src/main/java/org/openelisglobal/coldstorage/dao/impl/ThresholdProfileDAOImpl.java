package org.openelisglobal.coldstorage.dao.impl;

import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.coldstorage.dao.ThresholdProfileDAO;
import org.openelisglobal.coldstorage.valueholder.ThresholdProfile;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ThresholdProfileDAOImpl extends BaseDAOImpl<ThresholdProfile, Long> implements ThresholdProfileDAO {

    public ThresholdProfileDAOImpl() {
        super(ThresholdProfile.class);
    }

    @Override
    public Optional<ThresholdProfile> findByName(String name) {
        String hql = "FROM ThresholdProfile tp WHERE lower(tp.name) = lower(:name)";
        Query<ThresholdProfile> query = entityManager.unwrap(Session.class).createQuery(hql, ThresholdProfile.class);
        query.setParameter("name", name);
        query.setMaxResults(1);
        return query.uniqueResultOptional();
    }
}
