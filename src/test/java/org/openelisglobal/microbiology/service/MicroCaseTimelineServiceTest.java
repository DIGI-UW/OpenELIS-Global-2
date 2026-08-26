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

@RunWith(MockitoJUnitRunner.class)
public class MicroCaseTimelineServiceTest {

    @Mock
    private MicroCaseDAO caseDAO;
    @Mock
    private MicroCaseActivityDAO activityDAO;
    @Mock
    private NoteService noteService;

    private MicroCaseTimelineService service;

    @Before
    public void setUp() {
        service = new MicroCaseTimelineServiceImpl(caseDAO, activityDAO, noteService, "sample-item-table");
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setSampleItemId("sample-item-1");
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
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
        Note currentCase = note("note-1", "Current case", "MICROBIOLOGY_CASE:case-1", 2000L);
        Note siblingCase = note("note-2", "Sibling case", "MICROBIOLOGY_CASE:case-2", 3000L);
        when(activityDAO.getByCaseId("case-1")).thenReturn(List.of(activity));
        when(noteService.getNotesChronologicallyByRefIdAndRefTableAndType("sample-item-1", "sample-item-table",
                List.of(Note.INTERNAL))).thenReturn(List.of(currentCase, siblingCase));

        List<MicroCaseActivityForm> timeline = service.getTimeline("case-1");

        assertEquals(2, timeline.size());
        assertEquals(MicroCaseActivityType.INOCULATION_RECORDED.name(), timeline.get(0).activityType);
        assertEquals(MicroCaseActivityType.MANUAL_NOTE.name(), timeline.get(1).activityType);
        assertEquals("Current case", timeline.get(1).note);
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
