package org.openelisglobal.testcalculated.service;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.testcalculated.valueholder.ResultCalculation;
import org.springframework.beans.factory.annotation.Autowired;

public class ResultCalculationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ResultCalculationService resultCalculationService;

    @Autowired
    private javax.sql.DataSource dataSource;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/result-calculation-service.xml");
        resyncSequence("clinlims.result_calculation_seq", "clinlims.result_calculation");

        // Seed the test_operations join table via raw JDBC — DBUnit's REFRESH
        // cannot handle tables without a primary key (NoPrimaryKeyException).
        try (java.sql.Connection conn = dataSource.getConnection(); java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM clinlims.test_operations");
            stmt.execute(
                    "INSERT INTO clinlims.test_operations (result_calculation_id, test_id) VALUES (1, 1), (2, 2), (3, 1)");
        }
    }

    @Test
    public void delete_shouldRemoveResultCalculation() {
        ResultCalculation rc = new ResultCalculation();
        rc.setSysUserId(TEST_SYS_USER_ID);
        Integer id = resultCalculationService.insert(rc);

        ResultCalculation fetched = resultCalculationService.get(id);
        fetched.setSysUserId(TEST_SYS_USER_ID);
        resultCalculationService.delete(fetched);
    }

    @Test
    public void getResultCalculationByTest_shouldReturnMatchingCalculations() {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId("1");

        resultCalculationService.getResultCalculationByTest(test);
    }

    @Test
    public void getResultCalculationByTest_withUnlinkedTest_shouldReturnEmptyList() {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId("999");

        resultCalculationService.getResultCalculationByTest(test);
    }

    @Test
    public void insert_shouldCreateNewResultCalculation() {
        ResultCalculation rc = new ResultCalculation();
        rc.setSysUserId(TEST_SYS_USER_ID);

        Integer id = resultCalculationService.insert(rc);
        assertNotNull(id);
    }

    @Test
    public void update_shouldUpdateExistingResultCalculation() {
        ResultCalculation rc = resultCalculationService.get(1);
        rc.setSysUserId(TEST_SYS_USER_ID);

        resultCalculationService.update(rc);
    }
}
