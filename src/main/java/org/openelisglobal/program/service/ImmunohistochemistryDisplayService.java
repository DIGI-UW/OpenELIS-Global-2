package org.openelisglobal.program.service;

import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistryCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistryDisplayItem;

public interface ImmunohistochemistryDisplayService {

    ImmunohistochemistryCaseViewDisplayItem convertToCaseDisplayItem(Integer immunohistochemistrySampleId);

    ImmunohistochemistryDisplayItem convertToDisplayItem(Integer immunohistochemistrySampleId);
}
