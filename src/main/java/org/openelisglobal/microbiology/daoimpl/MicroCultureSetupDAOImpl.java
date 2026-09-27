package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroCultureSetupDAO;
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroCultureSetupDAOImpl extends BaseDAOImpl<MicroCultureSetup, String> implements MicroCultureSetupDAO {

    public MicroCultureSetupDAOImpl() {
        super(MicroCultureSetup.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCultureSetup> getActiveSetupsByWorkflowType(String workflowType) {
        Query<MicroCultureSetup> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroCultureSetup c where c.isActive = 'Y' and c.workflowType = :workflowType"
                        + " order by c.name", MicroCultureSetup.class);
        query.setParameter("workflowType", workflowType);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCultureSetup getActiveSetupForMethod(String methodId, String workflowType) {
        Query<MicroCultureSetup> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroCultureSetup c where c.isActive = 'Y' and c.methodId = :methodId"
                        + " and c.workflowType = :workflowType", MicroCultureSetup.class);
        query.setParameter("methodId", methodId);
        query.setParameter("workflowType", workflowType);
        return query.uniqueResultOptional().orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MicroCultureSetup> findByMethodAndWorkflowType(String methodId, String workflowType) {
        Query<MicroCultureSetup> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroCultureSetup c where c.methodId = :methodId" + " and c.workflowType = :workflowType",
                MicroCultureSetup.class);
        query.setParameter("methodId", methodId);
        query.setParameter("workflowType", workflowType);
        return query.uniqueResultOptional();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCultureSetup> search(String q, String status, String workflow, String sort, int offset,
            int limit) {
        Query<MicroCultureSetup> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroCultureSetup c" + searchWhere(q, status, workflow)
                        + ("name-desc".equals(sort) ? " order by lower(c.name) desc" : " order by lower(c.name) asc"),
                        MicroCultureSetup.class);
        setSearchParameters(query, q, status, workflow);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public long countSearch(String q, String status, String workflow) {
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(
                "select count(c.id) from MicroCultureSetup c" + searchWhere(q, status, workflow), Long.class);
        setSearchParameters(query, q, status, workflow);
        return query.getSingleResult();
    }

    private String searchWhere(String q, String status, String workflow) {
        StringBuilder hql = new StringBuilder(" where 1 = 1");
        if (q != null && !q.isBlank()) {
            hql.append(" and (lower(c.name) like :q or exists (select m.id from Method m")
                    .append(" where m.id = c.methodId and lower(m.methodName) like :q))");
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            hql.append(" and c.isActive = :active");
        }
        if (workflow != null && !workflow.isBlank()) {
            hql.append(" and c.workflowType = :workflow");
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
