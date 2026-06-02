package org.openelisglobal.audittrail;

import static org.junit.Assert.assertTrue;

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
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

public class PatientAuditTrailIntegrationTest extends BaseWebContextSensitiveTest {

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

    @Autowired
    private ReferenceTablesService referenceTablesService;

    private String personRefTableId;
    private String patientRefTableId;
    private String patientIdentityRefTableId;
    private String siteInfoRefTableId;

    @Before
    public void setUp() throws Exception {
        AuditTrailServiceImpl realAuditTrailService = new AuditTrailServiceImpl();
        ReflectionTestUtils.setField(realAuditTrailService, "referenceTablesService", referenceTablesService);
        ReflectionTestUtils.setField(realAuditTrailService, "historyService", historyService);

        for (Object service : new Object[] { personService, patientService, patientIdentityService,
                siteInformationService }) {
            Object target = AopTestUtils.getUltimateTargetObject(service);
            ReflectionTestUtils.setField(target, "auditTrailService", realAuditTrailService);
        }

        executeDataSetWithStateManagement("testdata/patient.xml");
        cleanRowsInCurrentConnection(new String[] { "patient_identity", "patient", "person", "history" });

        personRefTableId = ensureReferenceTable("PERSON");
        patientRefTableId = ensureReferenceTable("PATIENT");
        patientIdentityRefTableId = ensureReferenceTable("PATIENT_IDENTITY");
        siteInfoRefTableId = ensureReferenceTable("site_information");

        // This class audits updates to seed-provided rows but does not own those seeds;
        // a sibling fixture's TRUNCATE ... CASCADE can wipe them. Ensure the audit user
        // and a site_information row exist regardless of test order.
        ensureAuditSystemUser();
        ensureSiteInformationPresent();
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
        reloaded.setIdentityData("UPDATED-IDENTITY-DATA");
        reloaded.setSysUserId("1");
        patientIdentityService.update(reloaded);

        assertTrue("PATIENT_IDENTITY UPDATE history row must exist with the old identityData in changes XML",
                hasUpdateRowWithChanges(patientIdentityRefTableId, identityId, "ORIGINAL-IDENTITY-DATA"));
    }

    @Test
    public void siteInformationUpdate_stillEmitsAfterFix() {
        List<SiteInformation> all = siteInformationService.getAllSiteInformation();
        assertTrue("At least one SiteInformation row must exist for regression coverage", !all.isEmpty());
        SiteInformation si = all.get(0);
        String originalValue = si.getValue();
        si.setValue(originalValue == null ? "audit-regression" : originalValue + "-touched");
        si.setSysUserId("1");
        siteInformationService.update(si);

        assertTrue("SiteInformation UPDATE history must still emit after the patient-flow fix",
                hasUpdateRowWithChanges(siteInfoRefTableId, si.getId(), originalValue == null ? "" : originalValue));
    }
}
