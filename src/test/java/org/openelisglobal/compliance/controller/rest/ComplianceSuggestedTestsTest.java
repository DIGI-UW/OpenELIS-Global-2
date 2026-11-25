package org.openelisglobal.compliance.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.compliance.form.SuggestedTest;
import org.openelisglobal.compliance.service.ComplianceStandardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-768: the tests the selected compliance standards ask for, each once, with
 * the standards that cover it, their thresholds, whether those disagree, and
 * the sample types the test runs on.
 */
@Rollback
@Transactional
public class ComplianceSuggestedTestsTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ComplianceStandardService complianceStandardService;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/compliance-suggested-tests.xml");
    }

    @Test
    public void twoStandardsGiveEachTestOnceWithTheStandardsThatCoverIt() {
        List<SuggestedTest> tests = complianceStandardService.suggestedTests(List.of("9761", "9762"));

        assertEquals("Lead, pH and Turbidity by name; the inactive Nitrate threshold suggests nothing",
                List.of("9762", "9761", "9763"), tests.stream().map(SuggestedTest::testId).toList());

        SuggestedTest ph = tests.get(1);
        assertEquals(List.of("PP 22/2021", "WHO-DWG-4"),
                ph.coveredBy().stream().map(SuggestedTest.Coverage::regulationNumber).toList());
        assertTrue("6-9 and 6.5-8.5 disagree", ph.conflicting());
        assertEquals(2, ph.thresholds().size());
        assertEquals("11558-4", ph.loinc());
        assertEquals(List.of("9761"), ph.sampleTypeIds());

        SuggestedTest lead = tests.get(0);
        assertEquals(List.of("9761"), lead.coveredBy().stream().map(SuggestedTest.Coverage::standardId).toList());
        assertFalse(lead.conflicting());

        SuggestedTest turbidity = tests.get(2);
        assertEquals(2, turbidity.coveredBy().size());
        assertFalse("both standards set the same 5 NTU limit", turbidity.conflicting());
        assertEquals(List.of("9762"), turbidity.sampleTypeIds());
    }

    @Test
    public void oneStandardGivesOnlyItsTestsAndNoStandardGivesNone() {
        List<SuggestedTest> who = complianceStandardService.suggestedTests(List.of("9762"));

        assertEquals(List.of("9761", "9763"), who.stream().map(SuggestedTest::testId).toList());
        assertFalse("one standard cannot disagree with itself", who.get(0).conflicting());
        assertEquals(List.of(), complianceStandardService.suggestedTests(List.of()));
        assertEquals(List.of(), complianceStandardService.suggestedTests(List.of("abc", "999999")));
    }

    /**
     * OGC-768 review: a threshold on a test no sample type runs does not break the
     * plan, and only environmental sample types are offered: a clinical type the
     * test also runs on is left out.
     */
    @Test
    public void onlyEnvironmentalSampleTypesAreOfferedAndATestWithNoneStillPlans() {
        jdbcTemplate.update("UPDATE clinlims.compliance_threshold SET is_active = true WHERE id = 9766");
        jdbcTemplate.update("INSERT INTO clinlims.type_of_sample (id, description, domain, name_localization_id,"
                + " lastupdated) VALUES (9763, 'Serum', 'H', 9761, now())");
        jdbcTemplate
                .update("INSERT INTO clinlims.sampletype_test (id, sample_type_id, test_id) VALUES (9764, 9763, 9762)");

        List<SuggestedTest> tests = complianceStandardService.suggestedTests(List.of("9761", "9762"));

        SuggestedTest nitrate = tests.stream().filter(test -> "9764".equals(test.testId())).findFirst().orElseThrow();
        assertEquals(List.of(), nitrate.sampleTypeIds());
        SuggestedTest lead = tests.stream().filter(test -> "9762".equals(test.testId())).findFirst().orElseThrow();
        assertEquals(List.of("9761"), lead.sampleTypeIds());
    }

    @Test
    public void theEndpointAnswersWithTheUnion() throws Exception {
        mockMvc.perform(get("/rest/compliance/standards/suggested-tests").param("standardIds", "9761,9762"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[1].testName").value("pH"))
                .andExpect(jsonPath("$[1].coveredBy[1].regulationNumber").value("WHO-DWG-4"))
                .andExpect(jsonPath("$[1].thresholds[0].min").value(6.0))
                .andExpect(jsonPath("$[1].conflicting").value(true));
    }
}
