package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.openelisglobal.eqa.service.EQALabProgramEnrollmentService;
import org.openelisglobal.eqa.valueholder.EQALabProgramEnrollment;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * My EQA Programs lists a laboratory's enrolments grouped by status, which the
 * page can only do if they arrive grouped. The names here are chosen so that
 * ordering by name alone produces the opposite order: the active enrolment
 * sorts last alphabetically and must still be listed first.
 */
public class EQAMyProgramsOrderingIntegrationTest extends EQASpineTestBase {

    @Autowired
    private EQALabProgramEnrollmentService enrollmentService;

    private EQALabProgramEnrollment enrol(String programName, boolean active) {
        EQALabProgramEnrollment enrollment = new EQALabProgramEnrollment();
        enrollment.setProgramName(programName);
        enrollment.setProvider("Ordering test provider");
        enrollment.setIsActive(active);
        enrollment.setSysUserId(USER);
        return enrollmentService.createEnrollment(enrollment, null, null, null, null);
    }

    @Test
    public void activeEnrolmentsAreListedBeforeInactiveOnes() {
        enrol("Zeta ordering active", true);
        enrol("Alpha ordering inactive", false);
        enrol("Beta ordering active", true);

        List<String> listed = enrollmentService.findAll().stream()
                .filter(e -> "Ordering test provider".equals(e.getProvider()))
                .map(EQALabProgramEnrollment::getProgramName).collect(Collectors.toList());

        // Both active ones first and alphabetical within the group, so the page
        // can start a heading wherever the status changes.
        assertEquals(List.of("Beta ordering active", "Zeta ordering active", "Alpha ordering inactive"), listed);
    }

    @Test
    public void theStatusGroupsAreContiguousAcrossEveryEnrolment() {
        enrol("Zeta contiguity active", true);
        enrol("Alpha contiguity inactive", false);

        List<Boolean> statuses = enrollmentService.findAll().stream().map(EQALabProgramEnrollment::getIsActive)
                .collect(Collectors.toList());

        // A heading per status only reads correctly if the status changes once
        // across the whole list, whatever else the database already holds.
        long changes = 0;
        for (int i = 1; i < statuses.size(); i++) {
            if (!statuses.get(i).equals(statuses.get(i - 1))) {
                changes++;
            }
        }
        assertTrue("expected active rows then inactive rows, saw " + changes + " status changes in " + statuses,
                changes <= 1);
        assertTrue("expected the list to start with the active enrolments", statuses.get(0));
    }
}
