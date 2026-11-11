package org.openelisglobal.dictionary.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.openelisglobal.dictionary.valueholder.Dictionary;

public class SampleContainerClassificationServiceTest {
    @Test
    public void classificationComesFromSharedDictionaryMetadataRatherThanTheLabel() {
        DictionaryService dictionaries = mock(DictionaryService.class);
        Dictionary explicit = new Dictionary();
        explicit.setDictEntry("B17");
        explicit.setContainerPopulation("PAEDIATRIC");
        Dictionary unclassified = new Dictionary();
        unclassified.setDictEntry("Adult bottle");
        explicit.setIsActive("N");
        when(dictionaries.getDictionaryEntryByNameAndCategoryName("B17", "Sample Container")).thenReturn(explicit);
        when(dictionaries.getDictionaryEntryByNameAndCategoryName("Adult bottle", "Sample Container"))
                .thenReturn(unclassified);
        var service = new SampleContainerClassificationService(dictionaries);
        assertEquals("PAEDIATRIC", service.population("B17"));
        assertNull(service.population("Adult bottle"));
        assertNull(service.population("Paediatric unknown"));
        assertNull(service.population(null));
    }
}
