package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;

/**
 * Sign-out is the case save with {@code release}: the rules it checks, in the
 * order they fire, and what it writes (design table S). The final report the
 * controller asks for after the save is {@link PathologySignOutReportTest}'s
 * subject.
 */
public class PathologySignOutTest extends PathologyCaseTestSupport {

    /** The fixture's narrative conclusion on case 1, "Benign Tumor". */
    private static final int FIXTURE_NARRATIVE_ID = 501;

    private StageSwitches switches;
    private PathologyFindingsDictionary dictionary;

    @Before
    public void everythingButTheCode() {
        switches = StageSwitches.allOn();
        dictionary = PathologyFindingsDictionary.seed(jdbcTemplate);
        closeTheFixtureRequest();
        storeAt(PathologyStatus.UNDER_REVIEW);
    }

    @After
    public void putTheSwitchesBack() {
        dictionary.close();
        switches.close();
    }

    // S1
    @Test
    public void signingOut_movesTheCase_andSaysNoCriticalResultWasEmitted() {
        save(release(PathologyFindingsDictionary.CHRONIC_GASTRITIS));

        assertSignedOutFrom("UNDER_REVIEW");
        assertEquals("the code is stored with the sign-out", List.of(PathologyFindingsDictionary.CHRONIC_GASTRITIS),
                storedCodes());
        assertEquals("and no critical result was raised", Boolean.FALSE,
                signOutEntries().get(0).get("criticalResultEmitted"));
        assertTrue("no critical result entry is written",
                verbs().stream().noneMatch(PathologySampleService.PATHOLOGY_CRITICAL_RESULT_EMITTED::equals));
    }

    // S2, S3, S4
    @Test
    public void signingOut_needsAConclusion_afterTheDescriptions() {
        // S3: no code and the narrative posted blank
        PathologySampleForm blank = release();
        blank.setConclusionText("   ");
        assertRefusedWith("pathology.error.releaseWithoutConclusion", () -> save(blank));
        assertNothingWritten();

        // S3: no code and no narrative row at all
        removeTheNarrative();
        assertRefusedWith("pathology.error.releaseWithoutConclusion", () -> save(release()));
        assertNothingWritten();

        // S4: a blank description and no conclusion: the descriptions rule fires first
        PathologySampleForm noDescription = release();
        noDescription.setMicroscopyExam(" ");
        assertRefusedWith("pathology.error.releaseWithoutDescriptions", () -> save(noDescription));
        assertNothingWritten();

        // S2: a narrative alone is a conclusion
        PathologySampleForm narrative = release();
        narrative.setConclusionText("Reactive changes only");
        save(narrative);
        assertSignedOutFrom("UNDER_REVIEW");
    }

    // S5
    @Test
    public void signingOut_isHeldByAnOpenRequest_notByAClosedOne() {
        storeRequestStatus("OPENED");
        removeTheNarrative();
        assertRefusedWith("the missing conclusion fires before the open request",
                "pathology.error.releaseWithoutConclusion", release());

        LocalizedValidationException held = assertRefusedWith("an open request holds the sign-out",
                "pathology.error.releaseWithOpenRequests", release(PathologyFindingsDictionary.CHRONIC_GASTRITIS));
        assertEquals("naming how many", Map.of("count", "1"), held.getParams());
        assertNothingWritten();

        storeRequestStatus("CANCELLED");
        save(release(PathologyFindingsDictionary.CHRONIC_GASTRITIS));
        assertSignedOutFrom("UNDER_REVIEW");

        PathologyCaseRows.clearStageHistory(jdbcTemplate, CASE_ID);
        storeAt(PathologyStatus.UNDER_REVIEW);
        storeRequestStatus("COMPLETED");
        save(release());
        assertSignedOutFrom("UNDER_REVIEW");
    }

