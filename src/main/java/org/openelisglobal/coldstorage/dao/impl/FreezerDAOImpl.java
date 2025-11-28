package org.openelisglobal.coldstorage.dao.impl;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.coldstorage.dao.FreezerDAO;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@SuppressWarnings("unused")
public class FreezerDAOImpl extends BaseDAOImpl<Freezer, Long> implements FreezerDAO {

    public FreezerDAOImpl() {
        super(Freezer.class);
    }

    @Override
    public Optional<Freezer> findByName(String name) {
        String hql = "FROM Freezer f WHERE lower(f.name) = lower(:name)";
        Query<Freezer> query = entityManager.unwrap(Session.class).createQuery(hql, Freezer.class);
        query.setParameter("name", name);
        query.setMaxResults(1);
        return query.uniqueResultOptional();
    }

    @Override
    public List<Freezer> findActiveFreezers() {
        String hql = "SELECT DISTINCT f FROM Freezer f LEFT JOIN FETCH f.storageDevice WHERE f.active = true ORDER BY f.name";
        return entityManager.unwrap(Session.class).createQuery(hql, Freezer.class).list();
    }

    @Override
    public List<Freezer> getAllFreezers() {
        String hql = "SELECT DISTINCT f FROM Freezer f LEFT JOIN FETCH f.storageDevice ORDER BY f.name";
        return entityManager.unwrap(Session.class).createQuery(hql, Freezer.class).list();
    }

    @Override
    public List<Freezer> searchFreezers(String search) {
        String hql = "SELECT DISTINCT f FROM Freezer f LEFT JOIN FETCH f.storageDevice "
                + "WHERE lower(f.name) LIKE lower(:search) " + "ORDER BY f.name";
        Query<Freezer> query = entityManager.unwrap(Session.class).createQuery(hql, Freezer.class);
        query.setParameter("search", "%" + search + "%");
        return query.list();
    }
}
