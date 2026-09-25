package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroAntibioticDAOImpl extends BaseDAOImpl<MicroAntibiotic, String> implements MicroAntibioticDAO {

    public MicroAntibioticDAOImpl() {
        super(MicroAntibiotic.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAntibiotic> getActiveAntibiotics() {
        return entityManager.unwrap(Session.class).createQuery(
                "from MicroAntibiotic a where a.isActive = 'Y' order by a.displayName", MicroAntibiotic.class).list();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MicroAntibiotic> findByDisplayNameIgnoreCase(String displayName) {
        Query<MicroAntibiotic> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroAntibiotic a where lower(a.displayName) = lower(:displayName)", MicroAntibiotic.class);
        query.setParameter("displayName", displayName);
        return query.uniqueResultOptional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MicroAntibiotic> findByWhonetCodeIgnoreCase(String whonetCode) {
        Query<MicroAntibiotic> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroAntibiotic a where lower(a.whonetCode) = lower(:whonetCode)", MicroAntibiotic.class);
        query.setParameter("whonetCode", whonetCode);
        return query.uniqueResultOptional();
    }

    @Override
    @Transactional(readOnly = true)
    public long countWorkflowReferences(String antibioticId) {
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(
                "select count(r.id) from MicroAstReading r where r.antibioticId = :antibioticId", Long.class);
        query.setParameter("antibioticId", antibioticId);
        return query.uniqueResult();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAntibiotic> search(String q, String status, String category, String sort, int offset, int limit) {
        Query<MicroAntibiotic> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroAntibiotic a" + searchWhere(q, status, category)
                        + ("name-desc".equals(sort) ? " order by lower(a.displayName) desc"
                                : " order by lower(a.displayName) asc"),
                        MicroAntibiotic.class);
        setSearchParameters(query, q, status, category);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public long countSearch(String q, String status, String category) {
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(
                "select count(a.id) from MicroAntibiotic a" + searchWhere(q, status, category), Long.class);
        setSearchParameters(query, q, status, category);
        return query.getSingleResult();
    }

    private String searchWhere(String q, String status, String category) {
        StringBuilder hql = new StringBuilder(" where 1 = 1");
        if (q != null && !q.isBlank()) {
            hql.append(" and (lower(a.displayName) like :q or lower(a.whonetCode) like :q)");
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            hql.append(" and a.isActive = :active");
        }
        if (category != null && !category.isBlank()) {
            hql.append(" and lower(a.antibioticClass) = :category");
        }
        return hql.toString();
    }

    private void setSearchParameters(Query<?> query, String q, String status, String category) {
        if (q != null && !q.isBlank()) {
            query.setParameter("q", "%" + q.trim().toLowerCase(java.util.Locale.ROOT) + "%");
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            query.setParameter("active", "ACTIVE".equalsIgnoreCase(status) ? "Y" : "N");
        }
        if (category != null && !category.isBlank()) {
            query.setParameter("category", category.trim().toLowerCase(java.util.Locale.ROOT));
        }
    }
}
