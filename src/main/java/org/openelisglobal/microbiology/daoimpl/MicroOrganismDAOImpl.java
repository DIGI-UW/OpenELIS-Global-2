package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismMatch;
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
    public List<String> getLabGroupNames() {
        List<String> groups = entityManager.unwrap(Session.class)
                .createQuery("select distinct g from MicroOrganism o join o.labGroups g", String.class).list();
        groups.sort(String.CASE_INSENSITIVE_ORDER);
        return groups;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroOrganism> getByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Query<MicroOrganism> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroOrganism o where o.id in (:ids)", MicroOrganism.class);
        query.setParameterList("ids", ids);
        return query.list();
    }

    private static final String OLDER_NAME = " from MicroWhonetOrganism w where w.taxonomicStatus = 'O'"
            + " and (lower(w.whonetOrgCode) = lower(o.whonetCode) or lower(w.replacedBy) = lower(o.whonetCode))"
            + " and (lower(w.organism) like :fragment escape '!' or lower(w.whonetOrgCode) like :prefix escape '!')";
    private static final String OWN_NAME_OR_CODE = "lower(o.displayName) like :fragment escape '!'"
            + " or lower(o.whonetCode) like :prefix escape '!'";

    // An exact WHONET code first, then the organisms WHONET marks common, then
    // names that start with the text.
    private static final String SEARCH = "select o, case when " + OWN_NAME_OR_CODE + " then 1 else 0 end,"
            + " (select min(w.organism)" + OLDER_NAME + ")" + " from MicroOrganism o where o.isActive = 'Y'" + " and ("
            + OWN_NAME_OR_CODE + " or exists (select w.id" + OLDER_NAME + "))"
            + " order by case when lower(o.whonetCode) = :q then 0 else 1 end,"
            + " case when exists (select c.id from MicroWhonetOrganism c"
            + " where lower(c.whonetOrgCode) = lower(o.whonetCode) and c.taxonomicStatus = 'C' and c.common = 'X')"
            + " then 0 else 1 end,"
            + " case when lower(o.displayName) like :prefix escape '!' then 0 else 1 end, lower(o.displayName)";

    @Override
    @Transactional(readOnly = true)
    public List<MicroOrganismMatch> searchActive(String text, int limit) {
        String q = text == null ? "" : text.trim().toLowerCase(java.util.Locale.ROOT);
        String pattern = q.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        List<Object[]> found = entityManager.unwrap(Session.class).createQuery(SEARCH, Object[].class)
                .setParameter("q", q).setParameter("fragment", "%" + pattern + "%")
                .setParameter("prefix", pattern + "%").setMaxResults(limit).list();
        // An organism found by its own name or code shows no older name.
        return found.stream().map(row -> new MicroOrganismMatch((MicroOrganism) row[0],
                ((Number) row[1]).intValue() == 1 ? null : (String) row[2])).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getActiveOrganismGroups() {
        return entityManager.unwrap(Session.class).createQuery("select distinct o.organismGroup from MicroOrganism o"
                + " where o.isActive = 'Y' and o.organismGroup is not null", String.class).list();
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
    public List<MicroOrganism> getByDefaultAstPanelId(String panelId) {
        return entityManager.unwrap(Session.class)
                .createQuery("from MicroOrganism o where o.defaultAstPanelId = :panelId", MicroOrganism.class)
                .setParameter("panelId", panelId).list();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MicroOrganism> findByReference(String source, String code) {
        return entityManager.unwrap(Session.class)
                .createQuery("from MicroOrganism e where e.referenceSource = :source and e.referenceCode = :code",
                        MicroOrganism.class)
                .setParameter("source", source).setParameter("code", code).uniqueResultOptional();
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
