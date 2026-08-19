package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQACycleStateTransition;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQARound;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQAStateMachine;
import org.openelisglobal.eqa.valueholder.EQASubmissionChannel;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.openelisglobal.eqa.valueholder.EQATriggerEvent;
import org.openelisglobal.eqa.valueholder.EQATriggerType;

/**
 * OGC-609 [EQA V2.1 / T-08] — ORM defaults and enum vocabularies for the cycle
 * spine. No database connection (constitution V.4, under 5 seconds).
 *
 * <p>
 * The enum assertions are load-bearing, not ceremony: every value is written to
 * the DB as its Java name and matched against a CHECK constraint in
 * liquibase/qa/015 and qa/016. Renaming or dropping a constant without editing
 * the changeset produces a runtime constraint violation that no compile catches
 * — these tests are what turn that into a failing build.
 */
public class EQACycleMappingValidationTest {

    @Test
    public void cycleDefaultsToPlanned() {
        EQACycle cycle = new EQACycle();
        assertNotNull("EQACycle should be instantiable", cycle);
        assertEquals("a cycle starts planned", EQACycleStatus.PLANNED, cycle.getStatus());
        assertNull("fhirUuid is assigned at persist time, not construction", cycle.getFhirUuid());
    }

    @Test
    public void roundIsInstantiableWithNoStatusVocabulary() {
        EQARound round = new EQARound();
        assertNotNull("EQARound should be instantiable", round);
        assertNull("round status is free text; the FRS defines no vocabulary", round.getStatus());
    }

    @Test
    public void stateTransitionHasNoDefaults() {
        EQACycleStateTransition transition = new EQACycleStateTransition();
        assertNotNull("EQACycleStateTransition should be instantiable", transition);
        assertNull("every audit field is supplied by the writer", transition.getStateMachine());
        assertNull("prior state is null only at cycle creation", transition.getPriorState());
    }

    @Test
    public void participantResultDefaultsToDraft() {
        EQAParticipantResult result = new EQAParticipantResult();
        assertNotNull("EQAParticipantResult should be instantiable", result);
        assertEquals("a result starts as a draft", EQASubmissionStatus.DRAFT, result.getSubmissionStatus());
        assertNull("channel is set when the result is submitted", result.getSubmissionChannel());
    }

    @Test
    public void programDefaultsToInternationalPt() {
        EQAProgram program = new EQAProgram();
        assertEquals("V1 schemes are international PT (FR-V2.1-06)", EQASchemeType.INTERNATIONAL_PT,
                program.getSchemeType());
    }

    @Test
    public void schemeTypeVocabularyMatchesTheCheckConstraint() {
        assertEquals(Arrays.asList("INTERNATIONAL_PT", "REGIONAL_PT", "INTER_LAB_SPLIT", "IN_HOUSE"),
                names(EQASchemeType.values()));
    }

    @Test
    public void cycleStatusVocabularyCoversBothStateMachines() {
        assertEquals(Arrays.asList("PLANNED", "PANEL_RECEIVED", "TESTING", "READY_TO_SUBMIT", "SUBMITTED",
                "PREP_IN_PROGRESS", "READY_TO_SHIP", "SHIPPED", "DELIVERED", "SUBMISSIONS_OPEN", "SUBMISSIONS_CLOSED",
                "SCORING", "SCORED", "CLOSED"), names(EQACycleStatus.values()));
    }

    @Test
    public void auditVocabulariesMatchTheCheckConstraints() {
        assertEquals(Arrays.asList("PARTICIPANT", "PROVIDER"), names(EQAStateMachine.values()));
        assertEquals(Arrays.asList("AUTO", "MANUAL"), names(EQATriggerType.values()));
        // PANEL_RECEIPT joined with liquibase qa/024 and FIRST_SHIPMENT_SENT with
        // qa/028 — each extends the DB CHECK in step.
        assertEquals(Arrays.asList("LAST_VALIDATED_RESULT", "FHIR_SUBMIT_SUCCESS", "FHIR_SUBMIT_FAILURE_RETRY",
                "SCORE_INTAKE", "DEADLINE_TIMER", "ALL_SHIPMENTS_DELIVERED", "ALL_SUBMISSIONS_RECEIVED", "PANEL_SEAL",
                "PANEL_UNBLIND", "HOMOGENEITY_QC_PASSED", "MANUAL_OVERRIDE", "SCHEDULED_JOB", "PANEL_RECEIPT",
                "FIRST_SHIPMENT_SENT"), names(EQATriggerEvent.values()));
    }

    @Test
    public void submissionVocabulariesMatchTheCheckConstraints() {
        assertEquals(Arrays.asList("DRAFT", "VALIDATED_PARTIAL", "SUBMITTED", "SCORED", "MISSED_DEADLINE"),
                names(EQASubmissionStatus.values()));
        assertEquals(Arrays.asList("FHIR", "MANUAL"), names(EQASubmissionChannel.values()));
    }

    private List<String> names(Enum<?>[] values) {
        return Stream.of(values).map(Enum::name).collect(Collectors.toList());
    }
}
