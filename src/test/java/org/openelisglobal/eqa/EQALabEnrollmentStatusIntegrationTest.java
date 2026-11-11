package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.audittrail.daoimpl.AuditTrailServiceImpl;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.eqa.service.EQALabProgramEnrollmentService;
import org.openelisglobal.eqa.valueholder.EQALabProgramEnrollment;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * A laboratory suspends, resumes and withdraws its own EQA enrolment. Each move
 * carries a reason and the date it takes effect, and the prior status reaches
 * the audit history so a later reader can see what the enrolment was before.
 */
public class EQALabEnrollmentStatusIntegrationTest extends EQASpineTestBase {

    private static final String PROVIDER = "Lifecycle test provider";

    private static final Date EFFECTIVE = Date.valueOf("2026-09-30");

    @Autowired
    private EQALabProgramEnrollmentService enrollmentService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    /**
     * AppTestConfig supplies a mock audit trail, which records nothing. The history
     * assertions below need the real one, wired the way the container wires it.
     */
    @Before
    public void useTheRealAuditTrail() {
        AuditTrailServiceImpl realAuditTrail = new AuditTrailServiceImpl();
        ReflectionTestUtils.setField(realAuditTrail, "referenceTablesService", referenceTablesService);
        ReflectionTestUtils.setField(realAuditTrail, "historyService", historyService);
        Object target = AopTestUtils.getUltimateTargetObject(enrollmentService);
        ReflectionTestUtils.setField(target, "auditTrailService", realAuditTrail);
    }

    private EQALabProgramEnrollment enrol(String programName) {
        EQALabProgramEnrollment enrollment = new EQALabProgramEnrollment();
        enrollment.setProgramName(programName);
        enrollment.setProvider(PROVIDER);
        enrollment.setSysUserId(USER);
        return enrollmentService.createEnrollment(enrollment, null, null, null, null);
    }

    /** Every audit row this table holds for one enrolment, oldest first. */
    private List<History> audit(Long enrollmentId) {
        String referenceTableId = referenceTablesService.getReferenceTableByName("eqa_lab_program_enrollment").getId();
        List<History> rows = historyService.getHistoryByRefIdAndRefTableId(String.valueOf(enrollmentId),
                referenceTableId);
        rows.sort((left, right) -> Integer.compare(Integer.parseInt(left.getId()), Integer.parseInt(right.getId())));
        return rows;
    }

    private Set<String> auditRowIds(Long enrollmentId) {
        return audit(enrollmentId).stream().map(History::getId).collect(Collectors.toSet());
    }

    /**
     * The rows written since a snapshot of the ids. {@code history} keeps every row
     * ever written against a reference id, and no fixture in this suite empties it,
     * so counting the whole result measures the run rather than the transition
     * under test.
     */
    private List<History> auditRowsSince(Long enrollmentId, Set<String> before) {
        return audit(enrollmentId).stream().filter(row -> !before.contains(row.getId())).collect(Collectors.toList());
    }

    /**
     * Every row the query returned, not only the one an assertion picked. A row
     * whose content is unexpected and a row that was never ours read identically
     * from a message carrying one of them, and telling those apart after the fact
     * has cost this suite a full CI cycle.
     */
    private String describe(Long enrollmentId) {
        // The reference table id belongs in the dump too. A history row stores the
        // id the write resolved the name to, and the read resolves the name again,
        // so a row can be missed because that id moved rather than because the
        // write was wrong.
        StringBuilder detail = new StringBuilder("audit rows for enrolment ").append(enrollmentId)
                .append(" (reference table id ")
                .append(referenceTablesService.getReferenceTableByName("eqa_lab_program_enrollment").getId())
                .append("):");
        for (History row : audit(enrollmentId)) {
            detail.append("\n  id=").append(row.getId()).append(" activity=").append(row.getActivity())
                    .append(" sysUserId=").append(row.getSysUserId()).append(" changes=")
                    .append(row.getChanges() == null ? "<null>" : changesOf(row).replace('\n', ' '));
        }
        return detail.toString();
    }

    private String changesOf(History row) {
        return new String(row.getChanges(), StandardCharsets.UTF_8);
    }

    @Test
    public void anEnrolmentIsCreatedActiveAndTakingPart() {
        EQALabProgramEnrollment enrolled = enrol("Lifecycle new enrolment");

        assertEquals("Active", enrolled.getStatus());
        assertTrue(enrolled.getIsActive());
    }

