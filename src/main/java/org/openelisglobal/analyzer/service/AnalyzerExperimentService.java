package org.openelisglobal.analyzer.service;

import java.io.IOException;
import java.util.Map;
import org.openelisglobal.analyzer.valueholder.AnalyzerExperiment;
import org.openelisglobal.common.exception.LIMSException;
import org.openelisglobal.common.service.BaseObjectService;

public interface AnalyzerExperimentService extends BaseObjectService<AnalyzerExperiment, Integer> {

    Integer saveMapAsCSVFile(String filename, Map<String, String> wellValues) throws LIMSException;

    Map<String, String> getWellValuesForId(Integer id) throws IOException;
}
