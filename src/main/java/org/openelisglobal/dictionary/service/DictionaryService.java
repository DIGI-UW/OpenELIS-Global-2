package org.openelisglobal.dictionary.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public interface DictionaryService extends BaseObjectService<Dictionary, String> {
    @PreAuthorize("hasAuthority('PRIV_DICTIONARY_VIEW')")
    void getData(Dictionary dictionary);

    @PreAuthorize("hasAuthority('PRIV_DICTIONARY_VIEW')")
    Dictionary getDictionaryByLocalAbbrev(Dictionary dictionary);

    @PreAuthorize("hasAuthority('PRIV_DICTIONARY_VIEW')")
    public Dictionary getDictionaryByDictEntry(String dictEntry);

    @PreAuthorize("hasAuthority('PRIV_DICTIONARY_VIEW')")
    Dictionary getDictionaryById(String dictionaryId);

    @PreAuthorize("hasAuthority('PRIV_DICTIONARY_MANAGE')")
    boolean duplicateDictionaryExists(Dictionary dictionary);

    @PreAuthorize("hasAuthority('PRIV_DICTIONARY_VIEW')")
    boolean isDictionaryFrozen(Dictionary dictionary);

    @PreAuthorize("hasAuthority('PRIV_DICTIONARY_VIEW')")
    List<Dictionary> getDictionaryEntriesByCategoryId(String categoryId);

    @PreAuthorize("hasAuthority('PRIV_DICTIONARY_VIEW')")
    List<Dictionary> getDictionaryEntrysByCategoryAbbreviation(String fieldName, String fieldValue,
            boolean orderByDictEntry);

    @PreAuthorize("hasAuthority('PRIV_DICTIONARY_VIEW')")
    List<Dictionary> getDictionaryEntrysByCategoryAbbreviation(String filter, String dictionaryCategory);

    @PreAuthorize("hasAuthority('PRIV_DICTIONARY_VIEW')")
    Dictionary getDictionaryEntrysByNameAndCategoryDescription(String dictionaryName, String categoryDescription);

    @PreAuthorize("hasAuthority('PRIV_DICTIONARY_VIEW')")
    Dictionary getDictionaryEntryByNameAndCategoryName(String dictionaryName, String categoryName);

    @PreAuthorize("hasAuthority('PRIV_DICTIONARY_VIEW')")
    List<Dictionary> getDictionaryEntrysByCategoryNameLocalizedSort(String dictionaryCategoryName);

    @PreAuthorize("hasAuthority('PRIV_DICTIONARY_VIEW')")
    Dictionary getDataForId(String dictId);

    @PreAuthorize("hasAuthority('PRIV_DICTIONARY_MANAGE')")
    void update(Dictionary dictionary, boolean isDictionaryFrozenCheckRequired);

    @PreAuthorize("hasAuthority('PRIV_DICTIONARY_VIEW')")
    int getCountSearchedDictionaries(String searchString);

    @PreAuthorize("hasAuthority('PRIV_DICTIONARY_VIEW')")
    List<Dictionary> getPagesOfSearchedDictionaries(int startingRecNo, String searchString);
}
