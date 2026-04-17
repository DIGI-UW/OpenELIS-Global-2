package org.openelisglobal.analyzer.service;

import java.io.IOException;
import java.util.Map;
import org.openelisglobal.analyzer.valueholder.AnalyzerExperiment;
import org.openelisglobal.common.exception.LIMSException;
import org.openelisglobal.common.service.BaseObjectService;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
public interface AnalyzerExperimentService extends BaseObjectService<AnalyzerExperiment, Integer> {

    Integer saveMapAsCSVFile(String filename, Map<String, String> wellValues) throws LIMSException;

    Map<String, String> getWellValuesForId(Integer id) throws IOException;
}
