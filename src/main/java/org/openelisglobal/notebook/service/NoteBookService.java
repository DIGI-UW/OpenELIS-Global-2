package org.openelisglobal.notebook.service;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.notebook.bean.NoteBookDisplayBean;
import org.openelisglobal.notebook.bean.NoteBookFullDisplayBean;
import org.openelisglobal.notebook.bean.NotebookHierarchyDTO;
import org.openelisglobal.notebook.bean.SampleDisplayBean;
import org.openelisglobal.notebook.form.NoteBookForm;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.valueholder.TestSection;

public interface NoteBookService extends BaseObjectService<NoteBook, Integer> {

    List<NoteBook> filterNoteBookEntries(List<NoteBookStatus> statuses, List<String> types, List<String> tags,
            Date fromDate, Date toDate, Integer noteBookId, Boolean orphanOnly);

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
     * Check if a page is a storage page (by title pattern). Storage pages should
     * skip to Disposal & Archiving when marked complete, bypassing the Reporting
     * page.
     *
     * @param pageId the page ID
     * @return true if this is a storage page
     */
    boolean isStoragePage(Integer pageId);

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

    /**
     * Update the organizations assigned to a notebook template. This controls which
     * locations/labs can see and create entries from this template.
     *
     * @param notebookId      the template notebook ID
     * @param organizationIds list of organization IDs to assign
     * @param sysUserId       the user making the change
     */
    void updateTemplateOrganizations(Integer notebookId, List<String> organizationIds, String sysUserId);

    /**
     * Update the allowed roles for a notebook template. These roles can create
     * entries from this template.
     *
     * @param notebookId   the template notebook ID
     * @param allowedRoles list of role names to allow
     * @param sysUserId    the user making the change
     */
    void updateTemplateAllowedRoles(Integer notebookId, List<String> allowedRoles, String sysUserId);

    /**
     * Update the departments (test sections) assigned to a notebook template. This
     * controls which departments can see and create entries from this template.
     *
     * @param notebookId    the template notebook ID
     * @param departmentIds list of test section IDs to assign
     * @param sysUserId     the user making the change
     */
    void updateTemplateDepartments(Integer notebookId, List<String> departmentIds, String sysUserId);

    /**
     * Get the departments (test sections) assigned to a notebook template.
     * Initializes the lazy collection within a transaction.
     *
     * @param notebookId the notebook ID
     * @return set of TestSection or empty set
     */
    Set<TestSection> getNoteBookDepartments(Integer notebookId);

    /**
     * Get the organizations assigned to a notebook template. Initializes the lazy
     * collection within a transaction.
     *
     * @param notebookId the notebook ID
     * @return set of Organization or empty set
     */
    Set<Organization> getNoteBookOrganizations(Integer notebookId);

    /**
     * Get the allowed roles assigned to a notebook template. Initializes the lazy
     * collection within a transaction.
     *
     * @param notebookId the notebook ID
     * @return set of roles (Strings) or empty set
     */
    Set<String> getNoteBookAllowedRoles(Integer notebookId);

    /**
     * Find the parent template for a given entry ID.
     *
     * @param entryId the entry ID
     * @return the parent NoteBook template or null if not found
     */
    NoteBook getParentTemplate(Integer entryId);

    // ========== Notebook Hierarchy Methods ==========

    /**
     * Create a child instance from a parent template. The child inherits pages from
     * the parent (live inheritance) but gets copies of metadata that can be edited
     * independently.
     *
     * @param parentId  the ID of the parent template
     * @param title     the title for the child instance
     * @param sysUserId the user creating the instance
     * @return the created child instance
     * @throws IllegalArgumentException if parent doesn't exist or is not a template
     */
    NoteBook createChildInstance(Integer parentId, String title, String sysUserId);

    /**
     * Get all child instances for a parent template.
     *
     * @param parentId the ID of the parent template
     * @return list of child instances ordered by title
     */
    List<NoteBook> getChildInstances(Integer parentId);

    /**
     * Get the hierarchy tree structure for all notebooks. Returns parent templates
     * with their children nested.
     *
     * @return list of parent templates with children
     */
    List<NotebookHierarchyDTO> getHierarchyTree();

    /**
     * Get aggregated statistics for a parent template. Sums up statistics from all
     * child instances.
     *
     * @param parentId the ID of the parent template
     * @return map with keys: totalEntries, drafts, pendingReview, finalizedThisWeek
     */
    Map<String, Long> getAggregatedStatistics(Integer parentId);

    /**
     * Check if a notebook can accept entries. Only child instances can have entries
     * added directly.
     *
     * @param notebookId the notebook ID
     * @return true if the notebook is a child instance
     */
    boolean canAcceptEntries(Integer notebookId);

    /**
     * Get all parent templates (notebooks with isTemplate=true and no parent).
     *
     * @return list of parent templates
     */
    List<NoteBook> getAllParentTemplates();

    /**
     * Add an entry notebook to a parent notebook's entries collection. This method
     * handles the lazy collection within a transaction.
     *
     * @param notebookId the parent notebook ID (template or child instance)
     * @param entry      the entry notebook to add
     * @param sysUserId  the user performing the action
     */
    void addEntry(Integer notebookId, NoteBook entry, String sysUserId);
}
