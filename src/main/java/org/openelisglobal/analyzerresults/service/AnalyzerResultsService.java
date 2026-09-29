package org.openelisglobal.analyzerresults.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.analyzerresults.valueholder.SampleGrouping;
import org.openelisglobal.common.service.BaseObjectService;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_ANALYZER_IMPORT')")
public interface AnalyzerResultsService extends BaseObjectService<AnalyzerResults, String> {

    AnalyzerResults readAnalyzerResults(String idString);

    List<AnalyzerResults> getResultsbyAnalyzer(String analyzerId);

    void insertAnalyzerResults(List<AnalyzerResults> results, String sysUserId);

    void persistAnalyzerResults(List<AnalyzerResults> deletableAnalyzerResults, List<SampleGrouping> sampleGroupList,
            String sysUserId);

    List<AnalyzerResults> findHeldResultValuesByProfile(String profileId, int profileRevision);

    Map<String, Long> countHeldResultsByAnalyzerIds(List<String> analyzerIds);
}
