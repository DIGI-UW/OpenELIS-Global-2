package org.openelisglobal.biorepository.daoimpl;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.openelisglobal.biorepository.dao.RetentionPolicyDAO;
import org.openelisglobal.biorepository.valueholder.RetentionPolicy;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Repository;

/**
 * DAO implementation for RetentionPolicy entity.
 */
@Repository
public class RetentionPolicyDAOImpl extends BaseDAOImpl<RetentionPolicy, Integer> implements RetentionPolicyDAO {

    public RetentionPolicyDAOImpl() {
        super(RetentionPolicy.class);
    }

    @Override
    public List<RetentionPolicy> findAllActive() {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM RetentionPolicy rp WHERE rp.isActive = true ORDER BY rp.policyName";
        return session.createQuery(hql, RetentionPolicy.class).getResultList();
    }

    @Override
    public Optional<RetentionPolicy> findByProjectId(Integer projectId) {
        if (projectId == null) {
            return Optional.empty();
        }
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM RetentionPolicy rp WHERE rp.projectId = :projectId AND rp.isActive = true";
        List<RetentionPolicy> results = session.createQuery(hql, RetentionPolicy.class)
                .setParameter("projectId", projectId).setMaxResults(1).getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<RetentionPolicy> findBySampleTypeId(Integer sampleTypeId) {
        if (sampleTypeId == null) {
            return Optional.empty();
        }
        Session session = entityManager.unwrap(Session.class);
        // Only match sample type policies that don't have a project specified
        String hql = "FROM RetentionPolicy rp WHERE rp.sampleTypeId = :sampleTypeId "
                + "AND rp.projectId IS NULL AND rp.isActive = true";
        List<RetentionPolicy> results = session.createQuery(hql, RetentionPolicy.class)
                .setParameter("sampleTypeId", sampleTypeId).setMaxResults(1).getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<RetentionPolicy> findApplicablePolicy(Integer projectId, Integer sampleTypeId) {
        // Priority 1: Try to find project-specific policy
        if (projectId != null) {
            Optional<RetentionPolicy> projectPolicy = findByProjectId(projectId);
            if (projectPolicy.isPresent()) {
                return projectPolicy;
            }
        }

        // Priority 2: Fall back to sample type policy
        return findBySampleTypeId(sampleTypeId);
    }

    @Override
    public boolean existsByProjectId(Integer projectId) {
        if (projectId == null) {
            return false;
        }
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT COUNT(rp) FROM RetentionPolicy rp WHERE rp.projectId = :projectId AND rp.isActive = true";
        Long count = session.createQuery(hql, Long.class).setParameter("projectId", projectId).getSingleResult();
        return count > 0;
    }

    @Override
    public boolean existsBySampleTypeIdAndNoProject(Integer sampleTypeId) {
        if (sampleTypeId == null) {
            return false;
        }
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT COUNT(rp) FROM RetentionPolicy rp WHERE rp.sampleTypeId = :sampleTypeId "
                + "AND rp.projectId IS NULL AND rp.isActive = true";
        Long count = session.createQuery(hql, Long.class).setParameter("sampleTypeId", sampleTypeId).getSingleResult();
        return count > 0;
    }

    @Override
    public Optional<RetentionPolicy> findByProjectName(String projectName) {
        if (projectName == null || projectName.trim().isEmpty()) {
            return Optional.empty();
        }
        Session session = entityManager.unwrap(Session.class);
        // Match by project name (case-insensitive)
        String hql = "FROM RetentionPolicy rp WHERE LOWER(rp.projectName) = LOWER(:projectName) AND rp.isActive = true";
        List<RetentionPolicy> results = session.createQuery(hql, RetentionPolicy.class)
                .setParameter("projectName", projectName.trim()).setMaxResults(1).getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<RetentionPolicy> findApplicablePolicyByNames(String projectName, Integer sampleTypeId) {
        // Priority 1: Try to find project-specific policy by project name
        if (projectName != null && !projectName.trim().isEmpty()) {
            Optional<RetentionPolicy> projectPolicy = findByProjectName(projectName);
            if (projectPolicy.isPresent()) {
                return projectPolicy;
            }
        }

        // Priority 2: Fall back to sample type policy
        return findBySampleTypeId(sampleTypeId);
    }
}
