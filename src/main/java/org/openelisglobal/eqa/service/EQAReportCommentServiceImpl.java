package org.openelisglobal.eqa.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.eqa.dao.EQACycleDAO;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EQAReportCommentServiceImpl implements EQAReportCommentService {

    /** The table a report comment's note points at. */
    private static final String REFERENCE_TABLE = "eqa_cycle";

    /**
     * note.text is varchar(1000) while dict_entry allows 4000. A library entry
     * longer than the column is a configuration fault, and truncating it would put
     * half a sentence on a document someone signs.
     */
    private static final int MAX_TEXT_LENGTH = 1000;

    /** Hibernate maps dictionary_category.name to this property. */
    private static final String CATEGORY_FIELD = "categoryName";

    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private NoteService noteService;
    @Autowired
    private ReferenceTablesService referenceTablesService;
    @Autowired
    private EQACycleDAO eqaCycleDAO;

    @Override
    public List<LibraryEntry> getLibrary() {
        List<LibraryEntry> entries = new ArrayList<>();
        for (Dictionary entry : activeLibrary()) {
            entries.add(new LibraryEntry(entry.getId(), entry.getDictEntry()));
        }
        return entries;
    }

    @Override
    public List<AttachedComment> getComments(Long cycleId) {
        List<AttachedComment> comments = new ArrayList<>();
        for (Note note : attachedNotes(cycleId)) {
            comments.add(toComment(note));
        }
        return comments;
    }

    @Override
    @Transactional
    public List<AttachedComment> attach(Long cycleId, List<String> libraryEntryIds, String currentUserId) {
        requireCycle(cycleId);
        if (libraryEntryIds == null || libraryEntryIds.isEmpty()) {
            throw new IllegalArgumentException("No comment was selected");
        }

        Map<String, String> library = new LinkedHashMap<>();
        for (Dictionary entry : activeLibrary()) {
            library.put(entry.getId(), entry.getDictEntry());
        }

        // Already-attached ids are skipped rather than rejected: a resent
        // selection must not print the same paragraph twice, and must not fail
        // the ids alongside it either.
        Set<String> attached = new HashSet<>();
        for (Note note : attachedNotes(cycleId)) {
            attached.add(note.getSubject());
        }

        String referenceTableId = referenceTableId();
        List<AttachedComment> added = new ArrayList<>();
        for (String entryId : libraryEntryIds) {
            String text = library.get(entryId);
            if (text == null) {
                throw new IllegalArgumentException(
                        "Comment " + entryId + " is not an active entry of the " + CATEGORY_NAME + " library");
            }
            if (text.length() > MAX_TEXT_LENGTH) {
                throw new IllegalArgumentException("Comment " + entryId + " is longer than " + MAX_TEXT_LENGTH
                        + " characters and cannot be stored");
            }
            if (!attached.add(entryId)) {
                continue;
            }

            Note note = new Note();
            note.setReferenceId(String.valueOf(cycleId));
            note.setReferenceTableId(referenceTableId);
            note.setNoteType(NoteType.EXTERNAL.getDBCode());
            // The library entry id, so an attachment stays traceable to the phrase
            // it came from even after the phrase is reworded or retired.
            note.setSubject(entryId);
            note.setText(text);
            // SYS_USER_ID is mapped through the systemUser relation, not the
            // systemUserId field: setting the field alone would persist no author.
            note.setSystemUser(NoteServiceImpl.createSystemUser(currentUserId));
            note.setSysUserId(currentUserId);
            noteService.insert(note);
            added.add(toComment(note));
        }
        return added;
    }

    @Override
    @Transactional
    public void detach(Long cycleId, String commentId) {
        // Searched among this cycle's attachments rather than fetched by id: a
        // note belonging to another cycle, or already gone, is the same answer
        // here, and looking it up by id alone throws instead of saying so.
        Note note = attachedNotes(cycleId).stream().filter(candidate -> commentId.equals(candidate.getId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Comment " + commentId + " is not attached to cycle " + cycleId));
        noteService.delete(note);
    }

    private AttachedComment toComment(Note note) {
        return new AttachedComment(note.getId(), note.getSubject(), note.getText(), authorName(note),
                note.getLastupdated() == null ? null : DateUtil.formatDateTimeAsText(note.getLastupdated()));
    }

    /** Active entries of the library category, in their configured order. */
    private List<Dictionary> activeLibrary() {
        return dictionaryService.getDictionaryEntrysByCategoryAbbreviation(CATEGORY_FIELD, CATEGORY_NAME, false);
    }

    private List<Note> attachedNotes(Long cycleId) {
        List<Note> notes = noteService.getNotesChronologicallyByRefIdAndRefTableAndType(String.valueOf(cycleId),
                referenceTableId(), List.of(NoteType.EXTERNAL.getDBCode()));
        return notes == null ? List.of() : notes;
    }

    private String referenceTableId() {
        ReferenceTables table = referenceTablesService.getReferenceTableByName(REFERENCE_TABLE);
        if (table == null) {
            throw new IllegalStateException(
                    "reference_tables has no " + REFERENCE_TABLE + " row; liquibase qa/032 has not run");
        }
        return table.getId();
    }

    private void requireCycle(Long cycleId) {
        if (cycleId == null || eqaCycleDAO.get(cycleId).isEmpty()) {
            throw new IllegalArgumentException("Unknown cycle " + cycleId);
        }
    }

    /** Resolved inside the transaction: the relation is fetched, not eager. */
    private String authorName(Note note) {
        SystemUser author = note.getSystemUser();
        return author == null ? null : author.getNameForDisplay();
    }
}
