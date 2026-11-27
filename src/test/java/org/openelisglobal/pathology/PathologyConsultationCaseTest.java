package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.openelisglobal.pathology.PathologyConsultationFixture.CONSULTED;
import static org.openelisglobal.pathology.PathologyConsultationFixture.REQUESTER;
import static org.openelisglobal.pathology.PathologyConsultationFixture.TECHNICIAN;
import static org.openelisglobal.pathology.PathologyFindingsDictionary.ADENOCARCINOMA;
import static org.openelisglobal.pathology.PathologyFindingsDictionary.CHRONIC_GASTRITIS;
import static org.openelisglobal.pathology.PathologyFindingsDictionary.FROZEN_SECTION;

import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.service.ConsultationAnswer;
import org.openelisglobal.program.service.ConsultationDraft;
import org.openelisglobal.program.service.PathologyConsultationService;
import org.openelisglobal.program.valueholder.pathology.PathologyConsultation;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * What else the case does leaves a second opinion as it was, and a second
 * opinion holds nothing the case does (FR-11): the case save never touches the
 * rows, a return and a re-entry keep an awaited one, and the sign-out is held
 * neither by an awaited second opinion nor by the rule's suggestion (AC-22).
 */
public class PathologyConsultationCaseTest extends PathologyCaseTestSupport {

    private static final String READING = "Sheets of atypical cells";
    private static final String NARRATIVE = "Consistent with adenocarcinoma";

    private static final List<PathologyStatus> BENCH_STAGES = List.of(PathologyStatus.ACCESSIONED,
            PathologyStatus.GROSSING, PathologyStatus.DECALCIFICATION, PathologyStatus.PROCESSING,
            PathologyStatus.EMBEDDING, PathologyStatus.MICROTOMY, PathologyStatus.STAINING,
            PathologyStatus.COVERSLIPPING);

    @Autowired
    private PathologyConsultationService consultationService;

    private PathologyConsultationFixture fixture;
    private StageSwitches switches;
    private String ruleBefore;

