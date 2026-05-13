package org.openelisglobal.fhir.serviceImpl;

import java.util.List;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportStatus;
import org.hl7.fhir.r4.model.ResourceType;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.dataexchange.fhir.FHIRTransformUtil;
import org.openelisglobal.fhir.service.DiagnosticReportTransformService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DiagnosticReportTransformServiceImpl implements DiagnosticReportTransformService {
    @Autowired
    private FHIRTransformUtil fhirTransformUtil;

    @Autowired
    private ResultService resultService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Override
    public DiagnosticReport transformResultToDiagnosticReport(Analysis analysis) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformResultToDiagnosticReport",
                "transformResultToDiagnosticReport called");

        List<Result> allResults = resultService.getResultsByAnalysis(analysis);
        SampleItem sampleItem = analysis.getSampleItem();
        Patient patient = sampleHumanService.getPatientForSample(sampleItem.getSample());

        DiagnosticReport diagnosticReport = fhirTransformUtil.genNewDiagnosticReport(analysis);
        Test test = analysis.getTest();

        if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.Finalized))) {
            diagnosticReport.setStatus(DiagnosticReportStatus.FINAL);
        } else if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance))) {
            diagnosticReport.setStatus(DiagnosticReportStatus.PRELIMINARY);
        } else if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.TechnicalRejected))) {
            diagnosticReport.setStatus(DiagnosticReportStatus.PARTIAL);
        } else if (analysis.getStatusId().equals(statusService.getStatusID(AnalysisStatus.NotStarted))) {
            diagnosticReport.setStatus(DiagnosticReportStatus.REGISTERED);
        } else {
            diagnosticReport.setStatus(DiagnosticReportStatus.UNKNOWN);
        }

        diagnosticReport.addBasedOn(
                fhirTransformUtil.createReferenceFor(ResourceType.ServiceRequest, analysis.getFhirUuidAsString()));
        diagnosticReport.addSpecimen(
                fhirTransformUtil.createReferenceFor(ResourceType.Specimen, sampleItem.getFhirUuidAsString()));
        // OGC-356: Environmental samples don't have a patient
        if (patient != null) {
            diagnosticReport.setSubject(
                    fhirTransformUtil.createReferenceFor(ResourceType.Patient, patient.getFhirUuidAsString()));
        }
        for (Result curResult : allResults) {
            diagnosticReport.addResult(
                    fhirTransformUtil.createReferenceFor(ResourceType.Observation, curResult.getFhirUuidAsString()));
        }
        diagnosticReport.setCode(fhirTransformUtil.transformTestToCodeableConcept(test.getId()));

        return diagnosticReport;
    }

}
