package org.openelisglobal.program.service;

import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistryCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistryDisplayItem;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ImmunohistochemistryDisplayService {

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    ImmunohistochemistryCaseViewDisplayItem convertToCaseDisplayItem(Integer immunohistochemistrySampleId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    ImmunohistochemistryDisplayItem convertToDisplayItem(Integer immunohistochemistrySampleId);
}
