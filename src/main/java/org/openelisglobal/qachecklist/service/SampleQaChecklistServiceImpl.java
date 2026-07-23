package org.openelisglobal.qachecklist.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.qachecklist.dao.SampleQaChecklistDAO;
import org.openelisglobal.qachecklist.valueholder.SampleQaChecklist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SampleQaChecklistServiceImpl extends BaseObjectServiceImpl<SampleQaChecklist, Integer>
        implements SampleQaChecklistService {

    @Autowired
    private SampleQaChecklistDAO sampleQaChecklistDAO;

    @Autowired
    private DictionaryService dictionaryService;

    public SampleQaChecklistServiceImpl() {
        super(SampleQaChecklist.class);
    }

    @Override
    protected BaseDAO<SampleQaChecklist, Integer> getBaseObjectDAO() {
        return sampleQaChecklistDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public SampleQaChecklist findBySampleId(Integer sampleId) {
        return sampleQaChecklistDAO.findBySampleId(sampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public SampleQaChecklist findBySampleId(String sampleId) {
        return sampleQaChecklistDAO.findBySampleId(sampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dictionary> getActiveChecklistItems() {
        // Get all dictionary entries for the QAChecklistItem category
        List<Dictionary> allItems = dictionaryService
                .getDictionaryEntrysByCategoryNameLocalizedSort(QA_CHECKLIST_CATEGORY_NAME);

        // Filter to only active items and sort by sort_order
        return allItems.stream().filter(d -> "Y".equals(d.getIsActive())).sorted((a, b) -> {
            Integer orderA = a.getSortOrder() != null ? a.getSortOrder() : 999;
            Integer orderB = b.getSortOrder() != null ? b.getSortOrder() : 999;
            return orderA.compareTo(orderB);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SampleQaChecklist saveOrUpdateChecklist(Integer sampleId, Map<String, Boolean> verifiedItems,
            Integer userId) {

        SampleQaChecklist checklist = sampleQaChecklistDAO.findBySampleId(sampleId);

        if (checklist == null) {
            checklist = new SampleQaChecklist();
            checklist.setSampleId(sampleId);
        }

        checklist.setVerifiedItems(verifiedItems);

        if (userId != null) {
            checklist.setVerifiedByUserId(userId);
        }

        // Check if all active items are verified
        boolean allVerified = checkAllItemsVerified(verifiedItems);
        checklist.setAllRequiredVerified(allVerified);

        // Set verified date if all items are verified
        if (allVerified && checklist.getVerifiedDate() == null) {
            checklist.setVerifiedDate(new Timestamp(System.currentTimeMillis()));
        }

        return save(checklist);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean areAllItemsVerified(Integer sampleId) {
        SampleQaChecklist checklist = sampleQaChecklistDAO.findBySampleId(sampleId);
        if (checklist == null) {
            return false;
        }
        return Boolean.TRUE.equals(checklist.getAllRequiredVerified());
    }

    /**
     * Check if all active checklist items are verified based on the provided map.
     */
    private boolean checkAllItemsVerified(Map<String, Boolean> verifiedItems) {
        if (verifiedItems == null || verifiedItems.isEmpty()) {
            return false;
        }

        List<Dictionary> activeItems = getActiveChecklistItems();
        for (Dictionary item : activeItems) {
            Boolean isVerified = verifiedItems.get(item.getDictEntry());
            if (!Boolean.TRUE.equals(isVerified)) {
                return false;
            }
        }
        return true;
    }
}
