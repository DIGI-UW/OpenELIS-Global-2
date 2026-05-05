package org.openelisglobal.compliance.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.compliance.controller.rest.ComplianceThresholdListItem;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;

/**
 * Service interface for ComplianceThreshold entity operations.
 *
 * Constitutional compliance: extends BaseObjectService for standardized CRUD,
 * declares @Transactional boundaries on the impl, and exposes slim
 * DTO-returning variants so REST controllers stay thin (no lazy-association
 * traversal in the web layer).
 */
public interface ComplianceThresholdService extends BaseObjectService<ComplianceThreshold, String> {

    /** All thresholds for a parameter group, ordered by sort order. */
    List<ComplianceThreshold> getThresholdsByGroupId(String groupId);

    /**
     * One row per test that has at least one threshold linked. Each row is
     * {@code [testId, thresholdCount, standardCount]}. Backs the Tab 2 overview
     * table on the test-compliance admin screen.
     */
    List<Object[]> getTestThresholdSummary();

    /**
     * Bulk count of distinct linked tests per standard id — single SQL aggregate.
     * Used by the Standards list page to render the Tests column without an N+1
     * fan-out. Standards with zero linked tests are absent from the returned map.
     */
    Map<String, Integer> countLinkedTestsByStandardIds(Collection<String> standardIds);

    // ----- DTO-returning variants for REST callers -----
    //
    // The list-item DTO (group + standard summaries, value mappings) walks
    // lazy associations on the entity, so it must be built inside the service
    // transaction. Earlier the controllers carried @Transactional to keep the
    // session open through Jackson serialization — the methods below restore
    // proper layering by returning the DTO already-materialised.

    /** Returns the slim DTO list for thresholds in a parameter group. */
    List<ComplianceThresholdListItem> getThresholdItemsByGroupId(String groupId);

    /** Returns the slim DTO for a single threshold, or null if not found. */
    ComplianceThresholdListItem getThresholdItem(String thresholdId);

    /** Returns the slim DTO list for thresholds linked to a test. */
    List<ComplianceThresholdListItem> getThresholdItemsByTestId(String testId);

    /** Returns the slim DTO list for thresholds for a test under a standard. */
    List<ComplianceThresholdListItem> getThresholdItemsByTestAndStandard(String testId, String standardId);

    /**
     * Persists a new threshold and returns its slim DTO. The service resolves
     * managed entities for {@code group} and {@code test} (Jackson hydrates id-only
     * stubs from the request body, which Hibernate would refuse) so the controller
     * stays a thin request mapper.
     */
    ComplianceThresholdListItem createThresholdItem(ComplianceThreshold threshold, String sysUserId);

    /** Updates an existing threshold and returns its slim DTO. */
    ComplianceThresholdListItem updateThresholdItem(String id, ComplianceThreshold threshold, String sysUserId);
}
