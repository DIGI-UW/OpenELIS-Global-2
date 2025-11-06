package org.openelisglobal.note.service;

import jakarta.annotation.PostConstruct;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisServiceImpl;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.services.QAService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.util.StringUtil.EncodeContext;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.note.dao.NoteDAO;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.sample.service.SampleServiceImpl;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemServiceImpl;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@DependsOn({ "springContext" })
public class NoteServiceImpl extends AuditableBaseObjectServiceImpl<Note, String> implements NoteService {

    public enum NoteType {
        EXTERNAL(Note.EXTERNAL), INTERNAL(Note.INTERNAL), REJECTION_REASON(Note.REJECT_REASON),
        NON_CONFORMITY(Note.NON_CONFORMITY);

        String DBCode;

        NoteType(String dbCode) {
            DBCode = dbCode;
        }

        @Transactional(readOnly = true)
        public String getDBCode() {
            return DBCode;
        }
    }

    public enum BoundTo {
        ANALYSIS, QA_EVENT, SAMPLE, SAMPLE_ITEM
    }

    private static boolean SUPPORT_INTERNAL_EXTERNAL = ConfigurationProperties.getInstance()
            .isPropertyValueEqual(Property.NOTE_EXTERNAL_ONLY_FOR_VALIDATION, "true");
    private static String TABLE_REFERENCE_ID;

    @Autowired
    private NoteDAO baseObjectDAO;

    @Autowired
    private SampleQaEventService sampleQAService;
    @Autowired
    private ReferenceTablesService refTableService;

    @PostConstruct
    public void initializeGlobalVariables() {
        TABLE_REFERENCE_ID = refTableService.getReferenceTableByName("NOTE").getId();
    }

    NoteServiceImpl() {
        super(Note.class);
        this.auditTrailLog = true;
    }

    public static String getTableReferenceId() {
        return TABLE_REFERENCE_ID;
    }

    @Override
    protected NoteDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Transactional(readOnly = true)
    @Override
    public String getNotesAsString(NoteObject noteObject, boolean prefixType, boolean prefixTimestamp,
            String noteSeparator, NoteType[] filter, boolean excludeExternPrefix) {
        return getNotesAsString(noteObject, prefixType, prefixTimestamp, noteSeparator, filter, excludeExternPrefix,
                EncodeContext.HTML);
    }

    @Transactional(readOnly = true)
    @Override
    public String getNotesAsString(NoteObject noteObject, boolean prefixType, boolean prefixTimestamp,
            String noteSeparator, NoteType[] filter, boolean excludeExternPrefix, EncodeContext context) {
        boolean includeNoneConformity = false;
        List<String> dbFilter = new ArrayList<>(filter.length);
        for (NoteType type : filter) {
            if (type == NoteType.NON_CONFORMITY) {
                includeNoneConformity = true;
            }

            dbFilter.add(type.getDBCode());
        }

        List<Note> noteList = getNotesChronologicallyByRefIdAndRefTableAndType(noteObject.getObjectId(),
                noteObject.getTableId(), dbFilter);

        if (includeNoneConformity) {
            List<Note> nonConformityNoteList = getNonConformityReasons(noteObject);
            if (!nonConformityNoteList.isEmpty()) {
                noteList.addAll(nonConformityNoteList);
                Collections.sort(noteList, new Comparator<Note>() {
                    @Override
                    public int compare(Note o1, Note o2) {
                        return o1.getLastupdated().compareTo(o2.getLastupdated());
                    }
                });
            }
        }

        return notesToString(noteObject, prefixType, prefixTimestamp, noteSeparator, noteList, excludeExternPrefix,
                context);
    }

    private List<Note> getNonConformityReasons(NoteObject noteObject) {
        ArrayList<Note> notes = new ArrayList<>();

        if (noteObject.getBoundTo() == BoundTo.QA_EVENT) {
            return notes;
        }

        ArrayList<String> filter = new ArrayList<>(1);
        filter.add(NoteType.NON_CONFORMITY.getDBCode());
        Sample sample = null;
        SampleItem sampleItem = null;

        // get parent objects and the qa notes
        if (noteObject.getBoundTo() == BoundTo.ANALYSIS) {
            sampleItem = ((Analysis) noteObject).getSampleItem();
            notes.addAll(getNotesChronologicallyByRefIdAndRefTableAndType(sampleItem.getId(),
                    SampleItemServiceImpl.getSampleItemTableReferenceId(), filter));

            sample = sampleItem.getSample();
            notes.addAll(baseObjectDAO.getNotesChronologicallyByRefIdAndRefTableAndType(sample.getId(),
                    SampleServiceImpl.getTableReferenceId(), filter));
        } else if (noteObject.getBoundTo() == BoundTo.SAMPLE_ITEM) {
            sampleItem = (SampleItem) noteObject;
            sample = sampleItem.getSample();
            notes.addAll(baseObjectDAO.getNotesChronologicallyByRefIdAndRefTableAndType(sample.getId(),
                    SampleServiceImpl.getTableReferenceId(), filter));
        }

        if (sample != null) {
            List<SampleQaEvent> sampleQAList = sampleQAService.getSampleQaEventsBySample(sample);
            for (SampleQaEvent event : sampleQAList) {
                if (sampleItem == null || event.getSampleItem() == null
                        || sampleItem.getId().equals(event.getSampleItem().getId())) {
                    notes.addAll(baseObjectDAO.getNotesChronologicallyByRefIdAndRefTableAndType(event.getId(),
                            QAService.TABLE_REFERENCE_ID, filter));
                    Note proxyNote = new Note();
                    proxyNote.setNoteType(Note.NON_CONFORMITY);
                    proxyNote.setText(event.getQaEvent().getLocalizedName());
                    proxyNote.setLastupdated(event.getLastupdated());
                    notes.add(proxyNote);
                }
            }
        }

        return notes;
    }