    @Before
    public void seedThePeople() {
        switches = StageSwitches.allOn();
        fixture = PathologyConsultationFixture.seed(jdbcTemplate);
        ruleBefore = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.PATHOLOGY_SECOND_OPINION_SUGGEST_CODES);
        storeAt(PathologyStatus.UNDER_REVIEW);
    }

    @After
    public void removeThePeopleAndRestoreTheRule() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_SECOND_OPINION_SUGGEST_CODES,
                ruleBefore == null ? "" : ruleBefore);
        fixture.close();
        switches.close();
    }

    @Test
    public void aCaseSave_touchesNoConsultation() {
        PathologyConsultation answered = ask(REQUESTER, CONSULTED);
        consultationService.answer(CASE_ID, answered.getId(), new ConsultationAnswer("MODIFIED", "Grade 2"), CONSULTED);
        ask(TECHNICIAN, CONSULTED);
        ask(CONSULTED, REQUESTER);
        List<Map<String, Object>> before = consultationRows();
        assertEquals("setup: three rows", 3, before.size());

        save(caseViewForm());
        assertEquals("a save as loaded leaves the rows and their versions as they were", before, consultationRows());

        PathologySampleForm changed = caseViewForm();
        changed.setMicroscopyExam(READING);
        changed.setConclusionText(NARRATIVE);
        changed.setConclusions(List.of(ADENOCARCINOMA, CHRONIC_GASTRITIS));
        changed.setTechniques(List.of(FROZEN_SECTION));
        save(changed);
        assertEquals("setup: the save wrote the findings", 3,
                jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM clinlims.pathology_conclusion WHERE pathology_sample_id = ?",
                        Integer.class, CASE_ID).intValue());
        assertEquals("a save with changed findings leaves them as they were", before, consultationRows());
    }

    @Test
    public void aReturnToTheQueueAndToTheBench_keepsTheSecondOpinionAwaited_andTheServerTakesTheAnswerAtEachStop() {
        closeTheFixtureRequest();
        PathologyConsultation awaited = ask(REQUESTER, CONSULTED);

        assertAnswerTaken("under review", awaited);

        pathologySampleService.returnToPreviousStage(CASE_ID, PathologyStatus.UNDER_REVIEW, "Needs deeper levels",
                TECHNICIAN);
        assertEquals("setup: back in the queue", "READY_PATHOLOGIST", storedStage());
        assertAnswerTaken("in the queue", awaited);

        pathologySampleService.returnToPreviousStage(CASE_ID, PathologyStatus.READY_PATHOLOGIST, "Recut the block",
                TECHNICIAN);
        assertTrue("setup: back on the bench, at " + storedStage(),
                BENCH_STAGES.contains(PathologyStatus.valueOf(storedStage())));
        assertAnswerTaken("on the bench", awaited);

        storeAt(PathologyStatus.READY_PATHOLOGIST);
        assertAnswerTaken("in the queue again", awaited);

        pathologySampleService.advance(CASE_ID, PathologyStatus.READY_PATHOLOGIST, TECHNICIAN);
        assertEquals("setup: begun again", "UNDER_REVIEW", storedStage());
        assertAnswerTaken("under review again", awaited);

        assertEquals("the row was awaited throughout", "REQUESTED", consultationRow(awaited.getId()).get("status"));
        assertEquals("one row", 1, consultationRows().size());
    }

    @Test
    public void aSignOut_isNotHeldByAnAwaitedSecondOpinion_whichCanStillBeAnswered() {
        closeTheFixtureRequest();
        PathologyConsultation awaited = ask(REQUESTER, CONSULTED);

        signOut();

        assertEquals("the second opinion is still awaited", "REQUESTED",
                consultationRow(awaited.getId()).get("status"));

        PathologyConsultation answered = consultationService.answer(CASE_ID, awaited.getId(),
                new ConsultationAnswer("DISAGREE", "Favours a lymphoma"), CONSULTED);
        assertEquals("the outcome is recorded on the signed-out case", "ANSWERED",
                consultationRow(answered.getId()).get("status"));
        assertEquals("DISAGREE", consultationRow(answered.getId()).get("outcome"));
        assertEquals("the case stays signed out", "COMPLETED", storedStage());
    }

    @Test
    public void aSignOut_isNotHeldByASuggestion() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_SECOND_OPINION_SUGGEST_CODES, "C");
        PathologySampleForm findings = caseViewForm();
        findings.setConclusions(List.of(ADENOCARCINOMA));
        save(findings);
        assertEquals("setup: the rule suggests a second opinion", List.of("C80.1 Adenocarcinoma"), suggested());

        signOut();

        assertEquals("the sign-out is accepted", "COMPLETED", storedStage());
        assertEquals("and no second opinion was asked for on the way", 0, consultationRows().size());
    }

    // helpers

    private PathologyConsultation ask(String actor, String colleague) {
        return consultationService.request(CASE_ID, new ConsultationDraft(colleague, null, true), actor);
    }

    /**
     * The consulted pathologist's answer is taken where the case stands now; the
     * row is then put back as awaited so the next stop takes one as well.
     */
    private void assertAnswerTaken(String where, PathologyConsultation row) {
        assertEquals("awaited " + where, "REQUESTED", consultationRow(row.getId()).get("status"));
        PathologyConsultation answered = consultationService.answer(CASE_ID, row.getId(),
                new ConsultationAnswer("AGREE", null), CONSULTED);
        assertEquals("the answer is taken " + where, "ANSWERED", consultationRow(answered.getId()).get("status"));
        jdbcTemplate.update("UPDATE clinlims.pathology_consultation SET status = 'REQUESTED', outcome = NULL,"
                + " outcome_note = NULL, answered_at = NULL WHERE id = ?", row.getId());
    }

    private void signOut() {
        closeTheFixtureRequest();
        storeAt(PathologyStatus.UNDER_REVIEW);
        PathologySampleForm form = caseViewForm();
        form.setRelease(true);
        save(form);
        assertEquals("setup: the case is signed out", "COMPLETED", storedStage());
    }

    private List<String> suggested() {
        return new TransactionTemplate(transactionManager).execute(status -> {
            var pathologySample = pathologySampleService.get(CASE_ID);
            return consultationService.suggestedFor(pathologySample, consultationService.getForCase(CASE_ID));
        });
    }

    private List<Map<String, Object>> consultationRows() {
        return jdbcTemplate.queryForList("SELECT * FROM clinlims.pathology_consultation ORDER BY id");
    }

    private Map<String, Object> consultationRow(int id) {
        return jdbcTemplate.queryForMap("SELECT * FROM clinlims.pathology_consultation WHERE id = ?", id);
    }
}
