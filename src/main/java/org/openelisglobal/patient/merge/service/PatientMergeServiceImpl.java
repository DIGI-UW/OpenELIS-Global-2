package org.openelisglobal.patient.merge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.patient.dao.PatientDAO;
import org.openelisglobal.patient.merge.dao.PatientMergeAuditDAO;
import org.openelisglobal.patient.merge.dto.PatientMergeDataSummaryDTO;
import org.openelisglobal.patient.merge.dto.PatientMergeDetailsDTO;
import org.openelisglobal.patient.merge.dto.PatientMergeExecutionResultDTO;
import org.openelisglobal.patient.merge.dto.PatientMergeRequestDTO;
import org.openelisglobal.patient.merge.dto.PatientMergeValidationResultDTO;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.patientidentitytype.service.PatientIdentityTypeService;
import org.openelisglobal.patientidentitytype.valueholder.PatientIdentityType;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of PatientMergeService. Handles patient merge validation,
 * execution, and FHIR synchronization.
 */
@Service
@Transactional
public class PatientMergeServiceImpl implements PatientMergeService {

    @Autowired
    private PatientDAO patientDAO;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private PatientMergeAuditDAO patientMergeAuditDAO;

    @Autowired
    private FhirPatientLinkService fhirPatientLinkService;

    @Autowired
    private PatientMergeConsolidationService consolidationService;

    @Autowired
    private PatientIdentityTypeService patientIdentityTypeService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private IStatusService iStatusService;

    @Autowired
    private FhirPersistanceService fhirPersistanceService;

    @Value("${org.openelisglobal.fhir.integration.enabled:false}")
    private boolean fhirIntegrationEnabled;

    /**
     * Validates if two patients can be merged. Checks: same patient, patient not
     * found, already merged, circular references.
     */
    @Override
    @Transactional(readOnly = true)
    public PatientMergeValidationResultDTO validateMerge(PatientMergeRequestDTO request) {
        PatientMergeValidationResultDTO result = new PatientMergeValidationResultDTO();

        // Validation 1: Check if same patient ID
        if (request.getPatient1Id().equals(request.getPatient2Id())) {
            result.addError("Cannot merge same patient with itself");
            return result;
        }

        // Validation 2: Fetch both patients
        Patient patient1 = patientDAO.getData(request.getPatient1Id());
        Patient patient2 = patientDAO.getData(request.getPatient2Id());

        if (patient1 == null) {
            result.addError("Patient 1 not found: " + request.getPatient1Id());
            return result;
        }

        if (patient2 == null) {
            result.addError("Patient 2 not found: " + request.getPatient2Id());
            return result;
        }

        // Validation 3: Check if either patient is already merged
        if (Boolean.TRUE.equals(patient1.getIsMerged())) {
            result.addError("Patient 1 is already merged into patient " + patient1.getMergedIntoPatientId());
            return result;
        }

        if (Boolean.TRUE.equals(patient2.getIsMerged())) {
            result.addError("Patient 2 is already merged into patient " + patient2.getMergedIntoPatientId());
            return result;
        }

        // Validation 4: Check for circular references
        if (patient1.getMergedIntoPatientId() != null
                && patient1.getMergedIntoPatientId().equals(request.getPatient2Id())) {
            result.addError("Circular merge reference detected");
            return result;
        }

        if (patient2.getMergedIntoPatientId() != null
                && patient2.getMergedIntoPatientId().equals(request.getPatient1Id())) {
            result.addError("Circular merge reference detected");
            return result;
        }

        // All validations passed - create data summary
        result.setValid(true);
        result.setDataSummary(createDataSummary(patient1, patient2));

        return result;
    }

