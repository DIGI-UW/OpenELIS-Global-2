package org.openelisglobal.program.service.cytology;

import org.openelisglobal.program.valueholder.cytology.CytologyCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.cytology.CytologyDisplayItem;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CytologyDisplayService {

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    CytologyCaseViewDisplayItem convertToCaseDisplayItem(Integer cytologySampleId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    CytologyDisplayItem convertToDisplayItem(Integer cytologySampleId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    CytologySample getCytologySampleWithLoadedAttributes(Integer cytologySampleId);
}
