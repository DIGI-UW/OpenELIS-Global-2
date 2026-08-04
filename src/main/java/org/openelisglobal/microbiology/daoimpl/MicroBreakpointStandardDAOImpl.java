package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroBreakpointStandardDAO;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroBreakpointStandardDAOImpl extends BaseDAOImpl<MicroBreakpointStandard, String>
        implements MicroBreakpointStandardDAO {

    public MicroBreakpointStandardDAOImpl() {
        super(MicroBreakpointStandard.class);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroBreakpointStandard getActiveStandard(String authority, String version) {
        Query<MicroBreakpointStandard> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroBreakpointStandard s where s.isActive = 'Y' and s.authority = :authority"
                        + " and s.lifecycleStatus <> 'ARCHIVED' and s.version = :version",
                MicroBreakpointStandard.class);
        query.setParameter("authority", authority);
        query.setParameter("version", version);
        return query.uniqueResultOptional().orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroBreakpointStandard> getActiveStandards() {
        Query<MicroBreakpointStandard> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroBreakpointStandard s where s.isActive = 'Y' and s.lifecycleStatus <> 'ARCHIVED'"
                        + " order by s.authority, s.version", MicroBreakpointStandard.class);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MicroBreakpointStandard> findByAuthorityAndVersion(String authority, String version) {
        Query<MicroBreakpointStandard> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroBreakpointStandard s where s.authority = :authority and s.version = :version",
                MicroBreakpointStandard.class);
        query.setParameter("authority", authority);
        query.setParameter("version", version);
        return query.uniqueResultOptional();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroBreakpointStandard> getActiveForAuthority(String authority) {
        Query<MicroBreakpointStandard> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroBreakpointStandard s where s.authority = :authority and s.lifecycleStatus = 'ACTIVE'",
                MicroBreakpointStandard.class);
        query.setParameter("authority", authority);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroBreakpointStandard> search(String q, String status, String authority, String sort, int offset,
            int limit) {
        Query<MicroBreakpointStandard> query = entityManager.unwrap(Session.class)
                .createQuery(
                        "from MicroBreakpointStandard s" + searchWhere(q, status, authority)
                                + ("name-desc".equals(sort) ? " order by lower(s.authority) desc, s.version desc"
                                        : " order by lower(s.authority) asc, s.version desc"),
                        MicroBreakpointStandard.class);
        setSearchParameters(query, q, status, authority);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public long countSearch(String q, String status, String authority) {
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(
                "select count(s.id) from MicroBreakpointStandard s" + searchWhere(q, status, authority), Long.class);
        setSearchParameters(query, q, status, authority);
        return query.getSingleResult();
    }

    private String searchWhere(String q, String status, String authority) {
        StringBuilder hql = new StringBuilder(" where 1 = 1");
        if (q != null && !q.isBlank()) {
            hql.append(" and (lower(s.authority) like :q or lower(s.version) like :q)");
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            hql.append(" and s.lifecycleStatus = :status");
        }
        if (authority != null && !authority.isBlank()) {
            hql.append(" and lower(s.authority) = :authority");
        }
        return hql.toString();
    }

    private void setSearchParameters(Query<?> query, String q, String status, String authority) {
        if (q != null && !q.isBlank()) {
            query.setParameter("q", "%" + q.trim().toLowerCase(java.util.Locale.ROOT) + "%");
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            query.setParameter("status", status.trim().toUpperCase(java.util.Locale.ROOT));
        }
        if (authority != null && !authority.isBlank()) {
            query.setParameter("authority", authority.trim().toLowerCase(java.util.Locale.ROOT));
        }
    }
}
