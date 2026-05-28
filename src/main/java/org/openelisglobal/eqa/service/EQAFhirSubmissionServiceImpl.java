package org.openelisglobal.eqa.service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportStatus;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.eqa.dao.EQADistributionDAO;
import org.openelisglobal.eqa.dao.EQAResultDAO;
import org.openelisglobal.eqa.valueholder.EQADistribution;
import org.openelisglobal.eqa.valueholder.EQAResult;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EQAFhirSubmissionServiceImpl implements EQAFhirSubmissionService {

    private static final String EQA_SYSTEM = "/eqa";

    @Autowired
    private EQADistributionDAO distributionDAO;

    @Autowired
    private EQAResultDAO resultDAO;

    @Autowired
    private FhirPersistanceService fhirPersistanceService;

    @Autowired
    private FhirConfig fhirConfig;

    @Autowired
    private SystemUserService systemUserService;

    @Override
    public Map<String, Object> submitResultsViaFhir(Long distributionId, Long organizationId) {
        EQADistribution distribution = distributionDAO.get(distributionId)
                .orElseThrow(() -> new IllegalArgumentException("Distribution not found: " + distributionId));

        List<EQAResult> results = resultDAO.findByDistributionId(distributionId).stream()
                .filter(r -> r.getParticipantOrganizationId().equals(organizationId)).collect(Collectors.toList());

        if (results.isEmpty()) {
            throw new IllegalArgumentException(
                    "No results found for organization " + organizationId + " in distribution " + distributionId);
        }

        Map<String, Resource> fhirResources = new HashMap<>();

        DiagnosticReport report = buildDiagnosticReport(distribution, organizationId, results);
        fhirResources.put(report.getId(), report);

        for (EQAResult result : results) {
            Observation observation = buildObservation(result, distribution);
            fhirResources.put(observation.getId(), observation);
        }

        Map<String, Object> response = new HashMap<>();
        try {
            Bundle responseBundle = fhirPersistanceService.createFhirResourcesInFhirStore(fhirResources);
            response.put("success", true);
            response.put("bundleId", responseBundle.getId());
            response.put("resourceCount", fhirResources.size());
            response.put("distributionId", distributionId);
            response.put("organizationId", organizationId);

            LogEvent.logInfo(this.getClass().getSimpleName(), "submitResultsViaFhir",
                    "EQA FHIR submission successful: distribution=" + distributionId + ", org=" + organizationId
                            + ", resources=" + fhirResources.size());
        } catch (FhirLocalPersistingException e) {
            LogEvent.logError(e);
            response.put("success", false);
            response.put("error", "FHIR submission failed: " + e.getMessage());
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSubmissionLate(Long distributionId) {
        EQADistribution distribution = distributionDAO.get(distributionId)
                .orElseThrow(() -> new IllegalArgumentException("Distribution not found: " + distributionId));

        if (distribution.getDeadline() == null) {
            return false;
        }

        return new Timestamp(System.currentTimeMillis()).after(distribution.getDeadline());
    }

    @Override
    public Map<String, Object> approveLateSubmission(Long distributionId, Long organizationId, String justification,
            String supervisorUserId) {
        distributionDAO.get(distributionId)
                .orElseThrow(() -> new IllegalArgumentException("Distribution not found: " + distributionId));

        if (!isSubmissionLate(distributionId)) {
            throw new IllegalStateException("Distribution is not past deadline; late approval not needed");
        }

        SystemUser supervisor = systemUserService.get(supervisorUserId);

        List<EQAResult> results = resultDAO.findByDistributionId(distributionId).stream()
                .filter(r -> r.getParticipantOrganizationId().equals(organizationId)).collect(Collectors.toList());

        for (EQAResult result : results) {
            result.setIsLateSubmission(true);
            result.setLateSubmissionJustification(justification);
            result.setApprovedBy(supervisor);
            resultDAO.update(result);
        }

        Map<String, Object> fhirResult = submitResultsViaFhir(distributionId, organizationId);

        Map<String, Object> response = new HashMap<>();
        response.put("approved", true);
        response.put("distributionId", distributionId);
        response.put("organizationId", organizationId);
        response.put("approvedBy", supervisorUserId);
        response.put("justification", justification);
        response.put("fhirSubmission", fhirResult);

        LogEvent.logInfo(this.getClass().getSimpleName(), "approveLateSubmission",
                "Late submission approved: distribution=" + distributionId + ", org=" + organizationId + ", supervisor="
                        + supervisorUserId);

        return response;
    }

    private DiagnosticReport buildDiagnosticReport(EQADistribution distribution, Long organizationId,
            List<EQAResult> results) {
        DiagnosticReport report = new DiagnosticReport();

        String reportId = distribution.getFhirUuid().toString() + "-org-" + organizationId;
        report.setId(reportId);

        report.addIdentifier(
                createIdentifier(fhirConfig.getOeFhirSystem() + EQA_SYSTEM + "/diagnostic_report", reportId));

        report.addIdentifier(createIdentifier(fhirConfig.getOeFhirSystem() + EQA_SYSTEM + "/distribution_id",
                distribution.getId().toString()));

        report.setStatus(DiagnosticReportStatus.FINAL);

        CodeableConcept category = new CodeableConcept();
        category.addCoding(new Coding("http://terminology.hl7.org/CodeSystem/v2-0074", "LAB", "Laboratory"));
        report.addCategory(category);

        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding(fhirConfig.getOeFhirSystem() + EQA_SYSTEM, "eqa-proficiency-test",
                "EQA Proficiency Testing Results"));
        code.setText("EQA Results: " + distribution.getDistributionName());
        report.setCode(code);

        Reference orgRef = new Reference();
        orgRef.setReference(ResourceType.Organization + "/" + organizationId);
        report.setSubject(orgRef);

        for (EQAResult result : results) {
            Reference obsRef = new Reference();
            obsRef.setReference(ResourceType.Observation + "/" + result.getFhirUuid().toString());
            report.addResult(obsRef);
        }

        return report;
    }

    private Observation buildObservation(EQAResult result, EQADistribution distribution) {
        Observation observation = new Observation();

        observation.setId(result.getFhirUuid().toString());

        observation.addIdentifier(createIdentifier(fhirConfig.getOeFhirSystem() + EQA_SYSTEM + "/eqa_result_uuid",
                result.getFhirUuid().toString()));

        observation.setStatus(ObservationStatus.FINAL);

        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding(fhirConfig.getOeFhirSystem() + EQA_SYSTEM + "/test", result.getTestId().toString(),
                "EQA Test " + result.getTestId()));
        observation.setCode(code);

        if (result.getResultValue() != null) {
            Quantity quantity = new Quantity();
            quantity.setValue(result.getResultValue());
            observation.setValue(quantity);
        }

        Reference orgRef = new Reference();
        orgRef.setReference(ResourceType.Organization + "/" + result.getParticipantOrganizationId());
        observation.setSubject(orgRef);

        if (result.getZScore() != null) {
            Observation.ObservationComponentComponent zScoreComponent = new Observation.ObservationComponentComponent();
            CodeableConcept zScoreCode = new CodeableConcept();
            zScoreCode.addCoding(new Coding(fhirConfig.getOeFhirSystem() + EQA_SYSTEM, "z-score", "Z-Score"));
            zScoreComponent.setCode(zScoreCode);
            Quantity zScoreValue = new Quantity();
            zScoreValue.setValue(result.getZScore());
            zScoreComponent.setValue(zScoreValue);
            observation.addComponent(zScoreComponent);
        }

        if (result.getPerformanceStatus() != null) {
            CodeableConcept interpretation = new CodeableConcept();
            interpretation.addCoding(new Coding(fhirConfig.getOeFhirSystem() + EQA_SYSTEM + "/performance",
                    result.getPerformanceStatus().name(), result.getPerformanceStatus().name()));
            observation.addInterpretation(interpretation);
        }

        return observation;
    }

    private Identifier createIdentifier(String system, String value) {
        Identifier identifier = new Identifier();
        identifier.setSystem(system);
        identifier.setValue(value);
        identifier.setUse(Identifier.IdentifierUse.USUAL);
        return identifier;
    }
}
