package org.openelisglobal.analyzerimport.analyzerreaders;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;

public class AnalyzerReaderUtil {
    // private SampleDAO sampleDAO = new SampleDAOImpl();
    // private AnalysisDAO analysisDAO = new AnalysisDAOImpl();
    // private ResultDAO resultDAO = new ResultDAOImpl();

    protected SampleService sampleService = SpringContext.getBean(SampleService.class);
    protected AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
    protected ResultService resultService = SpringContext.getBean(ResultService.class);

    public AnalyzerResults createAnalyzerResultFromDB(AnalyzerResults resultFromAnalyzer) {

        Sample sample = sampleService.getSampleByAccessionNumber(resultFromAnalyzer.getAccessionNumber());

        if (sample != null && sample.getId() != null) {
            List<Analysis> analysisList = analysisService.getAnalysesBySampleId(sample.getId());
            String acceptedStatusId = SpringContext.getBean(IStatusService.class)
                    .getStatusID(AnalysisStatus.TechnicalAcceptance);

            for (Analysis analysis : analysisList) {
                if (analysis.getStatusId().equals(acceptedStatusId)
                        && analysis.getTest().getId().equals(resultFromAnalyzer.getTestId())) {
                    List<Result> resultList = resultService.getResultsByAnalysis(analysis);
                    if (resultList.size() > 0) {
                        try {
                            AnalyzerResults resultFromDB = (AnalyzerResults) resultFromAnalyzer.clone();
                            resultFromDB.setResult(resultList.get(resultList.size() - 1).getValue());
                            resultFromDB.setCompleteDate(analysis.getCompletedDate() == null ? null
                                    : new Timestamp(analysis.getCompletedDate().getTime()));
                            resultFromDB.setReadOnly(true);
                            resultFromDB.setResultType(resultFromAnalyzer.getResultType());
                            return resultFromDB;
                        } catch (CloneNotSupportedException e) {
                            LogEvent.logDebug(e);
                        }
                    }
                }
            }
        }
        return null;
    }
}
