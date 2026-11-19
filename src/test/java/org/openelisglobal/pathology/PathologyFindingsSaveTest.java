package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.audittrail.action.workers.AuditTrailItem;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.services.historyservices.ProgramSampleHistoryService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.service.PathologyCaseRuleException;
import org.openelisglobal.program.service.PathologyDisplayService;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.valueholder.pathology.ConclusionEntry;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The case save's rules for the review and the findings (FR-12, FR-13): which
 * fields a save takes where the case stands, what counts as a change, how coded
 * conclusions and techniques are stored, and what the audit trail records.
 *
 * <p>
 * Every section of the case view posts the whole case, so a save that carries
 * the findings as stored must pass anywhere, and only a real change outside the
 * reading window is refused, with nothing written.
 */
public class PathologyFindingsSaveTest extends PathologyCaseTestSupport {

    private static final String READING = "Atypical glands infiltrating the stroma";
    private static final String NARRATIVE = "Consistent with an adenocarcinoma";
    private static final String NOTES = "Discussed with the surgeon";
    private static final String SECOND_PATHOLOGIST_ID = "1003";

    private static final List<PathologyStatus> BENCH_STAGES = List.of(PathologyStatus.ACCESSIONED,
            PathologyStatus.GROSSING, PathologyStatus.DECALCIFICATION, PathologyStatus.PROCESSING,
            PathologyStatus.EMBEDDING, PathologyStatus.MICROTOMY, PathologyStatus.STAINING,
            PathologyStatus.COVERSLIPPING);

    @Autowired
    private PathologyDisplayService pathologyDisplayService;

    private StageSwitches switches;

    private PathologyFindingsDictionary dictionary;

    @Before
    public void seedTheDictionary() {
        switches = StageSwitches.allOn();
        removeReferrals(jdbcTemplate);
        dictionary = PathologyFindingsDictionary.seed(jdbcTemplate);
    }

    @After
    public void removeTheDictionary() {
        removeReferrals(jdbcTemplate);
        dictionary.close();
        switches.close();
    }

    // what is stored and read back

    @Test
    public void findingsAreStoredAndReadBack() {
        storeAt(PathologyStatus.UNDER_REVIEW);
        PathologySampleForm form = caseViewForm();
        form.setMicroscopyExam(READING);
        form.setConclusionText(NARRATIVE);
        form.setConclusions(List.of(PathologyFindingsDictionary.ADENOCARCINOMA));
        form.setTechniques(List.of(PathologyFindingsDictionary.FROZEN_SECTION));
        form.setReviewNotes(NOTES);

        save(form);

        PathologyCaseViewDisplayItem read = pathologyDisplayService.convertToCaseDisplayItem(CASE_ID);
        assertEquals("the microscopic description", READING, read.getMicroscopyExam());
        assertEquals("the narrative conclusion", NARRATIVE, read.getConclusionText());
        assertEquals("the coded conclusion", List.of(PathologyFindingsDictionary.ADENOCARCINOMA),
                read.getConclusions().stream().map(ConclusionEntry::id).collect(Collectors.toList()));
        assertEquals("the technique", List.of(PathologyFindingsDictionary.FROZEN_SECTION),
                read.getTechniques().stream().map(IdValuePair::getId).collect(Collectors.toList()));
        assertEquals("the review notes", NOTES, read.getReviewNotes());
    }

    @Test
    public void codedConclusionsAreStoredInThePostedOrder_afterOneTextRow() {
        storeAt(PathologyStatus.UNDER_REVIEW);
        PathologySampleForm form = caseViewForm();
        form.setConclusionText(NARRATIVE);
        form.setConclusions(
                List.of(PathologyFindingsDictionary.CHRONIC_GASTRITIS, PathologyFindingsDictionary.ADENOCARCINOMA));

        save(form);

        assertEquals("one text row, then one row per code in the order posted",
                List.of(List.of("TEXT", NARRATIVE),
                        List.of("DICTIONARY", PathologyFindingsDictionary.CHRONIC_GASTRITIS),
                        List.of("DICTIONARY", PathologyFindingsDictionary.ADENOCARCINOMA)),
                typedValues(conclusionRows()));
    }

    // the reading window (W1 to W7)

    @Test
    public void onTheBench_aChangedFindingIsRefused_andACaseFieldIsTaken() {
        for (PathologyStatus stage : BENCH_STAGES) {
            storeAt(stage);
            assertEveryFindingRefused("at " + stage, "pathology.error.findingsLocked");
            assertCaseFieldsTaken("at " + stage);
        }
        assertEquals("no refused save stored a microscopic description", "Microscopic details of sample",
                storedText("microscopy_exam"));
        assertEquals("nor a conclusion row", List.of(List.of("TEXT", "Benign Tumor")), typedValues(conclusionRows()));
        assertEquals("nor a referral", 0, referralCount(jdbcTemplate));
    }

