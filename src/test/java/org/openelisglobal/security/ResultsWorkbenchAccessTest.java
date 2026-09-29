package org.openelisglobal.security;

import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.result.action.util.ResultsValidation;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The Results workbench must be usable by the Results role alone.
 *
 * <p>
 * Every Results screen (By Unit, By Order, By Patient) funnels into
 * {@code LogbookResultsRestController} -> {@code ResultsLoadUtility}, and a
 * single denial anywhere in that chain returns 403 for the whole search,
 * leaving the role whose entire job is result entry unable to open its own
 * workbench. Two gates did exactly that, one behind the other:
 * {@code SampleHumanService.getPatientForSample} ({@code patient:view}, now
 * granted to Results in changeset 012-004i) and
 * {@code TestResultComponentService.getActiveComponentsByTestId}
 * ({@code test:configure}, now also accepting {@code catalogue:view}).
 *
 * <p>
 * The second only became reachable once the first was fixed, which is why this
 * asserts on the collaborators directly rather than trusting one green
 * end-to-end call: a chain test stops at its first denial and reports success
 * for everything it never reached.
 */
public class ResultsWorkbenchAccessTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private TestResultComponentService testResultComponentService;

    @Autowired
    private ResultsValidation resultsValidation;

    private void authenticateAsResults() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("results", "N/A", SeededRoleAuthorities.role("Results")));
    }

    /**
     * Resolving the patient behind an accession number: what the results list shows
     * on every row, and what the "By Patient" screen searches on.
     */
    @Test
    public void patientBehindASampleIsReadableByResults() {
        authenticateAsResults();
        // A transient Sample, not null: the gate is evaluated before the body, so a
        // denial throws here while a pass runs on into the lookup. An earlier
        // version passed null, which cleared the gate and then NPE'd inside the
        // DAO, reporting a gate failure that had not happened.
        Sample sample = new Sample();
        sample.setId("1");
        sampleHumanService.getPatientForSample(sample);
    }

    /**
     * The result components of a test: the fields a result has, with their labels,
     * order, type and precision. Result entry cannot render a row without them.
     */
    @Test
    public void testResultComponentsAreReadableByResults() {
        authenticateAsResults();
        assertNotNull(testResultComponentService.getActiveComponentsByTestId("1"));
        assertNotNull(testResultComponentService.getComponentsByTestId("1"));
    }

    /**
     * Inversion: the read gates opened, the write gates did not. Results may see
     * how a test reports its values and may not redefine them.
     */
    @Test(expected = AccessDeniedException.class)
    public void resultsCannotEditTestResultComponents() {
        authenticateAsResults();
        testResultComponentService.saveComponentsForTest("1", Collections.emptyList(), "1");
    }

    /**
     * Submitting a result runs form validation first (is the date parseable, is a
     * result present, is a numeric result numeric). Despite the name this is input
     * checking, not the result-validation workflow, and it was gated on
     * PRIV_RESULT_VALIDATE, so a technologist's save was refused while being
     * checked.
     */
    @Test
    public void submittedResultsCanBeValidatedByTheRoleThatEntersThem() {
        authenticateAsResults();
        assertNotNull(resultsValidation.validateModifiedItems(Collections.emptyList()));
        assertNotNull(resultsValidation.validateItem(new TestResultItem()));
    }

    /** Inversion: the gates are real, not absent. Neither privilege, no read. */
    @Test(expected = AccessDeniedException.class)
    public void withoutEitherPrivilegeTheComponentReadIsRefused() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("nobody", "N/A", Collections.emptyList()));
        testResultComponentService.getActiveComponentsByTestId("1");
    }
}
