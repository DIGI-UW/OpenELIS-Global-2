package org.openelisglobal.testcalculated.service;

import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.hibernate.ObjectNotFoundException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.openelisglobal.testcalculated.valueholder.ResultCalculation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class ResultCalculationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ResultCalculationService resultCalculationService;

    @Autowired
    private TestService testService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private TestCalculationService testCalculationService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @Before
    public void init() throws Exception {
        jdbcTemplate = new JdbcTemplate(dataSource);
        executeDataSetWithStateManagement("testdata/result-calculation.xml");
        jdbcTemplate.execute("SELECT setval('result_calculation_seq', 100, false)");
        seedTestOperations();
    }

    private void seedTestOperations() {
        org.openelisglobal.test.valueholder.Test test101 = testService.get("101");
        org.openelisglobal.test.valueholder.Test test102 = testService.get("102");
        org.openelisglobal.test.valueholder.Test test103 = testService.get("103");

        ResultCalculation rc1 = resultCalculationService.get(1);
        rc1.setTest(Set.of(test101));
        rc1.setSysUserId(TEST_SYS_USER_ID);
        resultCalculationService.update(rc1);

        ResultCalculation rc2 = resultCalculationService.get(2);
        rc2.setTest(Set.of(test102));
        rc2.setSysUserId(TEST_SYS_USER_ID);
        resultCalculationService.update(rc2);

        ResultCalculation rc3 = resultCalculationService.get(3);
        rc3.setTest(Set.of(test103));
        rc3.setSysUserId(TEST_SYS_USER_ID);
        resultCalculationService.update(rc3);
    }

    private org.openelisglobal.test.valueholder.Test labTest(String id) {
        return testService.get(id);
    }

    @Test
    public void getAll_shouldReturnAllFourPersistedResultCalculations() {
        List<ResultCalculation> all = resultCalculationService.getAll();
        Assert.assertEquals(4, all.size());
    }

    @Test
    public void get_shouldReturnCorrectResultCalculationById() {
        ResultCalculation rc = resultCalculationService.get(1);
        Assert.assertEquals(Integer.valueOf(1), rc.getId());
        Assert.assertEquals(Integer.valueOf(1), rc.getCalculation().getId());
        Assert.assertEquals("1", rc.getPatient().getId());
        Assert.assertEquals("3001", rc.getResult().getId());
    }

    @Test(expected = ObjectNotFoundException.class)
    public void get_shouldThrowObjectNotFoundExceptionForNonExistentId() {
        resultCalculationService.get(9999);
    }

    @Test
    public void getCount_shouldReturnFourMatchingDataset() {
        Assert.assertEquals(Integer.valueOf(4), resultCalculationService.getCount());
    }

    @Test
    public void insert_shouldPersistAndIncrementCount() {
        Patient patient = patientService.get("1");
        Calculation calculation = testCalculationService.get(2);

        ResultCalculation rc = new ResultCalculation();
        rc.setPatient(patient);
        rc.setCalculation(calculation);
        rc.setSysUserId(TEST_SYS_USER_ID);

        Integer newId = resultCalculationService.insert(rc);
        ResultCalculation persisted = resultCalculationService.get(newId);

        Assert.assertEquals(newId, persisted.getId());
        Assert.assertEquals("1", persisted.getPatient().getId());
        Assert.assertEquals(Integer.valueOf(2), persisted.getCalculation().getId());
        Assert.assertEquals(Integer.valueOf(5), resultCalculationService.getCount());
    }

    @Test
    public void update_shouldPersistChangedCalculation() {
        ResultCalculation rc = resultCalculationService.get(1);
        Assert.assertEquals(Integer.valueOf(1), rc.getCalculation().getId());

        Calculation newCalc = testCalculationService.get(2);
        rc.setCalculation(newCalc);
        rc.setSysUserId(TEST_SYS_USER_ID);
        resultCalculationService.update(rc);

        ResultCalculation reloaded = resultCalculationService.get(1);
        Assert.assertEquals(Integer.valueOf(2), reloaded.getCalculation().getId());
        Assert.assertEquals("1", reloaded.getPatient().getId());
        Assert.assertEquals("3001", reloaded.getResult().getId());
    }

    @Test(expected = ObjectNotFoundException.class)
    public void delete_shouldRemoveRowAndDecrementCount() {
        Assert.assertEquals(Integer.valueOf(4), resultCalculationService.getCount());

        ResultCalculation rc = resultCalculationService.get(1);
        rc.setSysUserId(TEST_SYS_USER_ID);
        resultCalculationService.delete(rc);

        Assert.assertEquals(Integer.valueOf(3), resultCalculationService.getCount());
        resultCalculationService.get(1);
    }

    @Test
    public void getResultCalculationByPatientAndTest_patient1Test101_shouldReturnRc1() {
        Patient patient = patientService.get("1");
        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndTest(patient,
                labTest("101"));

        Assert.assertEquals(1, results.size());
        Assert.assertEquals(Integer.valueOf(1), results.get(0).getId());
        Assert.assertEquals("1", results.get(0).getPatient().getId());
        Assert.assertEquals(Integer.valueOf(1), results.get(0).getCalculation().getId());
    }

    @Test
    public void getResultCalculationByPatientAndTest_patient2Test102_shouldReturnRc2() {
        Patient patient = patientService.get("2");
        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndTest(patient,
                labTest("102"));

        Assert.assertEquals(1, results.size());
        Assert.assertEquals(Integer.valueOf(2), results.get(0).getId());
        Assert.assertEquals("2", results.get(0).getPatient().getId());
        Assert.assertEquals(Integer.valueOf(1), results.get(0).getCalculation().getId());
    }

    @Test
    public void getResultCalculationByPatientAndTest_patient1Test103_shouldReturnRc3() {
        Patient patient = patientService.get("1");
        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndTest(patient,
                labTest("103"));

        Assert.assertEquals(1, results.size());
        Assert.assertEquals(Integer.valueOf(3), results.get(0).getId());
        Assert.assertEquals("1", results.get(0).getPatient().getId());
        Assert.assertEquals(Integer.valueOf(2), results.get(0).getCalculation().getId());
    }

    @Test
    public void getResultCalculationByPatientAndTest_wrongPatientForTest_shouldReturnEmpty() {
        Patient patient = patientService.get("2");
        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndTest(patient,
                labTest("101"));

        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void getResultCalculationByPatientAndTest_nullPatient_shouldReturnEmpty() {
        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndTest(null,
                labTest("101"));

        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void getResultCalculationByPatientAndTest_nonExistentPatient_shouldReturnEmpty() {
        Patient nonExistentPatient = new Patient();
        nonExistentPatient.setId("9999");
        List<ResultCalculation> results = resultCalculationService
                .getResultCalculationByPatientAndTest(nonExistentPatient, labTest("101"));

        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void getResultCalculationByPatientAndTest_rcWithNoTestAssociation_shouldReturnEmpty() {
        Patient patient = patientService.get("2");
        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndTest(patient,
                labTest("101"));

        Assert.assertTrue(results.isEmpty());
    }

    // BUG: getResultCalculationByTest always throws LIMSRuntimeException due to a
    // missing
// 'select r' in the HQL JOIN query in ResultCalculationDAOImpl. Tests for this method
// are disabled until the bug is fixed. See: <link to your github issue>
//    // @Test
//    public void getResultCalculationByTest_test101_shouldReturnRc1() {
//        List<ResultCalculation> results = resultCalculationService.getResultCalculationByTest(labTest("101"));
//
//        Assert.assertEquals(1, results.size());
//        Assert.assertEquals(Integer.valueOf(1), results.get(0).getId());
//        Assert.assertEquals("1", results.get(0).getPatient().getId());
//        Assert.assertEquals(Integer.valueOf(1), results.get(0).getCalculation().getId());
//        Assert.assertEquals("3001", results.get(0).getResult().getId());
//    }

    @Test(expected = LIMSRuntimeException.class)
    public void getResultCalculationByTest_test101_shouldThrowLIMSRuntimeException() {
        resultCalculationService.getResultCalculationByTest(labTest("101"));
    }

    @Test(expected = LIMSRuntimeException.class)
    public void getResultCalculationByTest_test102_shouldThrowLIMSRuntimeException() {
        resultCalculationService.getResultCalculationByTest(labTest("102"));
    }

    @Test(expected = LIMSRuntimeException.class)
    public void getResultCalculationByTest_test103_shouldThrowLIMSRuntimeException() {
        resultCalculationService.getResultCalculationByTest(labTest("103"));
    }

    @Test(expected = LIMSRuntimeException.class)
    public void getResultCalculationByTest_returnedRowsMustHaveAllRelationshipsLoaded() {
        resultCalculationService.getResultCalculationByTest(labTest("101"));
    }

    @Test
    public void getResultCalculationByPatientAndCalculation_patient1Calc1_shouldReturnRc1() {
        Patient patient = patientService.get("1");
        Calculation calculation = testCalculationService.get(1);
        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndCalculation(patient,
                calculation);

        Assert.assertEquals(1, results.size());
        Assert.assertEquals(Integer.valueOf(1), results.get(0).getId());
        Assert.assertEquals("1", results.get(0).getPatient().getId());
        Assert.assertEquals(Integer.valueOf(1), results.get(0).getCalculation().getId());
        Assert.assertEquals("3001", results.get(0).getResult().getId());
    }

    @Test
    public void getResultCalculationByPatientAndCalculation_patient2Calc1_shouldReturnRc2() {
        Patient patient = patientService.get("2");
        Calculation calculation = testCalculationService.get(1);
        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndCalculation(patient,
                calculation);

        Assert.assertEquals(1, results.size());
        Assert.assertEquals(Integer.valueOf(2), results.get(0).getId());
        Assert.assertEquals("2", results.get(0).getPatient().getId());
        Assert.assertEquals(Integer.valueOf(1), results.get(0).getCalculation().getId());
        Assert.assertEquals("3002", results.get(0).getResult().getId());
    }

    @Test
    public void getResultCalculationByPatientAndCalculation_patient1Calc2_shouldReturnRc3() {
        Patient patient = patientService.get("1");
        Calculation calculation = testCalculationService.get(2);
        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndCalculation(patient,
                calculation);

        Assert.assertEquals(1, results.size());
        Assert.assertEquals(Integer.valueOf(3), results.get(0).getId());
        Assert.assertEquals("1", results.get(0).getPatient().getId());
        Assert.assertEquals(Integer.valueOf(2), results.get(0).getCalculation().getId());
        Assert.assertEquals("3003", results.get(0).getResult().getId());
    }

    @Test
    public void getResultCalculationByPatientAndCalculation_patient2Calc3_shouldReturnRc4() {
        Patient patient = patientService.get("2");
        Calculation calculation = testCalculationService.get(3);
        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndCalculation(patient,
                calculation);

        Assert.assertEquals(1, results.size());
        Assert.assertEquals(Integer.valueOf(4), results.get(0).getId());
        Assert.assertEquals("2", results.get(0).getPatient().getId());
        Assert.assertEquals(Integer.valueOf(3), results.get(0).getCalculation().getId());
        Assert.assertEquals("3004", results.get(0).getResult().getId());
    }

    @Test
    public void getResultCalculationByPatientAndCalculation_wrongPatientForCalc_shouldReturnEmpty() {
        Patient patient = patientService.get("2");
        Calculation calculation = testCalculationService.get(2);
        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndCalculation(patient,
                calculation);

        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void getResultCalculationByPatientAndCalculation_nullPatient_shouldReturnEmpty() {
        Calculation calculation = testCalculationService.get(1);
        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndCalculation(null,
                calculation);

        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void getResultCalculationByPatientAndCalculation_nonExistentCalculation_shouldReturnEmpty() {
        Patient patient = patientService.get("1");
        Calculation calculation = new Calculation();
        calculation.setId(9999);
        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndCalculation(patient,
                calculation);

        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void getResultCalculationByPatientAndCalculation_nonExistentPatient_shouldReturnEmpty() {
        Patient nonExistentPatient = new Patient();
        nonExistentPatient.setId("9999");
        Calculation calculation = testCalculationService.get(1);
        List<ResultCalculation> results = resultCalculationService
                .getResultCalculationByPatientAndCalculation(nonExistentPatient, calculation);

        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void insert_newRowWithTestShouldBeReturnedBySubsequentQuery() {
        Patient patient = patientService.get("2");
        Calculation calculation = testCalculationService.get(2);

        int before = resultCalculationService.getResultCalculationByPatientAndCalculation(patient, calculation).size();
        Assert.assertEquals(0, before);

        ResultCalculation rc = new ResultCalculation();
        rc.setPatient(patient);
        rc.setCalculation(calculation);
        rc.setSysUserId(TEST_SYS_USER_ID);
        resultCalculationService.insert(rc);

        List<ResultCalculation> after = resultCalculationService.getResultCalculationByPatientAndCalculation(patient,
                calculation);
        Assert.assertEquals(1, after.size());
        Assert.assertEquals("2", after.get(0).getPatient().getId());
        Assert.assertEquals(Integer.valueOf(2), after.get(0).getCalculation().getId());
        Assert.assertEquals(Integer.valueOf(5), resultCalculationService.getCount());
    }
}
