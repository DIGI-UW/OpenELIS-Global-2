package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.systemuser.service.UserService;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * A patient report shows only the tests in the reader's Reports lab units, and
 * works out which those are once per report.
 *
 * <p>
 * It used to resolve them for every sample, and resolving them reads every test
 * in the reader's lab units, so a patient with hundreds of samples read the
 * whole catalogue hundreds of times: the report for one such patient took 83
 * seconds, long past the point where the browser gives up. Resolved once, the
 * same report takes 9.
 */
public class PatientReportLabUnitFilterTest extends BaseWebContextSensitiveTest {

    private static final String USER_ID = "7";
    private static final String OWN_TEST = "11";
    private static final String OTHER_TEST = "22";

    private PatientReport report;

    private UserService userService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        report = new PatientClinicalReport();
        userService = mock(UserService.class);
        when(userService.getTestIdsInUserLabUnits(USER_ID, Constants.ROLE_REPORTS))
                .thenReturn(new HashSet<>(Arrays.asList(OWN_TEST)));
        ReflectionTestUtils.setField(report, "userService", userService);
        report.setSystemUserId(USER_ID);
    }

    @Test
    public void filterAnalysesForReportUser_keepsOnlyTestsInTheReadersLabUnits() {
        List<Analysis> kept = report.filterAnalysesForReportUser(analyses(OWN_TEST, OTHER_TEST, OWN_TEST));

        assertEquals("only the reader's own tests belong in their report", Arrays.asList(OWN_TEST, OWN_TEST),
                kept.stream().map(analysis -> analysis.getTest().getId()).collect(Collectors.toList()));
    }

    @Test
    public void filterAnalysesForReportUser_resolvesTheReadersTestsOncePerReport() {
        report.filterAnalysesForReportUser(analyses(OWN_TEST));
        report.filterAnalysesForReportUser(analyses(OWN_TEST));
        report.filterAnalysesForReportUser(analyses(OTHER_TEST));

        verify(userService, times(1)).getTestIdsInUserLabUnits(USER_ID, Constants.ROLE_REPORTS);
    }

    @Test
    public void filterAnalysesForReportUser_toleratesAnAnalysisWithNoTest() {
        List<Analysis> withoutTest = new ArrayList<>(analyses(OWN_TEST));
        withoutTest.add(new Analysis());

        assertEquals("an analysis with no test belongs to no lab unit", 1,
                report.filterAnalysesForReportUser(withoutTest).size());
    }

    private List<Analysis> analyses(String... testIds) {
        List<Analysis> analyses = new ArrayList<>();
        for (String testId : testIds) {
            Analysis analysis = new Analysis();
            org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
            test.setId(testId);
            analysis.setTest(test);
            analyses.add(analysis);
        }
        return analyses;
    }
}
