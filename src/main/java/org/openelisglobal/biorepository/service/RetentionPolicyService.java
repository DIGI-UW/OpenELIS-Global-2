package org.openelisglobal.biorepository.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.biorepository.valueholder.RetentionPolicy;
import org.openelisglobal.common.service.BaseObjectService;

/**
 * Service interface for RetentionPolicy management.
 */
public interface RetentionPolicyService extends BaseObjectService<RetentionPolicy, Integer> {

    /**
     * Get all active retention policies.
     */
    List<RetentionPolicy> getAllActive();

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
     * Calculate the retention expiry date for a sample based on applicable policy.
     *
     * @param projectId    the project ID (can be null)
     * @param sampleTypeId the sample type ID
     * @param fromDate     the start date (typically collection or receipt date)
     * @return the calculated expiry date, or null if no policy applies
     */
    LocalDate calculateExpiryDate(Integer projectId, Integer sampleTypeId, LocalDate fromDate);

    /**
     * Create a retention policy from parsed CSV data.
     *
     * @param policyName     the policy name
     * @param projectName    the project name (optional)
     * @param sampleTypeName the sample type name (optional)
     * @param periodStr      the period string (e.g., "5 years", "18 months")
     * @param sysUserId      the user creating the policy
     * @return the created policy
     */
    RetentionPolicy createFromCsv(String policyName, String projectName, String sampleTypeName, String periodStr,
            String sysUserId);

    /**
     * Import retention policies from CSV content.
     *
     * @param csvContent the CSV content
     * @param sysUserId  the user performing the import
     * @return list of imported policies
     */
    List<RetentionPolicy> importFromCsv(String csvContent, String sysUserId);

    /**
     * Deactivate a policy (soft delete).
     *
     * @param policyId the policy ID
     */
    void deactivate(Integer policyId);

    /**
     * Check if a policy already exists for the given project.
     */
    boolean existsForProject(Integer projectId);

    /**
     * Check if a policy already exists for the given sample type (without project).
     */
    boolean existsForSampleType(Integer sampleTypeId);

    /**
     * Find the applicable retention policy using project name (string). This is
     * used when the sample stores project code as a string, not integer ID.
     * Priority: Project-specific policy > Sample type policy
     *
     * @param projectName  the project name/code (can be null)
     * @param sampleTypeId the sample type ID
     * @return the applicable policy if found
     */
    Optional<RetentionPolicy> findApplicablePolicyByProjectName(String projectName, Integer sampleTypeId);

    /**
     * Calculate the retention expiry date using project name (string).
     *
     * @param projectName  the project name/code (can be null)
     * @param sampleTypeId the sample type ID
     * @param fromDate     the start date (typically collection or receipt date)
     * @return the calculated expiry date, or null if no policy applies
     */
    LocalDate calculateExpiryDateByProjectName(String projectName, Integer sampleTypeId, LocalDate fromDate);
}
