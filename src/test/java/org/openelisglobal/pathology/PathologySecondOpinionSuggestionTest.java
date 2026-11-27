package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.openelisglobal.pathology.PathologyConsultationFixture.CONSULTED;
import static org.openelisglobal.pathology.PathologyConsultationFixture.REQUESTER;
import static org.openelisglobal.pathology.PathologyConsultationFixture.UNCODED_DIAGNOSIS;
import static org.openelisglobal.pathology.PathologyFindingsDictionary.ADENOCARCINOMA;
import static org.openelisglobal.pathology.PathologyFindingsDictionary.CHRONIC_GASTRITIS;

import java.util.List;
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
 * The laboratory's rule that suggests a second opinion (FR-11.5): the diagnosis
 * codes it lists are the starts of the codes of the case's coded conclusions,
 * read as the laboratory typed them, and a suggestion ends once a second
 * opinion is awaited or answered. It is a note on the screen and never holds
 * anything.
 */
public class PathologySecondOpinionSuggestionTest extends PathologyCaseTestSupport {

    private static final String ADENOCARCINOMA_SUGGESTED = "C80.1 Adenocarcinoma";
    private static final String GASTRITIS_SUGGESTED = "K29.5 Chronic gastritis";

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
    public void seedThePeopleAndTheRule() {
        switches = StageSwitches.allOn();
        fixture = PathologyConsultationFixture.seed(jdbcTemplate);
        ruleBefore = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.PATHOLOGY_SECOND_OPINION_SUGGEST_CODES);
        storeAt(PathologyStatus.UNDER_REVIEW);
    }

    @After
    public void removeThePeopleAndRestoreTheRule() {
        setRule(ruleBefore);
        fixture.close();
        switches.close();
    }

    @Test
    public void withNoRule_nothingIsSuggested() {
        storeConclusions(ADENOCARCINOMA, CHRONIC_GASTRITIS);

        for (String rule : new String[] { null, "", "   ", ", ,", ",," }) {
            setRule(rule);
            assertEquals("no suggestion for the rule [" + rule + "]", List.of(), suggested());
        }

        setRule("C");
        assertEquals("the same case is suggested for once a rule is set", List.of(ADENOCARCINOMA_SUGGESTED),
                suggested());
    }

    @Test
    public void aCodeThatStartsWithAListedPrefix_isSuggested_ignoringCaseAndSpaces() {
        storeConclusions(ADENOCARCINOMA, CHRONIC_GASTRITIS);

        setRule("C");
        assertEquals("C80.1 starts with C; K29.5 does not", List.of(ADENOCARCINOMA_SUGGESTED), suggested());

        setRule("C,D0");
        List<String> typedTidily = suggested();
        setRule(" c , d0 ");
        assertEquals("case and spaces do not matter", typedTidily, suggested());
        assertEquals(List.of(ADENOCARCINOMA_SUGGESTED), typedTidily);

        setRule("c80.1");
        assertEquals("the whole code is a start of itself", List.of(ADENOCARCINOMA_SUGGESTED), suggested());

        setRule("D0");
        assertEquals("a start no code has suggests nothing", List.of(), suggested());
    }

    @Test
    public void everyMatchingDiagnosisIsNamed_inStoredOrder_andOneWithoutACodeNever() {
        // Stored in an order that is neither alphabetical nor the dictionary's.
        storeConclusions(CHRONIC_GASTRITIS, UNCODED_DIAGNOSIS, ADENOCARCINOMA);

        setRule("C,K");
        assertEquals("both coded diagnoses, in stored order, and never the one without a code",
                List.of(GASTRITIS_SUGGESTED, ADENOCARCINOMA_SUGGESTED), suggested());

        setRule("C");
        storeConclusions(UNCODED_DIAGNOSIS);
        assertEquals("a coded conclusion with no code suggests nothing", List.of(), suggested());

        storeConclusions();
        assertEquals("a narrative alone suggests nothing", List.of(), suggested());
    }

    @Test
    public void anAwaitedOrAnsweredSecondOpinion_endsTheSuggestion_aWithdrawnOneDoesNot() {
        storeConclusions(ADENOCARCINOMA);
        setRule("C");
        assertEquals("before anyone is asked", List.of(ADENOCARCINOMA_SUGGESTED), suggested());

        PathologyConsultation withdrawn = ask();
        consultationService.withdraw(CASE_ID, withdrawn.getId(), REQUESTER);
        assertEquals("a withdrawn request does not end it", List.of(ADENOCARCINOMA_SUGGESTED), suggested());

        PathologyConsultation awaited = ask();
        assertEquals("an awaited request ends it", List.of(), suggested());

        consultationService.answer(CASE_ID, awaited.getId(), new ConsultationAnswer("AGREE", null), CONSULTED);
        assertEquals("an answered request keeps it ended", List.of(), suggested());
    }

    @Test
    public void onTheBench_andOnceSignedOut_nothingIsSuggested() {
        storeConclusions(ADENOCARCINOMA);
        setRule("C");
        assertFalse("before: under review it is suggested", suggested().isEmpty());

        for (PathologyStatus stage : BENCH_STAGES) {
            storeAt(stage);
            assertEquals("nothing is suggested at " + stage, List.of(), suggested());
        }

        storeAt(PathologyStatus.UNDER_REVIEW);
        assertTrue("back under review it is suggested again", suggested().size() == 1);
        signOut();
        assertEquals("nothing is suggested once the case is signed out", List.of(), suggested());
    }

    // helpers

    private PathologyConsultation ask() {
        return consultationService.request(CASE_ID, new ConsultationDraft(CONSULTED, null, true), REQUESTER);
    }

    /**
     * The service's own answer for the case as it stands, read inside one
     * transaction as the case view does.
     */
    private List<String> suggested() {
        return new TransactionTemplate(transactionManager).execute(status -> {
            var pathologySample = pathologySampleService.get(CASE_ID);
            return consultationService.suggestedFor(pathologySample, consultationService.getForCase(CASE_ID));
        });
    }

    private void setRule(String rule) {
        ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_SECOND_OPINION_SUGGEST_CODES,
                rule == null ? "" : rule);
    }

    /** Stores the coded conclusions through the case save, under review. */
    private void storeConclusions(String... dictionaryIds) {
        PathologySampleForm form = caseViewForm();
        form.setConclusions(List.of(dictionaryIds));
        save(form);
    }

    private void signOut() {
        closeTheFixtureRequest();
        storeAt(PathologyStatus.UNDER_REVIEW);
        PathologySampleForm form = caseViewForm();
        form.setRelease(true);
        save(form);
        assertEquals("setup: the case is signed out", "COMPLETED", storedStage());
    }
}
