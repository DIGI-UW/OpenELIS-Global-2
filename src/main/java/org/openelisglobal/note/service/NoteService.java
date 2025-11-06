package org.openelisglobal.note.service;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.util.StringUtil.EncodeContext;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;

public interface NoteService extends BaseObjectService<Note, String> {

    Note getData(String noteId);

    List<Note> getNoteByRefIAndRefTableAndSubject(String refId, String table_id, String subject);

    String getNotesAsString(NoteObject noteObject, String prefix, String noteSeparator);

    Note createSavableNote(NoteObject noteObject, NoteType type, String text, String subject, String currentUserId);

    Note getMostRecentNoteFilteredBySubject(NoteObject noteObject, String filter);

    String getNotesAsString(NoteObject noteObject, boolean prefixType, boolean prefixTimestamp, String noteSeparator,
            boolean excludeExternPrefix, EncodeContext context);

    String getNotesAsString(NoteObject noteObject, boolean prefixType, boolean prefixTimestamp, String noteSeparator,
            boolean excludeExternPrefix);

    String getNotesAsString(NoteObject noteObject, boolean prefixType, boolean prefixTimestamp, String noteSeparator,
            NoteType[] filter, boolean excludeExternPrefix, EncodeContext context);

    String getNotesAsString(NoteObject noteObject, boolean prefixType, boolean prefixTimestamp, String noteSeparator,
            NoteType[] filter, boolean excludeExternPrefix);

    List<Note> getNotesByNoteTypeRefIdRefTable(Note note);

    List<Note> getNotesInDateRangeAndType(Date lowDate, Date highDate, String noteType, String referenceTableId);

    List<Note> getAllNotesByRefIdRefTable(Note note);

    List<Note> getNotesChronologicallyByRefIdAndRefTable(String refId, String table_id);

    List<Note> getNotesChronologicallyByRefIdAndRefTableAndType(String objectId, String tableId, List<String> filter);

    List<Note> getNotes(NoteObject noteObject);

    boolean duplicateNoteExists(Note note);

    List<Note> getTestNotesInDateRangeByType(Date lowDate, Date highDate, NoteType noteType);
}
