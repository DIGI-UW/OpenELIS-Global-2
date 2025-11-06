/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.note.daoimpl;

import java.sql.Date;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.note.dao.NoteDAO;
import org.openelisglobal.note.valueholder.Note;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
@Component
@Transactional
public class NoteDAOImpl extends BaseDAOImpl<Note, String> implements NoteDAO {

    public NoteDAOImpl() {
        super(Note.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Note getData(String noteId) throws LIMSRuntimeException {
        try {
            Note note = entityManager.unwrap(Session.class).get(Note.class, noteId);
            return note;
        } catch (RuntimeException e) {
            handleException(e, "getData");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Note> getAllNotesByRefIdRefTable(Note note) throws LIMSRuntimeException {
        try {

            String sql = "from Note n where n.referenceId = :refId and n.referenceTableId = :refTableId order by"
                    + " n.noteType desc, n.lastupdated desc";
            Query<Note> query = entityManager.unwrap(Session.class).createQuery(sql, Note.class);
            query.setParameter("refId", Integer.parseInt(note.getReferenceId()));

            query.setParameter("refTableId", Integer.parseInt(note.getReferenceTableId()));

            List<Note> list = query.list();
            return list;

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Note getAllNotesByRefIdRefTable()", e);
        }
    }

    @Override
    public boolean duplicateNoteExists(Note note) {
        try {

            List<Note> list;

            String sql = "from Note t where trim(lower(t.noteType)) = :noteType and t.referenceId = :referenceId"
                    + " and t.referenceTableId = :referenceTableId and trim(lower(t.text)) = :param4 and"
                    + " trim(lower(t.subject)) = :param5 and t.id != :noteId";
            Query<Note> query = entityManager.unwrap(Session.class).createQuery(sql, Note.class);
            query.setParameter("noteType", note.getNoteType().toLowerCase().trim());
            query.setParameter("referenceId", Integer.parseInt(note.getReferenceId()));

            query.setParameter("referenceTableId", Integer.parseInt(note.getReferenceTableId()));
            query.setParameter("param4", note.getText().toLowerCase().trim());
            query.setParameter("param5", note.getSubject().toLowerCase().trim());

            int noteId = !StringUtil.isNullorNill(note.getId()) ? Integer.parseInt(note.getId()) : 0;

            query.setParameter("noteId", noteId);

            list = query.list();

            return list.size() > 0;

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateNoteExists()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Note> getNotesChronologicallyByRefIdAndRefTableAndType(String objectId, String tableId,
            List<String> filter) throws LIMSRuntimeException {
        String sql = "FROM Note n where n.referenceId = :refId and n.referenceTableId = :tableId and n.noteType"
                + " in ( :filter ) order by n.lastupdated asc";

        try {
            Query<Note> query = entityManager.unwrap(Session.class).createQuery(sql, Note.class);
            query.setParameter("refId", Integer.parseInt(objectId));
            query.setParameter("tableId", Integer.parseInt(tableId));
            query.setParameterList("filter", filter);

            List<Note> noteList = query.list();
            return noteList;
        } catch (HibernateException e) {
            handleException(e, "getNotesChronologicallyByRefIdAndRefTable");
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Note> getNotesInDateRangeAndType(Date lowDate, Date highDate, String noteType, String referenceTableId)
            throws LIMSRuntimeException {
        String sql = "FROM Note n where n.noteType = :type and n.referenceTableId = :referenceTableId and"
                + " n.lastupdated between :lowDate and :highDate";

        try {
            Query<Note> query = entityManager.unwrap(Session.class).createQuery(sql, Note.class);
            query.setParameter("type", noteType);
            query.setParameter("referenceTableId", Integer.parseInt(referenceTableId));
            query.setParameter("lowDate", lowDate);
            query.setParameter("highDate", highDate);

            List<Note> noteList = query.list();
            return noteList;
        } catch (HibernateException e) {
            handleException(e, "getNotesInDateRangeAndType");
        }
        return null;
    }
}
