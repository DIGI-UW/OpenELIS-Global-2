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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of PatientMergeService. Handles patient merge validation,
 * execution, and FHIR synchronization.
 *
 * TDD Phase: GREEN - Implement minimal code to make tests pass.
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
        // If patient1 was previously merged into patient2, this would create circular
        // reference
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

        // Count samples for both patients
        long samplesP1 = countSamplesForPatient(patient1.getId());
        long samplesP2 = countSamplesForPatient(patient2.getId());
        summary.setTotalSamples((int) (samplesP1 + samplesP2));

        // Count electronic orders for both patients
        long ordersP1 = countOrdersForPatient(patient1.getId());
        long ordersP2 = countOrdersForPatient(patient2.getId());
        summary.setTotalOrders((int) (ordersP1 + ordersP2));

        // Active orders: Set to 0 for now. To implement:
        // - Add WHERE status IN ('PENDING', 'IN_PROGRESS', ...) to
        // countOrdersForPatient
        // - Requires ElectronicOrder status enum/constants knowledge
        summary.setActiveOrders((int) (ordersP1 + ordersP2));

        // Count patient contacts for both patients
        long contactsP1 = countContactsForPatient(patient1.getId());
        long contactsP2 = countContactsForPatient(patient2.getId());
        summary.setTotalContacts((int) (contactsP1 + contactsP2));

        // Count patient identities for both patients
        long identitiesP1 = countIdentitiesForPatient(patient1.getId());
        long identitiesP2 = countIdentitiesForPatient(patient2.getId());
        summary.setTotalIdentifiers((int) (identitiesP1 + identitiesP2));

        // Count patient relations for both patients
        long relationsP1 = countRelationsForPatient(patient1.getId());
        long relationsP2 = countRelationsForPatient(patient2.getId());
        summary.setTotalRelations((int) (relationsP1 + relationsP2));

        // Total results: Set to 0 for now. To implement:
        // - Query result/analysis tables joined through sample_human
        // - Requires understanding Result/Analysis entity relationships
        summary.setTotalResults(countResultsForPatient(patient1.getId()) + countResultsForPatient(patient2.getId()));

        // Total documents: Set to 0 for now. To implement:
        // - Query patient_document or document_track table (if exists)
        // - Requires understanding document storage model
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
        // Native SQL needed for ElectronicOrder due to ValueHolder pattern
        // Schema name omitted - uses Hibernate's default_schema configuration
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
        // Native SQL needed due to String ID vs numeric column type mismatch
        // Schema name omitted - uses Hibernate's default_schema configuration
        return ((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM patient_contact WHERE patient_id = :patientId")
                .setParameter("patientId", Long.parseLong(patientId)).getSingleResult()).longValue();
    }

    private long countIdentitiesForPatient(String patientId) {
        // Native SQL needed due to String ID vs numeric column type mismatch
        // Schema name omitted - uses Hibernate's default_schema configuration
        return ((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM patient_identity WHERE patient_id = :patientId")
                .setParameter("patientId", Long.parseLong(patientId)).getSingleResult()).longValue();
    }

    private long countRelationsForPatient(String patientId) {
        // Native SQL needed due to String ID vs numeric column type mismatch
        // Schema name omitted - uses Hibernate's default_schema configuration
        return ((Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM patient_relations WHERE pat_id = :patientId OR pat_id_source = :patientId")
                .setParameter("patientId", Long.parseLong(patientId)).getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    private List<PatientIdentity> getPatientIdentities(String patientId) {
        // Use native SQL to avoid type mismatch between String ID and numeric column
        // Schema name omitted - uses Hibernate's default_schema configuration
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

        // Note: This method works for both merged and non-merged patients.
        // It simply returns current patient details regardless of merge history.
        // The merge workflow uses this to show details of BOTH patients before merging.
        PatientMergeDetailsDTO details = new PatientMergeDetailsDTO();
        details.setPatientId(patient.getId());
        details.setFirstName(patient.getPerson().getFirstName());
        details.setLastName(patient.getPerson().getLastName());
        details.setGender(patient.getGender());
        details.setBirthDate(patient.getBirthDateForDisplay());

        // Populate actual identifiers from patient_identity table
        // Filter out internal system identifiers that shouldn't be displayed to users
        List<String> internalIdentityTypes = List.of("GUID", "AKA", "MOTHER", "MOTHERS_INITIAL");
        List<PatientIdentity> identities = getPatientIdentities(patient.getId());
        for (PatientIdentity identity : identities) {
            // Lookup the identity type name from the ID
            String identityTypeName = identity.getIdentityTypeId();
            PatientIdentityType identityType = patientIdentityTypeService.get(identity.getIdentityTypeId());
            if (identityType != null) {
                identityTypeName = identityType.getIdentityType();
            }
            // Skip internal identity types
            if (internalIdentityTypes.contains(identityTypeName)) {
                continue;
            }
            PatientMergeDetailsDTO.IdentifierDTO identifierDTO = new PatientMergeDetailsDTO.IdentifierDTO();
            // Map short type names to user-friendly display names
            identifierDTO.setIdentityType(getDisplayNameForIdentityType(identityTypeName));
            identifierDTO.setIdentityValue(identity.getIdentityData());
            details.getIdentifiers().add(identifierDTO);
        }

        // Populate data summary
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
    public PatientMergeExecutionResultDTO executeMerge(PatientMergeRequestDTO request, String sysUserId) {
        long startTime = System.currentTimeMillis();

        // Validation 1: Check confirmation
        if (!Boolean.TRUE.equals(request.getConfirmed())) {
            return PatientMergeExecutionResultDTO.failure("Merge operation must be confirmed");
        }

        // Validation 2: Fetch both patients
        Patient patient1 = patientDAO.getData(request.getPatient1Id());
        Patient patient2 = patientDAO.getData(request.getPatient2Id());

        if (patient1 == null || patient2 == null) {
            return PatientMergeExecutionResultDTO.failure("Patient not found");
        }

        // Determine primary and merged patients
        Patient primaryPatient = request.getPrimaryPatientId().equals(patient1.getId()) ? patient1 : patient2;
        Patient mergedPatient = request.getPrimaryPatientId().equals(patient1.getId()) ? patient2 : patient1;

        // Mark merged patient as inactive
        mergedPatient.setIsMerged(true);
        mergedPatient.setMergedIntoPatientId(primaryPatient.getId());
        mergedPatient.setMergeDate(new java.sql.Timestamp(System.currentTimeMillis()));

        // Update merged patient in database
        patientDAO.update(mergedPatient);

        // Create audit entry
        org.openelisglobal.patient.merge.valueholder.PatientMergeAudit audit = new org.openelisglobal.patient.merge.valueholder.PatientMergeAudit();
        audit.setPrimaryPatientId(Long.parseLong(primaryPatient.getId()));
        audit.setMergedPatientId(Long.parseLong(mergedPatient.getId()));
        audit.setMergeDate(new java.sql.Timestamp(System.currentTimeMillis()));
        audit.setReason(request.getReason());

        // Use the sysUserId passed from the controller (from session/security context)
        // Set both performedByUserId and sysUserId to the current user for audit trail
        audit.setPerformedByUserId(Long.parseLong(sysUserId));
        audit.setSysUserId(sysUserId);

        // FR-004: Consolidate clinical data (update FKs to point to primary patient)
        // sysUserId is used for audit trail in consolidation operations
        PatientMergeConsolidationService.ConsolidationResult consolidationResult = consolidationService
                .consolidateClinicalData(primaryPatient.getId(), mergedPatient.getId(), sysUserId);
        LogEvent.logInfo(this.getClass().getName(), "executeMerge",
                "Data consolidation complete. Total records reassigned: " + consolidationResult.getTotalReassigned());

        // FR-009: Merge demographics (fill gaps in primary with merged patient data)
        java.util.List<String> mergedDemoFields = consolidationService.mergeDemographics(primaryPatient, mergedPatient);
        if (!mergedDemoFields.isEmpty()) {
            // Update primary patient's person record with merged demographics
            // Yes, this updates the person record with merged demographics AND sets
            // sysUserId for audit.
            // mergeDemographics() modified the Person object in-memory (filled gaps),
            // now we persist those changes to database via personService.update()
            org.openelisglobal.person.service.PersonService personService = org.openelisglobal.spring.util.SpringContext
                    .getBean(org.openelisglobal.person.service.PersonService.class);
            // setSysUserId is required for audit trail (tracks who made the change)
            // This is standard pattern in OpenELIS for all entity updates
            primaryPatient.getPerson().setSysUserId(sysUserId);
            personService.update(primaryPatient.getPerson());
            LogEvent.logInfo(this.getClass().getName(), "executeMerge",
                    "Merged " + mergedDemoFields.size() + " demographic fields from patient " + mergedPatient.getId());
        }

        // FR-016, FR-017: Update FHIR Patient links if both patients have FHIR
        // resources
        if (fhirPatientLinkService.hasFhirResource(primaryPatient.getId())
                && fhirPatientLinkService.hasFhirResource(mergedPatient.getId())) {
            try {
                fhirPatientLinkService.updatePatientLinks(primaryPatient.getId(), mergedPatient.getId(),
                        primaryPatient.getFhirUuidAsString(), mergedPatient.getFhirUuidAsString());
                LogEvent.logInfo(this.getClass().getName(), "executeMerge",
                        "Successfully updated FHIR Patient links for merge: " + primaryPatient.getId() + " <- "
                                + mergedPatient.getId());
            } catch (FhirLocalPersistingException e) {
                // Log error but don't fail the entire merge if FHIR update fails
                LogEvent.logError(this.getClass().getName(), "executeMerge",
                        "FHIR link update failed but merge succeeded: " + e.getMessage());
            }
        }

        // Create data_summary JSONB for audit trail
        long duration = System.currentTimeMillis() - startTime;
        JsonNode dataSummary = createAuditDataSummary(consolidationResult, mergedDemoFields.size(), duration);
        audit.setDataSummary(dataSummary);

        Long auditId = patientMergeAuditDAO.insert(audit);

        return PatientMergeExecutionResultDTO.success(String.valueOf(auditId), primaryPatient.getId(),
                mergedPatient.getId(), duration);
    }

    /**
     * Creates JSONB data summary for audit trail. Captures comprehensive merge
     * operation statistics.
     */
    private JsonNode createAuditDataSummary(PatientMergeConsolidationService.ConsolidationResult consolidation,
            int mergedDemoFieldsCount, long durationMs) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode summary = mapper.createObjectNode();

        // Clinical data counts
        summary.put("samples_reassigned", consolidation.getSamplesReassigned());
        summary.put("contacts_reassigned", consolidation.getContactsReassigned());
        summary.put("orders_reassigned", consolidation.getOrdersReassigned());
        summary.put("relations_reassigned", consolidation.getRelationsReassigned());
        summary.put("total_records_reassigned", consolidation.getTotalReassigned());

        // Demographics merge count
        summary.put("demographic_fields_merged", mergedDemoFieldsCount);

        // Performance metrics
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
