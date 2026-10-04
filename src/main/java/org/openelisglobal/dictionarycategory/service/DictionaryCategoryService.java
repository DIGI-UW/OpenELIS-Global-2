package org.openelisglobal.dictionarycategory.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.springframework.security.access.prepost.PreAuthorize;

public interface DictionaryCategoryService extends BaseObjectService<DictionaryCategory, String> {

    @PreAuthorize("hasAnyAuthority('PRIV_DICTIONARY_VIEW','PRIV_CATALOGUE_VIEW')")
    DictionaryCategory getDictionaryCategoryByName(String name);

    @PreAuthorize("hasAnyAuthority('PRIV_DICTIONARY_VIEW','PRIV_CATALOGUE_VIEW')")
    boolean duplicateDictionaryCategoryExists(DictionaryCategory dictionaryCategory);
}
