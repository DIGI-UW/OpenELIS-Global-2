package org.openelisglobal.program.service;

import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.pathology.PathologyDisplayItem;
import org.openelisglobal.program.valueholder.pathology.PathologySample;

public interface PathologyDisplayService {

    PathologyCaseViewDisplayItem convertToCaseDisplayItem(Integer pathologySampleId);

    PathologyDisplayItem convertToDisplayItem(Integer pathologySampleId);

    PathologySample getPathologySampleWithLoadedAtttributes(Integer pathologySampleId);
}
