package org.openelisglobal.textmacro.daoimpl;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.textmacro.dao.TextMacroDAO;
import org.openelisglobal.textmacro.valueholder.TextMacro;
import org.openelisglobal.textmacro.valueholder.TextMacroContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TextMacroDAOImpl extends BaseDAOImpl<TextMacro, String> implements TextMacroDAO {

    public TextMacroDAOImpl() {
        super(TextMacro.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TextMacro> findByCode(String code) {
        Query<TextMacro> query = entityManager.unwrap(Session.class).createQuery(
                "select distinct m from TextMacro m left join fetch m.contexts" + " where m.code = :code",
                TextMacro.class);
        query.setParameter("code", code);
        return query.uniqueResultOptional();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TextMacro> findActiveByContext(TextMacroContext context, String search, int limit) {
        String hql = "select m from TextMacro m" + " where m.active = true and :context member of m.contexts";
        if (search != null && !search.isBlank()) {
            hql += " and (lower(m.code) like :search or lower(m.expansionText) like :search)";
        }
        hql += " order by lower(m.code) asc";
        Query<TextMacro> query = entityManager.unwrap(Session.class).createQuery(hql, TextMacro.class);
        query.setParameter("context", context);
        if (search != null && !search.isBlank()) {
            query.setParameter("search", "%" + search.toLowerCase(Locale.ROOT) + "%");
        }
        query.setMaxResults(limit);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TextMacro> search(String search, TextMacroContext context, String status, String sort, int offset,
            int limit) {
        String hql = "select m from TextMacro m" + where(search, context, status) + orderBy(sort);
        Query<TextMacro> query = entityManager.unwrap(Session.class).createQuery(hql, TextMacro.class);
        setParameters(query, search, context, status);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public long countSearch(String search, TextMacroContext context, String status) {
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(
                "select count(distinct m.id) from TextMacro m" + where(search, context, status), Long.class);
        setParameters(query, search, context, status);
        return query.getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TextMacro> findAllWithContexts() {
        return entityManager.unwrap(Session.class)
                .createQuery("select distinct m from TextMacro m left join fetch m.contexts order by m.code asc",
                        TextMacro.class)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TextMacro> findByIdsWithContexts(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Query<TextMacro> query = entityManager.unwrap(Session.class).createQuery(
                "select distinct m from TextMacro m left join fetch m.contexts where m.id in :ids order by m.code asc",
                TextMacro.class);
        query.setParameter("ids", ids);
        return query.list();
    }

    private String where(String search, TextMacroContext context, String status) {
        StringBuilder hql = new StringBuilder(" where 1 = 1");
        if (search != null && !search.isBlank()) {
            hql.append(" and (lower(m.code) like :search or lower(m.expansionText) like :search)");
        }
        if (context != null) {
            hql.append(" and :context member of m.contexts");
        }
        if (!"ALL".equals(status)) {
            hql.append(" and m.active = :active");
        }
        return hql.toString();
    }

    private void setParameters(Query<?> query, String search, TextMacroContext context, String status) {
        if (search != null && !search.isBlank()) {
            query.setParameter("search", "%" + search.toLowerCase(Locale.ROOT) + "%");
        }
        if (context != null) {
            query.setParameter("context", context);
        }
        if (!"ALL".equals(status)) {
            query.setParameter("active", "ACTIVE".equals(status));
        }
    }

    private String orderBy(String sort) {
        if ("code:desc".equals(sort)) {
            return " order by lower(m.code) desc";
        }
        if ("updated:asc".equals(sort)) {
            return " order by m.lastupdated asc, lower(m.code) asc";
        }
        if ("updated:desc".equals(sort)) {
            return " order by m.lastupdated desc, lower(m.code) asc";
        }
        return " order by lower(m.code) asc";
    }
}
