package org.openelisglobal.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.action.IPatientUpdate.PatientUpdateStatus;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.service.PatientManagementUpdate;
import org.openelisglobal.sample.service.SamplePatientEntryService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

public class SamplePatientEntryServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SamplePatientEntryService samplePatientEntryService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private PersonService personService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private DataSource dataSource;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/samplepatiententry.xml");
        // Reset singleton caches to avoid stale identity type IDs from previous tests
        org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap.reset();
        // Ensure the address_part rows PatientManagementUpdate resolves at
        // @PostConstruct time exist, regardless of what other test classes'
        // fixtures have done to this shared table.
        ensureAddressPart("department");
        ensureAddressPart("commune");
        ensureAddressPart("village");
    }

    private void ensureAddressPart(String partName) throws Exception {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement select = conn
                        .prepareStatement("SELECT id FROM clinlims.address_part WHERE part_name = ?")) {
            select.setString(1, partName);
            try (ResultSet rs = select.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (Connection conn = dataSource.getConnection();
                PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO clinlims.address_part (id, part_name) VALUES (nextval('clinlims.address_part_seq'), ?)")) {
            insert.setString(1, partName);
            insert.executeUpdate();
        }
    }

    private PatientManagementUpdate newPatientManagementUpdate() {
        return webApplicationContext.getBean(PatientManagementUpdate.class);
    }

    @Test
    public void verifyTestData() {
        Person provider = personService.get("4");
        assertNotNull("Provider person should exist in test data", provider);

        Organization org = organizationService.get("1");
        assertNotNull("Organization should exist in test data", org);
    }

    @Test
    public void persistData_shouldHandleInvalidSample() {
        Sample invalidSample = new Sample();
        invalidSample.setAccessionNumber("INVALID123");

        SamplePatientUpdateData updateData = new SamplePatientUpdateData("1");
        updateData.setSample(invalidSample);

        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setPatientPK("testPatientId");

        SamplePatientEntryForm form = new SamplePatientEntryForm();
        form.setPatientProperties(patientInfo);

        MockHttpServletRequest request = new MockHttpServletRequest();

        try {
            samplePatientEntryService.persistData(updateData, new PatientManagementUpdate(), patientInfo, form,
                    request);
            fail("Expected exception was not thrown");
        } catch (Exception e) {
            assertNotNull("Exception should be thrown for invalid sample", e.getMessage());
        }
    }

    @Test
    public void persistData_shouldHandleMissingPatientId() throws Exception {
        Sample sample = sampleService.getSampleByAccessionNumber("TEST001");
        assertNotNull("Sample should exist", sample);

        SampleHuman sampleHuman = new SampleHuman();
        sampleHuman.setId("1");
        sampleHuman.setSampleId(String.valueOf(sample.getId()));
        sampleHumanService.getData(sampleHuman);

        SamplePatientUpdateData updateData = new SamplePatientUpdateData("1");
        updateData.setSample(sample);
        updateData.setSampleHuman(sampleHuman);

        PatientManagementInfo patientInfo = new PatientManagementInfo(); // No ID set to simulate missing ID
        SamplePatientEntryForm patientEntryForm = new SamplePatientEntryForm();
        patientEntryForm.setPatientProperties(patientInfo);

        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1); // audit trail FKs sys_user_id -> system_user; id=1 is the seeded admin
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, usd);

        PatientManagementUpdate patientUpdate = new PatientManagementUpdate();

        try {
            samplePatientEntryService.persistData(updateData, patientUpdate, patientInfo, patientEntryForm, request);
            fail("Expected exception due to missing patient ID was not thrown");
        } catch (Exception e) {
            assertNotNull("Exception should be thrown for missing patient ID", e.getMessage());
        }
    }

    private MockHttpServletRequest newRequestWithSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1); // audit trail FKs sys_user_id -> system_user; id=1 is the seeded admin
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        return request;
    }

    private SamplePatientUpdateData runPersistPatientFlow(PatientManagementInfo patientInfo,
            MockHttpServletRequest request) throws Exception {
        Sample sample = sampleService.getSampleByAccessionNumber("TEST001");
        assertNotNull("Sample should exist", sample);

        SampleHuman sampleHuman = new SampleHuman();
        sampleHuman.setId("1");
        sampleHuman.setSampleId(String.valueOf(sample.getId()));
        sampleHumanService.getData(sampleHuman);

        SamplePatientEntryForm patientEntryForm = new SamplePatientEntryForm();
        patientEntryForm.setPatientProperties(patientInfo);

        PatientManagementUpdate patientManagementUpdate = newPatientManagementUpdate();
        patientManagementUpdate.setPatientUpdateStatus(patientInfo);
        // Resolve currentUserId from the session's UserSessionData (id=1) so the
        // persisted patient/person/address rows carry a valid sys_user_id; without
        // this the audit trail fails with "System User ID is null".
        patientManagementUpdate.setSysUserIdFromRequest(request);

        SamplePatientUpdateData updateData = new SamplePatientUpdateData("1");
        updateData.setSample(sample);
        updateData.setSampleHuman(sampleHuman);
        updateData.setSampleItemsTests(new java.util.ArrayList<>());
        updateData.setSavePatient(patientManagementUpdate.getPatientUpdateStatus() != PatientUpdateStatus.NO_ACTION);

        if (updateData.isSavePatient()) {
            updateData.setPatientErrors(patientManagementUpdate.preparePatientData(request, patientInfo));
        }

        samplePatientEntryService.persistData(updateData, patientManagementUpdate, patientInfo, patientEntryForm,
                request);
        return updateData;
    }

    @Test
    public void persistData_existingPatientPKWithAddStatus_mustNotCreateDuplicate() throws Exception {
        long personCountBefore = personService.getAll().size();
        long patientCountBefore = patientService.getAll().size();

        // Existing patient (id=1 / person_id=4) from testdata/samplepatiententry.xml
        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setPatientPK("1");
        patientInfo.setPatientUpdateStatus(PatientUpdateStatus.ADD);
        patientInfo.setLastName("DoeUpdated");
        patientInfo.setFirstName("John");

        runPersistPatientFlow(patientInfo, newRequestWithSession());

        assertEquals("A non-blank patientPK must not result in a new person row", personCountBefore,
                personService.getAll().size());
        assertEquals("A non-blank patientPK must not result in a new patient row", patientCountBefore,
                patientService.getAll().size());
        assertEquals("The existing patientPK must be reused, not replaced by a newly generated one", "1",
                patientInfo.getPatientPK());
        assertEquals("The existing person's data must actually be updated, not left stale", "DoeUpdated",
                personService.get("4").getLastName());
    }

    @Test
    public void persistData_blankPatientPKWithAddStatus_stillCreatesNewPatient() throws Exception {
        long personCountBefore = personService.getAll().size();
        long patientCountBefore = patientService.getAll().size();

        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setPatientUpdateStatus(PatientUpdateStatus.ADD);
        patientInfo.setLastName("BrandNew");
        patientInfo.setFirstName("Patient");

        runPersistPatientFlow(patientInfo, newRequestWithSession());

        assertEquals("A genuinely new patient (no patientPK) must still be inserted as a new person",
                personCountBefore + 1, personService.getAll().size());
        assertEquals("A genuinely new patient (no patientPK) must still be inserted as a new patient",
                patientCountBefore + 1, patientService.getAll().size());
    }

    @Test
    public void persistData_nullUpdateStatusWithExistingPatientPK_mustNotCreateDuplicate() throws Exception {
        long personCountBefore = personService.getAll().size();
        long patientCountBefore = patientService.getAll().size();

        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setPatientPK("1");
        // patientUpdateStatus intentionally left null.
        patientInfo.setLastName("DoeViaNullStatus");

        runPersistPatientFlow(patientInfo, newRequestWithSession());

        assertEquals("A missing update-status with a valid patientPK must not create a duplicate person",
                personCountBefore, personService.getAll().size());
        assertEquals("A missing update-status with a valid patientPK must not create a duplicate patient",
                patientCountBefore, patientService.getAll().size());
        assertEquals("A missing update-status with a valid patientPK must still apply the update", "DoeViaNullStatus",
                personService.get("4").getLastName());
    }

    @Test
    public void persistData_noActionWithExistingPatientPK_appliesNoChanges() throws Exception {
        long personCountBefore = personService.getAll().size();
        long patientCountBefore = patientService.getAll().size();

        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setPatientPK("1");
        patientInfo.setPatientUpdateStatus(PatientUpdateStatus.NO_ACTION);
        patientInfo.setLastName("ShouldNeverBeWritten");

        runPersistPatientFlow(patientInfo, newRequestWithSession());

        assertEquals("NO_ACTION must not create a new person row", personCountBefore, personService.getAll().size());
        assertEquals("NO_ACTION must not create a new patient row", patientCountBefore, patientService.getAll().size());
        assertEquals("NO_ACTION must not modify the existing person's data", "Doe",
                personService.get("4").getLastName());
    }

}