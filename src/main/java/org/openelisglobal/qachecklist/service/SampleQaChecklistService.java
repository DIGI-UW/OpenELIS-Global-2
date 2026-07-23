package org.openelisglobal.qachecklist.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.qachecklist.valueholder.SampleQaChecklist;

public interface SampleQaChecklistService extends BaseObjectService<SampleQaChecklist, Integer> {

    /** Dictionary category name for QA checklist items. */
    String QA_CHECKLIST_CATEGORY_NAME = "QAChecklistItem";

    /**
     * Find QA checklist by sample ID.
     *
     * @param sampleId the sample ID
     * @return the QA checklist or null if not found
     */
    SampleQaChecklist findBySampleId(Integer sampleId);

    /**
     * Find QA checklist by sample ID (String version).
     *
     * @param sampleId the sample ID as string
     * @return the QA checklist or null if not found
     */
    SampleQaChecklist findBySampleId(String sampleId);

    /**
     * Get all active checklist items from the dictionary.
     *
     * @return list of active dictionary entries for QA checklist items
     */
    List<Dictionary> getActiveChecklistItems();

    /**
     * Save or update QA checklist for a sample. Creates a new checklist if one
     * doesn't exist, otherwise updates the existing one.
     *
     * @param sampleId      the sample ID
     * @param verifiedItems map of item keys (dict_entry) to their verification
     *                      status
     * @param userId        the user ID performing the verification
     * @return the saved checklist
     */
    SampleQaChecklist saveOrUpdateChecklist(Integer sampleId, Map<String, Boolean> verifiedItems, Integer userId);

    /**
     * Check if all active checklist items are verified for a sample.
     *
     * @param sampleId the sample ID
     * @return true if all active items are verified
     */
    boolean areAllItemsVerified(Integer sampleId);
}
