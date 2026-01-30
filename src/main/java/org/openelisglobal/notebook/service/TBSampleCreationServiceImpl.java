package org.openelisglobal.notebook.service;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.notebook.service.TBManifestImportService.TBManifestRow;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of TBSampleCreationService using REQUIRES_NEW propagation.
 * Each row is processed in its own transaction, allowing independent
 * success/failure.
 */
@Service
public class TBSampleCreationServiceImpl implements TBSampleCreationService {

    private static final Logger logger = LoggerFactory.getLogger(TBSampleCreationServiceImpl.class);

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private PersonService personService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private PatientIdentityService patientIdentityService;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RowCreationResult createSamplesForRow(Integer entryId, TBManifestRow row, String sysUserId) {
        List<SampleItem> createdSamples = new ArrayList<>();
        List<String> accessionNumbers = new ArrayList<>();

        try {
            // Validate specimen type before looking it up
            if (row.specimenType() == null || row.specimenType().isBlank()) {
                return new RowCreationResult(false, createdSamples, accessionNumbers, "Specimen type is required");
            }

            // Look up specimen type
            TypeOfSample searchType = new TypeOfSample();
            searchType.setDescription(row.specimenType());
            TypeOfSample specimenType = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);

            if (specimenType == null) {
                return new RowCreationResult(false, createdSamples, accessionNumbers,
                        "Unknown specimen type: " + row.specimenType());
            }

            // Get status ID for SampleEntered
            String sampleEnteredStatusId = statusService.getStatusID(SampleStatus.Entered);
            if (sampleEnteredStatusId == null || "-1".equals(sampleEnteredStatusId)) {
                sampleEnteredStatusId = "20";
            }

            // Create a parent Sample record
            Sample parentSample = new Sample();
            parentSample.setSysUserId(sysUserId);
            parentSample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
            parentSample.setReceivedTimestamp(new java.sql.Timestamp(System.currentTimeMillis()));

            // Use the proper accession number generator (atomic UPDATE...RETURNING)
            // instead of broken SELECT MAX() approach
            IAccessionNumberGenerator generator = AccessionNumberUtil.getMainAccessionNumberGenerator();
            if (generator == null) {
                return new RowCreationResult(false, createdSamples, accessionNumbers,
                        "Accession number generator not configured");
            }
            String accessionNumber = generator.getNextAvailableAccessionNumber(null, true);
            if (accessionNumber == null) {
                return new RowCreationResult(false, createdSamples, accessionNumbers,
                        "Failed to generate accession number");
            }
            parentSample.setAccessionNumber(accessionNumber);

            // Insert the sample
            String sampleIdDb = sampleService.insert(parentSample);
            parentSample.setId(sampleIdDb);

            logger.info("Created sample with accession number: {} for row {}", parentSample.getAccessionNumber(),
                    row.rowNumber());

            // Create or find Patient record and link to sample
            String patientIdDb = createOrFindPatient(row, sysUserId);
            if (patientIdDb != null) {
                // Create SampleHuman to link Sample to Patient
                SampleHuman sampleHuman = new SampleHuman();
                sampleHuman.setSampleId(sampleIdDb);
                sampleHuman.setPatientId(patientIdDb);
                sampleHuman.setLastupdated(DateUtil.getNowAsTimestamp());
                sampleHuman.setSysUserId(sysUserId);
                sampleHumanService.insert(sampleHuman);

                logger.info("Created SampleHuman link: sample {} -> patient {}", sampleIdDb, patientIdDb);
            }

            // Use sampleId from manifest or generate from patientId/row number
            String sampleIdForExternal = row.sampleId() != null ? row.sampleId()
                    : (row.patientId() != null ? row.patientId() : String.valueOf(row.rowNumber()));

            // Create individual SampleItem records
            for (int seq = 1; seq <= row.numOfSamples(); seq++) {
                SampleItem item = new SampleItem();
                item.setSample(parentSample);
                item.setTypeOfSample(specimenType);
                item.setExternalId(generateExternalId(sampleIdForExternal, seq));
                item.setSortOrder(Integer.toString(seq));
                item.setStatusId(sampleEnteredStatusId);
                item.setSysUserId(sysUserId);

                // Set received date from manifest row
                if (row.receivedDate() != null && !row.receivedDate().isBlank()) {
                    java.sql.Timestamp receivedTimestamp = parseDate(row.receivedDate(), row.receivedTime());
                    if (receivedTimestamp != null) {
                        item.setCollectionDate(receivedTimestamp);
                    }
                }

                String itemId = sampleItemService.insert(item);
                item.setId(itemId);
                createdSamples.add(item);
                accessionNumbers.add(parentSample.getAccessionNumber());

                // Build metadata map from manifest row for storage in NotebookPageSample.data
                java.util.Map<String, Object> manifestData = buildManifestData(row);

                // Add sample to entry with manifest metadata
                notebookEntryService.addSampleWithData(entryId, item, manifestData, sysUserId);
            }

            return new RowCreationResult(true, createdSamples, accessionNumbers, null);

        } catch (Exception e) {
            logger.error("Failed to create samples for row {}: {}", row.rowNumber(), e.getMessage(), e);
            return new RowCreationResult(false, createdSamples, accessionNumbers,
                    "Failed to create sample: " + e.getMessage());
        }
    }

    private String generateExternalId(String sampleId, int sequenceNumber) {
        return String.format("TB-%s-%03d", sampleId, sequenceNumber);
    }

    /**
     * Create or find a Patient record based on manifest row data. Uses patientId as
     * external identifier to find existing patients.
     *
     * @return Patient ID (database primary key), or null if no patient data
     *         available
     */
    private String createOrFindPatient(TBManifestRow row, String sysUserId) {
        // If no patient identifiable data, skip patient creation
        if ((row.patientId() == null || row.patientId().isBlank())
                && (row.patientName() == null || row.patientName().isBlank())) {
            return null;
        }

        // Try to find existing patient by external ID (patientId from manifest)
        Patient existingPatient = null;
        if (row.patientId() != null && !row.patientId().isBlank()) {
            existingPatient = patientService.getByExternalId(row.patientId());
        }

        if (existingPatient != null) {
            logger.info("Found existing patient with externalId: {}", row.patientId());
            return existingPatient.getId();
        }

        // Create new Person record
        Person person = new Person();
        if (row.patientName() != null && !row.patientName().isBlank()) {
            // Try to split name into first/last
            String[] nameParts = row.patientName().trim().split("\\s+", 2);
            if (nameParts.length >= 2) {
                person.setFirstName(nameParts[0]);
                person.setLastName(nameParts[1]);
            } else {
                person.setLastName(row.patientName().trim());
            }
        }
        person.setLastupdatedFields();
        person.setSysUserId(sysUserId);
        String personId = personService.insert(person);
        person.setId(personId);

        // Create new Patient record
        Patient patient = new Patient();
        patient.setPerson(person);
        if (row.patientId() != null && !row.patientId().isBlank()) {
            patient.setExternalId(row.patientId());
            patient.setNationalId(row.patientId());
        }
        if (row.patientSex() != null && !row.patientSex().isBlank()) {
            // Map common gender values
            String gender = row.patientSex().trim().toUpperCase();
            if (gender.startsWith("M")) {
                patient.setGender("M");
            } else if (gender.startsWith("F")) {
                patient.setGender("F");
            } else {
                patient.setGender(gender.substring(0, 1));
            }
        }
        patient.setSysUserId(sysUserId);
        String patientIdDb = patientService.insert(patient);
        patient.setId(patientIdDb);

        // Create patient identity if patientId provided
        if (row.patientId() != null && !row.patientId().isBlank()) {
            createPatientIdentity(patientIdDb, row.patientId(), sysUserId);
        }

        logger.info("Created new patient with id: {}, externalId: {}", patientIdDb, row.patientId());
        return patientIdDb;
    }

    /**
     * Create a PatientIdentity record to store the manifest patient ID as a SUBJECT
     * type identity.
     */
    private void createPatientIdentity(String patientId, String identityValue, String sysUserId) {
        try {
            String typeID = PatientIdentityTypeMap.getInstance().getIDForType("SUBJECT");
            if (typeID == null) {
                logger.warn("PatientIdentityType 'SUBJECT' not found, skipping identity creation");
                return;
            }

            PatientIdentity existingIdentity = patientIdentityService.getPatitentIdentityForPatientAndType(patientId,
                    typeID);
            if (existingIdentity == null) {
                PatientIdentity patientIdentity = new PatientIdentity();
                patientIdentity.setPatientId(patientId);
                patientIdentity.setIdentityData(identityValue);
                patientIdentity.setLastupdated(DateUtil.getNowAsTimestamp());
                patientIdentity.setIdentityTypeId(typeID);
                patientIdentityService.insert(patientIdentity);
            }
        } catch (Exception e) {
            logger.warn("Failed to create patient identity: {}", e.getMessage());
            // Non-fatal - patient was still created
        }
    }

    /**
     * Build a metadata map from the TB manifest row for storage in
     * NotebookPageSample.data.
     */
    private java.util.Map<String, Object> buildManifestData(TBManifestRow row) {
        java.util.Map<String, Object> data = new java.util.HashMap<>();

        // Store all TB-specific fields from the manifest
        if (row.specimenType() != null)
            data.put("specimenType", row.specimenType());
        if (row.specimenQuality() != null)
            data.put("specimenQuality", row.specimenQuality());
        if (row.documentNumber() != null)
            data.put("documentNumber", row.documentNumber());
        if (row.referringFacility() != null)
            data.put("referringFacility", row.referringFacility());
        if (row.patientName() != null)
            data.put("patientName", row.patientName());
        if (row.patientAge() != null)
            data.put("patientAge", row.patientAge());
        if (row.patientSex() != null)
            data.put("patientSex", row.patientSex());
        if (row.patientId() != null)
            data.put("patientId", row.patientId());
        if (row.studyId() != null)
            data.put("studyId", row.studyId());
        if (row.patientAddress() != null)
            data.put("patientAddress", row.patientAddress());
        if (row.patientPhone() != null)
            data.put("patientPhone", row.patientPhone());
        if (row.physicianPhone() != null)
            data.put("physicianPhone", row.physicianPhone());
        if (row.consentStatus() != null)
            data.put("consentStatus", row.consentStatus());
        if (row.treatmentHistory() != null)
            data.put("treatmentHistory", row.treatmentHistory());

        // Requested tests as a combined string
        java.util.List<String> tests = new java.util.ArrayList<>();
        if ("Yes".equalsIgnoreCase(row.culture()))
            tests.add("Culture");
        if ("Yes".equalsIgnoreCase(row.smearMicroscopy()))
            tests.add("Smear Microscopy");
        if ("Yes".equalsIgnoreCase(row.genexpert()))
            tests.add("GeneXpert");
        if ("Yes".equalsIgnoreCase(row.identification()))
            tests.add("Identification");
        if ("Yes".equalsIgnoreCase(row.dstFirstLine()))
            tests.add("DST First Line");
        if ("Yes".equalsIgnoreCase(row.dstSecondLine()))
            tests.add("DST Second Line");
        if (!tests.isEmpty()) {
            data.put("requestedTests", String.join(", ", tests));
        }

        if (row.intendedMethod() != null)
            data.put("intendedMethod", row.intendedMethod());
        if (row.receivedSite() != null)
            data.put("receivedSite", row.receivedSite());
        if (row.receivedDate() != null)
            data.put("receivedDate", row.receivedDate());
        if (row.receivedTime() != null)
            data.put("receivedTime", row.receivedTime());

        return data;
    }

    /**
     * Parse date and optional time from various formats.
     */
    private java.sql.Timestamp parseDate(String dateStr, String timeStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }

        String trimmedDate = dateStr.trim();
        String trimmedTime = timeStr != null ? timeStr.trim() : "";

        // Combine date and time if both present
        String combined = trimmedTime.isBlank() ? trimmedDate : trimmedDate + " " + trimmedTime;

        // Try datetime formats first
        String[] dateTimeFormats = { "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm:ss", "dd/MM/yyyy HH:mm",
                "MM/dd/yyyy HH:mm" };

        for (String format : dateTimeFormats) {
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(format);
                java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(combined, formatter);
                return java.sql.Timestamp.valueOf(dateTime);
            } catch (java.time.format.DateTimeParseException e) {
                // Try next format
            }
        }

        // Try date-only formats
        String[] dateFormats = { "yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd-MM-yyyy", "yyyy/MM/dd" };

        for (String format : dateFormats) {
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(format);
                java.time.LocalDate date = java.time.LocalDate.parse(trimmedDate, formatter);
                return java.sql.Timestamp.valueOf(date.atStartOfDay());
            } catch (java.time.format.DateTimeParseException e) {
                // Try next format
            }
        }

        return null;
    }
}
