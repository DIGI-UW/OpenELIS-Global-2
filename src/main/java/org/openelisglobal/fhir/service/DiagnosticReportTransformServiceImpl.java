package org.openelisglobal.fhir.service;

import java.util.List;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportStatus;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ResourceType;
import org.openelisglobal.analysis.service.AnalysisAnchorService;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DiagnosticReportTransformServiceImpl implements DiagnosticReportTransformService {

    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private AnalysisAnchorService analysisAnchorService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private FhirCommonTransformService common;
    @Autowired
    private TerminologyTransformService terminologyTransformService;

    @Override
    public DiagnosticReport transformResultToDiagnosticReport(String analysisId) {
        return transformResultToDiagnosticReport(analysisService.get(analysisId));
    }

    @Override
    public DiagnosticReport transformResultToDiagnosticReport(Analysis analysis) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformResultToDiagnosticReport",
                "transformResultToDiagnosticReport called");

        List<Result> allResults = resultService.getResultsByAnalysis(analysis);
        SampleItem sampleItem = analysis.getSampleItem();
        Sample sampleForAnalysis = analysisAnchorService.resolveSample(analysis);
        Patient patient = sampleForAnalysis != null ? sampleHumanService.getPatientForSample(sampleForAnalysis) : null;

        DiagnosticReport diagnosticReport = genNewDiagnosticReport(analysis);
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

        diagnosticReport
                .addBasedOn(common.createReferenceFor(ResourceType.ServiceRequest, analysis.getFhirUuidAsString()));
        diagnosticReport
                .addSpecimen(common.createReferenceFor(ResourceType.Specimen, sampleItem.getFhirUuidAsString()));
        // OGC-356: Environmental samples don't have a patient
        if (patient != null) {
            diagnosticReport.setSubject(common.createReferenceFor(ResourceType.Patient, patient.getFhirUuidAsString()));
        }
        for (Result curResult : allResults) {
            diagnosticReport
                    .addResult(common.createReferenceFor(ResourceType.Observation, curResult.getFhirUuidAsString()));
        }
        diagnosticReport.setCode(terminologyTransformService.transformTestToCodeableConcept(test.getId(),
                sampleItem.getTypeOfSampleId()));

        return diagnosticReport;
    }

    private DiagnosticReport genNewDiagnosticReport(Analysis analysis) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "genNewDiagnosticReport", "genNewDiagnosticReport called");

        DiagnosticReport diagnosticReport = new DiagnosticReport();
        diagnosticReport.setId(analysis.getFhirUuidAsString());
        diagnosticReport.getMeta().setLastUpdated(analysis.getLastupdated());
        diagnosticReport.addIdentifier(common.createIdentifier(fhirConfig.getOeFhirSystem() + "/analysisResult_uuid",
                analysis.getFhirUuidAsString()));
        Identifier facilityId = common.createFacilityIdentifier();
        if (facilityId != null) {
            diagnosticReport.addIdentifier(facilityId);
        }
        return diagnosticReport;
    }
}
