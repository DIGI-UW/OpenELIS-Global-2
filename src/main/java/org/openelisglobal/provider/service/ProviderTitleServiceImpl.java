package org.openelisglobal.provider.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderTitleServiceImpl implements ProviderTitleService {

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private DictionaryCategoryService dictionaryCategoryService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<Dictionary> getAllTitles() {
        DictionaryCategory category = dictionaryCategoryService.getDictionaryCategoryByName(CATEGORY_NAME);
        if (category == null) {
            return new ArrayList<>();
        }
        List<Dictionary> titles = new ArrayList<>(dictionaryService.getDictionaryEntriesByCategoryId(category.getId()));
        titles.sort(
                Comparator.comparing(entry -> entry.getSortOrder() == null ? Integer.MAX_VALUE : entry.getSortOrder()));
        return titles;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dictionary> getActiveTitles() {
        return dictionaryService.getActiveSortedEntriesByCategoryName(CATEGORY_NAME);
    }

    @Override
    @Transactional(readOnly = true)
    public Dictionary getByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (Dictionary title : getAllTitles()) {
            if (code.equalsIgnoreCase(title.getLocalAbbreviation())) {
                return title;
            }
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public int countProvidersUsing(String code) {
        if (code == null || code.isBlank()) {
            return 0;
        }
        Long count = entityManager
                .createQuery("SELECT count(p) FROM Provider p WHERE upper(p.person.titleCode) = :code", Long.class)
                .setParameter("code", code.toUpperCase()).getSingleResult();
        return count == null ? 0 : count.intValue();
    }
}
