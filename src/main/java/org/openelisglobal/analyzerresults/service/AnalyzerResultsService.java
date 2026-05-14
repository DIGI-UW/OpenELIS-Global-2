package org.openelisglobal.analyzerresults.service;

import java.util.List;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.analyzerresults.valueholder.SampleGrouping;
import org.openelisglobal.common.service.BaseObjectService;

public interface AnalyzerResultsService extends BaseObjectService<AnalyzerResults, String> {

    AnalyzerResults readAnalyzerResults(String idString);

    List<AnalyzerResults> getResultsbyAnalyzer(String analyzerId);

    void insertAnalyzerResults(List<AnalyzerResults> results, String sysUserId);

    void persistAnalyzerResults(List<AnalyzerResults> deletableAnalyzerResults, List<SampleGrouping> sampleGroupList,
            String sysUserId);

    /**
     * Staging rows flagged with an {@code import_issue_reason} (unmapped host code,
     * cartridge-as-result, unknown dict value, etc.). Backing query for the Import
     * Issues admin panel.
     */
    List<AnalyzerResults> findWithImportIssues(int limit);
}
