package org.openelisglobal.notebook.dao;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
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

    NoteBook findParentTemplate(Integer entryId);

    /**
     * Find the direct parent notebook of an entry (the notebook that contains the
     * entry in its entries collection). This could be either a parent template or a
     * child instance.
     *
     * @param entryId the ID of the entry
     * @return the direct parent notebook, or null if not found
     */
    NoteBook findDirectParentNotebook(Integer entryId);

    /**
     * Find all child instances for a given parent template.
     *
     * @param parentId the ID of the parent template
     * @return list of child instances ordered by title
     */
    List<NoteBook> findChildrenByParentId(Integer parentId);

    /**
     * Find all parent templates (isTemplate=true AND parentNotebookId IS NULL).
     *
     * @return list of parent templates ordered by title
     */
    List<NoteBook> findAllParentTemplates();

    /**
     * Count entries for each child notebook.
     *
     * @param childIds list of child notebook IDs
     * @return map of childId to entry count
     */
    Map<Integer, Long> countEntriesForChildren(List<Integer> childIds);

    /**
     * Get total entry count across all children of a parent.
     *
     * @param parentId the ID of the parent template
     * @return total entry count
     */
    Long countEntriesForParent(Integer parentId);

}
