package org.openelisglobal.analyzerresults.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.analyzerresults.valueholder.SampleGrouping;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.result.valueholder.Result;

public interface AnalyzerResultsService extends BaseObjectService<AnalyzerResults, String> {

    AnalyzerResults readAnalyzerResults(String idString);

    List<AnalyzerResults> getResultsbyAnalyzer(String analyzerId);

    void insertAnalyzerResults(List<AnalyzerResults> results, String sysUserId);

    /** @return the accepted results and the results calculations wrote from them */
    List<Result> persistAnalyzerResults(List<AnalyzerResults> deletableAnalyzerResults,
            List<SampleGrouping> sampleGroupList, String sysUserId);

    List<AnalyzerResults> findHeldMappingResultsByAnalyzer(String analyzerId);

    Map<String, Long> countHeldResultsByAnalyzerIds(List<String> analyzerIds);
}
