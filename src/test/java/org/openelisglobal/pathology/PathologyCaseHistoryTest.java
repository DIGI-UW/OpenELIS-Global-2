package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.audittrail.action.workers.AuditTrailItem;
import org.openelisglobal.common.services.historyservices.ProgramSampleHistoryService;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The order's audit trail reads the case's stage changes from the case's own
 * diff entries, one per move.
 *
 * <p>
 * The stage moves also write an entry under the same reference row whose
 * payload is JSON naming the verb, and a return carries the bench's free-text
 * reason. The audit trail used to read those entries as diffs: every attribute
 * came out blank, which blanked the older status lines, and a reason holding a
 * start tag with no end failed the whole view. These tests read the trail the
 * way the order's audit screen does.
 */
public class PathologyCaseHistoryTest extends PathologyCaseTestSupport {

    private static final String STATUS = "status";

    /** technician2, the fixture's other technician. */
    private static final String SECOND_TECHNICIAN_ID = "1004";

    @Autowired
    private PlatformTransactionManager transactionManager;

    private StageSwitches switches;

    @Before
    public void switchEveryStageOn() {
        switches = StageSwitches.allOn();
    }

    @After
    public void restoreTheSwitches() {
        switches.close();
    }

    @Test
    public void aMoveAndAReturn_readAsTheRealStagesOnTheCaseHistory() {
        pathologySampleService.advance(CASE_ID, PathologyStatus.GROSSING, TECHNICIAN_ID);
        pathologySampleService.returnToPreviousStage(CASE_ID, PathologyStatus.PROCESSING, "<status>x", TECHNICIAN_ID);

        List<AuditTrailItem> items = auditTrail();

        assertEquals("each move reads as one status line, newest first, naming the stages it left and entered",
                List.of(List.of("PROCESSING", "Grossing"), List.of("GROSSING", "PROCESSING")), statusLines(items));
        assertFalse("and no line reads as a value changed to nothing",
                items.stream().anyMatch(item -> StringUtils.isBlank(item.getNewValue())));
        assertEquals("each move leaves exactly one diff entry carrying the status", 2, statusDiffEntries());
    }

    @Test
    public void aRelease_readsAsOneStatusLineOnTheCaseHistory() {
        storeAt(PathologyStatus.READY_PATHOLOGIST);
        PathologySampleForm form = caseViewForm();
        form.setRelease(true);

        save(form);

        assertEquals("the release reads as the move onto Completed", List.of(List.of("READY_PATHOLOGIST", "Completed")),
                statusLines(auditTrail()));
        assertEquals("from exactly one diff entry carrying the status", 1, statusDiffEntries());
    }

    /**
     * A save between two moves reads as its own lines, and each move as its own
     * stages. The history used to treat every attribute an entry did not carry as
     * changed from nothing, so the newest entry showed the case's cassettes, flag
     * and pathologist as set by the latest move, and a move's status line lost the
     * stage it left.
     */
    @Test
    public void aSaveBetweenTwoMoves_leavesEachMoveReadingItsOwnStages() {
        jdbcTemplate.update("UPDATE clinlims.pathology_sample SET technician_id = NULL WHERE id = ?", CASE_ID);
        String description = pathologySampleService.get(CASE_ID).getGrossExam();
        pathologySampleService.advance(CASE_ID, PathologyStatus.GROSSING, TECHNICIAN_ID);
        PathologySampleForm form = caseViewForm();
        form.setGrossExam("Two fragments of grey-white tissue");
        form.setAssignedTechnicianId(SECOND_TECHNICIAN_ID);
        save(form);
        pathologySampleService.advance(CASE_ID, PathologyStatus.PROCESSING, TECHNICIAN_ID);

        List<AuditTrailItem> items = auditTrail();

        assertEquals("each move reads as the stages it left and entered, newest first",
                List.of(List.of("PROCESSING", "Embedding"), List.of("GROSSING", "PROCESSING")), statusLines(items));
        assertEquals("the save reads as one description line, from the old text to the new",
                List.of(List.of(description, "Two fragments of grey-white tissue")), linesFor(items, "grossExam"));
        assertEquals("and one technician line, from nobody to the technician it assigned",
                List.of(List.of("", assignedTechnicianName())), linesFor(items, "technician"));
        for (String unchanged : List.of("blocks", "slides", "pathologist", "decalcificationRequired")) {
            assertTrue("nothing reads as changing " + unchanged + ", which no entry changed",
                    linesFor(items, unchanged).isEmpty());
        }
    }

    // helpers

    /** The order's audit trail for the case, read as the audit screen reads it. */
    private List<AuditTrailItem> auditTrail() {
        return new TransactionTemplate(transactionManager).execute(
                status -> new ProgramSampleHistoryService(pathologySampleService.get(CASE_ID)).getAuditTrailItems());
    }

    private String assignedTechnicianName() {
        return new TransactionTemplate(transactionManager)
                .execute(status -> pathologySampleService.get(CASE_ID).getTechnician_Audit());
    }

    /** Each status line as the stage it left and the stage it entered. */
    private List<List<String>> statusLines(List<AuditTrailItem> items) {
        return linesFor(items, STATUS);
    }

    /** Each line about one attribute as its old and new value, newest first. */
    private List<List<String>> linesFor(List<AuditTrailItem> items, String attribute) {
        return items.stream().filter(item -> attribute.equals(item.getIdentifier())).map(item -> List
                .of(StringUtils.defaultString(item.getOldValue()), StringUtils.defaultString(item.getNewValue())))
                .collect(Collectors.toList());
    }

    private long statusDiffEntries() {
        return caseAudit().stream().map(this::changesOf)
                .filter(changes -> changes.startsWith("<") && changes.contains("<status>")).count();
    }
}
