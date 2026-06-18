package org.openelisglobal.testcalculated.service;

import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.openelisglobal.testcalculated.valueholder.ResultCalculation;
import org.springframework.beans.factory.annotation.Autowired;

public class ResultCalculationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ResultCalculationService resultCalculationService;

    @Autowired
    private TestService testService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/result-calculation.xml");
        seedTestOperations();
    }

    /**
     * Populates the test_operations @ElementCollection join table via the service
     * layer. DBUnit REFRESH cannot handle tables without a primary key
     * (NoPrimaryKeyException), so this data is seeded programmatically instead of
     * through the XML fixture.
     *
     * Dataset layout after seeding: rc 1 → patient 1, calc 1, result 3001, tests
     * {101} rc 2 → patient 1, calc 2, result 3002, tests {101} rc 3 → patient 2,
     * calc 2, result 3002, tests {101} rc 4 → patient 2, calc 3, result 3003, tests
     * {102}
     */
    private void seedTestOperations() {
        // Use testService.get() to obtain Hibernate-managed instances.
        // A bare new Test() with only setId() is transient and causes
        // TransientObjectException when Hibernate flushes the @ElementCollection.
        org.openelisglobal.test.valueholder.Test test101 = testService.get("101");
        org.openelisglobal.test.valueholder.Test test102 = testService.get("102");

        for (int rcId : new int[] { 1, 2, 3 }) {
            ResultCalculation rc = resultCalculationService.get(rcId);
            rc.setTest(Set.of(test101));
            rc.setSysUserId(TEST_SYS_USER_ID);
            resultCalculationService.update(rc);
        }
        ResultCalculation rc4 = resultCalculationService.get(4);
        rc4.setTest(Set.of(test102));
        rc4.setSysUserId(TEST_SYS_USER_ID);
        resultCalculationService.update(rc4);
    }

    /**
     * Creates an {@code org.openelisglobal.test.valueholder.Test} by id. Uses a
     * fully-qualified type name here only once to avoid the import clash with
     * {@code org.junit.Test}; callers receive the valueholder via this method
     * without needing the FQN themselves.
     */
    private org.openelisglobal.test.valueholder.Test labTest(String id) {
        org.openelisglobal.test.valueholder.Test t = new org.openelisglobal.test.valueholder.Test();
        t.setId(id);
        return t;
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

    @Test
    public void get_shouldReturnNullForNonExistentId() {
        ResultCalculation rc = resultCalculationService.get(9999);

        Assert.assertNull(rc);
    }

    @Test
    public void getCount_shouldReturnTotalCountMatchingDataset() {
        Integer count = resultCalculationService.getCount();

        Assert.assertEquals(Integer.valueOf(4), count);
    }

    @Test
    public void insert_shouldPersistNewResultCalculationAndIncrementCount() {
        Patient patient = new Patient();
        patient.setId("1");

        Calculation calculation = new Calculation();
        calculation.setId(1);

        ResultCalculation rc = new ResultCalculation();
        rc.setPatient(patient);
        rc.setCalculation(calculation);
        rc.setSysUserId(TEST_SYS_USER_ID);

        Integer newId = resultCalculationService.insert(rc);
        ResultCalculation persisted = resultCalculationService.get(newId);

        Assert.assertEquals(newId, persisted.getId());
        Assert.assertEquals("1", persisted.getPatient().getId());
        Assert.assertEquals(Integer.valueOf(1), persisted.getCalculation().getId());
        Assert.assertEquals(Integer.valueOf(5), resultCalculationService.getCount());
    }

    @Test
    public void update_shouldPersistChangedCalculationOnExistingRow() {
        ResultCalculation rc = resultCalculationService.get(1);
        Assert.assertEquals(Integer.valueOf(1), rc.getCalculation().getId());

        Calculation newCalculation = new Calculation();
        newCalculation.setId(2);
        rc.setCalculation(newCalculation);
        rc.setSysUserId(TEST_SYS_USER_ID);
        resultCalculationService.update(rc);

        ResultCalculation updated = resultCalculationService.get(1);
        Assert.assertEquals(Integer.valueOf(2), updated.getCalculation().getId());
        Assert.assertEquals("1", updated.getPatient().getId());
        Assert.assertEquals("3001", updated.getResult().getId());
    }

    @Test
    public void delete_shouldRemoveRowAndDecrementCount() {
        Assert.assertEquals(Integer.valueOf(4), resultCalculationService.getCount());

        ResultCalculation rc = resultCalculationService.get(1);
        rc.setSysUserId(TEST_SYS_USER_ID);
        resultCalculationService.delete(rc);

        Assert.assertNull(resultCalculationService.get(1));
        Assert.assertEquals(Integer.valueOf(3), resultCalculationService.getCount());
    }

    @Test
    public void getResultCalculationByPatientAndTest_patient1Test101_shouldReturnTwoRows() {
        Patient patient = new Patient();
        patient.setId("1");

        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndTest(patient,
                labTest("101"));

        Assert.assertEquals(2, results.size());
        results.forEach(rc -> {
            Assert.assertEquals("1", rc.getPatient().getId());
            Assert.assertTrue("test set must contain test 101 for rc " + rc.getId(),
                    rc.getTest().stream().anyMatch(t -> "101".equals(t.getId())));
        });
        long distinctIds = results.stream().map(ResultCalculation::getId).distinct().count();
        Assert.assertEquals("Returned rows must have distinct IDs", 2, distinctIds);
    }

    @Test
    public void getResultCalculationByPatientAndTest_patient2Test101_shouldReturnOneRow() {
        Patient patient = new Patient();
        patient.setId("2");

        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndTest(patient,
                labTest("101"));

        Assert.assertEquals(1, results.size());
        Assert.assertEquals(Integer.valueOf(3), results.get(0).getId());
        Assert.assertEquals("2", results.get(0).getPatient().getId());
        Assert.assertEquals(Integer.valueOf(2), results.get(0).getCalculation().getId());
    }

    @Test
    public void getResultCalculationByPatientAndTest_patient2Test102_shouldReturnOneRow() {
        Patient patient = new Patient();
        patient.setId("2");

        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndTest(patient,
                labTest("102"));

        Assert.assertEquals(1, results.size());
        Assert.assertEquals(Integer.valueOf(4), results.get(0).getId());
        Assert.assertEquals("2", results.get(0).getPatient().getId());
        Assert.assertEquals(Integer.valueOf(3), results.get(0).getCalculation().getId());
    }

    @Test
    public void getResultCalculationByPatientAndTest_patient1Test102_shouldReturnEmptyList() {
        Patient patient = new Patient();
        patient.setId("1");

        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndTest(patient,
                labTest("102"));

        Assert.assertTrue("Patient 1 has no rc linked to test 102", results.isEmpty());
    }

    @Test
    public void getResultCalculationByPatientAndTest_nullPatient_shouldReturnEmptyList() {
        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndTest(null,
                labTest("101"));

        Assert.assertTrue("Null patient guard must short-circuit and return empty list", results.isEmpty());
    }

    @Test
    public void getResultCalculationByPatientAndTest_nonExistentPatient_shouldReturnEmptyList() {
        Patient ghost = new Patient();
        ghost.setId("9999");

        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndTest(ghost,
                labTest("101"));

        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void getResultCalculationByTest_test101_shouldReturnThreeRows() {
        List<ResultCalculation> results = resultCalculationService.getResultCalculationByTest(labTest("101"));

        Assert.assertEquals(3, results.size());
        results.forEach(rc -> Assert.assertTrue("Every returned rc must carry test 101 in its test set",
                rc.getTest().stream().anyMatch(t -> "101".equals(t.getId()))));
        Assert.assertTrue("Result set must contain rc ids 1, 2, and 3",
                results.stream().map(ResultCalculation::getId).allMatch(id -> id == 1 || id == 2 || id == 3));
    }

    @Test
    public void getResultCalculationByTest_test102_shouldReturnOneRow() {
        List<ResultCalculation> results = resultCalculationService.getResultCalculationByTest(labTest("102"));

        Assert.assertEquals(1, results.size());
        Assert.assertEquals(Integer.valueOf(4), results.get(0).getId());
        Assert.assertEquals("2", results.get(0).getPatient().getId());
        Assert.assertEquals(Integer.valueOf(3), results.get(0).getCalculation().getId());
    }

    @Test
    public void getResultCalculationByTest_nonExistentTest_shouldReturnEmptyList() {
        List<ResultCalculation> results = resultCalculationService.getResultCalculationByTest(labTest("9999"));

        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void getResultCalculationByPatientAndCalculation_patient1Calculation1_shouldReturnRc1() {
        Patient patient = new Patient();
        patient.setId("1");

        Calculation calculation = new Calculation();
        calculation.setId(1);

        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndCalculation(patient,
                calculation);

        Assert.assertEquals(1, results.size());
        Assert.assertEquals(Integer.valueOf(1), results.get(0).getId());
        Assert.assertEquals("1", results.get(0).getPatient().getId());
        Assert.assertEquals(Integer.valueOf(1), results.get(0).getCalculation().getId());
        Assert.assertEquals("3001", results.get(0).getResult().getId());
    }

    @Test
    public void getResultCalculationByPatientAndCalculation_patient1Calculation2_shouldReturnRc2() {
        Patient patient = new Patient();
        patient.setId("1");

        Calculation calculation = new Calculation();
        calculation.setId(2);

        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndCalculation(patient,
                calculation);

        Assert.assertEquals(1, results.size());
        Assert.assertEquals(Integer.valueOf(2), results.get(0).getId());
        Assert.assertEquals("1", results.get(0).getPatient().getId());
        Assert.assertEquals(Integer.valueOf(2), results.get(0).getCalculation().getId());
        Assert.assertEquals("3002", results.get(0).getResult().getId());
    }

    @Test
    public void getResultCalculationByPatientAndCalculation_patient2Calculation2_shouldReturnRc3() {
        Patient patient = new Patient();
        patient.setId("2");

        Calculation calculation = new Calculation();
        calculation.setId(2);

        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndCalculation(patient,
                calculation);

        Assert.assertEquals(1, results.size());
        Assert.assertEquals(Integer.valueOf(3), results.get(0).getId());
        Assert.assertEquals("2", results.get(0).getPatient().getId());
        Assert.assertEquals(Integer.valueOf(2), results.get(0).getCalculation().getId());
        Assert.assertEquals("3002", results.get(0).getResult().getId());
    }

    @Test
    public void getResultCalculationByPatientAndCalculation_patient1Calculation3_shouldReturnEmptyList() {
        Patient patient = new Patient();
        patient.setId("1");

        Calculation calculation = new Calculation();
        calculation.setId(3);

        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndCalculation(patient,
                calculation);

        Assert.assertTrue("Patient 1 has no rc for calculation 3", results.isEmpty());
    }

    @Test
    public void getResultCalculationByPatientAndCalculation_nullPatient_shouldReturnEmptyList() {
        Calculation calculation = new Calculation();
        calculation.setId(1);

        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndCalculation(null,
                calculation);

        Assert.assertTrue("Null patient guard must short-circuit and return empty list", results.isEmpty());
    }

    @Test
    public void getResultCalculationByPatientAndCalculation_nonExistentCalculation_shouldReturnEmptyList() {
        Patient patient = new Patient();
        patient.setId("1");

        Calculation calculation = new Calculation();
        calculation.setId(9999);

        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndCalculation(patient,
                calculation);

        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void getResultCalculationByPatientAndCalculation_nonExistentPatient_shouldReturnEmptyList() {
        Patient ghost = new Patient();
        ghost.setId("9999");

        Calculation calculation = new Calculation();
        calculation.setId(1);

        List<ResultCalculation> results = resultCalculationService.getResultCalculationByPatientAndCalculation(ghost,
                calculation);

        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void getResultCalculationByTest_returnedRowsMustHaveAllRelationshipsLoaded() {
        List<ResultCalculation> results = resultCalculationService.getResultCalculationByTest(labTest("101"));

        Assert.assertEquals(3, results.size());
        for (ResultCalculation rc : results) {
            Assert.assertNotNull("calculation must be loaded for rc " + rc.getId(), rc.getCalculation());
            Assert.assertNotNull("patient must be loaded for rc " + rc.getId(), rc.getPatient());
            Assert.assertFalse("test set must be non-empty for rc " + rc.getId(), rc.getTest().isEmpty());
        }
    }

    @Test
    public void insert_newRowShouldBeReturnedBySubsequentPatientAndTestQuery() {
        Patient patient = new Patient();
        patient.setId("1");

        int before = resultCalculationService.getResultCalculationByPatientAndTest(patient, labTest("101")).size();
        Assert.assertEquals("Baseline must be 2 rows for patient 1 / test 101", 2, before);

        Calculation calculation = new Calculation();
        calculation.setId(1);

        ResultCalculation rc = new ResultCalculation();
        rc.setPatient(patient);
        rc.setCalculation(calculation);
        // testService.get() returns a managed entity — required when Hibernate flushes
        // the @ElementCollection; labTest() (transient) is only safe for query params.
        rc.setTest(Set.of(testService.get("101")));
        rc.setSysUserId(TEST_SYS_USER_ID);
        resultCalculationService.insert(rc);

        List<ResultCalculation> after = resultCalculationService.getResultCalculationByPatientAndTest(patient,
                labTest("101"));
        Assert.assertEquals("Inserted row must appear in subsequent query", 3, after.size());
        Assert.assertTrue("New rc must link to calculation 1",
                after.stream().filter(r -> r.getCalculation().getId().equals(1)).count() >= 2);
    }
}
