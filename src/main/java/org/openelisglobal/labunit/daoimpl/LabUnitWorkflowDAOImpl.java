package org.openelisglobal.labunit.daoimpl;

import jakarta.persistence.Query;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.labunit.dao.LabUnitWorkflowDAO;
import org.openelisglobal.labunit.valueholder.LabUnitWorkflow;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class LabUnitWorkflowDAOImpl extends BaseDAOImpl<LabUnitWorkflow, String> implements LabUnitWorkflowDAO {

    public LabUnitWorkflowDAOImpl() {
        super(LabUnitWorkflow.class);
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<LabUnitWorkflow> getWorkflowsByLabUnitId(String labUnitId) {
        try {
            Query query = entityManager.createQuery(
                    "FROM LabUnitWorkflow WHERE labUnitId = :labUnitId ORDER BY createdAt", LabUnitWorkflow.class);
            query.setParameter("labUnitId", labUnitId);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving workflows by lab unit", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<LabUnitWorkflow> getLabUnitsByWorkflowId(String workflowId) {
        try {
            Query query = entityManager.createQuery("FROM LabUnitWorkflow WHERE workflowId = :workflowId",
                    LabUnitWorkflow.class);
            query.setParameter("workflowId", workflowId);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving lab units by workflow", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LabUnitWorkflow getByLabUnitAndWorkflowId(String labUnitId, String workflowId) {
        try {
            Query query = entityManager.createQuery(
                    "FROM LabUnitWorkflow WHERE labUnitId = :labUnitId AND workflowId = :workflowId",
                    LabUnitWorkflow.class);
            query.setParameter("labUnitId", labUnitId);
            query.setParameter("workflowId", workflowId);
            query.setMaxResults(1);
            return (LabUnitWorkflow) query.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void deleteByLabUnitId(String labUnitId) {
        try {
            Query query = entityManager.createQuery("DELETE FROM LabUnitWorkflow WHERE labUnitId = :labUnitId");
            query.setParameter("labUnitId", labUnitId);
            query.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting workflows by lab unit", e);
        }
    }

    @Override
    public void deleteByWorkflowId(String workflowId) {
        try {
            Query query = entityManager.createQuery("DELETE FROM LabUnitWorkflow WHERE workflowId = :workflowId");
            query.setParameter("workflowId", workflowId);
            query.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting workflows by workflow", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<LabUnitWorkflow> getDefaultWorkflows(String labUnitId) {
        try {
            Query query = entityManager.createQuery(
                    "FROM LabUnitWorkflow WHERE labUnitId = :labUnitId AND isDefault = true", LabUnitWorkflow.class);
            query.setParameter("labUnitId", labUnitId);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving default workflows", e);
        }
    }

    @Override
    public void updateDefaultWorkflow(String labUnitId, String workflowId) {
        try {
            // Clear existing default
            Query clearQuery = entityManager
                    .createQuery("UPDATE LabUnitWorkflow SET isDefault = false WHERE labUnitId = :labUnitId");
            clearQuery.setParameter("labUnitId", labUnitId);
            clearQuery.executeUpdate();

            // Set new default
            Query setQuery = entityManager.createQuery(
                    "UPDATE LabUnitWorkflow SET isDefault = true WHERE labUnitId = :labUnitId AND workflowId = :workflowId");
            setQuery.setParameter("labUnitId", labUnitId);
            setQuery.setParameter("workflowId", workflowId);
            setQuery.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error updating default workflow", e);
        }
    }

    @Override
    public void clearDefaultWorkflows(String labUnitId) {
        try {
            Query query = entityManager
                    .createQuery("UPDATE LabUnitWorkflow SET isDefault = false WHERE labUnitId = :labUnitId");
            query.setParameter("labUnitId", labUnitId);
            query.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error clearing default workflows", e);
        }
    }
}