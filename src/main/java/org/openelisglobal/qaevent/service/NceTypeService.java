package org.openelisglobal.qaevent.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.qaevent.valueholder.NceType;
import org.springframework.security.access.prepost.PreAuthorize;

public interface NceTypeService extends BaseObjectService<NceType, Integer> {

    @PreAuthorize("hasAuthority('PRIV_NCE_VIEW')")
    List<NceType> getAllNceTypes();

    /**
     * Get all NCE types for a specific category.
     *
     * @param categoryId the category ID
     * @return list of NCE types in that category
     */
    @PreAuthorize("hasAuthority('PRIV_NCE_VIEW')")
    List<NceType> getNceTypesByCategoryId(Integer categoryId);

    @PreAuthorize("hasAuthority('PRIV_NCE_VIEW')")
    List<IdValuePair> getActiveTypesAsIdValuePairs();

    /**
     * All catalog labels for historical views, including inactive rows. Same
     * catalogue read as {@link #getActiveTypesAsIdValuePairs()}, differing only in
     * whether inactive rows are included, so it takes the same privilege.
     */
    @PreAuthorize("hasAuthority('PRIV_NCE_VIEW')")
    List<IdValuePair> getAllTypesAsIdValuePairs();
}
