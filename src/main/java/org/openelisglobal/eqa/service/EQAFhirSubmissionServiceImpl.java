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
import org.hl7.fhir.r4.model.StringType;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.eqa.dao.EQACycleDAO;
import org.openelisglobal.eqa.dao.EQADistributionDAO;
import org.openelisglobal.eqa.dao.EQAParticipantResultDAO;
import org.openelisglobal.eqa.dao.EQAResultDAO;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQADistribution;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAResult;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
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
    private EQACycleDAO cycleDAO;

    @Autowired
    private EQAParticipantResultDAO participantResultDAO;

    @Autowired
    private FhirPersistanceService fhirPersistanceService;

    @Autowired
    private OrganizationService organizationService;

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
    public boolean submitCycleViaFhir(Long cycleId, Long labEnrollmentId) {
        EQACycle cycle = cycleDAO.get(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + cycleId));

        List<EQAParticipantResult> results = submittableResults(cycleId, labEnrollmentId);
        if (results.isEmpty()) {
            throw new IllegalArgumentException(
                    "No submittable result for cycle " + cycleId + " and enrollment " + labEnrollmentId);
        }

        Map<String, Resource> fhirResources = new HashMap<>();
        DiagnosticReport report = buildCycleReport(cycle, labEnrollmentId, results);
        fhirResources.put(report.getId(), report);
        for (EQAParticipantResult result : results) {
            Observation observation = buildParticipantObservation(result);
            fhirResources.put(observation.getId(), observation);
        }

        try {
            Bundle responseBundle = fhirPersistanceService.createFhirResourcesInFhirStore(fhirResources);
            LogEvent.logInfo(this.getClass().getSimpleName(), "submitCycleViaFhir",
                    "EQA cycle submission successful: cycle=" + cycleId + ", enrollment=" + labEnrollmentId
                            + ", resources=" + fhirResources.size() + ", bundle=" + responseBundle.getId());
            return true;
        } catch (FhirLocalPersistingException | RuntimeException e) {
            // Any transport or serialization failure is a failed attempt, not a
            // crash: the caller counts it and retries under FR-V2.2-05 backoff.
            LogEvent.logError(this.getClass().getSimpleName(), "submitCycleViaFhir",
                    "EQA cycle submission failed: cycle=" + cycleId + ", enrollment=" + labEnrollmentId + ": "
                            + e.getMessage());
            return false;
        }
    }

    /**
     * Everything this lab has validated but not yet had scored. SUBMITTED rows are
     * included so a retry after a partial failure resends the whole set rather than
     * a fragment the provider cannot reconcile.
     */
    private List<EQAParticipantResult> submittableResults(Long cycleId, Long labEnrollmentId) {
        return participantResultDAO.getAllMatching(Map.of("cycle.id", cycleId, "labEnrollmentId", labEnrollmentId))
                .stream().filter(r -> r.getSubmissionStatus() == EQASubmissionStatus.VALIDATED_PARTIAL
                        || r.getSubmissionStatus() == EQASubmissionStatus.SUBMITTED)
                .collect(Collectors.toList());
    }

    /**
     * No subject reference: a participant self-enrollment
     * (eqa_lab_program_enrollment) records a free-text provider and no
     * organization, so an Organization/{id} reference would point at nothing. The
     * cycle and enrollment identifiers carry the routing instead.
     */
    private DiagnosticReport buildCycleReport(EQACycle cycle, Long labEnrollmentId,
            List<EQAParticipantResult> results) {
        DiagnosticReport report = new DiagnosticReport();
        String reportId = cycle.getFhirUuid().toString() + "-enrollment-" + labEnrollmentId;
        report.setId(reportId);
        report.addIdentifier(
                createIdentifier(fhirConfig.getOeFhirSystem() + EQA_SYSTEM + "/diagnostic_report", reportId));
        report.addIdentifier(
                createIdentifier(fhirConfig.getOeFhirSystem() + EQA_SYSTEM + "/cycle_id", cycle.getId().toString()));
        report.addIdentifier(createIdentifier(fhirConfig.getOeFhirSystem() + EQA_SYSTEM + "/lab_enrollment_id",
                labEnrollmentId.toString()));
        report.setStatus(DiagnosticReportStatus.FINAL);

        CodeableConcept category = new CodeableConcept();
        category.addCoding(new Coding("http://terminology.hl7.org/CodeSystem/v2-0074", "LAB", "Laboratory"));
        report.addCategory(category);

        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding(fhirConfig.getOeFhirSystem() + EQA_SYSTEM, "eqa-proficiency-test",
                "EQA Proficiency Testing Results"));
        code.setText("EQA Results: " + (cycle.getScheme() == null ? "" : cycle.getScheme().getName() + " ") + "cycle "
                + cycle.getCycleNumber());
        report.setCode(code);

        for (EQAParticipantResult result : results) {
            Reference obsRef = new Reference();
            obsRef.setReference(ResourceType.Observation + "/" + result.getFhirUuid().toString());
            report.addResult(obsRef);
        }
        return report;
    }

    /**
     * A participant result value is free text on the wire: external PT covers
     * qualitative analytes (HIV serology, blood-film ID) that have no numeric form,
     * so it is sent as a Quantity only when it parses as one.
     */
    private Observation buildParticipantObservation(EQAParticipantResult result) {
        Observation observation = new Observation();
        observation.setId(result.getFhirUuid().toString());
        observation
                .addIdentifier(createIdentifier(fhirConfig.getOeFhirSystem() + EQA_SYSTEM + "/participant_result_uuid",
                        result.getFhirUuid().toString()));
        observation.setStatus(ObservationStatus.FINAL);

        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding(fhirConfig.getOeFhirSystem() + EQA_SYSTEM + "/analyte",
                String.valueOf(result.getAnalyteId()), "EQA analyte " + result.getAnalyteId()));
        observation.setCode(code);

        String value = result.getResultValue();
        if (value != null && !value.isBlank()) {
            try {
                Quantity quantity = new Quantity();
                quantity.setValue(new java.math.BigDecimal(value.trim()));
                if (result.getResultUnit() != null && !result.getResultUnit().isBlank()) {
                    quantity.setUnit(result.getResultUnit());
                }
                observation.setValue(quantity);
            } catch (NumberFormatException e) {
                observation.setValue(new StringType(value.trim()));
            }
        }
        return observation;
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

        // Same rule as the observation below: an Organization is the report's
        // performer, and DiagnosticReport.subject would refuse it.
        Reference reportPerformer = organizationReference(organizationId);
        if (reportPerformer != null) {
            report.addPerformer(reportPerformer);
        }

        for (EQAResult result : results) {
            Reference obsRef = new Reference();
            obsRef.setReference(ResourceType.Observation + "/" + result.getFhirUuid().toString());
            report.addResult(obsRef);
        }

        return report;
    }

    /**
     * The participating laboratory, referenced by its FHIR uuid. Referencing the
     * OpenELIS numeric id makes the store try to auto-create a placeholder
     * Organization under a numeric client-assigned id, which it refuses
     * (HAPI-0960); an organization with no uuid yet is left unreferenced rather
     * than sent as a broken link.
     */
    private Reference organizationReference(Long organizationId) {
        Organization participant = organizationService.getOrganizationById(String.valueOf(organizationId));
        if (participant == null || participant.getFhirUuid() == null) {
            return null;
        }
        Reference reference = new Reference();
        reference.setReference(ResourceType.Organization + "/" + participant.getFhirUuid());
        return reference;
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

        // The participating laboratory is the performer, not the subject: FHIR R4
        // restricts Observation.subject to Patient|Group|Device|Location, so a store
        // refuses an Organization there with HTTP 422 (found returning scores over
        // FHIR on the dev stack, 2026-08-24 — this path had no live caller before).
        Reference performer = organizationReference(result.getParticipantOrganizationId());
        if (performer != null) {
            observation.addPerformer(performer);
        }

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