    @Test
    public void inTheQueue_aChangedFindingIsRefused() {
        storeAt(PathologyStatus.READY_PATHOLOGIST);

        assertEveryFindingRefused("in the queue where review is on", "pathology.error.findingsLocked");
        assertCaseFieldsTaken("in the queue");
        assertEquals("nothing of the findings is stored", "Microscopic details of sample",
                storedText("microscopy_exam"));
    }

    @Test
    public void whereReviewIsSwitchedOff_theQueueIsTheReadingStage() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_STAGE_UNDER_REVIEW_ENABLED, "false");
        storeAt(PathologyStatus.READY_PATHOLOGIST);

        assertEveryFindingTaken("in the queue where review is off", PathologyFindingsDictionary.ADENOCARCINOMA);
    }

    @Test
    public void underReview_findingsAreTaken_alsoOnACaseParkedThere() {
        storeAt(PathologyStatus.UNDER_REVIEW);
        assertEveryFindingTaken("under review", PathologyFindingsDictionary.ADENOCARCINOMA);

        ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_STAGE_UNDER_REVIEW_ENABLED, "false");
        assertEveryFindingTaken("on a case parked under review", PathologyFindingsDictionary.CHRONIC_GASTRITIS);
    }

    @Test
    public void aCompletedCaseNeverReleased_stillTakesFindings() {
        storeAt(PathologyStatus.COMPLETED);

        assertEveryFindingTaken("at Completed without a release", PathologyFindingsDictionary.ADENOCARCINOMA);
    }

    @Test
    public void aSignedOutCase_takesNoReviewOrFindingsChange() {
        signOut();

        Map<String, Consumer<PathologySampleForm>> everyField = new LinkedHashMap<>(findingsChanges());
        everyField.putAll(caseFieldChanges("after the sign-out"));
        assertEquals("eight fields", 8, everyField.size());
        everyField.forEach((field, change) -> {
            PathologySampleForm form = caseViewForm();
            change.accept(form);
            assertRefusedWith("pathology.error.caseCompleted", () -> save(form));
        });
        assertEquals("the signed-out description stands", "Microscopic details of sample",
                storedText("microscopy_exam"));
        assertEquals("and the macroscopic one", "Normal tissues observed", storedText("gross_exam"));
        assertNull("no notes were taken", storedText("review_notes"));
        assertEquals("the pathologist is kept", "1002", storedText("pathologist_id"));
        assertEquals("and no referral made", 0, referralCount(jdbcTemplate));
    }

    // leaving and re-entering the window (L1 to L6)

    @Test
    public void aBenchSaveOfTheCaseAsLoaded_leavesTheFindingsAndTheirRows() {
        writeFindingsUnderReview();
        returnToTheQueue();
        List<Object> conclusionIds = ids(conclusionRows());
        List<Object> techniqueIds = ids(techniqueRows());
        clearCaseAudit();

        save(asLoaded());

        assertEquals("the reading is as it was", READING, storedText("microscopy_exam"));
        assertEquals("the conclusion rows keep their ids", conclusionIds, ids(conclusionRows()));
        assertEquals("and so do the technique rows", techniqueIds, ids(techniqueRows()));
        assertTrue("no conclusion is audited", conclusionAudit().isEmpty());
    }

    @Test
    public void savingTheCodesAsLoaded_inAnotherOrderThanTheirNames_isNotAChange() {
        storeAt(PathologyStatus.UNDER_REVIEW);
        jdbcTemplate.update("INSERT INTO clinlims.pathology_technique (id, pathology_sample_id, type, value,"
                + " last_updated) VALUES (nextval('clinlims.pathology_technique_seq'), ?, 'DICTIONARY', ?, now())",
                CASE_ID, PathologyFindingsDictionary.OLD_TECHNIQUE);
        PathologySampleForm reading = caseViewForm();
        reading.setConclusions(
                List.of(PathologyFindingsDictionary.CHRONIC_GASTRITIS, PathologyFindingsDictionary.ADENOCARCINOMA));
        reading.setTechniques(
                List.of(PathologyFindingsDictionary.OLD_TECHNIQUE, PathologyFindingsDictionary.FROZEN_SECTION));
        save(reading);
        List<Object> conclusionIds = ids(conclusionRows());
        List<Object> techniqueIds = ids(techniqueRows());

        PathologyCaseViewDisplayItem read = pathologyDisplayService.convertToCaseDisplayItem(CASE_ID);
        List<String> loadedCodes = read.getConclusions().stream().map(ConclusionEntry::id).collect(Collectors.toList());
        List<String> loadedTechniques = read.getTechniques().stream().map(IdValuePair::getId)
                .collect(Collectors.toList());
        assertEquals("the codes read back in the order posted",
                List.of(PathologyFindingsDictionary.CHRONIC_GASTRITIS, PathologyFindingsDictionary.ADENOCARCINOMA),
                loadedCodes);
        assertEquals("and so do the techniques",
                List.of(PathologyFindingsDictionary.OLD_TECHNIQUE, PathologyFindingsDictionary.FROZEN_SECTION),
                loadedTechniques);

        storeAt(PathologyStatus.GROSSING);
        clearCaseAudit();
        PathologySampleForm benchSave = caseViewForm();
        benchSave.setConclusions(loadedCodes);
        benchSave.setTechniques(loadedTechniques);
        save(benchSave);

        assertEquals("the conclusion rows keep their ids, in order", conclusionIds, ids(conclusionRows()));
        assertEquals("and so do the technique rows", techniqueIds, ids(techniqueRows()));
        assertEquals("the codes keep their order",
                List.of(PathologyFindingsDictionary.CHRONIC_GASTRITIS, PathologyFindingsDictionary.ADENOCARCINOMA),
                codesOf(conclusionRows()));
        assertEquals("and so do the techniques",
                List.of(PathologyFindingsDictionary.OLD_TECHNIQUE, PathologyFindingsDictionary.FROZEN_SECTION),
                codesOf(techniqueRows()));
        assertTrue("no conclusion is audited", conclusionAudit().isEmpty());
    }

    /**
     * Rows written with the higher id first sit on the heap in the reverse of their
     * id order, so only an ordered read gives them back by id.
     */
    @Test
    public void codesStoredOutOfIdOrder_areReadBackInIdOrder() {
        jdbcTemplate.update("DELETE FROM clinlims.pathology_conclusion WHERE pathology_sample_id = ?", CASE_ID);
        jdbcTemplate.update("DELETE FROM clinlims.pathology_technique WHERE pathology_sample_id = ?", CASE_ID);
        try {
            insertHigherIdFirst("pathology_conclusion", PathologyFindingsDictionary.ADENOCARCINOMA,
                    PathologyFindingsDictionary.CHRONIC_GASTRITIS);
            insertHigherIdFirst("pathology_technique", PathologyFindingsDictionary.FROZEN_SECTION,
                    PathologyFindingsDictionary.OLD_TECHNIQUE);

            PathologyCaseViewDisplayItem read = pathologyDisplayService.convertToCaseDisplayItem(CASE_ID);

            assertEquals("the conclusions read back in id order, not in the order written",
                    List.of(PathologyFindingsDictionary.CHRONIC_GASTRITIS, PathologyFindingsDictionary.ADENOCARCINOMA),
                    read.getConclusions().stream().map(ConclusionEntry::id).collect(Collectors.toList()));
            assertEquals("and so do the techniques",
                    List.of(PathologyFindingsDictionary.OLD_TECHNIQUE, PathologyFindingsDictionary.FROZEN_SECTION),
                    read.getTechniques().stream().map(IdValuePair::getId).collect(Collectors.toList()));
        } finally {
            jdbcTemplate.update("DELETE FROM clinlims.pathology_conclusion WHERE pathology_sample_id = ?", CASE_ID);
            jdbcTemplate.update("DELETE FROM clinlims.pathology_technique WHERE pathology_sample_id = ?", CASE_ID);
        }
    }

    @Test
    public void returnedAndBegunAgain_theFindingsAreEditable() {
        writeFindingsUnderReview();
        returnToTheQueue();
        pathologySampleService.advance(CASE_ID, PathologyStatus.READY_PATHOLOGIST, TECHNICIAN_ID);

        assertEquals("the findings are there", READING, storedText("microscopy_exam"));
        PathologySampleForm form = asLoaded();
        form.setMicroscopyExam("A second reading");
        save(form);
        assertEquals("and editable again", "A second reading", storedText("microscopy_exam"));
    }

    @Test
    public void aBenchSaveWithADifferentReading_isRefused_andNothingIsStored() {
        writeFindingsUnderReview();
        returnToTheQueue();
        PathologySampleForm form = asLoaded();
        form.setMicroscopyExam("A reading typed behind a stale page");
        form.setGrossExam("A macroscopic change in the same save");

        assertRefusedWith("pathology.error.findingsLocked", () -> save(form));

        assertEquals("the stored reading is unchanged", READING, storedText("microscopy_exam"));
        assertEquals("and so is the rest of the save", "Normal tissues observed", storedText("gross_exam"));
    }

    @Test
    public void aSaveOfASignedOutCaseAsStored_isAccepted_andChangesNoFinding() {
        writeFindingsUnderReview();
        signOut();
        List<Object> conclusionIds = ids(conclusionRows());
        List<Object> techniqueIds = ids(techniqueRows());
        clearCaseAudit();

        save(asLoaded());

        assertEquals("the reading stands", READING, storedText("microscopy_exam"));
        assertEquals("and the notes", NOTES, storedText("review_notes"));
        assertEquals("the conclusion rows keep their ids", conclusionIds, ids(conclusionRows()));
        assertEquals("and the technique rows", techniqueIds, ids(techniqueRows()));
        assertTrue("no conclusion verb", conclusionAudit().isEmpty());
    }

    @Test
    public void aSaveThatOmitsTheReviewAndFindingsKeys_changesNoneOfThem() {
        writeFindingsUnderReview();
        List<Object> conclusionIds = ids(conclusionRows());

        for (PathologyStatus stage : List.of(PathologyStatus.UNDER_REVIEW, PathologyStatus.GROSSING,
                PathologyStatus.READY_PATHOLOGIST)) {
            storeAt(stage);
            PathologySampleForm bare = new PathologySampleForm();
            bare.setSystemUserId(TECHNICIAN_ID);
            bare.setBlocks(new ArrayList<>());
            bare.setSlides(new ArrayList<>());
            bare.setReports(new ArrayList<>());
            save(bare);

            assertEquals("at " + stage + ", the macroscopic description", "Normal tissues observed",
                    storedText("gross_exam"));
            assertEquals("the microscopic one", READING, storedText("microscopy_exam"));
            assertEquals("the notes", NOTES, storedText("review_notes"));
            assertEquals("the pathologist", "1002", storedText("pathologist_id"));
            assertEquals("the conclusion rows", conclusionIds, ids(conclusionRows()));
            assertEquals("the techniques", List.of(PathologyFindingsDictionary.FROZEN_SECTION),
                    codesOf(techniqueRows()));
        }
    }

    @Test
    public void anEditSavedAfterAColleagueSignedOut_isRefused() {
        writeFindingsUnderReview();
        PathologySampleForm openPage = asLoaded();
        openPage.setMicroscopyExam("An edit typed before the sign-out");
        signOut();

        assertRefusedWith("pathology.error.caseCompleted", () -> save(openPage));

        assertEquals("the edit is not stored", READING, storedText("microscopy_exam"));
    }

    // what counts as a change (C1 to C18)

    @Test
    public void aNullTextIsKept_anEmptyOneClears_andBlankOverNothingIsNoChange() {
        storeAt(PathologyStatus.UNDER_REVIEW);
        PathologySampleForm notSent = caseViewForm();
        save(notSent);
        assertEquals("a description not sent is kept", "Microscopic details of sample", storedText("microscopy_exam"));

        PathologySampleForm cleared = caseViewForm();
        cleared.setMicroscopyExam("");
        save(cleared);
        assertTrue("an empty one clears it", StringUtils.isEmpty(storedText("microscopy_exam")));

        jdbcTemplate.update("UPDATE clinlims.pathology_sample SET microscopy_exam = NULL WHERE id = ?", CASE_ID);
        storeAt(PathologyStatus.GROSSING);
        PathologySampleForm blankOverNothing = caseViewForm();
        blankOverNothing.setMicroscopyExam("");
        save(blankOverNothing);
        assertNull("an empty text over none is no change, so the bench may send it", storedText("microscopy_exam"));
    }

    @Test
    public void unchangedCodes_keepTheirRows_andWriteNoAudit() {
        storeAt(PathologyStatus.UNDER_REVIEW);
        saveCodes(List.of(PathologyFindingsDictionary.ADENOCARCINOMA, PathologyFindingsDictionary.CHRONIC_GASTRITIS));
        List<Object> rowIds = ids(conclusionRows());
        clearCaseAudit();

        save(caseViewForm());
        assertEquals("codes not sent are kept, row for row", rowIds, ids(conclusionRows()));

        saveCodes(List.of(PathologyFindingsDictionary.ADENOCARCINOMA, PathologyFindingsDictionary.CHRONIC_GASTRITIS));
        assertEquals("codes sent as stored keep their rows", rowIds, ids(conclusionRows()));
        assertTrue("and neither save is audited as a conclusion", conclusionAudit().isEmpty());
    }

    @Test
    public void reorderedOrClearedCodes_areRewritten_andAudited() {
        storeAt(PathologyStatus.UNDER_REVIEW);
        saveCodes(List.of(PathologyFindingsDictionary.ADENOCARCINOMA, PathologyFindingsDictionary.CHRONIC_GASTRITIS));
        clearCaseAudit();

        saveCodes(List.of(PathologyFindingsDictionary.CHRONIC_GASTRITIS, PathologyFindingsDictionary.ADENOCARCINOMA));
        assertEquals("reordered, the second code is first",
                List.of(PathologyFindingsDictionary.CHRONIC_GASTRITIS, PathologyFindingsDictionary.ADENOCARCINOMA),
                codesOf(conclusionRows()));
        Map<String, Object> reordered = payloadOf(conclusionAudit().get(0));
        assertEquals("the audit names the codes after the save",
                List.of(PathologyFindingsDictionary.CHRONIC_GASTRITIS, PathologyFindingsDictionary.ADENOCARCINOMA),
                reordered.get("coded"));
        assertEquals("none added", List.of(), reordered.get("added"));
        assertEquals("none removed", List.of(), reordered.get("removed"));
        assertEquals("the narrative unchanged", false, reordered.get("narrativeChanged"));

        clearCaseAudit();
        saveCodes(List.of());
        assertEquals("cleared, no code is left", List.of(), codesOf(conclusionRows()));
        Map<String, Object> cleared = payloadOf(conclusionAudit().get(0));
        assertEquals("the audit names both as removed",
                List.of(PathologyFindingsDictionary.CHRONIC_GASTRITIS, PathologyFindingsDictionary.ADENOCARCINOMA),
                cleared.get("removed"));
        assertEquals("and none coded", List.of(), cleared.get("coded"));
    }

    @Test
    public void aCodeSentTwice_isStoredOnce() {
        storeAt(PathologyStatus.UNDER_REVIEW);

        saveCodes(List.of(PathologyFindingsDictionary.ADENOCARCINOMA, PathologyFindingsDictionary.ADENOCARCINOMA,
                PathologyFindingsDictionary.CHRONIC_GASTRITIS));

        assertEquals("each code once, the first place kept",
                List.of(PathologyFindingsDictionary.ADENOCARCINOMA, PathologyFindingsDictionary.CHRONIC_GASTRITIS),
                codesOf(conclusionRows()));
    }

    @Test
    public void aRetiredCodeAlreadyOnTheCaseStays_aNewOneIsRefusedByName() {
        storeAt(PathologyStatus.UNDER_REVIEW);
        insertConclusionRow(PathologyFindingsDictionary.OLD_TERM);

        saveCodes(List.of(PathologyFindingsDictionary.ADENOCARCINOMA, PathologyFindingsDictionary.OLD_TERM));
        assertEquals("a retired code the case holds is kept",
                List.of(PathologyFindingsDictionary.ADENOCARCINOMA, PathologyFindingsDictionary.OLD_TERM),
                codesOf(conclusionRows()));

        saveCodes(List.of());
        LocalizedValidationException refusal = assertRefusedWith("pathology.error.conclusionNotInUse",
                () -> saveCodes(List.of(PathologyFindingsDictionary.OLD_TERM)));
        assertEquals("named, so the reader knows which one", Map.of("name", "Old term"), refusal.getParams());
        assertEquals("and not stored", List.of(), codesOf(conclusionRows()));
    }

    @Test
    public void aCodeThatIsNoDiagnosisEntry_isRefused() {
        storeAt(PathologyStatus.UNDER_REVIEW);

        for (String code : List.of(PathologyFindingsDictionary.ANY_STAIN, "adenocarcinoma",
                PathologyFindingsDictionary.NO_ENTRY)) {
            assertThrows("a code of " + code + " is refused with the reload sentence", PathologyCaseRuleException.class,
                    () -> saveCodes(List.of(code)));
        }
        assertEquals("nothing is stored", List.of(), codesOf(conclusionRows()));
    }

    @Test
    public void aStoredCodeThatNamesNoEntry_isNotCountedAsAChange() {
        storeAt(PathologyStatus.UNDER_REVIEW);
        saveCodes(List.of(PathologyFindingsDictionary.ADENOCARCINOMA));
        insertConclusionRow(PathologyFindingsDictionary.NO_ENTRY);
        List<Object> rowIds = ids(conclusionRows());
        storeAt(PathologyStatus.GROSSING);

        saveCodes(List.of(PathologyFindingsDictionary.ADENOCARCINOMA));

        assertEquals("the save is taken on the bench and no row is touched", rowIds, ids(conclusionRows()));
    }

    @Test
    public void aNarrativeLongerThanTheColumn_isRefused() {
        storeAt(PathologyStatus.UNDER_REVIEW);
        PathologySampleForm tooLong = caseViewForm();
        tooLong.setConclusionText(StringUtils.repeat("n", 256));

        assertRefusedWith("pathology.error.conclusionTooLong", () -> save(tooLong));

        PathologySampleForm fits = caseViewForm();
        fits.setConclusionText(StringUtils.repeat("n", 255));
        save(fits);
        assertEquals("255 characters fit", List.of(List.of("TEXT", StringUtils.repeat("n", 255))),
                typedValues(conclusionRows()));
    }

    @Test
    public void aChangedNarrative_isAuditedWithoutItsText_andABlankOneOverNothingWritesNoRow() {
        storeAt(PathologyStatus.UNDER_REVIEW);
        saveCodes(List.of(PathologyFindingsDictionary.ADENOCARCINOMA));
        clearCaseAudit();
        PathologySampleForm form = caseViewForm();
        form.setConclusionText("Malignant, said only in the narrative");

        save(form);

        assertEquals("one text row with the new narrative, then the codes",
                List.of(List.of("TEXT", "Malignant, said only in the narrative"),
                        List.of("DICTIONARY", PathologyFindingsDictionary.ADENOCARCINOMA)),
                typedValues(conclusionRows()));
        History audit = conclusionAudit().get(0);
        assertEquals("the audit says the narrative changed", true, payloadOf(audit).get("narrativeChanged"));
        assertEquals("the codes are as they were", List.of(PathologyFindingsDictionary.ADENOCARCINOMA),
                payloadOf(audit).get("coded"));
        assertFalse("and it carries no text: " + changesOf(audit), changesOf(audit).contains("Malignant"));

        jdbcTemplate.update("DELETE FROM clinlims.pathology_conclusion WHERE pathology_sample_id = ?", CASE_ID);
        clearCaseAudit();
        PathologySampleForm blank = caseViewForm();
        blank.setConclusionText("");
        save(blank);
        assertTrue("a blank narrative over none writes no row", conclusionRows().isEmpty());
        assertTrue("and no audit", conclusionAudit().isEmpty());
    }

    @Test
    public void techniquesFollowTheSameRules_andWriteNoVerb() {
        storeAt(PathologyStatus.UNDER_REVIEW);
        clearCaseAudit();

        saveTechniques(List.of(PathologyFindingsDictionary.FROZEN_SECTION, PathologyFindingsDictionary.FROZEN_SECTION));
        assertEquals("a technique sent twice is stored once", List.of(PathologyFindingsDictionary.FROZEN_SECTION),
                codesOf(techniqueRows()));
        assertTrue("and the fixture's text technique goes with the rewrite",
                typedValues(techniqueRows()).stream().noneMatch(row -> "TEXT".equals(row.get(0))));
        List<Object> rowIds = ids(techniqueRows());

        saveTechniques(null);
        saveTechniques(List.of(PathologyFindingsDictionary.FROZEN_SECTION));
        assertEquals("not sent or sent as stored, the row keeps its id", rowIds, ids(techniqueRows()));

        assertRefusedWith("pathology.error.techniqueNotInUse", () -> saveTechniques(
                List.of(PathologyFindingsDictionary.FROZEN_SECTION, PathologyFindingsDictionary.OLD_TECHNIQUE)));
        assertThrows("a diagnosis is no technique", PathologyCaseRuleException.class,
                () -> saveTechniques(List.of(PathologyFindingsDictionary.ADENOCARCINOMA)));

        saveTechniques(List.of());
        assertEquals("cleared", List.of(), codesOf(techniqueRows()));
        assertTrue("and no technique save is a conclusion verb", conclusionAudit().isEmpty());
    }

    @Test
    public void aBlankPathologistIsKept_andReviewNotesReadOnTheCaseHistory() {
        storeAt(PathologyStatus.UNDER_REVIEW);
        clearCaseAudit();
        PathologySampleForm form = caseViewForm();
        form.setAssignedPathologistId("");
        form.setReviewNotes("note");

        save(form);

        assertEquals("a blank pathologist keeps the one assigned", "1002", storedText("pathologist_id"));
        assertEquals("the notes are stored", "note", storedText("review_notes"));
        assertEquals("and read on the case's history, from nothing to the note", List.of(List.of("", "note")),
                historyLines("reviewNotes"));
    }

    @Test
    public void aReleaseWithoutBothDescriptions_isRefused() {
        storeAt(PathologyStatus.UNDER_REVIEW);
        jdbcTemplate.update("UPDATE clinlims.pathology_sample SET gross_exam = NULL WHERE id = ?", CASE_ID);
        assertRefusedWith("pathology.error.releaseWithoutDescriptions", () -> save(releaseForm()));

        jdbcTemplate.update("UPDATE clinlims.pathology_sample SET gross_exam = 'Grey tissue',"
                + " microscopy_exam = NULL WHERE id = ?", CASE_ID);
        assertRefusedWith("pathology.error.releaseWithoutDescriptions", () -> save(releaseForm()));

        PathologySampleForm spaces = releaseForm();
        spaces.setMicroscopyExam("   ");
        assertRefusedWith("pathology.error.releaseWithoutDescriptions", () -> save(spaces));
        assertEquals("a refused release leaves the case under review", "UNDER_REVIEW", storedStage());

        jdbcTemplate.update("UPDATE clinlims.pathology_sample SET gross_exam = NULL WHERE id = ?", CASE_ID);
        PathologySampleForm both = releaseForm();
        both.setGrossExam("Grey tissue");
        both.setMicroscopyExam(READING);
        save(both);
        assertEquals("both posted with the release itself, the case is signed out", "COMPLETED", storedStage());
    }

    @Test
    public void aRefusedSave_writesNoRow_noHistory_andNoIhcCase() {
        storeAt(PathologyStatus.UNDER_REVIEW);
        jdbcTemplate.update("UPDATE clinlims.pathology_sample SET microscopy_exam = NULL WHERE id = ?", CASE_ID);
        List<Object> rowIds = ids(conclusionRows());
        clearCaseAudit();
        PathologySampleForm form = releaseForm();
        form.setConclusionText(NARRATIVE);
        form.setConclusions(List.of(PathologyFindingsDictionary.ADENOCARCINOMA));
        form.setTechniques(List.of(PathologyFindingsDictionary.FROZEN_SECTION));
        form.setReviewNotes(NOTES);
        form.setReferToImmunoHistoChemistry(true);

        assertRefusedWith("pathology.error.releaseWithoutDescriptions", () -> save(form));

        assertEquals("no conclusion row is written", rowIds, ids(conclusionRows()));
        assertEquals("no technique row", List.of(), codesOf(techniqueRows()));
        assertNull("no notes", storedText("review_notes"));
        assertTrue("no history row", caseAudit().isEmpty());
        assertEquals("and no immunohistochemistry case", 0, referralCount(jdbcTemplate));
        assertEquals("the case stays under review", "UNDER_REVIEW", storedStage());
    }

    // helpers

    /** The findings fields, each changed against the fixture's case. */
    private static Map<String, Consumer<PathologySampleForm>> findingsChanges() {
        Map<String, Consumer<PathologySampleForm>> changes = new LinkedHashMap<>();
        changes.put("microscopic description", form -> form.setMicroscopyExam("A changed reading"));
        changes.put("narrative conclusion", form -> form.setConclusionText("A changed narrative"));
        changes.put("coded conclusions",
                form -> form.setConclusions(List.of(PathologyFindingsDictionary.ADENOCARCINOMA)));
        changes.put("techniques", form -> form.setTechniques(List.of(PathologyFindingsDictionary.FROZEN_SECTION)));
        changes.put("referral", form -> form.setReferToImmunoHistoChemistry(true));
        return changes;
    }

    /** The case fields, each changed against the fixture's case. */
    private static Map<String, Consumer<PathologySampleForm>> caseFieldChanges(String tag) {
        Map<String, Consumer<PathologySampleForm>> changes = new LinkedHashMap<>();
        changes.put("macroscopic description", form -> form.setGrossExam("Macroscopic, " + tag));
        changes.put("pathologist", form -> form.setAssignedPathologistId(SECOND_PATHOLOGIST_ID));
        changes.put("review notes", form -> form.setReviewNotes("Notes, " + tag));
        return changes;
    }

    private void assertEveryFindingRefused(String where, String errorCode) {
        findingsChanges().forEach((field, change) -> {
            PathologySampleForm form = caseViewForm();
            change.accept(form);
            LocalizedValidationException refusal = assertThrows(where + ", a changed " + field + " is refused",
                    LocalizedValidationException.class, () -> save(form));
            assertEquals(where + ", " + field, errorCode, refusal.getErrorCode());
        });
    }

    private void assertCaseFieldsTaken(String where) {
        PathologySampleForm form = caseViewForm();
        caseFieldChanges(where).values().forEach(change -> change.accept(form));
        save(form);
        assertEquals(where + ", the macroscopic description is taken", "Macroscopic, " + where,
                storedText("gross_exam"));
        assertEquals(where + ", the pathologist", SECOND_PATHOLOGIST_ID, storedText("pathologist_id"));
        assertEquals(where + ", the notes", "Notes, " + where, storedText("review_notes"));
    }

    private void assertEveryFindingTaken(String where, String code) {
        PathologySampleForm form = caseViewForm();
        findingsChanges().values().forEach(change -> change.accept(form));
        form.setMicroscopyExam("Reading, " + where);
        form.setConclusions(List.of(code));
        save(form);
        assertEquals(where + ", the reading is taken", "Reading, " + where, storedText("microscopy_exam"));
        assertEquals(where + ", the narrative", "A changed narrative", typedValues(conclusionRows()).get(0).get(1));
        assertEquals(where + ", the code", List.of(code), codesOf(conclusionRows()));
        assertEquals(where + ", the technique", List.of(PathologyFindingsDictionary.FROZEN_SECTION),
                codesOf(techniqueRows()));
        assertEquals(where + ", the referral", 1, referralCount(jdbcTemplate));
    }

    private void writeFindingsUnderReview() {
        storeAt(PathologyStatus.READY_PATHOLOGIST);
        pathologySampleService.advance(CASE_ID, PathologyStatus.READY_PATHOLOGIST, TECHNICIAN_ID);
        PathologySampleForm form = caseViewForm();
        form.setMicroscopyExam(READING);
        form.setConclusionText(NARRATIVE);
        form.setConclusions(
                List.of(PathologyFindingsDictionary.ADENOCARCINOMA, PathologyFindingsDictionary.CHRONIC_GASTRITIS));
        form.setTechniques(List.of(PathologyFindingsDictionary.FROZEN_SECTION));
        form.setReviewNotes(NOTES);
        save(form);
    }

    private void returnToTheQueue() {
        pathologySampleService.returnToPreviousStage(CASE_ID, PathologyStatus.UNDER_REVIEW, "Needs deeper levels",
                TECHNICIAN_ID);
        assertEquals("setup: the case is back in the queue", "READY_PATHOLOGIST", storedStage());
    }

    private void signOut() {
        if (!"UNDER_REVIEW".equals(storedStage())) {
            storeAt(PathologyStatus.UNDER_REVIEW);
        }
        save(releaseForm());
        assertEquals("setup: the case is signed out", "COMPLETED", storedStage());
    }

    private PathologySampleForm releaseForm() {
        PathologySampleForm form = caseViewForm();
        form.setRelease(true);
        return form;
    }

    /** The case as the case view loads it, posted back unchanged. */
    private PathologySampleForm asLoaded() {
        PathologySampleForm form = caseViewForm();
        form.setMicroscopyExam(storedText("microscopy_exam"));
        form.setReviewNotes(storedText("review_notes"));
        form.setAssignedPathologistId(storedText("pathologist_id"));
        List<List<Object>> conclusions = typedValues(conclusionRows());
        form.setConclusionText(conclusions.stream().filter(row -> "TEXT".equals(row.get(0)))
                .map(row -> (String) row.get(1)).findFirst().orElse(""));
        form.setConclusions(codesOf(conclusionRows()));
        form.setTechniques(codesOf(techniqueRows()));
        return form;
    }

    private void saveCodes(List<String> codes) {
        PathologySampleForm form = caseViewForm();
        form.setConclusions(codes);
        save(form);
    }

    private void saveTechniques(List<String> codes) {
        PathologySampleForm form = caseViewForm();
        form.setTechniques(codes);
        save(form);
    }

    private void insertConclusionRow(String code) {
        jdbcTemplate.update("INSERT INTO clinlims.pathology_conclusion (id, pathology_sample_id, type, value,"
                + " last_updated) VALUES (nextval('clinlims.pathology_conclusion_seq'), ?, 'DICTIONARY', ?, now())",
                CASE_ID, code);
    }

    /**
     * Two rows of the case, the one with the higher id written first: the lower id
     * carries {@code lowerIdCode}.
     */
    private void insertHigherIdFirst(String table, String higherIdCode, String lowerIdCode) {
        long lowerId = jdbcTemplate.queryForObject("SELECT nextval('clinlims." + table + "_seq')", Long.class);
        long higherId = jdbcTemplate.queryForObject("SELECT nextval('clinlims." + table + "_seq')", Long.class);
        String insert = "INSERT INTO clinlims." + table + " (id, pathology_sample_id, type, value, last_updated)"
                + " VALUES (?, ?, 'DICTIONARY', ?, now())";
        jdbcTemplate.update(insert, higherId, CASE_ID, higherIdCode);
        jdbcTemplate.update(insert, lowerId, CASE_ID, lowerIdCode);
    }

    private String storedText(String column) {
        Object value = jdbcTemplate.queryForObject("SELECT " + column + " FROM clinlims.pathology_sample WHERE id = ?",
                Object.class, CASE_ID);
        return value == null ? null : String.valueOf(value);
    }

    private List<Map<String, Object>> conclusionRows() {
        return jdbcTemplate.queryForList("SELECT id, type, value FROM clinlims.pathology_conclusion"
                + " WHERE pathology_sample_id = ? ORDER BY id", CASE_ID);
    }

    private List<Map<String, Object>> techniqueRows() {
        return jdbcTemplate.queryForList("SELECT id, type, value FROM clinlims.pathology_technique"
                + " WHERE pathology_sample_id = ? ORDER BY id", CASE_ID);
    }

    private static List<Object> ids(List<Map<String, Object>> rows) {
        return rows.stream().map(row -> row.get("id")).collect(Collectors.toList());
    }

    private static List<List<Object>> typedValues(List<Map<String, Object>> rows) {
        return rows.stream().map(row -> List.of(row.get("type"), row.get("value"))).collect(Collectors.toList());
    }

    private static List<String> codesOf(List<Map<String, Object>> rows) {
        return rows.stream().filter(row -> "DICTIONARY".equals(row.get("type"))).map(row -> (String) row.get("value"))
                .collect(Collectors.toList());
    }

    private List<History> conclusionAudit() {
        return caseAudit().stream().filter(history -> changesOf(history).startsWith("{")).filter(
                history -> PathologySampleService.PATHOLOGY_CONCLUSION_SAVED.equals(payloadOf(history).get("verb")))
                .collect(Collectors.toList());
    }

    /** Each history line about one attribute as its old and new value. */
    private List<List<String>> historyLines(String attribute) {
        List<AuditTrailItem> items = new TransactionTemplate(transactionManager).execute(
                status -> new ProgramSampleHistoryService(pathologySampleService.get(CASE_ID)).getAuditTrailItems());
        return items.stream().filter(item -> attribute.equals(item.getIdentifier())).map(item -> List
                .of(StringUtils.defaultString(item.getOldValue()), StringUtils.defaultString(item.getNewValue())))
                .collect(Collectors.toList());
    }

    static int referralCount(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinlims.immunohistochemistry_sample WHERE pathology_sample_id = ?",
                Integer.class, CASE_ID);
    }

    static void removeReferrals(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("DELETE FROM clinlims.immunohistochemistry_sample WHERE pathology_sample_id = ?", CASE_ID);
    }
}
