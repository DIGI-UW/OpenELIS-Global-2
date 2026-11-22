package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * OGC-1192 §2 — patient-scoped reports meet patientless (environmental)
 * samples. Looking the patient up for such a sample must leave the report
 * without a current patient instead of throwing, so the report loop can skip
 * the sample.
 */
public class PatientReportPatientlessTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleService sampleService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/order-dashboard-patientless.xml");
    }

    @Test
    public void findPatientFromSample_leavesNoCurrentPatient_forAPatientlessSample() {
        PatientReport report = new PatientClinicalReport();
        Sample patientless = sampleService.getSampleByAccessionNumber("DASH-0025");
        assertNotNull(patientless);
        ReflectionTestUtils.setField(report, "currentSample", patientless);
        ReflectionTestUtils.setField(report, "currentPatient", new Patient());

        ReflectionTestUtils.invokeMethod(report, "findPatientFromSample");

        assertNull("a patientless sample must clear the current patient",
                ReflectionTestUtils.getField(report, "currentPatient"));
    }
}
