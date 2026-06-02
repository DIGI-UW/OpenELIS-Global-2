package org.openelisglobal.dictionarycategory.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.springframework.security.access.prepost.PreAuthorize;

public interface DictionaryCategoryService extends BaseObjectService<DictionaryCategory, String> {

    @PreAuthorize("hasAuthority('PRIV_DICTIONARY_VIEW')")
    DictionaryCategory getDictionaryCategoryByName(String name);
}
