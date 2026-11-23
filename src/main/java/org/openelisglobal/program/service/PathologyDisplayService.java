package org.openelisglobal.program.service;

import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem.LabelVerificationView;
import org.openelisglobal.program.valueholder.pathology.PathologyDisplayItem;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;

public interface PathologyDisplayService {

    PathologyCaseViewDisplayItem convertToCaseDisplayItem(Integer pathologySampleId);

    PathologyDisplayItem convertToDisplayItem(Integer pathologySampleId);

    PathologySample getPathologySampleWithLoadedAtttributes(Integer pathologySampleId);

    /**
     * Who checked a slide's label and when, as the case view shows it, or null when
     * its label was never checked.
     */
    LabelVerificationView labelVerificationOf(PathologySlide slide);
}