    @Transactional(readOnly = true)
    @Override
    public String getNotesAsString(NoteObject noteObject, boolean prefixType, boolean prefixTimestamp,
            String noteSeparator, boolean excludeExternPrefix) {
        return getNotesAsString(noteObject, prefixType, prefixTimestamp, noteSeparator, excludeExternPrefix,
                EncodeContext.HTML);
    }

    @Transactional(readOnly = true)
    @Override
    public String getNotesAsString(NoteObject noteObject, boolean prefixType, boolean prefixTimestamp,
            String noteSeparator, boolean excludeExternPrefix, EncodeContext context) {
        List<Note> noteList = getNotesChronologicallyByRefIdAndRefTable(noteObject.getObjectId(),
                noteObject.getTableId());

        return notesToString(noteObject, prefixType, prefixTimestamp, noteSeparator, noteList, excludeExternPrefix,
                context);
    }

    private String notesToString(NoteObject noteObject, boolean prefixType, boolean prefixTimestamp,
            String noteSeparator, List<Note> noteList, boolean excludeExternPrefix, EncodeContext context) {
        if (noteList.isEmpty()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();

        for (Note note : noteList) {
            if (prefixType) {
                builder.append(StringUtil.encodeForContext(getNotePrefix(note, excludeExternPrefix), context));
                builder.append(" ");
            }

            if (prefixTimestamp) {
                builder.append(StringUtil.encodeForContext(getNoteTimestamp(note), context));
                builder.append(" ");
            }

            if (prefixType || prefixTimestamp) {
                builder.append(": ");
            }
            builder.append(StringUtil.encodeForContext(note.getText(), context));

            builder.append(StringUtil.blankIfNull(noteSeparator));
        }

        if (!GenericValidator.isBlankOrNull(noteSeparator)) {
            builder.setLength(builder.lastIndexOf(noteSeparator));
        }

        return builder.toString();
    }

    @Transactional(readOnly = true)
    @Override
    public List<Note> getNotes(NoteObject noteObject) {
        return getNotesChronologicallyByRefIdAndRefTable(noteObject.getObjectId(), noteObject.getTableId());
    }

    @Transactional(readOnly = true)
    @Override
    public String getNotesAsString(NoteObject noteObject, String prefix, String noteSeparator) {
        List<Note> noteList = getNotesChronologicallyByRefIdAndRefTable(noteObject.getObjectId(),
                noteObject.getTableId());

        if (noteList.isEmpty()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();

        for (Note note : noteList) {
            builder.append(StringUtil.blankIfNull(prefix));
            builder.append(note.getText());
            builder.append(StringUtil.blankIfNull(noteSeparator));
        }

        if (!GenericValidator.isBlankOrNull(noteSeparator)) {
            builder.setLength(builder.lastIndexOf(noteSeparator));
        }

        return builder.toString();
    }

    @Transactional(readOnly = true)
    @Override
    public Note getMostRecentNoteFilteredBySubject(NoteObject noteObject, String filter) {
        List<Note> noteList;
        if (GenericValidator.isBlankOrNull(filter)) {
            noteList = getNotesChronologicallyByRefIdAndRefTable(noteObject.getObjectId(), noteObject.getTableId());
            if (!noteList.isEmpty()) {
                return noteList.get(noteList.size() - 1);
            }
        } else {
            noteList = getNoteByRefIAndRefTableAndSubject(noteObject.getObjectId(), noteObject.getTableId(), filter);
            if (!noteList.isEmpty()) {
                return noteList.get(0);
            }
        }

        return null;
    }

    private String getNoteTimestamp(Note note) {
        return DateUtil.convertTimestampToStringDateAndTime(note.getLastupdated());
    }

    @Override
    public Note createSavableNote(NoteObject noteObject, NoteType type, String text, String subject,
            String currentUserId) {
        if (GenericValidator.isBlankOrNull(text)) {
            return null;
        }

        Note note = new Note();
        note.setReferenceId(noteObject.getObjectId());
        note.setReferenceTableId(noteObject.getTableId());
        note.setNoteType(type.getDBCode());
        note.setSubject(subject);
        note.setText(text);
        note.setSysUserId(currentUserId);
        note.setSystemUser(createSystemUser(currentUserId));

        return note;
    }

    public static SystemUser createSystemUser(String currentUserId) {
        SystemUser systemUser = new SystemUser();
        systemUser.setId(currentUserId);
        SpringContext.getBean(SystemUserService.class).getData(systemUser);
        return systemUser;
    }

    public static String getReferenceTableIdForNoteBinding(BoundTo binding) {
        switch (binding) {
        case ANALYSIS: {
            return AnalysisServiceImpl.getTableReferenceId();
        }
        case QA_EVENT: {
            return QAService.TABLE_REFERENCE_ID;
        }
        case SAMPLE: {
            return SampleServiceImpl.getTableReferenceId();
        }
        case SAMPLE_ITEM: {
            return SampleItemServiceImpl.getSampleItemTableReferenceId();
        }
        default: {
            return null;
        }
        }
    }

    public List<Note> getTestNotesInDateRangeByType(Date lowDate, Date highDate, NoteType noteType) {
        return baseObjectDAO.getNotesInDateRangeAndType(lowDate, DateUtil.addDaysToSQLDate(highDate, 1),
                noteType.DBCode, AnalysisServiceImpl.getTableReferenceId());
    }

    private String getNotePrefix(Note note, boolean excludeExternPrefix) {
        if (SUPPORT_INTERNAL_EXTERNAL) {
            if (Note.INTERNAL.equals(note.getNoteType())) {
                return MessageUtil.getMessage("note.type.internal");
            } else if (Note.EXTERNAL.equals(note.getNoteType())) {
                return excludeExternPrefix ? "" : MessageUtil.getMessage("note.type.external");
            } else if (Note.REJECT_REASON.equals(note.getNoteType())) {
                return MessageUtil.getMessage("note.type.rejectReason");
            } else if (Note.NON_CONFORMITY.equals(note.getNoteType())) {
                return MessageUtil.getMessage("note.type.nonConformity");
            }
        }

        return "";
    }

    @Override
    @Transactional(readOnly = true)
    public Note getData(String noteId) {
        return getBaseObjectDAO().getData(noteId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Note> getNotesByNoteTypeRefIdRefTable(Note note) {
        Map<String, Object> propertyValues = new HashMap<>();
        propertyValues.put("referenceId", note.getReferenceId());
        propertyValues.put("referenceTableId", note.getReferenceTableId());
        propertyValues.put("noteType", note.getNoteType());
        return getBaseObjectDAO().getAllMatchingOrdered(propertyValues, "lastupdated", false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Note> getNotesInDateRangeAndType(Date lowDate, Date highDate, String noteType,
            String referenceTableId) {
        return getBaseObjectDAO().getNotesInDateRangeAndType(lowDate, highDate, noteType, referenceTableId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Note> getAllNotesByRefIdRefTable(Note note) {
        return getBaseObjectDAO().getAllNotesByRefIdRefTable(note);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Note> getNotesChronologicallyByRefIdAndRefTable(String refId, String table_id) {
        Map<String, Object> propertyValues = new HashMap<>();
        propertyValues.put("referenceId", refId);
        propertyValues.put("referenceTableId", table_id);
        return baseObjectDAO.getAllMatchingOrdered(propertyValues, "lastupdated", false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Note> getNotesChronologicallyByRefIdAndRefTableAndType(String objectId, String tableId,
            List<String> filter) {
        return getBaseObjectDAO().getNotesChronologicallyByRefIdAndRefTableAndType(objectId, tableId, filter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Note> getNoteByRefIAndRefTableAndSubject(String refId, String table_id, String subject) {
        Map<String, Object> propertyValues = new HashMap<>();
        propertyValues.put("referenceId", refId);
        propertyValues.put("referenceTableId", table_id);
        propertyValues.put("subject", subject);
        return baseObjectDAO.getAllMatching(propertyValues);
    }

    @Override
    public String insert(Note note) {
        if (getBaseObjectDAO().duplicateNoteExists(note)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + note.getNoteType());
        }
        return super.insert(note);
    }

    @Override
    public Note save(Note note) {
        if (getBaseObjectDAO().duplicateNoteExists(note)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + note.getNoteType());
        }
        return super.save(note);
    }

    @Override
    public Note update(Note note) {
        if (getBaseObjectDAO().duplicateNoteExists(note)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + note.getNoteType());
        }
        return super.update(note);
    }

    @Override
    public boolean duplicateNoteExists(Note note) {
        return getBaseObjectDAO().duplicateNoteExists(note);
    }
}
