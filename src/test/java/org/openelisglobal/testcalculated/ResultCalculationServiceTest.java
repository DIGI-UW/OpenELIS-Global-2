package org.openelisglobal.testcalculated;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testcalculated.service.ResultCalculationService;
import org.openelisglobal.testcalculated.service.TestCalculationService;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.openelisglobal.testcalculated.valueholder.ResultCalculation;
import org.springframework.beans.factory.annotation.Autowired;

public class ResultCalculationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ResultCalculationService resultCalculationService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private TestService testService;
    @Autowired
    private TestCalculationService calculationService;

    private List<ResultCalculation> resultCalculations;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/result-calculation.xml");
    }

    @Test
    public void getResultCalculationByPatientAndTest_ShouldReturnAllResultCalculationsMatchingThePatientAndTestValuesPassedAsParameters() {
        Patient patient = patientService.get("201");
        org.openelisglobal.test.valueholder.Test test = testService.get("4001");
        resultCalculations = resultCalculationService.getResultCalculationByPatientAndTest(patient, test);
        assertNotNull(resultCalculations);
        assertEquals(1, resultCalculations.size());
        assertEquals(Integer.valueOf("701"), resultCalculations.get(0).getId());
    }

    @Test
    public void getResultCalculationByTest_ShouldReturnResultCalculationsWhoseTestIdValueIsInTheSetCollection() {
        org.openelisglobal.test.valueholder.Test test = testService.get("4001");
        resultCalculations = resultCalculationService.getResultCalculationByTest(test);
        assertNotNull(resultCalculations);
        assertEquals(1, resultCalculations.size());
        assertEquals(Integer.valueOf("701"), resultCalculations.get(0).getId());
    }

    @Test
    public void getResultCalculationByPatientAndCalculation_ShouldReturnAllResultCalculationsMatchingThePatientAndCalculationValuesPassedAsParameters() {
        Calculation calculation = calculationService.get(101);
        Patient patient = patientService.get("201");
        resultCalculations = resultCalculationService.getResultCalculationByPatientAndCalculation(patient, calculation);
        assertNotNull(resultCalculations);
        assertEquals(2, resultCalculations.size());
        assertEquals(Integer.valueOf("704"), resultCalculations.get(1).getId());
    }

    @Test
    public void getAll_ShouldReturnAllResultCalculations() {
        resultCalculations = resultCalculationService.getAll();
        assertNotNull(resultCalculations);
        assertEquals(5, resultCalculations.size());
        assertEquals(Integer.valueOf("705"), resultCalculations.get(4).getId());
    }
}
