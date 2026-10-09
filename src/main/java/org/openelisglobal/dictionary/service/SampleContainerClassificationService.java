package org.openelisglobal.dictionary.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads explicitly configured population metadata on the shared container
 * catalog.
 */
@Service
@Transactional(readOnly = true)
public class SampleContainerClassificationService {
    private final DictionaryService dictionaries;

    public SampleContainerClassificationService(DictionaryService dictionaries) {
        this.dictionaries = dictionaries;
    }

    public String population(String container) {
        if (container == null || container.isBlank())
            return null;
        var entry = dictionaries.getDictionaryEntryByNameAndCategoryName(container, "Sample Container");
        return entry == null ? null : entry.getContainerPopulation();
    }
}
