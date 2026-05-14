package org.openelisglobal.audittrail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.audittrail.daoimpl.AuditTrailServiceImpl;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.patientidentitytype.service.PatientIdentityTypeService;
import org.openelisglobal.patientidentitytype.valueholder.PatientIdentityType;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * UAT Round 2 (LO-01-02 / LO-01-05): "The audit trail only shows updated date
 * of birth. Does not capture audit trail for any other updated information."
 *
 * The patient-edit flow updates multiple fields in a single save: - Person row:
 * firstName, lastName, gender, birthDate, email, primaryPhone, streetAddress,
 * city. - Patient row: nationalId, externalId, subjectNumber. - PatientIdentity
 * rows: marital status, education, nationality, etc.
 *
 * Each service path is audited individually (the singular tests in
 * PatientAuditTrailIntegrationTest lock that). This file locks the multi- field
 * promise: a single update changing several Person fields must produce exactly
 * one PERSON history row whose XML contains the OLD value of each changed field
 * — not just one of them.
 */
public class PatientMultiFieldAuditTrailIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PersonService personService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private PatientIdentityService patientIdentityService;

    @Autowired
    private PatientIdentityTypeService patientIdentityTypeService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    private String personRefTableId;
    private String patientRefTableId;
    private String patientIdentityRefTableId;

    @Before
    public void setUp() throws Exception {
        AuditTrailServiceImpl realAuditTrailService = new AuditTrailServiceImpl();
        ReflectionTestUtils.setField(realAuditTrailService, "referenceTablesService", referenceTablesService);
        ReflectionTestUtils.setField(realAuditTrailService, "historyService", historyService);

        for (Object service : new Object[] { personService, patientService, patientIdentityService }) {
            Object target = AopTestUtils.getUltimateTargetObject(service);
            ReflectionTestUtils.setField(target, "auditTrailService", realAuditTrailService);
        }

        executeDataSetWithStateManagement("testdata/patient.xml");
        cleanRowsInCurrentConnection(new String[] { "patient_identity", "patient", "person", "history" });

        personRefTableId = ensureReferenceTable("PERSON");
        patientRefTableId = ensureReferenceTable("PATIENT");
        patientIdentityRefTableId = ensureReferenceTable("PATIENT_IDENTITY");
    }

    private List<String> changesXmlForUpdateRows(String referenceTableId, String referenceId) {
        List<History> rows = historyService.getHistoryByRefIdAndRefTableId(referenceId, referenceTableId);
        List<String> xmls = new ArrayList<>();
        for (History h : rows) {
            if ("U".equals(h.getActivity()) && h.getChanges() != null && h.getChanges().length > 0) {
                xmls.add(new String(h.getChanges()));
            }
        }
        return xmls;
    }

    /**
     * Person carries firstName, lastName, primaryPhone, email, streetAddress, city.
     * (gender + birthDate live on Patient.) UAT regression: multi-field Person
     * update must produce one PERSON history row whose XML contains the old value
     * of every changed field — not just one of them.
     */
    @Test
    public void personUpdate_capturesEveryChangedField() {
        Person person = new Person();
        person.setFirstName("OriginalFirst");
        person.setLastName("OriginalLast");
        person.setPrimaryPhone("+261 37 11 111 11");
        person.setEmail("original@example.org");
        person.setStreetAddress("OriginalStreet");
        person.setCity("OriginalCity");
        person.setSysUserId("1");
        personService.insert(person);
        String personId = person.getId();

        Person reloaded = personService.get(personId);
        reloaded.setFirstName("UpdatedFirst");
        reloaded.setLastName("UpdatedLast");
        reloaded.setPrimaryPhone("+261 38 22 222 22");
        reloaded.setEmail("updated@example.org");
        reloaded.setStreetAddress("UpdatedStreet");
        reloaded.setCity("UpdatedCity");
        reloaded.setSysUserId("1");
        personService.update(reloaded);

        List<String> xmls = changesXmlForUpdateRows(personRefTableId, personId);
        assertEquals("Exactly one PERSON UPDATE history row expected per save", 1, xmls.size());
        String xml = xmls.get(0);

        // Each old value must appear — the UAT bug was that only one field
        // (DOB on Patient) showed up.
        assertTrue("audit XML must contain old firstName 'OriginalFirst'\nxml: " + xml, xml.contains("OriginalFirst"));
        assertTrue("audit XML must contain old lastName 'OriginalLast'\nxml: " + xml, xml.contains("OriginalLast"));
        assertTrue("audit XML must contain old primaryPhone '+261 37 11 111 11'\nxml: " + xml,
                xml.contains("+261 37 11 111 11"));
        assertTrue("audit XML must contain old email 'original@example.org'\nxml: " + xml,
                xml.contains("original@example.org"));
        assertTrue("audit XML must contain old streetAddress 'OriginalStreet'\nxml: " + xml,
                xml.contains("OriginalStreet"));
        assertTrue("audit XML must contain old city 'OriginalCity'\nxml: " + xml, xml.contains("OriginalCity"));
    }

    /**
     * Patient carries gender, birthDate, nationalId. UAT regression: editing gender
     * + nationalId alongside birthDate must capture every change, not just
     * birthDate.
     */
    @Test
    public void patientUpdate_capturesEveryChangedField() {
        Person person = new Person();
        person.setFirstName("PatientMulti");
        person.setSysUserId("1");
        personService.insert(person);

        Patient patient = new Patient();
        patient.setPerson(person);
        patient.setGender("M");
        patient.setBirthDate(Timestamp.valueOf(LocalDate.of(1990, 1, 15).atStartOfDay()));
        patient.setNationalId("NID-ORIGINAL");
        patient.setSysUserId("1");
        patientService.insert(patient);
        String patientId = patient.getId();

        Patient reloaded = patientService.get(patientId);
        reloaded.setGender("F");
        reloaded.setBirthDate(Timestamp.valueOf(LocalDate.of(1985, 6, 20).atStartOfDay()));
        reloaded.setNationalId("NID-UPDATED");
        reloaded.setSysUserId("1");
        patientService.update(reloaded);

        List<String> xmls = changesXmlForUpdateRows(patientRefTableId, patientId);
        assertEquals("Exactly one PATIENT UPDATE history row expected per save", 1, xmls.size());
        String xml = xmls.get(0);

        assertTrue("audit XML must contain old nationalId 'NID-ORIGINAL'\nxml: " + xml, xml.contains("NID-ORIGINAL"));
        assertTrue("audit XML must reference the birthDate change (1990)\nxml: " + xml, xml.contains("1990"));
        assertTrue("audit XML must reference the gender change\nxml: " + xml, xml.toLowerCase().contains("gender"));
    }

    /**
     * Multi-row identity updates (marital, education, nationality, etc.) — each
     * PatientIdentity row should produce its own U history row when its value
     * changes. The patient-edit flow saves these via persistIdentityType which goes
     * through patientIdentityService.update.
     */
    @Test
    public void multiplePatientIdentityUpdates_eachEmitsItsOwnHistoryRow() {
        // Need 3 distinct PatientIdentityTypes to seed 3 identity rows.
        List<PatientIdentityType> identityTypes = patientIdentityTypeService.getAll();
        assertTrue("Seed data must provide at least 3 PatientIdentityType rows", identityTypes.size() >= 3);

        Person person = new Person();
        person.setFirstName("MultiIdent");
        person.setLastName("Patient");
        person.setSysUserId("1");
        personService.insert(person);

        Patient patient = new Patient();
        patient.setPerson(person);
        patient.setSysUserId("1");
        patientService.insert(patient);

        // Seed three identity rows with known initial values.
        String[] ids = new String[3];
        String[] originals = new String[] { "INIT_MARITAL", "INIT_EDU", "INIT_NATION" };
        for (int i = 0; i < 3; i++) {
            PatientIdentity identity = new PatientIdentity();
            identity.setPatientId(patient.getId());
            identity.setIdentityTypeId(identityTypes.get(i).getId());
            identity.setIdentityData(originals[i]);
            identity.setSysUserId("1");
            patientIdentityService.insert(identity);
            ids[i] = identity.getId();
        }

        // Update each.
        for (int i = 0; i < 3; i++) {
            PatientIdentity reloaded = patientIdentityService.get(ids[i]);
            reloaded.setIdentityData("UPDATED_" + i);
            reloaded.setSysUserId("1");
            patientIdentityService.update(reloaded);
        }

        // Each row must have at least one U history row whose XML contains
        // its original value. Missing rows would mean per-identity changes
        // (marital status, education, nationality) don't show in audit.
        for (int i = 0; i < 3; i++) {
            final String original = originals[i];
            final int rowIndex = i;
            List<String> xmls = changesXmlForUpdateRows(patientIdentityRefTableId, ids[i]);
            assertFalse("PATIENT_IDENTITY row index " + rowIndex + " (original '" + original
                    + "') must produce a U history row — UAT bug was that only one identity field "
                    + "appeared in the audit", xmls.isEmpty());
            boolean foundOriginal = xmls.stream().anyMatch(xml -> xml.contains(original));
            assertTrue("PATIENT_IDENTITY row " + rowIndex + " history XML must contain old value '" + original + "'",
                    foundOriginal);
        }
    }
}
