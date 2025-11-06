package org.openelisglobal.sample;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
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

    private PatientManagementUpdate patientManagementUpdate;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/samplepatiententry.xml");
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
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, usd);

        PatientManagementUpdate patientUpdate = new PatientManagementUpdate();

        try {
            samplePatientEntryService.persistData(updateData, patientUpdate, patientInfo, patientEntryForm, request);
            fail("Expected exception due to missing patient ID was not thrown");
        } catch (Exception e) {
            assertNotNull("Exception should be thrown for missing patient ID", e.getMessage());
        }
    }

}