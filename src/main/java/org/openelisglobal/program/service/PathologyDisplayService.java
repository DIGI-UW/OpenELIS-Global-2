package org.openelisglobal.program.service;

import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.pathology.PathologyDisplayItem;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.springframework.security.access.prepost.PreAuthorize;

public interface PathologyDisplayService {

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    PathologyCaseViewDisplayItem convertToCaseDisplayItem(Integer pathologySampleId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    PathologyDisplayItem convertToDisplayItem(Integer pathologySampleId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    PathologySample getPathologySampleWithLoadedAtttributes(Integer pathologySampleId);
}
