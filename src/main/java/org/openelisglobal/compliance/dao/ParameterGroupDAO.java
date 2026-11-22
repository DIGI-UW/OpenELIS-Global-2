package org.openelisglobal.compliance.dao;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.compliance.valueholder.ParameterGroup;

/**
 * ParameterGroupDAO interface for parameter group data access operations.
 */
public interface ParameterGroupDAO extends BaseDAO<ParameterGroup, String> {

    /**
     * Looks up the {@link ParameterGroup} that owns {@code (standardId, groupName)}
     * — null if no row matches. Used by save() to enforce the per-standard
     * uniqueness invariant without rejecting updates that keep the same name (the
     * existence-only check returned true for the row being edited).
     */
    ParameterGroup findByStandardIdAndName(String standardId, String groupName) throws LIMSRuntimeException;

    /** All parameter groups for a standard, ordered by sort order. */
    List<ParameterGroup> getGroupsByStandardId(String standardId) throws LIMSRuntimeException;

    /**
     * Bulk count of parameter groups across the given standard ids — a single SQL
     * aggregate. Standards with zero groups are absent from the returned map. Used
     * from list endpoints to avoid an N+1 fan-out.
     */
    Map<String, Integer> countGroupsByStandardIds(Collection<String> standardIds) throws LIMSRuntimeException;
}
