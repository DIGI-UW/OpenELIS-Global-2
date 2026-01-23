package org.openelisglobal.biorepository.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.biorepository.valueholder.RetentionPolicy;
import org.openelisglobal.common.dao.BaseDAO;

/**
 * DAO interface for RetentionPolicy entity.
 */
public interface RetentionPolicyDAO extends BaseDAO<RetentionPolicy, Integer> {

    /**
     * Find all active retention policies.
     */
    List<RetentionPolicy> findAllActive();

    /**
     * Find a retention policy by project ID.
     *
     * @param projectId the project ID
     * @return the policy if found
     */
    Optional<RetentionPolicy> findByProjectId(Integer projectId);

    /**
     * Find a retention policy by sample type ID (where no project is specified).
     *
     * @param sampleTypeId the sample type ID
     * @return the policy if found
     */
    Optional<RetentionPolicy> findBySampleTypeId(Integer sampleTypeId);

    /**
     * Find the applicable retention policy for a sample. Priority: Project-specific
     * policy > Sample type policy
     *
     * @param projectId    the project ID (can be null)
     * @param sampleTypeId the sample type ID
     * @return the applicable policy if found
     */
    Optional<RetentionPolicy> findApplicablePolicy(Integer projectId, Integer sampleTypeId);

    /**
     * Check if a policy with the given project ID already exists.
     */
    boolean existsByProjectId(Integer projectId);

    /**
     * Check if a policy with the given sample type ID (and no project) already
     * exists.
     */
    boolean existsBySampleTypeIdAndNoProject(Integer sampleTypeId);

    /**
     * Find a retention policy by project name (case-insensitive).
     *
     * @param projectName the project name/code (e.g., "PROJ-MALARIA-001")
     * @return the policy if found
     */
    Optional<RetentionPolicy> findByProjectName(String projectName);

    /**
     * Find the applicable retention policy for a sample using project name. This is
     * useful when the sample stores project code as string, not integer ID.
     * Priority: Project-specific policy > Sample type policy
     *
     * @param projectName  the project name/code (can be null)
     * @param sampleTypeId the sample type ID
     * @return the applicable policy if found
     */
    Optional<RetentionPolicy> findApplicablePolicyByNames(String projectName, Integer sampleTypeId);
}
