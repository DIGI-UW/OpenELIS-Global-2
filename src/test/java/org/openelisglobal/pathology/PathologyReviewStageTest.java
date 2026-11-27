package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.audittrail.action.workers.AuditTrailItem;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.services.historyservices.ProgramSampleHistoryService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.service.PathologyDisplayService;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.PathologyStageConflictException;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The pathologist's own stage (FR-12.1): Begin review moves a case from the
 * queue to Under Pathologist Review, in the name of whoever begins when nobody
 * is assigned, and from there only a release or a return moves it.
 */
public class PathologyReviewStageTest extends PathologyCaseTestSupport {

    private static final String PATHOLOGIST_ID = "1002";
    private static final String SECOND_PATHOLOGIST_ID = "1003";

    @Autowired
    private PathologyDisplayService pathologyDisplayService;

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
    public void beginningTheReview_movesTheCaseUnderReview_inTheActorsName() {
        storeAt(PathologyStatus.READY_PATHOLOGIST);
        jdbcTemplate.update("UPDATE clinlims.pathology_sample SET pathologist_id = NULL WHERE id = ?", CASE_ID);

        pathologySampleService.advance(CASE_ID, PathologyStatus.READY_PATHOLOGIST, SECOND_PATHOLOGIST_ID);

        assertEquals("the case is under review", "UNDER_REVIEW", storedStage());
        assertEquals("taken in the name of whoever began it", SECOND_PATHOLOGIST_ID, storedPathologist());
        Map<String, Object> event = stageEvents().get(stageEvents().size() - 1);
        assertEquals("the move is a stage event onto review", "UNDER_REVIEW", event.get("stage"));
        assertEquals("forward", "FORWARD", event.get("direction"));
        assertEquals("by the actor", SECOND_PATHOLOGIST_ID, operatorOf(event.get("entered_by")));
        assertEquals("audited as any advance", 1, entriesWithVerb(PathologySampleService.PATHOLOGY_STAGE_ADVANCED));
        List<List<String>> pathologistLines = historyLines("pathologist");
        assertEquals("the case's history row names the assignment", 1, pathologistLines.size());
        assertEquals("from nobody", "", pathologistLines.get(0).get(0));
        assertTrue("to the actor", StringUtils.isNotBlank(pathologistLines.get(0).get(1)));
    }

    @Test
    public void beginningTheReview_keepsAnAssignedPathologist() {
        storeAt(PathologyStatus.READY_PATHOLOGIST);

        pathologySampleService.advance(CASE_ID, PathologyStatus.READY_PATHOLOGIST, SECOND_PATHOLOGIST_ID);

        assertEquals("the case is under review", "UNDER_REVIEW", storedStage());
        assertEquals("with the pathologist it was assigned to", PATHOLOGIST_ID, storedPathologist());
        assertTrue("and no line about the pathologist", historyLines("pathologist").isEmpty());
    }

    @Test
    public void beginningTheReview_asksNothingAboutOpenRequests() {
        storeAt(PathologyStatus.READY_PATHOLOGIST);
        assertEquals("setup: the fixture's request is open", "OPENED", jdbcTemplate.queryForObject(
                "SELECT status FROM clinlims.pathology_request WHERE id = ?", String.class, FIXTURE_REQUEST_ID));

        pathologySampleService.advance(CASE_ID, PathologyStatus.READY_PATHOLOGIST, PATHOLOGIST_ID, List.of());

        assertEquals("the case is under review", "UNDER_REVIEW", storedStage());
        assertEquals("and no request is recorded as passed over", 0,
                entriesWithVerb(PathologySampleService.PATHOLOGY_REQUEST_OVERRIDDEN));
    }

