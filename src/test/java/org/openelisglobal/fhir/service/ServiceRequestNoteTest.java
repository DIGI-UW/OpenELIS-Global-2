package org.openelisglobal.fhir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.hibernate.LazyInitializationException;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.After;
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
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Issue #4900: Verify that internal notes (type "I", "R", "N", "U") are
 * excluded from ServiceRequest.note[] over the FHIR API, and only external
 * notes ("E") are published. Also verifies that transformNoteToAnnotation
 * populates text, timestamp, and author metadata.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ServiceRequestNoteTest {

    private static final String SITE_SYSTEM = "https://lab.example.org/fhir";

    @Mock
    private FhirConfig fhirConfig;
    @Mock
    private ElectronicOrderService electronicOrderService;
    @Mock
    private FhirPersistanceService fhirPersistanceService;
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
    private SampleService sampleService;
    @Mock
    private ObservationHistoryService observationHistoryService;
    @Mock
    private SampleLocationService sampleLocationService;
    @Mock
    private FhirCommonTransformService common;

    @InjectMocks
    private ServiceRequestTransformServiceImpl transformService;

    private final FhirCommonTransformServiceImpl commonImpl = new FhirCommonTransformServiceImpl();

    private Analysis analysis;
    private TableIdService tableIdsBefore;

    @Before
    public void setUp() {
        tableIdsBefore = TableIdService.getInstance();
        if (tableIdsBefore == null) {
            ReflectionTestUtils.setField(TableIdService.class, "INSTANCE", new TableIdService());
        }

        Sample sample = new Sample();
        sample.setId("1001");
        sample.setAccessionNumber("ACC-4900");

        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId("10");

        analysis = new Analysis();
        analysis.setId("42");
        analysis.setFhirUuid(UUID.randomUUID());
        analysis.setStatusId("1");
        analysis.setTest(test);

        Patient patient = new Patient();
        patient.setFhirUuid(UUID.randomUUID());

        when(fhirConfig.getOeFhirSystem()).thenReturn(SITE_SYSTEM);
        when(analysisAnchorService.resolveSample(analysis)).thenReturn(sample);
        when(statusService.getStatusID(any(AnalysisStatus.class))).thenReturn("-1");
        when(sampleHumanService.getPatientForSample(any(Sample.class))).thenReturn(patient);
        when(common.transformNoteToAnnotation(any(Note.class)))
                .thenAnswer(call -> commonImpl.transformNoteToAnnotation(call.getArgument(0)));
        when(common.createReferenceFor(any(ResourceType.class), anyString()))
                .thenAnswer(call -> new Reference(call.getArgument(0) + "/" + call.getArgument(1)));
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(TableIdService.class, "INSTANCE", tableIdsBefore);
    }

    @Test
    public void transformNoteToAnnotationHandlesNullGracefully() {
        assertNull(commonImpl.transformNoteToAnnotation(null));
    }

    @Test
    public void transformNoteToAnnotationPopulatesTextTimeAndAuthor() {
        Note note = new Note();
        note.setText("External clinical note");
        Timestamp now = new Timestamp(System.currentTimeMillis());
        note.setLastupdated(now);

        SystemUser user = new SystemUser();
        user.setFirstName("Jane");
        user.setLastName("Doe");
        note.setSystemUser(user);

        Annotation annotation = commonImpl.transformNoteToAnnotation(note);

        assertNotNull(annotation);
        assertEquals("External clinical note", annotation.getText());
        assertEquals(now, annotation.getTime());
        assertTrue(annotation.hasAuthorStringType());
        assertEquals("Jane Doe", annotation.getAuthorStringType().getValue());
    }

    @Test
    public void transformNoteToAnnotationFallsBackToLoginNameWhenNameIsBlank() {
        Note note = new Note();
        note.setText("Note with login author");

        SystemUser user = new SystemUser();
        user.setLoginName("labtech1");
        note.setSystemUser(user);

        Annotation annotation = commonImpl.transformNoteToAnnotation(note);

        assertNotNull(annotation);
        assertTrue(annotation.hasAuthorStringType());
        assertEquals("labtech1", annotation.getAuthorStringType().getValue());
    }

    @Test
    public void transformNoteToAnnotationHandlesMissingAuthorAndTime() {
        Note note = new Note();
        note.setText("Plain note");

        Annotation annotation = commonImpl.transformNoteToAnnotation(note);

        assertNotNull(annotation);
        assertEquals("Plain note", annotation.getText());
        assertFalse(annotation.hasTime());
        assertFalse(annotation.hasAuthor());
    }

    @Test
    public void internalNotesAreExcludedFromServiceRequestNotes() {
        Note externalNote = new Note();
        externalNote.setNoteType(Note.EXTERNAL);
        externalNote.setText("This is an external note for patient report");
        Timestamp ts1 = Timestamp.valueOf("2026-11-04 10:00:00");
        externalNote.setLastupdated(ts1);
        SystemUser user1 = new SystemUser();
        user1.setFirstName("Alice");
        user1.setLastName("Smith");
        externalNote.setSystemUser(user1);

        Note internalNote1 = new Note();
        internalNote1.setNoteType(Note.INTERNAL);
        internalNote1.setText("Redo test in lab — do not publish");

        Note internalNote2 = new Note();
        internalNote2.setNoteType(Note.INTERNAL);
        internalNote2.setText("Validation retest note");

        Note rejectionNote = new Note();
        rejectionNote.setNoteType(Note.REJECT_REASON);
        rejectionNote.setText("Specimen clotted");

        Note nonConformityNote = new Note();
        nonConformityNote.setNoteType(Note.NON_CONFORMITY);
        nonConformityNote.setText("Temperature out of range");

        when(noteService.getNotes(analysis))
                .thenReturn(List.of(externalNote, internalNote1, internalNote2, rejectionNote, nonConformityNote));

        ServiceRequest serviceRequest = transformService.transformToServiceRequest(analysis);

        assertEquals(1, serviceRequest.getNote().size());
        Annotation publishedNote = serviceRequest.getNote().get(0);
        assertEquals("This is an external note for patient report", publishedNote.getText());
        assertEquals(ts1, publishedNote.getTime());
        assertEquals("Alice Smith", publishedNote.getAuthorStringType().getValue());
    }

    @Test
    public void serviceRequestHasNoNotesWhenOnlyInternalNotesExist() {
        Note internalNote = new Note();
        internalNote.setNoteType(Note.INTERNAL);
        internalNote.setText("Internal lab comment");

        when(noteService.getNotes(analysis)).thenReturn(List.of(internalNote));

        ServiceRequest serviceRequest = transformService.transformToServiceRequest(analysis);

        assertTrue(serviceRequest.getNote().isEmpty());
    }

    @Test
    public void serviceRequestIncludesMultipleExternalNotes() {
        Note ext1 = new Note();
        ext1.setNoteType(Note.EXTERNAL);
        ext1.setText("First external note");

        Note ext2 = new Note();
        ext2.setNoteType(Note.EXTERNAL);
        ext2.setText("Second external note");

        Note internalNote = new Note();
        internalNote.setNoteType(Note.INTERNAL);
        internalNote.setText("Internal comment");

        when(noteService.getNotes(analysis)).thenReturn(List.of(ext1, internalNote, ext2));

        ServiceRequest serviceRequest = transformService.transformToServiceRequest(analysis);

        assertEquals(2, serviceRequest.getNote().size());
        assertEquals("First external note", serviceRequest.getNote().get(0).getText());
        assertEquals("Second external note", serviceRequest.getNote().get(1).getText());
    }

    @Test
    public void serviceRequestHandlesNullOrEmptyNotesSafely() {
        when(noteService.getNotes(analysis)).thenReturn(null);
        ServiceRequest sr1 = transformService.transformToServiceRequest(analysis);
        assertTrue(sr1.getNote().isEmpty());

        when(noteService.getNotes(analysis)).thenReturn(Collections.emptyList());
        ServiceRequest sr2 = transformService.transformToServiceRequest(analysis);
        assertTrue(sr2.getNote().isEmpty());
    }

    @Test
    public void serviceRequestHandlesNotesListContainingNullElements() {
        Note ext = new Note();
        ext.setNoteType(Note.EXTERNAL);
        ext.setText("Valid external note");

        List<Note> notesWithNulls = new ArrayList<>();
        notesWithNulls.add(null);
        notesWithNulls.add(ext);
        notesWithNulls.add(null);

        when(noteService.getNotes(analysis)).thenReturn(notesWithNulls);

        ServiceRequest serviceRequest = transformService.transformToServiceRequest(analysis);

        assertEquals(1, serviceRequest.getNote().size());
        assertEquals("Valid external note", serviceRequest.getNote().get(0).getText());
    }

    @Test
    public void transformNoteToAnnotationFallsBackToLoginNameWhenNamesAreWhitespaceOnly() {
        Note note = new Note();
        note.setText("Note with whitespace author names");

        SystemUser user = new SystemUser();
        user.setFirstName("   ");
        user.setLastName("   ");
        user.setLoginName("labtech_whitespace");
        note.setSystemUser(user);

        Annotation annotation = commonImpl.transformNoteToAnnotation(note);

        assertNotNull(annotation);
        assertTrue(annotation.hasAuthorStringType());
        assertEquals("labtech_whitespace", annotation.getAuthorStringType().getValue());
    }

    @Test
    public void transformNoteToAnnotationGracefullyHandlesLazyInitializationException() {
        Note note = new Note();
        note.setText("Note with uninitialized proxy user");

        SystemUser lazyProxy = org.mockito.Mockito.mock(SystemUser.class);
        when(lazyProxy.getFirstName()).thenThrow(new LazyInitializationException("Session closed"));
        note.setSystemUser(lazyProxy);

        Annotation annotation = commonImpl.transformNoteToAnnotation(note);

        assertNotNull(annotation);
        assertEquals("Note with uninitialized proxy user", annotation.getText());
        assertFalse(annotation.hasAuthor());
    }
}
