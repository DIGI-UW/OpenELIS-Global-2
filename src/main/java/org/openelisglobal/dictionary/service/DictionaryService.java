package org.openelisglobal.dictionary.service;

import java.util.List;
import org.openelisglobal.common.security.CrudPrivileges;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@CrudPrivileges(write = "PRIV_DICTIONARY_MANAGE")
/*
 * The dictionary backs the reference lists behind order-form dropdowns -
 * collection methods, specimen origins, sampling-site types, the acceptance
 * checklist. Reading those is a catalogue read, so every VIEW gate below also
 * accepts PRIV_CATALOGUE_VIEW. Editing the dictionary keeps
 * PRIV_DICTIONARY_MANAGE.
 */
public interface DictionaryService extends BaseObjectService<Dictionary, String> {
    @PreAuthorize("hasAnyAuthority('PRIV_DICTIONARY_VIEW','PRIV_CATALOGUE_VIEW')")
    void getData(Dictionary dictionary);

    @PreAuthorize("hasAnyAuthority('PRIV_DICTIONARY_VIEW','PRIV_CATALOGUE_VIEW')")
    Dictionary getDictionaryByLocalAbbrev(Dictionary dictionary);

    @PreAuthorize("hasAnyAuthority('PRIV_DICTIONARY_VIEW','PRIV_CATALOGUE_VIEW')")
    public Dictionary getDictionaryByDictEntry(String dictEntry);

    @PreAuthorize("hasAnyAuthority('PRIV_DICTIONARY_VIEW','PRIV_CATALOGUE_VIEW')")
    Dictionary getDictionaryById(String dictionaryId);

    @PreAuthorize("hasAuthority('PRIV_DICTIONARY_MANAGE')")
    boolean duplicateDictionaryExists(Dictionary dictionary);

    @PreAuthorize("hasAnyAuthority('PRIV_DICTIONARY_VIEW','PRIV_CATALOGUE_VIEW')")
    boolean isDictionaryFrozen(Dictionary dictionary);

    @PreAuthorize("hasAnyAuthority('PRIV_DICTIONARY_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Dictionary> getDictionaryEntriesByCategoryId(String categoryId);

    @PreAuthorize("hasAnyAuthority('PRIV_DICTIONARY_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Dictionary> getDictionaryEntrysByCategoryAbbreviation(String fieldName, String fieldValue,
            boolean orderByDictEntry);

    @PreAuthorize("hasAnyAuthority('PRIV_DICTIONARY_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Dictionary> getDictionaryEntrysByCategoryAbbreviation(String filter, String dictionaryCategory);

    @PreAuthorize("hasAnyAuthority('PRIV_DICTIONARY_VIEW','PRIV_CATALOGUE_VIEW')")
    Dictionary getDictionaryEntrysByNameAndCategoryDescription(String dictionaryName, String categoryDescription);

    @PreAuthorize("hasAnyAuthority('PRIV_DICTIONARY_VIEW','PRIV_CATALOGUE_VIEW')")
    Dictionary getDictionaryEntryByNameAndCategoryName(String dictionaryName, String categoryName);

    @PreAuthorize("hasAnyAuthority('PRIV_DICTIONARY_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Dictionary> getDictionaryEntrysByCategoryNameLocalizedSort(String dictionaryCategoryName);

    @PreAuthorize("hasAnyAuthority('PRIV_DICTIONARY_VIEW','PRIV_CATALOGUE_VIEW')")
    Dictionary getDataForId(String dictId);

    @PreAuthorize("hasAuthority('PRIV_DICTIONARY_MANAGE')")
    void update(Dictionary dictionary, boolean isDictionaryFrozenCheckRequired);

    @PreAuthorize("hasAnyAuthority('PRIV_DICTIONARY_VIEW','PRIV_CATALOGUE_VIEW')")
    int getCountSearchedDictionaries(String searchString);

    @PreAuthorize("hasAnyAuthority('PRIV_DICTIONARY_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Dictionary> getPagesOfSearchedDictionaries(int startingRecNo, String searchString);

    @PreAuthorize("hasAnyAuthority('PRIV_DICTIONARY_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Dictionary> getActiveSortedEntriesByCategoryName(String categoryName);
}
