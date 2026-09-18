package org.openelisglobal.compliance.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.compliance.valueholder.ParameterGroup;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * ParameterGroupService — manages parameter groups within compliance standards.
 */
public interface ParameterGroupService extends BaseObjectService<ParameterGroup, String> {

    /** All parameter groups for a standard, ordered. */
    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_VIEW')")
    List<ParameterGroup> getGroupsByStandardId(String standardId);

    /**
     * Bulk count of parameter groups across the given standard ids — a single SQL
     * aggregate. Standards with zero groups are absent from the returned map. Used
     * from the list endpoint to avoid an N+1 fan-out.
     */
    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_VIEW')")
    Map<String, Integer> countGroupsByStandardIds(Collection<String> standardIds);
}
