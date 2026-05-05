package org.openelisglobal.compliance.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.compliance.valueholder.ParameterGroup;

/**
 * ParameterGroupService — manages parameter groups within compliance standards.
 */
public interface ParameterGroupService extends BaseObjectService<ParameterGroup, String> {

    /** All parameter groups for a standard, ordered. */
    List<ParameterGroup> getGroupsByStandardId(String standardId);

    /**
     * Bulk count of parameter groups across the given standard ids — a single SQL
     * aggregate. Standards with zero groups are absent from the returned map. Used
     * from the list endpoint to avoid an N+1 fan-out.
     */
    Map<String, Integer> countGroupsByStandardIds(Collection<String> standardIds);
}
