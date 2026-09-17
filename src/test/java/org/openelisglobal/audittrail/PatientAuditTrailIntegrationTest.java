package org.openelisglobal.audittrail;

import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
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
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;

public class PatientAuditTrailIntegrationTest extends AuditTrailIntegrationTestSupport {

    @Autowired
    private PersonService personService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private PatientIdentityService patientIdentityService;

    @Autowired
    private PatientIdentityTypeService patientIdentityTypeService;

    @Autowired
    private SiteInformationService siteInformationService;

    @Autowired
    private HistoryService historyService;

    private String personRefTableId;
    private String patientRefTableId;
    private String patientIdentityRefTableId;
    private String siteInfoRefTableId;

    @Before
    public void setUp() {
        personRefTableId = requiredReferenceTable("PERSON");
        patientRefTableId = requiredReferenceTable("PATIENT");
        patientIdentityRefTableId = requiredReferenceTable("PATIENT_IDENTITY");
        siteInfoRefTableId = requiredReferenceTable("site_information");
    }

    private boolean hasUpdateRowWithChanges(String referenceTableId, String referenceId, String expectedOldValue) {
        List<History> rows = historyService.getHistoryByRefIdAndRefTableId(referenceId, referenceTableId);
        for (History h : rows) {
            if ("U".equals(h.getActivity()) && h.getChanges() != null && h.getChanges().length > 0) {
                String xml = new String(h.getChanges());
                if (xml.contains(expectedOldValue)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    public void personUpdate_emitsUpdateHistoryRowWithChangedFields() {
        Person person = new Person();
        person.setFirstName("AuditTest");
        person.setLastName("EmitCheck");
        person.setPrimaryPhone("+261 37 11 111 11");
        person.setSysUserId("1");
        personService.insert(person);
        String personId = person.getId();

        Person reloaded = personService.get(personId);
        detachSavedRecords();
        reloaded.setPrimaryPhone("+261 38 22 222 22");
        reloaded.setSysUserId("1");
        personService.update(reloaded);

        assertTrue("PERSON UPDATE history row must exist with the old primaryPhone in changes XML",
                hasUpdateRowWithChanges(personRefTableId, personId, "+261 37 11 111 11"));
    }

    @Test
    public void patientUpdate_emitsUpdateHistoryRowWithChangedFields() {
        Person person = new Person();
        person.setFirstName("AuditPatient");
        person.setLastName("EmitCheckPat");
        person.setSysUserId("1");
        personService.insert(person);

        Patient patient = new Patient();
        patient.setPerson(person);
        patient.setNationalId("NID-INITIAL");
        patient.setSysUserId("1");
        patientService.insert(patient);
        String patientId = patient.getId();

        Patient reloaded = patientService.get(patientId);
        detachSavedRecords();
        reloaded.setNationalId("NID-UPDATED");
        reloaded.setSysUserId("1");
        patientService.update(reloaded);

        assertTrue("PATIENT UPDATE history row must exist with the old nationalId in changes XML",
                hasUpdateRowWithChanges(patientRefTableId, patientId, "NID-INITIAL"));
    }

    @Test
    public void patientIdentityUpdate_emitsUpdateHistoryRowWithChangedFields() {
        Person person = new Person();
        person.setFirstName("AuditIdent");
        person.setLastName("EmitCheckIdent");
        person.setSysUserId("1");
        personService.insert(person);

        Patient patient = new Patient();
        patient.setPerson(person);
        patient.setSysUserId("1");
        patientService.insert(patient);

        List<PatientIdentityType> identityTypes = patientIdentityTypeService.getAll();
        assertTrue("At least one PatientIdentityType must exist in seed data", !identityTypes.isEmpty());
        PatientIdentityType identityType = identityTypes.get(0);

        PatientIdentity identity = new PatientIdentity();
        identity.setPatientId(patient.getId());
        identity.setIdentityTypeId(identityType.getId());
        identity.setIdentityData("ORIGINAL-IDENTITY-DATA");
        identity.setSysUserId("1");
        patientIdentityService.insert(identity);
        String identityId = identity.getId();

        PatientIdentity reloaded = patientIdentityService.get(identityId);
        detachSavedRecords();
        reloaded.setIdentityData("UPDATED-IDENTITY-DATA");
        reloaded.setSysUserId("1");
        patientIdentityService.update(reloaded);

        assertTrue("PATIENT_IDENTITY UPDATE history row must exist with the old identityData in changes XML",
                hasUpdateRowWithChanges(patientIdentityRefTableId, identityId, "ORIGINAL-IDENTITY-DATA"));
    }

    @Test
    public void siteInformationUpdate_stillEmitsAfterFix() {
        SiteInformation si = new SiteInformation();
        si.setName("audit-test-owned-setting");
        si.setValue("original-setting");
        si.setValueType("text");
        si.setSysUserId(TEST_SYS_USER_ID);
        siteInformationService.insert(si);
        detachSavedRecords();
        String originalValue = si.getValue();
        si.setValue(originalValue == null ? "audit-regression" : originalValue + "-touched");
        si.setSysUserId("1");
        siteInformationService.update(si);

        assertTrue("SiteInformation UPDATE history must still emit after the patient-flow fix",
                hasUpdateRowWithChanges(siteInfoRefTableId, si.getId(), originalValue == null ? "" : originalValue));
    }
}