    /**
     * Creates data summary for two patients to be merged. Queries actual counts
     * from related tables for frontend preview.
     */
    private org.openelisglobal.patient.merge.dto.PatientMergeDataSummaryDTO createDataSummary(Patient patient1,
            Patient patient2) {
        org.openelisglobal.patient.merge.dto.PatientMergeDataSummaryDTO summary = new org.openelisglobal.patient.merge.dto.PatientMergeDataSummaryDTO();

        long samplesP1 = countSamplesForPatient(patient1.getId());
        long samplesP2 = countSamplesForPatient(patient2.getId());
        summary.setTotalSamples((int) (samplesP1 + samplesP2));

        long ordersP1 = countOrdersForPatient(patient1.getId());
        long ordersP2 = countOrdersForPatient(patient2.getId());
        summary.setTotalOrders((int) (ordersP1 + ordersP2));
        summary.setActiveOrders((int) (ordersP1 + ordersP2));

        long contactsP1 = countContactsForPatient(patient1.getId());
        long contactsP2 = countContactsForPatient(patient2.getId());
        summary.setTotalContacts((int) (contactsP1 + contactsP2));

        long identitiesP1 = countIdentitiesForPatient(patient1.getId());
        long identitiesP2 = countIdentitiesForPatient(patient2.getId());
        summary.setTotalIdentifiers((int) (identitiesP1 + identitiesP2));

        long relationsP1 = countRelationsForPatient(patient1.getId());
        long relationsP2 = countRelationsForPatient(patient2.getId());
        summary.setTotalRelations((int) (relationsP1 + relationsP2));

        summary.setTotalResults(countResultsForPatient(patient1.getId()) + countResultsForPatient(patient2.getId()));
        summary.setTotalDocuments(0);

        return summary;
    }

    private int countResultsForPatient(String patientId) {
        Set<Integer> statusIdList = new HashSet<>();
        statusIdList.add(Integer.parseInt(iStatusService.getStatusID(AnalysisStatus.Canceled)));
        statusIdList.add(Integer.parseInt(iStatusService.getStatusID(AnalysisStatus.SampleRejected)));
        statusIdList.add(Integer.parseInt(iStatusService.getStatusID(AnalysisStatus.NotStarted)));
        List<Analysis> allAnalyses = new ArrayList<>();
        List<Sample> samples = sampleHumanService.getSamplesForPatient(patientId);
        for (Sample sample : samples) {
            List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
            for (SampleItem item : sampleItems) {
                List<Analysis> analysisList = analysisService.getAnalysesBySampleItemsExcludingByStatusIds(item,
                        statusIdList);
                allAnalyses.addAll(analysisList);
            }
        }
        return allAnalyses.size();
    }

