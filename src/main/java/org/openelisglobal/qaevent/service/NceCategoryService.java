package org.openelisglobal.qaevent.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.qaevent.valueholder.NceCategory;
import org.springframework.security.access.prepost.PreAuthorize;

public interface NceCategoryService extends BaseObjectService<NceCategory, Integer> {

    @PreAuthorize("hasAuthority('PRIV_NCE_VIEW')")
    List<NceCategory> getAllNceCategories();

    @PreAuthorize("hasAuthority('PRIV_NCE_VIEW')")
    List<IdValuePair> getActiveCategoriesAsIdValuePairs();
}
