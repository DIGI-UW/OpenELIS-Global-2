package org.openelisglobal.notebook.daoimpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.notebook.dao.WorkflowPageTemplateDAO;
import org.openelisglobal.notebook.valueholder.WorkflowPageTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for WorkflowPageTemplate operations. This is a simple
 * read-only DAO for static configuration data.
 */
@Component
@Transactional
public class WorkflowPageTemplateDAOImpl implements WorkflowPageTemplateDAO {

    @PersistenceContext
    protected EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowPageTemplate> findAllActive() {
        Session session = entityManager.unwrap(Session.class);
        Query<WorkflowPageTemplate> query = session.createNativeQuery(
                "SELECT * FROM clinlims.workflow_page_template WHERE is_active = true ORDER BY display_order",
                WorkflowPageTemplate.class);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowPageTemplate> findByCategory(String category) {
        Session session = entityManager.unwrap(Session.class);
        Query<WorkflowPageTemplate> query = session.createNativeQuery(
                "SELECT * FROM clinlims.workflow_page_template WHERE workflow_category = :category AND is_active = true ORDER BY display_order",
                WorkflowPageTemplate.class);
        query.setParameter("category", category);
        return query.list();
    }
}
