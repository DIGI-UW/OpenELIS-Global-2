package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCaseSpecimenDAO;
import org.openelisglobal.microbiology.form.MicroCaseActivityForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCaseSpecimen;
import org.openelisglobal.note.service.NoteObject;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;

@RunWith(MockitoJUnitRunner.class)
public class MicroCaseTimelineServiceTest {

    @Mock
    private MicroCaseDAO caseDAO;
    @Mock
    private MicroCaseActivityDAO activityDAO;
    @Mock
    private NoteService noteService;
    @Mock
    private SystemUserService systemUserService;

    @Mock
    private MicroCaseSpecimenDAO specimenDAO;
    @Mock
    private MicrobiologyCaseAccessService accessService;
    @Mock
    private ReferenceTablesService referenceTablesService;

    private MicroCaseTimelineService service;

    @Before
    public void setUp() {
        service = new MicroCaseTimelineServiceImpl(caseDAO, activityDAO, noteService, referenceTablesService,
                systemUserService, specimenDAO, accessService);
        ReferenceTables orderTable = new ReferenceTables();
        orderTable.setId("sample-table");
        when(referenceTablesService.getReferenceTableByName("SAMPLE")).thenReturn(orderTable);
        ReferenceTables memberTable = new ReferenceTables();
        memberTable.setId("sample-item-table");
        when(referenceTablesService.getReferenceTableByName("SAMPLE_ITEM")).thenReturn(memberTable);
        MicroCaseSpecimen member = new MicroCaseSpecimen();
        member.setCaseId("case-1");
        member.setSampleItemId("sample-item-1");
        when(specimenDAO.getByCaseId("case-1")).thenReturn(List.of(member));
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setSampleId("order-1");
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
    }

    @Test
    public void addsInternalOrderNoteWithStableCaseSubjectAndActor() {
        Note note = note("note-1", "Bench observation", "MICROBIOLOGY_CASE:case-1", 2000L);
        when(noteService.createSavableNote(any(NoteObject.class), eq(NoteServiceImpl.NoteType.INTERNAL),
                eq("Bench observation"), eq("MICROBIOLOGY_CASE:case-1"), eq("42"))).thenReturn(note);

        MicroCaseActivityForm result = service.addNote("case-1", " Bench observation ", "42");

        assertEquals(MicroCaseActivityType.MANUAL_NOTE.name(), result.activityType);
        assertEquals("Bench observation", result.note);
        org.mockito.ArgumentCaptor<NoteObject> binding = org.mockito.ArgumentCaptor.forClass(NoteObject.class);
        verify(noteService).createSavableNote(binding.capture(), eq(NoteServiceImpl.NoteType.INTERNAL),
                eq("Bench observation"), eq("MICROBIOLOGY_CASE:case-1"), eq("42"));
        assertEquals("order-1", binding.getValue().getObjectId());
        assertEquals("sample-table", binding.getValue().getTableId());
        assertEquals(NoteServiceImpl.BoundTo.SAMPLE, binding.getValue().getBoundTo());
        verify(accessService).requireResults("case-1", "42");
        verify(noteService).insert(note);
    }

    @Test
    public void collatesTypedActivitiesAndOnlyNotesForTheCurrentCaseChronologically() {
        MicroCaseActivity activity = new MicroCaseActivity();
        activity.setId("activity-1");
        activity.setCaseId("case-1");
        activity.setActivityType(MicroCaseActivityType.INOCULATION_RECORDED.name());
        activity.setOccurredAt(new Timestamp(1000L));
        activity.setPerformedBy("42");
        Note currentCase = note("note-1", "Current case", "MICROBIOLOGY_CASE:case-1", 2000L);
        Note siblingCase = note("note-2", "Sibling case", "MICROBIOLOGY_CASE:case-2", 3000L);
        SystemUser user = new SystemUser();
        user.setFirstName("Olivia");
        user.setLastName("Mendez");
        when(activityDAO.getByCaseId("case-1")).thenReturn(List.of(activity));
        when(noteService.getNotesChronologicallyByRefIdAndRefTableAndType("sample-item-1", "sample-item-table",
                List.of(Note.INTERNAL))).thenReturn(List.of(currentCase, siblingCase));
        Note orderNote = note("note-3", "Order case note", "MICROBIOLOGY_CASE:case-1", 4000L);
        when(noteService.getNotesChronologicallyByRefIdAndRefTableAndType("order-1", "sample-table",
                List.of(Note.INTERNAL))).thenReturn(List.of(orderNote, siblingCase));
        when(systemUserService.getUserById("42")).thenReturn(user);

        List<MicroCaseActivityForm> timeline = service.getTimeline("case-1");

        assertEquals(3, timeline.size());
        assertEquals(MicroCaseActivityType.INOCULATION_RECORDED.name(), timeline.get(0).activityType);
        assertEquals("Olivia Mendez", timeline.get(0).performedByDisplay);
        assertEquals(MicroCaseActivityType.MANUAL_NOTE.name(), timeline.get(1).activityType);
        assertEquals("Current case", timeline.get(1).note);
        assertEquals("Olivia Mendez", timeline.get(1).performedByDisplay);
        assertEquals("Order case note", timeline.get(2).note);
    }

    @Test
    public void readOnlyCaseUserCannotAddATimelineNote() {
        org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException("unit rights"))
                .when(accessService).requireResults("case-1", "17");

        org.junit.Assert.assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.addNote("case-1", "forbidden note", "17"));

        org.mockito.Mockito.verifyZeroInteractions(noteService);
        org.mockito.Mockito.verifyZeroInteractions(activityDAO);
    }

    private Note note(String id, String text, String subject, long occurredAt) {
        Note note = new Note();
        note.setId(id);
        note.setText(text);
        note.setSubject(subject);
        note.setNoteType(Note.INTERNAL);
        note.setSysUserId("42");
        note.setLastupdated(new Timestamp(occurredAt));
        return note;
    }
}