    // S6, S7
    @Test
    public void signingOut_fromTheQueue_isAsToday() {
        for (String reviewSwitch : List.of("false", "true")) {
            PathologyCaseRows.clearStageHistory(jdbcTemplate, CASE_ID);
            ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_STAGE_UNDER_REVIEW_ENABLED,
                    reviewSwitch);
            storeAt(PathologyStatus.READY_PATHOLOGIST);

            // The queue takes no findings where Under review is on, so the stored
            // narrative is the conclusion.
            save(release());

            assertSignedOutFrom("READY_PATHOLOGIST");
        }
    }

    // S8
    @Test
    public void aLegacyCompletedCase_isSignedOut_evenWithAnOpenRequest() {
        storeAt(PathologyStatus.COMPLETED);
        storeRequestStatus("OPENED");

        save(release(PathologyFindingsDictionary.CHRONIC_GASTRITIS));

        assertEquals("COMPLETED", storedStage());
        assertEquals("no entry onto the stage it already stands at", 0, stageEventCount());
        assertEquals("the sign-out is audited once, from Completed", List.of("COMPLETED"),
                signOutEntries().stream().map(entry -> entry.get("stageFrom")).collect(Collectors.toList()));
        assertTrue("and the case now reads as signed out",
                pathologySampleService.isSignedOut(pathologySampleService.get(CASE_ID)));
    }

    // S9, S14
    @Test
    public void theOldRefusals_standAsTheyWere() {
        save(release());
        assertRefusedWith("pathology.error.caseCompleted", () -> save(release()));
        assertEquals("the second release writes nothing", 1, signOutEntries().size());

        PathologyCaseRows.clearStageHistory(jdbcTemplate, CASE_ID);
        for (PathologyStatus bench : List.of(PathologyStatus.GROSSING, PathologyStatus.COVERSLIPPING)) {
            storeAt(bench);
            assertRefusedWith("pathology.error.releaseBeforeReview", () -> save(release()));
            assertEquals(bench.name(), storedStage());
        }
        assertNothingWritten();
    }

    // S10
    @Test
    public void editsAndSignOut_areOneSave() {
        removeTheNarrative();
        PathologySampleForm form = release(PathologyFindingsDictionary.CHRONIC_GASTRITIS);
        form.setMicroscopyExam("Chronic inflammation of the mucosa");

        save(form);

        assertSignedOutFrom("UNDER_REVIEW");
        assertEquals("the reading is stored", "Chronic inflammation of the mucosa", jdbcTemplate.queryForObject(
                "SELECT microscopy_exam FROM clinlims.pathology_sample WHERE id = ?", String.class, CASE_ID));
        assertEquals("with the code the same save added", List.of(PathologyFindingsDictionary.CHRONIC_GASTRITIS),
                storedCodes());
        assertTrue("and the conclusion is audited by that save",
                verbs().contains(PathologySampleService.PATHOLOGY_CONCLUSION_SAVED));
    }

    // S11
    @Test
    public void twoSignOutsAtOnce_signOutOnce() {
        Outcome outcome = holdOpenWhile(() -> save(release()), () -> {
            save(release());
            return null;
        });

        assertNull("the first sign-out stands", outcome.held());
        assertTrue("the second is refused", outcome.other() != null);
        assertEquals("COMPLETED", storedStage());
        assertEquals("one entry onto Completed", 1, stageEventCount());
        assertEquals("and one sign-out", 1, signOutEntries().size());
    }

    // helpers

    /** A release posting the stored descriptions and, when given, the codes. */
    private PathologySampleForm release(String... codes) {
        PathologySampleForm form = caseViewForm();
        form.setRelease(true);
        if (codes.length > 0) {
            form.setConclusions(List.of(codes));
        }
        return form;
    }

    private LocalizedValidationException assertRefusedWith(String what, String errorCode, PathologySampleForm form) {
        LocalizedValidationException refusal = assertRefusedWith(errorCode, () -> save(form));
        assertEquals(what, errorCode, refusal.getErrorCode());
        return refusal;
    }

    private void assertSignedOutFrom(String stageFrom) {
        assertEquals("the case is signed out", "COMPLETED", storedStage());
        List<Map<String, Object>> events = jdbcTemplate.queryForList(
                "SELECT stage, direction FROM clinlims.pathology_stage_event WHERE pathology_sample_id = ?"
                        + " AND completed_at IS NULL",
                CASE_ID);
        assertEquals("with one open entry, forward onto Completed",
                List.of(Map.of("stage", "COMPLETED", "direction", "FORWARD")), events);
        List<Map<String, Object>> signOuts = signOutEntries();
        assertEquals("audited as one sign-out", 1, signOuts.size());
        assertEquals("from the stage the case left", stageFrom, signOuts.get(0).get("stageFrom"));
    }

    private void assertNothingWritten() {
        assertFalse("the case is not signed out", "COMPLETED".equals(storedStage()));
        assertEquals("no stage entry is written", 0, stageEventCount());
        assertTrue("and nothing is audited", caseAudit().isEmpty());
    }

    private int stageEventCount() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinlims.pathology_stage_event WHERE pathology_sample_id = ?", Integer.class,
                CASE_ID);
    }

    private List<Map<String, Object>> signOutEntries() {
        return caseAudit().stream().map(this::changesOrEmpty)
                .filter(payload -> PathologySampleService.PATHOLOGY_CASE_SIGNED_OUT.equals(payload.get("verb")))
                .collect(Collectors.toList());
    }

    private List<Object> verbs() {
        return caseAudit().stream().map(this::changesOrEmpty).map(payload -> payload.get("verb"))
                .collect(Collectors.toList());
    }

    /** A named event's payload; the case's own save diffs are XML and name none. */
    private Map<String, Object> changesOrEmpty(org.openelisglobal.audittrail.valueholder.History history) {
        return changesOf(history).trim().startsWith("{") ? payloadOf(history) : Map.of();
    }

    private List<String> storedCodes() {
        return jdbcTemplate.queryForList("SELECT value FROM clinlims.pathology_conclusion WHERE pathology_sample_id = ?"
                + " AND type = 'DICTIONARY' ORDER BY id", String.class, CASE_ID);
    }

    private void removeTheNarrative() {
        jdbcTemplate.update("DELETE FROM clinlims.pathology_conclusion WHERE id = ?", FIXTURE_NARRATIVE_ID);
    }

    private void storeRequestStatus(String status) {
        jdbcTemplate.update("UPDATE clinlims.pathology_request SET status = ? WHERE id = ?", status,
                FIXTURE_REQUEST_ID);
    }
}
