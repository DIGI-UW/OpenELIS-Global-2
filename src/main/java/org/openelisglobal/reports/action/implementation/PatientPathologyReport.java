package org.openelisglobal.reports.action.implementation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.program.service.PathologyDisplayService;
import org.openelisglobal.program.util.DictionaryEntryNames;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion.ConclusionType;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;

public class PatientPathologyReport extends PatientProgramReport {
    protected PathologyDisplayService pathologyDisplayService = SpringContext.getBean(PathologyDisplayService.class);

    private PathologySample pathologySample;

    @Override
    protected String getReportName() {
        return "PatientPathologyReport";
    }

    @Override
    protected void setAdditionalReportItems() {
        data.setGrossExam(pathologySample.getGrossExam());
        data.setMicroExam(pathologySample.getMicroscopyExam());
        pathologySample.getConclusions().size();
        Optional<PathologyConclusion> conclusion = pathologySample.getConclusions().stream()
                .filter(e -> e.getType() == ConclusionType.TEXT).findFirst();
        data.setTextConclusion(conclusion.isPresent() ? conclusion.get().getValue() : "");

        List<String> codedConclusions = DictionaryEntryNames
                .namedPairs(dictionaryService,
                        pathologySample.getConclusions().stream().filter(e -> e.getType() == ConclusionType.DICTIONARY)
                                .map(PathologyConclusion::getValue))
                .stream().map(IdValuePair::getValue).collect(Collectors.toList());
        data.setCodedConclusion(codedConclusions);
    }

    @Override
    protected void innitializeSample(ReportForm form) {
        pathologySample = pathologyDisplayService
                .getPathologySampleWithLoadedAtttributes(Integer.valueOf(form.getProgramSampleId()));
        sample = pathologySample.getSample();
    }
}
