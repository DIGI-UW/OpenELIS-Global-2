package org.openelisglobal.testcalculated.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.springframework.beans.factory.annotation.Autowired;

public class TestCalculationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestCalculationService testCalculationService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/result-calculation-service.xml");
        resyncSequence("clinlims.calculation_seq", "clinlims.calculation");
    }

    @Test
    public void getAll_shouldReturnAllSeededCalculationsWithCorrectFields() {
        List<Calculation> calculations = testCalculationService.getAll();

        assertEquals("Fixture defines exactly two calculations", 2, calculations.size());
        assertCalculationState(1, "Calc 1", 1, 1, "5.0", true, true, "Note 1");
        assertCalculationState(2, "Calc 2", 1, 2, "10.0", true, true, "Note 2");
    }

    @Test
    public void insert_shouldCreateNewCalculationWithAllFields() {
        Calculation calculation = new Calculation();
        calculation.setName("New Calc");
        calculation.setSampleId(1);
        calculation.setTestId(1);
        calculation.setResult("15.5");
        calculation.setToggled(true);
        calculation.setActive(true);
        calculation.setNote("New note");
        calculation.setSysUserId(TEST_SYS_USER_ID);

        Integer savedId = testCalculationService.insert(calculation);

        assertNotNull("Insert should return a generated id", savedId);
        assertCalculationState(savedId, "New Calc", 1, 1, "15.5", true, true, "New note");
        assertEquals("Insert should increase row count", 3, testCalculationService.getAll().size());
    }

    @Test
    public void update_shouldUpdateExistingCalculation() {
        Calculation calculation = testCalculationService.get(1);
        calculation.setName("Updated Calc");
        calculation.setResult("99.9");
        calculation.setToggled(false);
        calculation.setActive(false);
        calculation.setNote("Updated note");
        calculation.setSysUserId(TEST_SYS_USER_ID);

        testCalculationService.update(calculation);

        assertCalculationState(1, "Updated Calc", 1, 1, "99.9", false, false, "Updated note");
        assertEquals("Update should not change row count", 2, testCalculationService.getAll().size());
    }

    @Test
    public void delete_shouldRemoveCalculation() {
        Calculation calculation = new Calculation();
        calculation.setName("To Delete");
        calculation.setSampleId(1);
        calculation.setTestId(1);
        calculation.setResult("1.0");
        calculation.setToggled(true);
        calculation.setActive(true);
        calculation.setNote("Delete me");
        calculation.setSysUserId(TEST_SYS_USER_ID);
        Integer savedId = testCalculationService.insert(calculation);

        int countBeforeDelete = testCalculationService.getAll().size();

        Calculation toDelete = testCalculationService.get(savedId);
        toDelete.setSysUserId(TEST_SYS_USER_ID);
        testCalculationService.delete(toDelete);

        List<Calculation> remaining = testCalculationService.getAll();
        assertEquals("Delete should reduce count by one", countBeforeDelete - 1, remaining.size());
        assertFalse("Deleted calculation should be absent",
                remaining.stream().anyMatch(c -> savedId.equals(c.getId())));
    }

    @Test
    public void getCount_shouldReturnTotalNumberOfCalculations() {
        assertEquals("Fixture seeds exactly two calculations", Integer.valueOf(2), testCalculationService.getCount());
    }

    @Test
    public void getAllMatching_byName_shouldReturnOnlyMatchingCalculations() {
        List<Calculation> results = testCalculationService.getAllMatching("name", "Calc 1");

        assertEquals("Exactly one calculation named 'Calc 1'", 1, results.size());
        assertEquals("Matching calculation should be id 1", Integer.valueOf(1), results.get(0).getId());
        assertEquals("Matching name", "Calc 1", results.get(0).getName());
    }

    @Test
    public void getAllMatching_byName_noMatch_shouldReturnEmptyList() {
        List<Calculation> results = testCalculationService.getAllMatching("name", "Nonexistent Calc");

        assertNotNull("Result list should not be null", results);
        assertEquals("No calculations should match a nonexistent name", 0, results.size());
    }

    @Test
    public void getAllOrdered_byNameAscending_shouldReturnCalculationsInOrder() {
        List<Calculation> results = testCalculationService.getAllOrdered("name", false);

        assertEquals("All calculations should be returned", 2, results.size());
        assertEquals("First in ascending order should be 'Calc 1'", "Calc 1", results.get(0).getName());
        assertEquals("Second in ascending order should be 'Calc 2'", "Calc 2", results.get(1).getName());
    }

    @Test
    public void getAllOrdered_byNameDescending_shouldReturnCalculationsInReverseOrder() {
        List<Calculation> results = testCalculationService.getAllOrdered("name", true);

        assertEquals("All calculations should be returned", 2, results.size());
        assertEquals("First in descending order should be 'Calc 2'", "Calc 2", results.get(0).getName());
        assertEquals("Second in descending order should be 'Calc 1'", "Calc 1", results.get(1).getName());
    }

    @Test
    public void getMatch_byName_shouldReturnMatchingCalculation() {
        Optional<Calculation> result = testCalculationService.getMatch("name", "Calc 2");

        assertTrue("A match should be found for 'Calc 2'", result.isPresent());
        assertEquals("Matched calculation id", Integer.valueOf(2), result.get().getId());
        assertEquals("Matched calculation name", "Calc 2", result.get().getName());
    }

    @Test
    public void getMatch_byName_noMatch_shouldReturnEmpty() {
        Optional<Calculation> result = testCalculationService.getMatch("name", "Does Not Exist");

        assertFalse("No match should be found for a nonexistent name", result.isPresent());
    }

    @Test(expected = org.hibernate.ObjectNotFoundException.class)
    public void get_nonExistentId_shouldThrowObjectNotFoundException() {
        // BaseDAOImpl.get() propagates Hibernate's ObjectNotFoundException
        // directly when no row exists for the given id.
        testCalculationService.get(99999);
    }

    private void assertCalculationState(Integer id, String name, Integer sampleId, Integer testId, String result,
            boolean toggled, boolean active, String note) {
        Calculation calculation = testCalculationService.get(id);
        assertEquals("Calculation id", id, calculation.getId());
        assertEquals("Calculation name for id " + id, name, calculation.getName());
        assertEquals("Calculation sampleId for id " + id, sampleId, calculation.getSampleId());
        assertEquals("Calculation testId for id " + id, testId, calculation.getTestId());
        assertEquals("Calculation result for id " + id, result, calculation.getResult());
        assertEquals("Calculation toggled for id " + id, toggled, calculation.getToggled());
        assertEquals("Calculation active for id " + id, active, calculation.getActive());
        assertEquals("Calculation note for id " + id, note, calculation.getNote());
    }
}
