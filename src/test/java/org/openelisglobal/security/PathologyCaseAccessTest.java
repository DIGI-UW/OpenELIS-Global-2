package org.openelisglobal.security;

import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.requester.service.SampleRequesterService;
import org.openelisglobal.result.service.LogbookResultsPersistService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The pathology, cytology and immunohistochemistry case views must be usable by
 * the sign-off roles alone.
 *
 * <p>
 * All three case views assemble the same header, so one denial anywhere in that
 * chain returns 403 for the whole case and a pathologist sees a dashboard they
 * cannot open anything from. Five gates did that, discovered one behind the
 * next: {@code SampleHumanService.getPatientForSample} ({@code patient:view},
 * granted in 012-004q), then {@code SampleRequesterService},
 * {@code OrganizationService}, {@code SampleQaEventService} and
 * {@code StatusOfSampleService} (granted in 012-004r).
 *
 * <p>
 * Signing a cytology case out is separate and was blocked on its own:
 * {@code LogbookResultsPersistService.persistDataSet} finalizes the analyses
 * and releases the case, gated on {@code result:enter}/{@code result:modify},
 * which neither pathology role holds. It now also accepts
 * {@code result:pathology-sign-off} - granting the other two instead would let
 * a pathologist enter and amend ordinary bench results.
 */
public class PathologyCaseAccessTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private SampleRequesterService sampleRequesterService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private LogbookResultsPersistService logbookResultsPersistService;

    /**
     * Authenticates with exactly the seeded role's privileges.
     *
     * <p>
     * Deliberately not named authenticateAs: the base class has a method by that
     * name which grants fullTestAuthorities(), i.e. everything. Using it here would
     * make every assertion below pass regardless of the seed, which is the opposite
     * of what this test is for.
     */
    private void authenticateWithSeededRole(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(role.toLowerCase(), "N/A", SeededRoleAuthorities.role(role)));
    }

    /** The patient behind the specimen: the case view's header. */
    @Test
    public void patientBehindASampleIsReadableByPathology() {
        authenticateWithSeededRole("Pathologist");
        Sample sample = new Sample();
        sample.setId("1");
        sampleHumanService.getPatientForSample(sample);
    }

    /** Who requested the specimen, and from which organisation. */
    @Test
    public void requesterAndOrganisationAreReadableByPathology() {
        authenticateWithSeededRole("Pathologist");
        assertNotNull(sampleRequesterService.getRequestersForSampleId("1"));
        organizationService.getOrganizationById("1");
    }

    /** Cytopathologist reaches the same header on its own case views. */
    @Test
    public void caseHeaderIsReadableByCytopathology() {
        authenticateWithSeededRole("Cytopathologist");
        Sample sample = new Sample();
        sample.setId("1");
        sampleHumanService.getPatientForSample(sample);
        assertNotNull(sampleRequesterService.getRequestersForSampleId("1"));
    }

    /**
     * The sign-off itself. Null arguments are fine: the gate is evaluated before
     * the body, so a denial throws here and a pass runs on into the persist, which
     * is what is under test.
     */
    @Test(expected = NullPointerException.class)
    public void cytologySignOffPassesTheGateAndReachesThePersist() {
        authenticateWithSeededRole("Cytopathologist");
        // Reaching a NullPointerException means the @PreAuthorize was satisfied;
        // an AccessDeniedException would mean the sign-off is still refused.
        logbookResultsPersistService.persistDataSet(null, Collections.emptyList(), "1");
    }

    /**
     * Inversion: the sign-off gate is real. A role holding none of the three
     * accepted privileges is still refused, so the test above is not passing
     * because the gate is absent.
     */
    @Test(expected = AccessDeniedException.class)
    public void signOffIsRefusedWithoutAnyOfTheThreePrivileges() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("nobody", "N/A", Collections.emptyList()));
        logbookResultsPersistService.persistDataSet(null, Collections.emptyList(), "1");
    }

    /**
     * Inversion: the grant is scoped. Pathology may read the case header, not
     * rewrite the organisations behind it.
     *
     * <p>
     * Deliberately a DECLARED method rather than the inherited CRUD:
     * OrganizationService carries neither a type-level gate nor
     * {@code @CrudPrivileges}, so {@code CrudGate} lets its inherited writes
     * through for any authenticated user. That is a pre-existing gap, tracked on
     * the baseline in {@code InheritedCrudGateCoverageTest} and out of scope here;
     * asserting on it would fail for a reason that has nothing to do with the
     * pathology grants.
     */
    @Test(expected = AccessDeniedException.class)
    public void pathologyCannotManageOrganisations() {
        authenticateWithSeededRole("Pathologist");
        organizationService.linkOrganizationAndType(new Organization(), "1");
    }
}
