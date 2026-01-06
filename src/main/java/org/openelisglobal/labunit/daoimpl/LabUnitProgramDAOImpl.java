package org.openelisglobal.labunit.daoimpl;

import java.util.List;
import jakarta.persistence.Query;
import org.openelisglobal.labunit.dao.LabUnitProgramDAO;
import org.openelisglobal.labunit.valueholder.LabUnitProgram;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Repository;

@Repository
public class LabUnitProgramDAOImpl extends BaseDAOImpl<LabUnitProgram, String> implements LabUnitProgramDAO {

    public LabUnitProgramDAOImpl() {
        super(LabUnitProgram.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<LabUnitProgram> getProgramsByLabUnitId(String labUnitId) {
        try {
            Query query = entityManager.createQuery(
                "FROM LabUnitProgram WHERE labUnitId = :labUnitId ORDER BY createdAt", 
                LabUnitProgram.class);
            query.setParameter("labUnitId", labUnitId);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving programs by lab unit", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<LabUnitProgram> getLabUnitsByProgramId(String programId) {
        try {
            Query query = entityManager.createQuery(
                "FROM LabUnitProgram WHERE programId = :programId", 
                LabUnitProgram.class);
            query.setParameter("programId", programId);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving lab units by program", e);
        }
    }

    @Override
    public LabUnitProgram getByLabUnitAndProgramId(String labUnitId, String programId) {
        try {
            Query query = entityManager.createQuery(
                "FROM LabUnitProgram WHERE labUnitId = :labUnitId AND programId = :programId", 
                LabUnitProgram.class);
            query.setParameter("labUnitId", labUnitId);
            query.setParameter("programId", programId);
            query.setMaxResults(1);
            return (LabUnitProgram) query.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void deleteByLabUnitId(String labUnitId) {
        try {
            Query query = entityManager.createQuery(
                "DELETE FROM LabUnitProgram WHERE labUnitId = :labUnitId");
            query.setParameter("labUnitId", labUnitId);
            query.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting programs by lab unit", e);
        }
    }

    @Override
    public void deleteByProgramId(String programId) {
        try {
            Query query = entityManager.createQuery(
                "DELETE FROM LabUnitProgram WHERE programId = :programId");
            query.setParameter("programId", programId);
            query.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting programs by program", e);
        }
    }
}