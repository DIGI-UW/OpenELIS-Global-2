package org.openelisglobal.dictionary.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.exception.LIMSFrozenRecordException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.dictionary.dao.DictionaryDAO;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DictionaryServiceImpl extends AuditableBaseObjectServiceImpl<Dictionary, String>
        implements DictionaryService {

    @Autowired
    protected DictionaryDAO baseObjectDAO;

    DictionaryServiceImpl() {
        super(Dictionary.class);
    }

    @Override
    protected DictionaryDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional
    public Dictionary update(Dictionary dictionary) {
        if (duplicateDictionaryExists(dictionary)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + dictionary.getDictEntry());
        } else {
            return super.update(dictionary);
        }
    }

    @Override
    @Transactional
    public String insert(Dictionary dictionary) {
        if (duplicateDictionaryExists(dictionary)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + dictionary.getDictEntry());
        } else {
            return super.insert(dictionary);
        }
    }

    @Override
    @Transactional
    public void delete(Dictionary dictionary) {
        Dictionary oldData = get(dictionary.getId());
        oldData.setIsActive(IActionConstants.NO);
        oldData.setSysUserId(dictionary.getSysUserId());
        updateDelete(oldData);
    }

    @Override
    @Transactional
    public void deleteAll(List<Dictionary> dictionaries) {
        for (Dictionary dictionary : dictionaries) {
            delete(dictionary);
        }
    }

    @Override
    @Transactional
    public void deleteAll(List<String> ids, String sysUserId) {
        for (String id : ids) {
            delete(id, sysUserId);
        }
    }

    @Override
    @Transactional
    public void update(Dictionary dictionary, boolean isDictionaryFrozenCheckRequired) {
        if (duplicateDictionaryExists(dictionary)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + dictionary.getDictEntry());
        }
        if (isDictionaryFrozenCheckRequired && isDictionaryFrozen(dictionary)) {
            throw new LIMSFrozenRecordException("Dictionary Entry is referenced " + dictionary.getDictEntry());
        }
        super.update(dictionary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dictionary> getPagesOfSearchedDictionaries(int startingRecNo, String searchString) {
        List<String> orderProperties = new ArrayList<>();
        orderProperties.add("dictionaryCategory.categoryName");
        orderProperties.add("dictEntry");
        return baseObjectDAO.getLikeOrderedPage("dictEntry", searchString, orderProperties, false, startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public int getCountSearchedDictionaries(String searchString) {
        return getCountLike("dictEntry", searchString);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dictionary> getDictionaryEntrysByCategoryAbbreviation(String fieldName, String fieldValue,
            boolean orderByDictEntry) {
        return baseObjectDAO.getDictionaryEntrysByCategoryAbbreviation(fieldName, fieldValue, orderByDictEntry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dictionary> getDictionaryEntriesByCategoryId(String dictionaryCategoryId) {
        return baseObjectDAO.getAllMatching("dictionaryCategory.id", dictionaryCategoryId);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(Dictionary dictionary) {
        getBaseObjectDAO().getData(dictionary);
    }

    @Override
    @Transactional(readOnly = true)
    public Dictionary getDictionaryByLocalAbbrev(Dictionary dictionary) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("localAbbreviation", dictionary.getLocalAbbreviation());
        properties.put("isActive", IActionConstants.YES);
        return getMatch(properties).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Dictionary getDictionaryByDictEntry(String dictEntry) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("dictEntry", dictEntry);
        properties.put("isActive", IActionConstants.YES);
        return getMatch(properties).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Dictionary getDictionaryById(String dictionaryId) {
        return getBaseObjectDAO().getDictionaryById(dictionaryId.trim());
    }

    @Override
    public boolean duplicateDictionaryExists(Dictionary dictionary) {
        return getBaseObjectDAO().duplicateDictionaryExists(dictionary);
    }

    @Override
    public boolean isDictionaryFrozen(Dictionary dictionary) {
        return getBaseObjectDAO().isDictionaryFrozen(dictionary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dictionary> getDictionaryEntrysByCategoryAbbreviation(String filter, String dictionaryCategory) {
        return getBaseObjectDAO().getDictionaryEntrysByCategoryAbbreviation(filter, dictionaryCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public Dictionary getDictionaryEntrysByNameAndCategoryDescription(String dictionaryName,
            String categoryDescription) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("dictEntry", dictionaryName);
        properties.put("dictionaryCategory.description", categoryDescription);
        return getMatch(properties).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dictionary> getDictionaryEntrysByCategoryNameLocalizedSort(String dictionaryCategoryName) {
        return getBaseObjectDAO().getDictionaryEntrysByCategoryNameLocalizedSort(dictionaryCategoryName);
    }

    @Override
    @Transactional(readOnly = true)
    public Dictionary getDataForId(String dictId) {
        return getBaseObjectDAO().getDataForId(dictId);
    }
}
