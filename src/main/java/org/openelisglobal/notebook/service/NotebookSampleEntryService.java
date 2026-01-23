package org.openelisglobal.notebook.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Service interface for sample entry operations in notebooks. Handles searching
 * for samples and linking them to notebook instances.
 */
public interface NotebookSampleEntryService {

    /**
     * Search for samples matching the given criteria.
     *
     * @param accessionNumber optional accession number filter
     * @param patientName     optional patient name filter
     * @param sampleType      optional sample type filter
     * @param dateFrom        optional start date filter
     * @param dateTo          optional end date filter
     * @return list of matching SampleItem entities
     */
    List<SampleItem> searchSamples(String accessionNumber, String patientName, String sampleType, String dateFrom,
            String dateTo);

    /**
     * Link multiple samples to a notebook instance. Creates NotebookPageSample
     * records for each sample on each page.
     *
     * @param notebookId    the notebook ID to link samples to
     * @param sampleItemIds list of sample item IDs to link
     * @return number of samples successfully linked
     */
    int linkSamplesToNotebook(Integer notebookId, List<Integer> sampleItemIds);

    /**
     * Unlink a sample from a notebook instance. Removes all NotebookPageSample
     * records for the sample.
     *
     * @param notebookId   the notebook ID
     * @param sampleItemId the sample item ID to unlink
     * @return true if successfully unlinked
     */
    boolean unlinkSampleFromNotebook(Integer notebookId, Integer sampleItemId);

    /**
     * Get samples linked to a notebook.
     *
     * @param notebookId the notebook ID
     * @return list of SampleItem entities linked to the notebook
     */
    List<SampleItem> getSamplesForNotebook(Integer notebookId);

    /**
     * Check if a sample is already linked to a notebook.
     *
     * @param notebookId   the notebook ID
     * @param sampleItemId the sample item ID
     * @return true if the sample is already linked
     */
    boolean isSampleLinked(Integer notebookId, Integer sampleItemId);

    /**
     * Create child samples from parent samples. Each parent creates the specified
     * number of child samples with parent-child linking.
     *
     * @param notebookId          the notebook ID for linking children
     * @param parentSampleIds     list of parent sample item IDs
     * @param childCountPerParent number of children to create per parent
     * @param externalIdPrefix    prefix for generated external IDs
     * @param sysUserId           the system user ID for audit trail
     * @return list of created child SampleItem entities
     */
    List<SampleItem> createChildSamples(Integer notebookId, List<Integer> parentSampleIds, int childCountPerParent,
            String externalIdPrefix, String sysUserId);

    /**
     * Create child samples from parent samples, linking them to a specific page.
     * Child samples will appear on the specified page with PENDING status.
     *
     * @param notebookId          the notebook ID for linking children
     * @param pageId              the page ID where child samples should appear
     * @param parentSampleIds     list of parent sample item IDs
     * @param childCountPerParent number of children to create per parent
     * @param externalIdPrefix    prefix for generated external IDs
     * @param sysUserId           the system user ID for audit trail
     * @return list of created child SampleItem entities
     */
    List<SampleItem> createChildSamplesForPage(Integer notebookId, Integer pageId, List<Integer> parentSampleIds,
            int childCountPerParent, String externalIdPrefix, String sysUserId);

    /**
     * Create child samples from parent samples, linking them to a specific page,
     * with aliquot-specific data (volume, type, etc.).
     *
     * @param notebookId          the notebook ID for linking children
     * @param pageId              the page ID where child samples should appear
     * @param parentSampleIds     list of parent sample item IDs
     * @param childCountPerParent number of children to create per parent
     * @param externalIdPrefix    prefix for generated external IDs
     * @param sysUserId           the system user ID for audit trail
     * @param aliquotData         optional aliquot-specific data to store
     * @return list of created child SampleItem entities
     */
    List<SampleItem> createChildSamplesForPage(Integer notebookId, Integer pageId, List<Integer> parentSampleIds,
            int childCountPerParent, String externalIdPrefix, String sysUserId, Map<String, Object> aliquotData);

    /**
     * Get child samples for a parent sample.
     *
     * @param parentSampleId the parent sample item ID
     * @return list of child SampleItem entities
     */
    List<SampleItem> getChildSamples(Integer parentSampleId);

    /**
     * Get parent sample for a child sample.
     *
     * @param childSampleId the child sample item ID
     * @return parent SampleItem or null if no parent
     */
    SampleItem getParentSample(Integer childSampleId);
}
