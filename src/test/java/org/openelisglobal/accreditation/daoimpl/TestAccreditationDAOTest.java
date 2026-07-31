package org.openelisglobal.accreditation.daoimpl;

import java.time.LocalDate;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.accreditation.dao.TestAccreditationDAO;
import org.openelisglobal.accreditation.valueholder.TestAccreditation;
import org.springframework.beans.factory.annotation.Autowired;

public class TestAccreditationDAOTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestAccreditationDAO testAccreditationDAO;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/accreditation.xml");
        resyncSequence("clinlims.accrediting_body_seq", "clinlims.accrediting_body");
        resyncSequence("clinlims.test_accreditation_seq", "clinlims.test_accreditation");
    }

    @Test
    public void findByTestId_shouldReturnAllAccreditationsForThatTest() {
        // Test 9001 (HIV Rapid Test) has two rows: one per body (ISO15189, SLIPTA)
        List<TestAccreditation> results = testAccreditationDAO.findByTestId("9001");

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.size());
    }

    @Test
    public void findByTestId_shouldReturnEmptyForUnaccreditedTest() {
        // Test 9003 (Malaria Smear) has no accreditation rows
        List<TestAccreditation> results = testAccreditationDAO.findByTestId("9003");

        Assert.assertNotNull(results);
        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void findByAccreditingBodyId_shouldReturnAllAccreditationsForThatBody() {
        // ISO15189 (9001) covers test 9001 (active) and test 9002 (expired)
        List<TestAccreditation> results = testAccreditationDAO.findByAccreditingBodyId(9001L);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.size());
    }

    @Test
    public void findByTestAndBody_shouldReturnSpecificAccreditation() {
        TestAccreditation result = testAccreditationDAO.findByTestAndBody("9001", 9001L);

        Assert.assertNotNull(result);
        Assert.assertEquals(Long.valueOf(9001L), result.getId());
        Assert.assertEquals(LocalDate.parse("2099-01-01"), result.getExpiresOn());
    }

    @Test
    public void findByTestAndBody_shouldReturnNullWhenNoSuchPairExists() {
        // Test 9003 has no link to any body
        TestAccreditation result = testAccreditationDAO.findByTestAndBody("9003", 9001L);

        Assert.assertNull(result);
    }

    @Test
    public void findExpiringOnOrBefore_shouldReturnOnlyExpiredOrDueRows() {
        List<TestAccreditation> expiring = testAccreditationDAO.findExpiringOnOrBefore(LocalDate.now().minusDays(1));

        Assert.assertNotNull(expiring);
        Assert.assertEquals(1, expiring.size());
        Assert.assertEquals(Long.valueOf(9002L), expiring.get(0).getId());
    }

    @Test
    public void findAllActive_shouldExcludeExpiredAndInactiveBodyRows() {
        // 9001: active body + future expiry -> included
        // 9002: active body + past expiry -> excluded (expired)
        // 9003: inactive body + future expiry -> excluded (body inactive)
        List<TestAccreditation> active = testAccreditationDAO.findAllActive();

        Assert.assertNotNull(active);
        Assert.assertEquals(1, active.size());
        Assert.assertEquals(Long.valueOf(9001L), active.get(0).getId());
    }

    @Test
    public void countActiveByTestId_shouldCountOnlyNonExpiredActiveBodyRows() {
        // Test 9001 has 2 rows: one active (ISO15189), one against inactive SLIPTA
        long count = testAccreditationDAO.countActiveByTestId("9001");

        Assert.assertEquals(1, count);
    }

    @Test
    public void existsByTestAndBody_shouldReturnTrueWhenPairExists() {
        boolean exists = testAccreditationDAO.existsByTestAndBody("9001", 9001L);

        Assert.assertTrue(exists);
    }

    @Test
    public void existsByTestAndBody_shouldReturnFalseWhenPairDoesNotExist() {
        boolean exists = testAccreditationDAO.existsByTestAndBody("9003", 9001L);

        Assert.assertFalse(exists);
    }

    @Test
    public void findByFilters_withBodyIdFilter_shouldReturnOnlyThatBodysAccreditations() {
        List<TestAccreditation> results = testAccreditationDAO.findByFilters(null, 9001L, null, null);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.size());
        Assert.assertTrue(results.stream().allMatch(ta -> ta.getAccreditingBody().getId().equals(9001L)));
    }

    @Test
    public void findByFilters_withTestIdFilter_shouldReturnOnlyThatTestsAccreditations() {
        List<TestAccreditation> results = testAccreditationDAO.findByFilters("9001", null, null, null);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.size());
        Assert.assertTrue(results.stream().allMatch(ta -> ta.getTest().getId().equals("9001")));
    }

    @Test
    public void findByFilters_withSearchQuery_shouldMatchTestDescription() {
        // "CD4 Count" (test 9002) matches query "cd4" case-insensitively
        List<TestAccreditation> results = testAccreditationDAO.findByFilters(null, null, null, "cd4");

        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(Long.valueOf(9002L), results.get(0).getId());
    }

    @Test
    public void findByFilters_withNoMatchingQuery_shouldReturnEmpty() {
        List<TestAccreditation> results = testAccreditationDAO.findByFilters(null, null, null, "nonexistentquery");

        Assert.assertNotNull(results);
        Assert.assertTrue(results.isEmpty());
    }
}
