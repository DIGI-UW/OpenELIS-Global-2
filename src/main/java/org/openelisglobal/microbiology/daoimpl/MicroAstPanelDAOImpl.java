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
    public List<MicroAstPanel> getActivePanelsByWorkflowType(String workflowType) {
        Query<MicroAstPanel> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroAstPanel p where p.isActive = 'Y' and p.isCurrent = 'Y' and p.workflowType = :workflowType"
                        + " order by p.name",
                MicroAstPanel.class);
        query.setParameter("workflowType", workflowType);
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
    public List<MicroAstPanel> search(String q, String status, String workflow, String sort, int offset, int limit) {
        Query<MicroAstPanel> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroAstPanel p" + searchWhere(q, status, workflow)
                        + ("name-desc".equals(sort) ? " order by lower(p.name) desc, p.versionNumber desc"
                                : " order by lower(p.name) asc, p.versionNumber desc"),
                        MicroAstPanel.class);
        setSearchParameters(query, q, status, workflow);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public long countSearch(String q, String status, String workflow) {
        Query<Long> query = entityManager.unwrap(Session.class)
                .createQuery("select count(p.id) from MicroAstPanel p" + searchWhere(q, status, workflow), Long.class);
        setSearchParameters(query, q, status, workflow);
        return query.getSingleResult();
    }

    private String searchWhere(String q, String status, String workflow) {
        StringBuilder hql = new StringBuilder(" where 1 = 1");
        if (q != null && !q.isBlank()) {
            hql.append(" and lower(p.name) like :q");
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            hql.append(" and p.isActive = :active");
        }
        if (workflow != null && !workflow.isBlank()) {
            hql.append(" and p.workflowType = :workflow");
        }
        return hql.toString();
    }

    private void setSearchParameters(Query<?> query, String q, String status, String workflow) {
        if (q != null && !q.isBlank()) {
            query.setParameter("q", "%" + q.trim().toLowerCase(java.util.Locale.ROOT) + "%");
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            query.setParameter("active", "ACTIVE".equalsIgnoreCase(status) ? "Y" : "N");
        }
        if (workflow != null && !workflow.isBlank()) {
            query.setParameter("workflow", workflow.trim().toUpperCase(java.util.Locale.ROOT));
        }
    }
}
