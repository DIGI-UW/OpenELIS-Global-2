package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroPatientOriginDAO;
import org.openelisglobal.microbiology.valueholder.MicroPatientOrigin;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroPatientOriginDAOImpl extends BaseDAOImpl<MicroPatientOrigin, String>
        implements MicroPatientOriginDAO {

    public MicroPatientOriginDAOImpl() {
        super(MicroPatientOrigin.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroPatientOrigin> getActivePatientOrigins() {
        return entityManager.unwrap(Session.class)
                .createQuery("from MicroPatientOrigin o where o.isActive = 'Y' order by o.sortOrder, o.displayName",
                        MicroPatientOrigin.class)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroPatientOrigin> getByCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        Query<MicroPatientOrigin> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroPatientOrigin o where o.code in (:codes)", MicroPatientOrigin.class);
        query.setParameterList("codes", codes);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveCode(String code) {
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(
                "select count(o.id) from MicroPatientOrigin o where o.isActive = 'Y' and o.code = :code", Long.class);
        query.setParameter("code", code);
        return query.getSingleResult() > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroPatientOrigin> search(String q, String status, String sort, int offset, int limit) {
        Query<MicroPatientOrigin> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroPatientOrigin o" + searchWhere(q, status)
                        + ("name-desc".equals(sort) ? " order by lower(o.displayName) desc"
                                : " order by lower(o.displayName) asc"),
                        MicroPatientOrigin.class);
        setSearchParameters(query, q, status);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public long countSearch(String q, String status) {
        Query<Long> query = entityManager.unwrap(Session.class)
                .createQuery("select count(o.id) from MicroPatientOrigin o" + searchWhere(q, status), Long.class);
        setSearchParameters(query, q, status);
        return query.getSingleResult();
    }

    private String searchWhere(String q, String status) {
        StringBuilder hql = new StringBuilder(" where 1 = 1");
        if (q != null && !q.isBlank()) {
            hql.append(" and (lower(o.displayName) like :q or lower(o.code) like :q")
                    .append(" or lower(o.whonetCode) like :q)");
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            hql.append(" and o.isActive = :active");
        }
        return hql.toString();
    }

    private void setSearchParameters(Query<?> query, String q, String status) {
        if (q != null && !q.isBlank()) {
            query.setParameter("q", "%" + q.trim().toLowerCase(java.util.Locale.ROOT) + "%");
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            query.setParameter("active", "ACTIVE".equalsIgnoreCase(status) ? "Y" : "N");
        }
    }
}
