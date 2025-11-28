package org.openelisglobal.notebook.dao;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;

public interface NoteBookDAO extends BaseDAO<NoteBook, Integer> {

    List<NoteBook> filterNoteBookEntries(List<NoteBookStatus> statuses, List<String> types, List<String> tags,
            Date fromDate, Date toDate, List<Integer> entryIds);

    List<NoteBook> filterNoteBooks(List<NoteBookStatus> statuses, List<String> types, List<String> tags, Date fromDate,
            Date toDate);

    Long getCountWithStatus(List<NoteBookStatus> statuses);

    Long getCountWithStatusBetweenDates(List<NoteBookStatus> statuses, Timestamp from, Timestamp to);

    Long getTotalCount();

}
