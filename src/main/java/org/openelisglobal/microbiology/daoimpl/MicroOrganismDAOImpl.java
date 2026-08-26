package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroOrganismDAOImpl extends BaseDAOImpl<MicroOrganism, String> implements MicroOrganismDAO {

    public MicroOrganismDAOImpl() {
        super(MicroOrganism.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroOrganism> getActiveOrganisms() {
        return entityManager.unwrap(Session.class)
                .createQuery("from MicroOrganism o where o.isActive = 'Y' order by o.displayName", MicroOrganism.class)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MicroOrganism> findByDisplayNameIgnoreCase(String displayName) {
        Query<MicroOrganism> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroOrganism o where lower(o.displayName) = lower(:displayName)", MicroOrganism.class);
        query.setParameter("displayName", displayName);
        return query.uniqueResultOptional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MicroOrganism> findByWhonetCodeIgnoreCase(String whonetCode) {
        Query<MicroOrganism> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroOrganism o where lower(o.whonetCode) = lower(:whonetCode)", MicroOrganism.class);
        query.setParameter("whonetCode", whonetCode);
        return query.uniqueResultOptional();
    }

    @Override
    @Transactional(readOnly = true)
    public long countWorkflowReferences(String organismId) {
        Query<Long> query = entityManager.unwrap(Session.class)
                .createQuery("select count(i.id) from MicroIsolate i where i.organismId = :organismId", Long.class);
        query.setParameter("organismId", organismId);
        return query.uniqueResult();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroOrganism> search(String q, String status, String category, String sort, int offset, int limit) {
        Query<MicroOrganism> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroOrganism o" + searchWhere(q, status, category)
                        + ("name-desc".equals(sort) ? " order by lower(o.displayName) desc"
                                : " order by lower(o.displayName) asc"),
                        MicroOrganism.class);
        setSearchParameters(query, q, status, category);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public long countSearch(String q, String status, String category) {
        Query<Long> query = entityManager.unwrap(Session.class)
                .createQuery("select count(o.id) from MicroOrganism o" + searchWhere(q, status, category), Long.class);
        setSearchParameters(query, q, status, category);
        return query.getSingleResult();
    }

    private String searchWhere(String q, String status, String category) {
        StringBuilder hql = new StringBuilder(" where 1 = 1");
        if (q != null && !q.isBlank()) {
            hql.append(" and (lower(o.displayName) like :q or lower(coalesce(o.shortName, '')) like :q")
                    .append(" or lower(o.whonetCode) like :q)");
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            hql.append(" and o.isActive = :active");
        }
        if (category != null && !category.isBlank()) {
            hql.append(" and lower(o.organismGroup) = :category");
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
