package org.openelisglobal.labunit.daoimpl;

import java.util.List;
import jakarta.persistence.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.labunit.dao.LabUnitProjectDAO;
import org.openelisglobal.labunit.valueholder.LabUnitProject;
import org.springframework.stereotype.Repository;

@Repository
public class LabUnitProjectDAOImpl extends BaseDAOImpl<LabUnitProject, String> implements LabUnitProjectDAO {

    public LabUnitProjectDAOImpl() {
        super(LabUnitProject.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<LabUnitProject> getProjectsByLabUnitId(String labUnitId) {
        try {
            Query query = entityManager.createQuery(
                "FROM LabUnitProject WHERE labUnitId = :labUnitId ORDER BY createdAt", 
                LabUnitProject.class);
            query.setParameter("labUnitId", labUnitId);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving projects by lab unit", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<LabUnitProject> getLabUnitsByProjectId(String projectId) {
        try {
            Query query = entityManager.createQuery(
                "FROM LabUnitProject WHERE projectId = :projectId", 
                LabUnitProject.class);
            query.setParameter("projectId", projectId);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving lab units by project", e);
        }
    }

    @Override
    public LabUnitProject getByLabUnitAndProjectId(String labUnitId, String projectId) {
        try {
            Query query = entityManager.createQuery(
                "FROM LabUnitProject WHERE labUnitId = :labUnitId AND projectId = :projectId", 
                LabUnitProject.class);
            query.setParameter("labUnitId", labUnitId);
            query.setParameter("projectId", projectId);
            query.setMaxResults(1);
            List<LabUnitProject> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving lab unit project assignment", e);
        }
    }

    @Override
    public void deleteByLabUnitId(String labUnitId) {
        try {
            Query query = entityManager.createQuery(
                "DELETE FROM LabUnitProject WHERE labUnitId = :labUnitId");
            query.setParameter("labUnitId", labUnitId);
            query.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting projects by lab unit", e);
        }
    }

    @Override
    public void deleteByProjectId(String projectId) {
        try {
            Query query = entityManager.createQuery(
                "DELETE FROM LabUnitProject WHERE projectId = :projectId");
            query.setParameter("projectId", projectId);
            query.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting projects by project", e);
        }
    }
}