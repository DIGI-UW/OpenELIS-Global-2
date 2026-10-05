package org.openelisglobal.security;

import java.util.Collections;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.services.IReportTrackingService;
import org.openelisglobal.common.services.ReportTrackingService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The Validation workbench must be usable by the Validation role alone.
 *
 * <p>
 * All five Validation screens (Routine, By Order, By Range of Order Numbers, By
 * Date, Study) read the one {@code AccessionValidation} endpoint, so a single
 * denial anywhere in that chain takes out the whole module. Two did:
 * {@code SampleHumanService.getPatientForSample} ({@code patient:view}, granted
 * to Validation in changeset 012-004l) blocked every search, and
 * {@code ReportTrackingService.getLastReportForSample} ({@code report:run})
 * blocked the release itself, AFTER the 21 CFR Part 11 signature had already
 * been recorded.
 *
 * <p>
 * The second is not Validation-specific. {@code patientReportHasBeenDone} asks
 * whether a patient report has already gone out, so an amended result is
 * annotated as a corrected one, and the same call sits in the results logbook,
 * {@code ResultUtil} and the home dashboard. Hence the read is widened to
 * accept both result privileges rather than granting Validation a reporting
 * one.
 */
public class ValidationWorkbenchAccessTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleHumanService sampleHumanService;

    // The interface, not the impl: the bean is a JDK proxy (it is @PreAuthorize-d),
    // so injecting the concrete class fails with "expected ReportTrackingService
    // but
    // was $ProxyNNN". ReportType is declared on the impl, hence both imports.
    @Autowired
    private IReportTrackingService reportTrackingService;

    private void authenticateAs(String user, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, "N/A", SeededRoleAuthorities.role(role)));
    }

    /** The patient behind a sample: rendered on every validation row. */
    @Test
    public void patientBehindASampleIsReadableByValidation() {
        authenticateAs("validation", "Validation");
        Sample sample = new Sample();
        sample.setId("1");
        sampleHumanService.getPatientForSample(sample);
    }

    /**
     * The release path's "has a patient report already gone out?" check. A
     * transient Sample, not null: the gate runs before the body, so a denial throws
     * while a pass reads through to the DAO.
     */
    @Test
    public void priorPatientReportIsReadableByValidation() {
        authenticateAs("validation", "Validation");
        Sample sample = new Sample();
        sample.setId("1");
        reportTrackingService.getLastReportForSample(sample, ReportTrackingService.ReportType.PATIENT);
    }

    /**
     * The same check runs in the results logbook and on the home dashboard, so the
     * technologist must satisfy it too. Widening for Validation alone would have
     * left Results denied on a path this walkthrough never reached.
     */
    @Test
    public void priorPatientReportIsAlsoReadableByResults() {
        authenticateAs("results", "Results");
        Sample sample = new Sample();
        sample.setId("1");
        reportTrackingService.getLastReportForSample(sample, ReportTrackingService.ReportType.PATIENT);
    }

    /**
     * Inversion: the read opened, report administration did not. Neither result
     * role may add a report-tracking record.
     */
    @Test(expected = AccessDeniedException.class)
    public void validationCannotRecordAnIssuedReport() {
        authenticateAs("validation", "Validation");
        reportTrackingService.addReports(Collections.singletonList("1"), ReportTrackingService.ReportType.PATIENT,
                "patient", "1");
    }

    /** Inversion: the gate is real. Neither privilege, no read. */
    @Test(expected = AccessDeniedException.class)
    public void withoutAnyOfThePrivilegesThePriorReportReadIsRefused() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("nobody", "N/A", Collections.emptyList()));
        Sample sample = new Sample();
        sample.setId("1");
        reportTrackingService.getLastReportForSample(sample, ReportTrackingService.ReportType.PATIENT);
    }
}
