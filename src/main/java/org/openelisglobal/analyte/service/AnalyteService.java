package org.openelisglobal.analyte.service;

import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.service.BaseObjectService;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_ANALYTE_VIEW')")
public interface AnalyteService extends BaseObjectService<Analyte, String> {

    Analyte getAnalyteByName(Analyte analyte, boolean ignoreCase);
}
