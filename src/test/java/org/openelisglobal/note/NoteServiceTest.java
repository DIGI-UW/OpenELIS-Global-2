package org.openelisglobal.note;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.hibernate.ObjectNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.note.service.NoteObject;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.springframework.beans.factory.annotation.Autowired;

public class NoteServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private NoteService noteService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    private static final String REF_ID = "1001";
    private static final String REF_TABLE_ID = "1";

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/notes.xml");
    }

    @Test
    public void deleteNote_shouldDeleteNote() throws Exception {
        Note savedNote = noteService.get("1");
        assertNotNull("Note should exist in dataset", savedNote);

        noteService.delete(savedNote);

        List<Note> remainingNotes = noteService.getAll();
        assertEquals("Note should be deleted", 1, remainingNotes.size());
    }

    @Test
    public void deleteAllNotes_shouldClearAllNotes() throws Exception {

        List<Note> initialNotes = noteService.getAll();
        assertEquals("Dataset should contain 2 notes", 2, initialNotes.size());

        noteService.deleteAll(initialNotes);
        List<Note> remainingNotes = noteService.getAll();

        assertTrue("All notes should be deleted", remainingNotes.isEmpty());
    }

    @Test
    public void getNote_shouldReturnNoteForValidId() throws Exception {
        Note note = noteService.get("1");
        assertNotNull("Note should exist in dataset", note);
        assertEquals("Note ID should match", "1", note.getId());
    }

    @Test
    public void getNotesOrderedByTypeAndLastUpdated_shouldReturnNotesInCorrectOrder() throws Exception {
        Note noteCriteria = new Note();
        noteCriteria.setReferenceId("1001");
        noteCriteria.setReferenceTableId("1");

        List<Note> notes = noteService.getAllNotesByRefIdRefTable(noteCriteria);

        assertNotNull("Returned notes list should not be null", notes);
        assertEquals("There should be exactly 2 notes", 2, notes.size());

        assertEquals("First note should be INTERNAL", Note.INTERNAL, notes.get(0).getNoteType());
        assertEquals("First note's lastupdated should match the dataset",
                java.sql.Timestamp.valueOf("2024-02-22 10:00:00"), notes.get(0).getLastupdated());

        assertEquals("Second note should be EXTERNAL", Note.EXTERNAL, notes.get(1).getNoteType());
        assertEquals("Second note's lastupdated should match the dataset",
                java.sql.Timestamp.valueOf("2024-02-22 11:00:00"), notes.get(1).getLastupdated());
    }

    @Test
    public void getNotesByRefIAndRefTableAndSubject_shouldReturnMatchingNotes() throws Exception {
        List<Note> notes = noteService.getNoteByRefIAndRefTableAndSubject("1001", "1", "Subject 1");
        assertFalse("Notes should not be empty", notes.isEmpty());
        assertEquals("Subject should match", "Subject 1", notes.get(0).getSubject());
    }

    @Test
    public void getNotesChronologicallyByRefIdAndRefTable_shouldReturnNotesInOrder() throws Exception {
        List<Note> notes = noteService.getNotesChronologicallyByRefIdAndRefTable("1001", "1");
        assertFalse("Notes should not be empty", notes.isEmpty());

        Date previousTimestamp = notes.get(0).getLastupdated();
        for (Note note : notes) {
            assertTrue("Notes should be in chronological order",
                    note.getLastupdated().compareTo(previousTimestamp) >= 0);
            previousTimestamp = note.getLastupdated();
        }
    }

    @Test
    public void getTestNotesInDateRangeByType_shouldReturnEmptyListForNoMatchingNotes() throws Exception {
        java.sql.Date lowDate = java.sql.Date.valueOf("2023-01-01");
        java.sql.Date highDate = java.sql.Date.valueOf("2023-12-31");
        NoteServiceImpl.NoteType noteType = NoteServiceImpl.NoteType.INTERNAL;

        List<Note> notes = noteService.getTestNotesInDateRangeByType(lowDate, highDate, noteType);

        assertNotNull("Returned notes list should not be null", notes);
        assertTrue("Returned notes list should be empty", notes.isEmpty());
    }

    @Test
    public void getNotesInDateRangeByType_shouldHandleInvalidDateRange() throws Exception {
        java.sql.Date lowDate = java.sql.Date.valueOf("2024-12-31");
        java.sql.Date highDate = java.sql.Date.valueOf("2024-01-01");
        NoteServiceImpl.NoteType noteType = NoteServiceImpl.NoteType.INTERNAL;

        List<Note> notes = noteService.getTestNotesInDateRangeByType(lowDate, highDate, noteType);

        assertNotNull("Returned notes list should not be null", notes);
        assertTrue("Returned notes list should be empty for invalid date range", notes.isEmpty());
    }

    @Test
    public void getMostRecentNoteFilteredBySubject_shouldReturnMostRecentNote() throws Exception {
        Note mostRecentNote = noteService.getMostRecentNoteFilteredBySubject(new NoteObject() {
            @Override
            public String getTableId() {
                return "1";
            }

            @Override
            public String getObjectId() {
                return "1001";
            }

            @Override
            public NoteServiceImpl.BoundTo getBoundTo() {
                return NoteServiceImpl.BoundTo.SAMPLE;
            }
        }, "Subject 1");

        assertNotNull("Most recent note should not be null", mostRecentNote);
        assertEquals("Subject should match", "Subject 1", mostRecentNote.getSubject());
    }

    @Test
    public void getNotesByNoteTypeRefIdRefTable_shouldReturnMatchingNotes() throws Exception {
        Note noteCriteria = new Note();
        noteCriteria.setReferenceId("1001");
        noteCriteria.setReferenceTableId("1");
        noteCriteria.setNoteType(Note.INTERNAL);

        List<Note> notes = noteService.getNotesByNoteTypeRefIdRefTable(noteCriteria);
        assertFalse("Notes should not be empty", notes.isEmpty());
        assertEquals("Note type should match", Note.INTERNAL, notes.get(0).getNoteType());
    }

    @Test
    public void getNotesChronologicallyByRefIdAndRefTableAndType_shouldReturnMatchingNotes() throws Exception {
        List<String> filter = new ArrayList<>();
        filter.add(Note.INTERNAL);

        List<Note> notes = noteService.getNotesChronologicallyByRefIdAndRefTableAndType("1001", "1", filter);
        assertFalse("Notes should not be empty", notes.isEmpty());
        assertEquals("Note type should match", Note.INTERNAL, notes.get(0).getNoteType());
    }

    @Test
    public void createSavableNote_shouldCreateNoteWithCorrectProperties() throws Exception {
        NoteObject noteObject = new NoteObject() {
            @Override
            public String getTableId() {
                return "1";
            }

            @Override
            public String getObjectId() {
                return "1001";
            }

            @Override
            public NoteServiceImpl.BoundTo getBoundTo() {
                return NoteServiceImpl.BoundTo.SAMPLE;
            }
        };

        Note note = noteService.createSavableNote(noteObject, NoteServiceImpl.NoteType.INTERNAL, "Test Note",
                "Test Subject", "1");
        assertNotNull("Created note should not be null", note);
        assertEquals("Reference ID should match", "1001", note.getReferenceId());
        assertEquals("Reference Table ID should match", "1", note.getReferenceTableId());
        assertEquals("Note type should match", Note.INTERNAL, note.getNoteType());
        assertEquals("Note text should match", "Test Note", note.getText());
        assertEquals("Note subject should match", "Test Subject", note.getSubject());
    }

    @Test
    public void getNotesAsString_withNonConformityType_shouldHandleNonConformity() {

        NoteObject noteObject = createTestNoteObject("1001", "1");

        String result = noteService.getNotesAsString(noteObject, true, true, "\n",
                new NoteServiceImpl.NoteType[] { NoteServiceImpl.NoteType.NON_CONFORMITY }, false,
                StringUtil.EncodeContext.HTML);

        assertNull("Should return null when no matching notes found", result);
    }

    @Test
    public void getNotesAsString_shouldFormatTimestampsConsistently() {
        NoteObject noteObject = createTestNoteObject("1001", "1");
        Note note = noteService.get("1");

        String expectedTimestamp = DateUtil.convertTimestampToStringDateAndTime(note.getLastupdated());

        String result = noteService.getNotesAsString(noteObject, true, true, "|", false, StringUtil.EncodeContext.HTML);

        assertTrue("Should contain timestamp in expected format: " + expectedTimestamp,
                result.contains(expectedTimestamp));
    }

    @Test
    public void getNotesAsString_shouldReturnNullForEmptyNoteList() {
        NoteObject noteObject = createTestNoteObject("999", "1");

        String result = noteService.getNotesAsString(noteObject, true, true, "\n", false,
                StringUtil.EncodeContext.HTML);

        assertNull("Expected null for empty note list", result);
    }

    @Test
    public void getNotesAsString_withSubjectFilter_shouldReturnMatchingNote() {
        NoteObject noteObject = createTestNoteObject("1001", "1");
        String result = noteService.getNotesAsString(noteObject, "Subject 1", "SAMPLE");

        assertTrue(result.contains("Subject 1"));
        assertFalse(result.contains("Subject 2"));
    }

    @Test
    public void getNotesAsString_withTypeFilter_shouldFilterCorrectly() {
        NoteObject noteObject = createTestNoteObject("1001", "1");
        String result = noteService.getNotesAsString(noteObject, true, true, "\n",
                new NoteServiceImpl.NoteType[] { NoteServiceImpl.NoteType.INTERNAL }, false,
                StringUtil.EncodeContext.HTML);

        assertTrue(result.contains("I"));
        assertFalse(result.contains("E"));
    }

    @Test
    public void getReferenceTableIdForNoteBinding_shouldReturnCorrectId() {
        List<Note> notes = noteService.getNoteByRefIAndRefTableAndSubject("1001", "1", "Subject 1");

        String tableId = null;
        if (notes != null && !notes.isEmpty()) {
            tableId = notes.get(0).getReferenceTableId();
        }

        assertEquals("1", tableId);
    }

    @Test
    public void getTableReferenceId_shouldReturnConstantValue() {
        List<Note> notes = noteService.getNoteByRefIAndRefTableAndSubject("1001", "1", "Subject 1");

        String referenceTableId = notes.isEmpty() ? null : notes.get(0).getReferenceTableId();

        assertEquals("Note should return correct reference", "1", referenceTableId);
    }

    @Test(expected = ObjectNotFoundException.class)
    public void getNote_shouldThrowExceptionForInvalidId() {
        noteService.get("999");
    }

    @Test
    public void getNotesChronologicallyByRefIdAndRefTableAndType_shouldFilterByType() {
        List<String> typeFilter = new ArrayList<>();
        typeFilter.add("I");

        List<Note> notes = noteService.getNotesChronologicallyByRefIdAndRefTableAndType("1001", "1", typeFilter);

        assertEquals(1, notes.size());
        assertEquals("I", notes.get(0).getNoteType());
    }

    private NoteObject createTestNoteObject(String refId, String tableId) {
        return new NoteObject() {
            @Override
            public String getTableId() {
                return tableId;
            }

            @Override
            public String getObjectId() {
                return refId;
            }

            @Override
            public NoteServiceImpl.BoundTo getBoundTo() {
                return NoteServiceImpl.BoundTo.SAMPLE;
            }

            @Override
            public String toString() {
                return "TestNoteObject[refId=" + refId + ", tableId=" + tableId + "]";
            }
        };
    }
}