    @Test
    public void thereIsNoMoveBeyondTheReadingStage() {
        storeAt(PathologyStatus.UNDER_REVIEW);
        assertRefusedWith("pathology.error.advanceBeyondBench",
                () -> pathologySampleService.advance(CASE_ID, PathologyStatus.UNDER_REVIEW, PATHOLOGIST_ID));

        ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_STAGE_UNDER_REVIEW_ENABLED, "false");
        assertRefusedWith("pathology.error.advanceBeyondBench",
                () -> pathologySampleService.advance(CASE_ID, PathologyStatus.UNDER_REVIEW, PATHOLOGIST_ID));
        storeAt(PathologyStatus.READY_PATHOLOGIST);
        assertRefusedWith("pathology.error.advanceBeyondBench",
                () -> pathologySampleService.advance(CASE_ID, PathologyStatus.READY_PATHOLOGIST, PATHOLOGIST_ID));
        assertTrue("and no move is recorded", stageEvents().isEmpty());
    }

    @Test
    public void beginningTwice_isAStageConflict_andOneEvent() {
        storeAt(PathologyStatus.READY_PATHOLOGIST);
        pathologySampleService.advance(CASE_ID, PathologyStatus.READY_PATHOLOGIST, PATHOLOGIST_ID);

        assertThrows("a page that still shows the queue is told the case moved", PathologyStageConflictException.class,
                () -> pathologySampleService.advance(CASE_ID, PathologyStatus.READY_PATHOLOGIST,
                        SECOND_PATHOLOGIST_ID));

        assertEquals("one move onto review", 1, stageEvents().size());
        assertEquals("one advance in the audit", 1, entriesWithVerb(PathologySampleService.PATHOLOGY_STAGE_ADVANCED));
        assertEquals("the case stays with its pathologist", PATHOLOGIST_ID, storedPathologist());
    }

    @Test
    public void returningFromReview_putsTheCaseBackInTheQueue_withItsFindings() {
        storeAt(PathologyStatus.READY_PATHOLOGIST);
        pathologySampleService.advance(CASE_ID, PathologyStatus.READY_PATHOLOGIST, PATHOLOGIST_ID);
        PathologySampleForm form = caseViewForm();
        form.setMicroscopyExam("Atypical glands");
        form.setConclusionText("Suspicious");
        save(form);

        pathologySampleService.returnToPreviousStage(CASE_ID, PathologyStatus.UNDER_REVIEW, "Needs levels",
                TECHNICIAN_ID);
        assertEquals("back in the queue", "READY_PATHOLOGIST", storedStage());
        assertEquals("the reading is kept", "Atypical glands", jdbcTemplate.queryForObject(
                "SELECT microscopy_exam FROM clinlims.pathology_sample WHERE id = ?", String.class, CASE_ID));
        assertEquals("and the narrative", List.of("Suspicious"), jdbcTemplate.queryForList(
                "SELECT value FROM clinlims.pathology_conclusion" + " WHERE pathology_sample_id = ? AND type = 'TEXT'",
                String.class, CASE_ID));
        assertEquals("and the pathologist", PATHOLOGIST_ID, storedPathologist());

        ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_STAGE_UNDER_REVIEW_ENABLED, "false");
        storeAt(PathologyStatus.UNDER_REVIEW);
        pathologySampleService.returnToPreviousStage(CASE_ID, PathologyStatus.UNDER_REVIEW, "Parked here",
                TECHNICIAN_ID);
        assertEquals("a case parked under review goes back to the queue", "READY_PATHOLOGIST", storedStage());

        pathologySampleService.returnToPreviousStage(CASE_ID, PathologyStatus.READY_PATHOLOGIST, "Recut",
                TECHNICIAN_ID);
        assertEquals("from the queue the case goes to the last bench stage", "COVERSLIPPING", storedStage());
    }

    @Test
    public void aRelease_fromReviewOrFromTheQueue_completesTheCase() {
        storeAt(PathologyStatus.READY_PATHOLOGIST);
        pathologySampleService.advance(CASE_ID, PathologyStatus.READY_PATHOLOGIST, PATHOLOGIST_ID);

        save(releaseForm());

        assertEquals("released under review, the case is complete", "COMPLETED", storedStage());
        Map<String, Object> review = stageEvents().stream().filter(event -> "UNDER_REVIEW".equals(event.get("stage")))
                .findFirst().orElseThrow();
        assertNotNull("the review's stage event is closed", review.get("completed_at"));

        assertRefusedWith("pathology.error.caseCompleted",
                () -> pathologySampleService.advance(CASE_ID, PathologyStatus.COMPLETED, PATHOLOGIST_ID));
        assertRefusedWith("pathology.error.caseCompleted", () -> pathologySampleService.returnToPreviousStage(CASE_ID,
                PathologyStatus.COMPLETED, "Too late", PATHOLOGIST_ID));

        for (String reviewSwitch : List.of("true", "false")) {
            PathologyCaseRows.clearStageHistory(jdbcTemplate, CASE_ID);
            clearCaseAudit();
            ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_STAGE_UNDER_REVIEW_ENABLED,
                    reviewSwitch);
            storeAt(PathologyStatus.READY_PATHOLOGIST);
            save(releaseForm());
            assertEquals("released from the queue with review " + reviewSwitch + ", the case is complete", "COMPLETED",
                    storedStage());
        }
    }

    @Test
    public void theCaseSaysWhereItGoesNext() {
        storeAt(PathologyStatus.READY_PATHOLOGIST);
        assertEquals("in the queue with review on", "UNDER_REVIEW", read().getNextStage());

        storeAt(PathologyStatus.UNDER_REVIEW);
        PathologyCaseViewDisplayItem underReview = read();
        assertNull("under review there is no next stage", underReview.getNextStage());
        assertEquals("and the previous one is the queue", "READY_PATHOLOGIST", underReview.getPreviousStage());

        ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_STAGE_UNDER_REVIEW_ENABLED, "false");
        storeAt(PathologyStatus.READY_PATHOLOGIST);
        assertNull("in the queue with review off there is none", read().getNextStage());
    }

    // helpers

    private PathologyCaseViewDisplayItem read() {
        return pathologyDisplayService.convertToCaseDisplayItem(CASE_ID);
    }

    private PathologySampleForm releaseForm() {
        PathologySampleForm form = caseViewForm();
        form.setRelease(true);
        return form;
    }

    private String storedPathologist() {
        Number id = jdbcTemplate.queryForObject("SELECT pathologist_id FROM clinlims.pathology_sample WHERE id = ?",
                Number.class, CASE_ID);
        return id == null ? null : String.valueOf(id.intValue());
    }

    private List<Map<String, Object>> stageEvents() {
        return jdbcTemplate.queryForList("SELECT id, stage, direction, entered_by, completed_at"
                + " FROM clinlims.pathology_stage_event WHERE pathology_sample_id = ? ORDER BY id", CASE_ID);
    }

    /** entered_by is numeric(10), which JDBC answers as a BigDecimal. */
    private static String operatorOf(Object operator) {
        return operator == null ? null : String.valueOf(((Number) operator).intValue());
    }

    private long entriesWithVerb(String verb) {
        List<History> named = caseAudit().stream().filter(history -> changesOf(history).startsWith("{"))
                .filter(history -> verb.equals(payloadOf(history).get("verb"))).collect(Collectors.toList());
        return named.size();
    }

    /** Each history line about one attribute as its old and new value. */
    private List<List<String>> historyLines(String attribute) {
        List<AuditTrailItem> items = new TransactionTemplate(transactionManager).execute(
                status -> new ProgramSampleHistoryService(pathologySampleService.get(CASE_ID)).getAuditTrailItems());
        return items.stream().filter(item -> attribute.equals(item.getIdentifier())).map(item -> List
                .of(StringUtils.defaultString(item.getOldValue()), StringUtils.defaultString(item.getNewValue())))
                .collect(Collectors.toList());
    }
}
