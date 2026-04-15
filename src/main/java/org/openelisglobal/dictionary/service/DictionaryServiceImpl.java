package org.openelisglobal.dictionary.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.exception.LIMSFrozenRecordException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.dictionary.dao.DictionaryDAO;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.LocalizationServiceImpl.LocalizationType;
import org.openelisglobal.localization.valueholder.Localization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DictionaryServiceImpl extends AuditableBaseObjectServiceImpl<Dictionary, String>
        implements DictionaryService {

    @Autowired
    protected DictionaryDAO baseObjectDAO;

    @Autowired
    private LocalizationService localizationService;

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
            // Generate UUID if not already set (defense-in-depth for UI-created
            // dictionaries)
            if (dictionary.getGuid() == null || dictionary.getGuid().isEmpty()) {
                dictionary.setGuid(UUID.randomUUID().toString());
            }
            return super.update(dictionary);
        }
    }

    @Override
    @Transactional
    public String insert(Dictionary dictionary) {
        if (duplicateDictionaryExists(dictionary)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + dictionary.getDictEntry());
        }
        // Generate UUID if not already set (for UI-created dictionaries)
        if (dictionary.getGuid() == null || dictionary.getGuid().isEmpty()) {
            dictionary.setGuid(UUID.randomUUID().toString());
        }
        // Auto-create localization record if not present
        if (dictionary.getLocalizedDictionaryName() == null && dictionary.getDictEntry() != null) {
            Localization localization = createLocalizationForDictionary(dictionary);
            localization.setSysUserId(dictionary.getSysUserId());
            localizationService.insert(localization);
            dictionary.setLocalizedDictionaryName(localization);
        }
        return super.insert(dictionary);
    }

    private Localization createLocalizationForDictionary(Dictionary dictionary) {
        Localization localization = new Localization();
        localization.setDescription(LocalizationType.DICTIONARY_NAME.getDBDescription());
        localization.setEnglish(dictionary.getDictEntry());
        return localization;
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

    @Override
    @Transactional(readOnly = true)
    public Dictionary getDictionaryByGuid(String guid) {
        return getBaseObjectDAO().getDictionaryByGuid(guid);
    }

    @Override
    @Transactional(readOnly = true)
    public Dictionary getDictionaryByLoincCode(String loincCode) {
        return getBaseObjectDAO().getDictionaryByLoincCode(loincCode);
    }

    @Override
    @Transactional(readOnly = true)
    public Dictionary getDictionaryByDictEntryIgnoreCase(String name) {
        return getBaseObjectDAO().getDictionaryByDictEntryIgnoreCase(name);
    }
}