    @Test
    public void suspendingRecordsTheReasonTheDateAndTakesTheLaboratoryOutOfPlay() {
        EQALabProgramEnrollment enrolled = enrol("Lifecycle suspend");

        enrollmentService.updateStatus(enrolled.getId(), "Suspended", "Analyser away for repair", EFFECTIVE, USER);

        EQALabProgramEnrollment reloaded = enrollmentService.get(enrolled.getId());
        assertEquals("Suspended", reloaded.getStatus());
        assertEquals("Analyser away for repair", reloaded.getStatusReason());
        assertEquals(EFFECTIVE.toString(),
                new SimpleDateFormat("yyyy-MM-dd").format(reloaded.getStatusEffectiveDate()));
        assertEquals(Long.valueOf(USER), reloaded.getStatusChangedBy());
        assertNotNull(reloaded.getStatusChangedDate());
        // The rest of the module reads the flag, so a suspended laboratory has to
        // fall out of the active enrolments the cycle gate sizes itself by.
        assertFalse(reloaded.getIsActive());
        assertFalse(
                enrollmentService.findActiveEnrollments().stream().anyMatch(e -> e.getId().equals(enrolled.getId())));
    }

    @Test
    public void aSuspendedEnrolmentResumesAndTakesPartAgain() {
        EQALabProgramEnrollment enrolled = enrol("Lifecycle resume");
        enrollmentService.updateStatus(enrolled.getId(), "Suspended", "Analyser away for repair", EFFECTIVE, USER);

        enrollmentService.updateStatus(enrolled.getId(), "Active", "Analyser back in service", EFFECTIVE, USER);

        EQALabProgramEnrollment reloaded = enrollmentService.get(enrolled.getId());
        assertEquals("Active", reloaded.getStatus());
        assertEquals("Analyser back in service", reloaded.getStatusReason());
        assertTrue(reloaded.getIsActive());
        assertTrue(
                enrollmentService.findActiveEnrollments().stream().anyMatch(e -> e.getId().equals(enrolled.getId())));
    }

    @Test
    public void withdrawnIsTheEndOfTheRow() {
        EQALabProgramEnrollment enrolled = enrol("Lifecycle withdraw");

        enrollmentService.updateStatus(enrolled.getId(), "Withdrawn", "Laboratory left the programme", EFFECTIVE, USER);

        assertEquals("Withdrawn", enrollmentService.get(enrolled.getId()).getStatus());
        // A laboratory that comes back enrols again rather than reviving this row,
        // which is how a provider's own enrolment behaves.
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> enrollmentService.updateStatus(enrolled.getId(), "Active", "Changed our minds", EFFECTIVE, USER));
        assertTrue(refused.getMessage(), refused.getMessage().contains("Withdrawn"));
        assertEquals("Withdrawn", enrollmentService.get(enrolled.getId()).getStatus());
    }

    @Test
    public void aTransitionWithoutAReasonOrADateIsRefused() {
        EQALabProgramEnrollment enrolled = enrol("Lifecycle required fields");

        assertThrows(IllegalArgumentException.class,
                () -> enrollmentService.updateStatus(enrolled.getId(), "Suspended", "  ", EFFECTIVE, USER));
        assertThrows(IllegalArgumentException.class,
                () -> enrollmentService.updateStatus(enrolled.getId(), "Suspended", "No date given", null, USER));

        // Neither attempt may have moved the enrolment.
        assertEquals("Active", enrollmentService.get(enrolled.getId()).getStatus());
    }

    @Test
    public void theAuditKeepsThePriorStatusTheReasonAndTheUser() {
        EQALabProgramEnrollment enrolled = enrol("Lifecycle audit");

        Set<String> beforeSuspension = auditRowIds(enrolled.getId());
        enrollmentService.updateStatus(enrolled.getId(), "Suspended", "Analyser away for repair", EFFECTIVE, USER);

        List<History> suspension = auditRowsSince(enrolled.getId(), beforeSuspension);
        assertEquals(describe(enrolled.getId()), 1, suspension.size());
        // The history row carries the values the enrolment held before the move,
        // which is the half a status column on its own cannot answer.
        String recorded = changesOf(suspension.get(0));
        assertTrue(describe(enrolled.getId()), recorded.contains("status") && recorded.contains("Active"));
        assertEquals("U", suspension.get(0).getActivity());
        assertEquals(USER, suspension.get(0).getSysUserId());
        assertNotNull(suspension.get(0).getTimestamp());

        Set<String> beforeWithdrawal = auditRowIds(enrolled.getId());
        enrollmentService.updateStatus(enrolled.getId(), "Withdrawn", "Laboratory left the programme", EFFECTIVE, USER);

        List<History> withdrawal = auditRowsSince(enrolled.getId(), beforeWithdrawal);
        assertEquals(describe(enrolled.getId()), 1, withdrawal.size());
        String second = changesOf(withdrawal.get(0));
        assertTrue(describe(enrolled.getId()), second.contains("Suspended"));
        assertTrue(describe(enrolled.getId()), second.contains("Analyser away for repair"));
        assertEquals(USER, withdrawal.get(0).getSysUserId());
    }
}
