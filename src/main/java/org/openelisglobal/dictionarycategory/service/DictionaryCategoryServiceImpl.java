package org.openelisglobal.dictionarycategory.service;

import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.dictionarycategory.dao.DictionaryCategoryDAO;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DictionaryCategoryServiceImpl extends AuditableBaseObjectServiceImpl<DictionaryCategory, String>
        implements DictionaryCategoryService {
    @Autowired
    protected DictionaryCategoryDAO baseObjectDAO;

    DictionaryCategoryServiceImpl() {
        super(DictionaryCategory.class);
    }

    @Override
    protected DictionaryCategoryDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    public String insert(DictionaryCategory dictionaryCategory) {
        if (getBaseObjectDAO().duplicateDictionaryCategoryExists(dictionaryCategory)) {
            throw new LIMSDuplicateRecordException(
                    "Duplicate record exists for " + dictionaryCategory.getCategoryName());
        }
        return super.insert(dictionaryCategory);
    }

    @Override
    public DictionaryCategory save(DictionaryCategory dictionaryCategory) {
        if (getBaseObjectDAO().duplicateDictionaryCategoryExists(dictionaryCategory)) {
            throw new LIMSDuplicateRecordException(
                    "Duplicate record exists for " + dictionaryCategory.getCategoryName());
        }
        return super.save(dictionaryCategory);
    }

    @Override
    public DictionaryCategory update(DictionaryCategory dictionaryCategory) {
        if (getBaseObjectDAO().duplicateDictionaryCategoryExists(dictionaryCategory)) {
            throw new LIMSDuplicateRecordException(
                    "Duplicate record exists for " + dictionaryCategory.getCategoryName());
        }
        return super.update(dictionaryCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public DictionaryCategory getDictionaryCategoryByName(String name) {
        return getBaseObjectDAO().getDictionaryCategoryByName(name);
    }
}
