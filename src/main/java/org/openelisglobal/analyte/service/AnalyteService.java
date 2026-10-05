package org.openelisglobal.analyte.service;

import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.service.BaseObjectService;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * getAnalyteByName also accepts PRIV_CATALOGUE_VIEW: it is a catalogue lookup
 * by name, and the request-scoped ResultsValidationRetroCIUtility resolves
 * "Conclusion" and "generated CD4 Count" through it in a @PostConstruct, so
 * under analyte:view alone (Reports only) the bean failed to initialise and
 * every study-validation page 500'd for Validation.
 */
@PreAuthorize("hasAuthority('PRIV_ANALYTE_VIEW')")
public interface AnalyteService extends BaseObjectService<Analyte, String> {

    @PreAuthorize("hasAnyAuthority('PRIV_ANALYTE_VIEW','PRIV_CATALOGUE_VIEW')")

    Analyte getAnalyteByName(Analyte analyte, boolean ignoreCase);
}
