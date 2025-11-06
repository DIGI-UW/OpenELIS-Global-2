package org.openelisglobal.program.service.cytology;

import org.openelisglobal.program.valueholder.cytology.CytologyCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.cytology.CytologyDisplayItem;
import org.openelisglobal.program.valueholder.cytology.CytologySample;

public interface CytologyDisplayService {

    CytologyCaseViewDisplayItem convertToCaseDisplayItem(Integer cytologySampleId);

    CytologyDisplayItem convertToDisplayItem(Integer cytologySampleId);

    CytologySample getCytologySampleWithLoadedAttributes(Integer cytologySampleId);
}
