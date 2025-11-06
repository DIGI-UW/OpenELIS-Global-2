package org.openelisglobal.analyte.service;

import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.service.BaseObjectService;

public interface AnalyteService extends BaseObjectService<Analyte, String> {

    Analyte getAnalyteByName(Analyte analyte, boolean ignoreCase);
}
