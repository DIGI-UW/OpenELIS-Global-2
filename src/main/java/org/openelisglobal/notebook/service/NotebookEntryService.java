package org.openelisglobal.notebook.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.notebook.valueholder.NotebookEntry.EntryStatus;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Service interface for NotebookEntry operations.
 */
public interface NotebookEntryService extends BaseObjectService<NotebookEntry, Integer> {

    /**
     * Create a new entry for a notebook (template).
     *
     * @param notebookId the template notebook ID
     * @param title      optional title for the entry (uses notebook title if null)
     * @param sysUserId  the creating user ID
     * @return the created entry
     */
    NotebookEntry createEntry(Integer notebookId, String title, String sysUserId);

    /**
     * Find all entries for a notebook template.
     */
    List<NotebookEntry> findByNotebookId(Integer notebookId);

    /**
     * Find entries by status.
     */
    List<NotebookEntry> findByStatus(EntryStatus status);

    /**
     * Find entries by technician.
     */
    List<NotebookEntry> findByTechnicianId(Integer technicianId);

    /**
     * Update entry status.
     */
    void updateStatus(Integer entryId, EntryStatus status, String sysUserId);

    /**
     * Add a sample to an entry.
     */
    void addSample(Integer entryId, SampleItem sample, String sysUserId);

    /**
     * Add multiple samples to an entry.
     */
    void addSamples(Integer entryId, List<SampleItem> samples, String sysUserId);

    /**
     * Remove a sample from an entry.
     */
    void removeSample(Integer entryId, Integer sampleItemId, String sysUserId);

    /**
     * Get samples for an entry.
     */
    List<SampleItem> getSamples(Integer entryId);

    /**
     * Count entries for a notebook.
     */
    Long countByNotebookId(Integer notebookId);

    /**
     * Count entries by status.
     */
    Long countByStatus(EntryStatus status);

    /**
     * Add a comment to an entry.
     */
    void addComment(Integer entryId, String text, String sysUserId);

    /**
     * Get entry by ID with lazy relationships initialized. Use this method when the
     * entry needs to be accessed outside the transaction context.
     */
    NotebookEntry getWithRelationships(Integer entryId);
}