    private long countElectronicOrdersForPatient(String patientId) {
        return ((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM electronic_order WHERE patient_id = :patientId")
                .setParameter("patientId", Long.parseLong(patientId)).getSingleResult()).longValue();
    }

    private long countSamplesForPatient(String patientId) {
        List<SampleItem> allsampleItems = new ArrayList<>();
        List<Sample> samples = sampleHumanService.getSamplesForPatient(patientId);
        for (Sample sample : samples) {
            List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
            allsampleItems.addAll(sampleItems);
        }
        return (long) allsampleItems.size();
    }

    private long countOrdersForPatient(String patientId) {
        return ((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM sample_human WHERE patient_id = :patientId")
                .setParameter("patientId", Long.parseLong(patientId)).getSingleResult()).longValue();
    }

    private long countContactsForPatient(String patientId) {
        return ((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM patient_contact WHERE patient_id = :patientId")
                .setParameter("patientId", Long.parseLong(patientId)).getSingleResult()).longValue();
    }

    private long countIdentitiesForPatient(String patientId) {
        return ((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM patient_identity WHERE patient_id = :patientId")
                .setParameter("patientId", Long.parseLong(patientId)).getSingleResult()).longValue();
    }

    private long countRelationsForPatient(String patientId) {
        return ((Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM patient_relations WHERE pat_id = :patientId OR pat_id_source = :patientId")
                .setParameter("patientId", Long.parseLong(patientId)).getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    private List<PatientIdentity> getPatientIdentities(String patientId) {
        return entityManager.createNativeQuery("SELECT * FROM patient_identity WHERE patient_id = :patientId",
                PatientIdentity.class).setParameter("patientId", Long.parseLong(patientId)).getResultList();
    }

    /**
     * Retrieves detailed information about a patient for merge preview. Includes
     * demographics, data summary, and identifiers.
     */
    @Override
    @Transactional(readOnly = true)
    public PatientMergeDetailsDTO getMergeDetails(String patientId) {
        Patient patient = patientDAO.getData(patientId);
        if (patient == null) {
            return null;
        }

        PatientMergeDetailsDTO details = new PatientMergeDetailsDTO();
        details.setPatientId(patient.getId());
        details.setFirstName(patient.getPerson().getFirstName());
        details.setLastName(patient.getPerson().getLastName());
        details.setGender(patient.getGender());
        details.setBirthDate(patient.getBirthDateForDisplay());

        List<String> internalIdentityTypes = List.of("GUID", "AKA", "MOTHER", "MOTHERS_INITIAL");
        List<PatientIdentity> identities = getPatientIdentities(patient.getId());
        for (PatientIdentity identity : identities) {
            String identityTypeName = identity.getIdentityTypeId();
            PatientIdentityType identityType = patientIdentityTypeService.get(identity.getIdentityTypeId());
            if (identityType != null) {
                identityTypeName = identityType.getIdentityType();
            }
            if (internalIdentityTypes.contains(identityTypeName)) {
                continue;
            }
            PatientMergeDetailsDTO.IdentifierDTO identifierDTO = new PatientMergeDetailsDTO.IdentifierDTO();
            identifierDTO.setIdentityType(getDisplayNameForIdentityType(identityTypeName));
            identifierDTO.setIdentityValue(identity.getIdentityData());
            details.getIdentifiers().add(identifierDTO);
        }

        PatientMergeDataSummaryDTO dataSummary = new PatientMergeDataSummaryDTO();
        dataSummary.setTotalSamples((int) countSamplesForPatient(patient.getId()));
        dataSummary.setTotalOrders((int) countOrdersForPatient(patient.getId()));
        dataSummary.setActiveOrders((int) countOrdersForPatient(patient.getId()));
        dataSummary.setTotalResults(countResultsForPatient(patient.getId()));
        dataSummary.setTotalIdentifiers(identities.size());
        details.setDataSummary(dataSummary);

        return details;
    }

    /**
     * Executes the patient merge operation. Consolidates all data, marks merged
     * patient, and creates audit trail. Runs in single transaction with rollback on
     * failure.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PatientMergeExecutionResultDTO executeMerge(PatientMergeRequestDTO request, String sysUserId) {
        long startTime = System.currentTimeMillis();

        if (!Boolean.TRUE.equals(request.getConfirmed())) {
            return PatientMergeExecutionResultDTO.failure("Merge operation must be confirmed");
        }

        Patient patient1 = patientDAO.getData(request.getPatient1Id());
        Patient patient2 = patientDAO.getData(request.getPatient2Id());

        if (patient1 == null || patient2 == null) {
            return PatientMergeExecutionResultDTO.failure("Patient not found");
        }

        Patient primaryPatient = request.getPrimaryPatientId().equals(patient1.getId()) ? patient1 : patient2;
        Patient mergedPatient = request.getPrimaryPatientId().equals(patient1.getId()) ? patient2 : patient1;

        if (fhirIntegrationEnabled) {
            validateFhirConsistency(primaryPatient, mergedPatient);
        }

        mergedPatient.setIsMerged(true);
        mergedPatient.setMergedIntoPatientId(primaryPatient.getId());
        mergedPatient.setMergeDate(new java.sql.Timestamp(System.currentTimeMillis()));
        patientDAO.update(mergedPatient);

        org.openelisglobal.patient.merge.valueholder.PatientMergeAudit audit = new org.openelisglobal.patient.merge.valueholder.PatientMergeAudit();
        audit.setPrimaryPatientId(Long.parseLong(primaryPatient.getId()));
        audit.setMergedPatientId(Long.parseLong(mergedPatient.getId()));
        audit.setMergeDate(new java.sql.Timestamp(System.currentTimeMillis()));
        audit.setReason(request.getReason());
        audit.setPerformedByUserId(Long.parseLong(sysUserId));
        audit.setSysUserId(sysUserId);

        PatientMergeConsolidationService.ConsolidationResult consolidationResult = consolidationService
                .consolidateClinicalData(primaryPatient.getId(), mergedPatient.getId(), sysUserId);
        LogEvent.logInfo(this.getClass().getName(), "executeMerge",
                "Data consolidation complete. Total records reassigned: " + consolidationResult.getTotalReassigned());

        java.util.List<String> mergedDemoFields = consolidationService.mergeDemographics(primaryPatient, mergedPatient);
        if (!mergedDemoFields.isEmpty()) {
            org.openelisglobal.person.service.PersonService personService = org.openelisglobal.spring.util.SpringContext
                    .getBean(org.openelisglobal.person.service.PersonService.class);
            primaryPatient.getPerson().setSysUserId(sysUserId);
            personService.update(primaryPatient.getPerson());
            LogEvent.logInfo(this.getClass().getName(), "executeMerge",
                    "Merged " + mergedDemoFields.size() + " demographic fields from patient " + mergedPatient.getId());
        }

        if (fhirIntegrationEnabled && fhirPatientLinkService.hasFhirResource(primaryPatient.getId())
                && fhirPatientLinkService.hasFhirResource(mergedPatient.getId())) {
            try {
                fhirPatientLinkService.updatePatientLinks(primaryPatient.getId(), mergedPatient.getId(),
                        primaryPatient.getFhirUuidAsString(), mergedPatient.getFhirUuidAsString());
                LogEvent.logInfo(this.getClass().getName(), "executeMerge",
                        "Successfully updated FHIR Patient links for merge: " + primaryPatient.getId() + " <- "
                                + mergedPatient.getId());
            } catch (FhirLocalPersistingException e) {
                throw new IllegalStateException("FHIR link update failed during patient merge", e);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        JsonNode dataSummary = createAuditDataSummary(consolidationResult, mergedDemoFields.size(), duration);
        audit.setDataSummary(dataSummary);
        Long auditId = patientMergeAuditDAO.insert(audit);

        return PatientMergeExecutionResultDTO.success(String.valueOf(auditId), primaryPatient.getId(),
                mergedPatient.getId(), duration);
    }

    /**
     * Validates FHIR resource consistency between two patients.
     */
    private void validateFhirConsistency(Patient primaryPatient, Patient mergedPatient) {
        String primaryUuid = primaryPatient.getFhirUuidAsString();
        String mergedUuid = mergedPatient.getFhirUuidAsString();

        boolean primaryHasFhir = hasActualFhirResource(primaryUuid);
        boolean mergedHasFhir = hasActualFhirResource(mergedUuid);

        if (primaryHasFhir && mergedHasFhir) {
            return;
        }

        if (!primaryHasFhir && !mergedHasFhir) {
            return;
        }

        String patientRoleWithFhir = primaryHasFhir ? "primary" : "merged";
        String fhirUuidWithFhir = primaryHasFhir ? primaryUuid : mergedUuid;
        String message = "Cannot merge patients: only " + patientRoleWithFhir + " patient has a FHIR resource (UUID: "
                + fhirUuidWithFhir
                + "). Both patients must have FHIR resources or neither should have one to maintain data consistency.";
        throw new IllegalStateException(message);
    }

    /**
     * Checks whether a FHIR Patient resource exists on the configured FHIR server.
     * Returns false if the UUID is null/blank, FHIR integration is disabled, or if
     * any error occurs while checking.
     */
    private boolean hasActualFhirResource(String fhirUuid) {
        if (fhirUuid == null || fhirUuid.trim().isEmpty()) {
            return false;
        }

        if (!fhirIntegrationEnabled) {
            return false;
        }

        try {
            var fhirPatient = fhirPersistanceService.getPatientByUuid(fhirUuid);
            return fhirPatient.isPresent();
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "hasActualFhirResource",
                    "Error checking FHIR resource for UUID " + fhirUuid + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Creates JSONB data summary for audit trail.
     */
    private JsonNode createAuditDataSummary(PatientMergeConsolidationService.ConsolidationResult consolidation,
            int mergedDemoFieldsCount, long durationMs) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode summary = mapper.createObjectNode();

        summary.put("samples_reassigned", consolidation.getSamplesReassigned());
        summary.put("contacts_reassigned", consolidation.getContactsReassigned());
        summary.put("orders_reassigned", consolidation.getOrdersReassigned());
        summary.put("relations_reassigned", consolidation.getRelationsReassigned());
        summary.put("total_records_reassigned", consolidation.getTotalReassigned());
        summary.put("demographic_fields_merged", mergedDemoFieldsCount);
        summary.put("merge_duration_ms", durationMs);

        return summary;
    }

    /**
     * Maps internal identity type codes to user-friendly display names.
     */
    private String getDisplayNameForIdentityType(String identityType) {
        if (identityType == null) {
            return "Unknown";
        }
        switch (identityType.toUpperCase()) {
        case "SUBJECT":
            return "Subject Number";
        case "NATIONAL":
            return "National ID";
        case "ST":
            return "ST Number";
        case "INSURANCE":
            return "Insurance ID";
        case "OCCUPATION":
            return "Occupation";
        case "ORG_SITE":
            return "Organization Site";
        case "EDUCATION":
            return "Education";
        case "MARITIAL":
            return "Marital Status";
        case "NATIONALITY":
            return "Nationality";
        case "OTHER NATIONALITY":
            return "Other Nationality";
        case "HEALTH DISTRICT":
            return "Health District";
        case "HEALTH REGION":
            return "Health Region";
        case "OB_NUMBER":
            return "OB Number";
        case "PC_NUMBER":
            return "PC Number";
        default:
            return identityType;
        }
    }
}