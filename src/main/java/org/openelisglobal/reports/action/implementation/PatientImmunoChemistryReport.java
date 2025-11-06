package org.openelisglobal.reports.action.implementation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.program.service.ImmunohistochemistrySampleService;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.reports.action.implementation.reportBeans.ProgramSampleReportData;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestServiceImpl;

public class PatientImmunoChemistryReport extends PatientProgramReport {

    private ImmunohistochemistrySampleService immunohistochemistrySampleService = SpringContext
            .getBean(ImmunohistochemistrySampleService.class);
    private ImmunohistochemistrySample immunohistochemistrySample;

    @Override
    protected String getReportName() {
        return "PatientImmunoChemistryReport";
    }

    @Override
    protected void setAdditionalReportItems() {
        List<Analysis> analyses = analysisService.getAnalysesBySampleIdAndStatusId(sampleService.getId(sample),
                analysisStatusIds);
        List<ProgramSampleReportData.Result> resultsData = new ArrayList<>();
        analyses.forEach(analysis -> {
            ProgramSampleReportData.Result resultData = new ProgramSampleReportData.Result();
            resultData.setTest(TestServiceImpl.getUserLocalizedReportingTestName(analysis.getTest()));
            List<Result> resultList = analysisService.getResults(analysis);
            resultData.setResult(getAppropriateResults(resultList));
            resultData.setUom(getUnitOfMeasure(analysis.getTest()));
            resultsData.add(resultData);
        });
        data.setResults(resultsData);
        if (form.getCodedConclusions() != null) {
            List<String> codedConclusions = form.getCodedConclusions().stream()
                    .map(e -> dictionaryService.get(e).getLocalizedName()).collect(Collectors.toList());
            data.setCodedConclusion(codedConclusions);
        }
        data.setTextConclusion(form.getConclusion());
    }

    @Override
    protected void innitializeSample(ReportForm form) {
        immunohistochemistrySample = immunohistochemistrySampleService.get(Integer.valueOf(form.getProgramSampleId()));
        sample = immunohistochemistrySample.getSample();
    }
}
