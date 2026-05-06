package org.openelisglobal.compliance.dao;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;
import org.openelisglobal.compliance.valueholder.ThresholdType;

/**
 * DAO interface for ComplianceThreshold entity operations.
 *
 * Follows OpenELIS DAO patterns extending BaseDAO for standard CRUD operations.
 * Provides domain-specific query methods for threshold management.
 */
public interface ComplianceThresholdDAO extends BaseDAO<ComplianceThreshold, String> {

    /** All thresholds for a parameter group, ordered by sort order. */
    List<ComplianceThreshold> getThresholdsByGroupId(String groupId) throws LIMSRuntimeException;

    /**
     * Check if a (parameterCode, thresholdType) combination already exists within a
     * group. Enforces per-type uniqueness so multi-limit saves (HIGH + BORDERLINE
     * on the same parameter) don't collide.
     */
    boolean parameterExistsInGroupForType(String groupId, String parameterCode, ThresholdType thresholdType)
            throws LIMSRuntimeException;

    /** All thresholds for a specific test. */
    List<ComplianceThreshold> getThresholdsByTestId(String testId) throws LIMSRuntimeException;

    /** Thresholds for a specific test and standard combination. */
    List<ComplianceThreshold> getThresholdsByTestAndStandard(String testId, String standardId)
            throws LIMSRuntimeException;

    /**
     * One row per test that has at least one threshold linked. Each row is
     * {@code [testId, thresholdCount, standardCount]}. Computed as a single GROUP
     * BY so the Tab 2 overview table loads with one query instead of fanning out
     * per-test.
     */
    List<Object[]> getTestThresholdSummary() throws LIMSRuntimeException;

    /**
     * For each given standard id, the number of distinct linked tests
     * ({@code COUNT(DISTINCT ct.test.id)}). Computed as a single aggregate so the
     * Standards list page can render the Tests column without N+1 fan-out.
     * Standards with zero linked tests are absent from the map.
     */
    Map<String, Integer> countLinkedTestsByStandardIds(Collection<String> standardIds) throws LIMSRuntimeException;

    /**
     * Returns true if any {@link ComplianceThreshold} references the given
     * parameter group. Used to enforce BR-003: a parameter group cannot be deleted
     * while thresholds reference it.
     */
    boolean groupHasThresholds(String groupId) throws LIMSRuntimeException;

    /**
     * Returns true if any {@link ComplianceThreshold} (via its parameter group)
     * references the given standard. Used to enforce BR-002: a standard with linked
     * thresholds cannot be deleted, only archived.
     */
    boolean standardHasThresholds(String standardId) throws LIMSRuntimeException;
}
