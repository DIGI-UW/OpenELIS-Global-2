package org.openelisglobal.reports.action.implementation;

import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.program.service.ImmunohistochemistrySampleService;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;

public class DualInSituHybridizationReport extends PatientProgramReport {

    private ImmunohistochemistrySampleService immunohistochemistrySampleService = SpringContext
            .getBean(ImmunohistochemistrySampleService.class);

    private ImmunohistochemistrySample immunohistochemistrySample;

    @Override
    protected String getReportName() {
        return "DualInSituHybridizationReport";
    }

    @Override
    protected void setAdditionalReportItems() {
        String reportDetails = MessageUtil.getMessage("dualInSitu.report.details", new String[] {
                form.getNumberOfcancerNuclei(), form.getAverageHer2(), form.getAverageChrom(), form.getIhcRatio() });
        data.setIhcReportDetails(reportDetails);
        if (form.getIhcScore() != null) {
            if (form.getIhcScore().equals("AMPLIFICATION")) {
                data.setIhcScore(MessageUtil.getMessage("dualInSitu.label.amp"));
            } else {
                data.setIhcScore(MessageUtil.getMessage("dualInSitu.label.noAmp"));
            }
        }
    }

    @Override
    protected void innitializeSample(ReportForm form) {
        immunohistochemistrySample = immunohistochemistrySampleService.get(Integer.valueOf(form.getProgramSampleId()));
        sample = immunohistochemistrySample.getSample();
    }
}
