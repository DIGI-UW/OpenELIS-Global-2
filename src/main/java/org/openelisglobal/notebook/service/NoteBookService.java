package org.openelisglobal.notebook.service;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.notebook.bean.NoteBookDisplayBean;
import org.openelisglobal.notebook.bean.NoteBookFullDisplayBean;
import org.openelisglobal.notebook.bean.SampleDisplayBean;
import org.openelisglobal.notebook.form.NoteBookForm;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

public interface NoteBookService extends BaseObjectService<NoteBook, Integer> {

    List<NoteBook> filterNoteBookEntries(List<NoteBookStatus> statuses, List<String> types, List<String> tags,
            Date fromDate, Date toDate, Integer noteBookId);

    List<NoteBook> filterNoteBooks(List<NoteBookStatus> statuses, List<String> types, List<String> tags, Date fromDate,
            Date toDate);

    List<NoteBook> getAllTemplateNoteBooks();

    List<NoteBook> getNoteBookEntries(Integer templateId);

    void updateWithStatus(Integer noteBookId, NoteBookStatus status, String sysUserId);

    NoteBook createWithFormValues(NoteBookForm form);

    void updateWithFormValues(Integer noteBookId, NoteBookForm form);

    NoteBookDisplayBean convertToDisplayBean(Integer noteBookId);

    NoteBookFullDisplayBean convertToFullDisplayBean(Integer noteBookId);

    Long getCountWithStatus(List<NoteBookStatus> statuses);

    Long getCountWithStatusBetweenDates(List<NoteBookStatus> statuses, Timestamp from, Timestamp to);

    Long getTotalCount();

    List<SampleDisplayBean> searchSampleItems(String accession);

    List<NoteBook> getAllActiveNotebooks();

    SampleDisplayBean convertSampleToDisplayBean(SampleItem sampleItem);

    /**
     * Create a new notebook instance from a template. Copies all pages from the
     * template to the new instance.
     *
     * @param templateId the ID of the template notebook
     * @param title      the title for the new instance
     * @param sysUserId  the user creating the instance
     * @return the created notebook instance
     */
    NoteBook createInstanceFromTemplate(Integer templateId, String title, String sysUserId);

    /**
     * Get a notebook page by its ID.
     *
     * @param pageId the page ID
     * @return the NoteBookPage or null if not found
     */
    NoteBookPage getPage(Integer pageId);

    /**
     * Get the next page in the workflow sequence after the given page.
     *
     * @param pageId the current page ID
     * @return the next NoteBookPage or null if this is the last page
     */
    NoteBookPage getNextPage(Integer pageId);

    /**
     * Get the previous page in the workflow sequence before the given page.
     *
     * @param pageId the current page ID
     * @return the previous NoteBookPage or null if this is the first page
     */
    NoteBookPage getPreviousPage(Integer pageId);

    /**
     * Get the last page (archiving page) for a notebook.
     *
     * @param notebookId the notebook ID
     * @return the last NoteBookPage in the workflow sequence
     */
    NoteBookPage getLastPage(Integer notebookId);

    /**
     * Check if a page is a routing page (by title pattern).
     *
     * @param pageId the page ID
     * @return true if this is a routing page
     */
    boolean isRoutingPage(Integer pageId);

    /**
     * Attach a file to a notebook for audit trail purposes. Creates a NoteBookFile
     * record linked to the notebook.
     *
     * @param notebookId the notebook ID
     * @param fileData   the file content as byte array
     * @param fileName   the file name (e.g., "Results_2024-01-15.xlsx")
     * @param fileType   the MIME type (e.g.,
     *                   "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
     * @param sysUserId  the user attaching the file
     * @return the ID of the created NoteBookFile record
     */
    Integer attachFile(Integer notebookId, byte[] fileData, String fileName, String fileType, String sysUserId);

    /**
     * Get all files attached to a notebook.
     *
     * @param notebookId the notebook ID
     * @return list of NoteBookFile records
     */
    List<org.openelisglobal.notebook.valueholder.NoteBookFile> getFiles(Integer notebookId);

    /**
     * Sync pages from template to an instance notebook. Adds any pages that exist
     * in the template but are missing from the instance. This is useful when new
     * pages are added to a template after instances were created.
     *
     * @param instanceId the notebook instance ID
     * @param sysUserId  the user performing the sync
     * @return number of pages added to the instance
     */
    int syncPagesFromTemplate(Integer instanceId, String sysUserId);
}
