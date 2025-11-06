package org.openelisglobal.reports.action.implementation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.program.service.ImmunohistochemistrySampleService;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.valueholder.Test;

public class BreastCancerHormoneReceptorReport extends PatientProgramReport {

    private ImmunohistochemistrySampleService immunohistochemistrySampleService = SpringContext
            .getBean(ImmunohistochemistrySampleService.class);

    private ImmunohistochemistrySample immunohistochemistrySample;

    @Override
    protected String getReportName() {
        return "BreastCancerHormoneReceptorReport";
    }

    @Override
    protected void setAdditionalReportItems() {
        Test erTest = testService.getActiveTestByLocalizedName("Anti-ER", Locale.ENGLISH);
        data.setErResult(getResult(erTest));

        Test prTest = testService.getActiveTestByLocalizedName("Anti-PR", Locale.ENGLISH);
        data.setPrResult(getResult(prTest));

        Test mibTest = testService.getActiveTestByLocalizedName("Anti-Ki67", Locale.ENGLISH);
        data.setMibResult(getResult(mibTest));
    }

    @Override
    protected void createExtraReportParameters() {
        reportParameters.put("erPercent", form.getErPercent());
        reportParameters.put("erIntensity", form.getErIntensity());
        reportParameters.put("erScore", form.getErScore());
        reportParameters.put("prPercent", form.getPrPercent());
        reportParameters.put("prIntensity", form.getPrIntensity());
        reportParameters.put("prScore", form.getPrScore());
        reportParameters.put("mib", form.getMib());
        reportParameters.put("pattern", form.getPattern());
        reportParameters.put("herAssesment", form.getHerAssesment());
        reportParameters.put("herScore", form.getHerScore());
        reportParameters.put("diagnosis", form.getDiagnosis());
        reportParameters.put("molecularSubType", form.getMolecularSubType());
    }

    @Override
    protected void innitializeSample(ReportForm form) {
        immunohistochemistrySample = immunohistochemistrySampleService.get(Integer.valueOf(form.getProgramSampleId()));
        sample = immunohistochemistrySample.getSample();
    }

    private List<Analysis> getAnalysesByTestAndSampleAndStatus(Sample sample, Test test, Set<Integer> statusIdList) {
        List<Integer> sampleIdList = new ArrayList<>();
        sampleIdList.add(Integer.valueOf(sample.getId()));

        List<Integer> testIdList = new ArrayList<>();
        testIdList.add(Integer.valueOf(test.getId()));
        List<Analysis> analyses = analysisService.getAnalysesBySampleIdTestIdAndStatusId(sampleIdList, testIdList,
                new ArrayList<>(statusIdList));
        return analyses != null ? analyses : new ArrayList<Analysis>();
    }

    private String getResult(Test test) {
        String result = "";
        if (test == null) {
            return result;
        }
        List<Analysis> analyses = new ArrayList<>();
        analyses = getAnalysesByTestAndSampleAndStatus(sample, test, analysisStatusIds);
        if (!analyses.isEmpty()) {
            List<Result> resultList = analysisService.getResults(analyses.get(0));
            result = getAppropriateResults(resultList);
        } else {
            return result;
        }
        return result;
    }
}
