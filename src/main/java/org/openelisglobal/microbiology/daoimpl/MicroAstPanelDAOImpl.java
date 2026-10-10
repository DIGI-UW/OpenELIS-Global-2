package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroAstPanelDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroAstPanelDAOImpl extends BaseDAOImpl<MicroAstPanel, String> implements MicroAstPanelDAO {

    public MicroAstPanelDAOImpl() {
        super(MicroAstPanel.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstPanel> getByIds(List<String> panelIds) {
        if (panelIds.isEmpty()) {
            return List.of();
        }
        Query<MicroAstPanel> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroAstPanel p where p.id in (:panelIds)", MicroAstPanel.class);
        query.setParameterList("panelIds", panelIds);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstPanel> getActivePanelsByOrganismGroup(String organismGroup) {
        StringBuilder hql = new StringBuilder("from MicroAstPanel p where p.isActive = 'Y' and p.isCurrent = 'Y'");
        if (organismGroup != null && !organismGroup.isBlank()) {
            hql.append(" and p.organismGroup = :organismGroup");
        }
        hql.append(" order by p.name");
        Query<MicroAstPanel> query = entityManager.unwrap(Session.class).createQuery(hql.toString(),
                MicroAstPanel.class);
        if (organismGroup != null && !organismGroup.isBlank()) {
            query.setParameter("organismGroup", organismGroup);
        }
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public MicroAstPanel findCurrentByLogicalKey(String logicalKey) {
        Query<MicroAstPanel> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroAstPanel p where p.logicalKey = :logicalKey and p.isCurrent = 'Y'", MicroAstPanel.class);
        query.setParameter("logicalKey", logicalKey);
        return query.uniqueResultOptional().orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstPanel> search(String q, String status, String organismGroup, String sort, int offset,
            int limit) {
        Query<MicroAstPanel> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroAstPanel p" + searchWhere(q, status, organismGroup)
                        + ("name-desc".equals(sort) ? " order by lower(p.name) desc, p.versionNumber desc"
                                : " order by lower(p.name) asc, p.versionNumber desc"),
                        MicroAstPanel.class);
        setSearchParameters(query, q, status, organismGroup);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public long countSearch(String q, String status, String organismGroup) {
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(
                "select count(p.id) from MicroAstPanel p" + searchWhere(q, status, organismGroup), Long.class);
        setSearchParameters(query, q, status, organismGroup);
        return query.getSingleResult();
    }

    private String searchWhere(String q, String status, String organismGroup) {
        StringBuilder hql = new StringBuilder(" where 1 = 1");
        if (q != null && !q.isBlank()) {
            hql.append(" and lower(p.name) like :q");
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            hql.append(" and p.isActive = :active");
        }
        if (organismGroup != null && !organismGroup.isBlank()) {
            hql.append(" and p.organismGroup = :organismGroup");
        }
        return hql.toString();
    }

    private void setSearchParameters(Query<?> query, String q, String status, String organismGroup) {
        if (q != null && !q.isBlank()) {
            query.setParameter("q", "%" + q.trim().toLowerCase(java.util.Locale.ROOT) + "%");
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            query.setParameter("active", "ACTIVE".equalsIgnoreCase(status) ? "Y" : "N");
        }
        if (organismGroup != null && !organismGroup.isBlank()) {
            query.setParameter("organismGroup", organismGroup.trim());
        }
    }
}
