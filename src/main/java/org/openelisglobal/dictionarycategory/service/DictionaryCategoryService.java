package org.openelisglobal.dictionarycategory.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;

public interface DictionaryCategoryService extends BaseObjectService<DictionaryCategory, String> {

    DictionaryCategory getDictionaryCategoryByName(String name);
}
