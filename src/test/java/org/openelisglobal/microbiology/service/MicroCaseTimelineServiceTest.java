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
import org.openelisglobal.microbiology.form.MicroCaseActivityForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.note.service.NoteObject;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl;
import org.openelisglobal.note.valueholder.Note;
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
    private MicrobiologyCaseAccessService access;
    private MicroCaseTimelineService service;

    @Before
    public void setUp() {
        service = new MicroCaseTimelineServiceImpl(caseDAO, activityDAO, noteService, systemUserService,
                "sample-item-table");
        org.springframework.test.util.ReflectionTestUtils.setField(service, "access", access);
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setSampleItemId("sample-item-1");
        org.mockito.Mockito.lenient().when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        org.mockito.Mockito.lenient().when(caseDAO.getForUpdate("case-1")).thenReturn(microCase);
        org.mockito.Mockito.lenient().when(
                access.hasLabUnitRole(eq("42"), any(), eq(org.openelisglobal.common.constants.Constants.ROLE_RESULTS)))
                .thenReturn(true);
    }

    @Test
    public void addsInternalSampleItemNoteWithStableCaseSubjectAndActor() {
        Note note = note("note-1", "Bench observation", "MICROBIOLOGY_CASE:case-1", 2000L);
        when(noteService.createSavableNote(any(NoteObject.class), eq(NoteServiceImpl.NoteType.INTERNAL),
                eq("Bench observation"), eq("MICROBIOLOGY_CASE:case-1"), eq("42"))).thenReturn(note);

        MicroCaseActivityForm result = service.addNote("case-1", " Bench observation ", "42");

        assertEquals(MicroCaseActivityType.MANUAL_NOTE.name(), result.activityType);
        assertEquals("Bench observation", result.note);
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
                List.of(Note.INTERNAL, Note.EXTERNAL))).thenReturn(List.of(currentCase, siblingCase));
        when(systemUserService.getUserById("42")).thenReturn(user);

        List<MicroCaseActivityForm> timeline = service.getTimeline("case-1");

        assertEquals(2, timeline.size());
        assertEquals(MicroCaseActivityType.INOCULATION_RECORDED.name(), timeline.get(0).activityType);
        assertEquals("Olivia Mendez", timeline.get(0).performedByDisplay);
        assertEquals(MicroCaseActivityType.MANUAL_NOTE.name(), timeline.get(1).activityType);
        assertEquals("Current case", timeline.get(1).note);
        assertEquals("Olivia Mendez", timeline.get(1).performedByDisplay);
    }

    @Test
    public void attachmentNotesStayScopedAndOnlyExternalNotesReachReports() {
        var external = new MicroCaseActivity();
        external.setCaseId("case-1");
        external.setActivityType("MANUAL_NOTE");
        external.setStructuredData("SEND_RESULT:CULTURE:culture-1");
        external.setNote("Culture report comment");
        external.setOccurredAt(new Timestamp(1000));
        var internal = new MicroCaseActivity();
        internal.setCaseId("case-1");
        internal.setActivityType("MANUAL_NOTE");
        internal.setStructuredData("IN_LAB_ONLY:CULTURE:culture-1");
        internal.setNote("Private culture comment");
        internal.setOccurredAt(new Timestamp(2000));
        when(activityDAO.getByCaseId("case-1")).thenReturn(List.of(external, internal));
        var timeline = service.getTimeline("case-1");
        assertEquals(2, timeline.size());
        assertEquals("CULTURE", timeline.get(0).targetType);
        assertEquals("culture-1", timeline.get(0).targetId);
        assertEquals(List.of("Culture report comment"),
                service.getReportNotes("case-1").stream().map(n -> n.note).toList());
    }

    @Test
    public void persistsUuidAttachmentAsActivityInsteadOfOverflowingLegacyNoteSubject() {
        var cultures = org.mockito.Mockito.mock(org.openelisglobal.microbiology.dao.MicroCaseInoculationDAO.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "cultures", cultures);
        String cultureId = java.util.UUID.randomUUID().toString();
        var row = new org.openelisglobal.microbiology.valueholder.MicroCaseInoculation();
        row.setId(cultureId);
        row.setCaseId("case-1");
        when(cultures.get(cultureId)).thenReturn(Optional.of(row));
        var result = service.addNote("case-1", "Report beside this culture", true, "CULTURE", cultureId, "42");
        var persisted = org.mockito.ArgumentCaptor.forClass(MicroCaseActivity.class);
        org.mockito.Mockito.verify(activityDAO).insert(persisted.capture());
        assertEquals("case-1", persisted.getValue().getCaseId());
        assertEquals("SEND_RESULT:CULTURE:" + cultureId, persisted.getValue().getStructuredData());
        assertEquals(cultureId, result.targetId);
        assertEquals("SEND_RESULT", result.structuredData);
        org.mockito.Mockito.verifyZeroInteractions(noteService);
    }

    @Test
    public void rejectsAttachmentToAnotherCasesCultureBeforeWriting() {
        var cultures = org.mockito.Mockito.mock(org.openelisglobal.microbiology.dao.MicroCaseInoculationDAO.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "cultures", cultures);
        var row = new org.openelisglobal.microbiology.valueholder.MicroCaseInoculation();
        row.setId("culture-other");
        row.setCaseId("case-other");
        when(cultures.get("culture-other")).thenReturn(Optional.of(row));
        try {
            service.addNote("case-1", "Cannot attach here", true, "CULTURE", "culture-other", "42");
            org.junit.Assert.fail("cross-case attachment accepted");
        } catch (IllegalArgumentException e) {
            assertEquals("MICROBIOLOGY_NOTE_TARGET_INVALID", e.getMessage());
        }
        org.mockito.Mockito.verifyZeroInteractions(noteService, activityDAO);
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
