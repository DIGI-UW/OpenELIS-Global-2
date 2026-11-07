package org.openelisglobal.fhir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.hibernate.LazyInitializationException;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisAnchorService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;

/**
 * Issue #4901: Expose clinical interpretations over FHIR via
 * DiagnosticReport.conclusion and Observation.note.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class DiagnosticReportAndObservationInterpretationTest {

    private static final String SITE_SYSTEM = "https://lab.example.org/fhir";

    @Mock
    private FhirConfig fhirConfig;
    @Mock
    private AnalysisAnchorService analysisAnchorService;
    @Mock
    private IStatusService statusService;
    @Mock
    private TerminologyTransformService terminologyTransformService;
    @Mock
    private NoteService noteService;
    @Mock
    private SampleHumanService sampleHumanService;
    @Mock
    private ResultService resultService;
    @Mock
    private FhirCommonTransformService common;

    @InjectMocks
    private DiagnosticReportTransformServiceImpl diagnosticReportTransformService;

    @InjectMocks
    private ObservationTransformServiceImpl observationTransformService;

    private final FhirCommonTransformServiceImpl commonImpl = new FhirCommonTransformServiceImpl();

    private Analysis analysis;
    private org.openelisglobal.test.valueholder.Test testEntity;
    private Result result;
    private TestResultComponent component1;

    @Before
    public void setUp() {
        when(fhirConfig.getOeFhirSystem()).thenReturn(SITE_SYSTEM);
        when(statusService.getStatusID(AnalysisStatus.Finalized)).thenReturn("1");
        when(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance)).thenReturn("2");
        when(statusService.getStatusID(AnalysisStatus.TechnicalRejected)).thenReturn("3");
        when(statusService.getStatusID(AnalysisStatus.NotStarted)).thenReturn("4");

        when(common.createReferenceFor(any(ResourceType.class), anyString()))
                .thenAnswer(inv -> new Reference(inv.getArgument(0).toString() + "/" + inv.getArgument(1)));
        when(common.createIdentifier(anyString(), anyString()))
                .thenAnswer(inv -> new Identifier().setSystem(inv.getArgument(0)).setValue(inv.getArgument(1)));
        when(common.transformNoteToAnnotation(any(Note.class)))
                .thenAnswer(inv -> commonImpl.transformNoteToAnnotation(inv.getArgument(0)));

        analysis = new Analysis();
        analysis.setId("5001");
        analysis.setFhirUuid(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        analysis.setStatusId("1");
        analysis.setLastupdated(new Timestamp(1700000000000L));

        testEntity = new org.openelisglobal.test.valueholder.Test();
        testEntity.setId("201");
        testEntity.setDescription("Karyotype Analysis");
        analysis.setTest(testEntity);

        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("301");
        sampleItem.setFhirUuid(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        analysis.setSampleItem(sampleItem);

        Sample sample = new Sample();
        sample.setId("401");
        sampleItem.setSample(sample);

        Patient patient = new Patient();
        patient.setId("601");
        patient.setFhirUuid(UUID.fromString("33333333-3333-3333-3333-333333333333"));

        when(sampleHumanService.getPatientForSample(any(Sample.class))).thenReturn(patient);
        when(terminologyTransformService.transformTestToCodeableConcept(anyString(), anyString()))
                .thenReturn(new CodeableConcept());
        when(terminologyTransformService.transformResultCodeableConcept(any(), any(), any()))
                .thenReturn(new CodeableConcept());

        component1 = new TestResultComponent();
        component1.setId("comp-001");
        component1.setLabel("Chromosome 21");

        result = new Result();
        result.setId("801");
        result.setFhirUuid(UUID.fromString("44444444-4444-4444-4444-444444444444"));
        result.setAnalysis(analysis);
        result.setValue("Normal 46,XX");
        result.setResultType("T");
        result.setLastupdated(new Timestamp(1700000000000L));

        TestResult testResult = new TestResult();
        testResult.setId("tr-001");
        testResult.setComponentId("comp-001");
        result.setTestResult(testResult);

        when(resultService.getResultsByAnalysis(analysis)).thenReturn(Collections.singletonList(result));
        when(terminologyTransformService.resolveResultComponent("201", result)).thenReturn(component1);
    }

    private Note createNote(String id, String type, String subject, String text, String componentId, long timeMillis) {
        Note note = new Note();
        note.setId(id);
        note.setNoteType(type);
        note.setSubject(subject);
        note.setText(text);
        note.setTestResultComponentId(componentId);
        note.setLastupdated(new Timestamp(timeMillis));
        return note;
    }

    // --- DiagnosticReport.conclusion Tests ---

    @Test
    public void testDiagnosticReportConclusion_UnscopedExternalInterpretationNoteSetsConclusion() {
        Note note = createNote("n1", Note.EXTERNAL, "Interpretation",
                "Apparent normal female karyotype without numerical aberrations.", null, 1700000100000L);
        when(noteService.getNotes(analysis)).thenReturn(Collections.singletonList(note));

        DiagnosticReport report = diagnosticReportTransformService.transformResultToDiagnosticReport(analysis);

        assertNotNull(report);
        assertEquals("Apparent normal female karyotype without numerical aberrations.", report.getConclusion());
    }

    @Test
    public void testDiagnosticReportConclusion_IgnoresInternalInterpretationNotes() {
        Note internalNote = createNote("n1", Note.INTERNAL, "Interpretation",
                "Internal technician comment: re-checked metaphase spread 4.", null, 1700000100000L);
        when(noteService.getNotes(analysis)).thenReturn(Collections.singletonList(internalNote));

        DiagnosticReport report = diagnosticReportTransformService.transformResultToDiagnosticReport(analysis);

        assertNotNull(report);
        assertNull(report.getConclusion());
    }

    @Test
    public void testDiagnosticReportConclusion_IgnoresComponentScopedInterpretationNotes() {
        Note componentNote = createNote("n1", Note.EXTERNAL, "Interpretation",
                "Trisomy 21 detected in chromosome 21 component.", "comp-001", 1700000100000L);
        when(noteService.getNotes(analysis)).thenReturn(Collections.singletonList(componentNote));

        DiagnosticReport report = diagnosticReportTransformService.transformResultToDiagnosticReport(analysis);

        assertNotNull(report);
        assertNull(report.getConclusion());
    }

    @Test
    public void testDiagnosticReportConclusion_MultipleUnscopedNotes_PicksLatest() {
        Note olderNote = createNote("n1", Note.EXTERNAL, "Interpretation", "Initial impression: borderline mosaicism.",
                null, 1700000100000L);
        Note newerNote = createNote("n2", Note.EXTERNAL, "Interpretation",
                "Final confirmed impression: no mosaicism observed.", null, 1700000200000L);

        List<Note> notes = new ArrayList<>();
        notes.add(olderNote);
        notes.add(newerNote);
        when(noteService.getNotes(analysis)).thenReturn(notes);

        DiagnosticReport report = diagnosticReportTransformService.transformResultToDiagnosticReport(analysis);

        assertNotNull(report);
        assertEquals("Final confirmed impression: no mosaicism observed.", report.getConclusion());
    }

    @Test
    public void testDiagnosticReportConclusion_NonInterpretationSubjectIgnored() {
        Note generalNote = createNote("n1", Note.EXTERNAL, "Result Note", "Specimen slightly hemolyzed upon arrival.",
                null, 1700000100000L);
        when(noteService.getNotes(analysis)).thenReturn(Collections.singletonList(generalNote));

        DiagnosticReport report = diagnosticReportTransformService.transformResultToDiagnosticReport(analysis);

        assertNotNull(report);
        assertNull(report.getConclusion());
    }

    @Test
    public void testDiagnosticReportConclusion_NullAndEmptyNotesHandledSafely() {
        when(noteService.getNotes(analysis)).thenReturn(null);
        DiagnosticReport report1 = diagnosticReportTransformService.transformResultToDiagnosticReport(analysis);
        assertNotNull(report1);
        assertNull(report1.getConclusion());

        when(noteService.getNotes(analysis)).thenReturn(Collections.emptyList());
        DiagnosticReport report2 = diagnosticReportTransformService.transformResultToDiagnosticReport(analysis);
        assertNotNull(report2);
        assertNull(report2.getConclusion());
    }

    // --- Observation.note Tests ---

    @Test
    public void testObservationNote_ComponentScopedExternalNoteIsAttached() {
        Note compNote = createNote("n1", Note.EXTERNAL, "Interpretation",
                "Component finding: 20 metaphases analyzed, all show normal 46,XX.", "comp-001", 1700000100000L);
        when(noteService.getNotes(analysis)).thenReturn(Collections.singletonList(compNote));

        Observation obs = observationTransformService.transformResultToObservation(result);

        assertNotNull(obs);
        assertEquals(1, obs.getNote().size());
        assertEquals("Component finding: 20 metaphases analyzed, all show normal 46,XX.",
                obs.getNote().get(0).getText());
    }

    @Test
    public void testObservationNote_IgnoresOtherComponentNotesAndUnscopedNotes() {
        Note otherCompNote = createNote("n1", Note.EXTERNAL, "Interpretation", "Other component note for Chromosome 18",
                "comp-999", 1700000100000L);
        Note unscopedNote = createNote("n2", Note.EXTERNAL, "Interpretation", "Overall report summary", null,
                1700000200000L);

        List<Note> notes = new ArrayList<>();
        notes.add(otherCompNote);
        notes.add(unscopedNote);
        when(noteService.getNotes(analysis)).thenReturn(notes);

        Observation obs = observationTransformService.transformResultToObservation(result);

        assertNotNull(obs);
        assertTrue(obs.getNote().isEmpty());
    }

    @Test
    public void testObservationNote_IgnoresInternalComponentNotes() {
        Note internalCompNote = createNote("n1", Note.INTERNAL, "Interpretation", "Internal QC note for this component",
                "comp-001", 1700000100000L);
        when(noteService.getNotes(analysis)).thenReturn(Collections.singletonList(internalCompNote));

        Observation obs = observationTransformService.transformResultToObservation(result);

        assertNotNull(obs);
        assertTrue(obs.getNote().isEmpty());
    }

    @Test
    public void testObservationNote_IgnoresNonInterpretationSubjectComponentNotes() {
        Note nonInterpretationNote = createNote("n1", Note.EXTERNAL, "General Comment",
                "Unrelated component note that is not an interpretation", "comp-001", 1700000100000L);
        when(noteService.getNotes(analysis)).thenReturn(Collections.singletonList(nonInterpretationNote));

        Observation obs = observationTransformService.transformResultToObservation(result);

        assertNotNull(obs);
        assertTrue(obs.getNote().isEmpty());
    }

    @Test
    public void testObservationNote_ReusesPassedAnalysisNotes() {
        Note compNote = createNote("n1", Note.EXTERNAL, "Interpretation",
                "Directly passed component interpretation note", "comp-001", 1700000100000L);

        Observation obs = observationTransformService.transformResultToObservation(result,
                Collections.singletonList(compNote));

        assertNotNull(obs);
        assertEquals(1, obs.getNote().size());
        assertEquals("Directly passed component interpretation note", obs.getNote().get(0).getText());
        verify(noteService, never()).getNotes(any());
    }

    @Test
    public void testObservationNote_CachesNotesPerAnalysisToAvoidRepeatedQueries() {
        Note compNote = createNote("n1", Note.EXTERNAL, "Interpretation",
                "Component finding reused across observations", "comp-001", 1700000100000L);
        when(noteService.getNotes(analysis)).thenReturn(Collections.singletonList(compNote));

        Observation obs1 = observationTransformService.transformResultToObservation(result);
        Observation obs2 = observationTransformService.transformResultToObservation(result);

        assertNotNull(obs1);
        assertNotNull(obs2);
        assertEquals(1, obs1.getNote().size());
        assertEquals(1, obs2.getNote().size());
        verify(noteService, times(1)).getNotes(analysis);
    }

    @Test
    public void testObservationNote_MultipleComponentNotesAttachedInOrder() {
        Note note1 = createNote("n1", Note.EXTERNAL, "Interpretation", "First component interpretation", "comp-001",
                1700000100000L);
        Note note2 = createNote("n2", Note.EXTERNAL, "Interpretation", "Second supplementary component note",
                "comp-001", 1700000200000L);

        List<Note> notes = new ArrayList<>();
        notes.add(note1);
        notes.add(note2);
        when(noteService.getNotes(analysis)).thenReturn(notes);

        Observation obs = observationTransformService.transformResultToObservation(result);

        assertNotNull(obs);
        assertEquals(2, obs.getNote().size());
        assertEquals("First component interpretation", obs.getNote().get(0).getText());
        assertEquals("Second supplementary component note", obs.getNote().get(1).getText());
    }

    @Test
    public void testObservationNote_NullAndEmptyNotesHandledSafely() {
        when(noteService.getNotes(analysis)).thenReturn(null);
        Observation obs1 = observationTransformService.transformResultToObservation(result);
        assertNotNull(obs1);
        assertTrue(obs1.getNote().isEmpty());

        when(noteService.getNotes(analysis)).thenReturn(Collections.emptyList());
        Observation obs2 = observationTransformService.transformResultToObservation(result);
        assertNotNull(obs2);
        assertTrue(obs2.getNote().isEmpty());
    }

    // --- FhirCommonTransformService.transformNoteToAnnotation Metadata Tests ---

    @Test
    public void testTransformNoteToAnnotation_NullNoteReturnsNull() {
        assertNull(commonImpl.transformNoteToAnnotation(null));
    }

    @Test
    public void testTransformNoteToAnnotation_FullMetadata() {
        Note note = new Note();
        note.setId("101");
        note.setText("Valid clinical finding");
        note.setLastupdated(new Timestamp(1700000123000L));

        SystemUser author = new SystemUser();
        author.setFirstName("Marie");
        author.setLastName("Curie");
        author.setLoginName("mcurie");
        note.setSystemUser(author);

        Annotation annotation = commonImpl.transformNoteToAnnotation(note);

        assertNotNull(annotation);
        assertEquals("Valid clinical finding", annotation.getText());
        assertEquals(new Date(1700000123000L), annotation.getTime());
        assertEquals("Marie Curie", annotation.getAuthorStringType().getValue());
    }

    @Test
    public void testTransformNoteToAnnotation_AuthorFallbackToLoginName() {
        Note note = new Note();
        note.setId("102");
        note.setText("Note with login-only author");
        note.setLastupdated(new Timestamp(1700000123000L));

        SystemUser author = new SystemUser();
        author.setLoginName("tech_user");
        note.setSystemUser(author);

        Annotation annotation = commonImpl.transformNoteToAnnotation(note);

        assertNotNull(annotation);
        assertEquals("tech_user", annotation.getAuthorStringType().getValue());
    }

    @Test
    public void testTransformNoteToAnnotation_PartiallyPopulatedNames_FirstNameOnly() {
        Note note = new Note();
        note.setId("104");
        note.setText("Note with first-name-only author");
        note.setLastupdated(new Timestamp(1700000123000L));

        SystemUser author = new SystemUser();
        author.setFirstName("Marie");
        author.setLastName(null);
        author.setLoginName("mcurie");
        note.setSystemUser(author);

        Annotation annotation = commonImpl.transformNoteToAnnotation(note);

        assertNotNull(annotation);
        assertEquals("Marie", annotation.getAuthorStringType().getValue());
    }

    @Test
    public void testTransformNoteToAnnotation_PartiallyPopulatedNames_LastNameOnly() {
        Note note = new Note();
        note.setId("105");
        note.setText("Note with last-name-only author");
        note.setLastupdated(new Timestamp(1700000123000L));

        SystemUser author = new SystemUser();
        author.setFirstName(null);
        author.setLastName("Curie");
        author.setLoginName("mcurie");
        note.setSystemUser(author);

        Annotation annotation = commonImpl.transformNoteToAnnotation(note);

        assertNotNull(annotation);
        assertEquals("Curie", annotation.getAuthorStringType().getValue());
    }

    @Test
    public void testTransformNoteToAnnotation_PartiallyPopulatedNames_LiteralNullString() {
        Note note = new Note();
        note.setId("106");
        note.setText("Note with literal null first name string");
        note.setLastupdated(new Timestamp(1700000123000L));

        SystemUser author = new SystemUser();
        author.setFirstName("null");
        author.setLastName("Curie");
        author.setLoginName("mcurie");
        note.setSystemUser(author);

        Annotation annotation = commonImpl.transformNoteToAnnotation(note);

        assertNotNull(annotation);
        assertEquals("Curie", annotation.getAuthorStringType().getValue());
    }

    @Test
    public void testTransformNoteToAnnotation_LazyInitializationExceptionHandledGracefully() {
        Note note = new Note() {
            @Override
            public SystemUser getSystemUser() {
                throw new LazyInitializationException("could not initialize proxy - no Session");
            }
        };
        note.setId("103");
        note.setText("Safe uninitialized proxy note");
        note.setLastupdated(new Timestamp(1700000123000L));

        Annotation annotation = commonImpl.transformNoteToAnnotation(note);

        assertNotNull(annotation);
        assertEquals("Safe uninitialized proxy note", annotation.getText());
        assertEquals(new Date(1700000123000L), annotation.getTime());
        assertNull(annotation.getAuthor());
    }
}
